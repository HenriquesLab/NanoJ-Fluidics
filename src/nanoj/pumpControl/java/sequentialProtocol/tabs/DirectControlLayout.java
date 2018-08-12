package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import java.awt.*;

class DirectControlLayout extends GroupLayout {
    
    DirectControlLayout(Container host) {
        super(host);
        DirectControl panel = (DirectControl) host;

        setAutoCreateGaps(true);
        setAutoCreateContainerGaps(true);

        setVerticalGroup(
                createSequentialGroup()
                        .addGroup(
                                createSequentialGroup()
                                        .addGroup(
                                                createParallelGroup()
                                                        .addComponent(panel.pumpStatusLabel)
                                                        .addComponent(panel.pumpStatus)
                                        )
                        )
                        .addGroup(
                                createParallelGroup()
                                        .addComponent(panel.pumpSelectionLabel)
                                        .addComponent(panel.pumpSelection, GroupLayout.PREFERRED_SIZE, GUI.rowHeight, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                createParallelGroup()
                                        .addComponent(panel.syringeLabel)
                                        .addComponent(panel.syringeComboBox, GroupLayout.PREFERRED_SIZE, GUI.rowHeight, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                createParallelGroup()
                                        .addComponent(panel.rateLabel)
                                        .addComponent(panel.rateSlider, GroupLayout.PREFERRED_SIZE, GUI.rowHeight, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                createParallelGroup()
                                        .addComponent(panel.targetLabel)
                                        .addComponent(panel.targetVolume, GroupLayout.PREFERRED_SIZE, GUI.rowHeight, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(
                                createParallelGroup()
                                        .addComponent(panel.actionLabel)
                                        .addComponent(panel.infuse)
                                        .addComponent(panel.withdraw)
                        )
                        .addGroup(
                                createParallelGroup()
                                        .addComponent(panel.startPumpButton)
                                        .addComponent(panel.stopPumpButton)
                        )
        );

        setHorizontalGroup(
                createSequentialGroup()
                        .addGroup(
                                createParallelGroup()
                                        .addGroup(
                                                createSequentialGroup()
                                                        .addComponent(panel.pumpStatusLabel)
                                                        .addComponent(panel.pumpStatus)
                                        )
                                        .addGroup(
                                                createSequentialGroup()
                                                        .addGroup(
                                                                createParallelGroup()
                                                                        .addComponent(panel.pumpSelectionLabel, GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(panel.syringeLabel, GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(panel.rateLabel, GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(panel.targetLabel, GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(panel.actionLabel, GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(panel.startPumpButton, GroupLayout.PREFERRED_SIZE, GUI.largeButtonWidth, GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(
                                                                createParallelGroup()
                                                                        .addComponent(panel.pumpSelection, GroupLayout.PREFERRED_SIZE, GUI.sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(panel.syringeComboBox, GroupLayout.PREFERRED_SIZE, GUI.sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                        .addGroup(
                                                                                createSequentialGroup()
                                                                                        .addComponent(panel.rateSlider, GroupLayout.PREFERRED_SIZE, GUI.sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                        )
                                                                        .addComponent(panel.targetVolume, GroupLayout.PREFERRED_SIZE, GUI.sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                                                        .addGroup(
                                                                                createSequentialGroup()
                                                                                        .addComponent(panel.infuse)
                                                                                        .addComponent(panel.withdraw)
                                                                        )
                                                                        .addComponent(panel.stopPumpButton, GroupLayout.PREFERRED_SIZE, GUI.largeButtonWidth, GroupLayout.PREFERRED_SIZE))
                                        )
                        )
        );
    }
}
