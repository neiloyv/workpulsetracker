package com.workpulsetracker.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccessKeyEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AccessKeyEmailService.class);
    private static final String schema = "public";

    public void sendAccessKey(String email, String displayName, String accessKey) {
        logger.info(
                "schema={} Sending access key email to={} displayName={} accessKey={}",
                schema,
                email,
                displayName,
                accessKey
        );
    }
}
