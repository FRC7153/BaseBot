// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.DriveBase;
import frc.robot.subsystems.LimitSwitch;
import frc.robot.subsystems.Pneumatics;
import frc.robot.subsystems.RangeFinder;

public class Robot extends TimedRobot {
  // Controllers
  public XboxController controller = new XboxController(1);

  // Subsystems
  public DriveBase drive = new DriveBase(controller);
  public LimitSwitch limitSwitch = new LimitSwitch(9, "Limit Switch 9");
  public RangeFinder rangeFinder = new RangeFinder(0);
  public Pneumatics pneumaticHub = new Pneumatics(6, controller);

  @Override
  public void robotInit() {}

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run(); // Start subsystems periodic() functions
  }

  @Override
  public void autonomousInit() {}

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {}

  @Override
  public void teleopPeriodic() {
    //drive.drive();
    pneumaticHub.teleopPeriodic();
  }

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  @Override
  public void testInit() {}

  @Override
  public void testPeriodic() {}
}