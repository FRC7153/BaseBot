package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;

public class DriveBase {
    // Joysticks
    private XboxController controller;
    private IMU gyro;
    private double maxSpeed;
    
    // Drive Wheels
    private WPI_TalonFX wheel_lm = new WPI_TalonFX(2);
    private WPI_TalonFX wheel_ls = new WPI_TalonFX(3);
    private WPI_TalonFX wheel_rm = new WPI_TalonFX(4);
    private WPI_TalonFX wheel_rs = new WPI_TalonFX(5);

    // Drive Base
    private DifferentialDrive diffDrive = new DifferentialDrive(wheel_lm, wheel_rm);

    // Constuctor
    public DriveBase(XboxController xboxController, IMU imu, double maximumSpeed) {
        wheel_ls.follow(wheel_lm);
        wheel_rs.follow(wheel_rm);

        controller = xboxController;
        gyro = imu;
        maxSpeed = maximumSpeed;

        kPEntry = Shuffleboard.getTab("Drive").add("Turn kP", 0.05).getEntry();
    }

    // Clamp
    private double clamp(double value) { return Math.min(Math.max(value, -maxSpeed), maxSpeed); }
    private double scaleClamp(double value) { return value * maxSpeed; }

    // Drive
    public void drive() {
        //diffDrive.arcadeDrive(controller.getLeftY(), controller.getRightX());
        diffDrive.tankDrive(-scaleClamp(controller.getLeftY()), scaleClamp(controller.getRightY()), false);
    }

    // P Controller
    private NetworkTableEntry kPEntry;
    private double maxDegreesPerSecond = 5;

    public void driveP() {
        double sp = controller.getRightX() * -maxDegreesPerSecond;
        double offset = kPEntry.getDouble(0.0) * (sp - gyro.getGyroRate());

        diffDrive.tankDrive(-clamp(controller.getLeftY() + offset), clamp(controller.getLeftY() - offset));
    }
}