package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OptionDisable implements Command {

  @Override
  public String getDescription() {
    return "--disable=<module>: Enable module (for install)";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    JsonObject j = new JsonObject();
    j.put("action", "disable");
    j.put("id", ar.getString(offset));
    v.installArray.add(j);
    handler.handle(Future.succeededFuture());
  }
}
