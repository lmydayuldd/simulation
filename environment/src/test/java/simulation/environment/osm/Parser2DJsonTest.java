package simulation.environment.osm;

import com.google.gson.Gson;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.InputStream;

/**
 * Created by Shahriar Robbani on 08-Jan-17.
 */
public class Parser2DJsonTest extends TestCase {
    public static Test suite()
    {
        return new TestSuite( Parser2DJsonTest.class );
    }
    public void testApp() throws Exception {
        InputStream in = getClass().getResourceAsStream("/map_buildings_test.osm");
        Parser2D parser = new Parser2D(new ParserSettings(in, ParserSettings.ZCoordinates.ALLZERO));
        parser.parse();

        Gson gson = new Gson();
        String jsonString = gson.toJson(parser.getContainer());

        assertNotNull(jsonString);

        System.out.println(jsonString);

    }
}
