package io.hyperfoil.hotrod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Properties;

import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.commons.test.TestResourceTracker;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.statistics.StatisticsSnapshot;
import io.hyperfoil.core.session.BaseScenarioTest;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class HotRodTest extends BaseScenarioTest {

   protected HotRodServer hotrodServers;

   @Before
   public void before(TestContext ctx) {
      super.before(ctx);
      TestResourceTracker.setThreadTestName("hyperfoil-HotRodTest");

      GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder();
      // TODO
      globalBuilder.globalState().persistentLocation("/tmp");
      globalBuilder.globalState().enabled(true);
      globalBuilder.security().authorization().disable();

      HotRodServerConfigurationBuilder serverBuilder = new HotRodServerConfigurationBuilder();
      serverBuilder.adminOperationsHandler(new EmbeddedServerAdminOperationHandler());

      ConfigurationBuilder cacheBuilder = new ConfigurationBuilder();

      EmbeddedCacheManager em = new DefaultCacheManager(globalBuilder.build());
      em.createCache("my-cache", cacheBuilder.build());
      hotrodServers = HotRodClientTestingUtil.startHotRodServer(em, serverBuilder);
   }

   @After
   public void after(TestContext ctx) {
      super.after(ctx);
      if (hotrodServers != null) {
         hotrodServers.stop();
      }
   }

   @Test
   public void test() {
      Benchmark benchmark = loadScenario("scenarios/HotRodTest.hf.yaml");
      Map<String, StatisticsSnapshot> stats = runScenario(benchmark);
      assertTrue(stats.get("example").requestCount > 0);
      assertEquals(0, stats.get("example").resetCount);
   }

   @Override
   protected Properties getScenarioReplacements() {
      Properties properties = new Properties();
      properties.put("hotrod://localhost:11222", "hotrod://localhost:" + hotrodServers.getPort());
      return properties;
   }
}
