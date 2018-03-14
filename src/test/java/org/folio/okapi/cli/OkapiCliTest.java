package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import org.folio.okapi.common.OkapiLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@java.lang.SuppressWarnings({"squid:S1192"})
@RunWith(VertxUnitRunner.class)
public class OkapiCliTest {

  private final Logger logger = OkapiLogger.get();
  private Vertx vertx;
  private static final String LS = System.lineSeparator();
  private final int port1 = 9230;
  private String vert1;

  public OkapiCliTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
    Async async = context.async();

    DeploymentOptions opt = new DeploymentOptions()
      .setConfig(new JsonObject().put("port", Integer.toString(port1)));
    vertx.deployVerticle(org.folio.okapi.MainVerticle.class.getName(), opt, res -> {
      if (res.failed()) {
        context.fail(res.cause());
      } else {
        vert1 = res.result();
        async.complete();
      }
    });
  }

  @After
  public void tearDown(TestContext context) {
    td(context, context.async());
  }

  private void td(TestContext context, Async async) {
    if (vert1 != null) {
      vertx.undeploy(vert1, res -> {
        vert1 = null;
        td(context, async);
      });
    } else {
      vertx.close(x -> {
        async.complete();
      });
    }
  }

  private void runIt(JsonArray ar, Handler<AsyncResult<String>> handler) {
    DeploymentOptions opt = new DeploymentOptions();
    JsonObject j = new JsonObject();
    j.put("file", "OkapiCliTest.txt");
    j.put("args", ar);
    opt.setConfig(j);

    vertx.deployVerticle(new MainVerticle(), opt, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        vertx.undeploy(res.result(), res2 -> {
          if (res2.failed()) {
          } else {
            File f = new File("OkapiCliTest.txt");
            try {
              byte[] bytes = Files.readAllBytes(f.toPath());
              handler.handle(Future.succeededFuture(new String(bytes)));
            } catch (FileNotFoundException e) {
              handler.handle(Future.failedFuture(e));
            } catch (IOException e) {
              handler.handle(Future.failedFuture(e));
            }
          }
        });
      }
    });
  }

  @Test
  public void test0(TestContext context) {
    Async async = context.async();

    HttpClient cli = vertx.createHttpClient();
    Buffer body = Buffer.buffer();
    HttpClientRequest req = cli.get(port1, "localhost", "/_/version", res -> {
      res.handler(body::appendBuffer);
      res.endHandler(x -> {
        logger.info("buffer=" + body.toString());
        context.assertNotNull(body.toString());
        async.complete();
      });
      res.exceptionHandler(x -> {
        context.fail();
        async.complete();
      });
    });
    req.exceptionHandler(res -> {
      context.fail();
      async.complete();
    });
    req.end();
  }

  @Test
  public void test1(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapiurl=http://localhost:" + Integer.toString(port1));
    ar.add("version");
    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      context.assertNotNull(res.result());
      context.assertTrue(res.result().length() > 4);
      async.complete();
    });
  }

  @Test
  public void test2(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapiurl=http://localhost:" + Integer.toString(port1));
    ar.add("--tenant=foo");
    ar.add("version");
    ar.add("version");
    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      context.assertTrue(res.result().length() > 8);
      async.complete();
    });
  }

  @Test
  public void test3(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapiurl=http://localhost:" + Integer.toString(port1 + 1));
    ar.add("version");
    runIt(ar, res -> {
      context.assertFalse(res.succeeded());
      async.complete();
    });
  }

}