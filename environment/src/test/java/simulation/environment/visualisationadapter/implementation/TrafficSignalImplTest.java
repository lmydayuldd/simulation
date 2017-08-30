package simulation.environment.visualisationadapter.implementation;

import org.junit.Test;

/**
 * Created by eh5i0 on 28-Jan-17.
 */
public class TrafficSignalImplTest {
    @Test
    public void executeLoopIteration() throws Exception {
        TrafficSignalImpl trafficSignal = new TrafficSignalImpl();
        trafficSignal.executeLoopIteration(30);
    }

}