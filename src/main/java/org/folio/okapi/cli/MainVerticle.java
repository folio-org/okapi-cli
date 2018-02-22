package org.folio.okapi.cli;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static org.folio.okapi.common.ErrorType.*;
import org.folio.okapi.common.ExtendedAsyncResult;
import org.folio.okapi.common.Failure;
import org.folio.okapi.common.OkapiClient;
import org.folio.okapi.common.OkapiLogger;
import org.folio.okapi.common.SemVer;
import org.folio.okapi.common.Success;

public class MainVerticle extends AbstractVerticle {

  private Map<String, String> headers = new HashMap<>();
  private String okapiUrl;
  private final Logger logger = OkapiLogger.get();
  private OkapiClient cli;
  private JsonObject conf;
  private Vertx vertx;

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

  private void version(Handler<ExtendedAsyncResult<String>> fut) {
    cli.get("/_/version", res -> {
      if (res.failed()) {
        fut.handle(res);
      } else {
        try {
          SemVer okapiVer = new SemVer(res.result());
        } catch (IllegalArgumentException ex) {
          fut.handle(new Failure<>(INTERNAL,
            "Bad version response from Okapi " + res.result() + ": " + ex.getMessage()));
          return;
        }
        fut.handle(new Success<>(res.result()));
      }
    });
  }

  private void usage(Handler<ExtendedAsyncResult<String>> fut) {
    fut.handle(new Failure<>(USER, "No command given"));
  }

  private void start2(Handler<ExtendedAsyncResult<String>> fut) {
    cli = new OkapiClient(okapiUrl, vertx, headers);
    JsonArray ar = conf.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      usage(fut);
    } else {
      String a = ar.getString(0);
      if (a.equals("version")) {
        version(res -> {
          if (res.succeeded()) {
            System.out.println(res.result());
          }
          fut.handle(res);
        });
      } else {
        fut.handle(new Failure<>(USER, "Bad command " + a));
      }
    }
  }
}
