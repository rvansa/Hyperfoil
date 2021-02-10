package io.hyperfoil.hotrod.parser;

import org.yaml.snakeyaml.events.SequenceStartEvent;

import io.hyperfoil.api.config.BenchmarkBuilder;
import io.hyperfoil.core.parser.AbstractParser;
import io.hyperfoil.core.parser.Context;
import io.hyperfoil.core.parser.ParserException;
import io.hyperfoil.core.parser.PropertyParser;
import io.hyperfoil.hotrod.config.HotRodBuilder;
import io.hyperfoil.hotrod.config.HotRodPluginBuilder;

public class HotRodParser extends AbstractParser<BenchmarkBuilder, HotRodBuilder> {
   public HotRodParser() {
      register("uri", new PropertyParser.String<>(HotRodBuilder::uri));
      register("cacheName", new PropertyParser.String<>(HotRodBuilder::cacheName));
   }

   @Override
   public void parse(Context ctx, BenchmarkBuilder target) throws ParserException {
      HotRodPluginBuilder plugin = target.addPlugin(HotRodPluginBuilder::new);
      if (ctx.peek() instanceof SequenceStartEvent) {
         ctx.parseList(plugin, (ctx1, builder) -> {
            HotRodBuilder hotRod = builder.decoupledHotRod();
            callSubBuilders(ctx1, hotRod);
            builder.addHotRod(hotRod);
         });
      } else {
         callSubBuilders(ctx, plugin.hotRod());
      }
   }
}
