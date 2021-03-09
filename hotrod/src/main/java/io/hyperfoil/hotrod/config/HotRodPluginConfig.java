package io.hyperfoil.hotrod.config;

import io.hyperfoil.api.config.PluginConfig;

public class HotRodPluginConfig implements PluginConfig {
   private final HotRod hotRod;

   public HotRodPluginConfig(HotRod hotRod) {
      this.hotRod = hotRod;
   }

   public HotRod getHotRod() {
      return hotRod;
   }
}
