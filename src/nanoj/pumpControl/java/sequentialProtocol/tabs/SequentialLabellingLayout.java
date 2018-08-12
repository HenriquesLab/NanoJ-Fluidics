package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.sequentialProtocol.GUI;
import nanoj.pumpControl.java.sequentialProtocol.Step;

import javax.swing.*;
import java.awt.*;

import static nanoj.pumpControl.java.sequentialProtocol.GUI.rowHeight;

class SequentialLabellingLayout extends GroupLayout {

    SequentialLabellingLayout(Container host) {
        super(host);
        SequentialLabelling panel = (SequentialLabelling) host;

        setAutoCreateGaps(true);
        setAutoCreateContainerGaps(true);

        GroupLayout topPanelLayout = new GroupLayout(panel.topPanel);
        topPanelLayout.setAutoCreateGaps(true);
        topPanelLayout.setAutoCreateContainerGaps(true);

        topPanelLayout.setVerticalGroup(
                topPanelLayout.createSequentialGroup()
                        .addGroup(
                                topPanelLayout.createParallelGroup()
                                        .addComponent(panel.protocolLoad)
                                        .addComponent(panel.protocolSave)
                        )
                        .addGroup(
                                topPanelLayout.createParallelGroup()
                                        .addComponent(panel.stepsLabel, GroupLayout.Alignment.CENTER)
                                        .addComponent(panel.numberOfSteps, GroupLayout.PREFERRED_SIZE, rowHeight, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(panel.addStepButton)
                                        .addComponent(panel.removeStepButton)
                                        .addComponent(panel.startSeqButton)
                                        .addComponent(panel.stopSeqButton)
                                        .addComponent(panel.stopPumpButton)
                                        .addComponent(panel.syringeReadyButton)
                        )
                        .addComponent(panel.syringeReadyLabel)
                        .addComponent(panel.seqStatus)
                        .addGroup(
                                topPanelLayout.createParallelGroup()
                                        .addComponent(panel.pumpStatusOnSeqLabel)
                                        .addComponent(panel.pumpStatusOnSeq)
                        )
                        .addGroup(
                                topPanelLayout.createParallelGroup()
                                        .addComponent(panel.suckBetweenStepsLabel)
                                        .addComponent(panel.suckBetweenSteps)
                        )
                        .addGroup(
                                topPanelLayout.createParallelGroup()
                                        .addComponent(panel.suckStepLabel)
                                        .addComponent(panel.suckStepPanel, GroupLayout.PREFERRED_SIZE, rowHeight+5, GroupLayout.PREFERRED_SIZE)
                        )
        );

        topPanelLayout.setHorizontalGroup(
                topPanelLayout.createParallelGroup()
                        .addGroup(
                                topPanelLayout.createSequentialGroup()
                                        .addComponent(panel.protocolLoad)
                                        .addComponent(panel.protocolSave)
                        )
                        .addGroup(
                                topPanelLayout.createSequentialGroup()
                                        .addComponent(panel.stepsLabel)
                                        .addComponent(panel.numberOfSteps, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(panel.addStepButton, GroupLayout.PREFERRED_SIZE, GUI.smallButtonWidth, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(panel.removeStepButton, GroupLayout.PREFERRED_SIZE, GUI.smallButtonWidth, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(panel.startSeqButton)
                                        .addComponent(panel.stopSeqButton)
                                        .addComponent(panel.stopPumpButton)
                                        .addComponent(panel.syringeReadyButton)
                        )
                        .addGroup(
                                topPanelLayout.createParallelGroup()
                                        .addComponent(panel.syringeReadyLabel)
                                        .addComponent(panel.seqStatus)

                        )
                        .addGroup(
                                topPanelLayout.createSequentialGroup()
                                        .addComponent(panel.pumpStatusOnSeqLabel)
                                        .addComponent(panel.pumpStatusOnSeq)
                        )
                        .addGroup(
                                topPanelLayout.createSequentialGroup()
                                        .addComponent(panel.suckBetweenStepsLabel)
                                        .addComponent(panel.suckBetweenSteps)
                        )
                        .addGroup(
                                topPanelLayout.createSequentialGroup()
                                        .addComponent(panel.suckStepLabel)
                                        .addComponent(panel.suckStepPanel)
                        )
        );

        panel.topPanel.setLayout(topPanelLayout);

        //Panel with list of steps
        GridLayout steps_layout = new GridLayout(0, 1);
        panel.stepsPanel.setLayout(steps_layout);

        for (Step item: panel.sequence) {
            item.addActionListener(panel.updateStepPump);
            panel.stepsPanel.add(item.getStepPanel());
        }

        //Bring panels together
        panel.add(panel.topPanel);
        panel.add(panel.scrollPane);

        GroupLayout sequential_layout = new GroupLayout(panel);
        sequential_layout.setAutoCreateContainerGaps(true);
        sequential_layout.setAutoCreateGaps(true);
        panel.setLayout(sequential_layout);

        sequential_layout.setVerticalGroup(
                sequential_layout.createSequentialGroup()
                        .addGroup(
                                sequential_layout.createSequentialGroup()
                                        .addComponent(panel.topPanel)
                                        .addComponent(panel.scrollPane))
        );

        sequential_layout.setHorizontalGroup(
                sequential_layout.createParallelGroup()
                        .addGroup(
                                sequential_layout.createParallelGroup()
                                        .addComponent(panel.topPanel)
                                        .addComponent(panel.scrollPane)
                        )
        );
    }
}
