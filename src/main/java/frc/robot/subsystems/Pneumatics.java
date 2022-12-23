package frc.robot.subsystems;

import java.util.Map;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

public class Pneumatics {
    // Solenoids
    private DoubleSolenoid dbSolenoid;
    private Solenoid sSolenoid;

    // Objects
    private XboxController controller;
    private Compressor comp;
    private NetworkTableEntry pressureOutput;

    // Constructor
    public Pneumatics(int canID, XboxController xboxController) {
        dbSolenoid = new DoubleSolenoid(canID, PneumaticsModuleType.REVPH, 0, 1);
        sSolenoid = new Solenoid(canID, PneumaticsModuleType.REVPH, 2);
        comp = new Compressor(canID, PneumaticsModuleType.REVPH);

        ShuffleboardTab tab = Shuffleboard.getTab("Sensors");
        pressureOutput = tab.add("Pressure (PSI)", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withProperties(Map.of("min", 0, "max", 120))
            .getEntry();

        controller = xboxController;

    }

    // Execute
    public void teleopPeriodic() {
        if (controller.getRightTriggerAxis() > 0.5) {
            dbSolenoid.set(DoubleSolenoid.Value.kReverse);
        } else {
            dbSolenoid.set(DoubleSolenoid.Value.kForward);
        }

        sSolenoid.set(controller.getXButton());

        // This is bad code:
        pressureOutput.setDouble(comp.getPressure());
        // telemetry updates should run periodically, regardless of robot state
    }
}
