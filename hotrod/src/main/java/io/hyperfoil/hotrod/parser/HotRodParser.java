package io.hyperfoil.hotrod.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;

import io.hyperfoil.api.config.BenchmarkBuilder;
import io.hyperfoil.core.parser.AbstractParser;
import io.hyperfoil.core.parser.Context;
import io.hyperfoil.core.parser.Parser;
import io.hyperfoil.core.parser.ParserException;
import io.hyperfoil.hotrod.config.HotRod;
import io.hyperfoil.hotrod.config.HotRodBuilder;
import io.hyperfoil.hotrod.config.HotRodPluginBuilder;

public class HotRodParser extends AbstractParser<BenchmarkBuilder, HotRodBuilder> {
   public HotRodParser() {
      register("uri", new HotRodUriParser<>(HotRodBuilder::uri));
   }

   @Override
   public void parse(Context ctx, BenchmarkBuilder target) throws ParserException {
      HotRodPluginBuilder plugin = target.addPlugin(HotRodPluginBuilder::new);
      if (ctx.peek() instanceof SequenceStartEvent) {
         ctx.parseList(plugin, (ctx1, builder) -> {
            HotRodBuilder hotRod = builder.hotRod();
            callSubBuilders(ctx1, hotRod);
         });
      } else {
         callSubBuilders(ctx, plugin.hotRod());
      }
   }

   public static class HotRodUriParser<T> implements Parser<T> {
      private final BiConsumer<T, HotRod.HotRodUri> consumer;

      public HotRodUriParser(BiConsumer<T, HotRod.HotRodUri> consumer) {
         this.consumer = consumer;
      }

      @Override
      public void parse(Context ctx, T target) throws ParserException {
         ScalarEvent eventUri = ctx.expectEvent(ScalarEvent.class);
         ScalarEvent eventCaches = ctx.expectEvent(ScalarEvent.class);
         List<String> caches = new ArrayList<>();
         while (ctx.hasNext()) {
            Event next = ctx.next();
            if (next instanceof SequenceStartEvent) {
               continue;
            } else if (next instanceof ScalarEvent) {
               caches.add(((ScalarEvent) next).getValue());
            } else if (next instanceof SequenceEndEvent) {
               break;
            }  else {
               throw ctx.unexpectedEvent(next);
            }
         }
         HotRod.HotRodUri uri = new HotRod.HotRodUri(eventUri.getValue(), caches);
         consumer.accept(target, uri);
      }
   }
}
