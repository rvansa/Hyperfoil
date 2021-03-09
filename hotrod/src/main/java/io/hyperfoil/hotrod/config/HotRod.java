package io.hyperfoil.hotrod.config;

import java.io.Serializable;
import java.util.List;

import org.infinispan.client.hotrod.impl.HotRodURI;

public class HotRod implements Serializable {

   private final List<HotRodUri> uris;

   public HotRod(List<HotRodUri> uris) {
      this.uris = uris;
   }

   public List<HotRodUri> getUris() {
      return this.uris;
   }

   public static class HotRodUri implements Serializable {

      // https://infinispan.org/blog/2020/05/26/hotrod-uri
      private final String uri;
      private final List<String> caches;

      public HotRodUri(String uri, List<String> caches) {
         // used to validate the uri
         HotRodURI.create(uri);

         this.uri = uri;
         this.caches = caches;
      }

      public String getUri() {
         return this.uri;
      }

      public List<String> getCaches() {
         return this.caches;
      }
   }
}
