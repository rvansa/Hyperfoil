package io.hyperfoil.hotrod.steps;

import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.hotrod.resource.HotRodResource;
import io.hyperfoil.hotrod.resource.HotRodResourceKey;

public class HotRodResponseStep implements Step {

   final HotRodResourceKey futureWrapperKey;

   protected HotRodResponseStep(HotRodResourceKey futureWrapperKey) {
      this.futureWrapperKey = futureWrapperKey;
   }

   @Override
   public boolean invoke(Session session) {
      HotRodResource resource = session.getResource(futureWrapperKey);
      boolean complete = resource.isComplete();
      return complete;
   }
}