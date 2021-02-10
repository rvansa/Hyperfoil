package io.hyperfoil.hotrod.steps;

import io.hyperfoil.api.config.BuilderBase;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.function.SerializableFunction;

@FunctionalInterface
public interface HotRodCacheNameBuilder extends BuilderBase<HotRodCacheNameBuilder> {
   SerializableFunction<Session, String> build();
   class Provided implements SerializableFunction<Session, String> {
      private final String cacheName;

      public Provided(String cacheName) {
         this.cacheName = cacheName;
      }

      @Override
      public String apply(Session o) {
         return cacheName;
      }
   }
}
