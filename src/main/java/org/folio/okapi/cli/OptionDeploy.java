package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionDeploy implements Command {

  @Override
  public String getDescription() {
    return "--deploy=<boolean>: whether to deploy via install/update";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.cliConfig.put("deploy", ar.getString(offset));
    handler.handle(Future.succeededFuture());
  }

}
