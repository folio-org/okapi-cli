package org.folio.okapi.cli;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.folio.okapi.common.OkapiClient;
import org.folio.okapi.common.OkapiLogger;
import org.folio.okapi.common.XOkapiHeaders;

public class MainVerticle extends AbstractVerticle {

  protected Map<String, String> headers = new HashMap<>();
  protected final Logger logger = OkapiLogger.get();
  protected OkapiClient cli;
  private JsonObject vertxConfig;
  protected JsonObject cliConfig;
  private JsonArray requestLog;
  private PrintWriter out;
  private FileSystem fs;
  protected String tenant;
  protected JsonArray installArray;
  private String confFname;

  @Override
  public void init(Vertx vertx, Context context) {
    logger.info("init begin");
    this.vertx = vertx;
    requestLog = new JsonArray();
    fs = vertx.fileSystem();
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "*/*");
    vertxConfig = context.config();
    cliConfig = new JsonObject();
    cliConfig.put("deploy", "false");
    cliConfig.put("okapiUrl", "http://localhost:9130");
    installArray = new JsonArray();
    JsonArray pullUrls = new JsonArray();
    pullUrls.add("http://folio-registry.aws.indexdata.com:80");
    cliConfig.put("pullUrls", pullUrls);
    confFname = vertxConfig.getString("okapi-cli-config-file");
    if (confFname == null) {
      final String home = System.getProperty("user.home");
      if (home != null) {
        confFname = home + "/" + ".okapi.cli";
      } else {
        confFname = ".okapi.cli";
      }
    }
    logger.info("init done");
  }

  @Override
  public void start(Future<Void> fut) throws IOException {
    logger.info("start");
    start1(res -> {
      if (res.failed()) {
        fut.handle(Future.failedFuture(res.cause()));
      } else {
        fut.complete();
      }
    });
  }

  private void setJsonBody(JsonObject obj, String key, Buffer b) {
    if (b == null || b.length() == 0) {
      return;
    }
    try {
      obj.put(key, b.toJsonObject());
      return;
    } catch (DecodeException ex) {
    }
    try {
      obj.put(key, b.toJsonArray());
      return;
    } catch (DecodeException ex) {
    }
    obj.put(key, b.toString());
  }

  private void usage(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.failedFuture("No command given; use help for help"));
  }

  protected void requestBuffer(HttpMethod method, String path, Buffer b,
    Handler<AsyncResult<Void>> handler) {

    JsonObject jReq = new JsonObject();
    jReq.put("method", method.name());
    jReq.put("path", path);
    JsonObject h = new JsonObject();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      h.put(entry.getKey(), entry.getValue());
    }
    jReq.put("headers", h);
    setJsonBody(jReq, "request", b);
    cli.setHeaders(headers);
    cli.request(method, path, b.toString(), res -> {
      if (res.failed()) {
        jReq.put("diagnostic", res.cause().getMessage());
        requestLog.add(jReq);
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        String token = cli.getRespHeaders().get(XOkapiHeaders.TOKEN);
        if (token != null) {
          headers.put(XOkapiHeaders.TOKEN, token);
        }
        setJsonBody(jReq, "response", Buffer.buffer(res.result()));
        requestLog.add(jReq);
        handler.handle(Future.succeededFuture());
      }
    });
  }

  protected void requestFile(HttpMethod method, String path, String file,
    Handler<AsyncResult<Void>> handler) {

    if (file.startsWith("{") || file.isEmpty()) {
      requestBuffer(method, path, Buffer.buffer(file), handler);
    } else {
      fs.readFile(path, res -> {
        if (res.failed()) {
          handler.handle(Future.failedFuture(res.cause()));
        } else {
          requestBuffer(method, path, res.result(), handler);
        }
      });
    }
  }

  private void readConf(Handler<AsyncResult<Void>> handler)  {
    fs.readFile(confFname, res -> {
      if (res.failed()) {
        logger.warn(confFname + ": " + res.cause().getMessage());
      } else {
        logger.info("reading " + confFname + "  OK");
        Buffer buf = res.result();
        cliConfig = new JsonObject(buf);
        tenant = cliConfig.getString("tenant");
        final String token = cliConfig.getString(XOkapiHeaders.TOKEN);
        if (token != null) {
          headers.put(XOkapiHeaders.TOKEN, token);
        }
        final String tenant = cliConfig.getString(XOkapiHeaders.TENANT);
        if (tenant != null) {
          headers.put(XOkapiHeaders.TENANT, tenant);
        }
      }
      handler.handle(Future.succeededFuture());
    });
  }

  private void confPut(String key, String val) {
    if (val == null) {
      cliConfig.remove(key);
    } else {
      cliConfig.put(key, val);
    }
  }
  private void writeConf(Handler<AsyncResult<Void>> handler) {
    confPut("tenant", tenant);
    confPut(XOkapiHeaders.TOKEN, headers.get(XOkapiHeaders.TOKEN));
    confPut(XOkapiHeaders.TENANT, headers.get(XOkapiHeaders.TENANT));
    fs.writeFile(confFname, cliConfig.toBuffer(), handler);
  }

  private void start1(Handler<AsyncResult<Void>> handler) {
    logger.info("start1");
    readConf(res -> {
      start2(res1 -> {
        writeConf(res2 -> {
          if (res1.failed()) {
            handler.handle(Future.failedFuture(res1.cause()));
          } else if (res2.failed()) {
            handler.handle(Future.failedFuture(res2.cause()));
          } else {
            handler.handle(Future.succeededFuture());
          }
        });
      });
    });
  }

  private void start2(Handler<AsyncResult<Void>> handler) {
    cli = new OkapiClient(cliConfig.getString("okapiUrl"), vertx, headers);

    List<Command> commands = new LinkedList<>();
    commands.add(new CommandDelete());
    commands.add(new CommandEnv());
    commands.add(new CommandGet());
    commands.add(new CommandInstall());
    commands.add(new CommandLogin());
    commands.add(new CommandLogout());
    commands.add(new CommandPost());
    commands.add(new CommandPull());
    commands.add(new CommandPut());
    commands.add(new CommandTenant());
    commands.add(new CommandVersion());

    JsonArray ar = vertxConfig.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      usage(handler);
    } else {
      Future<Void> futF = Future.future();

      futF.setHandler(h -> {
        if (h.succeeded()) {
          String fname = vertxConfig.getString("okapi-cli-output-file");
          if (fname != null) {
            try {
              out = new PrintWriter(fname);
              out.print(requestLog.encodePrettily());
              out.close();
            } catch (IOException ex) {
              handler.handle(Future.failedFuture(h.cause().getMessage()));
            }
          } else {
            System.out.println(requestLog.encodePrettily());
          }
          handler.handle(Future.succeededFuture());
        } else {
          handler.handle(Future.failedFuture(h.cause().getMessage()));
        }
      });

      Future<Void> fut1 = Future.future();
      fut1.complete();
      for (int i = 0; i < ar.size(); i++) {
        String a = ar.getString(i);
        logger.info("Inspecting a=" + a + " i=" + i);
        Future<Void> fut2 = Future.future();

        Command cmd = null;
        for (Command c : commands) {
          if (c.getName().equals(a)) {
            cmd = c;
            break;
          }
        }
        if (cmd != null) {
          int no = cmd.getNoArgs();
          if (i + no >= ar.size()) {
            fut1.compose(v -> {
              fut2.fail("Missing args for command: " + a);
            }, futF);
          } else {
            final int offset = i + 1;
            final Command fCmd = cmd;
            fut1.compose(v -> {
              fCmd.run(this, ar, offset, fut2.completer());
            }, futF);
            i += no;
          }
        } else if (a.equals("help")) {
          for (Command c : commands) {
            System.out.println(c.getDescription());
          }
          continue;
        } else if (a.startsWith("--okapi-url=")) {
          fut1.compose(v -> {
            final String url = a.substring(12);
            cliConfig.put("okapiUrl", url);
            if (cli != null) {
              cli.close();
            }
            cli = new OkapiClient(url, vertx, headers);
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--tenant=")) {
          fut1.compose(v -> {
            tenant = a.substring(9);
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--pull-url=")) {
          fut1.compose(v -> {
            final String [] urls = a.substring(11).split(",");
            JsonArray pullUrls = new JsonArray();
            for (String url : urls) {
              pullUrls.add(url);
            }
            cliConfig.put("pullUrls", pullUrls);
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--deploy=")) {
          fut1.compose(v -> {
            JsonObject j = new JsonObject();
            cliConfig.put("deploy", a.substring(9));
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--enable=")) {
          fut1.compose(v -> {
            JsonObject j = new JsonObject();
            j.put("action", "enable");
            j.put("id", a.substring(9));
            installArray.add(j);
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--disable=")) {
          fut1.compose(v -> {
            JsonObject j = new JsonObject();
            j.put("action", "disable");
            j.put("id", a.substring(10));
            installArray.add(j);
            fut2.complete();
          }, futF);
        } else if (a.equals("--no-tenant")) {
          fut1.compose(v -> {
            tenant = null;
            fut2.complete();
          }, futF);
        } else {
          fut1.compose(v -> {
            fut2.fail("Bad command: " + a);
          }, futF);
        }
        fut1 = fut2;
      }
      fut1.compose(v -> {
        futF.complete();
      }, futF);
    }
 }
}
