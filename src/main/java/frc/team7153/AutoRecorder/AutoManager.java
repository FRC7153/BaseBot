package frc.team7153.AutoRecorder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.shuffleboard.SimpleWidget;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/*
*  How Recordings Are Stored (with base64):

--AUTO_FILE_START--
->Controller 1
#startBttn:startState;startBttn:startState
>time,bttn,state
>time,bttn,state
->Controller 2
#startBttn:startState;startBttn:startState
>time,bttn,state
>time,bttn,state

*/

public class AutoManager extends SubsystemBase {
    // Logging
    private Logger log;

    // Shuffleboard Tab
    private ShuffleboardTab shuffle_tab;
    private NetworkTableEntry shuffle_mode;
    private NetworkTableEntry shuffle_time;

    private NetworkTableEntry shuffle_playbackLoaded;
    private NetworkTableEntry shuffle_playbackPercent;
    
    private SimpleWidget shuffle_playbackPercentWidget;

    private PlaybackLoader shuffle_playbackLoadBttn;

    private NetworkTableEntry shuffle_recordId;
    private NetworkTableEntry shuffle_recordDuration;
    private NetworkTableEntry shuffle_recordDelay;
    private NetworkTableEntry shuffle_recordSaveIP;
    private NetworkTableEntry shuffle_recordStatus;
    private NetworkTableEntry shuffle_recordisRecording;

    private ShuffleboardLayout shuffle_controllers;

    // Playback Percent
    private int playback_framesPlayed = 0;
    private int playback_totalFrames = 0;

    // Mode
    private String[] modeTitles = {"Record delay", "Preparing controllers", "Playback/Recording done", "None", "Teleop", "Playback", "Record"};
    private int mode = -1; //-4 = delay, -3 = preparing controllers, -2 = playback/recording done, -1 = none, 0 = teleop, 1 = playback, 2 = record

    public int getMode() { return mode; }

    private void setMode(int m) { mode = m; shuffle_mode.setString(modeTitles[m+4]); shuffle_time.setString("0.0"); }

    // Set Time
    public double start;

    private void setShuffleboardTime() {
        shuffle_time.setString(String.valueOf(Timer.getFPGATimestamp() - start));
    }

    // Constructor
    public AutoManager() {
        // Logging
        log = Logger.getLogger("Auto Manager");

        // Create Shuffleboard Items
        shuffle_tab = Shuffleboard.getTab("Auto");

        shuffle_mode = shuffle_tab.add("Mode", "None")
            .withPosition(3, 0)
            .withSize(2, 1)
            .getEntry();
        shuffle_time = shuffle_tab.add("Time (Seconds)", "0.0")
            .withPosition(3, 1)
            .withSize(2, 1)
            .getEntry();

        shuffle_playbackLoaded = shuffle_tab.add("Playback Loaded?", false)
            .withPosition(0, 1)
            .withSize(1, 1)
            .getEntry();
        shuffle_playbackPercentWidget = shuffle_tab.add("Playback Progress", 0)
            .withWidget(BuiltInWidgets.kDial)
            .withProperties(Map.of("min", 0, "max", 0))
            .withPosition(1, 1)
            .withSize(2, 1);
        
        shuffle_playbackPercent = shuffle_playbackPercentWidget.getEntry();
        
        shuffle_recordId = shuffle_tab.add("Record ID", "")
            .withPosition(0, 3)
            .withSize(2, 1)
            .getEntry();
        shuffle_recordDuration = shuffle_tab.add("Duration (s)", 15)
            .withPosition(2, 3)
            .withSize(1, 1)
            .getEntry();
        shuffle_recordDelay = shuffle_tab.add("Record Delay (s)", 5)
            .withPosition(3, 3)
            .withSize(1, 1)
            .getEntry();
        shuffle_recordSaveIP = shuffle_tab.add("Save IP", "127.0.0.1")
            .withPosition(0, 4)
            .withSize(1, 1)
            .getEntry();
        shuffle_recordStatus = shuffle_tab.add("Recording Status", "Not recording...")
            .withPosition(1, 4)
            .withSize(2, 1)
            .getEntry();
        shuffle_recordisRecording = shuffle_tab.add("Recording?", false)
            .withPosition(3, 4)
            .withSize(1, 1)
            .getEntry();

        shuffle_playbackLoadBttn = new PlaybackLoader(this, shuffle_playbackLoaded, shuffle_tab);

        shuffle_tab.add("Load Playback", shuffle_playbackLoadBttn)
            .withPosition(2, 0)
            .withSize(1, 1);

        shuffle_controllers = shuffle_tab.getLayout("Controllers", BuiltInLayouts.kList)
            .withPosition(5, 0)
            .withSize(3, 5)
            .withProperties(Map.of("Label position", "TOP"));
    }

    // Adding Controllers
    private ArrayList<ControlBase> controllers = new ArrayList<ControlBase>();

    public void registerController(ControlBase c) {
        if (controllers.indexOf(c) != -1) { return; }

        // For duplicate controllers:
        for (ControlBase existing : controllers) {
            if (existing.uniqueID.equals(c.uniqueID)) {
                c.writeChangesTo = existing.writeChangesTo;
                log.info("Duplicate controllers found, now writing to same save location.");
                break;
            }
        }

        controllers.add(c);

        if (c.autoManager != this) { c.registerToController(this); }

        c.shuffleboardEntry = shuffle_controllers.add(String.format("%s (%s)", c.uniqueID, c.creationId), "Ready...").getEntry();
    }

    // Recording
    private double record_delay;
    private double record_duration;
    private HTTPHandler http;

    public void recordingInit() {
        shuffle_recordStatus.setString("Preparing controllers...");

        // Prepare controllers
        setMode(-3);
        
        for (int x = 0; x < controllers.size(); x++) {
            ControlBase c = controllers.get(x);

            c.record_lastFrameData = c.getTotalState();
            c.record_frameCount = 0;

            // Build Record Data
            StringBuilder startData = new StringBuilder();

            for (String k : c.record_lastFrameData.keySet()) {
                startData.append(String.format("%s:%s;", k, c.record_lastFrameData.get(k)));
            }

            if (startData.length() == 0) {
                log.warning(String.format("Controller %s has empty start data!", c.uniqueID));
            } else {
                startData.deleteCharAt(startData.length()-1);
            }

            c.record_recordData = new StringBuilder(String.format("->%s\n#%s", c.uniqueID, startData.toString()));
        }

        // Get Shuffleboard Values
        record_duration = shuffle_recordDuration.getDouble(15.0);
        record_delay = shuffle_recordDelay.getDouble(5.0);

        if (record_delay <= 0.0) {
            // Start recording
            shuffle_recordStatus.setString(String.format("Recording... (0/%s)", record_duration));
            setMode(2);
            shuffle_recordisRecording.setBoolean(true);
        } else {
            // Start delay
            shuffle_recordStatus.setString(String.format("Starting in: %s", record_delay));
            setMode(-4);
        }
        start = Timer.getFPGATimestamp();
    }

    // Record Periodic
    public void recordPeriodic() {
        if (mode != 2 && mode != -4) { return; }
        
        setShuffleboardTime();

        if (mode == -4) {
            // Record Delay
            if (Timer.getFPGATimestamp() - start >= record_delay) {
                shuffle_recordStatus.setString(String.format("Recording... (0/%s)", record_duration));
                setMode(2);
                shuffle_recordisRecording.setBoolean(true);
                start = Timer.getFPGATimestamp();
            } else {
                shuffle_recordStatus.setString(String.format("Starting in: %s", record_delay - (Timer.getFPGATimestamp() - start)));
            }
        } else if (mode == 2) {
            if (Timer.getFPGATimestamp() - start >= record_duration) {
                // Save
                setMode(-2);
                shuffle_recordStatus.setString("Formatting...");

                // Get Recording Data
                StringBuilder recording = new StringBuilder("--AUTO_FILE_START--");

                for (ControlBase c : controllers) {
                    if (c.writeChangesTo != c) { continue; }
                    recording.append("\n");
                    recording.append(c.record_recordData);
                }

                // Add Footer
                recording.append("\n--AUTO_FILE_END--");

                // Convert to Base64 and add Newlines
                String b64ed = new String(Base64.getEncoder().encode(recording.toString().getBytes()));
                b64ed = String.join("\n", b64ed.split("(?<=\\G.{100})")); // Splits into groups of 100 chars

                // Sav Recording Data
                shuffle_recordStatus.setString("Sending data...");
                http = new HTTPHandler(shuffle_recordSaveIP.getString("192.168.1.1"));
                http.initiatePayload(b64ed);
                http.sendPayloadTitle(shuffle_recordId.getString("Untitled Recording"));

                while (!http.doneSendingPackets()) {
                    shuffle_recordStatus.setString(String.format("Sending packet %s of %s...", http.packetsSent, http.numOfPackets));
                    http.sendNextPacket();
                }

                http.terminatePayload(true);

                unloadData();
                shuffle_recordStatus.setString("Not recording...");
            } else {
                // Update shuffleboard
                shuffle_recordStatus.setString(String.format("Recording... (Remaining: %s seconds)", record_duration - (Timer.getFPGATimestamp()-start))); // If optimization is needed, remove THIS line!
            }
        }
    }

    // Load Playback
    public String kAutoBaseFolder = "autos"; // CONFIG: set to the parent folder of the auto programs (should be in 'deploy' directory)

    public void distributePlaybackData(HashMap<String, ControllerRecording> recordings) {
        playback_totalFrames = 0;

        for (ControlBase c : controllers) {
            if (recordings.containsKey(c.uniqueID)) {
                playback_totalFrames += c.sendPlayback(recordings.get(c.uniqueID).clone());
            } else {
                log.warning(String.format("Could not find recording for: %s", c.uniqueID));
            }
        }

        shuffle_playbackPercentWidget.withProperties(Map.of("min", 0, "max", playback_totalFrames));
    }

    // Playback Threaded
    private boolean playback_threaded;
    private ScheduledExecutorService playback_executor = Executors.newSingleThreadScheduledExecutor();

    private class PlaybackThread implements Runnable {
        @Override
        public void run() {
            if (mode != 1 || playback_threaded == false) { return; }
            setShuffleboardTime();
            for (ControlBase c : controllers) { c.playbackPeriodic(); }
        }
    }

    // Start Playback
    public void playbackInit(Boolean threaded) {
        if (shuffle_playbackLoaded.getBoolean(false) == false) {
            log.warning("No auto loaded!");
            setMode(-2);
        } else {
            start = Timer.getFPGATimestamp();
            setMode(1);
            playback_threaded = threaded;
            if (threaded) {
                playback_executor.scheduleAtFixedRate(new PlaybackThread(), 0, 20, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void playbackInit() { playbackInit(true); }

    // Playback Periodic
    public void playbackPeriodic() {
        if (playback_threaded) {
            log.warning("playbackPeriodic called, even though the playback is Threaded! (Remove the function call)");
        }
        if (mode != 1) { return; }
        setShuffleboardTime();
        for (ControlBase c : controllers) { c.playbackPeriodic(); }
    }

    // Run Whenever a Controller Plays A Frame
    public void playedAFrame() {
        playback_framesPlayed ++;
        shuffle_playbackPercent.setNumber(playback_framesPlayed);

        //if (playback_framesPlayed == playback_totalFrames) { setMode(-2); unloadData(); } // Do not unload automatically when done
    }

    // Set Teleop
    public void teleopInit(Boolean unload) {
        setMode(0);
        if (unload) { unloadData(); }
    }

    public void teleopInit() { teleopInit(true); }

    // Set Disabled
    public void disabledInit(Boolean unload) {
        setMode(-1);
        if (unload) { unloadData(); }
    }

    public void disabledInit() { disabledInit(true); }

    // Unload Data (Clear Memory and Reset Shuffleboard)
    public void unloadData() {
        if (mode == 1 || mode == 2) { log.warning("unloadData called while in playback/record mode (may cause problems!)"); }
        
        if (http != null) {
            if (!http.sentDone) { http.terminatePayload(false); }
            http = null;
        }

        if (playback_threaded) { playback_executor.shutdown(); }

        for (int x = 0; x < controllers.size(); x++) { controllers.get(x).unloadData(); }

        playback_framesPlayed = 0;
        playback_totalFrames = 0;

        shuffle_playbackLoaded.setBoolean(false);
        shuffle_playbackPercent.setNumber(0);
        shuffle_playbackPercentWidget.withProperties(Map.of("min", 0, "max", 0));

        shuffle_recordId.setString("");
        shuffle_recordisRecording.setBoolean(false);

        shuffle_recordStatus.setString("Not recording...");

    }
}
