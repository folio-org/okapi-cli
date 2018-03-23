package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class OptionPullUrl implements Command {

  @Override
  public String getDescription() {
    return "--pull-url=<urls>: URLs to use for pull";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    final String[] urls = ar.getString(offset).split(",");
    JsonArray pullUrls = new JsonArray();
    for (String url : urls) {
      pullUrls.add(url);
    }
    v.cliConfig.put("pullUrls", pullUrls);
    handler.handle(Future.succeededFuture());
  }

}
