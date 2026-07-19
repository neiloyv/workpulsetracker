package com.timetracker.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String uiOrigin = "http://localhost:5173";
    private final Download download = new Download();

    public String getUiOrigin() {
        return uiOrigin;
    }

    public void setUiOrigin(String uiOrigin) {
        this.uiOrigin = uiOrigin;
    }

    public Download getDownload() {
        return download;
    }

    public static class Download {
        private String windowsUrl = "/downloads/timetracker-agent-windows.msi";
        private String macosUrl = "/downloads/timetracker-agent-macos.dmg";
        private String linuxUrl = "/downloads/timetracker-agent-linux.deb";

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
}
