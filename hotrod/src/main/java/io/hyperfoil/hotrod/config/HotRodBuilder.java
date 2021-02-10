package io.hyperfoil.hotrod.config;

import org.infinispan.client.hotrod.impl.HotRodURI;

import io.hyperfoil.api.config.Rewritable;

public class HotRodBuilder implements Rewritable<HotRodBuilder> {

   private String uri;
   private String cacheName;

   HotRodBuilder() {
   }

   @Override
   public void readFrom(HotRodBuilder other) {

   }

   // https://infinispan.org/blog/2020/05/26/hotrod-uri
   public HotRodBuilder uri(String uri) {
      // used to validate the uri
      HotRodURI.create(uri);

      // set
      this.uri = uri;
      return this;
   }

   // if needed, we can have/allow multiple caches using `,` as a split
   public HotRodBuilder cacheName(String cacheName) {
      this.cacheName = cacheName;
      return this;
   }

   public HotRod build() {
      HotRod hotRod = new HotRod(this.uri, this.cacheName);
      return hotRod;
   }

   public String getCacheName() {
      return cacheName;
   }
}
