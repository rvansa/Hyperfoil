package io.hyperfoil.hotrod;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.Scenario;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.api.PluginRunData;
import io.hyperfoil.core.impl.ConnectionStatsConsumer;
import io.hyperfoil.hotrod.api.HotRodRemoteCachePool;
import io.hyperfoil.hotrod.config.HotRodPluginConfig;
import io.hyperfoil.hotrod.connection.HotRodRemoteCachePoolImpl;
import io.netty.channel.EventLoop;
import io.vertx.core.Future;

public class HotRodRunData implements PluginRunData {

   private final HotRodPluginConfig plugin;
   private HotRodRemoteCachePool[] pool;

   public HotRodRunData(Benchmark benchmark, EventLoop[] executors, int agentId) {
      this.plugin = benchmark.plugin(HotRodPluginConfig.class);

      if (this.plugin.getClusters().size() > 1) {
         // uri: hotrod://site-a:11222 -> should run only on site-a
         // uri: hotrod://site-b:11222 -> should run only on site-b
         throw new BenchmarkDefinitionException("Hyperfoil HotRod plugin support only one uri.");
      }

      List<String> allCaches = new ArrayList<>();
      this.plugin.getClusters().values().forEach(hotRodCluster -> {
         for (String cacheName : hotRodCluster.getCaches()) {
            // validation
            if (allCaches.contains(cacheName)) {
               throw new BenchmarkDefinitionException(String.format("Duplicated cache: %s", cacheName));
            }
            allCaches.add(cacheName);
         }
      });

      this.pool = new HotRodRemoteCachePool[executors.length];
      for (int i = 0; i < executors.length; i++) {
         this.pool[i] = new HotRodRemoteCachePoolImpl(this.plugin.getClusters(), executors[i]);
      }
   }

   @Override
   public void initSession(Session session, int executorId, Scenario scenario, Clock clock) {
      HotRodRemoteCachePool pollById = this.pool[executorId];
      session.declareSingletonResource(HotRodRemoteCachePool.KEY, pollById);
   }

   @Override
   public void openConnections(Consumer<Future<Void>> promiseCollector) {
      for (HotRodRemoteCachePool p : this.pool) {
         p.start();
      }
   }

   @Override
   public void listConnections(Consumer<String> connectionCollector) {

   }

   @Override
   public void visitConnectionStats(ConnectionStatsConsumer consumer) {

   }

   @Override
   public void shutdown() {
      for (HotRodRemoteCachePool p : this.pool) {
         p.shutdown();
      }
   }
}
