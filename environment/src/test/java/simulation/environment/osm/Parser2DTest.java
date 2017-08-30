package simulation.environment.osm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.InputStream;

/**
 * Created by lukas on 08.01.17.
 */
public class Parser2DTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public Parser2DTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(Parser2DTest.class);
    }


    public void testApp() throws Exception {
        // test will always failed with maven because it should read the file from inside the jar.
        // String filePath = "src/test/data/map_buildings_test.osm";
        InputStream in = getClass().getResourceAsStream("/map_buildings_test.osm");
        Parser2D parser = new Parser2D(new ParserSettings(in, ParserSettings.ZCoordinates.ALLZERO));
        parser.parse();

        assertNotNull(parser.getBuildings());
        assertNotNull(parser.getStreets());


    }
}
