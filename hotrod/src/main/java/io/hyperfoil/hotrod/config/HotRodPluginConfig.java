package io.hyperfoil.hotrod.config;

import java.util.List;

import io.hyperfoil.api.config.PluginConfig;

public class HotRodPluginConfig implements PluginConfig {
   private final List<HotRod> hotRodList;

   public HotRodPluginConfig(List<HotRod> hotRodList) {
      this.hotRodList = hotRodList;
   }

   public List<HotRod> hotRod() {
      return hotRodList;
   }
}
