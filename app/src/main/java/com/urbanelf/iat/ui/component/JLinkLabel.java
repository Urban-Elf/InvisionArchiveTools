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

package com.urbanelf.iat.ui.component;

import org.w3c.dom.Text;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class JLinkLabel extends JLabel {
    private final String linkText;

    public JLinkLabel(String url) {
        this(url, url);
    }

    public JLinkLabel(String text, String url) {
        this(text, toUri(url));
    }

    public JLinkLabel(String text, URI uri) {
        super(text);
        this.linkText = text;

        setForeground(UIManager.getColor("Component.linkColor"));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setText("<html><u>" + text + "</u></html>"); // initial render

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1)
                    return;
                if (uri != null && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(uri);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                JLinkLabel.this,
                                "Could not open link:\n" + uri,
                                "An error occurred",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });
    }

    private static URI toUri(String url) {
        try {
            return new URI(url);
        } catch (Exception e) {
            return null;
        }
    }
}
