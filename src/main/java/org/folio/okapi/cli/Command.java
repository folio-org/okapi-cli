package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface Command {

  String getDescription();

  void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler);

  default Future<Void> run(MainVerticle v, JsonArray ar, int offset) {
    return Future.future(promise -> run(v, ar, offset, promise));
  }
}
