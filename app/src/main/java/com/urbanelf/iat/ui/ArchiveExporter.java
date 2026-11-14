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

package com.urbanelf.iat.ui;

import com.urbanelf.iat.Core;
import com.urbanelf.iat.content.ArchiveFormat;
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.content.writer.WriterDispatcher;
import com.urbanelf.iat.content.writer.WriterTracker;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.StringUtils;
import com.urbanelf.iat.util.UIUtils;

import org.openqa.selenium.json.JsonException;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class ArchiveExporter {
    private static final String TAG = ArchiveExporter.class.getSimpleName();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private void showCancelledDialog(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
                "Operation cancelled.\n\n(Hint: You can always restart IAT to recover unexported archives.)",
                "Export Archive...",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void exportArchive(JFrame parent, ContentSpec spec, Consumer<ExportResult> onCompletion) {
        exportArchive(parent, spec, false, onCompletion);
    }

    /** Runs on the Swing EDT. */
    public void exportArchive(JFrame parent, ContentSpec spec, boolean destroyOnFinish, @Nonnull Consumer<ExportResult> onCompletion) {
        // Put in runnable for convenient thread switching (see bottom of method)

        Runnable exportArchiveRunnable = () -> {
            // Custom button labels
            String[] options = {"Cancel", "OK"};

            ExportDialogPanel panel = new ExportDialogPanel();
            int result = JOptionPane.showOptionDialog(
                    parent,
                    panel,
                    "Export archive...",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[1]
            );

            if (result == 0) {
                showCancelledDialog(parent);
            } else if (result == 1) {
                // Resolve format
                ArchiveFormat format = (ArchiveFormat) Objects.requireNonNull(panel.archiveFileFormat.getSelectedItem());

                // FX task
                CompletableFuture<File> dstTask = new CompletableFuture<>();

                // Obtain dst path on worker thread
                SwingWorker<File, Void> worker = new SwingWorker<>() {
                    @Override
                    protected File doInBackground() {
                        // Method is blocking, so make sure deadlock doesn't occur if a Swing file chooser has to show (fallback scenario)
                        return Core.saveFile("Save Archive", StringUtils.cleanFileName(spec.meta().getTitle()) + '.' + format.getExtension(),
                                format.getExtensionFilter());
                    }

                    @Override
                    protected void done() {
                        try {
                            // get() is blocking
                            dstTask.complete(get());
                        } catch (InterruptedException | ExecutionException e) {
                            dstTask.completeExceptionally(e);
                        }
                    }
                };
                worker.execute();

                // Writer worker
                executor.submit(() -> {
                    File file;

                    try {
                        file = dstTask.get();
                        // Exit if null
                        if (file == null) {
                            Core.info(TAG, "Export cancelled.");
                            onCompletion.accept(new ExportResult(ExportResult.Type.CANCELLED, null));
                            return;
                        }
                        Core.info(TAG, "Export destination: " + file.getAbsolutePath());
                    } catch (InterruptedException | ExecutionException e) {
                        onCompletion.accept(new ExportResult(ExportResult.Type.EXCEPTION, e));
                        return;
                    }

                    try {
                        Path dstPath = file.toPath();

                        final AtomicReference<Path> outputPath = new AtomicReference<>();

                        Runnable onProgressCompletion = () -> {
                            SwingUtilities.invokeLater(() -> {
                                // Notify success
                                Object[] options2 = {"Open Directory", "Close"};
                                int result2 = JOptionPane.showOptionDialog(
                                        parent,
                                        "Archive exported successfully.",
                                        "Export Finished",
                                        JOptionPane.DEFAULT_OPTION,
                                        JOptionPane.INFORMATION_MESSAGE,
                                        null,
                                        options2,
                                        options2[0]
                                );
                                if (result2 == 0) {
                                    try {
                                        Desktop.getDesktop().open(outputPath.get().toFile());
                                    } catch (IOException | NullPointerException e) {
                                        Core.error(TAG, "Failed to open output directory", e);
                                    }
                                }
                                // Destroy frame
                                if (parent != null && destroyOnFinish)
                                    parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));

                                // Report completion to original caller
                                onCompletion.accept(new ExportResult(ExportResult.Type.SUCCESS, null));
                            });
                        };

                        if (format == ArchiveFormat.JSON) {
                            // Edge case: copy archive folder (contents already stored as JSON)
                            Files.copy(spec.path(), dstPath);
                            outputPath.set(dstPath);
                            // This function must be run after outputPath is set
                            onProgressCompletion.run();
                        } else {
                            // Convert file
                            ProgressDialog dialog = new ProgressDialog(parent);
                            // This function must be run after outputPath is set
                            // (works here because onCompletion is invoked by WorkerTracker handled by Writer)
                            dialog.onCompletion(onProgressCompletion);
                            // Show dialog
                            SwingUtilities.invokeLater(() -> {
                                dialog.setVisible(true);
                                dialog.toFront();
                            });
                            // Write data
                            Path temp = WriterDispatcher.write(spec, dstPath, format, dialog.getTracker());
                            outputPath.set(temp);
                        }
                        Core.info(TAG, "Successfully exported archive (format: " + format.name() + ")");

                        // Delete archive folder (processing has finished successfully)
                        Path specPath = spec.path();
                        try {
                            FileTree.deleteRecursively(specPath);
                            Core.info(TAG, "Deleted '" + specPath.getFileName().toString() + "'");
                        } catch (IOException e) {
                            Core.error(TAG, "Failed to delete '" + specPath.toString() + "'", e);
                        }

                        // Finish task
                    } catch (IOException | JsonException e) {
                        Core.error(TAG, "Failed to export archive '" + spec.path().getFileName() + "' to " + file.getAbsolutePath(), e);
                        onCompletion.accept(new ExportResult(ExportResult.Type.EXCEPTION, e));
                    }
                });
            }
        };

        // Only runs on Swing thread
        UIUtils.executeOnSwingThread(exportArchiveRunnable);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public record ExportResult(Type type, @Nullable Throwable throwable) {
        public enum Type {
            SUCCESS,
            CANCELLED,
            EXCEPTION
        }
    }

    private static class ExportDialogPanel extends JPanel {
        final JComboBox<ArchiveFormat> archiveFileFormat;

        public ExportDialogPanel() {
            super(new GridBagLayout());

            final JPanel formatPanel = new JPanel();
            formatPanel.setLayout(new BoxLayout(formatPanel, BoxLayout.X_AXIS));
            archiveFileFormat = new JComboBox<>(ArchiveFormat.values());
            formatPanel.add(new JLabel("File Format: "));
            formatPanel.add(Box.createHorizontalStrut(4));
            formatPanel.add(archiveFileFormat);

            add(formatPanel, new GridBagConstraints() {
                {
                    this.gridx = 0;
                    this.gridy = 0;
                    this.weightx = 1;
                    this.fill = HORIZONTAL;
                }
            });
        }
    }

    public static class ProgressDialog extends JDialog {
        private final WriterTracker tracker;
        private Runnable onCompletion;

        public ProgressDialog(JFrame parent) {
            super(parent, "Export archive...", ModalityType.APPLICATION_MODAL);

            JLabel label = new JLabel("Preparing write...");
            label.setHorizontalAlignment(JLabel.CENTER);
            JProgressBar progressBar = new JProgressBar();
            tracker = new WriterTracker() {
                @Override
                public void maximumCalculated(long maximum) {
                    progressBar.setMaximum((int) maximum);
                }

                @Override
                public void progressChanged(long current) {
                    progressBar.setValue((int) current);
                }

                @Override
                public void messageUpdate(String message) {
                    UIUtils.executeOnSwingThread(() -> {
                        label.setText(message);
                    });
                }

                @Override
                public void completed() {
                    Timer timer = new Timer(550, e -> {
                        ProgressDialog.this.dispose();
                        if (onCompletion != null)
                            onCompletion.run();
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            };

            JPanel content = new JPanel(new BorderLayout(0, 12));
            content.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));
            content.add(label, BorderLayout.NORTH);
            content.add(progressBar, BorderLayout.CENTER);
            Dimension size = progressBar.getPreferredSize();
            progressBar.setPreferredSize(new Dimension(300, size.height));

            setContentPane(content);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setResizable(false);
            pack();
            setLocationRelativeTo(parent);
        }

        public WriterTracker getTracker() {
            return tracker;
        }

        public void onCompletion(Runnable onCompletion) {
            this.onCompletion = onCompletion;
        }
    }
}
