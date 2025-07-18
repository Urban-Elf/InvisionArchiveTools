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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.urbanelf.iat.Core;
import com.urbanelf.iat.Version;
import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.IC4;
import com.urbanelf.iat.ic.IC5;
import com.urbanelf.iat.proto.ClientPacket;
import com.urbanelf.iat.proto.PythonServer;
import com.urbanelf.iat.proto.constants.ClientSA;
import com.urbanelf.iat.proto.constants.WorkerType;
import com.urbanelf.iat.ui.component.JLinkLabel;
import com.urbanelf.iat.util.DateUtils;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.LocalStorage;
import com.urbanelf.iat.util.PlatformUtils;
import com.urbanelf.iat.util.ThemeManager;
import com.urbanelf.iat.util.UIUtils;
import com.urbanelf.iat.util.URLUtils;

import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class MainFrame extends JFrame {
    private static final String TAG = MainFrame.class.getSimpleName();

    private static final String LS_COMMUNITY_URLS = "community_urls";

    private static final Dimension ICON_BUTTON_SIZE = new Dimension(28, 28);

    private final int spacing;
    private final int spacingSecondary;
    private final ArrayList<FlatSVGIcon> managedIcons;
    private final DefaultListModel<IC> communityListModel;
    private final DefaultComboBoxModel<IC> currentCommunityModel;
    private final JComboBox<IC> currentCommunity;

    private IC currentIc;
    private ArrayList<JButton> workerButtons;
    private WorkerFrame currentWorker;

    public MainFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Invision Archive Tools");

        // Create the menu bar
        final JMenuBar menuBar = createMenuBar();

        // Set the menu bar on the frame
        setJMenuBar(menuBar);

        setMinimumSize(new Dimension(762, 436));
        setSize(new Dimension(904, 486));
        setLocationByPlatform(true);

        managedIcons = new ArrayList<>();
        communityListModel = new DefaultListModel<>();
        currentCommunityModel = new DefaultComboBoxModel<>();
        currentCommunity = new JComboBox<>(currentCommunityModel);
        currentCommunity.addActionListener(actionEvent -> {
            final IC ic = (IC) currentCommunity.getSelectedItem();
            if (ic != null)
                currentIc = ic;
        });
        workerButtons = new ArrayList<>();

        spacing = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 6 : 12;
        spacingSecondary = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 4 : 7;

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(spacing, spacing + 2, spacing, spacing + 2));

        final JPanel centerPanel = createCenterPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 1.0f; // Allows it to take up space
        gbc.weighty = 1.0f; // Allows it to take up space
        panel.add(centerPanel, gbc);//, BorderLayout.CENTER);
        //panel.add(eastPanel, BorderLayout.EAST);

        setContentPane(panel);

        refreshIcons();

        // Deserialize persistent data
        try {
            LocalStorage.getJsonObject().getJSONArray(LS_COMMUNITY_URLS).forEach(object -> {
                try {
                    if (object instanceof JSONObject jsonObject) {
                        // Ignore malformed URLs
                        if (jsonObject.get("url") instanceof String url && URLUtils.isURLHTTP(url)) {
                            communityListModel.addElement(Objects.requireNonNull(Version.ofInteger((int) jsonObject.get("version"))).getIcClass()
                                    .getDeclaredConstructor(String.class).newInstance(URLUtils.normalizeURLString(url)));
                        }
                    }
                } catch (Exception e) {
                    Core.error(TAG, "Error while attempting to restore persistent data", e);
                }
            });
        } catch (Exception e) {
            Core.error(TAG, "Error while attempting to restore persistent data", e);
        }

        communityListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                LocalStorage.getJsonObject().put(LS_COMMUNITY_URLS, createJsonObjects());
                LocalStorage.serialize();
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                LocalStorage.getJsonObject().put(LS_COMMUNITY_URLS, createJsonObjects());
                LocalStorage.serialize();
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
                LocalStorage.getJsonObject().put(LS_COMMUNITY_URLS, createJsonObjects());
                LocalStorage.serialize();
            }

            private JSONObject[] createJsonObjects() {
                final JSONObject[] jsonArray = new JSONObject[communityListModel.getSize()];
                final Object[] listArray = communityListModel.toArray();
                for (int i = 0; i < communityListModel.size(); i++) {
                    final IC ic = ((IC)listArray[i]);
                    jsonArray[i] = new JSONObject()
                            .put("url", ic.getRootUrl())
                            .put("version", ic.getVersionNumber());
                }
                return jsonArray;
            }
        });
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        // Session
        final JMenu sessionMenu = new JMenu("Session");
        sessionMenu.add(new JMenuItem("Quit") {
            {
                addActionListener(e -> System.exit(0));
            }
        });
        menuBar.add(sessionMenu);

        // Help
        final JMenu helpMenu = new JMenu("Help");

        // Create menu items
        final JMenuItem reportBugItem = new JMenuItem("Report Bug");
        final JMenuItem aboutItem = new JMenuItem("About");

        // "Report Bug" section
        final JPanel reportBugPanel = new JPanel();
        reportBugPanel.setLayout(new BoxLayout(reportBugPanel, BoxLayout.Y_AXIS));
        reportBugPanel.add(new JLabel("<html>If there's an issue you've encountered, please report it to the developer" +
                "<br>via one of the methods listed below."));
        reportBugPanel.add(Box.createVerticalStrut(10)); // spacing
        reportBugPanel.add(new JLabel("<html>Please attach or concatenate the relevant log files with the message." +
                "<br>If you have any questions, feel free to ask in advance!</html>"));
        reportBugPanel.add(Box.createVerticalStrut(12)); // spacing
        reportBugPanel.add(new JLabel("<html>  - Email:</html>"));
        reportBugPanel.add(Box.createVerticalStrut(8)); // spacing
        reportBugPanel.add(new JLinkLabel("mailto:" + Core.DEVELOPER_EMAIL, "mailto:" + Core.DEVELOPER_EMAIL + "?subject=Bug%20Report"));
        reportBugPanel.add(Box.createVerticalStrut(8)); // spacing
        reportBugPanel.add(new JLabel("<html>  - Catholic Harbor:</html>"));
        reportBugPanel.add(Box.createVerticalStrut(8)); // spacing
        reportBugPanel.add(new JLinkLabel("https://www.catholicharbor.com/messenger/compose/?to=7190"));
        reportBugPanel.add(Box.createVerticalStrut(4)); // spacing

        // Add event listeners
        reportBugItem.addActionListener(e -> {
            // Custom button labels
            String[] options = {"Open Logs Directory", "Close"};

            SwingUtilities.updateComponentTreeUI(reportBugPanel);
            reportBugPanel.revalidate();
            reportBugPanel.repaint();

            int result = JOptionPane.showOptionDialog(
                    MainFrame.this,
                    reportBugPanel,
                    "Report a Bug",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (result == 0) {
                try {
                    Desktop.getDesktop().open(FileTree.getLogPath().toFile());
                } catch (Exception ex) {
                    Core.error(TAG, "Failed to open log directory", ex);
                }
            } else {
            }
        });

        // "About" Panel
        final JPanel aboutPanel = createAboutPanel();

        aboutItem.addActionListener(e -> {
            String[] options = {"Close"};

            SwingUtilities.updateComponentTreeUI(aboutPanel);
            aboutPanel.revalidate();
            aboutPanel.repaint();

            JOptionPane.showOptionDialog(
                    MainFrame.this,
                    aboutPanel,
                    "About",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );
        });

        // Add items to the menu
        helpMenu.add(reportBugItem);
        helpMenu.add(aboutItem);

        // Add the menu to the menu bar
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createAboutPanel() {
        // "About" tab
        final JPanel aboutPanel = new JPanel(new GridBagLayout());

        aboutPanel.add(Box.createVerticalStrut(6), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
            }
        }); // spacing
        aboutPanel.add(new JLabel(new FlatSVGIcon("icon-shadow.svg", 134, 134)) {
            {
                setMaximumSize(getPreferredSize());
            }
        }, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
            }
        });
        aboutPanel.add(Box.createVerticalStrut(20), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 2;
            }
        }); // spacing
        aboutPanel.add(new JLabel("Invision Archive Tools v" + com.urbanelf.iat.Version.VERSION) {
            {
                setMaximumSize(getPreferredSize());
            }
        }, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 3;
            }
        });
        aboutPanel.add(Box.createVerticalStrut(16), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 4;
            }
        }); // spacing
        final String year = DateUtils.year();
        aboutPanel.add(new JLabel("© Copyright " + (year.equals("2025") ? year : "2025-" + year) + " Mark \"Urban-Elf\" Fisher"), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 5;
            }
        });
        aboutPanel.add(Box.createVerticalStrut(20), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 6;
            }
        }); // spacing
        aboutPanel.add(new JLabel("This program is provided with absolutely no warranty."), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 7;
            }
        });
        aboutPanel.add(Box.createVerticalStrut(14), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 8;
            }
        }); // spacing
        aboutPanel.add(new JPanel() {
            {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                add(new JLabel("See the "));
                //add(Box.createHorizontalStrut(4));
                add(new JLinkLabel("GNU General Public License, version 3 or later", "https://www.gnu.org/licenses/"));
                //add(Box.createHorizontalStrut(4));
                add(new JLabel(" for details."));
            }
        }, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 9;
            }
        });

        // "Credits" tab
        final JPanel creditsPanel = new JPanel(new GridBagLayout());
        creditsPanel.add(Box.createVerticalStrut(6), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
            }
        }); // spacing

        final JLabel credits = new JLabel("""
                <html>
                <br>
                <b>Core Development</b><br><br>
                  - Mark "Urban-Elf" Fisher
                <br><br><br>
                
                <b>UI/UX Design</b>
                <br><br>
                  - Mark "Urban-Elf" Fisher
                <br><br><br>
                
                <b>UI Themes</b>
                <br><br>
                  - FlatLaF Project
                <br><br><br>
                
                <b>Asset Attributions</b>
                <br><br>
                  - "envelope-solid.svg": FontAwesome<br>
                  - "comment-solid.svg": FontAwesome<br>
                  - "comments-solid-scaled.svg": FontAwesome<br>
                  - "book-open-reader-solid.svg": FontAwesome<br>
                  - "plus-solid.svg": FontAwesome<br>
                  - "trash-solid.svg": FontAwesome<br>
                  - "sun-solid.svg": FontAwesome<br>
                  - "moon-solid.svg": FontAwesome<br><br>
                  
                  - "drill.svg" (app icon): iconify.design<br>
                  - "archive.svg" (app icon): iconify.design<br>
                <br>
                </html>
                """, JLabel.CENTER);

        creditsPanel.add(new JScrollPane(credits) {
            {
                UIUtils.height(this, 350);
            }
        }, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                this.weightx = 1;
                this.fill = GridBagConstraints.BOTH;
            }
        });

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("About", aboutPanel);
        tabbedPane.addTab("Credits", creditsPanel);

        final JPanel wrapper = new JPanel(new GridLayout(1, 0));
        wrapper.add(tabbedPane);

        return wrapper;
    }

    public void refreshIcons() {
        managedIcons.forEach(icon -> {
            final Color lafTextColor = UIManager.getColor("Button.foreground");
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> lafTextColor));
        });
    }

    private void setCurrentWorker(WorkerFrame frame) {
        currentWorker = frame;
        workerButtons.forEach(button -> button.setEnabled(frame == null));
    }

    private JPanel createCenterPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        panel.add(new JPanel(new BorderLayout()) {
            {
                add(new JLabel("Choose content to archive...") {
                    {
                        //setFont(getFont().deriveFont(Font.BOLD, getFont().getSize()));
                    }
                }, BorderLayout.WEST);
                final JButton iconButton = createToggleIconButton(new FlatSVGIcon[] {
                        new FlatSVGIcon("icons/moon-solid.svg", 10, 14),
                        new FlatSVGIcon("icons/sun-solid.svg", 14, 14)
                    }, ThemeManager.isDark() ? 1 : 0);
                iconButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent mouseEvent) {
                        ThemeManager.toggleTheme(MainFrame.this::refreshIcons);
                    }
                });
                iconButton.setToolTipText("Toggle theme");
                add(iconButton, BorderLayout.EAST);
            }
        }, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
                final int inset = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 8 : 6;
                this.insets.set(0, inset, 0, 0);
            }
        });

        panel.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
                final int inset = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 3 : 1;
                this.insets.set(8, inset, 12, inset);
            }
        });

        final JPanel buttonsPanel = new JPanel(new GridBagLayout());

        final Component messengerButton = createContentButton(WorkerType.MESSENGER_WORKER, "Messenger", "Private conversations among community members.",
                "icons/envelope-solid.svg");
        final Component topicButton = createContentButton(WorkerType.TOPIC_WORKER, "Topic", "Public discussion threads within a forum.",
                "icons/comment-solid.svg");
        final Component forumButton = createContentButton(WorkerType.FORUM_WORKER, "Forum", "Organized collections of topics.",
                "icons/comments-solid-scaled.svg");
        final Component blogButton = createContentButton(WorkerType.BLOG_WORKER, "Blog", "Public articles by individuals or groups.",
                "icons/book-open-reader-solid.svg");

        final int inset = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 4 : 8;

        buttonsPanel.add(messengerButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        buttonsPanel.add(topicButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                this.weightx = 0.5f;
                this.insets.set(inset, 0, inset, 0);
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        buttonsPanel.add(forumButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 2;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        buttonsPanel.add(blogButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 3;
                this.weightx = 0.5f;
                this.insets.set(inset, 0, 0, 0);
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        panel.add(buttonsPanel, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 2;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        communityListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                updateButtonStates();
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                updateButtonStates();
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
            }

            private void updateButtonStates() {
                final boolean enabled = !communityListModel.isEmpty();
                messengerButton.setEnabled(enabled);
                topicButton.setEnabled(enabled);
                forumButton.setEnabled(enabled);
                blogButton.setEnabled(enabled);
            }
        });

        final JPanel panelWrapper = new JPanel();
        panelWrapper.setLayout(new BoxLayout(panelWrapper, BoxLayout.X_AXIS));
        UIUtils.minWidth(panelWrapper, 850);
        UIUtils.prefWidth(panelWrapper, 1200);
        UIUtils.maxWidth(panelWrapper, 1200);
        panelWrapper.add(panel);
        panelWrapper.add(Box.createHorizontalStrut(spacing));
        panelWrapper.add(createEastPanel());

        return panelWrapper;
    }

    private JButton createContentButton(WorkerType type, String name, String description, String icon) {
        final JButton button = new JButton("<html><b>" + name + "</b><br>" + description + "</html>");
        UIUtils.minWidth(button, 500);
        //UIUtils.prefWidth(button, 350);
        UIUtils.height(button, 70);
        final FlatSVGIcon messengerIcon = new FlatSVGIcon(icon, 36, 36);
        managedIcons.add(messengerIcon);
        button.setIcon(messengerIcon);
        button.setIconTextGap(18);
        button.setHorizontalAlignment(JButton.LEFT);
        // Add to managed buttons
        workerButtons.add(button);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (!button.isEnabled())
                    return;
                setCurrentWorker(new WorkerFrame(currentIc, type) {
                    {
                        addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosed(WindowEvent windowEvent) {
                                setCurrentWorker(null);
                            }
                        });
                        setVisible(true);
                    }
                });
            }
        });
        return button;
    }

    private JPanel createEastPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(350, 0));

        panel.add(createCommunitiesPanel(), new GridBagConstraints() {
            {
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        panel.add(createSettingsPanel(), new GridBagConstraints() {
            {
                this.gridy = 1;
                this.weightx = 0.5f;
                this.insets.set(8, 0, 0, 0);
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        //final JPanel panelWrapper = new JPanel(new GridBagLayout());
        //panelWrapper.add(panel, new GridBagConstraints() {{ this.weightx = 0.5f; this.fill = GridBagConstraints.HORIZONTAL; this.anchor = GridBagConstraints.PAGE_START; }});
        return panel;
    }

    private JPanel createCommunitiesPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(spacingSecondary, spacingSecondary, spacingSecondary, spacingSecondary));

        final JList<IC> list = new JList<>(communityListModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setFixedCellHeight(24);
        final JScrollPane listScroller = new JScrollPane(list) {
            final Dimension SIZE = new Dimension(300, 200);

            @Override
            public Dimension getMinimumSize() {
                return SIZE;
            }

            @Override
            public Dimension getPreferredSize() {
                return SIZE;
            }

            @Override
            public Dimension getMaximumSize() {
                return SIZE;
            }
        };

        panel.add(listScroller, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 1.0;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        /////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////// FIELD BUTTONS ///////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////

        final JPanel fieldButtonsPanel = new JPanel();
        fieldButtonsPanel.setLayout(new BoxLayout(fieldButtonsPanel, BoxLayout.X_AXIS));

        // Add button
        final JButton addButton = createIconButton("icons/plus-solid.svg", 14, 14);
        addButton.setEnabled(false);
        final JLabel errorLabel = new JLabel("Not a valid URL") {
            {
                setFont(Fonts.SMALL);
                setForeground(Colors.ERROR_RED);
                setHorizontalTextPosition(JLabel.LEFT);
            }
        };
        errorLabel.setVisible(false);
        final JTextField field = new JTextField();
        final JComboBox<Version> version = new JComboBox<>(Version.values());
        version.setSelectedItem(Version.v4);

        final Consumer<String> addElementConsumer = url -> {
            IC ic;
            try {
                ic = ((Version) Objects.requireNonNull(version.getSelectedItem())).getIcClass()
                        .getDeclaredConstructor(String.class).newInstance(URLUtils.normalizeURLString(field.getText()));
            } catch (Exception e) {
                Core.error(TAG, "Failed to construct IC", e);
                return;
            }
            communityListModel.addElement(ic);
            field.setText("");
            errorLabel.setVisible(false);
        };

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtonState();
            }

            private void updateButtonState() {
                final boolean isURLValid = URLUtils.isURLHTTP(field.getText());
                addButton.setEnabled(!field.getText().isBlank() && isURLValid);
                errorLabel.setVisible(!isURLValid);
            }
        });
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER && !field.getText().isBlank()
                    && URLUtils.isURLHTTP(field.getText())) {
                    addElementConsumer.accept(field.getText());
                }
            }
        });
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (!addButton.isEnabled() || field.getText().isBlank() || !URLUtils.isURLHTTP(field.getText()))
                    return;
                addElementConsumer.accept(field.getText());
            }
        });
        final JButton removeButton = createIconButton("icons/trash-solid.svg", 12, 14);
        removeButton.setEnabled(false);
        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (!removeButton.isEnabled())
                    return;
                list.getSelectedValuesList().forEach(communityListModel::removeElement);
            }
        });
        communityListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                updateCurrentModel();
                removeButton.setEnabled(!communityListModel.isEmpty());
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                updateCurrentModel();
                removeButton.setEnabled(!communityListModel.isEmpty());
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
                updateCurrentModel();
            }

            private void updateCurrentModel() {
                currentCommunityModel.removeAllElements();
                communityListModel.elements().asIterator().forEachRemaining(currentCommunityModel::addElement);
            }
        });

        final int spacing = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 3 : 6;

        fieldButtonsPanel.add(field);
        fieldButtonsPanel.add(Box.createHorizontalStrut(spacing));
        fieldButtonsPanel.add(version);
        fieldButtonsPanel.add(Box.createHorizontalStrut(spacing));
        fieldButtonsPanel.add(addButton);
        fieldButtonsPanel.add(Box.createHorizontalStrut(spacing));
        fieldButtonsPanel.add(new JSeparator(JSeparator.VERTICAL));
        fieldButtonsPanel.add(Box.createHorizontalStrut(spacing));
        fieldButtonsPanel.add(removeButton);

        /*fieldButtonsPanel.add(field, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.weightx = 0.5;
                this.ipady = 16;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        fieldButtonsPanel.add(version, new GridBagConstraints() {
            {
                this.gridx = 1;
                this.insets.set(0, 4, 0, 0);
            }
        });
        fieldButtonsPanel.add(addButton, new GridBagConstraints() {
            {
                this.gridx = 2;
                this.insets.set(0, 4, 0, 4);
                this.fill = GridBagConstraints.VERTICAL;
            }
        });
        fieldButtonsPanel.add(new JSeparator(JSeparator.VERTICAL), new GridBagConstraints() {{ this.gridx = 3; this.weighty = 0.5; this.fill = GridBagConstraints.VERTICAL; }});
        fieldButtonsPanel.add(removeButton, new GridBagConstraints() {
            {
                this.gridx = 4;
                this.insets.set(0, 4, 0, 0);
                this.fill = GridBagConstraints.NONE;
            }
        });*/

        panel.add(fieldButtonsPanel, new GridBagConstraints() {
            {
                this.gridy = 1;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
                final int inset = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac ? 8 : 10;
                this.insets.set(inset, 0, 6, 0);
            }
        });

        panel.add(errorLabel, new GridBagConstraints() {
            {
                this.gridy = 2;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        final JPanel panelWrapper = new JPanel(new GridBagLayout());
        panelWrapper.setBorder(BorderFactory.createTitledBorder("  Invision Communities  "));
        panelWrapper.add(panel, new GridBagConstraints() {{ this.weightx = 0.5f; this.fill = GridBagConstraints.BOTH; }});

        return panelWrapper;
    }

    private JButton createToggleIconButton(FlatSVGIcon[] icons, int initialIndex) {
        return createToggleIconButton(icons, null, initialIndex);
    }

    private JButton createToggleIconButton(FlatSVGIcon[] icons, Runnable[] runnables, int initialIndex) {
        managedIcons.addAll(Arrays.asList(icons));
        final JButton button = new JButton(icons[initialIndex]) {
            @Override
            public Dimension getMaximumSize() {
                return ICON_BUTTON_SIZE;
            }
            @Override
            public Dimension getMinimumSize() {
                return ICON_BUTTON_SIZE;
            }
            @Override
            public Dimension getPreferredSize() {
                return ICON_BUTTON_SIZE;
            }
        };
        button.addMouseListener(new MouseAdapter() {
            private int index = initialIndex;

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                index++;
                index = index % icons.length;
                button.setIcon(icons[index]);
                if (runnables != null)
                    runnables[index].run();
            }
        });
        return button;
    }

    private JButton createIconButton(String iconPath, int iconWidth, int iconHeight) {
        final FlatSVGIcon icon = new FlatSVGIcon(iconPath, iconWidth, iconHeight);
        managedIcons.add(icon);
        final JButton button = new JButton(icon) {
            @Override
            public Dimension getMaximumSize() {
                return ICON_BUTTON_SIZE;
            }
            @Override
            public Dimension getMinimumSize() {
                return ICON_BUTTON_SIZE;
            }
            @Override
            public Dimension getPreferredSize() {
                return ICON_BUTTON_SIZE;
            }
        };
        return button;
    }

    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(spacingSecondary, spacingSecondary, spacingSecondary, spacingSecondary));

        final JLabel errorLabel = new JLabel("No community selected.  Add one to begin archiving.") {
            {
                setFont(Fonts.SMALL);
                setForeground(Colors.ERROR_RED);
                setHorizontalTextPosition(JLabel.LEFT);
            }
        };
        //errorLabel.setVisible(false);

        currentCommunityModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                errorLabel.setVisible(currentCommunityModel.getSize() < 1);
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                errorLabel.setVisible(currentCommunityModel.getSize() < 1);
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
            }
        });

        final Dimension size = currentCommunity.getPreferredSize();
        size.height = 28;
        currentCommunity.setPreferredSize(size);
        panel.add(currentCommunity);
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4)) {
            {
                add(errorLabel);
            }
        });

        final JPanel panelWrapper = new JPanel();
        panelWrapper.setLayout(new BoxLayout(panelWrapper, BoxLayout.X_AXIS));
        panelWrapper.setBorder(BorderFactory.createTitledBorder("  Active Community  "));
        panelWrapper.add(panel);

        return panelWrapper;
    }

    private enum Version {
        v4(IC4.class),
        v5(IC5.class);

        final Class<? extends IC> icClass;

        static Version ofInteger(int version) {
            switch (version) {
                case 4 -> {
                    return v4;
                }
                case 5 -> {
                    return v5;
                }
            }
            return null;
        }

        Version(Class<? extends IC> icClass) {
            this.icClass = icClass;
        }

        public Class<? extends IC> getIcClass() {
            return icClass;
        }
    }
}
