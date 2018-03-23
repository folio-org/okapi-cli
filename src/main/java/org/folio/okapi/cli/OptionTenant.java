package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionTenant implements Command {

  @Override
  public String getDescription() {
    return "--tenant=<tenant>: tenant to use for administrative calls";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.tenant = ar.getString(offset);
    handler.handle(Future.succeededFuture());
  }

}
