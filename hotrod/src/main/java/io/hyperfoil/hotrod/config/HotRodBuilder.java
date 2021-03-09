package io.hyperfoil.hotrod.config;

import java.util.ArrayList;
import java.util.List;

import io.hyperfoil.api.config.Rewritable;

public class HotRodBuilder implements Rewritable<HotRodBuilder> {

   private List<HotRod.HotRodUri> uris;

   HotRodBuilder() {
      this.uris = new ArrayList<>();
   }

   @Override
   public void readFrom(HotRodBuilder other) {

   }

   public HotRodBuilder uris(List<HotRod.HotRodUri> uris) {
      this.uris.addAll(uris);
      return this;
   }

   public HotRodBuilder uri(HotRod.HotRodUri uri) {
      this.uris.add(uri);
      return this;
   }

   public HotRod build() {
      HotRod hotRod = new HotRod(this.uris);
      return hotRod;
   }
}
