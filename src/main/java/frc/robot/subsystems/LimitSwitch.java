package frc.robot.subsystems;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class LimitSwitch extends SubsystemBase {
    // Objects
    private DigitalInput limitSwitch;
    private NetworkTableEntry shuffleboardValue;

    // Constructor
    public LimitSwitch(int id, String shuffleboardTitle) {
        limitSwitch = new DigitalInput(id);

        shuffleboardValue = Shuffleboard.getTab("Sensors").add(shuffleboardTitle, false).getEntry();
    }

    // Read
    public boolean getValue() {
        return limitSwitch.get();
    }

    // Periodic
    @Override
    public void periodic() {
        System.out.println("ran");
        shuffleboardValue.setBoolean(getValue());
    }
}