package org.folio.okapi.cli;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@java.lang.SuppressWarnings({"squid:S1192"})
@RunWith(VertxUnitRunner.class)
public class OkapiCliTest {

  private static final Logger logger = LogManager.getLogger(OkapiCliTest.class);

  private Vertx vertx;
  private final int port1 = 9230;
  private FileSystem fs;

  public OkapiCliTest() {
  }

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();

    DeploymentOptions opt = new DeploymentOptions()
        .setConfig(new JsonObject().put("port", Integer.toString(port1)));

    fs = vertx.fileSystem();
    fs.delete("okapi-cli-config.txt")
    .otherwiseEmpty()
    .compose(x -> vertx.deployVerticle(org.folio.okapi.MainVerticle.class, opt))
    .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  private void runIt(JsonArray ar, Handler<AsyncResult<String>> handler) {
    DeploymentOptions opt = new DeploymentOptions();
    JsonObject j = new JsonObject();
    j.put("okapi-cli-output-file", "okapi-cli-output.txt");
    j.put("okapi-cli-config-file", "okapi-cli-config.txt");
    j.put("args", ar);
    opt.setConfig(j);

    vertx.deployVerticle(new MainVerticle(), opt, res -> {
      if (res.failed()) {
        logger.info("cause : " + res.cause().getMessage());
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        vertx.undeploy(res.result(), res2 -> {
          if (res2.failed()) {
            handler.handle(Future.failedFuture(res2.cause()));
          } else {
            File f = new File(j.getString("okapi-cli-output-file"));
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
    WebClient.create(vertx).get(port1, "localhost", "/_/version").send()
    .onComplete(context.asyncAssertSuccess(buffer -> {
        context.assertNotNull(buffer.toString());
    }));
  }

  @Test
  public void test1(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
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

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
    ar.add("tenant");
    ar.add("supertenant");
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

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
    ar.add("tenant");
    ar.add("foo");
    ar.add("version");
    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void test4(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1 + 1));
    ar.add("version");
    runIt(ar, res -> {
      context.assertFalse(res.succeeded());
      async.complete();
    });
  }

  @Test
  public void testPostMd(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod);
    ar.add("get");
    ar.add("/_/proxy/modules");
    ar.add("delete");
    ar.add("/_/proxy/modules/mod-1.0.0");
    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      context.assertTrue(res.result().contains("mod-1.0.0"));

      ar.clear();
      ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
      ar.add("get");
      ar.add("/_/proxy/modules");
      runIt(ar, res2 -> {
        context.assertTrue(res2.succeeded());
        context.assertFalse(res2.result().contains("mod-1.0.0"));
        async.complete();
      });
    });
  }

  @Test
  public void testInstall1(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod);

    ar.add("post");
    ar.add("/_/proxy/tenants");
    ar.add("{\"id\": \"testlib\"}");

    ar.add("--deploy=false");
    ar.add("--tenant=testlib");
    ar.add("--enable=mod-1.0.0");
    ar.add("install");

    ar.add("--disable=mod-1.0.0");
    ar.add("install");

    ar.add("--no-tenant");

    ar.add("delete");
    ar.add("/_/proxy/modules/mod-1.0.0");

    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });
  }

  @Test
  public void testInstall2(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod);

    ar.add("post");
    ar.add("/_/proxy/tenants");
    ar.add("{\"id\": \"testlib\"}");

    // fails because modules does not exist
    ar.add("--tenant=testlib");
    ar.add("--enable=mod-2.0.0");
    ar.add("--simulate=true");
    ar.add("install");

    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testInstallNoTenant(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    // fails because no tenant is selected
    ar.add("install");

    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testInstallNoModules(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    // fails because no tenant is selected
    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod);

    ar.add("post");
    ar.add("/_/proxy/tenants");
    ar.add("{\"id\": \"testlib\"}");

    ar.add("--tenant=testlib");
    // fails because no modules is selected
    ar.add("install");

    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testUpgrade1(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod1 = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod1);

    ar.add("available");

    ar.add("post");
    ar.add("/_/proxy/tenants");
    ar.add("{\"id\": \"testlib\"}");

    ar.add("--tenant=testlib");
    ar.add("--enable=mod");
    ar.add("install");

    final String mod2 = "{\"id\": \"mod-1.2.0\", \"provides\": [], \"requires\":[]}";
    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod2);

    ar.add("upgrade");
    ar.add("enabled");

    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });
  }

  @Test
  public void testUpgradeNoTenant(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    // fails because no tenant is selected
    ar.add("upgrade");

    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testPut(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    ar.add("post");
    ar.add("/_/proxy/tenants");
    ar.add("{\"id\": \"testlib\"}");

    ar.add("put");
    ar.add("/_/proxy/tenants/testlib");

    Buffer buf = Buffer.buffer("{\"id\": \"testlib\", \"name\" : \"Test Library\"}");
    fs.writeFileBlocking("okapi-cli-input.txt", buf);
    ar.add("@okapi-cli-input.txt");

    ar.add("get");
    ar.add("/_/proxy/tenants/testlib");

    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      context.assertTrue(res.result().contains("Test Library"));

      async.complete();
    });
  }

  @Test
  public void testHelp(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("help");
    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });
  }

  @Test
  public void testLogon(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
    ar.add("login");
    ar.add("testlib");
    ar.add("testuser");
    ar.add("testpass");
    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testLogout(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
    ar.add("tenant");
    ar.add("testlib");
    ar.add("logout");
    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });
  }

  @Test
  public void testBadFile(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    ar.add("post");
    ar.add("/_/proxy/tenants");
    ar.add("@does_not_exist");

    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testNoCommand(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testMissingArgs(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("get");
    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testBadCommand(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("foocommand");
    runIt(ar, res -> {
      context.assertTrue(res.failed());
      async.complete();
    });
  }

  @Test
  public void testEnv(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));
    ar.add("env");
    ar.add("v1=n1");
    ar.add("env");
    ar.add("v2=n2");
    ar.add("env");
    ar.add("v1");
    ar.add("env");
    ar.add("v1=");
    ar.add("env");
    ar.add("v2");
    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      logger.info(res.result());
      async.complete();
    });
  }

  @Test
  public void testPull(TestContext context) {
    Async async = context.async();
    JsonArray ar = new JsonArray();
    final String mod = "{\"id\": \"mod-1.0.0\", \"provides\": [], \"requires\":[]}";

    ar.add("--okapi-url=http://localhost:" + Integer.toString(port1));

    ar.add("post");
    ar.add("/_/proxy/modules");
    ar.add(mod);

    ar.add("--pull-url=http://localhost:" + Integer.toString(port1));
    ar.add("pull");

    runIt(ar, res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });

  }

  @Test
  public void testMain(TestContext context) {
    Async async = context.async();
    String[] args = {"--okapi-url=http://localhost:" + Integer.toString(port1), "version"};
    Main.deploy(args, res -> {
      context.assertTrue(res.succeeded());
      async.complete();
    });
  }
}
