package com.urbanelf.ima.util;

public class PlatformUtils {
    private static Platform PLATFORM;

    private PlatformUtils() {
    }

    public static void initialize() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            PLATFORM = Platform.Linux;
        } else if (osName.contains("windows")) {
            PLATFORM = Platform.Windows;
        } else if (osName.contains("mac")) {
            PLATFORM = Platform.Mac;
        } else {
            PLATFORM = Platform.Unknown;
        }
    }

    public static Platform getRunningPlatform() {
        return PLATFORM;
    }

    public enum Platform {
        Linux,
        Windows,
        Mac,
        Unknown;
    }
}
