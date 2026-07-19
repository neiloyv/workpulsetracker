package com.timetracker.agent.focus;

/**
 * Платформозависимый доступ к информации об активном окне ОС.
 */
public interface NativeOSService {

    WindowInfo getActiveWindowInfo();

    String getOperatingSystemName();
}
