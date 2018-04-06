package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import org.folio.okapi.common.OkapiLogger;

public class Main {

  private Main() {
    throw new IllegalAccessError("Main");
  }

  public static void main(String[] args) {
    Logger logger = OkapiLogger.get();
    deploy(args, dep -> {
      if (dep.failed()) {
        logger.fatal(dep.cause().getMessage());
        System.exit(1);
      } else {
        System.exit(0);
      }
    });
  }

  public static void deploy(String[] args, Handler<AsyncResult<String>> fut) {
    JsonArray ar = new JsonArray();
    for (int i = 0; i < args.length; i++) {
      ar.add(args[i]);
    }
    Vertx vertx = Vertx.vertx();
    JsonObject conf = new JsonObject();
    conf.put("args", ar);
    DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    vertx.deployVerticle(new MainVerticle(), opt, fut);
  }
}
