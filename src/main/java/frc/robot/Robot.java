/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PWMVictorSPX;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Relay.*;
import edu.wpi.first.wpilibj.DigitalInput;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ty = table.getEntry("ty"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ta = table.getEntry("ta"); // area of the object
  NetworkTableEntry tv = table.getEntry("tv"); // 1 if have vision 0 if no vision
  NetworkTableEntry tlong = table.getEntry("tlong"); // length of longest side
  NetworkTableEntry tshort = table.getEntry("tshort"); // length of shortest side
  NetworkTableEntry tvert = table.getEntry("tvert"); // vertical distance
  NetworkTableEntry thor = table.getEntry("thor"); // horizontal distance
  NetworkTableEntry getpipe = table.getEntry("getpipe"); // this tells us what "pipeline" we are on, basically different
                                                         // settings for the camera
  NetworkTableEntry ts = table.getEntry("ts"); // skew or rotation of target

  Joystick joy = new Joystick(0);

  VictorSP frontLeft = new VictorSP(8);
  VictorSP frontRight = new VictorSP(2);
  VictorSP rearLeft = new VictorSP(4);
  VictorSP rearRight = new VictorSP(3);
  // VictorSP solo = new VictorSP(5);
  PWMVictorSPX lift1 = new PWMVictorSPX(0);
  PWMVictorSPX lift2 = new PWMVictorSPX(1);

  Relay belt = new Relay(0);
  Relay intake = new Relay(1);

  SpeedControllerGroup left = new SpeedControllerGroup(frontLeft, rearLeft);
  SpeedControllerGroup right = new SpeedControllerGroup(frontRight, rearRight);
  SpeedControllerGroup lift = new SpeedControllerGroup(lift1, lift2);

  DifferentialDrive buffet = new DifferentialDrive(left, right);

  double pizza;
  double taco;

  double camx;
  double camy;
  double camarea;
  double targetWidth;
  double degreeWidth = 0;
  double targetRatio;
  double targetRatioInverse;

  boolean aligned;
  boolean distanced;

  DigitalInput inDown = new DigitalInput(0);
  DigitalInput inUp = new DigitalInput(1);
  DigitalInput liftUp = new DigitalInput(2);
  DigitalInput liftDown = new DigitalInput(3);
  DigitalInput b1 = new DigitalInput(4);
  DigitalInput b2 = new DigitalInput(5);
  DigitalInput b3 = new DigitalInput(6);

  /**
   * This function is run when the robot is first started up and should be used
   * for any initialization code.
   */
  @Override
  public void robotInit() {

    SmartDashboard.putString("General Kenobi", "Hello there");
  }

  /**
   * This function is called every robot packet, no matter the mode. Use this for
   * items like diagnostics that you want ran during disabled, autonomous,
   * teleoperated and test.
   *
   * <p>
   * This runs after the mode specific periodic functions, but before LiveWindow
   * and SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select
   * between different autonomous modes using the dashboard. The sendable chooser
   * code works with the Java SmartDashboard. If you prefer the LabVIEW Dashboard,
   * remove all of the chooser code and uncomment the getString line to get the
   * auto name from the text box below the Gyro
   *
   * <p>
   * You can add additional auto modes by adding additional comparisons to the
   * switch structure below with additional strings. If using the SendableChooser
   * make sure to add them to the chooser code above as well.
   */
  @Override
  public void autonomousInit() {

  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    // insert delay (duration determined by selecter on ShuffleBoard)
    autoShoot("auto");
  }

  /**
   * This function is called periodically during operator control.
   */
  @Override
  public void teleopPeriodic() {
    pizza = joy.getRawAxis(1);
    taco = joy.getRawAxis(4);
    buffet.arcadeDrive(-pizza, taco);

    // Moves intake up and down
    if (inDown.get() == false && joy.getRawButton(1)) {
      intake.set(Value.kForward);
    } else if (inUp.get() == false && joy.getRawButton(2)) {
      intake.set(Value.kReverse);
    } else {
      intake.set(Value.kOff);
    }

    // Moves lift up and down
    if (liftDown.get() == false && joy.getRawButton(3)) {
      lift.set(1);
    } else if (liftUp.get() == false && joy.getRawButton(4)) {
      lift.set(-1);
    } else {
      lift.set(0);
    }

    // Moves belt automatically
    if (b1.get() == true && b3.get() == false) {
      belt.set(Value.kForward);
    } else if (b2.get() == true && b1.get() == false) {
      belt.set(Value.kOff);
    }


    camx = tx.getDouble(0.0);
    camy = ty.getDouble(0.0);
    camarea = ta.getDouble(0.0);
    targetWidth = thor.getDouble(0);
    degreeWidth = targetWidth * 0.16875;// degrees per pixel
    targetRatio = 10 / degreeWidth; // ratio of width of desired target to width of target (13 ft away and
    // perpendicular)
    // if the robot is closer, then it should move more (and if farther away, it
    // should move less)
    // up close, degreeWidth will be greater than 1.0602..., and so the robot will
    // try to be more accurate
    // farther away, it will have more leeway
    System.out.println("The target is " + targetWidth + " pixels wide, " + degreeWidth + " degrees.");

    double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    SmartDashboard.putNumber("Vision", tv);
    SmartDashboard.putNumber("LimelightX", camx);
    SmartDashboard.putNumber("LimelightY", camy);
    SmartDashboard.putNumber("LimelightArea", camarea);
    NetworkTableInstance.getDefault();

    SmartDashboard.putBoolean("Aligned", aligned);
    SmartDashboard.putBoolean("DistancED", distanced);

    SmartDashboard.putBoolean("Motor Safety", frontLeft.isSafetyEnabled());

    SmartDashboard.putNumber("Angle width", degreeWidth);

    if (joy.getRawButton(6) == true && tv == 1) {// Moves us into auto-shooting if button is pressed
      autoShoot("tele");
    } else {
      aligned = false;
      distanced = false;
      // solo.set(0);
      // belt.set(Value.kOff);
    }
  }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
  }

  public void autoShoot(String mode) {
    double upperYBound = 0; // just initializing, they'll never be 0
    double lowerYBound = 0;// just initializing, they'll never be 0
    // Auto-Aligns to the reflective tape
    aligned = false;
    distanced = false;

    switch (mode) {
    case "auto":
      upperYBound = -7;
      lowerYBound = -9;
      break;

    case "tele":
      upperYBound = -7;
      lowerYBound = -9;
      break;
    }

    if (camx > targetRatio * 1.5) {
      left.set((camx * camx + 1) / (2 * camx * camx + 16));
      right.set(0);
      System.out.println("Left set to " + targetRatioInverse + " * " + (camx * camx + 1) / (2 * camx * camx + 16)
          + " = " + targetRatioInverse * (camx * camx + 1) / (2 * camx * camx + 16));
      // System.out.println("Setting motors to "+(camx*camx+1)/(3*camx*camx+16));
      aligned = false;
      // System.out.println("Should be turning right ("+camx+")");
      // On left, twist right
    } else if (camx < targetRatio * -1.5) {
      left.set(0);
      right.set(-(camx * camx + 1) / (2 * camx * camx + 16));
      System.out.println("Right set to " + targetRatioInverse + " * " + (camx * camx + 1) / (2 * camx * camx + 16)
          + " = " + targetRatioInverse * (camx * camx + 1) / (2 * camx * camx + 16));
      // System.out.println("Setting motors to "+(-(camx*camx+1)/(3*camx*camx+16)));
      aligned = false;
      // System.out.println("Should be turning left ("+camx+")");
      // On right, twist left
    } else if (camx > targetRatio * -1.5 && camx < targetRatio * 1.5) {
      aligned = true;
      // System.out.println("Should be staying put because the x value is "+camx);
      // We be aligned
    } else {
      System.out.println("I am the print line... that doesn't do anything. Camx is " + camx);
    }

    // Moves to correct distance from reflective tape

    if (camy > upperYBound && aligned == true) {
      left.set(-(camy * camy + 16 * camy + 65) / (1.5 * (camy * camy + 16 * camy + 72)));
      right.set((camy * camy + 16 * camy + 65) / (1.5 * (camy * camy + 16 * camy + 72)));
      distanced = false;
    } else if (camy < lowerYBound && aligned == true) {
      left.set((camy * camy + 16 * camy + 65) / (1.5 * (camy * camy + 16 * camy + 72)));
      right.set(-(camy * camy + 16 * camy + 65) / (1.5 * (camy * camy + 16 * camy + 72)));
      distanced = false;
    } else if (camy < upperYBound && camy > lowerYBound && aligned == true) {
      distanced = true;
    }

    // Shooting der ball m8

    if (distanced == true && aligned == true) {
      System.out.println("we got through the shooting boiz");
      // solo.set(1);
      // belt.set(Value.kforward);
      aligned = false;
      distanced = false;
    }
  }
}
