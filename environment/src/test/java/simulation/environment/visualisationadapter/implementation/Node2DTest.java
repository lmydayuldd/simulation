package simulation.environment.visualisationadapter.implementation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import simulation.environment.visualisationadapter.interfaces.EnvIntersection;
import simulation.environment.visualisationadapter.interfaces.EnvNode;

import java.util.ArrayList;

/**
 * Created by lukas on 22.01.17.
 */
public class Node2DTest extends TestCase {
    /**
    * Create the test case
    *
    * @param testName name of the test case
    */
    public Node2DTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(Node2DTest.class);
    }


    public void testApp() throws Exception {
        EnvNode n1 = new Node2D(2,1,10);
        EnvNode n2 = new Node2D(2,1,10);
        EnvNode n3 = new Node2D(223,2,11);
        EnvNode n4 = new Node2D(26,14,12);
        EnvNode n5 = new Node2D(25,11,13);
        EnvIntersection n6 = new Intersection2D(2,1,10);

        ArrayList<EnvNode> nodes = new ArrayList<>();
        nodes.add(n1);
        nodes.add(n3);
        nodes.add(n4);
        nodes.add(n5);
        nodes.add(n1);

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
        assertFalse(n1.equals(n3));

        assertTrue(n6.equals(n1));
        assertTrue(n1.equals(n6));

        assertTrue(nodes.contains(n2));
        assertTrue(nodes.contains(new Node2D(2,1,10)));

        assertEquals(0, nodes.indexOf(n1));
        assertEquals(nodes.indexOf(n1), nodes.indexOf(n6));

        assertEquals(4, nodes.lastIndexOf(new Node2D(2,1,10)));

    }
}
