package simulation.environment.osm;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.InputStream;

/**
 * Created by lukas on 15.12.16.
 */
public class IntersectionFinderTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public IntersectionFinderTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(IntersectionFinderTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        // maven test will fail for this file path
//        String filePath = "src/test/data/min_intersection_test.osm";

        InputStream in = getClass().getResourceAsStream("/min_intersection_test.osm");

        InMemoryMapDataSet data = null;


/*        OSMImporter importer = new OSMImporter(filePath);
        try {
            importer.parse();
            data = importer.getData();
        } catch (OsmInputException e) {
            e.printStackTrace();
            fail();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail();
        }*/

        // Parser2D is more appropriate
        Parser2D parser2D = new Parser2D(new ParserSettings(in, ParserSettings.ZCoordinates.ALLZERO));
        try {
            parser2D.parse();
            data = parser2D.getDataSet();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        IntersectionFinder finder = IntersectionFinder.getInstance();
        finder.findIntersections(data);

        assertEquals(1, finder.getNumberOfIntersections());
    }
}
