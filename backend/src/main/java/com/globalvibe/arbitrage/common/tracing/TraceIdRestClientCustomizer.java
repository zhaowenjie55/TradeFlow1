package com.globalvibe.arbitrage.common.tracing;

import org.slf4j.MDC;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adds an outbound {@link ClientHttpRequestInterceptor} to every
 * {@link RestClient.Builder} bean that Spring Boot auto-configures. The
 * interceptor reads the current {@code traceId} from the SLF4J MDC (populated
 * by {@link TraceIdFilter} on inbound requests) and stamps it onto each
 * outbound HTTP call as the {@value TraceIdFilter#TRACE_ID_HEADER} header,
 * giving end-to-end correlation across Spring → FastAPI → any downstream.
 *
 * <p>Clients constructed via the injected {@code RestClient.Builder} bean
 * automatically inherit this customizer; clients that call
 * {@code RestClient.builder()} directly do NOT, which is why the 7 existing
 * HTTP clients are being migrated to inject the builder.
 */
@Component
public class TraceIdRestClientCustomizer implements RestClientCustomizer {

    @Override
    public void customize(RestClient.Builder restClientBuilder) {
        restClientBuilder.requestInterceptor((request, body, execution) -> {
            String traceId = MDC.get(TraceIdFilter.MDC_KEY);
            if (traceId != null && !traceId.isBlank()
                    && !request.getHeaders().containsKey(TraceIdFilter.TRACE_ID_HEADER)) {
                request.getHeaders().add(TraceIdFilter.TRACE_ID_HEADER, traceId);
            }
            return execution.execute(request, body);
        });
    }
}
