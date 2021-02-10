package io.hyperfoil.hotrod.config;

import java.io.Serializable;

public class HotRod implements Serializable {

   private final String uri;
   private final String cacheName;

   public HotRod(String uri, String cacheName) {
      this.uri = uri;
      this.cacheName = cacheName;
   }

   public String getUri() {
      return uri;
   }

   public String getCacheName() {
      return cacheName;
   }
}
