package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CommandPull implements Command {
  @Override
  public String getDescription() {
    return "pull: pull module descriptors from remote repository";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    JsonObject j = new JsonObject();
    j.put("urls", v.cliConfig.getJsonArray("pullUrls"));
    v.requestBuffer(HttpMethod.POST, "/_/proxy/pull/modules", j.toBuffer(), handler);
  }

}
