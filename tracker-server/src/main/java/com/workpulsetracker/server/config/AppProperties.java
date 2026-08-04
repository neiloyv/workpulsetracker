package com.workpulsetracker.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String uiOrigin = "http://localhost:5173";
    private final Download download = new Download();
    private final Jwt jwt = new Jwt();

    public String getUiOrigin() {
        return uiOrigin;
    }

    public void setUiOrigin(String uiOrigin) {
        this.uiOrigin = uiOrigin;
    }

    public Download getDownload() {
        return download;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public static class Download {
        /**
         * Локальная папка с установщиками (относительно working directory процесса сервера).
         * По умолчанию — {@code downloads} в корне репозитория при запуске из корня.
         */
        private String directory = "downloads";
        private String windowsUrl = "/downloads/workpulsetracker-agent-windows.zip";
        private String macosUrl = "/downloads/workpulsetracker-agent-macos.dmg";
        private String linuxUrl = "/downloads/workpulsetracker-agent-linux.deb";

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public String getWindowsUrl() {
            return windowsUrl;
        }

        public void setWindowsUrl(String windowsUrl) {
            this.windowsUrl = windowsUrl;
        }

        public String getMacosUrl() {
            return macosUrl;
        }

        public void setMacosUrl(String macosUrl) {
            this.macosUrl = macosUrl;
        }

        public String getLinuxUrl() {
            return linuxUrl;
        }

        public void setLinuxUrl(String linuxUrl) {
            this.linuxUrl = linuxUrl;
        }
    }

    public static class Jwt {
        private String secret = "workpulsetracker-dev-jwt-secret-change-me-32b";
        private long expirationSeconds = 2_592_000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }
}
