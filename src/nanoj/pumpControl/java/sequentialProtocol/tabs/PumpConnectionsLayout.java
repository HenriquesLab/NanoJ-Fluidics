package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import java.awt.*;

class PumpConnectionsLayout extends GroupLayout {
    PumpConnectionsLayout(Container host) {
        super(host);
        PumpConnections panel = (PumpConnections) host;

        setAutoCreateGaps(true);
        setAutoCreateContainerGaps(true);

        setVerticalGroup(
                createSequentialGroup()
                        .addGroup(createParallelGroup()
                                .addComponent(panel.pumpListLabel)
                                .addComponent(panel.availablePumpsList, GroupLayout.PREFERRED_SIZE, GUI.rowHeight, GroupLayout.PREFERRED_SIZE)
                        )
                        .addGroup(createParallelGroup()
                                .addComponent(panel.connectLabel)
                                .addComponent(panel.portsList, GroupLayout.PREFERRED_SIZE, GUI.rowHeight, GroupLayout.PREFERRED_SIZE)
                                .addComponent(panel.connectButton)
                                .addComponent(panel.disconnectButton)
                        )
                        .addGroup(createParallelGroup().addComponent(panel.connectedPumpsLabel))
                        .addGroup(createParallelGroup().addComponent(panel.connectedPumpsListPane))
        );

        setHorizontalGroup(
                createParallelGroup()
                        .addGroup(createSequentialGroup()
                                .addGroup(createParallelGroup()
                                        .addComponent(panel.pumpListLabel, GroupLayout.Alignment.TRAILING)
                                        .addComponent(panel.connectLabel, GroupLayout.Alignment.TRAILING)
                                )
                                .addGroup(createParallelGroup()
                                        .addComponent(panel.availablePumpsList, GroupLayout.PREFERRED_SIZE, GUI.sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(panel.portsList, GroupLayout.PREFERRED_SIZE, GUI.sizeSecondColumn, GroupLayout.PREFERRED_SIZE)
                                )
                                .addGroup(createSequentialGroup()
                                        .addComponent(panel.connectButton)
                                        .addComponent(panel.disconnectButton)
                                )
                        )
                        .addGroup(createParallelGroup().addComponent(panel.connectedPumpsLabel))
                        .addGroup(createParallelGroup().addComponent(panel.connectedPumpsListPane))
        );
    }
}
