package nanoj.pumpControl.java.sequentialProtocol.tabs;

import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import java.awt.*;

public class PumpCalibrationLayout extends GroupLayout {

    public PumpCalibrationLayout(Container host) {
        super(host);

        PumpCalibration panel = (PumpCalibration) host;

        setAutoCreateGaps(true);
        setAutoCreateContainerGaps(true);

        setVerticalGroup(
                createSequentialGroup()
                        .addGroup(createParallelGroup()
                                .addComponent(panel.loadCalibration)
                                .addComponent(panel.saveCalibration)
                                .addComponent(panel.resetCalibration)
                        )
                        .addGroup(createParallelGroup()
                                .addComponent(panel.pumpList)
                                .addComponent(panel.timeToPumpLabel)
                                .addComponent(panel.timeToPump)
                                .addComponent(panel.calibrateButton)
                                .addComponent(panel.stopButton)
                        )
                        .addGroup(createParallelGroup().addComponent(panel.tableLabel))
                        .addGroup(createParallelGroup().addComponent(panel.tableScrollPane))
        );

        setHorizontalGroup(
                createParallelGroup()
                        .addGroup(createSequentialGroup()
                                .addComponent(panel.loadCalibration)
                                .addComponent(panel.saveCalibration)
                                .addComponent(panel.resetCalibration)
                        )
                        .addGroup(createSequentialGroup()
                                .addComponent(panel.pumpList)
                                .addComponent(panel.timeToPumpLabel)
                                .addComponent(panel.timeToPump)
                                .addComponent(panel.calibrateButton)
                                .addComponent(panel.stopButton)
                        )
                        .addGroup(createParallelGroup().addComponent(panel.tableLabel))
                        .addGroup(createParallelGroup().addComponent(panel.tableScrollPane))
        );
    }
}
