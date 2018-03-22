package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

public class CommandPost implements Command {

  @Override
  public String getDescription() {
    return "post <path> <body>: HTTP post";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    v.requestFile(HttpMethod.POST, ar.getString(offset), ar.getString(offset + 1), handler);
  }
}
