package com.health.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.health")
@MapperScan("com.health.**.mapper")
public class HealthApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthApplication.class, args);
    }
}
