package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionSimulate implements Command {

  @Override
  public String getDescription() {
    return "--simulate=<boolean>: whether to simulate install/upgrade calls";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.cliConfig.put("simulate", ar.getString(offset));
    handler.handle(Future.succeededFuture());
  }

}
