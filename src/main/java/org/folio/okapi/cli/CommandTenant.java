package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.folio.okapi.common.XOkapiHeaders;

public class CommandTenant implements Command {

  @Override
  public String getName() {
    return "tenant";
  }

  @Override
  public int getNoArgs() {
    return 1;
  }

  @Override
  public String getDescription() {
    return "tenant <tenant> : act as tenant";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    final String tenant = ar.getString(offset);
    v.headers.put(XOkapiHeaders.TENANT, tenant);
    handler.handle(Future.succeededFuture());
  }

}
