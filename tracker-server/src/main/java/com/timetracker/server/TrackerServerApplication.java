package com.timetracker.server;

import com.timetracker.server.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TrackerServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackerServerApplication.class, args);
    }
}
