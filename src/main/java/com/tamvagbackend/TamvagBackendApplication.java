package com.tamvagbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TamvagBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TamvagBackendApplication.class, args);
    }

}
