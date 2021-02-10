package io.hyperfoil.core.metric;

import io.hyperfoil.function.SerializableBiFunction;

public class ProvidedMetricSelector implements SerializableBiFunction<String, String, String> {
   private String name;

   public ProvidedMetricSelector() {
   }

   public ProvidedMetricSelector(String name) {
      this.name = name;
   }

   @Override
   public String apply(String authority, String path) {
      return name;
   }
}
