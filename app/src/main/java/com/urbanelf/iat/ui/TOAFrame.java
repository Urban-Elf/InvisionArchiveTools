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

import com.urbanelf.iat.util.PlatformUtils;
import com.urbanelf.iat.util.UIUtils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

public class TOAFrame extends JFrame {
    public TOAFrame() {
        setTitle("IAT Terms of Agreement");
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final AtomicBoolean originContinue = new AtomicBoolean(false);

        final JButton continueButton = new JButton("Continue");
        continueButton.addActionListener(e -> {
            accepted();
            originContinue.set(true);
            dispatchEvent(new WindowEvent(TOAFrame.this, WindowEvent.WINDOW_CLOSING));
        });
        continueButton.setEnabled(false);

        final JCheckBox checkBox = new JCheckBox("On my honor as a child of God, I have read and agree to the terms above.");
        checkBox.addItemListener(e -> {
            final boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            continueButton.setEnabled(selected);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                if (!originContinue.get())
                    System.exit(0);
            }
        });

        // Custom content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final String termsHtml = "<html>" +
                " <br>" +
                " <b>Terms of Agreement</b><br>" +
                " <br>" +
                " <b>Effective Date:</b> 7/18/2025<br>" +
                " <b>Application:</b> Invision Archive Tools (\"IAT\", \"the App\")<br>" +
                " <br>" +
                " <i>By using this App, you agree to the following terms. If you do not agree, do not use this App.</i><br>" +
                " <br>" +
                " <b>1. Definition of \"Archive\"</b><br>" +
                " An <b>\"Archive\"</b> refers to any content, data, or material retrieved, saved, or reconstructed<br>" +
                " by the App from a third-party Invision Community (IC) platform. This includes but is not limited to<br>" +
                " posts, threads, user profiles, and private messages.<br>" +
                " <br>" +
                " <b>2. Permitted Use</b><br>" +
                " You are granted permission to use the App and any resulting Archives <b>solely for private,<br>" +
                " non-commercial purposes</b>. You may view and store Archives for personal reference or record-keeping.<br>" +
                " <br>" +
                " You may share or otherwise use an Archive <b>only</b> under one of the following conditions:<br>" +
                " - You have <b>explicit permission</b> from the Invision Community site administrator,<br>" +
                " <b>or</b><br>" +
                " - In the case of <b>private messages</b>, you have the <b>consent of all message participants</b>.<br>" +
                " <br>" +
                " <b>3. Prohibited Use</b><br>" +
                " You agree <b>not to use</b> the App or its Archives:<br>" +
                " - To <b>exploit, expose, or harass individuals</b>;<br>" +
                " - To <b>bypass, undermine, or evade</b> moderation actions, bans, deletions, or other enforcement<br>" +
                "   taken on the originating Invision Community site;<br>" +
                " - To <b>publicly repost, republish, or redistribute</b> content without the appropriate permissions;<br>" +
                " - In any way that would violate the <b>terms of service of the originating IC site</b>,<br>" +
                "   or any applicable laws or regulations.<br>" +
                " <br>" +
                " <b>4. Disclaimer</b><br>" +
                " This App is <b>not affiliated with or endorsed by Invision Community or its operators</b>.<br>" +
                " You use this App <b>at your own risk</b>. The developer is not responsible for any misuse of Archives,<br>" +
                " violations of third-party terms, or other outcomes resulting from the use of this App.<br>" +
                " <br>" +
                " <b>5. Changes</b><br>" +
                " These Terms may be updated at any time without prior notice. Continued use of the App after<br>" +
                " changes implies acceptance.<br>" +
                " <br>" +
                "</html>";

        final JLabel content = new JLabel(termsHtml, JLabel.CENTER);
        contentPanel.add(new JScrollPane(content) {
             {
                 UIUtils.height(this, 420);
             }
         });

        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(checkBox);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            dispatchEvent(new WindowEvent(TOAFrame.this, WindowEvent.WINDOW_CLOSING));
        });
        buttonPanel.add(closeButton);
        if (PlatformUtils.getRunningPlatform() != PlatformUtils.Platform.Mac)
            buttonPanel.add(Box.createHorizontalStrut(2));
        buttonPanel.add(continueButton);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // size to fit components
        setLocationRelativeTo(null); // center
    }

    protected void accepted() {
    }
}
