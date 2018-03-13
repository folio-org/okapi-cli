package org.folio.okapi.cli;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import java.io.IOException;
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
  private Vertx vertx;
  private String tenant;

  @Override
  public void init(Vertx vertx, Context context) {
    this.vertx = vertx;
    headers.put("Content-Type", "application/json");
    conf = context.config();
    okapiUrl = "http://localhost:9130";
  }

  @Override
  public void start(Future<Void> fut) throws IOException {
    start2(res -> {
      if (res.failed()) {
        fut.handle(Future.failedFuture(res.cause()));
      } else {
        fut.complete();
      }
      vertx.close();
    });
  }

  int count = 0;

  private void version(Handler<AsyncResult<String>> handler) {
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
        try {
          SemVer okapiVer = new SemVer(res.result());
        } catch (IllegalArgumentException ex) {
          handler.handle(Future.failedFuture(ex));
          return;
        }
        System.out.println(res.result());
        handler.handle(Future.succeededFuture(res.result()));
      } else {
        handler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  private void usage(Handler<AsyncResult<String>> handler) {
    handler.handle(Future.failedFuture("No command given"));
  }

  private void setTenant(String s, Handler<AsyncResult<String>> handler) {
    logger.info("setTenant " + s);
    tenant = s;
    if (s.startsWith("/")) {
      handler.handle(Future.failedFuture("bad tenant " + s));
    } else {
      handler.handle(Future.succeededFuture(""));
    }
  }

  private void login(String username, String password, Handler<AsyncResult<String>> handler) {
    handler.handle(Future.succeededFuture(""));
  }

  private void start2(Handler<AsyncResult<String>> handler) {
    cli = new OkapiClient(okapiUrl, vertx, headers);

    JsonArray ar = conf.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      usage(handler);
    } else {
      Future<String> futF = Future.future();

      futF.setHandler(h -> {
        if (h.succeeded()) {
          logger.info("futF succeeeded");
          handler.handle(Future.succeededFuture());
        } else {
          logger.info("futF failed");
          handler.handle(Future.failedFuture(h.cause()));
        }
      });

      Future<String> fut1 = Future.future();
      fut1.complete();
      for (int i = 0; i < ar.size(); i++) {
        String a = ar.getString(i);
        Future<String> fut2 = Future.future();
        if (a.startsWith("--tenant=")) {
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
