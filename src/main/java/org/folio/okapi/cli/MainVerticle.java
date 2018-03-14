package org.folio.okapi.cli;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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

public class MainVerticle extends AbstractVerticle {

  private Map<String, String> headers = new HashMap<>();
  private String okapiUrl;
  private final Logger logger = OkapiLogger.get();
  private OkapiClient cli;
  private JsonObject conf;
  private String tenant;
  private Buffer buf;
  private PrintWriter out;

  @Override
  public void init(Vertx vertx, Context context) {
    logger.info("OkapiCli.init");
    this.vertx = vertx;
    headers.put("Content-Type", "application/json");
    conf = context.config();
    okapiUrl = "http://localhost:9130";
    buf = Buffer.buffer();
  }

  @Override
  public void start(Future<Void> fut) throws IOException {
    logger.info("OkapiCli.start");
    start2(res -> {
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
    logger.info("begin version");
    cli.get("/_/version", res -> {
      logger.info("end version");
      if (res.succeeded()) {
        logger.info("res.result()=" + res.result());
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

  private void setTenant(String s, Handler<AsyncResult<Void>> handler) {
    logger.info("setTenant " + s);
    tenant = s;
    if (s.startsWith("/")) {
      handler.handle(Future.failedFuture("bad tenant " + s));
    } else {
      handler.handle(Future.succeededFuture());
    }
  }

  private void login(String username, String password, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  private void start2(Handler<AsyncResult<Void>> handler) {
    cli = new OkapiClient(okapiUrl, vertx, headers);

    JsonArray ar = conf.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      usage(handler);
    } else {
      Future<String> futF = Future.future();

      futF.setHandler(h -> {
        if (h.succeeded()) {
          String fname = conf.getString("file");
          if (fname != null) {
            try {
              out = new PrintWriter(fname);
              out.print(buf.toString());
              out.close();
            } catch (IOException ex) {
              handler.handle(Future.failedFuture(h.cause().getMessage()));
            }
          } else {
            System.out.println(buf.toString());
          }
          handler.handle(Future.succeededFuture());
        } else {
          logger.info("futF failed");
          handler.handle(Future.failedFuture(h.cause().getMessage()));
        }
      });

      Future<Void> fut1 = Future.future();
      fut1.complete();
      for (int i = 0; i < ar.size(); i++) {
        String a = ar.getString(i);
        Future<Void> fut2 = Future.future();
        if (a.startsWith("--okapiurl=")) {
          fut1.compose(v -> {
            okapiUrl = a.substring(11);
            cli = new OkapiClient(okapiUrl, vertx, headers);
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--tenant=")) {
          fut1.compose(v -> {
            setTenant(a.substring(9), fut2.completer());
          }, futF);
        } else if (a.equals("login")) {
          final String username = ar.getString(++i);
          final String password = ar.getString(++i);
          fut1.compose(v -> {
            login(username, password, fut2.completer());
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
      logger.info("Done");
    }
 }
}
