package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.okapi.common.XOkapiHeaders;

public class CommandLogin implements Command {

  @Override
  public String getName() {
    return "login";
  }

  @Override
  public int getNoArgs() {
    return 3;
  }

  @Override
  public String getDescription() {
    return "login <tenant> <username> <password> : log on via /authn/login endpoint";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int i, Handler<AsyncResult<Void>> handler) {
    final String tenant = ar.getString(i);
    final String username = ar.getString(i + 1);
    final String password = ar.getString(i + 2);

    v.headers.put(XOkapiHeaders.TENANT, tenant);
    JsonObject j = new JsonObject();
    j.put("username", username);
    if (password != null) {
      j.put("password", password);
    }
    v.requestBuffer(HttpMethod.POST, "/authn/login", j.toBuffer(), res -> {
      v.headers.remove(XOkapiHeaders.TENANT);
      handler.handle(res);
    });
  }
}
