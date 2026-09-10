package com.ai_resume.trends_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * trends-service exposes global trend aggregations over resume analysis data.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class TrendsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrendsServiceApplication.class, args);
    }
}

