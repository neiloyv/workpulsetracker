package com.workpulsetracker.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class DownloadsStaticConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(DownloadsStaticConfig.class);
    private static final String schema = "public";

    private final AppProperties appProperties;

    public DownloadsStaticConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path downloadsDirectory = Path.of(appProperties.getDownload().getDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(downloadsDirectory)) {
            logger.warn(
                    "schema={} Downloads directory does not exist yet: {}. Create it and publish the Windows MSI.",
                    schema,
                    downloadsDirectory
            );
        }
        String location = downloadsDirectory.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/downloads/**")
                .addResourceLocations(location)
                .setCachePeriod(0);
        logger.info("schema={} Serving agent downloads from {}", schema, downloadsDirectory);
    }
}
