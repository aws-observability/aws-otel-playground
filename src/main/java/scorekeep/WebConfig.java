package scorekeep;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.metrics.MetricsSegmentListener;
import com.amazonaws.xray.slf4j.SLF4JSegmentListener;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.opentracingshim.TraceShim;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentracing.contrib.apache.http.client.TracingHttpClientBuilder;
import io.opentracing.util.GlobalTracer;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.io.IOException;
import java.net.URL;
import java.lang.Exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class WebConfig {
  private static final String DEFAULT_APP_NAME = "Scorekeep";
  private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

  @Bean
  public Filter TracingFilter() {
    return new AWSXRayServletFilter(getApplicationName());
  }

  @Bean
  public Filter SimpleCORSFilter() {
    return new SimpleCORSFilter();
  }

  private static String getApplicationName() {
    if(System.getenv("XRAY_APPLICATION_NAME") != null) {
      return System.getenv("XRAY_APPLICATION_NAME");
    }

    return DEFAULT_APP_NAME;
  }

  static {
    //Use the X-Ray OpenTelemetry Implementation
    System.setProperty("io.opentelemetry.trace.spi.TracerProvider",
            "com.amazonaws.xray.opentelemetry.tracing.TracingProvider");

    //Setup X-Ray as Normal
    AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
            .withDefaultPlugins()
            .withSegmentListener(new SLF4JSegmentListener())
            .withSegmentListener(new MetricsSegmentListener());
    URL ruleFile = WebConfig.class.getResource("/sampling-rules.json");
    builder.withSamplingStrategy(new LocalizedSamplingStrategy(ruleFile));
    AWSXRay.setGlobalRecorder(builder.build());

    //Create a Tracer
    Tracer opTracer = OpenTelemetry.getTracerFactory().get(WebConfig.class.getName());

    //Make Spans (Replacing existing segments from the example app here)
    Span span = opTracer.spanBuilder(getApplicationName()).setNoParent().startSpan();

    if ( System.getenv("NOTIFICATION_EMAIL") != null ){
      try { Sns.createSubscription(); }
      catch (Exception e ) {
        logger.warn("Failed to create subscription for email "+  System.getenv("NOTIFICATION_EMAIL"));
      }
    }

    //Demo other spans
    Span span2 = opTracer.spanBuilder("OTSubsegment").setParent(span).startSpan();
    Span span3 = opTracer.spanBuilder("OTChildSubsegment").setParent(span2).startSpan();
    Span span4 = opTracer.spanBuilder("OTNotChildSubsegment").setParent(span).startSpan();
    span3.end();
    span2.end();
    span4.end();

    //Demo 3p library support - Disabled pending https://github.com/open-telemetry/opentelemetry-java/issues/671
    /*
    opTracer.withSpan(span);
    io.opentracing.Tracer tracer = TraceShim.createTracerShim();
    GlobalTracer.registerIfAbsent(tracer);

    HttpClient httpClient = new TracingHttpClientBuilder().build();
    try {
      httpClient.execute(new HttpPost("http://amazon.com"));
    } catch (ClientProtocolException e) {
      logger.error("Can't execute demo HTTP call!", e);
    } catch (IOException e) {
      logger.error("Can't execute demo HTTP call!", e);
    }
    */

    span.end();
  }
}
