package com.urbanelf.ima.ui;

import com.urbanelf.ima.ic.workers.ICWorker;
import com.urbanelf.ima.ic.workers.state.ICWorkerState;
import com.urbanelf.ima.ic.workers.state.ICWorkerStateObserver;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class WorkerFrame extends JFrame {
    private final ICWorker<?> worker;

    private final JPanel statePanelWrapper;
    private final JPanel progressiveStatePanel;
    private final JPanel selectiveStatePanel;

    private final JLabel stateNote;
    private final JLabel stateHint;
    private final JProgressBar stateProgress;

    public WorkerFrame(ICWorker<?> worker) {
        if (worker == null)
            throw new IllegalArgumentException("worker cannot be null");
        this.worker = worker;
        worker.addStateObserver(new ICWorkerStateObserver() {
            @Override
            public void stateChanged(ICWorkerState state) {
                SwingUtilities.invokeLater(() -> {
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
                });
            }

            @Override
            public void progressChanged(float progress) {
                SwingUtilities.invokeLater(() -> stateProgress.setValue((int) (progress * stateProgress.getMaximum())));
            }
        });

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setTitle(worker.getTitle());
        setMinimumSize(new Dimension(320, (int) (320 * 1.7f)));
        setLocationByPlatform(true);

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

        //pack();

        /////////////////////////////////////////////////////////////

        //stateNote.setText("Please sign in to continue.");
        //stateProgress.setIndeterminate(true);
        //layoutSelectiveStatePanel(ICWorkerState.AUTH_REQUIRED);
        //statePanelWrapper.add(selectiveStatePanel);
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
                    config.callback().run();
                    worker.pushClientObject(config.clientObject());
                }
            });
            selectiveStatePanel.add(button);
        }
    }

    public ICWorker<?> getWorker() {
        return worker;
    }
}
