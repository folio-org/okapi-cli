package org.folio.okapi.cli;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.folio.okapi.common.OkapiClient;
import org.folio.okapi.common.XOkapiHeaders;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  protected Map<String, String> headers = new HashMap<>();
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
    logger.debug("init begin");
    this.vertx = vertx;
    requestLog = new JsonArray();
    fs = vertx.fileSystem();
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "*/*");
    vertxConfig = context.config();
    cliConfig = new JsonObject();
    cliConfig.put("deploy", "false");
    cliConfig.put("simulate", "false");
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
    logger.debug("init done");
  }

  @Override
  public void start(Promise<Void> fut) {
    logger.debug("start");
    start1().onComplete(fut);
  }

  private void setJsonBody(JsonObject obj, String key, String body) {
    if (Strings.isEmpty(body)) {
      return;
    }
    setJsonBody(obj, key, Buffer.buffer(body));
  }

  private void setJsonBody(JsonObject obj, String key, Buffer b) {
    if (b == null || b.length() == 0) {
      return;
    }
    try {
      obj.put(key, b.toJsonObject());
    } catch (DecodeException ex1) {
      try {
        obj.put(key, b.toJsonArray());
      } catch (DecodeException ex2) {
        obj.put(key, b.toString());
      }
    }
  }

  private <T> Future<T> usage() {
    return Future.failedFuture("No command given; use help for help");
  }

  protected Future<Void> requestBuffer(HttpMethod method, String path, Buffer b) {

    JsonObject hReq = new JsonObject();
    hReq.put("method", method.name());
    hReq.put("path", path);
    JsonObject h1 = new JsonObject();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      h1.put(entry.getKey(), entry.getValue());
    }
    JsonObject jReq = new JsonObject();
    jReq.put("headers", h1);
    setJsonBody(jReq, "body", b);
    hReq.put("request", jReq);
    cli.setHeaders(headers);
    return cli.request(method, path, b.toString())
        .onComplete(res -> {
          JsonObject jRes = new JsonObject();
          hReq.put("response", jRes);
          MultiMap respHeaders = cli.getRespHeaders();
          if (respHeaders != null) {
            final JsonObject h2 = new JsonObject();
            for (Map.Entry<String, String> entry : respHeaders.entries()) {
              h2.put(entry.getKey(), entry.getValue());
            }
            jRes.put("headers", h2);
          }
          if (res.failed()) {
            jRes.put("diagnostic", res.cause().getMessage());
            requestLog.add(hReq);
          } else {
            final String token = respHeaders.get(XOkapiHeaders.TOKEN);
            if (token != null) {
              headers.put(XOkapiHeaders.TOKEN, token);
            }
            setJsonBody(jRes, "body", res.result());
            requestLog.add(hReq);
          }
        })
        .<Void>mapEmpty();
  }

  protected void requestBuffer(HttpMethod method, String path, Buffer b,
      Handler<AsyncResult<Void>> handler) {

    requestBuffer(method, path, b).onComplete(handler);
  }

  protected Future<Void> requestFile(HttpMethod method, String path, String file) {

    if (file.startsWith("@")) {
      return fs.readFile(file.substring(1))
          .compose(res -> requestBuffer(method, path, res));
    }
    return requestBuffer(method, path, Buffer.buffer(file));
  }

  protected void requestFile(HttpMethod method, String path, String file,
      Handler<AsyncResult<Void>> handler) {

    requestFile(method, path, file).onComplete(handler);
  }

  private void confGet(String key) {
    final String val = cliConfig.getString(key);
    if (val != null) {
      headers.put(key, val);
    }
  }

  private Future<Void> readConf() {
    return fs.readFile(confFname)
        .onSuccess(buf -> {
          logger.debug("reading " + confFname + "  OK");
          cliConfig = new JsonObject(buf);
          tenant = cliConfig.getString("tenant");

          confGet(XOkapiHeaders.TOKEN);
          confGet(XOkapiHeaders.TENANT);
        })
        .otherwiseEmpty()
        .mapEmpty();
  }

  protected void installArrayAdd(String id, String name, String value) {
    JsonObject j = new JsonObject();
    j.put(name, value);
    j.put("id", id);
    installArray.add(j);
  }

  private void confPut(String key, String val) {
    if (val == null) {
      cliConfig.remove(key);
    } else {
      cliConfig.put(key, val);
    }
  }
  private Future<Void> writeConf() {
    confPut("tenant", tenant);
    confPut(XOkapiHeaders.TOKEN, headers.get(XOkapiHeaders.TOKEN));
    confPut(XOkapiHeaders.TENANT, headers.get(XOkapiHeaders.TENANT));
    return fs.writeFile(confFname, cliConfig.toBuffer());
  }

  private Future<Void> start1() {
    logger.debug("start1");
    return readConf()
        .compose(x -> start2())
        .compose(x -> writeConf());
  }

  private Future<Void> printRequestLog() {
    String fname = vertxConfig.getString("okapi-cli-output-file");
    if (fname == null) {
      System.out.println(requestLog.encodePrettily());
      return Future.succeededFuture();
    }
    try {
      out = new PrintWriter(fname);
      out.print(requestLog.encodePrettily());
      out.close();
      return Future.succeededFuture();
    } catch (IOException ex) {
      return Future.failedFuture(ex);
    }
  }

  private Future<Void> start2() {
    logger.debug("start2");

    cli = new OkapiClient(cliConfig.getString("okapiUrl"), vertx, headers);
    CommandFactory factory = new CommandFactory();

    JsonArray ar = vertxConfig.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      return usage();
    }
    Future<Void> future = Future.succeededFuture();
    int i = 0;
    while (i < ar.size()) {
      String a = ar.getString(i);
      logger.debug("Inspecting a=" + a + " i=" + i);

      Command cmd = factory.create(a);
      if (cmd != null) {
        int no = factory.noArgs(cmd);
        if (i + no >= ar.size()) {
          future = future.compose(v -> Future.failedFuture("Missing args for command: " + a));
        } else {
          final Command fCmd = cmd;
          if (cmd.getDescription().startsWith("-")) {
            JsonArray ar1 = new JsonArray();
            String opt = ar.getString(i);
            int idx = opt.indexOf('=');
            if (idx != -1) {
              ar1.add(opt.substring(idx + 1));
            }
            future = future.compose(v -> fCmd.run(this, ar1, 0));
          } else {
            final int offset = i + 1;
            future = future.compose(v -> fCmd.run(this, ar, offset));
          }
          i += no;
        }
      } else if (a.equals("help")) {
        factory.help();
        i++;
        continue;
      } else {
        future = future.compose(v -> Future.failedFuture("Bad command: " + a));
      }
      i++;
    }
    return future.compose(v -> printRequestLog());
  }
}
