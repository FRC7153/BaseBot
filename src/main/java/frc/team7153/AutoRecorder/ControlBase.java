package frc.team7153.AutoRecorder;

import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Timer;

public abstract class ControlBase {
    // Id (INIT)
    protected String uniqueID;
    protected Object rawObject;
    protected String creationId;

    // Adding to Controller (INIT)
    protected AutoManager autoManager;
    protected NetworkTableEntry shuffleboardEntry;
    protected Logger log;

    public final void init(String uniqueCreationId) {
        creationId = uniqueCreationId;
        log = Logger.getLogger(String.format("Auto - %s (%s)", uniqueID, creationId));
    }

    public final void init() { init(UUID.randomUUID().toString()); }

    public final void registerToController(AutoManager m) {
        autoManager = m;
        
        m.registerController(this); // Just in case :)
    }

    // Return Total State (RECORD)
    //public abstract HashMap<String, Double> getTotalState(); // No longer user defined, use annotations instead
    public HashMap<String, Double> getTotalState() {
        HashMap<String, Double>state = new HashMap<String, Double>();

        if (autoManager.getMode() != -3) {
            log.warning("getTotalState called while mode is not -3!");
        }

        for (Method m : this.getClass().getMethods()) {
            if (m.isAnnotationPresent(ControllerAction.class)) {
                ControllerAction annotation = m.getDeclaredAnnotation(ControllerAction.class);
                
                try {
                    int[] args = annotation.iterateThroughArgs();
                    if (args.length == 0) {
                        // No parameters
                        Object output = m.invoke(this);

                        if (annotation.bool()) {
                            if (output.equals(true)) {
                                state.put(annotation.id(), 1.0);
                            } else {
                                state.put(annotation.id(), 0.0);
                            }
                        } else {
                            state.put(annotation.id(), (Double)output);
                        }
                    } else {
                        // Multiple parameters
                        for (int arg : args) {
                            Object output = m.invoke(this, arg);
    
                            if (annotation.bool()) {
                                if (output.equals(true)) {
                                    state.put(annotation.id() + String.valueOf(arg), 1.0);
                                } else {
                                    state.put(annotation.id() + String.valueOf(arg), 0.0);
                                }
                            } else {
                                state.put(annotation.id() + String.valueOf(arg), (Double)output);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warning(String.format("Could not invoke method: %s", e));
                }
            }
        }

        return state;
    }

    // Cache Alternative Methods
    private HashMap<String, Method> teleop_cachedMethods = new HashMap<String, Method>();

    // Saving Record Data (RECORD)
    public HashMap<String, Double> record_lastFrameData;
    public StringBuilder record_recordData;
    public int record_frameCount;

    public ControlBase writeChangesTo = this;

    // Receive Frames (PLAYBACK)
    public ControllerRecording playback_recording;
    private HashMap<String, Double> playback_currentPlaybackState;

    public final int sendPlayback(ControllerRecording recording) {
        playback_currentPlaybackState = recording.startData;
        playback_recording = recording;
        shuffleboardEntry.setString(String.format("%s frames loaded", playback_recording.numOfFrames()));
        return playback_recording.numOfFrames();
    }

    // Process Frames (PLAYBACK)
    public final void playbackPeriodic() {
        if (playback_recording.numOfFrames() == 0) { shuffleboardEntry.setString("Done"); return; }

        while (playback_recording.numOfFrames() > 0 && (Double)playback_recording.getFirstFrame()[0] <= Timer.getFPGATimestamp() - autoManager.start) {
            Object[] f = playback_recording.getFirstFrame();

            playback_currentPlaybackState.put((String)f[1], (Double)f[2]);

            playback_recording.removeFirstFrame();
            autoManager.playedAFrame();

            if (autoManager.getMode() != 1) { shuffleboardEntry.setString("Done"); return; }
            if (playback_recording.numOfFrames() == 0) { shuffleboardEntry.setString("Done"); return; }
        }
        shuffleboardEntry.setString(String.format("%s frames remaining...", playback_recording.numOfFrames()));
    }

    // Get/Save Current Value (PLAYBACK/RECORD/TELEOP)
    private final double processButtonState(String bttn, String alternativeName, int mode, Integer arg) {
        if (mode == -2 || mode == -4) { // Record/Playback Done, or Record Delay
            return 0.0;
        } else if (mode == -1) { // No Mode
            log.warning("getButtonState called, but autoManager does not have a MODE!");
            return 0.0;
        } else if (mode == 1) { // Playback
            return playback_currentPlaybackState.get(bttn);
        } else { // Teleop/Record/Prepare
            try {
                Method m = teleop_cachedMethods.get(alternativeName);
                if (m == null) {
                    if (arg == null) {
                        m = rawObject.getClass().getMethod(alternativeName);
                    } else {
                        m = rawObject.getClass().getMethod(alternativeName, int.class);
                    }
                    log.info(String.format("Cached method '%s' for %s (%s)", alternativeName, uniqueID, creationId));
                    teleop_cachedMethods.put(alternativeName, m);
                }

                Object alt;

                if (arg == null) {
                    alt = m.invoke(rawObject);
                } else {
                    alt = m.invoke(rawObject, (int)arg);
                }
                
                if (alt.equals(true)) {
                    return 1.0;
                } else if (alt.equals(false)) {
                    return 0.0;
                } else if (alt.getClass() == int.class) {
                    return Double.valueOf((int)alt);
                } else {
                    return (double)alt;
                }
            } catch (NoSuchMethodException e) {
                log.warning(String.format("No such method: %s (%s)", alternativeName, e));
                return 0.0;
            } catch (Exception e) {
                log.warning(String.format("Invoking alternative method (%s) for %s threw error! (%s)", alternativeName, bttn, e));
                return 0.0;
            }
        }
    }

    public final double getDoubleButtonState(String bttn, String alternativeName, Integer arg) {
        int m = autoManager.getMode();

        if (m == 2) { // Record
            Double s = processButtonState(bttn, alternativeName, m, arg);

            if (!writeChangesTo.record_lastFrameData.get(bttn).equals(s)) {
                record_frameCount += 1;

                writeChangesTo.record_lastFrameData.put(bttn, s);
                writeChangesTo.record_recordData.append(String.format("\n>%s,%s,%s", Timer.getFPGATimestamp()-autoManager.start, bttn, s));
                shuffleboardEntry.setString(String.format("Recorded %s frames", record_frameCount));
            }

            return s;
        }

        return processButtonState(bttn, alternativeName, m, arg);
    }

    public final double getDoubleButtonState(String bttn, String alternativeName) { return getDoubleButtonState(bttn, alternativeName, null); }

    public final boolean getBooleanButtonState(String bttn, String alternativeName, Integer arg) { return (getDoubleButtonState(bttn, alternativeName, arg) == 1.0);}

    public final boolean getBooleanButtonState(String bttn, String alternativeName) { return (getDoubleButtonState(bttn, alternativeName, null) == 1.0); }

    // Unload High-Memory Variables (and clear shuffleboard)
    public void unloadData() {
        record_frameCount = 0;
        record_recordData = null;
        record_lastFrameData = null;

        playback_recording = null;
        playback_currentPlaybackState = null;

        shuffleboardEntry.setString("Ready...");
    }
}
