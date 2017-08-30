package simulation.environment.geometry;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import simulation.environment.geometry.osmadapter.LinearSplineDeterminator;
import simulation.environment.geometry.osmadapter.SplineDeterminator;
import simulation.environment.osm.Parser2D;
import simulation.environment.osm.ParserSettings;
import simulation.environment.visualisationadapter.implementation.Node2D;
import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;
import simulation.environment.visualisationadapter.interfaces.VisualisationEnvironmentContainer;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by lukas on 31.01.17.
 */
public class LinearSplineDeterminatorTest extends TestCase {
    public LinearSplineDeterminatorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(LinearSplineDeterminatorTest.class);
    }

    public void testApp() throws Exception {
        InputStream in = getClass().getResourceAsStream("/min_intersection_test.osm");
        Parser2D p = new Parser2D(new ParserSettings(in, ParserSettings.ZCoordinates.ALLZERO));

        p.parse();
        VisualisationEnvironmentContainer c = p.getContainer();
        ArrayList<SplineDeterminator> splines = new ArrayList<>();
        for(EnvStreet s : c.getStreets()) {
            splines.add(new LinearSplineDeterminator(s));
        }

        EnvNode n = new Node2D(0, 0, 0, 0);

        SplineDeterminator minSplineDeterminator = null;
        double minDist = Double.MAX_VALUE;
        for(SplineDeterminator s : splines) {
            System.out.println(s.getStreet().getNodes());
            System.out.println(s.determineSplineDistance(n));

        }


    }

}
