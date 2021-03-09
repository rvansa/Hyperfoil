package io.hyperfoil.hotrod.config;

import java.util.List;

import io.hyperfoil.api.config.Rewritable;

public class HotRodUriBuilder implements Rewritable<HotRodUriBuilder> {

   private List<String> caches;

   HotRodUriBuilder() {
   }

   @Override
   public void readFrom(HotRodUriBuilder other) {

   }

   public HotRodUriBuilder caches(List<String> caches) {
      this.caches = caches;
      return this;
   }

   public HotRod.HotRodUri build() {
      HotRod.HotRodUri hotRodUri = new HotRod.HotRodUri("", this.caches);
      return hotRodUri;
   }
}