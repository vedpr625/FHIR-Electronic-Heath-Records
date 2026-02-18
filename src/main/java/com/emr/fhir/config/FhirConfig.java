package com.emr.fhir.config;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class FhirConfig {
    @Value("${fhir.server.url:http://hapi.fhir.org/baseR4}")
    private String fhirServerUrl;
    @Bean
    public FhirContext fhirContext() { return FhirContext.forR4(); }
    @Bean
    public IGenericClient fhirClient(FhirContext ctx) {
        ctx.getRestfulClientFactory().setConnectTimeout(30000);
        ctx.getRestfulClientFactory().setSocketTimeout(60000);
        IGenericClient client = ctx.newRestfulGenericClient(fhirServerUrl);
        LoggingInterceptor li = new LoggingInterceptor();
        li.setLogRequestSummary(true); li.setLogResponseSummary(true);
        client.registerInterceptor(li);
        return client;
    }
}
