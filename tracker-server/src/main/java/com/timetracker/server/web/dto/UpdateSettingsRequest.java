package com.timetracker.server.web.dto;

import java.util.Map;

public record UpdateSettingsRequest(Map<String, String> settings) {
}
