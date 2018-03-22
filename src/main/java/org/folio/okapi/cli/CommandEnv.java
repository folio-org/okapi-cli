package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CommandEnv implements Command {

  @Override
  public String getName() {
    return "env";
  }

  @Override
  public int getNoArgs() {
    return 1;
  }

  @Override
  public String getDescription() {
    return "env <var>=<value> : sets <var> to <value> ; or unsets if <value> is empty";
  }

  @Override
  public void run(MainVerticle v, JsonArray ar, int offset, Handler<AsyncResult<Void>> handler) {
    String e = ar.getString(offset);
    int idx = e.indexOf('=');
    if (idx != -1) {
      String name = e.substring(0, idx);
      String value = e.substring(idx + 1);
      if (value.isEmpty()) {
        v.requestBuffer(HttpMethod.DELETE, "/_/env/" + name, Buffer.buffer(), handler);
      } else {
        JsonObject j = new JsonObject();
        j.put("name", name);
        j.put("value", value);
        v.requestBuffer(HttpMethod.POST, "/_/env", j.toBuffer(), handler);
      }
    } else {
      v.requestBuffer(HttpMethod.GET, "/_/env/" + e, Buffer.buffer(), handler);
    }
  }
}
