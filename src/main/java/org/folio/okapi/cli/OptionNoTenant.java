package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionNoTenant implements Command {

  @Override
  public String getDescription() {
    return "--no-tenant: clear tenant for administrative calls";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.tenant = null;
    handler.handle(Future.succeededFuture());
  }
}
