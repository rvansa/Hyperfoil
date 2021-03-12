package io.hyperfoil.hotrod.config;

import io.hyperfoil.api.config.BenchmarkBuilder;
import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.PluginBuilder;
import io.hyperfoil.api.config.PluginConfig;

public class HotRodPluginBuilder extends PluginBuilder<HotRodErgonomics> {

   private HotRodClusterBuilder defaultHotRod;

   public HotRodPluginBuilder(BenchmarkBuilder parent) {
      super(parent);
   }

   @Override
   public HotRodErgonomics ergonomics() {
      return null;
   }

   @Override
   public void prepareBuild() {
      if (defaultHotRod == null) {
         throw new BenchmarkDefinitionException("No HotRod target set!");
      }
   }

   @Override
   public PluginConfig build() {
      return new HotRodPluginConfig(defaultHotRod.build());
   }

   public HotRodClusterBuilder hotRod() {
      if (defaultHotRod == null) {
         defaultHotRod = new HotRodClusterBuilder();
      }
      return defaultHotRod;
   }
}
