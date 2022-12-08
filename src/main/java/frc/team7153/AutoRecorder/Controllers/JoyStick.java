package frc.team7153.AutoRecorder.Controllers;

import frc.team7153.AutoRecorder.ControlBase;
import frc.team7153.AutoRecorder.ControllerAction;
import edu.wpi.first.wpilibj.Joystick;

public class JoyStick extends ControlBase {
    // ID and Joystick
    private int id;

    // Constructors
    public JoyStick(int _id) {
        id = _id;
        uniqueID = String.format("Joystick %s", id);

        rawObject = new Joystick(id);

        init();
    }

    public JoyStick(int _id, String creationId) {
        id = _id;
        uniqueID = String.format("Joystick %s", id);

        rawObject = new Joystick(id);

        init(creationId);
    }

    // Get Button
    @ControllerAction(id = "", bool = true, iterateThroughArgs = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    public boolean getButton(int button) { return getBooleanButtonState(String.valueOf(button), "getRawButton", button); }


    // Get Trigger
    @ControllerAction(id = "trigger", bool = true)
    public boolean getTrigger() { return getBooleanButtonState("trigger", "getTrigger"); }


    // Get X, Y, Z, and Throttle
    @ControllerAction(id = "x", bool = false)
    public double getX() { return getDoubleButtonState("x", "getX"); }


    @ControllerAction(id = "y", bool = false)
    public double getY() { return getDoubleButtonState("y", "getY"); }


    @ControllerAction(id = "z", bool = false)
    public double getZ() { return getDoubleButtonState("z", "getZ"); }


    @ControllerAction(id = "throttle", bool = false)
    public double getThrottle() { return getDoubleButtonState("throttle", "getThrottle"); }
}
