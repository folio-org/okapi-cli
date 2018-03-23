package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionOkapiUrl implements Command {

  @Override
  public String getDescription() {
    return "--okapi-url=<url>: set Okapi URL";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    String url = ar.getString(offset);
    v.cliConfig.put("okapiUrl", url);
    v.cli.setOkapiUrl(url);
    handler.handle(Future.succeededFuture());
  }

}
