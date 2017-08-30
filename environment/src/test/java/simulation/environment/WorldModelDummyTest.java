package simulation.environment;

import org.junit.Test;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.environment.visualisationadapter.interfaces.VisualisationEnvironmentContainer;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Christian on 11.11.2016.
 */
public class WorldModelDummyTest {
    @Test
    public void getGround() throws Exception {

    }

    @Test
    public void getContainer() {
        WorldModelDummy worldModelDummy = new WorldModelDummy();
        VisualisationEnvironmentContainer container;
        try {
            container = worldModelDummy.getContainer();
            ArrayList<EnvNode> intersections = new ArrayList<>();
            for(EnvStreet street : container.getStreets()) {

                for(EnvNode n : street.getNodes()) {
                    assertTrue(n.getX().doubleValue() >= 0);
                    assertTrue(n.getY().doubleValue() >= 0);
                }

                for(EnvNode in : street.getIntersections()) {
                    assertTrue(in.getX().doubleValue() >= 0);
                    assertTrue(in.getY().doubleValue() >= 0);
                    assertTrue(street.getNodes().contains(in));
                }

                intersections.addAll(street.getIntersections());
            }


            HashMap<EnvNode, Integer> numberOfStreets = new HashMap<>();
            for(EnvNode n : intersections) {
                for(EnvStreet street : container.getStreets()) {
                    if(!numberOfStreets.containsKey(n)) {
                        numberOfStreets.put(n, 0);
                    }

                    if(street.getNodes().contains(n)) {
                        numberOfStreets.put(n,numberOfStreets.get(n) + 1);
                    }
                }
            }


            for(EnvNode n : numberOfStreets.keySet()) {
                assertTrue(numberOfStreets.get(n) >= 1);
            }

        } catch (Exception e) {
            fail();
        }




    }

}