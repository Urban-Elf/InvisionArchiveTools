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
import com.urbanelf.iat.content.model.Meta;
import com.urbanelf.iat.content.model.RefreshSpec;
import com.urbanelf.iat.content.parser.ArchiveParseException;
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.content.parser.ParserDispatcher;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.UIUtils;
import com.urbanelf.iat.util.function.TriConsumer;

import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class ArchiveRecoverer {
    private static final String TAG = ArchiveRecoverer.class.getSimpleName();

    public static void processOrphanedArchives(JFrame frame, Path path, @Nonnull TriConsumer<CompletableFuture<Void>, RefreshSpec, Path> loadableConsumer, @Nullable Consumer<Boolean> onCompleted) {
        //AtomicBoolean orphansSkipped = new AtomicBoolean();
        // ^ return this to determine whether a worker can be launched from the buttons

        // Handle unexported archives

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                AtomicBoolean recoveredArchive = new AtomicBoolean(false);

                try {
                    final List<Path> paths;
                    try (Stream<Path> walk = Files.walk(path)) {
                        paths = walk
                                .filter(Files::isDirectory)
                                .sorted(Comparator.reverseOrder())
                                .toList();
                    }

                    final Iterator<Path> iterator = paths.iterator();

                    while (iterator.hasNext() && !isCancelled()) {
                        final Path nextPath = iterator.next();

                        if (!ParserDispatcher.validateArchive(nextPath)) {
                            try {
                                if (FileTree.isEmptyDirectory(nextPath)) {
                                    Files.delete(nextPath);
                                    Core.info(TAG, "Archive '" + nextPath.getFileName() + "' is totally empty; deleting");
                                }
                            } catch (IOException e) {
                                Core.error(TAG, "Error deleting empty archive", e);
                            }
                            continue;
                        }

                        Core.info(TAG, "Processing unexported archive '" + nextPath.getFileName() + "'");

                        // Analyze meta
                        AtomicReference<String> metaMessage = new AtomicReference<>();
                        Meta meta = null;

                        try {
                            meta = ParserDispatcher.parseGenericMetadata(nextPath, false);
                        } catch (IOException | JSONException e) {
                            Core.warning(TAG, "Metadata failed to load; omitting in description");
                        }

                        if (meta != null) {
                            metaMessage.set("Title: \"" + meta.getTitle() + "\"\n"
                                    + "Type: " + meta.getContentType().getTitle());
                        }

                        CompletableFuture<Integer> resultTask = new CompletableFuture<>();

                        UIUtils.executeOnSwingThread(() -> {
                            Object[] options = {"Delete", "Skip", "Recover"};
                            int result = JOptionPane.showOptionDialog(
                                    frame,
                                    "An unexported archive has been detected, do you want to recover it?\n\n"
                                            + metaMessage.get(),
                                    "Recovery",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null,
                                    options,
                                    options[2]);
                            resultTask.complete(result);
                        });

                        int result;

                        try {
                            result = resultTask.get();
                        } catch (InterruptedException | ExecutionException e) {
                            Core.error(TAG, "Error fetching option dialog result", e);
                            continue;
                        }

                        if (result == 2) {
                            boolean recovery = ParserDispatcher.isArchiveLoadable(nextPath);

                            ContentSpec spec = null;

                            try {
                                spec = ParserDispatcher.process(nextPath, recovery);
                                Core.info(TAG, "Content spec processed successfully.");
                            } catch (IOException | JSONException | ArchiveParseException e) {
                                Core.error(TAG, "Error parsing archive '" + nextPath.getFileName().toString() + (recovery ? "' (refresh_spec load mode)" : ""), e);
                                CompletableFuture<Void> errorFuture = showErrorAndDisposeArchive(frame, nextPath, recovery);
                                try {
                                    errorFuture.get();
                                } catch (InterruptedException | ExecutionException ignored) {
                                }
                            }

                            if (spec != null) {
                                CompletableFuture<Void> future = new CompletableFuture<>();

                                if (recovery) {
                                    // Recovery mode (load archive from stored state)
                                    RefreshSpec refreshSpec = spec.refreshSpec();
                                    loadableConsumer.accept(future, refreshSpec, nextPath);
                                } else {
                                    // Export archive
                                    Core.exportArchive(frame, spec, e -> {
                                        future.complete(null);
                                    });
                                }

                                // Wait for consumer to finish
                                try {
                                    future.get();
                                } catch (InterruptedException | ExecutionException ignored) {
                                }

                                // Indicate at least one action was performed in this entire worker loop
                                recoveredArchive.set(true);
                            }
                        } else if (result == 0) {
                            try {
                                FileTree.deleteRecursively(nextPath);
                                Core.info(TAG, "Deleted archive '" + nextPath.getFileName().toString() + "'");
                            } catch (IOException e2) {
                                Core.error(TAG, "Failed to delete archive '" + nextPath.getFileName().toString() + "'", e2);
                            }
                        }
                    }

                } catch (IOException e2) {
                    Core.error(TAG, "An error occurred while attempting to traverse export path", e2);
                }

                return recoveredArchive.get();
            }

            @Override
            protected void done() {
                if (onCompleted != null) {
                    try {
                        onCompleted.accept(get());
                    } catch (InterruptedException | ExecutionException e) {
                        Core.error(TAG, "Error obtaining boolean result of recovery worker", e);
                        onCompleted.accept(true);
                    }
                }
                Core.info(TAG, "No remaining archives to recover.");
            }
        };
        worker.execute();
    }

    private static CompletableFuture<Void> showErrorAndDisposeArchive(JFrame parent, Path path, boolean recovery) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        UIUtils.executeOnSwingThread(() -> {
            JOptionPane.showMessageDialog(parent,
                    "Could not load archive" + (recovery ? " in recovery mode" : "") + " (see log for details):" + "\n\n"
                            + path.getFileName().toString(),
                    "An error occurred",
                    JOptionPane.WARNING_MESSAGE);

            // Move corrupted archive to a safe location
            Path dstPath = FileTree.getCorruptedExportsPath().resolve(path.getFileName());
            Path dstPathSafe = FileTree.resolveDestinationDirectory(dstPath);
            try {
                Files.move(path, dstPathSafe);
                Core.info(TAG, "Moved corrupted archive '" + path.getFileName() + "' to destination path " + dstPathSafe);
                future.complete(null);
            } catch (IOException e) {
                Core.error(TAG, "Failed to move corrupted archive at " + path + " to destination path " + dstPathSafe, e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
