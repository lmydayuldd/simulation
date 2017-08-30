package simulation.environment.object;


import commons.simulation.SimulationLoopExecutable;
import simulation.environment.visualisationadapter.implementation.TrafficLight;
import simulation.environment.visualisationadapter.interfaces.SignTypeAndState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 10.03.17.
 */
public class TrafficLightSwitcher implements SimulationLoopExecutable{

    private static List<TrafficLightSwitcher> switcher = new ArrayList<>();

    public static List<TrafficLightSwitcher> getSwitcher() {
        return switcher;
    }

    public static void addSwitcher(TrafficLightSwitcher switcherInst) {
        switcher.add(switcherInst);
    }


    private long time;

    private List<TrafficLight> signals;

    private int currentIndex;

    private List<Long> changedState;

    public TrafficLightSwitcher(List<TrafficLight> signals) {
        this.signals = signals;
        this.time = 0l;
        this.currentIndex = 0;
        this.changedState = new ArrayList<>();
        initSignalStates();
    }

    private void initSignalStates() {
        for(int i = 0; i < signals.size(); i++) {
            if(i == currentIndex) {
                signals.get(i).setState(SignTypeAndState.TRAFFIC_LIGHT_GREEN);
            } else {
                signals.get(i).setState(SignTypeAndState.TRAFFIC_LIGHT_RED);
            }
        }
    }


    @Override
    public void executeLoopIteration(long l) {
        this.changedState.clear();
        this.time += l;
        if(time >= 30000 && time <= 40000) {
            this.signals.get(currentIndex).setState(SignTypeAndState.TRAFFIC_LIGHT_YELLOW);
            this.changedState.add(this.signals.get(currentIndex).getId());
        }

        if(time > 40000) {
            this.signals.get(currentIndex).setState(SignTypeAndState.TRAFFIC_LIGHT_RED);
            this.changedState.add(this.signals.get(currentIndex).getId());

            currentIndex++;
            currentIndex = currentIndex % this.signals.size();


            this.signals.get(currentIndex).setState(SignTypeAndState.TRAFFIC_LIGHT_RED_YELLOW);
            this.changedState.add(this.signals.get(currentIndex).getId());
            this.time = 0;
        }

        if(time > 10000 && time < 30000) {
            this.signals.get(currentIndex).setState(SignTypeAndState.TRAFFIC_LIGHT_GREEN);
            this.changedState.add(this.signals.get(currentIndex).getId());
        }
    }

    public List<Long> getChangedState() {
        return this.changedState;
    }
}
