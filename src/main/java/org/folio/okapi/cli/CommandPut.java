package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

public class CommandPut implements Command {

  @Override
  public String getName() {
    return "put";
  }

  @Override
  public int getNoArgs() {
    return 2;
  }

  @Override
  public String getDescription() {
    return "put <path> <body>: HTTP PUT";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.requestFile(HttpMethod.PUT, ar.getString(offset), ar.getString(offset + 1), handler);
  }

}
