// The good ultrasonic range finder

package frc.robot.subsystems;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class PingRangeFinder extends SubsystemBase {
    // Objects
    private Ultrasonic sensor;
    private NetworkTableEntry shuffleboardEntry;

    // Constructor
    public PingRangeFinder(int pingID, int echoID) {
        sensor = new Ultrasonic(pingID, echoID);
        Ultrasonic.setAutomaticMode(true);

        ShuffleboardTab tab = Shuffleboard.getTab("Sensors");
        shuffleboardEntry = tab.add(String.format("Ping Range Finder (%s -> %s) MM", pingID, echoID), 0).getEntry();
    }

    // Get
    public double getInches() {
        return sensor.getRangeInches();
    }

    public double getMM() {
        return sensor.getRangeMM();
    }

    // Periodic
    @Override
    public void periodic() {
        shuffleboardEntry.setDouble(getMM());
    }
}
