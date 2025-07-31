/*
 * This file is part of Invision Archive Tools (IAT).
 *
 * Copyright (C) 2025 Mark Fisher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.urbanelf.iat;

import com.urbanelf.iat.content.ArchiveFormat;
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.content.writer.WriterDispatcher;
import com.urbanelf.iat.proto.PythonServer;
import com.urbanelf.iat.ui.MainFrame;
import com.urbanelf.iat.ui.TOAFrame;
import com.urbanelf.iat.util.ArrayUtils;
import com.urbanelf.iat.util.Benchmark;
import com.urbanelf.iat.util.DateUtils;
import com.urbanelf.iat.util.DefaultLogger;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.LocalStorage;
import com.urbanelf.iat.util.Logger;
import com.urbanelf.iat.util.PlatformUtils;
import com.urbanelf.iat.util.StringUtils;
import com.urbanelf.iat.util.ThemeManager;
import com.urbanelf.iat.util.function.QuadConsumer;
import com.urbanelf.iat.util.function.TriConsumer;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;

public class Core {
    private static final String TAG = Core.class.getSimpleName();

    public static final String DEVELOPER_EMAIL = "iat.legacy037@aleeas.com";
    private static final String LS_AGREED_TO_TERMS = "agreed_to_terms";

    private static FileChooser FILE_CHOOSER;

    private static FileLock lock;
    private static FileChannel channel;

    private static Logger logger;
    private static String logFileNameRaw;
    private static File logFile;
    private static File fatalLogFile;
    private static PrintStream logStream;

    private static final JPanel exportArchivePanel;
    private static final JComboBox<ArchiveFormat> archiveFileFormat;

    static {
        exportArchivePanel = new JPanel(new GridBagLayout());

        final JPanel formatPanel = new JPanel();
        formatPanel.setLayout(new BoxLayout(formatPanel, BoxLayout.X_AXIS));
        archiveFileFormat = new JComboBox<>(ArchiveFormat.values());
        formatPanel.add(new JLabel("File Format: "));
        formatPanel.add(Box.createHorizontalStrut(4));
        formatPanel.add(archiveFileFormat);

        exportArchivePanel.add(formatPanel, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 1;
                this.fill = HORIZONTAL;
            }
        });
    }

    private static boolean lockInstance(Path lockFilePath) {
        try {
            final File file = lockFilePath.toFile();

            channel = new RandomAccessFile(file, "rw").getChannel();

            lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return false; // already locked
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    channel.close();
                    file.delete();
                } catch (Exception e) {
                    Core.error(TAG, "Failed to free session lock", e);
                }
            }));

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void exportArchive(JFrame parent, ContentSpec spec) {
        exportArchive(parent, spec, false);
    }

    public static void exportArchive(JFrame parent, ContentSpec spec, boolean destroyOnFinish) {
        // Custom button labels
        String[] options = {"Cancel", "OK"};

        SwingUtilities.updateComponentTreeUI(exportArchivePanel);
        exportArchivePanel.revalidate();
        exportArchivePanel.repaint();

        int result = JOptionPane.showOptionDialog(
                parent,
                exportArchivePanel,
                "Export archive...",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1]
        );

        if (result == 1) {
            final CompletableFuture<Void> fxTask = new CompletableFuture<>();

            Platform.runLater(() -> {
                try {
                    // Resolve format
                    final ArchiveFormat format = (ArchiveFormat) Objects.requireNonNull(archiveFileFormat.getSelectedItem());
                    // Resolve destination
                    final File dst = Core.saveFile(StringUtils.cleanFileName(spec.content().getTitle()) + '.' + format.getExtension(),
                            format.getExtensionFilter());
                    if (dst == null) {
                        final CompletableFuture<Void> exportCancelledTask = new CompletableFuture<>();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(parent,
                                    "Operation cancelled.",
                                    "Export Archive...",
                                    JOptionPane.INFORMATION_MESSAGE);
                            exportCancelledTask.complete(null);
                        });
                        try {
                            exportCancelledTask.get();
                        } catch (Exception ex) {
                        }
                        Core.info(TAG, "Export canceled.");
                        fxTask.complete(null);
                        return;
                    }
                    final File outputDir;
                    if (format == ArchiveFormat.JSON) {
                        // Edge case: copy file (already stored as JSON)
                        Files.copy(spec.file().toPath(), dst.toPath());
                        outputDir = dst.getParentFile();
                    } else {
                        // Convert file
                        outputDir = WriterDispatcher.write(spec, dst, format);
                    }
                    // Delete temp file
                    spec.file().deleteOnExit();

                    // Notify success
                    SwingUtilities.invokeLater(() -> {
                        Object[] options2 = {"Open Directory", "Close"};
                        int result2 = JOptionPane.showOptionDialog(
                                parent,
                                "Archive exported successfully.",
                                "Export Finished",
                                JOptionPane.DEFAULT_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                options2,
                                options2[1]
                        );
                        if (result2 == 0) {
                            try {
                                Desktop.getDesktop().open(outputDir);
                            } catch (IOException e) {
                                Core.error(TAG, "Failed to open output directory", e);
                            }
                        }
                        // Destroy frame
                        if (parent != null && destroyOnFinish)
                            parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
                        fxTask.complete(null);
                    });
                } catch (Exception ex) {
                    Core.fatal(TAG, "Failed to export archive", ex);
                    fxTask.completeExceptionally(ex);
                }
            });

            // Wait for JavaFX task to complete (blocks current thread)
            try {
                fxTask.get();
            } catch (Exception ex) {
                Core.error(TAG, "Error while waiting on FX export task", ex);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // Required by FileTree invocation (↓)
        PlatformUtils.initialize();

        // Only permit one instance running at a time to avoid conflicts
        if (!lockInstance(FileTree.getRootPath().resolve("session.lock"))) {
            System.out.println("Another instance is already running.");
            System.exit(0);
        }

        logger = new DefaultLogger();
        logFileNameRaw = DateUtils.format("MM-dd-yyyy_h:mm:ss_a");
        fatalLogFile = FileTree.getRootPath().resolve("fatal_" + logFileNameRaw + ".log").toFile();

        try {
            initializeLogSystem();
        } catch (IOException e) {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new FileOutputStream(fatalLogFile));
                e.printStackTrace(writer);
            } catch (IOException e2) {
            }

            final PrintWriter finalWriter = writer;
            SwingUtilities.invokeLater(() -> {
                Object[] options = {"No", "Yes"};
                int n = JOptionPane.showOptionDialog(null,
                        "IAT was unable to initialize its internal log system.\n\n"
                                + "  - Please verify that your home directory is writable\n"
                                + "  - Contact the developer at:\n"
                                + "     " + DEVELOPER_EMAIL
                                + "\n\nOpen log?",
                        "An error occurred",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[1]);

                if (n == 1) {
                    try {
                        Desktop.getDesktop().open(fatalLogFile);
                    } catch (Exception e2) {
                        if (finalWriter != null) {
                            finalWriter.println();
                            e2.printStackTrace(finalWriter);
                        }
                    }
                }
            });

            if (writer != null)
                writer.close();

            return;
        }

        final QuadConsumer<TriConsumer<Byte[], Integer, Integer>, Byte[], Integer, Integer> streamWrite =
                (writeFunc, buf, off, len) -> {
                    writeFunc.accept(buf, off, len);
                    logStream.write(ArrayUtils.unbox(buf), off, len);
                };

        System.setOut(new PrintStream(System.out, true) {
            @Override
            public void write(byte[] buf, int off, int len) {
                streamWrite.accept((_buf, _off, _len) -> super.write(ArrayUtils.unbox(_buf), _off, _len), ArrayUtils.box(buf), off, len);
            }
        });

        System.setErr(new PrintStream(System.err, true) {
            @Override
            public void write(byte[] buf, int off, int len) {
                streamWrite.accept((_buf, _off, _len) -> super.write(ArrayUtils.unbox(_buf), _off, _len), ArrayUtils.box(buf), off, len);
            }
        });

        logger.printHeader();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Core.fatal(TAG, "A fatal error occurred", throwable);
            SwingUtilities.invokeLater(() -> {
                Object[] options = {"No", "Yes"};
                int n = JOptionPane.showOptionDialog(null,
                        "IAT has encountered a fatal error, and has crashed.\n\n"
                                + "Please contact the developer at the email below,\n"
                                + "and attach the most recent log file along with your message.\n\n"
                                + DEVELOPER_EMAIL
                                + "\n\nOpen log?",
                        "An error occurred",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[1]);

                if (n == 1) {
                    try {
                        Desktop.getDesktop().open(logFile);
                    } catch (Exception e2) {
                    }
                }

                System.exit(1);
            });
        });

        Benchmark.begin();
        final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        // Periodic log purge task
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try (Stream<Path> files = Files.list(FileTree.getLogPath())) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().contains(logFileNameRaw))
                        .forEach(path -> {
                            try {
                                final BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                                // 30-day log lifespan
                                if (ChronoUnit.DAYS.between(attr.creationTime().toInstant(), Instant.now()) >= 30L) {
                                    Files.deleteIfExists(path);
                                    info(TAG, "Purged log '" + path + "'");
                                }
                            } catch (IOException e) {
                                error(TAG, "An error occurred while attempting to read attributes of log file " + path, e);
                            }
                        });
            } catch (IOException e) {
                error(TAG, "Error while trying to index log directory for purge", e);
            }
        }, 0, 1, TimeUnit.HOURS);
        Core.info(TAG, "Started executor service [" + Benchmark.end() + "ms]");

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logStream.close();
            scheduledExecutor.shutdown();
            Core.info(TAG, "Session ended on " + DateUtils.date());
        }, "Core-ShutdownHook"));

        // Initialize python server
        PythonServer.initialize();

        Core.info(TAG, "Session began on " + DateUtils.date());

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        System.setProperty("flatlaf.useUbuntuFont", "true");

        // Setup default LaF
        ThemeManager.applyDefaultTheme();

        Benchmark.begin();
        new JFXPanel(); // Initialize JavaFX
        Core.info(TAG, "Initialized JavaFX [" + Benchmark.end() + "ms]");
        Platform.runLater(() -> {
            FILE_CHOOSER = new FileChooser();
            FILE_CHOOSER.setInitialDirectory(new File(System.getProperty("user.home")));

            SwingUtilities.invokeLater(() -> {
                if (LocalStorage.getJsonObject().has(LS_AGREED_TO_TERMS)) {
                    new MainFrame(); // Automatically shows
                } else {
                    new TOAFrame() {
                        @Override
                        protected void result(boolean accepted) {
                            LocalStorage.getJsonObject().put(LS_AGREED_TO_TERMS, true);
                            LocalStorage.serialize();
                            new MainFrame(); // Automatically shows
                        }
                    }.setVisible(true);
                }
            });
        });

        //var group = new ICWorkerStack<IC4Worker<?>>(2);
        //group.setIC(new IC4("https://catholicharbor.com/"));
        //group.pushWorker(new MessengerWorker(12, posts -> {}));
    }

    public static File openFile(FileChooser.ExtensionFilter... extensionFilters) {
        FILE_CHOOSER.setTitle("Open File");
        FILE_CHOOSER.getExtensionFilters().clear();
        FILE_CHOOSER.getExtensionFilters().addAll(extensionFilters);
        FILE_CHOOSER.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        return FILE_CHOOSER.showOpenDialog(null);
    }

    public static File saveFile(String initialFileName, FileChooser.ExtensionFilter... extensionFilters) {
        FILE_CHOOSER.setTitle("");//"Save File");
        FILE_CHOOSER.setInitialFileName(initialFileName);
        FILE_CHOOSER.getExtensionFilters().clear();
        FILE_CHOOSER.getExtensionFilters().addAll(extensionFilters);
        FILE_CHOOSER.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        return FILE_CHOOSER.showSaveDialog(null);
    }

    private static void initializeLogSystem() throws IOException {
        final Stream<Path> files = Files.list(FileTree.getLogPath());
        files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().contains(logFileNameRaw))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Unable to delete file '" + path + "':");
                        e.printStackTrace();
                    }
                });
        files.close();

        try {
            logFile = FileTree.getLogPath().resolve(logFileNameRaw + ".log").toFile();
            logStream = new PrintStream(logFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getLogFile() {
        return logFile;
    }

    public static void info(String tag, String message) {
        logger.info(tag, message);
    }

    public static void warning(String tag, String message) {
        logger.warning(tag, message);
    }

    public static void error(String tag, Throwable throwable) {
        logger.error(tag, throwable);
    }

    public static void error(String tag, String message) {
        logger.error(tag, message);
    }

    public static void error(String tag, String message, Throwable throwable) {
        logger.error(tag, message, throwable);
    }

    public static void fatal(String tag, Throwable throwable) {
        logger.fatal(tag, throwable);
    }

    public static void fatal(String tag, String message) {
        logger.fatal(tag, message);
    }

    public static void fatal(String tag, String message, Throwable throwable) {
        logger.fatal(tag, message, throwable);
    }

    public static void debug(String tag, String message) {
        logger.debug(tag, message);
    }
}
