package io.hyperfoil.hotrod.config;

import java.util.List;

import io.hyperfoil.api.config.BenchmarkBuilder;
import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.PluginBuilder;
import io.hyperfoil.api.config.PluginConfig;

public class HotRodClusterPluginBuilder extends PluginBuilder<HotRodErgonomics> {

   private HotRodClusterBuilder defaultHotRod;

   public HotRodClusterPluginBuilder(BenchmarkBuilder parent) {
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

   public HotRodClusterBuilder hotRod(List<HotRodCluster> hotRods) {
      HotRodClusterBuilder builder = new HotRodClusterBuilder().hotRods(hotRods);
      return builder;
   }

   public HotRodClusterBuilder hotRod() {
      if (defaultHotRod == null) {
         defaultHotRod = new HotRodClusterBuilder();
      }
      return defaultHotRod;
   }
}
