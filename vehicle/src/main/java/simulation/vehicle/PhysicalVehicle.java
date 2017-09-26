package simulation.vehicle;

import commons.controller.interfaces.Bus;
import commons.controller.interfaces.FunctionBlockInterface;
import commons.simulation.IdGenerator;
import commons.simulation.PhysicalObject;
import commons.simulation.PhysicalObjectType;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.linear.*;
import commons.simulation.SimulationLoopExecutable;
import simulation.environment.WorldModel;
import simulation.util.Log;
import simulation.util.MathHelper;

import java.util.*;

import static simulation.vehicle.MassPointType.*;
import static simulation.vehicle.VehicleActuatorType.*;

/**
 * Class that represents all physical properties of a vehicle and performs physics computations
 */
public class PhysicalVehicle implements SimulationLoopExecutable, PhysicalObject {

    /** x_cm bar of formula */
    private RealVector localPos;

    /** x_cm of formula */
    private RealVector pos;

    /** v_cm / x_cm dot of formula */
    private RealVector velocity;

    /** x_cm dot dot of formula */
    private RealVector acceleration;

    /** F of formula */
    private RealVector force;

    /** I bar ^-1 of formula */
    private RealMatrix localInertiaInverse;

    /** I ^-1 of formula */
    private RealMatrix inertiaInverse;


    /** A of formula */
    private RealMatrix rotationMatrix;

    /** omega of formula */
    private RealVector angularVelocity;

    /** L of formula */
    private RealVector angularMomentum;

    /** tau of formula */
    private RealVector angularMomentumDeriv;

    /** Type of the vehicle as physical object */
    PhysicalObjectType physicalObjectType;

    /** Indicator whether the vehicle has collided with another object */
    private boolean collision;

    /** Error of the object */
    private boolean error;

    /** Indicator whether the vehicle is fully initialized or not */
    private boolean physicalVehicleInitialized = false;

    /** Unique ID */
    private final long uniqueId = IdGenerator.getSharedInstance().generateUniqueId();

    /** The vehicle */
    private final Vehicle simulationVehicle;

    /**
     * Constructor for a physical vehicle that is standing at its position
     * Use other functions to initiate movement and position updates
     *
     * @param controllerBus Optional bus for the controller of the vehicle
     * @param controller Optional controller of the vehicle
     * @param navigation Optional navigation of the vehicle
     */
    protected PhysicalVehicle(Optional<Bus> controllerBus, Optional<FunctionBlockInterface> controller, Optional<FunctionBlockInterface> navigation) {
        // Default values
        physicalVehicleInitialized = false;

        // PhysicalVehicle is standing, no velocity, acceleration, force
        velocity = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        acceleration = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        force = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        // Init values for matrices
        localInertiaInverse = MatrixUtils.createRealIdentityMatrix(3);
        inertiaInverse = MatrixUtils.createRealIdentityMatrix(3);

        // Init values for more vectors and matrices
        angularVelocity = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        angularMomentum = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        angularMomentumDeriv = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        // Center of mass in local coordinate system is initialized with 0
        localPos = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        // By default vehicle is not in collision or computational error and has default car object type
        physicalObjectType = PhysicalObjectType.PHYSICAL_OBJECT_TYPE_CAR_DEFAULT;
        collision = false;
        error = false;

        // Set default simulationVehicle object with optional bus and controller, might be changed via PhysicalVehicleBuilder
        this.simulationVehicle = new Vehicle(controllerBus, controller, navigation);

        // Center of car geometry position
        double groundZ = WorldModel.getInstance().getGround(0.0, 0.0, 0.0).doubleValue();
        setGlobalPos(0.0, 0.0, (groundZ + 0.5 * simulationVehicle.getHeight() + simulationVehicle.getWheelRadius()));
        
        // Put rotation values in matrix
        setGlobalRotation(0.0, 0.0, 0.0);

        // Initialize physicalVehicle based on current values
        initPhysicalVehicle(controllerBus, controller, navigation);
        physicalVehicleInitialized = true;

        Log.finest("PhysicalVehicle: Constructor - PhysicalVehicle constructed: " + this);
    }

    /**
     * Function that initializes the physicalVehicle when it is created
     * Should only be called by constructor or builder
     *
     * @param controllerBus Optional bus for the controller of the vehicle
     * @param controller Optional controller of the vehicle
     * @param navigation Optional navigation of the vehicle
     */
    void initPhysicalVehicle(Optional<Bus> controllerBus, Optional<FunctionBlockInterface> controller, Optional<FunctionBlockInterface> navigation) {
        Log.finest("PhysicalVehicle: initPhysicalVehicle - PhysicalVehicle at start: " + this);

        // Set controllerBus and controller to the vehicle
        simulationVehicle.setControllerBus(controllerBus);
        simulationVehicle.setController(controller);
        simulationVehicle.setNavigation(navigation);

        // Initialize values for physicalVehicle after constructor or builder
        initLocalPos();
        initMassPointLocalCenterDiff();
        initLocalInertiaInverse();
        calcInertiaInverse();
        calcAngularVelocity();

        // This initializes wheel mass point positions correctly before first update
        calcMassPointCenterDiff();
        calcMassPointPosition();

        Log.finest("PhysicalVehicle: initPhysicalVehicle - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that computes the center of mass position in the local coordinate system
     * Based on mass and local positions of mass points
     * Should only be called by initPhysicalVehicle
     */
    private void initLocalPos() {
        Log.finest("PhysicalVehicle: initLocalPos - PhysicalVehicle at start: " + this);
        RealVector result = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        for (MassPoint mp : simulationVehicle.getWheelMassPoints()) {
            result = result.add(mp.getLocalPos().mapMultiplyToSelf(mp.getMass()));
        }

        localPos = result.mapDivideToSelf(simulationVehicle.getMass());
        Log.finest("PhysicalVehicle: initLocalPos - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that computes the localCenterDiff values for all mass points
     * Based on current physicalVehicles local center of mass position
     * Should only be called by initPhysicalVehicle
     */
    private void initMassPointLocalCenterDiff() {
        Log.finest("PhysicalVehicle: initMassPointLocalCenterDiff - PhysicalVehicle at start: " + this);

        for (MassPoint mp : simulationVehicle.getWheelMassPoints()) {
            mp.setLocalCenterDiff(mp.getLocalPos().subtract(localPos));
        }

        Log.finest("PhysicalVehicle: initMassPointLocalCenterDiff - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that computes the localInertiaInverse values for the physicalVehicle
     * Based on current vehicles mass point information
     * Should only be called by initPhysicalVehicle
     */
    private void initLocalInertiaInverse(){
        Log.finest("PhysicalVehicle: initLocalInertiaInverse - PhysicalVehicle at start: " + this);
        try {
            // Matrix of dimension 3x3 with zero values
            RealMatrix result = MatrixUtils.createRealMatrix(3, 3);

            // Add up values from mass points
            for (MassPoint mp : simulationVehicle.getWheelMassPoints()) {
                RealMatrix matrixMassPoint = MathHelper.vector3DToCrossProductMatrix(mp.getLocalCenterDiff()).power(2).scalarMultiply(-mp.getMass());
                result = result.add(matrixMassPoint);
            }

            // Compute inverse
            localInertiaInverse = MathHelper.matrixInvert(result);

        } catch (Exception e) {
            Log.severe("PhysicalVehicle: initLocalInertiaInverse - Could not calculate local inertia inverse. Cross product matrix or matrix inversion failed.");
            e.printStackTrace();
        }
        Log.finest("PhysicalVehicle: initLocalInertiaInverse - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the acceleration force for a given mass point
     *
     * @param mp MassPoint for which force should be computed
     * @param deltaT Time difference to previous step in seconds
     * @return RealVector that represents the force
     */
    protected RealVector calcAccelerationForce(MassPoint mp, double deltaT){
        // Check wheels with ground contact
        double groundContact = PhysicsEngine.calcGroundContact(mp, this, deltaT);

        RealVector forceAcceleration = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        double countWheelGroundContact = 0.0;
        for (MassPoint mpTmp : getSimulationVehicle().getWheelMassPoints()) {
            if (PhysicsEngine.calcGroundContact(mpTmp, this, deltaT) >= 0.5) {
                countWheelGroundContact = countWheelGroundContact + 1.0;
            }
        }

        if (mp.getType().ordinal() > MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal() || groundContact == 0.0 || countWheelGroundContact == 0.0) {
            return forceAcceleration;
        }

        // Compute motor acceleration values shared among wheels with ground contact
        double accelerationPerWheel = (4.0 * getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_MOTOR).getActuatorValueCurrent() / countWheelGroundContact);

        RealVector vehicleOrientation = getGeometryRot().operate(new ArrayRealVector(new double[] {0.0, 1.0, 0.0}));
        double steeringAngle = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_STEERING).getActuatorValueCurrent();

        // Scale force down when near zero velocity to avoid permanent positive / negative changes
        double velocityNorm = mp.getVelocity().getNorm();
        double brakeValueActuatorFrontLeft = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT).getActuatorValueCurrent();
        double brakeValueActuatorFrontRight = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT).getActuatorValueCurrent();
        double brakeValueActuatorBackLeft = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT).getActuatorValueCurrent();
        double brakeValueActuatorBackRight = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT).getActuatorValueCurrent();
        double brakeValueActuator = (brakeValueActuatorFrontLeft + brakeValueActuatorFrontRight + brakeValueActuatorBackLeft + brakeValueActuatorBackRight) /4;
        if (velocityNorm >= 0.0 && velocityNorm < 0.35 && brakeValueActuator >= Math.abs(accelerationPerWheel)) {
            accelerationPerWheel = 0.0;
        }

        // Force: Motor acceleration, F = mass * acceleration
        forceAcceleration = vehicleOrientation.mapMultiply(mp.getMass() * accelerationPerWheel);

        // Front wheels: Consider steering
        if (mp.getType().ordinal() == MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal() || mp.getType().ordinal() == MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()) {
            Rotation steerRotZ = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, 0.0, 0.0, -steeringAngle);
            RealMatrix steerRotZMatrix = new BlockRealMatrix(steerRotZ.getMatrix());
            forceAcceleration = steerRotZMatrix.operate(forceAcceleration);
        }

        return forceAcceleration.mapMultiply(groundContact);
    }

    /**
     * Function that calculates the brake force for a given mass point
     *
     * @param mp MassPoint for which force should be computed
     * @param deltaT Time difference to previous step in seconds
     * @return RealVector that represents the force
     */
    protected RealVector calcBrakeForce(MassPoint mp, double deltaT){
        // Check wheels with ground contact
        double groundContact = PhysicsEngine.calcGroundContact(mp, this, deltaT);

        RealVector forceBrake = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        if (mp.getType().ordinal() > MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal() || groundContact == 0.0) {
            return forceBrake;
        }

        // Individual brake force for each wheel
        double brakeValueActuator = 0.0;
        switch (mp.getType()) {
            case MASS_POINT_TYPE_WHEEL_FRONT_LEFT:
                brakeValueActuator = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT).getActuatorValueCurrent();
                break;
            case MASS_POINT_TYPE_WHEEL_FRONT_RIGHT:
                brakeValueActuator = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT).getActuatorValueCurrent();
                break;
            case MASS_POINT_TYPE_WHEEL_BACK_LEFT:
                brakeValueActuator = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT).getActuatorValueCurrent();
                break;
            case MASS_POINT_TYPE_WHEEL_BACK_RIGHT:
                brakeValueActuator = getSimulationVehicle().getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT).getActuatorValueCurrent();
                break;
        }

        // Brakes work against mass point velocity with amount of acceleration
        forceBrake = mp.getVelocity().mapMultiply(-1.0);
        double velocityNorm = mp.getVelocity().getNorm();

        if (velocityNorm > 0.0) {
            forceBrake = forceBrake.mapDivide(velocityNorm);
        }

        // Scale force down when near zero velocity to avoid permanent positive / negative changes
        double brakeAmount = brakeValueActuator;
        if (velocityNorm >= 0.0 && velocityNorm < 0.35) {
            brakeAmount = velocityNorm * brakeValueActuator;
        }

        // Force: Brake force F = mass * acceleration
        // Consider amount of acceleration, do not cause negative acceleration due to brakes
        forceBrake = forceBrake.mapMultiply(mp.getMass() * brakeAmount);

        return forceBrake.mapMultiply(groundContact);
    }

    /**
     * Function that calculates the inertiaInverse matrix for physicalVehicle
     * Based on current physicalVehicles localInertiaInverse and rotationMatrix
     */
    private void calcInertiaInverse(){
        Log.finest("PhysicalVehicle: calcInertiaInverse - PhysicalVehicle at start: " + this);
        inertiaInverse = rotationMatrix.multiply(localInertiaInverse).multiply(rotationMatrix.transpose());
        Log.finest("PhysicalVehicle: calcInertiaInverse - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the angularVelocity vector for the physicalVehicle
     * Based on current physicalVehicles inertiaInverse and angularMomentum
     */
    private void calcAngularVelocity(){
        Log.finest("PhysicalVehicle: calcAngularVelocity - PhysicalVehicle at start: " + this);
        angularVelocity = inertiaInverse.operate(angularMomentum);
        Log.finest("PhysicalVehicle: calcAngularVelocity - PhysicalVehicle at end: " + this);
    }

    /**
     * Recalculates the r_i after one integration step
     */
    private void calcMassPointCenterDiff(){
        Log.finest("PhysicalVehicle: calcMassPointCenterDiff - PhysicalVehicle at start: " + this);
        for(MassPoint massPoint : this.simulationVehicle.getWheelMassPoints()){
            massPoint.setCenterDiff(this.rotationMatrix.operate(massPoint.getLocalCenterDiff()));
        }
        Log.finest("PhysicalVehicle: calcMassPointCenterDiff - PhysicalVehicle at end: " + this);
    }

    /**
     * Calculates the positions of the mass points
     */
    private void calcMassPointPosition(){
        Log.finest("PhysicalVehicle: calcMassPointPosition - PhysicalVehicle at start: " + this);
        for(MassPoint massPoint : this.simulationVehicle.getWheelMassPoints()){
            massPoint.setPos(this.pos.add(massPoint.getCenterDiff()));

            RealVector massPointPos = massPoint.getPos();
            double groundZ = WorldModel.getInstance().getGround(massPointPos.getEntry(0), massPointPos.getEntry(1), massPointPos.getEntry(2)).doubleValue();
            massPoint.setGroundZ(groundZ);
            double limitZ = groundZ + simulationVehicle.getWheelRadius();

            // If mass point position goes way below ground position + wheel radius, then set computational error
            if (massPointPos.getEntry(2) < (limitZ - 0.5 * simulationVehicle.getWheelRadius())) {
                setError(true);
            }
        }
        Log.finest("PhysicalVehicle: calcMassPointPosition - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the velocity and acceleration vectors for all mass points
     * @param deltaT Time difference to previous step in seconds
     */
    private void calcMassPointVelocityAndAcceleration(double deltaT){
        Log.finest("PhysicalVehicle: calcMassPointVelocityAndAcceleration - PhysicalVehicle at start: " + this);
        for(MassPoint massPoint : this.simulationVehicle.getWheelMassPoints()){

            RealVector previousVelocity = massPoint.getVelocity().copy();

            try {
                massPoint.setVelocity(this.velocity.add(MathHelper.vector3DCrossProduct(this.angularVelocity, massPoint.getCenterDiff())));
            } catch (Exception e) {
                e.printStackTrace();
            }

            RealVector zeroVector = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            double threshold = 0.0000000000001;
            if (MathHelper.vectorEquals(massPoint.getVelocity(), zeroVector, threshold)) {
                massPoint.setVelocity(zeroVector);
            }

            if (deltaT > 0.0) {
                massPoint.setAcceleration(massPoint.getVelocity().subtract(previousVelocity).mapDivide(deltaT));
            }
        }
        Log.finest("PhysicalVehicle: calcMassPointVelocityAndAcceleration - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the position vector for the physicalVehicle
     * Based on current physicalVehicles velocity and time input
     *
     * @param deltaT Time difference to previous step in seconds
     */
    private void calcPosition(double deltaT){
        Log.finest("PhysicalVehicle: calcPosition - Input time: " + deltaT + ", PhysicalVehicle at start: " + this);
        pos = pos.add(velocity.mapMultiply(deltaT));
        Log.finest("PhysicalVehicle: calcPosition - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the velocity and acceleration vector for the physicalVehicle
     * Based on current physicalVehicles force and time input
     *
     * @param deltaT Time difference to previous step in seconds
     */
    private void calcVelocityAndAcceleration(double deltaT){
        Log.finest("PhysicalVehicle: calcVelocityAndAcceleration - Input time: " + deltaT + ", PhysicalVehicle at start: " + this);

        RealVector previousVelocity = velocity.copy();
        velocity = velocity.add((force.mapDivide(simulationVehicle.getMass())).mapMultiply(deltaT));

        RealVector zeroVector = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
        double threshold = 0.0000000000001;
        if (MathHelper.vectorEquals(velocity, zeroVector, threshold)) {
            velocity = zeroVector;
        }

        if (deltaT > 0.0) {
            acceleration = velocity.subtract(previousVelocity).mapDivide(deltaT);
        }

        Log.finest("PhysicalVehicle: calcVelocityAndAcceleration - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the rotationMatrix for the physicalVehicle
     * Based on current physicalVehicles angularVelocity and time input
     *
     * @param deltaT Time difference to previous step in seconds
     */
    private void calcRotationMatrix(double deltaT){
        Log.finest("PhysicalVehicle: calcRotationMatrix - Input time: " + deltaT + ", PhysicalVehicle at start: " + this);

        try {
            rotationMatrix = rotationMatrix.add((MathHelper.vector3DToCrossProductMatrix(angularVelocity).multiply(rotationMatrix)).scalarMultiply(deltaT));
        } catch (Exception e) {
            Log.severe("PhysicalVehicle: calcRotationMatrix - Exception:" + e.toString());
            e.printStackTrace();
        }

        // Always orthonormalize matrix after computations to avoid numerical issues
        rotationMatrix = MathHelper.matrix3DOrthonormalize(rotationMatrix);

        Log.finest("PhysicalVehicle: calcRotationMatrix - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the angularMomentum for the physicalVehicle
     * Based on current physicalVehicles angularMomentumDeriv and time input
     *
     * @param deltaT Time difference to previous step in seconds
     */
    private void calcAngularMomentum(double deltaT){
        Log.finest("PhysicalVehicle: calcAngularMomentum - Input time: " + deltaT + ", PhysicalVehicle at start: " + this);
        angularMomentum = angularMomentum.add(angularMomentumDeriv.mapMultiply(deltaT));
        Log.finest("PhysicalVehicle: calcAngularMomentum - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the angularMomentumDeriv for the physicalVehicle
     * Based on current forces and center differences of vehicles mass points
     */
    private void calcAngularMomentumDeriv(){
        Log.finest("PhysicalVehicle: calcAngularMomentumDeriv - PhysicalVehicle at start: " + this);
        RealVector result = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        for (MassPoint mp : simulationVehicle.getWheelMassPoints()) {
            try {
                result = result.add(MathHelper.vector3DCrossProduct(mp.getCenterDiff(), mp.getForce()));
            }
            catch (Exception e) {
                Log.severe("PhysicalVehicle: calcAngularMomentumDeriv - Exception:" + e.toString());
                e.printStackTrace();
            }
        }

        angularMomentumDeriv = result;
        Log.finest("PhysicalVehicle: calcAngularMomentumDeriv - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that calculates the force for the vehicle
     * Based on current forces of vehicles mass points
     */
    private void calcForce(){
        Log.finest("PhysicalVehicle: calcForce - PhysicalVehicle at start: " + this);
        RealVector result = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});

        for (MassPoint mp : simulationVehicle.getWheelMassPoints()) {
            result = result.add(mp.getForce());
        }

        force = result;
        Double forceNorm = force.getNorm();

        // Set computational error if forces are way too high to keep vehicle in stable state
        if (forceNorm.isInfinite() || forceNorm.isNaN() || forceNorm > 1.0E10) {
            setError(true);
        }

        Log.finest("PhysicalVehicle: calcForce - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that sets the global rotation to the physicalVehicle
     *
     * @param rotX Global x rotation of physicalVehicle
     * @param rotY Global y rotation of physicalVehicle
     * @param rotZ Global z rotation of physicalVehicle
     */
    public void setGlobalRotation(double rotX, double rotY, double rotZ) {
        Log.finest("PhysicalVehicle: setGlobalRotation - PhysicalVehicle at start: " + this);
        Rotation rot = new Rotation(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR, rotX, rotY, rotZ);
        rotationMatrix = new BlockRealMatrix(rot.getMatrix());
        Log.finest("PhysicalVehicle: setGlobalRotation - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that sets the global position to the physicalVehicle
     * Note: This refers to the cars geometry center position, not the center of mass position
     * @param x Global x position of physicalVehicle
     * @param y Global y position of physicalVehicle
     * @param z Global z position of physicalVehicle
     */
    public void setGlobalPos(double x, double y, double z) {
        Log.finest("PhysicalVehicle: setGlobalPos - PhysicalVehicle at start: " + this);

        // The input relates to the center geometry position of the car, thus lower the height of pos to wheel level
        pos = new ArrayRealVector(new double[] {x, y, z - (simulationVehicle.getHeight() / 2.0)});

        Log.finest("PhysicalVehicle: setGlobalPos - PhysicalVehicle at end: " + this);
    }

    /**
     * Function that sets the physical object type to physicalVehicle
     *
     * @param physicalObjectType New object type of the physical vehicle
     */
    protected void setPhysicalObjectType(PhysicalObjectType physicalObjectType) {
        Log.finest("PhysicalVehicle: setPhysicalObjectType - physicalObjectType: " + physicalObjectType + ", PhysicalVehicle at start: " + this);
        this.physicalObjectType = physicalObjectType;
        Log.finest("PhysicalVehicle: setPhysicalObjectType - physicalObjectType: " + physicalObjectType + ", PhysicalVehicle at end: " + this);
    }

    /**
     * Function that returns the current simulation vehicle object
     *
     * @return Current simulation vehicle object
     */
    public Vehicle getSimulationVehicle() {
        return simulationVehicle;
    }

    /**
     * Function that returns a copy of localPos
     *
     * @return Copy of localPos
     */
    public RealVector getLocalPos() {
        return localPos.copy();
    }

    /**
     * Function that returns a copy of pos
     *
     * @return Copy of pos
     */
    public RealVector getPos() {
        return pos.copy();
    }

    /**
     * Function that returns a copy of velocity
     *
     * @return Copy of velocity
     */
    public RealVector getVelocity() {
        return velocity.copy();
    }

    /**
     * Function that returns a copy of acceleration
     *
     * @return Copy of acceleration
     */
    public RealVector getAcceleration() {
        return acceleration.copy();
    }

    /**
     * Function that returns a copy of force
     *
     * @return Copy of force
     */
    public RealVector getForce() {
        return force.copy();
    }

    /**
     * Function that returns the steering angle of the vehicle
     * @return Steering angle
     */
    public double getSteeringAngle() {
        return simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_STEERING).getActuatorValueCurrent();
    }

    /**
     * Function that returns a copy of localInertiaInverse
     *
     * @return Copy of localInertiaInverse
     */
    public RealMatrix getLocalInertiaInverse() {
        return localInertiaInverse.copy();
    }

    /**
     * Function that returns a copy of inertiaInverse
     *
     * @return Copy of inertiaInverse
     */
    public RealMatrix getInertiaInverse() {
        return inertiaInverse.copy();
    }

    /**
     * Function that returns a copy of angularVelocity
     *
     * @return Copy of angularVelocity
     */
    public RealVector getAngularVelocity() {
        return angularVelocity.copy();
    }

    /**
     * Function that returns a copy of angularMomentum
     *
     * @return Copy of angularMomentum
     */
    public RealVector getAngularMomentum() {
        return angularMomentum.copy();
    }

    /**
     * Function that returns a copy of angularMomentumDeriv
     *
     * @return Copy of angularMomentumDeriv
     */
    public RealVector getAngularMomentumDeriv() {
        return angularMomentumDeriv.copy();
    }

    /**
     * Function that returns a list of wheel mass point positions
     *
     * @return List of wheel mass point positions
     */
    public List<RealVector> getWheelMassPointPositions() {

        List<RealVector> WheelMassPointPosition = new LinkedList<>();

        // For every wheel retrieve the mass point positions
        for(MassPoint massPoint : this.simulationVehicle.getWheelMassPoints()) {
            WheelMassPointPosition.add(massPoint.getPos());

        }

        return WheelMassPointPosition;
    }

    /**
     * Function that provides public access to compute mass point position updates
     */
    public void updateMassPointPositions() {
        Log.finest("PhysicalVehicle: updateMassPointPositions, PhysicalVehicle at start: " + this);
        calcMassPointCenterDiff();
        calcMassPointPosition();
        Log.finest("PhysicalVehicle: updateMassPointPositions, PhysicalVehicle at end: " + this);
    }

    /**
     * Requests the called object to update its state for given time difference
     *
     * @param timeDiffMs Difference in time measured in milliseconds
     */
    @Override
    public void executeLoopIteration(long timeDiffMs) {
        simulationVehicle.updateAllSensors();

        if (!this.collision && !this.error) {
            Log.finest("PhysicalVehicle: executeLoopIteration - timeDiffMs: " + timeDiffMs + ", PhysicalVehicle at start: " + this);

            final double deltaT = (timeDiffMs / 1000.0);

            // Exchange data with controller
            simulationVehicle.exchangeDataWithController(deltaT);

            // Update vehicle actuators
            simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_MOTOR).update(deltaT);
            simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT).update(deltaT);
            simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT).update(deltaT);
            simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT).update(deltaT);
            simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT).update(deltaT);
            simulationVehicle.getVehicleActuator(VEHICLE_ACTUATOR_TYPE_STEERING).update(deltaT);

            // Perform loop computations
            calcAngularMomentumDeriv();
            calcForce();

            calcPosition(deltaT);
            calcVelocityAndAcceleration(deltaT);
            calcRotationMatrix(deltaT);
            calcAngularMomentum(deltaT);
            calcInertiaInverse();
            calcAngularVelocity();

            calcMassPointCenterDiff();
            calcMassPointPosition();
            calcMassPointVelocityAndAcceleration(deltaT);

            Log.finest("PhysicalVehicle: executeLoopIteration - timeDiffMs: " + timeDiffMs +  ", PhysicalVehicle at end: " + this);
        } else {
            Log.finest("PhysicalVehicle: Vehicle collided or had a computational error and will therefore not move anymore, PhysicalVehicle: " + this);
        }
    }

    /**
     * Function that returns the type of the object
     *
     * @return Type of the object
     */
    @Override
    public PhysicalObjectType getPhysicalObjectType() {
        return physicalObjectType;
    }

    /**
     * Function that returns a vector with the x, y and z coordinates of the object
     * This refers to the center position of the geometry object (i.e. NOT mass point position)
     *
     * @return Vector with x, y, z coordinates of the object center
     */
    @Override
    public RealVector getGeometryPos() {
        RealVector relVectorBottomTop = new ArrayRealVector(new double[] {0.0, 0.0, getHeight()});
        relVectorBottomTop = getGeometryRot().operate(relVectorBottomTop);
        relVectorBottomTop = relVectorBottomTop.mapMultiply(0.5);

        RealVector relVectorBackFront = (simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getPos().subtract(simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getPos()));
        RealVector relVectorLeftRight = (simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getPos().subtract(simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getPos()));
        relVectorBackFront = relVectorBackFront.mapMultiply(0.5);
        relVectorLeftRight = relVectorLeftRight.mapMultiply(0.5);

        RealVector absGeometryCenterPos = simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getPos();
        absGeometryCenterPos = absGeometryCenterPos.add(relVectorBackFront);
        absGeometryCenterPos = absGeometryCenterPos.add(relVectorLeftRight);
        absGeometryCenterPos = absGeometryCenterPos.add(relVectorBottomTop);
        return absGeometryCenterPos;
    }

    /**
     * Function that returns a vector with the x, y and z coordinates of the front left wheel
     * This refers to the center position of the geometry wheel (i.e. NOT mass point position)
     *
     * @return Vector with x, y, z coordinates of the object center
     */
    public RealVector getFrontLeftWheelGeometryPos() {
        return (simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getPos());
    }

    /**
     * Function that returns a vector with the x, y and z coordinates of the front right wheel
     * This refers to the center position of the geometry wheel (i.e. NOT mass point position)
     *
     * @return Vector with x, y, z coordinates of the object center
     */
    public RealVector getFrontRightWheelGeometryPos() {
        return (simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getPos());
    }

    /**
     * Function that returns a vector with the x, y and z coordinates of the back left wheel
     * This refers to the center position of the geometry wheel (i.e. NOT mass point position)
     *
     * @return Vector with x, y, z coordinates of the object center
     */
    public RealVector getBackLeftWheelGeometryPos() {
        return (simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getPos());
    }

    /**
     * Function that returns a vector with the x, y and z coordinates of the back right wheel
     * This refers to the center position of the geometry wheel (i.e. NOT mass point position)
     *
     * @return Vector with x, y, z coordinates of the object center
     */
    public RealVector getBackRightWheelGeometryPos() {
        return (simulationVehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getPos());
    }

    /**
     * Function that returns a matrix with the rotation of the object
     *
     * @return Matrix with the rotation of the object
     */
    @Override
    public RealMatrix getGeometryRot() {
        return rotationMatrix.copy();
    }

    /**
     * Function that returns the width of an object. Only meaningful for a box object, otherwise returns 0.0.
     *
     * @return Width of a box object, otherwise 0.0
     */
    @Override
    public double getWidth() {
        return simulationVehicle.getWidth();
    }

    /**
     * Function that returns the length of an object. Only meaningful for a box object, otherwise returns 0.0.
     *
     * @return Length of a box object, otherwise 0.0
     */
    @Override
    public double getLength() {
        return simulationVehicle.getLength();
    }

    /**
     * Function that returns the height of an object. Only meaningful for a box object, otherwise returns 0.0.
     *
     * @return Height of a box object, otherwise 0.0
     */
    @Override
    public double getHeight() {
        return simulationVehicle.getHeight();
    }

    /**
     * Function that returns the z offset of an object. This is used to represent the wheel radius for vehicles, otherwise 0.0
     *
     * @return Z offset of an object, i.e. wheel radius for vehicles, otherwise 0.0
     */
    @Override
    public double getOffsetZ() {
        return simulationVehicle.getWheelRadius();
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
     * @param collision Boolean that indicates a collision of that object
     */
    @Override
    public void setCollision(boolean collision) {
        Log.warning("PhysicalVehicle: setCollision - collision: " + collision + ", PhysicalVehicle at start: " + this);
        this.collision = collision;

        if (collision) {
            this.velocity = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.acceleration = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.angularVelocity = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.angularMomentum = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.angularMomentumDeriv = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            MassPoint [] points = this.simulationVehicle.getWheelMassPoints();
            for(MassPoint point : points){
                point.setAcceleration(new ArrayRealVector(new double[] {0.0, 0.0, 0.0}));
                point.setVelocity(new ArrayRealVector(new double[] {0.0, 0.0, 0.0}));
                point.setForce(new ArrayRealVector(new double[] {0.0, 0.0, 0.0}));
            }
            this.simulationVehicle.setWheelMassPoints(points);
        }

        Log.warning("PhysicalVehicle: setCollision - collision: " + collision + ", PhysicalVehicle at end: " + this);
    }

    /**
     * Function that returns a boolean indicating if an object had a computational error
     *
     * @return Boolean that indicates a computational error of that object
     */
    @Override
    public boolean getError() {
        return error;
    }

    /**
     * Function that sets a computational error for this object
     *
     * @param error Boolean that indicates a computational error of that object
     */
    @Override
    public void setError(boolean error) {
        Log.warning("PhysicalVehicle: setError - error: " + error + ", PhysicalVehicle at start: " + this);
        this.error = error;

        if (error) {
            this.velocity = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.acceleration = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.angularVelocity = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.angularMomentum = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            this.angularMomentumDeriv = new ArrayRealVector(new double[] {0.0, 0.0, 0.0});
            MassPoint [] points = this.simulationVehicle.getWheelMassPoints();
            for(MassPoint point : points){
                point.setAcceleration(new ArrayRealVector(new double[] {0.0, 0.0, 0.0}));
                point.setVelocity(new ArrayRealVector(new double[] {0.0, 0.0, 0.0}));
                point.setForce(new ArrayRealVector(new double[] {0.0, 0.0, 0.0}));
            }
            this.simulationVehicle.setWheelMassPoints(points);
        }

        Log.warning("PhysicalVehicle: setError - error: " + error + ", PhysicalVehicle at end: " + this);
    }

    /**
     * Returns the unique ID of the object. Valid IDs are positive numbers.
     * @return Unique ID
     */
    public long getId() {
        return this.uniqueId;
    }

    /**
     * Function that returns a list of pairs of 3D points, indicating the beginning and end of a vector in absolute 3D global coordinates
     * These vectors are checked for overlaps / going under the map in collision detection
     *
     * @return List of pairs of 3D points, indicating the beginning and end of a vector in absolute 3D global coordinates
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

    /**
     *
     * @return The rotation matrix of the vehicle
     */
    public RealMatrix getRotationMatrix() {
        return rotationMatrix;
    }


    /**
     *
     * @param rotationMatrix new rotation matrix for the vehicle
     */
    protected void setRotationMatrix(RealMatrix rotationMatrix) {
        this.rotationMatrix = rotationMatrix;
    }

    /**
     * Overwrite toString() to get a nice output for physicalVehicles
     * @return String that contains all information of physicalVehicles
     */
    @Override
    public String toString() {
        return  "PhysicalVehicle " + getId() +
                (physicalVehicleInitialized ? " , geometryPos: " + getGeometryPos() : "") +
                " , localPos: " + localPos +
                " , pos: " + pos +
                " , velocity: " + velocity+
                " , acceleration: " + acceleration +
                " , force: " + force +
                " , localInertiaInverse: " + localInertiaInverse +
                " , inertiaInverse: " + inertiaInverse +
                " , rotationMatrix: " + rotationMatrix +
                " , angularVelocity: " + angularVelocity +
                " , angularMomentum: " + angularMomentum +
                " , angularMomentumDeriv: " + angularMomentumDeriv +
                " , physicalObjectType: " + physicalObjectType +
                " , collision: " + collision +
                " , error: " + error +
                " , physicalVehicleInitialized: " + physicalVehicleInitialized +
                " , simulationVehicle: " + simulationVehicle;
    }
}
