package com.urbanelf.iat.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.UIManager;

public class FileTree {
    private static final Path rootPath;
    private static final Path logPath;

    private FileTree() {
    }

    static {
        switch (PlatformUtils.getRunningPlatform()) {
            case Windows -> rootPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "ICT");
            case Mac -> rootPath = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "ICT");
            // Linux, Unknown
            default -> rootPath = Paths.get(System.getProperty("user.home"), ".ict");
        }

        logPath = rootPath.resolve("logs");

        try {
            Files.createDirectories(logPath); // Build hierarchy
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getRootPath() {
        return rootPath;
    }

    public static Path getLogPath() {
        return logPath;
    }
}
