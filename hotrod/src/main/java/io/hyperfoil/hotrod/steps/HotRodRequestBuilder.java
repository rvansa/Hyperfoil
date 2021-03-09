package io.hyperfoil.hotrod.steps;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.Locator;
import io.hyperfoil.api.config.Name;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.config.StepBuilder;
import io.hyperfoil.core.builders.BaseStepBuilder;
import io.hyperfoil.core.metric.PathMetricSelector;
import io.hyperfoil.core.metric.ProvidedMetricSelector;
import io.hyperfoil.core.session.SessionFactory;
import io.hyperfoil.core.steps.StatisticsStep;
import io.hyperfoil.hotrod.api.HotRodOperation;
import io.hyperfoil.hotrod.resource.HotRodResource;

/**
 * Issues a HotRod request and registers handlers for the response.
 */
@MetaInfServices(StepBuilder.class)
@Name("hotrodRequest")
public class HotRodRequestBuilder extends BaseStepBuilder<HotRodRequestBuilder> {

   private HotRodOperationBuilder operation;
   private HotRodCacheNameBuilder cacheName;
   private ProvidedMetricSelector metricSelector;

   @Override
   public void prepareBuild() {
      if (metricSelector == null) {
         String sequenceName = Locator.current().sequence().name();
         metricSelector = new ProvidedMetricSelector(sequenceName);
      }
   }

   @Override
   public List<Step> build() {
      int stepId = StatisticsStep.nextId();
      HotRodResource.Key key = new HotRodResource.Key();
      HotRodRequestStep step = new HotRodRequestStep(stepId, key, operation.build(), cacheName.build(), metricSelector,
            SessionFactory.access("cacheKey"), SessionFactory.access("cacheValue"));
      HotRodResponseStep secondHotRodStep = new HotRodResponseStep(key);
      return Arrays.asList(step, secondHotRodStep);
   }

   /**
    * Requests statistics will use this metric name.
    *
    * @param name Metric name.
    * @return Self.
    */
   public HotRodRequestBuilder metric(String name) {
      return metric(new ProvidedMetricSelector(name));
   }

   public HotRodRequestBuilder metric(ProvidedMetricSelector selector) {
      this.metricSelector = selector;
      return this;
   }

   /**
    * Allows categorizing request statistics into metrics based on the request path.
    *
    * @return Builder.
    */
   public PathMetricSelector metric() {
      PathMetricSelector selector = new PathMetricSelector();
      this.metricSelector = selector;
      return selector;
   }

   /**
    * A named cache from the remote server if the cache has been defined, otherwise if the cache name is undefined,
    * it will return null.
    *
    * @param cacheName name of cache to retrieve
    * @return
    */
   public HotRodRequestBuilder cacheName(String cacheName) {
      return cacheName(() -> new HotRodCacheNameBuilder.Provided(cacheName));
   }

   public HotRodRequestBuilder cacheName(HotRodCacheNameBuilder cacheName) {
      this.cacheName = cacheName;
      return this;
   }

   public HotRodRequestBuilder operation(HotRodOperation operation) {
      return operation(() -> new HotRodOperationBuilder.Provided(operation));
   }

   public HotRodRequestBuilder operation(HotRodOperationBuilder operation) {
      this.operation = operation;
      return this;
   }

   /**
    * Adds or overrides each specified entry in the remote cache.
    *
    * @param cacheName name of cache to put data
    * @return
    */
   public HotRodRequestBuilder put(String cacheName) {
      return operation(HotRodOperation.PUT).cacheName(cacheName);
   }
}
