package simulation.environment;

import junit.framework.TestCase;
import org.junit.Test;
import simulation.environment.osm.ParserSettings;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.environment.visualisationadapter.interfaces.SignTypeAndState;
import simulation.environment.weather.WeatherSettings;

public class WorldModelTest extends TestCase {
    @Test
    public void testApp() throws Exception {
        try {
            World world = WorldModel.getInstance();
        } catch (Exception e) {
            fail();
        }


        WorldModel.init(new ParserSettings(getClass().getResourceAsStream("/map_ahornstrasse.osm"),
                                            ParserSettings.ZCoordinates.STATIC), new WeatherSettings());

        World world = WorldModel.getInstance();

        boolean allZero = true;

        for(EnvStreet s : world.getContainer().getStreets()) {
            for(EnvNode n : s.getNodes()) {
                allZero &= n.getZ().doubleValue() == 0;
            }
        }

        assertFalse(allZero);


        WorldModel.init(new ParserSettings(getClass().getResourceAsStream("/map_ahornstrasse.osm"),
                ParserSettings.ZCoordinates.ALLZERO), new WeatherSettings());

        world = WorldModel.getInstance();

        allZero = true;

        for(EnvStreet s : world.getContainer().getStreets()) {
            for(EnvNode n : s.getNodes()) {
                allZero &= n.getZ().doubleValue() == 0;
            }
        }

        assertTrue(allZero);


        WorldModel.init(new ParserSettings(getClass().getResourceAsStream("/map_ahornstrasse.osm"),
                ParserSettings.ZCoordinates.RANDOM), new WeatherSettings());

        world = WorldModel.getInstance();

        allZero = true;

        for(EnvStreet s : world.getContainer().getStreets()) {
            for(EnvNode n : s.getNodes()) {
                allZero &= n.getZ().doubleValue() == 0;
            }
        }

        assertFalse(allZero);


        WorldModel.init(new ParserSettings(getClass().getResourceAsStream("/map_ahornstrasse.osm"),
                ParserSettings.ZCoordinates.ALLZERO), new WeatherSettings());

        world = WorldModel.getInstance();

        assertTrue(world.getContainer().getBounds().getMinX() < 0);
        assertTrue(world.getContainer().getBounds().getMinY() < 0);
        assertTrue(world.getContainer().getBounds().getMinZ() == 0);

        WorldModel.init(new ParserSettings(getClass().getResourceAsStream("/map_ahornstrasse.osm"),
                ParserSettings.ZCoordinates.STATIC), new WeatherSettings());

        world = WorldModel.getInstance();
        assertTrue(world.getContainer().getBounds().getMinX() < 0);
        assertTrue(world.getContainer().getBounds().getMinY() < 0);
        assertTrue(world.getContainer().getBounds().getMinZ() >= 0);

        WorldModel.init(new ParserSettings(getClass().getResourceAsStream("/map_ahornstrasse.osm"),
                ParserSettings.ZCoordinates.RANDOM), new WeatherSettings());

        world = WorldModel.getInstance();
        assertTrue(world.getContainer().getBounds().getMinX() < 0);
        assertTrue(world.getContainer().getBounds().getMinY() < 0);
        assertTrue(world.getContainer().getBounds().getMinZ() >= 0);


        for(EnvStreet s : world.getContainer().getStreets()) {
            for(EnvNode n : s.getNodes()) {
                if(n.getStreetSign().getType() != SignTypeAndState.EMPTY_SIGN) {
                    System.out.println(n.getStreetSign().getX1());
                    System.out.println(n.getStreetSign().getX2());
                    assertTrue(n.getStreetSign().isOne() || n.getStreetSign().isTwo());
                }
            }
        }
    }
}
