package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

public class CommandInstall implements Command {
  @Override
  public String getDescription() {
    return "install: changes modules enabled by tenant";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    if (v.tenant == null) {
      handler.handle(Future.failedFuture("Tenant not set (use --tenant=val)"));
    } else if (v.installArray.isEmpty()) {
      handler.handle(Future.failedFuture("Nothing to install"));
    } else {
      Buffer b = v.installArray.toBuffer();
      v.installArray.clear();
      v.requestBuffer(HttpMethod.POST, "/_/proxy/tenants/" + v.tenant
        + "/install?deploy=" + v.cliConfig.getString("deploy"),
        b, handler);
    }
  }
}
