package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

public class CommandVersion implements Command {
  @Override
  public String getDescription() {
    return "version: show Okapi version";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.requestBuffer(HttpMethod.GET, "/_/version", Buffer.buffer(), handler);
  }

}
