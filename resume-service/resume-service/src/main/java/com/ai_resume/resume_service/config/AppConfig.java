package com.ai_resume.resume_service.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    /**
     * Load-balanced RestTemplate used to reach ai-service by its Eureka service id
     * ({@code http://ai-service/...}) instead of a hardcoded host and port.
     *
     * <p>Explicit timeouts are essential: the default is "wait forever", so a hung
     * ai-service would pin a resume-service HTTP thread (and an open DB
     * transaction) indefinitely.
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(
            @Value("${ai.service.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${ai.service.read-timeout-ms:60000}") long readTimeoutMs) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new RestTemplate(factory);
    }
}
