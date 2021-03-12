package io.hyperfoil.hotrod.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.hyperfoil.api.config.Rewritable;

public class HotRodClusterBuilder implements Rewritable<HotRodClusterBuilder> {

   private List<HotRodCluster> hotRods;

   HotRodClusterBuilder() {
      this.hotRods = new ArrayList<>();
   }

   @Override
   public void readFrom(HotRodClusterBuilder other) {

   }

   public HotRodClusterBuilder hotRods(List<HotRodCluster> hotRods) {
      this.hotRods.addAll(hotRods);
      return this;
   }

   public HotRodClusterBuilder hotRod(HotRodCluster hotRod) {
      this.hotRods.add(hotRod);
      return this;
   }

   public Map<String, HotRodCluster> build() {
      Map<String, HotRodCluster> clusters = new HashMap<>();
      this.hotRods.forEach(hotRod -> clusters.put(hotRod.getUri(), hotRod));
      return clusters;
   }
}
