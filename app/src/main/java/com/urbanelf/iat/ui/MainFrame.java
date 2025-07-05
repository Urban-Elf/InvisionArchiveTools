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

import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.IC4;
import com.urbanelf.iat.ic.IC5;
import com.urbanelf.iat.ic.workers.ICWorker;
import com.urbanelf.iat.ic.workers.ICWorkerStack;
import com.urbanelf.iat.ic.workers.ic4.MessengerWorker;
import com.urbanelf.iat.util.LocalStorage;
import com.urbanelf.iat.util.URLUtils;

import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class MainFrame extends JFrame {
    private static final String LS_COMMUNITY_URLS = "community_urls";

    private final ICWorkerStack<ICWorker<?>> workerStack;

    private final DefaultListModel<IC> communityListModel;
    private final DefaultComboBoxModel<IC> currentCommunityModel;
    private final JComboBox<IC> currentCommunity;

    public MainFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Invision Archive Tools");

        setSize(new Dimension(854, 480));
        setLocationByPlatform(true);

        workerStack = new ICWorkerStack<>(1); // TODO: increase concurrent count?

        communityListModel = new DefaultListModel<>();
        currentCommunityModel = new DefaultComboBoxModel<>();
        currentCommunity = new JComboBox<>(currentCommunityModel);
        currentCommunity.addActionListener(actionEvent -> {
            final IC ic = (IC) currentCommunity.getSelectedItem();
            if (ic != null)
                workerStack.setIC(ic);
        });

        final JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        final JPanel centerPanel = createCenterPanel();
        final JPanel eastPanel = createEastPanel();

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(eastPanel, BorderLayout.EAST);

        setContentPane(panel);

        // Deserialize persistent data
        try {
            LocalStorage.getJson().getJSONArray(LS_COMMUNITY_URLS).forEach(object -> {
                try {
                    if (object instanceof JSONObject jsonObject) {
                        // Ignore malformed URLs
                        if (jsonObject.get("url") instanceof String url && URLUtils.isURLHTTP(url)) {
                            communityListModel.addElement(Objects.requireNonNull(Version.ofInteger((int) jsonObject.get("version"))).getIcClass()
                                    .getDeclaredConstructor(String.class).newInstance(URLUtils.normalizeURLString(url)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // FIXME: log warning
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME: log warning
        }

        communityListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                LocalStorage.getJson().put(LS_COMMUNITY_URLS, createJsonObjects());
                LocalStorage.serialize();
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                LocalStorage.getJson().put(LS_COMMUNITY_URLS, createJsonObjects());
                LocalStorage.serialize();
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
                LocalStorage.getJson().put(LS_COMMUNITY_URLS, createJsonObjects());
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

    private JPanel createCenterPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        panel.add(new JLabel("Choose content to archive...") {
            {
                setFont(getFont().deriveFont(Font.BOLD, getFont().getSize()));
            }
        }, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
                this.insets.set(0, 8, 0, 0);
            }
        });

        panel.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                //this.weightx = 0.5f;
                //this.fill = GridBagConstraints.HORIZONTAL;
                this.insets.set(8, 0, 12, 0);
            }
        });

        final JPanel buttonsPanel = new JPanel(new GridBagLayout());

        final JButton messengerButton = new JButton("<html><b>Messenger</b><br>Private conversation among community members.</html>");
        final ImageIcon messengerIcon = new ImageIcon(ClassLoader.getSystemResource("messenger.png"));
        messengerButton.setIcon(messengerIcon);
        messengerButton.setIconTextGap(18);
        messengerButton.setHorizontalAlignment(JButton.LEFT);
        //messengerButton.setFocusable(false);

        messengerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                final MessengerWorker worker = new MessengerWorker(posts -> {});
                new WorkerFrame(worker).setVisible(true);
                workerStack.pushWorker(worker);
            }
        });

        final JButton topicButton = new JButton("<html><b>Topic</b><br>Public discussion thread within a forum.</html>",
                new ImageIcon(ClassLoader.getSystemResource("topic.png")));
        topicButton.setIconTextGap(18);
        topicButton.setHorizontalAlignment(JButton.LEFT);
        //topicButton.setFocusable(false);

        final JButton forumButton = new JButton("<html><b>Forum</b><br>Organized collection of topics.</html>",
                new ImageIcon(ClassLoader.getSystemResource("forum.png")));
        forumButton.setIconTextGap(18);
        forumButton.setHorizontalAlignment(JButton.LEFT);
        //forumButton.setFocusable(false);

        final JButton blogButton = new JButton("<html><b>Blog</b><br>Personal writings on ridiculous subjects for unclear reasons.</html>",
                new ImageIcon(ClassLoader.getSystemResource("blog.png")));
        blogButton.setIconTextGap(18);
        blogButton.setHorizontalAlignment(JButton.LEFT);
        //blogButton.setFocusable(false);

        buttonsPanel.add(messengerButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 0.5f;
                this.ipady = 20;
                this.insets.set(0, 0, 4, 0);
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        buttonsPanel.add(topicButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                this.weightx = 0.5f;
                this.ipady = 20;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        buttonsPanel.add(forumButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 2;
                this.weightx = 0.5f;
                this.ipady = 20;
                this.insets.set(4, 0, 4, 0);
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        buttonsPanel.add(blogButton, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 3;
                this.weightx = 0.5f;
                this.ipady = 20;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        panel.add(buttonsPanel, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 2;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.BOTH;
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

        return panel;
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
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final JList<IC> list = new JList<>(communityListModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
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
        //listScroller.setPreferredSize(new Dimension(120, 300));

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

        final JPanel fieldButtonsPanel = new JPanel(new GridBagLayout());
        final JButton addButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("add.png")));
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
                e.printStackTrace();
                // FIXME: Log error
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
        final JButton removeButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("remove.png")));
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

        fieldButtonsPanel.add(field, new GridBagConstraints() {
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
                this.fill = GridBagConstraints.VERTICAL;
            }
        });

        panel.add(fieldButtonsPanel, new GridBagConstraints() {
            {
                this.gridy = 1;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
                this.insets.set(8, 0, 6, 0);
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

    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

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

        panel.add(currentCommunity, new GridBagConstraints() {
            {
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
                this.insets.set(0, 0, 6, 0);
            }
        });

        panel.add(errorLabel, new GridBagConstraints() {
            {
                this.gridy = 1;
                this.weightx = 0.5f;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        final JPanel panelWrapper = new JPanel(new GridBagLayout());
        panelWrapper.setBorder(BorderFactory.createTitledBorder("  Active Community  "));
        panelWrapper.add(panel, new GridBagConstraints() {{ this.weightx = 0.5f; this.fill = GridBagConstraints.BOTH; }});

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
