package simulation.vehicle;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.junit.*;
import simulation.util.Log;
import simulation.util.MathHelper;

import static org.junit.Assert.*;

/**
 * Class that tests the MassPoint class
 */
public class MassPointTest {

    private MassPoint mp = null;

    @BeforeClass
    public static void setUpClass() {
        Log.setLogEnabled(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Log.setLogEnabled(true);
    }

    @Before
    public void setUp() {
        RealVector localPos = new ArrayRealVector(new double[] {0.2346, 0.3678, 0.2486});
        RealVector localCenterDiff = new ArrayRealVector(new double[] {5.2346, 0.678, 1.2486});
        RealVector pos = new ArrayRealVector(new double[] {249.2346, 10.3678, 3.2486});
        RealVector centerDiff = new ArrayRealVector(new double[] {240.2346, 11.3678, 2.2486});
        RealVector velocity = new ArrayRealVector(new double[] {0.0, 50.3, -0.25});
        RealVector acceleration = new ArrayRealVector(new double[] {0.0, 10.0, -0.1});
        RealVector force = new ArrayRealVector(new double[] {5.02, 0.1, -39.4});
        double mass = 500.0;
        mp = new MassPoint(MassPointType.MASS_POINT_TYPE_UNKNOWN, localPos, localCenterDiff, pos, centerDiff, velocity, acceleration, force, mass);
    }

    @Test
    public void testGetterAndSetter() {

        RealVector localPos = new ArrayRealVector(new double[] {0.2346, 0.3678, 0.2486});
        RealVector localCenterDiff = new ArrayRealVector(new double[] {5.2346, 0.678, 1.2486});
        RealVector pos = new ArrayRealVector(new double[] {249.2346, 10.3678, 3.2486});
        RealVector centerDiff = new ArrayRealVector(new double[] {240.2346, 11.3678, 2.2486});
        RealVector velocity = new ArrayRealVector(new double[] {0.0, 50.3, -0.25});
        RealVector acceleration = new ArrayRealVector(new double[] {0.0, 10.0, -0.1});
        RealVector force = new ArrayRealVector(new double[] {5.02, 0.1, -39.4});
        double mass = 500.0;

        // Ensure that constructor works
        assertTrue(MathHelper.vectorEquals(mp.getLocalPos(), localPos, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getLocalCenterDiff(), localCenterDiff, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getPos(), pos, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getCenterDiff(), centerDiff, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getVelocity(), velocity, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getAcceleration(), acceleration, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getForce(), force, 0.00000001));
        assertTrue(mp.getMass() == mass);

        // Modify local copies of vectors, mass point values should not change!
        localPos.setEntry(0, 0.3256);
        localCenterDiff.setEntry(0, 0.3256);
        pos.setEntry(0, 0.3256);
        centerDiff.setEntry(0, 0.3256);
        velocity.setEntry(0, 0.3256);
        acceleration.setEntry(0, 0.3256);
        force.setEntry(0, 0.3256);

        // Now the vectors should not be the same anymore
        assertFalse(MathHelper.vectorEquals(mp.getLocalPos(), localPos, 0.00000001));
        assertFalse(MathHelper.vectorEquals(mp.getLocalCenterDiff(), localCenterDiff, 0.00000001));
        assertFalse(MathHelper.vectorEquals(mp.getPos(), pos, 0.00000001));
        assertFalse(MathHelper.vectorEquals(mp.getCenterDiff(), centerDiff, 0.00000001));
        assertFalse(MathHelper.vectorEquals(mp.getVelocity(), velocity, 0.00000001));
        assertFalse(MathHelper.vectorEquals(mp.getAcceleration(), acceleration, 0.00000001));
        assertFalse(MathHelper.vectorEquals(mp.getForce(), force, 0.00000001));

        // Use setter to modify data
        mp.setLocalPos(localPos);
        mp.setLocalCenterDiff(localCenterDiff);
        mp.setPos(pos);
        mp.setCenterDiff(centerDiff);
        mp.setVelocity(velocity);
        mp.setAcceleration(acceleration);
        mp.setForce(force);
        mp.setMass(600.0);

        // Ensure that setter work
        assertTrue(MathHelper.vectorEquals(mp.getLocalPos(), localPos, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getLocalCenterDiff(), localCenterDiff, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getPos(), pos, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getCenterDiff(), centerDiff, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getVelocity(), velocity, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getAcceleration(), acceleration, 0.00000001));
        assertTrue(MathHelper.vectorEquals(mp.getForce(), force, 0.00000001));
        assertTrue(mp.getMass() == 600.0);
    }
}