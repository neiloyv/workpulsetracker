package com.workpulsetracker.common.i18n;

/**
 * Ключи пользовательских сообщений (общие для agent / server / ui).
 * В коде используем константы, а не «магические» строки.
 */
public final class MessageCodes {

    public static final String ERROR_AGENT_STARTUP_FAILED = "error.agent.startup.failed";
    public static final String ERROR_AGENT_NATIVE_HOOK_FAILED = "error.agent.native.hook.failed";
    public static final String ERROR_AGENT_RESOURCE_CLOSE_FAILED = "error.agent.resource.close.failed";
    public static final String ERROR_GENERIC_UNEXPECTED = "error.generic.unexpected";
    public static final String ERROR_SETTINGS_LANGUAGE_UNSUPPORTED = "error.settings.language.unsupported";

    public static final String UI_APP_TITLE = "ui.app.title";
    public static final String UI_TAB_MAIN = "ui.tab.main";
    public static final String UI_TAB_STATISTICS = "ui.tab.statistics";

    public static final String UI_MAIN_WORK_TIME = "ui.main.work.time";
    public static final String UI_MAIN_START = "ui.main.start";
    public static final String UI_MAIN_PAUSE = "ui.main.pause";
    public static final String UI_MAIN_STATUS_RUNNING = "ui.main.status.running";
    public static final String UI_MAIN_STATUS_PAUSED = "ui.main.status.paused";
    public static final String UI_MAIN_APPLICATIONS_TODAY = "ui.main.applications.today";
    public static final String UI_MAIN_NO_APPLICATIONS = "ui.main.no.applications";
    public static final String UI_MAIN_SYNC = "ui.main.sync";
    public static final String UI_MAIN_SYNC_TOOLTIP = "ui.main.sync.tooltip";
    public static final String UI_MAIN_SYNC_DISABLED_TOOLTIP = "ui.main.sync.disabled.tooltip";
    public static final String UI_MAIN_SYNC_NOT_IMPLEMENTED = "ui.main.sync.not.implemented";

    public static final String UI_STATS_PERIOD = "ui.stats.period";
    public static final String UI_STATS_PERIOD_DAY = "ui.stats.period.day";
    public static final String UI_STATS_PERIOD_WEEK = "ui.stats.period.week";
    public static final String UI_STATS_PERIOD_MONTH = "ui.stats.period.month";
    public static final String UI_STATS_PERIOD_YEAR = "ui.stats.period.year";
    public static final String UI_STATS_PERIOD_ALL = "ui.stats.period.all";
    public static final String UI_STATS_TOTAL = "ui.stats.total";
    public static final String UI_STATS_BY_DAY = "ui.stats.by.day";
    public static final String UI_STATS_BY_APP = "ui.stats.by.app";
    public static final String UI_STATS_EMPTY = "ui.stats.empty";

    public static final String UI_ACTIVATION_TITLE = "ui.activation.title";
    public static final String UI_ACTIVATION_DESCRIPTION = "ui.activation.description";
    public static final String UI_ACTIVATION_KEY_LABEL = "ui.activation.key.label";
    public static final String UI_ACTIVATION_ACTIVATE = "ui.activation.activate";
    public static final String UI_ACTIVATION_LOCAL_ONLY = "ui.activation.local.only";
    public static final String UI_ACTIVATION_KEY_REQUIRED = "ui.activation.key.required";

    public static final String UI_TRAY_OPEN = "ui.tray.open";
    public static final String UI_TRAY_EXIT = "ui.tray.exit";

    private MessageCodes() {
    }
}
