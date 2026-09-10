package com.ai_resume.dashboard_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * dashboard-service — a stateless aggregator (BFF) for the frontend dashboard.
 *
 * <p>It owns no database and no business rules. It reads from the services that
 * own the data (resume-service today, notification-service later) through their
 * existing public APIs and projects the results into one frontend-shaped payload.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DashboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardServiceApplication.class, args);
    }
}

