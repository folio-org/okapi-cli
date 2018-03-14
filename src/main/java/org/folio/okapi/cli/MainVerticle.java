package org.folio.okapi.cli;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.folio.okapi.common.OkapiClient;
import org.folio.okapi.common.OkapiLogger;
import org.folio.okapi.common.SemVer;

public class MainVerticle extends AbstractVerticle {

  private Map<String, String> headers = new HashMap<>();
  private String okapiUrl;
  private final Logger logger = OkapiLogger.get();
  private OkapiClient cli;
  private JsonObject conf;
  private String tenant;
  private Buffer buf;
  private PrintWriter out;
  private FileSystem fs;

  @Override
  public void init(Vertx vertx, Context context) {
    this.vertx = vertx;
    headers.put("Content-Type", "application/json");
    conf = context.config();
    okapiUrl = "http://localhost:9130";
    buf = Buffer.buffer();
    fs = vertx.fileSystem();
  }

  @Override
  public void start(Future<Void> fut) throws IOException {
    start2(res -> {
      if (res.failed()) {
        fut.handle(Future.failedFuture(res.cause()));
      } else {
        fut.complete();
      }
    });
  }

  int count = 0;

  private void version(Handler<AsyncResult<Void>> handler) {
    if (++count > 10) {
      try { // testing that we do not use stack with compose
        throw new NullPointerException();
      } catch (NullPointerException ex) {
        ex.printStackTrace();
      }
      handler.handle(Future.failedFuture("provoked failure"));
      return;
    }
    cli.get("/_/version", res -> {
      if (res.succeeded()) {
        try {
          SemVer okapiVer = new SemVer(res.result());
        } catch (IllegalArgumentException ex) {
          handler.handle(Future.failedFuture(ex));
          return;
        }
        buf.appendString(res.result());
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  private void usage(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.failedFuture("No command given"));
  }

  private void setTenant(String s, Handler<AsyncResult<Void>> handler) {
    if (s.startsWith("/")) {
      handler.handle(Future.failedFuture("bad tenant " + s));
    } else {
      tenant = s;
      headers.put("X-Okapi-Tenant", s);
      cli.setHeaders(headers);
      handler.handle(Future.succeededFuture());
    }
  }

  private void login(String username, String password,
    Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  private void requestBuffer(HttpMethod method, String path, Buffer b,
    Handler<AsyncResult<Void>> handler) {

    cli.request(method, path, b.toString(), res2 -> {
      if (res2.failed()) {
        handler.handle(Future.failedFuture(res2.cause()));
      } else {
        buf.appendString(res2.result());
        handler.handle(Future.succeededFuture());
      }
    });
  }

  private void requestFile(HttpMethod method, String path, String file,
    Handler<AsyncResult<Void>> handler) {

    if (file.startsWith("{")) {
      requestBuffer(method, path, Buffer.buffer(file), handler);
    } else {
      fs.readFile(path, res -> {
        if (res.failed()) {
          handler.handle(Future.failedFuture(res.cause()));
        } else {
          requestBuffer(method, path, res.result(), handler);
        }
      });
    }

  }

  private void post(String path, String file, Handler<AsyncResult<Void>> handler) {
    requestFile(HttpMethod.POST, path, file, handler);
  }

  private void put(String path, String file, Handler<AsyncResult<Void>> handler) {
    requestFile(HttpMethod.PUT, path, file, handler);
  }

  private void get(String path, Handler<AsyncResult<Void>> handler) {
    requestBuffer(HttpMethod.GET, path, Buffer.buffer(), handler);
  }

  private void delete(String path, Handler<AsyncResult<Void>> handler) {
    requestBuffer(HttpMethod.DELETE, path, Buffer.buffer(), handler);
  }

  private void start2(Handler<AsyncResult<Void>> handler) {
    cli = new OkapiClient(okapiUrl, vertx, headers);

    JsonArray ar = conf.getJsonArray("args");
    if (ar == null || ar.isEmpty()) {
      usage(handler);
    } else {
      Future<Void> futF = Future.future();

      futF.setHandler(h -> {
        if (h.succeeded()) {
          String fname = conf.getString("file");
          if (fname != null) {
            try {
              out = new PrintWriter(fname);
              out.print(buf.toString());
              out.close();
            } catch (IOException ex) {
              handler.handle(Future.failedFuture(h.cause().getMessage()));
            }
          } else {
            if (buf.length() > 0) {
              System.out.println(buf.toString());
            }
          }
          handler.handle(Future.succeededFuture());
        } else {
          handler.handle(Future.failedFuture(h.cause().getMessage()));
        }
      });

      Future<Void> fut1 = Future.future();
      fut1.complete();
      for (int i = 0; i < ar.size(); i++) {
        String a = ar.getString(i);
        Future<Void> fut2 = Future.future();
        if (a.startsWith("--okapiurl=")) {
          fut1.compose(v -> {
            okapiUrl = a.substring(11);
            if (cli != null) {
              cli.close();
            }
            cli = new OkapiClient(okapiUrl, vertx, headers);
            fut2.complete();
          }, futF);
        } else if (a.startsWith("--tenant=")) {
          fut1.compose(v -> {
            setTenant(a.substring(9), fut2.completer());
          }, futF);
        } else if (a.equals("login")) {
          final String username = ar.getString(++i);
          final String password = ar.getString(++i);
          fut1.compose(v -> {
            login(username, password, fut2.completer());
          }, futF);
        } else if (a.equals("post")) {
          final String path = ar.getString(++i);
          final String file = ar.getString(++i);
          fut1.compose(v -> {
            post(path, file, fut2.completer());
          }, futF);
        } else if (a.equals("put")) {
          final String path = ar.getString(++i);
          final String file = ar.getString(++i);
          fut1.compose(v -> {
            put(path, file, fut2.completer());
          }, futF);
        } else if (a.equals("get")) {
          final String path = ar.getString(++i);
          fut1.compose(v -> {
            get(path, fut2.completer());
          }, futF);
        } else if (a.equals("delete")) {
          final String path = ar.getString(++i);
          fut1.compose(v -> {
            delete(path, fut2.completer());
          }, futF);
        } else if (a.equals("version")) {
          fut1.compose(v -> {
            version(fut2.completer());
          }, futF);
        } else {
          fut1.compose(v -> {
            fut2.fail("Bad command: " + a);
          }, futF);
        }
        fut1 = fut2;
      }
      fut1.compose(v -> {
        futF.complete();
      }, futF);
    }
 }
}
