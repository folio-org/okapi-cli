package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

public class CommandUpgrade implements Command {

  @Override
  public String getDescription() {
    return "upgrade: upgrade modules for current tenant";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    if (v.tenant == null) {
      handler.handle(Future.failedFuture("Tenant not set (use --tenant=val)"));
    } else {
      v.requestBuffer(HttpMethod.POST, "/_/proxy/tenants/" + v.tenant
        + "/upgrade?deploy=" + v.cliConfig.getString("deploy"),
        Buffer.buffer(), handler);
    }
  }  
}
