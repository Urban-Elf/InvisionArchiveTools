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
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.content.parser.ParserDispatcher;
import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.ICWorkerState;
import com.urbanelf.iat.proto.ClientPacket;
import com.urbanelf.iat.proto.PythonServer;
import com.urbanelf.iat.proto.ServerPacket;
import com.urbanelf.iat.proto.constants.ClientSA;
import com.urbanelf.iat.proto.constants.WorkerType;
import com.urbanelf.iat.util.LocalStorage;

import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class WorkerFrame extends JFrame {
    private static final String TAG = WorkerFrame.class.getSimpleName();

    private static final String LS_IGNORE_CHROMEDRIVER_NOTICE = "ignore_chromedriver_notice";

    private final IC ic;
    private final WorkerType workerType;
    private final PythonServer.ServerListener serverListener;
    private String workerId;
    private ContentSpec contentSpec;

    private final JPanel statePanelWrapper;
    private final JPanel progressiveStatePanel;
    private final JPanel selectiveStatePanel;

    private final JLabel stateNote;
    private final JLabel stateHint;
    private final JProgressBar stateProgress;

    public WorkerFrame(IC ic, WorkerType workerType) {
        this.ic = ic;
        this.workerType = workerType;

        serverListener = new PythonServer.ServerListener() {
            private final Pattern VERSION_REGEX = Pattern.compile("(?i)chromedriver.*?version (\\d+(?:\\.\\d+)*).*?browser version is (\\d+(?:\\.\\d+)*)",
                    Pattern.DOTALL);

            private final JPanel startupPanel;
            private final JCheckBox dontShowAgain;

            {
                startupPanel = new JPanel();
                startupPanel.setLayout(new BoxLayout(startupPanel, BoxLayout.Y_AXIS));

                startupPanel.add(new JLabel("""
                                            <html>
                                            This tool uses its own session of Chrome (ChromeDriver)<br>
                                            to emulate the Chrome browser.<br><br>
                                            
                                            To avoid issues, please don’t interact with the window<br>
                                            unless prompted, or update Chrome while this tool is running.
                                            </html>
                                            """));
                startupPanel.add(Box.createVerticalStrut(16));
                dontShowAgain = new JCheckBox("Don't show again");
                startupPanel.add(dontShowAgain);
            }

            @Override
            public void packetReceived(ServerPacket packet) {
                if (workerId != null && !packet.getWorkerId().equals(workerId))
                    return;

                try {
                    switch (packet.getSharedAction()) {
                        case UUID_AVAILABLE -> workerId = (String) packet.getData().get("uuid");
                        case CHROMEDRIVER_STARTED -> {
                            if (!LocalStorage.getJsonObject().has(LS_IGNORE_CHROMEDRIVER_NOTICE)) {
                                // Reset checkbox
                                dontShowAgain.setSelected(false);
                                // Show dialog
                                SwingUtilities.invokeLater(() -> {
                                    final String title = "Before you begin...";
                                    final Object[] options = {"Got it!"};

                                    final JOptionPane optionPane = new JOptionPane(
                                            startupPanel,
                                            JOptionPane.INFORMATION_MESSAGE,
                                            JOptionPane.DEFAULT_OPTION,
                                            null,
                                            options,
                                            options[0]
                                    );

                                    final JDialog dialog = optionPane.createDialog(WorkerFrame.this, title);
                                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                    dialog.setModal(false); // non-blocking
                                    optionPane.addPropertyChangeListener(evt -> {
                                        final String prop = evt.getPropertyName();

                                        if (evt.getSource() == optionPane && JOptionPane.VALUE_PROPERTY.equals(prop)) {
                                            if (dontShowAgain.isSelected()) {
                                                LocalStorage.getJsonObject().put(LS_IGNORE_CHROMEDRIVER_NOTICE, true);
                                                LocalStorage.serialize();
                                            }
                                        }
                                    });
                                    dialog.setVisible(true);
                                });
                            }
                        }
                        case CHROMEDRIVER_ERROR -> {
                            final String stacktrace = ((String) packet.getData().get("stacktrace"))
                                    .toLowerCase();

                            // Determine exception
                            final String text;
                            final String[] options;
                            final Runnable okAction;
                            if (stacktrace.contains("network is unreachable")) {
                                options = new String[] {"OK"};
                                text = """
                                    IAT failed to start chromedriver.
                                    
                                    Please verify that you're connected to the internet, and try again.
                                    """;
                                okAction = () -> {
                                };
                            } else if (stacktrace.contains("this version of chromedriver only supports")) {
                                options = new String[] {"Close"};
                                // Extract versions
                                String driverVersion = "Unknown";
                                String browserVersion = "Unknown";
                                final Matcher matcher = VERSION_REGEX.matcher(stacktrace);
                                if (matcher.find()) {
                                    driverVersion = matcher.group(1);
                                    browserVersion = matcher.group(2);
                                }
                                text = "Your computer's version of Chrome appears to be out of date.\n"
                                        + "Please update it, and try again.\n\n"
                                        + "Browser version: " + browserVersion + "\n"
                                        + "ChromeDriver version: " + driverVersion;
                                okAction = () -> {
                                };
                            } else {
                                options = new String[] {"Open Link", "Close"};
                                final String link = "https://www.google.com/chrome/";
                                text = "IAT failed to start chromedriver.\n\n"
                                        + " - Please verify you have Chrome installed on your machine, and try again.\n"
                                        + "   Download at: " + link + "\n"
                                        + " - If you have any questions, feel free to contact the developer:\n"
                                        + "   (Help -> Report Bug)";
                                okAction = () -> {
                                    try {
                                        Desktop.getDesktop().browse(URI.create(link));
                                    } catch (Exception e) {
                                        Core.error(TAG, e);
                                    }
                                };
                            }

                            SwingUtilities.invokeLater(() -> {
                                int n = JOptionPane.showOptionDialog(null,
                                        text,
                                        "An error occurred",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.ERROR_MESSAGE,
                                        null,
                                        options,
                                        options[0]);

                                if (n == 0)
                                    okAction.run();

                                // Destroy frame
                                dispatchEvent(new WindowEvent(WorkerFrame.this, WindowEvent.WINDOW_CLOSING));
                            });
                        }
                        case STATE_CHANGED -> {
                            final ICWorkerState state = new ICWorkerState((JSONObject) packet.getData().get("state"));
                            SwingUtilities.invokeLater(() -> setState(state));
                        }
                        case PROGRESS_UPDATE -> {
                            final BigDecimal object = (BigDecimal) packet.getData().get("progress");
                            SwingUtilities.invokeLater(() -> stateProgress.setValue((int) (object.floatValue() * stateProgress.getMaximum())));
                        }
                        case RESULT_AVAILABLE -> {
                            final String path = (String) packet.getData().get("path");
                            // Dispatch parser and store ContentSpec
                            contentSpec = ParserDispatcher.process(new File(path));
                        }
                    }
                } catch (Exception e) {
                    Core.error(TAG, "Malformed packet for " + workerType.name(), e);
                }
            }

            @Override
            public void exceptionThrown(Throwable throwable) {
                Core.error(TAG, throwable);
            }
        };

        PythonServer.addListener(serverListener);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                PythonServer.writePacket(new ClientPacket(workerId, ClientSA.TERMINATE_WORKER));
                PythonServer.removeListener(serverListener);
            }
        });

        setTitle(workerType.getUiTitle());
        setMinimumSize(new Dimension(320, (int) (320 * 1.7f)));

        stateNote = new JLabel();
        stateNote.setHorizontalAlignment(JLabel.CENTER);
        stateHint = new JLabel();
        stateHint.setHorizontalAlignment(JLabel.RIGHT);
        stateHint.setFont(Fonts.SMALL);
        stateProgress = new JProgressBar();

        statePanelWrapper = new JPanel();
        progressiveStatePanel = layoutProgressiveStatePanel();
        selectiveStatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(stateNote, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.insets = new Insets(0, 0, 8, 0);
                this.weightx = 1;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        panel.add(statePanelWrapper, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                this.weightx = 1;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        final JPanel panelWrapper = new JPanel(new BorderLayout(0, 8));
        panelWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelWrapper.add(panel, BorderLayout.CENTER);
        panelWrapper.add(stateHint, BorderLayout.SOUTH);

        setContentPane(panelWrapper);

        /////////////////////////////////////////////////////////////

        // Set initial state
        setState(ICWorkerState.CONNECTING);

        // Start worker
        PythonServer.writePacket(new ClientPacket(ClientSA.DISPATCH_WORKER)
                .addData("ic", ic.toJson())
                .addData("worker_type", workerType));
    }

    public void updateComponentTreeUI() {
        SwingUtilities.updateComponentTreeUI(this);
        SwingUtilities.updateComponentTreeUI(progressiveStatePanel);
        SwingUtilities.updateComponentTreeUI(selectiveStatePanel);
    }

    private void setState(ICWorkerState state) {
        stateNote.setText(state.getNote());
        stateHint.setText(state.getHint());
        statePanelWrapper.removeAll();
        if (state.isProgressive()) {
            stateProgress.setIndeterminate(state.isIndeterminate());
            statePanelWrapper.add(progressiveStatePanel);
        } else {
            layoutSelectiveStatePanel(state);
            statePanelWrapper.add(selectiveStatePanel);
        }
    }

    private JPanel layoutProgressiveStatePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(stateProgress, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 1;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        return panel;
    }

    private void layoutSelectiveStatePanel(ICWorkerState state) {
        selectiveStatePanel.removeAll();
        for (ICWorkerState.ButtonConfig config : state.getButtonConfigs()) {
            final JButton button = new JButton(config.text());
            button.setFocusable(false);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    if (!button.isEnabled() || mouseEvent.getButton() != MouseEvent.BUTTON1)
                        return;
                    switch (config.sharedAction()) {
                        case OPEN_LOG -> {
                            try {
                                Desktop.getDesktop().open(Core.getLogFile());
                            } catch (Exception e) {
                                Core.error(TAG, "Error opening log: '" + Core.getLogFile().getName() + "'", e);
                            }
                        }
                        case EXPORT_ARCHIVE -> {
                            Core.exportArchive(WorkerFrame.this, contentSpec, true);
                        }
                        case TERMINATE -> {
                            // Destroy frame
                            dispatchEvent(new WindowEvent(WorkerFrame.this, WindowEvent.WINDOW_CLOSING));
                        }
                    }
                    PythonServer.writePacket(new ClientPacket(workerId, ClientSA.STATE_INPUT)
                            .addData("client_object", config.clientObject()));
                }
            });
            selectiveStatePanel.add(button);
        }
    }

    public IC getIc() {
        return ic;
    }

    public WorkerType getWorkerType() {
        return workerType;
    }
}
