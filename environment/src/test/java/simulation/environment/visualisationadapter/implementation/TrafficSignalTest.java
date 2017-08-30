package simulation.environment.visualisationadapter.implementation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import simulation.environment.object.TrafficLightSwitcher;
import simulation.environment.visualisationadapter.interfaces.SignTypeAndState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 13.03.17.
 */
public class TrafficSignalTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TrafficSignalTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TrafficSignalTest.class);
    }

    public void testApp() {
        TrafficLight t1 = new TrafficLight(0);
        TrafficLight t2 = new TrafficLight(1);
        TrafficLight t3 = new TrafficLight(2);

        List<TrafficLight> list = new ArrayList<>();
        list.add(t1);
        list.add(t2);
        list.add(t3);

        assertEquals(0l, t1.getId());
        assertEquals(1l, t2.getId());
        assertEquals(2l, t3.getId());


        TrafficLightSwitcher switcher = new TrafficLightSwitcher(list);

        assertEquals(new ArrayList<>(), switcher.getChangedState());

        switcher.executeLoopIteration(20000);

        List<Long> expectedIds = new ArrayList<>();
        expectedIds.add(0l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_GREEN, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t3.getSignState());


        switcher.executeLoopIteration(20000);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_YELLOW, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(0l);
        expectedIds.add(1l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED_YELLOW, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(1l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_GREEN, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(1l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_YELLOW, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(1l);
        expectedIds.add(2l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED_YELLOW, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(2l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_GREEN, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(2l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_YELLOW, t3.getSignState());

        switcher.executeLoopIteration(20000);
        expectedIds.clear();
        expectedIds.add(0l);
        expectedIds.add(2l);
        assertTrue(expectedIds.containsAll(switcher.getChangedState()));
        assertTrue(switcher.getChangedState().containsAll(expectedIds));
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED_YELLOW, t1.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t2.getSignState());
        assertEquals(SignTypeAndState.TRAFFIC_LIGHT_RED, t3.getSignState());





    }
}
