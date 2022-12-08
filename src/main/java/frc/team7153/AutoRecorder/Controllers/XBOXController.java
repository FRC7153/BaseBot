package frc.team7153.AutoRecorder.Controllers;

import edu.wpi.first.wpilibj.XboxController;
import frc.team7153.AutoRecorder.ControlBase;
import frc.team7153.AutoRecorder.ControllerAction;

public class XBOXController extends ControlBase{
    // ID and Controller
    private int id;

    // Constructors
    public XBOXController(int _id) {
        id = _id;
        uniqueID = String.format("Xbox Controller %s", id);

        rawObject = new XboxController(id);

        init();
    }

    public XBOXController(int _id, String creationId) {
        id = _id;
        uniqueID = String.format("Xbox Controller %s", id);

        rawObject = new XboxController(id);

        init(creationId);
    }
    
    // Get Letter Buttons
    @ControllerAction(id = "a", bool = true)
    public boolean getAButton() { return getBooleanButtonState("a", "getAButton"); }


    @ControllerAction(id = "b", bool = true)
    public boolean getBButton() { return getBooleanButtonState("b", "getBButton"); }


    @ControllerAction(id = "x", bool = true)
    public boolean getXButton() { return getBooleanButtonState("x", "getXButton"); }


    @ControllerAction(id = "y", bool = true)
    public boolean getYButton() { return getBooleanButtonState("y", "getYButton"); }


    // Get Bumpers (Front Triggers)
    @ControllerAction(id = "lBumper", bool = true)
    public boolean getLeftBumper() { return getBooleanButtonState("lBumper", "getLeftBumper"); }


    @ControllerAction(id = "rBumper", bool = true)
    public boolean getRightBumper() { return getBooleanButtonState("rBumper", "getRightBumper"); }


    // Get Triggers (Back Triggers)
    @ControllerAction(id = "lTrigger", bool = false)
    public double getLeftTrigger() { return getDoubleButtonState("lTrigger", "getLeftTriggerAxis"); }


    @ControllerAction(id = "rTrigger", bool = false)
    public double getRightTrigger() { return getDoubleButtonState("rTrigger", "getRightTriggerAxis"); }


    // Stick Buttons
    @ControllerAction(id = "lStickBttn", bool = true)
    public boolean getLeftStickPressed() { return getBooleanButtonState("lStickBttn", "getLeftStickButton"); }


    @ControllerAction(id = "rStickBttn", bool = true)
    public boolean getRightStickPressed() { return getBooleanButtonState("rStickBttn", "getRightStickButton"); }


    // Stick Directions
    @ControllerAction(id = "rStickX", bool = false)
    public double getRightStickX() { return getDoubleButtonState("rStickX", "getRightX"); }


    @ControllerAction(id = "rStickY", bool = false)
    public double getRightStickY() { return getDoubleButtonState("rStickY", "getRightY"); }


    @ControllerAction(id = "lStickX", bool = false)
    public double getLeftStickX() { return getDoubleButtonState("lStickX", "getLeftX"); }


    @ControllerAction(id = "lStickY", bool = false)
    public double getLeftStickY() { return getDoubleButtonState("lStickY", "getLeftY"); }


    // Get D-Pad
    @ControllerAction(id = "d", bool = false)
    public double getDPad() { return getDoubleButtonState("d", "getPOV"); }


    // Start and Back Button
    @ControllerAction(id = "start", bool = true)
    public boolean getStartButton() { return getBooleanButtonState("start", "getStartButton"); }


    @ControllerAction(id = "back", bool = true)
    public boolean getBackButton() { return getBooleanButtonState("back", "getBackButton"); }
}
