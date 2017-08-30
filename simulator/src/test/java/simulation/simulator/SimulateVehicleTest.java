package simulation.simulator;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.*;
import simulation.util.Log;
import simulation.util.MathHelper;
import simulation.vehicle.*;

import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static simulation.vehicle.MassPointType.*;
import static simulation.vehicle.MassPointType.MASS_POINT_TYPE_WHEEL_BACK_RIGHT;
import static simulation.vehicle.VehicleStatusSensor.VEHICLE_STATUS_SENSOR_ENGINE_STATUS;

/**
 * JUnit Test-suite for simulating a vehicle
 */
public class SimulateVehicleTest {

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
        Simulator.resetSimulator();

        //Set update frequency to 30 loop iterations per second
        Simulator sim = Simulator.getSharedInstance();
        sim.setSimulationType(SimulationType.SIMULATION_TYPE_FIXED_TIME);
        sim.setSimulationLoopFrequency(30);
        sim.setSynchronousSimulation(true);
        sim.setPausedInFuture(true);
    }

    @Test
    public void testMotor() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Set actuator values for testing
        VehicleActuator motor = vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR);

        try {
            motor.setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_MOTOR_ACCELERATION_MAX);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Start simulation
        // After 5 seconds, value should be reached
        sim.stopAfter(5000);
        sim.startSimulation();

        assertTrue(motor.getActuatorValueCurrent() == Vehicle.VEHICLE_DEFAULT_MOTOR_ACCELERATION_MAX);

        try {
            motor.setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_MOTOR_ACCELERATION_MIN);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // After 5 seconds, value should be reached
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        assertTrue(motor.getActuatorValueCurrent() == Vehicle.VEHICLE_DEFAULT_MOTOR_ACCELERATION_MIN);

        try {
            motor.setActuatorValueTarget(0);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // After 5 seconds, value should be reached
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        assertTrue(motor.getActuatorValueCurrent() == 0);
    }

    @Test
    public void testBrakes() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Set actuator values for testing
        VehicleActuator brakes = vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT);

        try {
            brakes.setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (17 seconds)
        sim.stopAfter(17000);

        // Start simulation
        // After 5 seconds, value should be reached
        sim.stopAfter(5000);
        sim.startSimulation();
        assertTrue(brakes.getActuatorValueCurrent() == Vehicle.VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX);

        try {
            brakes.setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // After 5 seconds, value should be reached
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        assertTrue(brakes.getActuatorValueCurrent() == Vehicle.VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN);

        try {
            brakes.setActuatorValueTarget(0);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // After 5 seconds, value should be reached
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        assertTrue(brakes.getActuatorValueCurrent() == 0);
    }

    @Test
    public void testSteering() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Set actuator values for testing
        VehicleActuator steering = vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING);

        try {
            steering.setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_STEERING_ANGLE_MAX);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (17 seconds)
        sim.stopAfter(17000);

        // Start simulation
        // After 5 seconds, value should be reached
        sim.stopAfter(5000);
        sim.startSimulation();
        assertTrue(steering.getActuatorValueCurrent() == Vehicle.VEHICLE_DEFAULT_STEERING_ANGLE_MAX);

        try {
            steering.setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_STEERING_ANGLE_MIN);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // After 5 seconds, value should be reached
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        assertTrue(steering.getActuatorValueCurrent() == Vehicle.VEHICLE_DEFAULT_STEERING_ANGLE_MIN);

        try {
            steering.setActuatorValueTarget(0);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // After 5 seconds, value should be reached
        sim.extendSimulationTime(5000);
        sim.startSimulation();
        assertTrue(steering.getActuatorValueCurrent() == 0);
    }

    /**
     * Check if the vehicle drives straight forward, if there is no steering angle
     */
    @Test
    public void testDriveStraightForward() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_MOTOR_ACCELERATION_MAX);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        RealVector startPosition = physicalVehicle.getPos();

        //Run simulation
        sim.stopAfter(5000);
        sim.startSimulation();

        RealVector endPosition = physicalVehicle.getPos();

        //Ignore y direction
        startPosition.setEntry(1,0);
        endPosition.setEntry(1,0);

        assertTrue(MathHelper.vectorEquals(startPosition, endPosition, 0.001));
    }

    /**
     * Checks, whether the vehicle stands still, if there is no acceleration
     */
    @Test
    public void testNoDriveIfNoAcceleration() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        //Remember start position
        RealVector startPosition = physicalVehicle.getPos();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        //Run simulation
        sim.stopAfter(5000);
        sim.startSimulation();

        //Compare to end position
        RealVector endPosition = physicalVehicle.getPos();
        assertTrue(MathHelper.vectorEquals(startPosition, endPosition, 0.001));
    }


    /**
     * Checks if the force applied on the vehicle diifers between driving forward andr steering to the right
     */
    @Test
    public void testCentripetalForceHasInfluence() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();
        VehicleActuator motor = vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR);
        VehicleActuator steering = vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING);

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (5 seconds)
        sim.stopAfter(5000);


        //Start simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        try {
            motor.setActuatorValueTarget(1);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        RealVector forceStart = physicalVehicle.getForce();

        try {
            steering.setActuatorValueTarget(0.5);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        sim.extendSimulationTime(5000);
        sim.startSimulation();
        RealVector forceEnd = physicalVehicle.getForce();

        assertTrue(forceStart.getEntry(0) != forceEnd.getEntry(0) || forceStart.getEntry(1) != forceEnd.getEntry(1) ||
                forceStart.getEntry(2) != forceEnd.getEntry(2));
    }

    @Test
    public void testHasNoVelocityAtStart() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Start simulation
        sim.stopAfter(1000);
        sim.startSimulation();
        RealVector velocityStart = physicalVehicle.getVelocity();

        assertTrue(velocityStart.getEntry(0) == 0);
        assertTrue(velocityStart.getEntry(1) == 0);
        assertTrue(velocityStart.getEntry(2) == 0);
    }


    /**
     * Test if airdrag slows the vehicle down, by accelerating it for one second and then step of the gas
     * and check after 20 seconds if it is slower
     */
    @Test
    public void testBecomesSlowerThroughAirdrag() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (30 seconds)
        sim.stopAfter(30000);

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(1);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }


        //Start simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        RealVector velocityStart = physicalVehicle.getVelocity();

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(Vehicle.VEHICLE_DEFAULT_MOTOR_ACCELERATION_MIN);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        sim.extendSimulationTime(20000);
        sim.startSimulation();

        RealVector velocityEnd = physicalVehicle.getVelocity();

        assertTrue(velocityStart.getEntry(0) == velocityEnd.getEntry(0));
        assertTrue(velocityStart.getEntry(1) > velocityEnd.getEntry(1));
        assertTrue(velocityStart.getEntry(2) == velocityEnd.getEntry(2));
    }

    /**
     * Test if the vehicle will drive in a straight line after steering to the right and
     * constantly accelerating
     */
    @Test
    public void testWillDriveInStraightLineAfterSteering() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (30 seconds)
        sim.stopAfter(30000);

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(1);
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING).setActuatorValueTarget(0.3);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Start simulation
        sim.stopAfter(5000);
        sim.startSimulation();

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING).setActuatorValueTarget(0);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        RealVector afterSteeringValuePos = physicalVehicle.getPos();

        sim.extendSimulationTime(20000);
        sim.startSimulation();

        RealVector endValuePos = physicalVehicle.getPos();

        assertTrue(afterSteeringValuePos.getEntry(0) < endValuePos.getEntry(0));
        assertTrue(afterSteeringValuePos.getEntry(1) < endValuePos.getEntry(1));
        assertTrue(afterSteeringValuePos.getEntry(2) == endValuePos.getEntry(2));
    }

    /**
     * Test if the vehicle has no applied force in the beginning
     */
    @Test
    public void testHasNoForceAtStart() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (30 seconds)
        sim.stopAfter(2000);

        //Start simulation
        sim.startSimulation();


        assertTrue(physicalVehicle.getForce().getEntry(0) == 0);
        assertTrue(physicalVehicle.getForce().getEntry(1) == 0);
        assertTrue(physicalVehicle.getForce().getEntry(2) == 0);
    }

    /**
     * Test if the vehicle has no angular velocity in the beginning
     */
    @Test
    public void testHasNoAngularVelocityAtStart() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (30 seconds)
        sim.stopAfter(2000);

        //Start simulation
        sim.startSimulation();


        assertTrue(physicalVehicle.getAngularVelocity().getEntry(0) == 0);
        assertTrue(physicalVehicle.getAngularVelocity().getEntry(1) == 0);
        assertTrue(physicalVehicle.getAngularVelocity().getEntry(2) == 0);
    }

    /**
     * Test if the rotation matrix stays orthogonal
     */
    @Test
    public void testRotationMatrixStaysOrthogonal() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Set simulation duration (30 seconds)
        sim.stopAfter(30000);

        //Start simulation
        sim.startSimulation();

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(1);
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING).setActuatorValueTarget(0.3);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Start simulation
        sim.stopAfter(2000);
        sim.startSimulation();

        RealMatrix matrix1 = null;
        RealMatrix matrix2 = null;

        try {
            matrix1 = MathHelper.matrixInvert(physicalVehicle.getGeometryRot());
            matrix2 =  physicalVehicle.getGeometryRot().transpose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(MathHelper.matrixEquals(matrix1, matrix2, 0.00001));

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_STEERING).setActuatorValueTarget(0);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        sim.extendSimulationTime(2000);
        sim.startSimulation();

        try {
            matrix1 = MathHelper.matrixInvert(physicalVehicle.getGeometryRot());
            matrix2 =  physicalVehicle.getGeometryRot().transpose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(MathHelper.matrixEquals(matrix1, matrix2, 0.00001));

        sim.extendSimulationTime(2000);
        sim.startSimulation();

        try {
            matrix1 = MathHelper.matrixInvert(physicalVehicle.getGeometryRot());
            matrix2 =  physicalVehicle.getGeometryRot().transpose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(MathHelper.matrixEquals(matrix1, matrix2, 0.00001));
    }

    /**
     * Test if airdrag brings the vehicle to a full stop
     */
    @Test
    public void testWillComeToHold() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(1);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Start simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        RealVector velocityStart = physicalVehicle.getVelocity();

        try {
            vehicle.getVehicleActuator(VehicleActuatorType.VEHICLE_ACTUATOR_TYPE_MOTOR).setActuatorValueTarget(0);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        sim.extendSimulationTime(45000);
        sim.startSimulation();

        RealVector velocityEnd = physicalVehicle.getVelocity();

        assertTrue(velocityStart.getEntry(0) == velocityEnd.getEntry(0));
        assertTrue(velocityEnd.getEntry(1) < 1 && (velocityEnd.getEntry(1) > -1));
        assertTrue(velocityStart.getEntry(2) == velocityEnd.getEntry(2));
    }

    @Test
    public void testMassDistribution() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Asserts
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_FRONT / 2));
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_FRONT / 2));
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_BACK / 2));
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_BACK / 2));
        assertTrue(vehicle.getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_FRONT + Vehicle.VEHICLE_DEFAULT_MASS_BACK));

        // Start simulation
        sim.stopAfter(3000);
        sim.startSimulation();

        // Asserts
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_FRONT / 2));
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_FRONT / 2));
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_BACK / 2));
        assertTrue(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_BACK / 2));
        assertTrue(vehicle.getMass() == (Vehicle.VEHICLE_DEFAULT_MASS_FRONT + Vehicle.VEHICLE_DEFAULT_MASS_BACK));
    }

    @Test
    public void testMassPointDistances() {
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        // Start simulation
        sim.stopAfter(1000);
        sim.startSimulation();

        // Asserts
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);

        sim.extendSimulationTime(2000);
        sim.startSimulation();

        // Asserts
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
        assertTrue(Math.abs(vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getCenterDiff().getNorm() - vehicle.getWheelMassPoints()[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].getLocalCenterDiff().getNorm()) < 0.0001);
    }

    @Test
    public void statusLoggerContainsAddedMessage(){
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        sim.stopAfter(10000);

        //Add engine failure message
        vehicle.setStatusLogger(new RandomStatusLogger());
        StatusMessage message = new StatusMessage(VEHICLE_STATUS_SENSOR_ENGINE_STATUS,VehicleStatus.VEHICLE_STATUS_CRITICAL, 0x5535);
        vehicle.getStatusLogger().addMessage(message);

        //Run simulation
        sim.startSimulation();
        sim.waitUntilSimulationFinished();

        //Check the error memory for engine failure
        List<StatusMessage> statusList = vehicle.getStatusLogger().readStatusMemory(VEHICLE_STATUS_SENSOR_ENGINE_STATUS);
        assertTrue(statusList.contains(message));
    }

    @Test
    public void statusLoggerDeletesAddedMessage(){
        Simulator sim = Simulator.getSharedInstance();

        // Create a new vehicle
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Vehicle vehicle = physicalVehicle.getSimulationVehicle();

        // Add physicalVehicle to simulation
        sim.registerSimulationObject(physicalVehicle);

        sim.stopAfter(5000);

        //Add engine failure message
        vehicle.setStatusLogger(new RandomStatusLogger());
        StatusMessage message = new StatusMessage(VEHICLE_STATUS_SENSOR_ENGINE_STATUS,VehicleStatus.VEHICLE_STATUS_CRITICAL, 0x5535);
        vehicle.getStatusLogger().addMessage(message);

        //Run simulation
        sim.startSimulation();

        //Check the error memory for engine failure
        List<StatusMessage> statusList = vehicle.getStatusLogger().readStatusMemory(VEHICLE_STATUS_SENSOR_ENGINE_STATUS);
        assertTrue(statusList.contains(message));
        vehicle.getStatusLogger().clearMemory();

        sim.extendSimulationTime(5000);
        sim.startSimulation();

        //Check the error memory for engine failure
        statusList = vehicle.getStatusLogger().readStatusMemory(VEHICLE_STATUS_SENSOR_ENGINE_STATUS);
        assertFalse(statusList.contains(message));
    }
}
