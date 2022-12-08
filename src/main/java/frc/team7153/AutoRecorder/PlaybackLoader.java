package frc.team7153.AutoRecorder;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;

public class PlaybackLoader extends CommandBase {
    // Shuffleboard
    private NetworkTableEntry shuffleboardPlaybackLoaded;

    SendableChooser<File> playbackChoose = new SendableChooser<>();

    // AutoManager
    private AutoManager parentManager;

    // Logger
    private Logger log = Logger.getLogger("Auto Playback Loader");

    // List Files In Dir
    private class fileList {
        public ArrayList<File> files = new ArrayList<File>();
        public ArrayList<String> titles = new ArrayList<String>();
    }

    private fileList listDir(File dir, String relativePath) {
        fileList files = new fileList();

        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                files.files.add(f);
                files.titles.add(relativePath + f.getName());
            } else {
                fileList childFiles = listDir(f, relativePath + f.getName() + "/");
                files.files.addAll(childFiles.files);
                files.titles.addAll(childFiles.titles);
            }
        }

        return files;
    }

    // Constructor
    public PlaybackLoader(AutoManager manager, NetworkTableEntry shuffleboard_playbackLoaded, ShuffleboardTab tab) {
        shuffleboardPlaybackLoaded = shuffleboard_playbackLoaded;
        parentManager = manager;

        // Create Dropdown
        fileList files = listDir(new File(Filesystem.getDeployDirectory(), parentManager.kAutoBaseFolder), "");
        boolean first = true;

        for (int x = 0; x < files.files.size(); x++) {
            if (first) {
                playbackChoose.setDefaultOption(files.titles.get(x), files.files.get(x));
                first = false;
            } else {
                playbackChoose.addOption(files.titles.get(x), files.files.get(x));
            }
        }

        tab.add("Playback ID", playbackChoose)
            .withPosition(0, 0)
            .withSize(2, 1);
    }

    // Give Button Name
    @Override
    public String getName() { return "Load"; }

    // Run When Disabled
    @Override
    public boolean runsWhenDisabled() { return true; }

    // Load Playback (Button Pressed)
    @Override
    public void initialize() {
        // Can Not Load Playbacks While Playbacks Are Playing!
        if (parentManager.getMode() == 1) {
            log.warning("New playbacks can NOT be loaded while another playback is playing!");
            return;
        }

        File selected = playbackChoose.getSelected();

        shuffleboardPlaybackLoaded.setBoolean(false);

        // Read File
        try {
            StringBuilder fileReader = new StringBuilder();
            Scanner fileScanner = new Scanner(selected);

            while (fileScanner.hasNextLine()) {
                fileReader.append(fileScanner.nextLine());
            }

            fileScanner.close();

            String fileContents = fileReader.toString().replace("\n", "");
            ArrayList<String> data = new ArrayList<String>(Arrays.asList(new String(Base64.getDecoder().decode(fileContents)).split("\n")));
            

            if (data.get(0).equals("--AUTO_FILE_START--")) {
                data.remove(0);
            } else {
                throw new Exception("Auto file did not start with correct auto file heading! (File may be corrupt)");
            }

            int last = data.size()-1;
            if (data.get(last).equals("--AUTO_FILE_END--")) {
                data.remove(last);
            } else {
                throw new Exception("Auto file did not end with correct auto file footer! (File may be corrupt)");
            }

            // Parse Data
            ControllerRecording workingRecording = null;
            HashMap<String, ControllerRecording> fullRecording = new HashMap<String, ControllerRecording>();

            for (String line : data) {
                if (line.substring(0, 2).equals("->")) {
                    if (workingRecording != null) {
                        fullRecording.put(workingRecording.id, workingRecording);
                    }

                    workingRecording = new ControllerRecording(line.substring(2, line.length()));
                } else if (line.substring(0, 1).equals("#")) {
                    for (String kvPair : line.substring(1).split(";")) {
                        String[] kv = kvPair.split(":");
                        workingRecording.addStartData(kv[0], Double.parseDouble(kv[1]));
                    }
                } else if (line.substring(0, 1).equals(">")) {
                    String[] newState = line.substring(1).split(",");
                    workingRecording.addFrame(Double.parseDouble(newState[0]), newState[1], Double.parseDouble(newState[2]));
                } else {
                    log.warning("[POSSIBLY FATAL]: UNKNOWN LINE FOUND IN PLAYBACK FILE (CHECK FOR CORRUPT FILE)");
                }
            }

            if (workingRecording != null) {
                fullRecording.put(workingRecording.id, workingRecording);
            }

            parentManager.distributePlaybackData(fullRecording);

            shuffleboardPlaybackLoaded.setBoolean(true);
            this.cancel();
        } catch (Exception e) {
            log.warning(String.format("Could not load playback: %s", e));
            return;
        }
    }
}
