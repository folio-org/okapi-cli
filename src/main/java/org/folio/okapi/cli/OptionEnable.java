package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionEnable implements Command {

  @Override
  public String getDescription() {
    return "--enable=<module>: Enable module (for install)";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.installArrayAdd(ar.getString(offset), "action", "enable");
    handler.handle(Future.succeededFuture());
  }

}
