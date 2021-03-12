package io.hyperfoil.hotrod.config;

import java.util.Map;

import io.hyperfoil.api.config.PluginConfig;

public class HotRodPluginConfig implements PluginConfig {
   private final Map<String, HotRodCluster> clusters;

   public HotRodPluginConfig(Map<String, HotRodCluster> clusters) {
      this.clusters = clusters;
   }

   public Map<String, HotRodCluster> getClusters() {
      return clusters;
   }
}
