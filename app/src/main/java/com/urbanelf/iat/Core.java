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

import com.urbanelf.iat.ui.MainFrame;
import com.urbanelf.iat.ui.WorkerFrame;
import com.urbanelf.iat.util.PlatformUtils;

import java.io.File;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;

public class Core {
    private static FileChooser FILE_CHOOSER;

    public static void main(String[] args) {
        PlatformUtils.initialize();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        new JFXPanel(); // Initialize JavaFX
        Platform.runLater(() -> {
            FILE_CHOOSER = new FileChooser();
            FILE_CHOOSER.setInitialDirectory(new File(System.getProperty("user.home")));
            //saveFile();

            SwingUtilities.invokeLater(() -> {
                new MainFrame().setVisible(true);
                //new WorkerFrame(null).setVisible(true);
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

    public static File saveFile(FileChooser.ExtensionFilter... extensionFilters) {
        FILE_CHOOSER.setTitle("");//"Save File");
        FILE_CHOOSER.getExtensionFilters().clear();
        FILE_CHOOSER.getExtensionFilters().addAll(extensionFilters);
        FILE_CHOOSER.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        return FILE_CHOOSER.showSaveDialog(null);
    }
}
