package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

public class CommandGet implements Command {

  @Override
  public String getName() {
    return "get";
  }

  @Override
  public int getNoArgs() {
    return 1;
  }

  @Override
  public String getDescription() {
    return "get <path> : HTTP GET";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.requestBuffer(HttpMethod.GET, ar.getString(offset), Buffer.buffer(), handler);
  }

}
