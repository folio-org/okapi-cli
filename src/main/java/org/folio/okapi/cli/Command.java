package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface Command {

  String getDescription();

  void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler);
}
