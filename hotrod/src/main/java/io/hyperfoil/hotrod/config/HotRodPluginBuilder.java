package io.hyperfoil.hotrod.config;

import java.util.ArrayList;
import java.util.List;

import io.hyperfoil.api.config.BenchmarkBuilder;
import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.PluginBuilder;
import io.hyperfoil.api.config.PluginConfig;

public class HotRodPluginBuilder extends PluginBuilder<HotRodErgonomics> {

   private HotRodBuilder defaultHotRod;
   private List<HotRodBuilder> hotRodBuilderList = new ArrayList<>();

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
         if (hotRodBuilderList.isEmpty()) {
            // may be removed in the future when we define more than HotRod connections
            throw new BenchmarkDefinitionException("No default HotRod target set!");
         } else if (hotRodBuilderList.size() == 1) {
            defaultHotRod = hotRodBuilderList.iterator().next();
         }
      } else {
         hotRodBuilderList.add(defaultHotRod);
      }
   }

   @Override
   public PluginConfig build() {
      List<HotRod> hotRodList = new ArrayList<>();
      hotRodBuilderList.forEach(hrb -> hotRodList.add(hrb.build()));
      return new HotRodPluginConfig(hotRodList);
   }

   public HotRodBuilder hotRod(String uri) {
      HotRodBuilder builder = new HotRodBuilder().uri(uri);
      hotRodBuilderList.add(builder);
      return builder;
   }

   public HotRodBuilder decoupledHotRod() {
      return new HotRodBuilder();
   }

   public void addHotRod(HotRodBuilder hotRod) {
      hotRodBuilderList.add(hotRod);
   }

   public HotRodBuilder hotRod() {
      if (defaultHotRod == null) {
         defaultHotRod = new HotRodBuilder();
      }
      return defaultHotRod;
   }
}
