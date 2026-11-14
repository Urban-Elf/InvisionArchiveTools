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
import com.urbanelf.iat.util.UIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import javafx.application.Platform;
import javafx.stage.FileChooser;

public class CustomFileChooser {
    private static final String TAG = CustomFileChooser.class.getSimpleName();

    private static final int OPEN_MULTIPLE = 1;
    private static final int OPEN = 2;
    private static final int SAVE = 3;
    public static AtomicBoolean USE_SWING_FILE_CHOOSERS = new AtomicBoolean(true);

    private static void populateExtensionFilters(FileChooser fileChooser, FileChooser.ExtensionFilter[] extensionFilters) {
        if (extensionFilters != null && extensionFilters.length != 0) {
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().addAll(extensionFilters);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            fileChooser.setSelectedExtensionFilter(extensionFilters[0]);
        }
    }

    private static <T> T getOrLogResult(CompletableFuture<T> future, String errorMessage) {
        // Wait for result
        T result = null;
        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            Core.error(TAG, errorMessage, e);
        }
        return result;
    }

    public static File openFile(FileChooser.ExtensionFilter... extensionFilters) {
        return openFile("Open File", extensionFilters);
    }

    public static File openFile(String title, FileChooser.ExtensionFilter... extensionFilters) {
        if (USE_SWING_FILE_CHOOSERS.get()) {
            final List<File> result = showFileChooser(OPEN, title, System.getProperty("user.home"), null, extensionFilters);
            return !result.isEmpty() ? result.get(0) : null;
        }
        // JavaFX
        CompletableFuture<File> future = new CompletableFuture<>();
        Runnable openFileRunnable = () -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            populateExtensionFilters(fileChooser, extensionFilters);
            File result = fileChooser.showOpenDialog(null);
            future.complete(result);
        };
        // Ensure correct thread
        UIUtils.executeOnJavaFXThread(openFileRunnable);
        // Wait for result
        return getOrLogResult(future, "Failed to open file");
    }

    public static File saveFile(String initialFileName, FileChooser.ExtensionFilter... extensionFilters) {
        return saveFile("Save File", initialFileName, extensionFilters);
    }

    public static File saveFile(String title, String initialFileName, FileChooser.ExtensionFilter... extensionFilters) {
        if (USE_SWING_FILE_CHOOSERS.get()) {
            final List<File> result = showFileChooser(SAVE, title, System.getProperty("user.home"), initialFileName, extensionFilters);
            return !result.isEmpty() ? result.get(0) : null;
        }
        CompletableFuture<File> future = new CompletableFuture<>();
        Runnable saveFileRunnable = () -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            fileChooser.setInitialFileName(initialFileName);
            populateExtensionFilters(fileChooser, extensionFilters);
            File result = fileChooser.showSaveDialog(null);
            future.complete(result);
        };
        // Ensure correct thread
        UIUtils.executeOnJavaFXThread(saveFileRunnable);
        // Wait for result
        return getOrLogResult(future, "Failed to save file");
    }

    /* Opens a swing JFileChooser
     * returns:
     * if multiple selection mode the list of Files selected
     * if single selection mode a list with one File Object
     * if nothing selected or dialog is cancelled:
     *    an empty list
     */
    private static List<File> showFileChooser(
            int mode, String title, String defaultPath, String initialFileName,
            FileChooser.ExtensionFilter... extensionFilters) {

        CompletableFuture<List<File>> future = new CompletableFuture<>();

        Runnable showFileChooserRunnable = () -> {
            JFileChooser fileChooser = new JFileChooser(defaultPath);
            fileChooser.setDialogTitle(title);

            // Add filters
            if (extensionFilters != null) {
                fileChooser.setAcceptAllFileFilterUsed(false);
                for (FileChooser.ExtensionFilter fxFilter : extensionFilters) {
                    String desc = fxFilter.getDescription();
                    String[] exts = fxFilter.getExtensions().stream()
                            .map(ext -> ext.startsWith("*.") ? ext.substring(2) : ext) // ".json"
                            .toArray(String[]::new);
                    fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(desc, exts));
                }
                if (fileChooser.getChoosableFileFilters().length > 0) {
                    fileChooser.setFileFilter(fileChooser.getChoosableFileFilters()[0]);
                }
            }

            int res;
            if (mode == OPEN_MULTIPLE) {
                fileChooser.setMultiSelectionEnabled(true);
                res = fileChooser.showOpenDialog(null);
            } else if (mode == OPEN) {
                fileChooser.setMultiSelectionEnabled(false);
                res = fileChooser.showOpenDialog(null);
            } else {
                // SAVE mode → set default filename if provided
                if (initialFileName != null && !initialFileName.isEmpty()) {
                    fileChooser.setSelectedFile(new File(defaultPath, initialFileName));
                }
                res = fileChooser.showSaveDialog(null);
            }

            List<File> result = new ArrayList<>();
            if (res == JFileChooser.APPROVE_OPTION) {
                if (fileChooser.isMultiSelectionEnabled()) {
                    Collections.addAll(result, fileChooser.getSelectedFiles());
                } else {
                    result.add(fileChooser.getSelectedFile());
                }
            }

            future.complete(result);
        };

        // Ensure correct thread
        UIUtils.executeOnSwingThread(showFileChooserRunnable);

        // Wait for result
        return getOrLogResult(future, "Failed to get result for Swing chooser (mode: " + mode + ")");
    }
}
