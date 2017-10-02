package simulation.environment.pedestrians;

import commons.simulation.IdGenerator;
import commons.simulation.PhysicalObject;
import commons.simulation.PhysicalObjectType;
import commons.simulation.SimulationLoopExecutable;
import commons.utils.Geometry;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import simulation.environment.geometry.osmadapter.GeomStreet;
import simulation.environment.geometry.splines.StreetInterpolator;
import simulation.util.Log;

import java.util.*;

/**
 * Class that represents a pedestrian in the simulation
 */
public class Pedestrian implements SimulationLoopExecutable, PhysicalObject, IPedestrian {

    // 4.5 km/h in meters per millisecond
    private static final double PEDESTRIAN_SPEED_DEFAULT = 4.5 / 3600;

    /**
     * Variables
     */
    private PedestrianStreetParameters params;
    private GeomStreet geomStreet;

    /** Position of the object */
    private RealVector position;

    /** Collision of the object */
    private boolean collision;

    /** Error of the object */
    private boolean error;

    /** Speed of the object */
    private RealVector speed;

    /** Rotation Z of the object */
    private double rotationZ;

    /** Unique ID */
    private long uniqueId = IdGenerator.getSharedInstance().generateUniqueId();

    private StreetInterpolator mInterpolator;

    /**
     * Constructor for a pedestrian that is standing at its position Initial
     * position at origin
     */
    public Pedestrian(GeomStreet geomStreet) {
        this.geomStreet = geomStreet;
        position = new ArrayRealVector(new double[] { 0.0, 0.0, 0.0 });
        collision = false;
        error = false;
        speed = new ArrayRealVector(new double[] { 0.0, 0.0, 0.0 });
        rotationZ = 0.0;

        mInterpolator = new StreetInterpolator(geomStreet);
    }

    /**
     * Function that gets the speed of the object
     *
     * @return Speed of the object
     */
    public RealVector getSpeed() {
        return speed.copy();
    }

    /**
     * Function that sets the speed of the object
     *
     * @param speed
     *            Speed of the object
     */
    public void setSpeed(RealVector speed) {
        this.speed = speed.copy();
    }

    /**
     * Function that sets the position of the object
     *
     * @param position
     *            Position of the object
     */
    public void setPosition(RealVector position) {
        this.position = position.copy();
    }

    /**
     * Function that sets the z rotation of the object
     *
     * @param rotationZ
     *            Z rotation of the object
     */
    public void setRotationZ(double rotationZ) {
        this.rotationZ = rotationZ;
    }

    /**
     * Function that returns the type of the object
     *
     * @return Type of the object
     */
    @Override
    public PhysicalObjectType getPhysicalObjectType() {
        return PhysicalObjectType.PHYSICAL_OBJECT_TYPE_PEDESTRIAN;
    }

    /**
     * Function that returns a vector with the x, y and z coordinates of the
     * object This refers to the center position of the geometry object (i.e.
     * NOT mass point position)
     *
     * @return Vector with x, y, z coordinates of the object center
     */
    @Override
    public RealVector getGeometryPos() {
        return position.copy();
    }

    /**
     * Function that returns a matrix with the rotation of the object
     *
     * @return Matrix with the rotation of the object
     */
    @Override
    public RealMatrix getGeometryRot() {
        Rotation rot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, rotationZ);
        return new BlockRealMatrix(rot.getMatrix());
    }

    /**
     * Function that returns the width of an object. Only meaningful for a box
     * object, otherwise returns 0.0.
     *
     * @return Width of a box object, otherwise 0.0
     */
    @Override
    public double getWidth() {
        return 0.5;
    }

    /**
     * Function that returns the length of an object. Only meaningful for a box
     * object, otherwise returns 0.0.
     *
     * @return Length of a box object, otherwise 0.0
     */
    @Override
    public double getLength() {
        return 0.5;
    }

    /**
     * Function that returns the height of an object. Only meaningful for a box
     * object, otherwise returns 0.0.
     *
     * @return Height of a box object, otherwise 0.0
     */
    @Override
    public double getHeight() {
        return 1.8;
    }

    /**
     * Function that returns the z offset of an object. This is used to
     * represent the wheel radius for vehicles, otherwise 0.0
     *
     * @return Z offset of an object, i.e. wheel radius for vehicles, otherwise
     *         0.0
     */
    @Override
    public double getOffsetZ() {
        return 0.0;
    }

    /**
     * Function that returns a boolean indicating if an object had a collision
     *
     * @return Boolean that indicates a collision of that object
     */
    @Override
    public boolean getCollision() {
        return collision;
    }

    /**
     * Function that sets collision for this object
     *
     * @param collision
     *            Boolean that indicates a collision of that object
     */
    @Override
    public void setCollision(boolean collision) {
        Log.warning("Pedestrian: setCollision - collision: " + collision + ", Pedestrian at start: " + this);
        this.collision = collision;
        Log.warning("Pedestrian: setCollision - collision: " + collision + ", Pedestrian at end: " + this);
    }

    /**
     * Function that returns a boolean indicating if an object had a computational error
     *
     * @return Boolean that indicates an error of that object
     */
    @Override
    public boolean getError() {
        return error;
    }

    /**
     * Function that sets error for this object
     *
     * @param error Boolean that indicates an error of that object
     */
    @Override
    public void setError(boolean error) {
        Log.warning("Pedestrian: setError - error: " + error + ", Pedestrian at start: " + this);
        this.error = error;
        Log.warning("Pedestrian: setError - error: " + error + ", Pedestrian at end: " + this);
    }

    /**
     * Returns the unique ID of the object. Valid IDs are positive numbers.
     *
     * @return Unique ID
     */
    public long getId() {
        return this.uniqueId;
    }

    /**
     * Returns the acceleration of the object
     * @return Acceleration in m/s^2
     */
    public RealVector getAcceleration() {
        return new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
    }

    /**
     * Returns the velocity of the object. Just a copy of getSpeed().
     * @return Velocity in m/s
     */
    public RealVector getVelocity() {
        return getSpeed();
    }

    /**
     * Returns the steering angle of the object
     * @return Steering angle
     */
    public double getSteeringAngle() {
        return 0;
    }

    /**
     * Function that returns a list of pairs of 3D points, indicating the
     * beginning and end of a vector in absolute 3D global coordinates These
     * vectors are checked for overlaps / going under the map in collision
     * detection
     *
     * @return List of pairs of 3D points, indicating the beginning and end of a
     *         vector in absolute 3D global coordinates
     */
    @Override
    public List<Map.Entry<RealVector, RealVector>> getBoundaryVectors() {

        // Build relative vectors between vertices
        RealVector relVectorBackFront = new ArrayRealVector(new double[] {0.0, getLength(), 0.0});
        RealVector relVectorLeftRight = new ArrayRealVector(new double[] {getWidth(), 0.0, 0.0});
        RealVector relVectorBottomTop = new ArrayRealVector(new double[] {0.0, 0.0, getHeight()});

        // Rotate relative vectors
        relVectorBackFront = getGeometryRot().operate(relVectorBackFront);
        relVectorLeftRight = getGeometryRot().operate(relVectorLeftRight);
        relVectorBottomTop = getGeometryRot().operate(relVectorBottomTop);

        // From center coordinate, compute to bottom left vertex of box
        RealVector absBackLeft = getGeometryPos();
        absBackLeft = absBackLeft.add(relVectorBottomTop.mapMultiply(-0.5));
        absBackLeft = absBackLeft.add(relVectorBackFront.mapMultiply(-0.5));
        absBackLeft = absBackLeft.add(relVectorLeftRight.mapMultiply(-0.5));

        // Compute absolute vectors
        RealVector backLeft = absBackLeft.copy();
        RealVector backRight = absBackLeft.add(relVectorLeftRight);
        RealVector frontLeft = absBackLeft.add(relVectorBackFront);
        RealVector frontRight = absBackLeft.add(relVectorLeftRight).add(relVectorBackFront);

        // Put vectors in list and return
        // Create map entries and insert them into list
        // Ordering is important here
        List<Map.Entry<RealVector, RealVector>> boundaryVectors = new LinkedList<>();
        boundaryVectors.add(new AbstractMap.SimpleEntry<>(backLeft, backRight));
        boundaryVectors.add(new AbstractMap.SimpleEntry<>(backRight, frontRight));
        boundaryVectors.add(new AbstractMap.SimpleEntry<>(frontRight, frontLeft));
        boundaryVectors.add(new AbstractMap.SimpleEntry<>(frontLeft, backLeft));
        return boundaryVectors;
    }

    @Override
    public RealVector getFrontLeftWheelGeometryPos() {
        return null;
    }

    @Override
    public RealVector getFrontRightWheelGeometryPos() {
        return null;
    }

    @Override
    public RealVector getBackLeftWheelGeometryPos() {
        return null;
    }

    @Override
    public RealVector getBackRightWheelGeometryPos() {
        return null;
    }

    @Override
    public void executeLoopIteration(long timeDiffMs) {
        // get last movement parameters
        PedestrianStreetParameters movementParameters = this.getStreetParameters();

        /*
        if (this.collision) {
            // If we have collided with another object we will stop movement
            return;
        }
        */

        if(Math.random() < 0.0003 && !movementParameters.isCrossing()) {
            // So there is a 0.03% chances that we will cross the street
            // This is the case and we will be just updating the state with the crossing flag
            movementParameters = new PedestrianStreetParameters(
                    true,
                    movementParameters.getPosition(),
                    movementParameters.isDirection(),
                    movementParameters.isLeftPavement()
            );
        }

        // compute new movement parameters
        // movement a pedestrians does in each time step
        double distance = PEDESTRIAN_SPEED_DEFAULT * timeDiffMs;
        //PedestrianStreetParameters newParams = this.geomStreet.getMovementOfPedestrian(movementParameters, distance);
        PedestrianStreetParameters newParams = mInterpolator.calculateNewMovement(movementParameters, distance);

        // set new movement parameters in pedestrian
        this.setStreetParameters(newParams);
    }

    @Override
    public PedestrianStreetParameters getStreetParameters() {
        return params;
    }

    @Override
    public void setStreetParameters(PedestrianStreetParameters newParams) {
        params = newParams;
        setPosition(Geometry.point3D2RealVector(newParams.getPosition()));
    }

    public void spawnAtRandomLocation(Random random) {
        PedestrianStreetParameters params = mInterpolator.spawnAtRandomLocation(random);
        setStreetParameters(params);
    }
}