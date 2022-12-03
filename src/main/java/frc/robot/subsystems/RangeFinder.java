package frc.robot.subsystems;

import java.util.Map;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class RangeFinder extends SubsystemBase {
    // Objects
    private AnalogInput input;
    private NetworkTableEntry shuffleboardEntry;

    // Constructor
    public RangeFinder(int id) {
        input = new AnalogInput(id);

        ShuffleboardTab tab = Shuffleboard.getTab("Sensors");
        shuffleboardEntry = tab.add(String.format("Range Finder %s", id), 0)
            .withWidget(BuiltInWidgets.kDial)
            .withProperties(Map.of("min", 0, "max", 5))
            .getEntry();
    }

    // Periodic
    @Override
    public void periodic() {
        shuffleboardEntry.setDouble(input.getVoltage());
    }
}
