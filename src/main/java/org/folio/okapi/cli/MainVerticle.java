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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.folio.okapi.common.OkapiClient;
import org.folio.okapi.common.OkapiLogger;
import org.folio.okapi.common.SemVer;
import org.folio.okapi.common.XOkapiHeaders;

public class MainVerticle extends AbstractVerticle {

  private Map<String, String> headers = new HashMap<>();
  private final Logger logger = OkapiLogger.get();
  private OkapiClient cli;
  private JsonObject vertxConfig;
  private JsonObject cliConfig;
  private Buffer buf;
  private PrintWriter out;
  private FileSystem fs;
  private String tenant;
  private JsonArray installArray;
  private String confFname;

  @Override
  public void init(Vertx vertx, Context context) {
    logger.info("init begin");
    this.vertx = vertx;
    buf = Buffer.buffer();
    fs = vertx.fileSystem();
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "*/*");
    vertxConfig = context.config();
    cliConfig = new JsonObject();
    cliConfig.put("okapiUrl", "http://localhost:9130");
    installArray = new JsonArray();
    JsonArray pullUrls = new JsonArray();
    pullUrls.add("http://folio-registry.aws.indexdata.com:80");
    cliConfig.put("pullUrls", pullUrls);
    confFname = vertxConfig.getString("okapi-cli-config-fname");
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

  int count = 0;

  private void version(Handler<AsyncResult<Void>> handler) {
    if (++count > 10) {
      try { // testing that we do not use stack with compose
        throw new NullPointerException();
      } catch (NullPointerException ex) {
        ex.printStackTrace();
      }
      handler.handle(Future.failedFuture("provoked failure"));
      return;
    }
    cli.setHeaders(headers);
    cli.get("/_/version", res -> {
      if (res.succeeded()) {
        try {
          SemVer okapiVer = new SemVer(res.result());
        } catch (IllegalArgumentException ex) {
          handler.handle(Future.failedFuture(ex));
          return;
        }
        buf.appendString(res.result());
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  private void usage(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.failedFuture("No command given"));
  }

  private void login(String tenant, String username, String password,
    Handler<AsyncResult<Void>> handler) {

    headers.put(XOkapiHeaders.TENANT, tenant);
    if (username == null) {
      handler.handle(Future.succeededFuture());
    } else {
      JsonObject j = new JsonObject();
      j.put("username", username);
      if (password != null) {
        j.put("password", password);
      }
      post("/authn/login", j.encode(), res -> {
        headers.remove(XOkapiHeaders.TENANT);
        if (res.failed()) {
          handler.handle(Future.failedFuture(res.cause()));
        } else {
          String token = cli.getRespHeaders().get(XOkapiHeaders.TOKEN);
          if (token != null) {
            headers.put(XOkapiHeaders.TOKEN, token);
          }
          handler.handle(Future.succeededFuture());
        }
      });
    }
  }

  private void logout(Handler<AsyncResult<Void>> handler) {
    headers.remove(XOkapiHeaders.TENANT);
    headers.remove(XOkapiHeaders.TOKEN);
    handler.handle(Future.succeededFuture());
  }

  private void requestBuffer(HttpMethod method, String path, Buffer b,
    Handler<AsyncResult<Void>> handler) {

    cli.setHeaders(headers);
    cli.request(method, path, b.toString(), res2 -> {
      if (res2.failed()) {
        handler.handle(Future.failedFuture(res2.cause()));
      } else {
        buf.appendString(res2.result());
        handler.handle(Future.succeededFuture());
      }
    });
  }

  private void requestFile(HttpMethod method, String path, String file,
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

  private void post(String path, String file, Handler<AsyncResult<Void>> handler) {
    requestFile(HttpMethod.POST, path, file, handler);
  }

  private void put(String path, String file, Handler<AsyncResult<Void>> handler) {
    requestFile(HttpMethod.PUT, path, file, handler);
  }

  private void get(String path, Handler<AsyncResult<Void>> handler) {
    requestBuffer(HttpMethod.GET, path, Buffer.buffer(), handler);
  }

  private void delete(String path, Handler<AsyncResult<Void>> handler) {
    requestBuffer(HttpMethod.DELETE, path, Buffer.buffer(), handler);
  }

  private void pull(Handler<AsyncResult<Void>> handler) {
    JsonObject j = new JsonObject();
    j.put("urls", cliConfig.getJsonArray("pullUrls"));
    post("/_/proxy/pull/modules", j.encode(), handler);
  }

  private void install(Handler<AsyncResult<Void>> handler) {
    if (tenant == null) {
      handler.handle(Future.failedFuture("Tenant not set (use --tenant=val)"));
    } else if (installArray.isEmpty()) {
      handler.handle(Future.failedFuture("Nothing to install"));
    } else {
      Buffer b = installArray.toBuffer();
      installArray.clear();
      requestBuffer(HttpMethod.POST, "/_/proxy/tenants/" + tenant + "/install",
        b, handler);
    }
  }

  private void readConf(Handler<AsyncResult<Void>> handler)
  {
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

    JsonArray ar = vertxConfig.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      usage(handler);
    } else {
      Future<Void> futF = Future.future();

      futF.setHandler(h -> {
        if (h.succeeded()) {
          String fname = vertxConfig.getString("file");
          if (fname != null) {
            try {
              out = new PrintWriter(fname);
              out.print(buf.toString());
              out.close();
            } catch (IOException ex) {
              handler.handle(Future.failedFuture(h.cause().getMessage()));
            }
          } else {
            if (buf.length() > 0) {
              System.out.println(buf.toString());
            }
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
        Future<Void> fut2 = Future.future();
        if (a.startsWith("--okapi-url=")) {
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
        } else if (a.startsWith("--no-tenant")) {
          fut1.compose(v -> {
            tenant = null;
            fut2.complete();
          }, futF);
        } else if (a.equals("tenant")) {
          final String tenant = ar.getString(++i);
          fut1.compose(v -> {
            login(tenant, null, null, fut2.completer());
          }, futF);
        } else if (a.equals("login")) {
          final String tenant = ar.getString(++i);
          final String username = ar.getString(++i);
          final String password = ar.getString(++i);
          fut1.compose(v -> {
            login(tenant, username, password, fut2.completer());
          }, futF);
        } else if (a.equals("logout")) {
          fut1.compose(v -> {
            logout(fut2.completer());
          }, futF);
        } else if (a.equals("pull")) {
          fut1.compose(v -> {
            pull(fut2.completer());
          }, futF);
        } else if (a.equals("install")) {
          fut1.compose(v -> {
            install(fut2.completer());
          }, futF);
        } else if (a.equals("post")) {
          final String path = ar.getString(++i);
          final String file = ar.getString(++i);
          fut1.compose(v -> {
            post(path, file, fut2.completer());
          }, futF);
        } else if (a.equals("put")) {
          final String path = ar.getString(++i);
          final String file = ar.getString(++i);
          fut1.compose(v -> {
            put(path, file, fut2.completer());
          }, futF);
        } else if (a.equals("get")) {
          final String path = ar.getString(++i);
          fut1.compose(v -> {
            get(path, fut2.completer());
          }, futF);
        } else if (a.equals("delete")) {
          final String path = ar.getString(++i);
          fut1.compose(v -> {
            delete(path, fut2.completer());
          }, futF);
        } else if (a.equals("version")) {
          fut1.compose(v -> {
            version(fut2.completer());
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
