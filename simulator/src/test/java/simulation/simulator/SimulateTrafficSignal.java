package simulation.simulator;

import org.junit.Before;
import org.junit.Test;
import simulation.environment.visualisationadapter.implementation.TrafficSignalImpl;

import static org.junit.Assert.assertTrue;
import static simulation.environment.visualisationadapter.interfaces.TrafficSignalStatus.*;

/**
 * Created by Shahriar Robbani on 28-Jan-17.
 */
public class SimulateTrafficSignal {

    @Before
    public void setUp() {
        Simulator.resetSimulator();

        //Set update frequency to 30 loop iterations per second
        Simulator sim = Simulator.getSharedInstance();
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_FIXED_TIME);
        sim.setSimulationLoopFrequency(30);
        sim.setSynchronousSimulation(true);
        sim.setPausedInFuture(true);
    }

    @Test
    public void testLight() {
        Simulator simulator = Simulator.getSharedInstance();
        TrafficSignalImpl trafficSignal = new TrafficSignalImpl();
        simulator.registerSimulationObject(trafficSignal);

        // initially the signalA will be Green and signalB will be Red.
        assertTrue(trafficSignal.getSignalA() == GREEN);
        assertTrue(trafficSignal.getSignalB() == RED);

        // After 20 sec both signal will be yellow for 10 ms
        simulator.stopAfter(22000);
        simulator.startSimulation();
        assertTrue(trafficSignal.getSignalA() == YELLOW);
        assertTrue(trafficSignal.getSignalB() == YELLOW);

        // After 30 sec signalA will be Red and the signalB will be Green.
        simulator.extendSimulationTime(10000);
        simulator.startSimulation();
        assertTrue(trafficSignal.getSignalA() == RED);
        assertTrue(trafficSignal.getSignalB() == GREEN);

        // After 50 sec both signal will be yellow for 10 ms
        simulator.extendSimulationTime(20000);
        simulator.startSimulation();
        assertTrue(trafficSignal.getSignalA() == YELLOW);
        assertTrue(trafficSignal.getSignalB() == YELLOW);


        // After 70s signalA will be Green and signalB will be Red again.
        simulator.extendSimulationTime(20000);
        simulator.startSimulation();
        assertTrue(trafficSignal.getSignalA() == GREEN);
        assertTrue(trafficSignal.getSignalB() == RED);


    }
}
