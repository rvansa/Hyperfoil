package io.hyperfoil.core.extractors;

import io.hyperfoil.api.http.Processor;
import io.netty.buffer.ByteBuf;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.api.ResourceUtilizer;

public class CountRecorder implements Processor, ResourceUtilizer {
   private final String var;

   public CountRecorder(String var) {
      this.var = var;
   }

   @Override
   public void before(Session session) {
      session.setInt(var, 0);
   }

   @Override
   public void process(Session session, ByteBuf data, int offset, int length, boolean isLastPart) {
      if (isLastPart) {
         session.addToInt(var, 1);
      }
   }

   @Override
   public void reserve(Session session) {
      session.declareInt(var);
   }
}