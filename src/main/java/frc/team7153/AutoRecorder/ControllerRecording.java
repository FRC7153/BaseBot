package frc.team7153.AutoRecorder;

import java.util.ArrayList;
import java.util.HashMap;

public class ControllerRecording {
    // Data
    public String id;

    public HashMap<String, Double> startData = new HashMap<String, Double>();
    private ArrayList<Double> times = new ArrayList<Double>();
    private ArrayList<String> bttns = new ArrayList<String>();
    private ArrayList<Double> states = new ArrayList<Double>();

    // Constructor
    public ControllerRecording(String uniqueId) { id = uniqueId; }

    // Constructor (For cloning)
    public ControllerRecording(String uniqueId, HashMap<String, Double> sd, ArrayList<Double> t, ArrayList<String> b, ArrayList<Double> s) {
        id = uniqueId;
        startData = sd;
        times = t;
        bttns = b;
        states = s;
    }

    // Clone
    @SuppressWarnings("unchecked")
    public ControllerRecording clone() { return new ControllerRecording(id, new HashMap<String, Double>(startData), (ArrayList<Double>)times.clone(), (ArrayList<String>)bttns.clone(), (ArrayList<Double>)states.clone()); }

    // Add Start Data
    public void addStartData(String bttn, Double state) { startData.put(bttn, state); }

    // Add Frame
    public void addFrame(Double time, String bttn, Double state) { times.add(time); bttns.add(bttn); states.add(state); }
    
    // Get Number of Frames
    public int numOfFrames() { return times.size(); }

    // Get First Frame
    public Object[] getFirstFrame() {
        if (times.size() == 0) { return null; }

        Object[] data = {times.get(0), bttns.get(0), states.get(0)};
        return data;
    }

    // Remove First Frame
    public void removeFirstFrame() {
        if (times.size() == 0) { return; }

        times.remove(0);
        bttns.remove(0);
        states.remove(0);
    }
}
