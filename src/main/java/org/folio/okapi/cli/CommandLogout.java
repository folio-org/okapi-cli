package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.folio.okapi.common.XOkapiHeaders;

public class CommandLogout implements Command {

  @Override
  public String getName() {
    return "logout";
  }

  @Override
  public int getNoArgs() {
    return 0;
  }

  @Override
  public String getDescription() {
    return "logout: removes token and tenant from session";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.headers.remove(XOkapiHeaders.TENANT);
    v.headers.remove(XOkapiHeaders.TOKEN);
    handler.handle(Future.succeededFuture());
  }

}
