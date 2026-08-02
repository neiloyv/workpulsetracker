package com.workpulsetracker.server.config;

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
        /**
         * Directory with installer files, relative to process working directory or absolute.
         * Expected Windows file name: workpulsetracker-agent-windows.msi
         */
        private String directory = "downloads";
        private String windowsUrl = "/downloads/workpulsetracker-agent-windows.msi";
        private String macosUrl = "/downloads/workpulsetracker-agent-macos.dmg";
        private String linuxUrl = "/downloads/workpulsetracker-agent-linux.deb";
        private String windowsFileName = "workpulsetracker-agent-windows.msi";
        private String macosFileName = "workpulsetracker-agent-macos.dmg";
        private String linuxFileName = "workpulsetracker-agent-linux.deb";

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

        public String getWindowsFileName() {
            return windowsFileName;
        }

        public void setWindowsFileName(String windowsFileName) {
            this.windowsFileName = windowsFileName;
        }

        public String getMacosFileName() {
            return macosFileName;
        }

        public void setMacosFileName(String macosFileName) {
            this.macosFileName = macosFileName;
        }

        public String getLinuxFileName() {
            return linuxFileName;
        }

        public void setLinuxFileName(String linuxFileName) {
            this.linuxFileName = linuxFileName;
        }
    }
}
