package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;

public class DriveBase {
    // Joysticks
    private Joystick joystick;
    
    // Drive Wheels
    private WPI_TalonFX wheel_fl = new WPI_TalonFX(1);
    private WPI_TalonFX wheel_fr = new WPI_TalonFX(2);
    private WPI_TalonFX wheel_rl = new WPI_TalonFX(3);
    private WPI_TalonFX wheel_rr = new WPI_TalonFX(4);
    
    private MotorControllerGroup leftWheels = new MotorControllerGroup(wheel_fl, wheel_rl);
    private MotorControllerGroup rightWheels = new MotorControllerGroup(wheel_fr, wheel_rr);

    // Drive Base
    private DifferentialDrive diffDrive = new DifferentialDrive(leftWheels, rightWheels);

    // Drive
    public void drive() {
        diffDrive.arcadeDrive(joystick.getY(), joystick.getZ());
    }
}