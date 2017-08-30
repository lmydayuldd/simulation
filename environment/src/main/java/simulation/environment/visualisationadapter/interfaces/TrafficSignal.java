package simulation.environment.visualisationadapter.interfaces;

import commons.simulation.SimulationLoopExecutable;

/**
 * Created by Shahriar Robbani on 26.01.17.
 */
@Deprecated
public interface TrafficSignal extends SimulationLoopExecutable {
    public TrafficSignalStatus getSignalA();

    public TrafficSignalStatus getSignalB();
}
