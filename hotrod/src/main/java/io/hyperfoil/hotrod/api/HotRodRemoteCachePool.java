package io.hyperfoil.hotrod.api;

import org.infinispan.client.hotrod.RemoteCache;

import io.hyperfoil.api.session.Session;

public interface HotRodRemoteCachePool extends Session.Resource {

   Session.ResourceKey<HotRodRemoteCachePool> KEY = new Session.ResourceKey<>() {};

   static HotRodRemoteCachePool get(Session session) {
      return session.getResource(KEY);
   }

   void start();
   void shutdown();
   RemoteCache getRemoteCache(String cacheName);
}
