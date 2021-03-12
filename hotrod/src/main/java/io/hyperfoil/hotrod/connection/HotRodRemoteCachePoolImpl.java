package io.hyperfoil.hotrod.connection;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.transport.netty.ChannelFactory;

import io.hyperfoil.hotrod.api.HotRodRemoteCachePool;
import io.hyperfoil.hotrod.config.HotRodCluster;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;

public class HotRodRemoteCachePoolImpl implements HotRodRemoteCachePool {

   private final Map<String, HotRodCluster> clusters;
   private final EventLoop eventLoop;

   private Map<String, RemoteCacheManager> remoteCacheManagers;
   private Map<String, RemoteCache> remoteCaches;

   public HotRodRemoteCachePoolImpl(Map<String, HotRodCluster> clusters, EventLoop eventLoop) {
      this.clusters = clusters;
      this.eventLoop = eventLoop;

      this.remoteCacheManagers = new HashMap<>();
      this.remoteCaches = new HashMap<>();
   }

   @Override
   public void start() {
      for (String uri : clusters.keySet()) {
         Properties properties = new Properties();
         properties.setProperty("infinispan.client.hotrod.default_executor_factory.pool_size", "1");
         ConfigurationBuilder cb = new ConfigurationBuilder().uri(uri);
         cb.withProperties(properties);
         cb.asyncExecutorFactory().factory(p -> eventLoop);
         RemoteCacheManager remoteCacheManager = new RemoteCacheManager(cb.build());
         this.remoteCacheManagers.put(uri, remoteCacheManager);
         validateEventLoop(remoteCacheManager);
         clusters.get(uri).getCaches().forEach(cacheName -> remoteCaches.put(cacheName, remoteCacheManager.getCache(cacheName)));
      }
   }

   private void validateEventLoop(RemoteCacheManager remoteCacheManager) {
      ChannelFactory channelFactory = remoteCacheManager.getChannelFactory();
      try {
         Field eventLoopField = ChannelFactory.class.getDeclaredField("eventLoopGroup");
         eventLoopField.setAccessible(true);
         EventLoopGroup actualEventLoop = (EventLoopGroup) eventLoopField.get(channelFactory);
         if (actualEventLoop != eventLoop) {
            throw new IllegalStateException("Event loop was not injected correctly. This is a classpath issue.");
         }
      } catch (NoSuchFieldException | IllegalAccessException e) {
         throw new IllegalStateException(e);
      }
      ExecutorService asyncExecutorService = remoteCacheManager.getAsyncExecutorService();
      if (asyncExecutorService != eventLoop) {
         throw new IllegalStateException("Event loop was not configured correctly.");
      }
   }

   @Override
   public void shutdown() {
      this.remoteCacheManagers.values().forEach(rcm -> rcm.stop());
   }

   @Override
   public RemoteCache getRemoteCache(String cacheName) {
      return this.remoteCaches.get(cacheName);
   }
}
