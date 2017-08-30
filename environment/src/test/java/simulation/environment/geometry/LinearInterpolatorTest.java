package simulation.environment.geometry;

import commons.map.IControllerNode;
import javafx.geometry.Point3D;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import simulation.environment.WorldModel;
import simulation.environment.geometry.splines.LinearInterpolator;
import simulation.environment.pedestrians.PedestrianStreetParameters;
import simulation.environment.visualisationadapter.interfaces.EnvStreet;

import java.util.ArrayList;

/**
 * Created by lukas on 21.01.17.
 */
public class LinearInterpolatorTest extends TestCase {
    public LinearInterpolatorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(LinearInterpolatorTest.class);
    }

    public void testApp() throws Exception {
        LinearInterpolator interpol = new LinearInterpolator(new Point3D(3,3,0), new Point3D(1,1,0), EnvStreet.STREET_WIDTH, -1l, -1l, true);

        double dist = interpol.computeDistanceToMiddle(new Point3D(2,1,0));

        assertEquals(Math.sqrt(0.5), dist);


        Point3D p = interpol.computePoint(8);
        assertEquals(0d, interpol.computeDistanceToMiddle(p));

        assertEquals(EnvStreet.STREET_WIDTH, interpol.computeDistanceToLeft(p) + interpol.computeDistanceToRight(p), LinearInterpolator.precision);

        interpol = new LinearInterpolator(new Point3D(-5,-1,0), new Point3D(3,0,0), 0.006, -1l, -1l, true);

        assertFalse(interpol.isOnStreet(new Point3D(6,0,0)));

        assertTrue(interpol.isOnStreet(new Point3D(3,0,0)));

        interpol = new LinearInterpolator(new Point3D(0,0,0), new Point3D(1,2,0), 2*Math.sqrt(5), -1l, -1l, true);

        assertTrue(interpol.isOnStreet(new Point3D(2,0,0)));

        assertFalse(interpol.isOnStreet(new Point3D(3,-2,0)));

        interpol = new LinearInterpolator(new Point3D(-5,-1,0), new Point3D(3,0,0), 0.006, -1l, -1l, true);



        assertEquals(-0d,interpol.computeT(new Point3D(-5,-1,0)));

        assertEquals(0.5,interpol.computeT(new Point3D(-1,-0.5,0)));

        assertEquals(1d, interpol.computeT(new Point3D(3,0,0)));

        p = new Point3D(2,3,4);

        assertEquals(interpol.computeDistanceToMiddle(p), interpol.computePoint(interpol.computeT(p)).distance(p), LinearInterpolator.precision);

        interpol = new LinearInterpolator(new Point3D(1,1,0), new Point3D(2,2,0), EnvStreet.STREET_WIDTH, -1, -1, true);
        ArrayList<IControllerNode> cNodes = interpol.convertToControllerList();
        for(int i = 0; i < interpol.convertToControllerList().size() - 1; i++) {
            IControllerNode c1 = interpol.convertToControllerList().get(i);
            IControllerNode c2 = interpol.convertToControllerList().get(i + 1);
            double cDist = c1.getPoint().distance(c2.getPoint());
            assertTrue(cDist <= IControllerNode.INTERPOLATION_DISTANCE + Math.pow(10,-10));
        }


        interpol = new LinearInterpolator(new Point3D(0,0,0), new Point3D(4,4,4), 3, -1, -1, true, 1.d);

        assertTrue(interpol.isOnStreet(new Point3D(3,1,2)));

        assertFalse(interpol.isOnStreet(new Point3D(4.5,1.5,3)));

        assertTrue(interpol.isOnPavement(new Point3D(4.5,1.5,3)));

        assertFalse(interpol.isOnPavement(new Point3D(4.5,1.5,2)));

        assertFalse(interpol.isOnPavement(new Point3D(4,0,2)));

        assertFalse(interpol.isOnStreet(new Point3D(4,0,2)));

        WorldModel world = (WorldModel) (WorldModel.getInstance());

        interpol = new LinearInterpolator(new Point3D(0,0,0), new Point3D(0,4,0), 3, -1, -1, true, 1.d);

        assertEquals(new Point3D(0.75,1,0), interpol.spawnCar(true, new Point3D(1,1,1)));

        interpol = new LinearInterpolator(new Point3D(0,0,0), new Point3D(8,8,0), 3, -1, -1, true, 1.d);

        Point3D spawningPoint = interpol.spawnCar(true, new Point3D(1.5,1.5,0));
        Point3D expectedPoint = new Point3D(2,1,0);

        assertEquals(expectedPoint.getX(), spawningPoint.getX(), 0.04);
        assertEquals(expectedPoint.getY(), spawningPoint.getY(), 0.04);
        assertEquals(expectedPoint.getZ(), spawningPoint.getZ(), 0.04);

        assertEquals(0.75,new Point3D(1.5,1.5,0).distance(spawningPoint), 0.04);


        spawningPoint = interpol.spawnCar(true, new Point3D(2,2,0));
        expectedPoint = new Point3D(2.5,1.5,0);

        assertEquals(expectedPoint.getX(), spawningPoint.getX(), 0.04);
        assertEquals(expectedPoint.getY(), spawningPoint.getY(), 0.04);
        assertEquals(expectedPoint.getZ(), spawningPoint.getZ(), 0.04);

        assertEquals(0.75,new Point3D(2,2,0).distance(spawningPoint), 0.04);

        spawningPoint = interpol.spawnCar(false, new Point3D(2,2,0));
        expectedPoint = new Point3D(1.5,2.5,0);

        assertEquals(expectedPoint.getX(), spawningPoint.getX(), 0.04);
        assertEquals(expectedPoint.getY(), spawningPoint.getY(), 0.04);
        assertEquals(expectedPoint.getZ(), spawningPoint.getZ(), 0.04);

        assertEquals(0.75,new Point3D(2,2,0).distance(spawningPoint), 0.04);

        spawningPoint = interpol.spawnCar(false, new Point3D(1.5,1.5,0));
        expectedPoint = new Point3D(1,2,0);

        assertEquals(expectedPoint.getX(), spawningPoint.getX(), 0.04);
        assertEquals(expectedPoint.getY(), spawningPoint.getY(), 0.04);
        assertEquals(expectedPoint.getZ(), spawningPoint.getZ(), 0.04);

        assertEquals(0.75,new Point3D(1.5,1.5,0).distance(spawningPoint), 0.04);

        boolean isLeft = true;
        LinearInterpolator pavement = interpol.getPavement(isLeft);
        Point3D initPoint = pavement.computePoint(0.5);

        assertEquals(initPoint, interpol.computePointForPedestrian(new PedestrianStreetParameters(false, initPoint, true, isLeft), Math.sqrt(128)+1).getPosition());

        assertTrue(initPoint.distance(pavement.getP1()) < interpol.computePointForPedestrian(new PedestrianStreetParameters(false, initPoint, true, isLeft), 1).getPosition().distance(pavement.getP1()));

        Point3D newPoint = interpol.computePointForPedestrian(new PedestrianStreetParameters(true, initPoint, true, isLeft), 1).getPosition();
        assertTrue(interpol.computeDistanceToMiddle(initPoint) > interpol.computeDistanceToMiddle(newPoint));

        initPoint = interpol.computePoint(0.5);

        newPoint = interpol.computePointForPedestrian(new PedestrianStreetParameters(true, initPoint, true, isLeft), 2).getPosition();
        assertTrue(interpol.isOnPavement(newPoint));


        Point3D p1 = new Point3D(-1,1,0);

        System.out.println(p1.angle(0,1,0));
        System.out.println(Math.toRadians(p1.angle(0,1,0)));

    }
}
