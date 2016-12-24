package net.soundvibe.reacto.server.handlers;

import com.fasterxml.jackson.core.*;
import com.netflix.hystrix.*;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.*;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import java.io.*;

/**
 * @author OZY on 2015.11.09.
 */
public interface HystrixEventStreamHandler {

    Logger log = LoggerFactory.getLogger(HystrixEventStreamHandler.class);

    JsonFactory jsonFactory = new JsonFactory();

    static void handle(HttpServerResponse response) {
        HystrixDashboardStream.getInstance().observe()
                .retry()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(dashboardData -> writeDashboardData(dashboardData, response));
    }

    static void writeDashboardData(HystrixDashboardStream.DashboardData dashboardData, HttpServerResponse response) {
        dashboardData.getCommandMetrics()
                .forEach(hystrixCommandMetrics -> {
                    try {
                        SSEHandler.writeData(response, getCommandJson(hystrixCommandMetrics));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        dashboardData.getCollapserMetrics()
                .forEach(hystrixCollapserMetrics -> {
                    try {
                        SSEHandler.writeData(response, getCollapserJson(hystrixCollapserMetrics));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        dashboardData.getThreadPoolMetrics()
                .forEach(hystrixThreadPoolMetrics -> {
                    try {
                        SSEHandler.writeData(response, getThreadPoolJson(hystrixThreadPoolMetrics));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    static void safelyWriteNumberField(JsonGenerator json, String name, Func0<Long> metricGenerator) throws IOException {
        try {
            json.writeNumberField(name, metricGenerator.call());
        } catch (NoSuchFieldError error) {
            log.error("While publishing Hystrix metrics stream, error looking up eventType for : {}.  Please check that all Hystrix versions are the same!", name);
            json.writeNumberField(name, 0L);
        }
    }

    static String getCommandJson(final HystrixCommandMetrics commandMetrics) throws IOException {
        HystrixCommandKey key = commandMetrics.getCommandKey();
        HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(key);

        StringWriter jsonString = new StringWriter();
        JsonGenerator json = jsonFactory.createGenerator(jsonString);

        json.writeStartObject();
        json.writeStringField("type", "HystrixCommand");
        json.writeStringField("name", key.name());
        json.writeStringField("group", commandMetrics.getCommandGroup().name());
        json.writeNumberField("currentTime", System.currentTimeMillis());

        // circuit breaker
        if (circuitBreaker == null) {
            // circuit breaker is disabled and thus never open
            json.writeBooleanField("isCircuitBreakerOpen", false);
        } else {
            json.writeBooleanField("isCircuitBreakerOpen", circuitBreaker.isOpen());
        }
        HystrixCommandMetrics.HealthCounts healthCounts = commandMetrics.getHealthCounts();
        json.writeNumberField("errorPercentage", healthCounts.getErrorPercentage());
        json.writeNumberField("errorCount", healthCounts.getErrorCount());
        json.writeNumberField("requestCount", healthCounts.getTotalRequests());

        // rolling counters
        safelyWriteNumberField(json, "rollingCountBadRequests", () -> commandMetrics.getRollingCount(HystrixEventType.BAD_REQUEST));
        safelyWriteNumberField(json, "rollingCountCollapsedRequests", () -> commandMetrics.getRollingCount(HystrixEventType.COLLAPSED));
        safelyWriteNumberField(json, "rollingCountEmit", () -> commandMetrics.getRollingCount(HystrixEventType.EMIT));
        safelyWriteNumberField(json, "rollingCountExceptionsThrown", () -> commandMetrics.getRollingCount(HystrixEventType.EXCEPTION_THROWN));
        safelyWriteNumberField(json, "rollingCountFailure", () -> commandMetrics.getRollingCount(HystrixEventType.FAILURE));
        safelyWriteNumberField(json, "rollingCountFallbackEmit", () -> commandMetrics.getRollingCount(HystrixEventType.FALLBACK_EMIT));
        safelyWriteNumberField(json, "rollingCountFallbackFailure", () -> commandMetrics.getRollingCount(HystrixEventType.FALLBACK_FAILURE));
        safelyWriteNumberField(json, "rollingCountFallbackMissing", () -> commandMetrics.getRollingCount(HystrixEventType.FALLBACK_MISSING));
        safelyWriteNumberField(json, "rollingCountFallbackRejection", () -> commandMetrics.getRollingCount(HystrixEventType.FALLBACK_REJECTION));
        safelyWriteNumberField(json, "rollingCountFallbackSuccess", () -> commandMetrics.getRollingCount(HystrixEventType.FALLBACK_SUCCESS));
        safelyWriteNumberField(json, "rollingCountResponsesFromCache", () -> commandMetrics.getRollingCount(HystrixEventType.RESPONSE_FROM_CACHE));
        safelyWriteNumberField(json, "rollingCountSemaphoreRejected", () -> commandMetrics.getRollingCount(HystrixEventType.SEMAPHORE_REJECTED));
        safelyWriteNumberField(json, "rollingCountShortCircuited", () -> commandMetrics.getRollingCount(HystrixEventType.SHORT_CIRCUITED));
        safelyWriteNumberField(json, "rollingCountSuccess", () -> commandMetrics.getRollingCount(HystrixEventType.SUCCESS));
        safelyWriteNumberField(json, "rollingCountThreadPoolRejected", () -> commandMetrics.getRollingCount(HystrixEventType.THREAD_POOL_REJECTED));
        safelyWriteNumberField(json, "rollingCountTimeout", () -> commandMetrics.getRollingCount(HystrixEventType.TIMEOUT));

        json.writeNumberField("currentConcurrentExecutionCount", commandMetrics.getCurrentConcurrentExecutionCount());
        json.writeNumberField("rollingMaxConcurrentExecutionCount", commandMetrics.getRollingMaxConcurrentExecutions());

        // latency percentiles
        json.writeNumberField("latencyExecute_mean", commandMetrics.getExecutionTimeMean());
        json.writeObjectFieldStart("latencyExecute");
        json.writeNumberField("0", commandMetrics.getExecutionTimePercentile(0));
        json.writeNumberField("25", commandMetrics.getExecutionTimePercentile(25));
        json.writeNumberField("50", commandMetrics.getExecutionTimePercentile(50));
        json.writeNumberField("75", commandMetrics.getExecutionTimePercentile(75));
        json.writeNumberField("90", commandMetrics.getExecutionTimePercentile(90));
        json.writeNumberField("95", commandMetrics.getExecutionTimePercentile(95));
        json.writeNumberField("99", commandMetrics.getExecutionTimePercentile(99));
        json.writeNumberField("99.5", commandMetrics.getExecutionTimePercentile(99.5));
        json.writeNumberField("100", commandMetrics.getExecutionTimePercentile(100));
        json.writeEndObject();
        //
        json.writeNumberField("latencyTotal_mean", commandMetrics.getTotalTimeMean());
        json.writeObjectFieldStart("latencyTotal");
        json.writeNumberField("0", commandMetrics.getTotalTimePercentile(0));
        json.writeNumberField("25", commandMetrics.getTotalTimePercentile(25));
        json.writeNumberField("50", commandMetrics.getTotalTimePercentile(50));
        json.writeNumberField("75", commandMetrics.getTotalTimePercentile(75));
        json.writeNumberField("90", commandMetrics.getTotalTimePercentile(90));
        json.writeNumberField("95", commandMetrics.getTotalTimePercentile(95));
        json.writeNumberField("99", commandMetrics.getTotalTimePercentile(99));
        json.writeNumberField("99.5", commandMetrics.getTotalTimePercentile(99.5));
        json.writeNumberField("100", commandMetrics.getTotalTimePercentile(100));
        json.writeEndObject();

        // property values for reporting what is actually seen by the command rather than what was set somewhere
        HystrixCommandProperties commandProperties = commandMetrics.getProperties();

        json.writeNumberField("propertyValue_circuitBreakerRequestVolumeThreshold", commandProperties.circuitBreakerRequestVolumeThreshold().get());
        json.writeNumberField("propertyValue_circuitBreakerSleepWindowInMilliseconds", commandProperties.circuitBreakerSleepWindowInMilliseconds().get());
        json.writeNumberField("propertyValue_circuitBreakerErrorThresholdPercentage", commandProperties.circuitBreakerErrorThresholdPercentage().get());
        json.writeBooleanField("propertyValue_circuitBreakerForceOpen", commandProperties.circuitBreakerForceOpen().get());
        json.writeBooleanField("propertyValue_circuitBreakerForceClosed", commandProperties.circuitBreakerForceClosed().get());
        json.writeBooleanField("propertyValue_circuitBreakerEnabled", commandProperties.circuitBreakerEnabled().get());

        json.writeStringField("propertyValue_executionIsolationStrategy", commandProperties.executionIsolationStrategy().get().name());
        json.writeNumberField("propertyValue_executionIsolationThreadTimeoutInMilliseconds", commandProperties.executionTimeoutInMilliseconds().get());
        json.writeNumberField("propertyValue_executionTimeoutInMilliseconds", commandProperties.executionTimeoutInMilliseconds().get());
        json.writeBooleanField("propertyValue_executionIsolationThreadInterruptOnTimeout", commandProperties.executionIsolationThreadInterruptOnTimeout().get());
        json.writeStringField("propertyValue_executionIsolationThreadPoolKeyOverride", commandProperties.executionIsolationThreadPoolKeyOverride().get());
        json.writeNumberField("propertyValue_executionIsolationSemaphoreMaxConcurrentRequests", commandProperties.executionIsolationSemaphoreMaxConcurrentRequests().get());
        json.writeNumberField("propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests", commandProperties.fallbackIsolationSemaphoreMaxConcurrentRequests().get());

                    /*
                     * The following are commented out as these rarely change and are verbose for streaming for something people don't change.
                     * We could perhaps allow a property or request argument to include these.
                     */

        //                    json.put("propertyValue_metricsRollingPercentileEnabled", commandProperties.metricsRollingPercentileEnabled().get());
        //                    json.put("propertyValue_metricsRollingPercentileBucketSize", commandProperties.metricsRollingPercentileBucketSize().get());
        //                    json.put("propertyValue_metricsRollingPercentileWindow", commandProperties.metricsRollingPercentileWindowInMilliseconds().get());
        //                    json.put("propertyValue_metricsRollingPercentileWindowBuckets", commandProperties.metricsRollingPercentileWindowBuckets().get());
        //                    json.put("propertyValue_metricsRollingStatisticalWindowBuckets", commandProperties.metricsRollingStatisticalWindowBuckets().get());
        json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", commandProperties.metricsRollingStatisticalWindowInMilliseconds().get());

        json.writeBooleanField("propertyValue_requestCacheEnabled", commandProperties.requestCacheEnabled().get());
        json.writeBooleanField("propertyValue_requestLogEnabled", commandProperties.requestLogEnabled().get());

        json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster
        json.writeStringField("threadPool", commandMetrics.getThreadPoolKey().name());

        json.writeEndObject();
        json.close();

        return jsonString.getBuffer().toString();
    }

    static String getThreadPoolJson(final HystrixThreadPoolMetrics threadPoolMetrics) throws IOException {
        HystrixThreadPoolKey key = threadPoolMetrics.getThreadPoolKey();
        StringWriter jsonString = new StringWriter();
        JsonGenerator json = jsonFactory.createGenerator(jsonString);
        json.writeStartObject();

        json.writeStringField("type", "HystrixThreadPool");
        json.writeStringField("name", key.name());
        json.writeNumberField("currentTime", System.currentTimeMillis());

        json.writeNumberField("currentActiveCount", threadPoolMetrics.getCurrentActiveCount().intValue());
        json.writeNumberField("currentCompletedTaskCount", threadPoolMetrics.getCurrentCompletedTaskCount().longValue());
        json.writeNumberField("currentCorePoolSize", threadPoolMetrics.getCurrentCorePoolSize().intValue());
        json.writeNumberField("currentLargestPoolSize", threadPoolMetrics.getCurrentLargestPoolSize().intValue());
        json.writeNumberField("currentMaximumPoolSize", threadPoolMetrics.getCurrentMaximumPoolSize().intValue());
        json.writeNumberField("currentPoolSize", threadPoolMetrics.getCurrentPoolSize().intValue());
        json.writeNumberField("currentQueueSize", threadPoolMetrics.getCurrentQueueSize().intValue());
        json.writeNumberField("currentTaskCount", threadPoolMetrics.getCurrentTaskCount().longValue());
        safelyWriteNumberField(json, "rollingCountThreadsExecuted", () -> threadPoolMetrics.getRollingCount(HystrixEventType.ThreadPool.EXECUTED));
        json.writeNumberField("rollingMaxActiveThreads", threadPoolMetrics.getRollingMaxActiveThreads());
        safelyWriteNumberField(json, "rollingCountCommandRejections", () -> threadPoolMetrics.getRollingCount(HystrixEventType.ThreadPool.REJECTED));

        json.writeNumberField("propertyValue_queueSizeRejectionThreshold", threadPoolMetrics.getProperties().queueSizeRejectionThreshold().get());
        json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", threadPoolMetrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get());

        json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster

        json.writeEndObject();
        json.close();

        return jsonString.getBuffer().toString();
    }

    static String getCollapserJson(final HystrixCollapserMetrics collapserMetrics) throws IOException {
        HystrixCollapserKey key = collapserMetrics.getCollapserKey();
        StringWriter jsonString = new StringWriter();
        JsonGenerator json = jsonFactory.createGenerator(jsonString);
        json.writeStartObject();

        json.writeStringField("type", "HystrixCollapser");
        json.writeStringField("name", key.name());
        json.writeNumberField("currentTime", System.currentTimeMillis());

        safelyWriteNumberField(json, "rollingCountRequestsBatched", () -> collapserMetrics.getRollingCount(HystrixEventType.Collapser.ADDED_TO_BATCH));
        safelyWriteNumberField(json, "rollingCountBatches", () -> collapserMetrics.getRollingCount(HystrixEventType.Collapser.BATCH_EXECUTED));
        safelyWriteNumberField(json, "rollingCountResponsesFromCache", () -> collapserMetrics.getRollingCount(HystrixEventType.Collapser.RESPONSE_FROM_CACHE));

        // batch size percentiles
        json.writeNumberField("batchSize_mean", collapserMetrics.getBatchSizeMean());
        json.writeObjectFieldStart("batchSize");
        json.writeNumberField("25", collapserMetrics.getBatchSizePercentile(25));
        json.writeNumberField("50", collapserMetrics.getBatchSizePercentile(50));
        json.writeNumberField("75", collapserMetrics.getBatchSizePercentile(75));
        json.writeNumberField("90", collapserMetrics.getBatchSizePercentile(90));
        json.writeNumberField("95", collapserMetrics.getBatchSizePercentile(95));
        json.writeNumberField("99", collapserMetrics.getBatchSizePercentile(99));
        json.writeNumberField("99.5", collapserMetrics.getBatchSizePercentile(99.5));
        json.writeNumberField("100", collapserMetrics.getBatchSizePercentile(100));
        json.writeEndObject();

        // shard size percentiles (commented-out for now)
        //json.writeNumberField("shardSize_mean", collapserMetrics.getShardSizeMean());
        //json.writeObjectFieldStart("shardSize");
        //json.writeNumberField("25", collapserMetrics.getShardSizePercentile(25));
        //json.writeNumberField("50", collapserMetrics.getShardSizePercentile(50));
        //json.writeNumberField("75", collapserMetrics.getShardSizePercentile(75));
        //json.writeNumberField("90", collapserMetrics.getShardSizePercentile(90));
        //json.writeNumberField("95", collapserMetrics.getShardSizePercentile(95));
        //json.writeNumberField("99", collapserMetrics.getShardSizePercentile(99));
        //json.writeNumberField("99.5", collapserMetrics.getShardSizePercentile(99.5));
        //json.writeNumberField("100", collapserMetrics.getShardSizePercentile(100));
        //json.writeEndObject();

        //json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", collapserMetrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get());
        json.writeBooleanField("propertyValue_requestCacheEnabled", collapserMetrics.getProperties().requestCacheEnabled().get());
        json.writeNumberField("propertyValue_maxRequestsInBatch", collapserMetrics.getProperties().maxRequestsInBatch().get());
        json.writeNumberField("propertyValue_timerDelayInMilliseconds", collapserMetrics.getProperties().timerDelayInMilliseconds().get());

        json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster

        json.writeEndObject();
        json.close();

        return jsonString.getBuffer().toString();
    }


}
