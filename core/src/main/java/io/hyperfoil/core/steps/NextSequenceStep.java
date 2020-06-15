package io.hyperfoil.core.steps;

import java.util.Collections;
import java.util.List;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.InitFromParam;
import io.hyperfoil.api.config.Name;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.config.StepBuilder;
import io.hyperfoil.api.session.Session;

public class NextSequenceStep implements Step {
   private final String name;

   public NextSequenceStep(String name) {
      this.name = name;
   }

   @Override
   public boolean invoke(Session session) {
      session.startSequence(name, Session.ConcurrencyPolicy.FAIL);
      return true;
   }

   /**
    * Schedules a new sequence instance to be executed.
    */
   @MetaInfServices(StepBuilder.class)
   @Name("nextSequence")
   public static class Builder implements StepBuilder<Builder>, InitFromParam<Builder> {
      private String name;

      /**
       * Sequence name.
       *
       * @param name Name of the instantiated sequence.
       * @return Self.
       */
      public Builder name(String name) {
         this.name = name;
         return this;
      }

      @Override
      public List<Step> build() {
         return Collections.singletonList(new NextSequenceStep(name));
      }

      /**
       * @param param Name of the instantiated sequence.
       * @return Self.
       */
      @Override
      public Builder init(String param) {
         return name(param);
      }
   }
}
