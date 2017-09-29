package simulation.vehicle;

import commons.controller.commons.BusEntry;
import commons.controller.commons.NavigationEntry;
import commons.controller.commons.Surface;
import commons.controller.commons.Vertex;
import commons.controller.interfaces.Bus;
import commons.controller.interfaces.FunctionBlockInterface;
import commons.map.IAdjacency;
import commons.map.IControllerNode;
import commons.simulation.Sensor;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import simulation.environment.WorldModel;
import simulation.environment.osm.IntersectionFinder;
import simulation.util.Log;

import java.awt.*;
import java.util.*;
import java.util.List;

import static commons.controller.commons.BusEntry.*;
import static simulation.environment.visualisationadapter.interfaces.EnvStreet.StreetTypes.*;
import static simulation.vehicle.MassPointType.*;
import static simulation.vehicle.VehicleActuatorType.*;

/**
 * Simulation objects for a generic vehicle.
 */
public class Vehicle {

    // Default average values for vehicle constructor

    /** Length of the vehicle in meters */
    public static final double VEHICLE_DEFAULT_LENGTH = 4.236423828125;

    /** Width of the vehicle in meters */
    public static final double VEHICLE_DEFAULT_WIDTH = 2.02712567705637776;

    /** Height of the vehicle in meters */
    public static final double VEHICLE_DEFAULT_HEIGHT = 1.19524474896355328;

    /** Maximum velocity of the vehicle */
    public static final double VEHICLE_DEFAULT_APPROX_MAX_VELOCITY = 100.0;

    /** Mass of the vehicle's front */
    public static final double VEHICLE_DEFAULT_MASS_FRONT = 950.0;

    /** Mass of the vehicle's back */
    public static final double VEHICLE_DEFAULT_MASS_BACK = 850.0;

    /** Radius of the wheels in meters */
    public static final double VEHICLE_DEFAULT_WHEEL_RADIUS = 0.3334;

    /** Distance between the left and the right wheels in meters */
    public static final double VEHICLE_DEFAULT_WHEEL_DIST_LEFT_RIGHT = 1.62025;

    /** Distance between front and back wheels in meters */
    public static final double VEHICLE_DEFAULT_WHEEL_DIST_FRONT_BACK = 2.921;

    /** Minimum acceleration that can be made by the motor */
    public static final double VEHICLE_DEFAULT_MOTOR_ACCELERATION_MIN = -1.5;

    /** Maximum acceleration that can be made by the motor */
    public static final double VEHICLE_DEFAULT_MOTOR_ACCELERATION_MAX = 3.5;

    /** Rate at which the motor can accelerate */
    public static final double VEHICLE_DEFAULT_MOTOR_ACCELERATION_RATE = 2.0;

    /** Minimum acceleration that can be made by the brakes */
    public static final double VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN = 0.0;

    /** Maximum acceleration that can be made by the brakes */
    public static final double VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX = 5.0;

    /** Acceleration rate of the brakes */
    public static final double VEHICLE_DEFAULT_BRAKES_ACCELERATION_RATE = 5.0;

    /** Minimum steering angle */
    public static final double VEHICLE_DEFAULT_STEERING_ANGLE_MIN = -0.785398;

    /** Maximum steering angle */
    public static final double VEHICLE_DEFAULT_STEERING_ANGLE_MAX = 0.785398;

    /** Rate at which the steering angle can change */
    public static final double VEHICLE_DEFAULT_STEERING_ANGLE_RATE = 0.5;

    /** Average tire pressure for car wheels in bar */
    public static final double VEHICLE_DEFAULT_TIRE_PRESSURE = 2.5;

    // Variables of the car

    /** M of formula */
    private double mass;

    /** Indicator whether the constant bus data was sent */
    private boolean constantBusDataSent;

    /** Representation of wheel mass points, indexed data */
    private MassPoint[] wheelMassPoints = new MassPoint[4];

    /** Motor of vehicle */
    private VehicleActuator motor;

    /** Brakes of vehicle */
    private VehicleActuator brakesFrontLeft;
    private VehicleActuator brakesFrontRight;
    private VehicleActuator brakesBackLeft;
    private VehicleActuator brakesBackRight;

    /** Steering of vehicle */
    private VehicleActuator steering;

    /** Status logging module */
    private StatusLogger statusLogger;

    /** Dimensions of vehicle in meters */
    private double length, width, height;

    /** Estimated maximum total velocity of vehicle */
    private double approxMaxTotalVelocity;

    /** Maximum temporary allowed velocity of vehicle */
    private double maxTemporaryAllowedVelocity;

    /** Radius of vehicle wheels in meters */
    private double wheelRadius;

    /** Track of the vehicle wheels */
    private double wheelDistLeftRight;

    /** Wheelbase of the vehicle wheels */
    private double wheelDistFrontBack;

    /** List of all the sensors of the vehicle */
    private List<Sensor> sensorList;

    /** Bus for the controller */
    private Optional<Bus> controllerBus;

    /** Controller for vehicle */
    private Optional<FunctionBlockInterface> controller;

    /** Navigation for vehicle */
    private Optional<FunctionBlockInterface> navigation;

    /** Last navigation target for vehicle */
    private Optional<IControllerNode> lastNavigationTarget;

    /** Camera image from visualization */
    private Optional<Image> cameraImage;

    /**
     * Constructor for a vehicle that is standing at its position
     * Use other functions to initiate movement and position updates
     *
     * @param controllerBus Optional bus for the controller of the vehicle
     * @param controller Optional controller of the vehicle
     * @param navigation Optional navigation of the vehicle
     */
    protected Vehicle(Optional<Bus> controllerBus, Optional<FunctionBlockInterface> controller, Optional<FunctionBlockInterface> navigation) {
        // Dimensions
        setDimensions(VEHICLE_DEFAULT_LENGTH, VEHICLE_DEFAULT_WIDTH, VEHICLE_DEFAULT_HEIGHT);

        // Approx maximum velocity
        setApproxMaxTotalVelocity(VEHICLE_DEFAULT_APPROX_MAX_VELOCITY);

        // Set mass of entire vehicle
        setWheelProperties(VEHICLE_DEFAULT_MASS_FRONT, VEHICLE_DEFAULT_MASS_BACK, VEHICLE_DEFAULT_WHEEL_RADIUS, VEHICLE_DEFAULT_WHEEL_DIST_LEFT_RIGHT, VEHICLE_DEFAULT_WHEEL_DIST_FRONT_BACK);

        // When created, maximum temporary allowed velocity is not limited
        maxTemporaryAllowedVelocity = Double.MAX_VALUE;

        // When created, the constant bus data is not sent yet
        constantBusDataSent = false;

        // Actuators
        setActuatorProperties(VEHICLE_ACTUATOR_TYPE_MOTOR, VEHICLE_DEFAULT_MOTOR_ACCELERATION_MIN, VEHICLE_DEFAULT_MOTOR_ACCELERATION_MAX, VEHICLE_DEFAULT_MOTOR_ACCELERATION_RATE);
        setActuatorProperties(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX, VEHICLE_DEFAULT_BRAKES_ACCELERATION_RATE);
        setActuatorProperties(VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX, VEHICLE_DEFAULT_BRAKES_ACCELERATION_RATE);
        setActuatorProperties(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX, VEHICLE_DEFAULT_BRAKES_ACCELERATION_RATE);
        setActuatorProperties(VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MIN, VEHICLE_DEFAULT_BRAKES_ACCELERATION_MAX, VEHICLE_DEFAULT_BRAKES_ACCELERATION_RATE);
        setActuatorProperties(VEHICLE_ACTUATOR_TYPE_STEERING, VEHICLE_DEFAULT_STEERING_ANGLE_MIN, VEHICLE_DEFAULT_STEERING_ANGLE_MAX, VEHICLE_DEFAULT_STEERING_ANGLE_RATE);

        //Create status logger
        statusLogger = new StatusLogger();

        // Set components of the vehicle
        sensorList = new ArrayList<>();
        this.controllerBus = controllerBus;
        this.controller = controller;
        this.navigation = navigation;
        this.lastNavigationTarget = Optional.empty();

        // Initialize camera image with empty optional
        cameraImage = Optional.empty();

        Log.finest("Vehicle: Constructor - Vehicle constructed: " + this);
    }

    /**
     * Function that sets wheel properties to the vehicle
     *
     * @param massFront          Sum of mass for both front wheels
     * @param massBack           Sum of mass for both back wheels
     * @param wheelRadius        Radius of wheels
     * @param wheelDistLeftRight Distance between left and right wheels
     * @param wheelDistFrontBack Distance between front and back wheels
     */
    void setWheelProperties(double massFront, double massBack, double wheelRadius, double wheelDistLeftRight, double wheelDistFrontBack) {
        Log.finest("Vehicle: setWheelProperties - Vehicle at start: " + this);
        mass = massFront + massBack;
        this.wheelRadius = wheelRadius;
        this.wheelDistLeftRight = wheelDistLeftRight;
        this.wheelDistFrontBack = wheelDistFrontBack;

        // Create mass points with incomplete information
        RealVector localPosFrontLeft = new ArrayRealVector(new double[]{-(wheelDistLeftRight / 2), (wheelDistFrontBack / 2), 0.0});
        RealVector localPosFrontRight = new ArrayRealVector(new double[]{(wheelDistLeftRight / 2), (wheelDistFrontBack / 2), 0.0});
        RealVector localPosBackLeft = new ArrayRealVector(new double[]{-(wheelDistLeftRight / 2), -(wheelDistFrontBack / 2), 0.0});
        RealVector localPosBackRight = new ArrayRealVector(new double[]{(wheelDistLeftRight / 2), -(wheelDistFrontBack / 2), 0.0});
        RealVector zeroVector = new ArrayRealVector(new double[]{0.0, 0.0, 0.0});
        RealVector gravitationVectorFrontWheel = new ArrayRealVector(new double[]{0.0, 0.0, (massFront / 2) * -9.81});
        RealVector gravitationVectorBackWheel = new ArrayRealVector(new double[]{0.0, 0.0, (massBack / 2) * -9.81});

        wheelMassPoints[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()] = new MassPoint(MASS_POINT_TYPE_WHEEL_FRONT_LEFT, localPosFrontLeft, localPosFrontLeft, zeroVector, zeroVector, zeroVector, zeroVector, gravitationVectorFrontWheel, (massFront / 2));
        wheelMassPoints[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()] = new MassPoint(MASS_POINT_TYPE_WHEEL_FRONT_RIGHT, localPosFrontRight, localPosFrontRight, zeroVector, zeroVector, zeroVector, zeroVector, gravitationVectorFrontWheel, (massFront / 2));
        wheelMassPoints[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()] = new MassPoint(MASS_POINT_TYPE_WHEEL_BACK_LEFT, localPosBackLeft, localPosBackLeft, zeroVector, zeroVector, zeroVector, zeroVector, gravitationVectorBackWheel, (massBack / 2));
        wheelMassPoints[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()] = new MassPoint(MASS_POINT_TYPE_WHEEL_BACK_RIGHT, localPosBackRight, localPosBackRight, zeroVector, zeroVector, zeroVector, zeroVector, gravitationVectorBackWheel, (massBack / 2));

        wheelMassPoints[MASS_POINT_TYPE_WHEEL_FRONT_LEFT.ordinal()].setPressure(VEHICLE_DEFAULT_TIRE_PRESSURE);
        wheelMassPoints[MASS_POINT_TYPE_WHEEL_FRONT_RIGHT.ordinal()].setPressure(VEHICLE_DEFAULT_TIRE_PRESSURE);
        wheelMassPoints[MASS_POINT_TYPE_WHEEL_BACK_LEFT.ordinal()].setPressure(VEHICLE_DEFAULT_TIRE_PRESSURE);
        wheelMassPoints[MASS_POINT_TYPE_WHEEL_BACK_RIGHT.ordinal()].setPressure(VEHICLE_DEFAULT_TIRE_PRESSURE);
        Log.finest("Vehicle: setWheelProperties - Vehicle at end: " + this);
    }

    /**
     * Function that sets actuator properties to the vehicle
     *
     * @param actuatorType       Type of the actuator
     * @param actuatorValueMin   Minimum allowed value of the actuator
     * @param actuatorValueMax   Maximum allowed value of the actuator
     * @param actuatorChangeRate Change rate of the actuator
     */
    void setActuatorProperties(VehicleActuatorType actuatorType, double actuatorValueMin, double actuatorValueMax, double actuatorChangeRate) {
        Log.finest("Vehicle: setActuatorProperties - Vehicle at start: " + this);

        switch (actuatorType) {
            case VEHICLE_ACTUATOR_TYPE_MOTOR:
                motor = new VehicleActuator(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
                break;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT:
                brakesFrontLeft = new VehicleActuator(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
                break;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT:
                brakesFrontRight = new VehicleActuator(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
                break;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT:
                brakesBackLeft = new VehicleActuator(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
                break;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT:
                brakesBackRight = new VehicleActuator(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
                break;
            case VEHICLE_ACTUATOR_TYPE_STEERING:
                steering = new VehicleActuator(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
                break;
            default:
                break;
        }

        Log.finest("Vehicle: setActuatorProperties - Vehicle at end: " + this);
    }

    /**
     * Function that sets dimensions to the vehicle
     *
     * @param length Length of the vehicle
     * @param width  Width of the vehicle
     * @param height Height of the vehicle
     */
    void setDimensions(double length, double width, double height) {
        Log.finest("Vehicle: setDimensions - Vehicle at start: " + this);
        this.length = length;
        this.width = width;
        this.height = height;
        Log.finest("Vehicle: setDimensions - Vehicle at end: " + this);
    }

    /**
     * Function that sets the maximum approximate velocity of vehicle
     *
     * @param approxMaxTotalVelocity Maximum approximate velocity of vehicle
     */
    protected void setApproxMaxTotalVelocity(double approxMaxTotalVelocity) {
        this.approxMaxTotalVelocity = approxMaxTotalVelocity;
    }

    /**
     * Function that returns the optional controller bus
     *
     * @return Optional controller bus of simulated car
     */
    protected Optional<Bus> getControllerBus() {
        return controllerBus;
    }

    /**
     * Function that sets the optional controller bus
     *
     * @param controllerBus Optional controller bus of simulated car
     */
    protected void setControllerBus(Optional<Bus> controllerBus) {
        this.controllerBus = controllerBus;
    }

    /**
     * Function that returns the optional controller
     *
     * @return Optional controller of simulated car
     */
    protected Optional<FunctionBlockInterface> getController() {
        return controller;
    }

    /**
     * Function that sets the optional controller
     *
     * @param controller Optional controller of simulated car
     */
    protected void setController(Optional<FunctionBlockInterface> controller) {
        this.controller = controller;
    }

    /**
     * Function that returns the optional navigation
     *
     * @return Optional navigation of simulated car
     */
    public Optional<FunctionBlockInterface> getNavigation() {
        return navigation;
    }

    /**
     * Function that sets the optional navigation
     *
     * @param navigation Optional navigation of simulated car
     */
    public void setNavigation(Optional<FunctionBlockInterface> navigation) {
        this.navigation = navigation;
    }

    /**
     * Function that exchanges data with the controller
     *
     * @param deltaT Time difference of the last update loop in seconds
     */
    protected void exchangeDataWithController(double deltaT) {

        // Send / Retrieve data from controller bus, if available
        if (controllerBus.isPresent() && controller.isPresent()) {

            // Send vehicle data to controller
            if (!constantBusDataSent) {
                controllerBus.get().setData(CONSTANT_NUMBER_OF_GEARS.toString(), 1);
                controllerBus.get().setData(CONSTANT_WHEELBASE.toString(), getWheelDistFrontBack());
                controllerBus.get().setData(CONSTANT_MAXIMUM_TOTAL_VELOCITY.toString(), getApproxMaxTotalVelocity());
                controllerBus.get().setData(CONSTANT_MOTOR_MAX_ACCELERATION.toString(), motor.getActuatorValueMax());
                controllerBus.get().setData(CONSTANT_MOTOR_MIN_ACCELERATION.toString(), motor.getActuatorValueMin());
                controllerBus.get().setData(CONSTANT_BRAKES_MAX_ACCELERATION.toString(), brakesFrontLeft.getActuatorValueMax());
                controllerBus.get().setData(CONSTANT_BRAKES_MIN_ACCELERATION.toString(), brakesFrontLeft.getActuatorValueMin());
                controllerBus.get().setData(CONSTANT_STEERING_MAX_ANGLE.toString(), steering.getActuatorValueMax());
                controllerBus.get().setData(CONSTANT_STEERING_MIN_ANGLE.toString(), steering.getActuatorValueMin());
                controllerBus.get().setData(CONSTANT_TRAJECTORY_ERROR.toString(), 0.0);

                constantBusDataSent = true;
            }

            // Send sensor data: Write values to bus
            for (Sensor sensor : sensorList) {
                // Put data from sensor on the bus
                controllerBus.get().setData(sensor.getType().toString(), sensor.getValue());

                // Special case for weather / surface, for now just constant Asphalt
                if (sensor.getType() == SENSOR_WEATHER) {
                    Surface surface = Surface.Asphalt;
                    controllerBus.get().setData(SENSOR_CURRENT_SURFACE.toString(), surface);
                }
            }

            // TODO: This logic should be moved to the controller!
            Optional<Sensor> streetTypeSensor = getSensorByType(SENSOR_STREETTYPE);
            if (streetTypeSensor.isPresent()) {
                String streetType = (String)(streetTypeSensor.get().getValue());
                double allowedVelocityByStreetType = Double.MAX_VALUE;

                switch(streetType){
                    case "MOTORWAY":
                        allowedVelocityByStreetType = 100.0;
                        break;
                    case "A_ROAD":
                        allowedVelocityByStreetType = 70.0;
                        break;
                    case "STREET":
                        allowedVelocityByStreetType = 50.0;
                        break;
                    case "LIVING_STREET":
                        allowedVelocityByStreetType = 30.0;
                        break;
                }

                setMaxTemporaryAllowedVelocity(Math.min(getMaxTemporaryAllowedVelocity(), allowedVelocityByStreetType));
            }

            // Set other values on bus that can change during simulation
            controllerBus.get().setData(SIMULATION_DELTA_TIME.toString(), deltaT);
            controllerBus.get().setData(VEHICLE_MAX_TEMPORARY_ALLOWED_VELOCITY.toString(), getMaxTemporaryAllowedVelocity());

            //Give the bus to the mainControlBlock
            controller.get().setInputs(controllerBus.get().getAllData());

            // Call controller to compute new values
            controller.get().execute();

            //Pass the data of the mainControlBlock to the bus
            controllerBus.get().setAllData(controller.get().getOutputs());

            // Read new values from bus
            double motorValue = (Double)(controllerBus.get().getData(BusEntry.ACTUATOR_ENGINE.toString()));
            double brakeValue = (Double)(controllerBus.get().getData(BusEntry.ACTUATOR_BRAKE.toString()));
            double steeringValue = (Double)(controllerBus.get().getData(BusEntry.ACTUATOR_STEERING.toString()));

            // Set new values from bus to actuators
            try {
                motor.setActuatorValueTarget(motorValue);
                brakesFrontLeft.setActuatorValueTarget(brakeValue);
                brakesFrontRight.setActuatorValueTarget(brakeValue);
                brakesBackLeft.setActuatorValueTarget(brakeValue);
                brakesBackRight.setActuatorValueTarget(brakeValue);
                steering.setActuatorValueTarget(steeringValue);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function that returns the current vehicle actuators
     *
     * @param type Type of the vehicle actuator to get
     * @return Current vehicle actuator object for the type, otherwise null
     */
    public VehicleActuator getVehicleActuator(VehicleActuatorType type) {
        switch (type) {
            case VEHICLE_ACTUATOR_TYPE_MOTOR:
                return motor;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_LEFT:
                return brakesFrontLeft;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_FRONT_RIGHT:
                return brakesFrontRight;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_LEFT:
                return brakesBackLeft;
            case VEHICLE_ACTUATOR_TYPE_BRAKES_BACK_RIGHT:
                return brakesBackRight;
            case VEHICLE_ACTUATOR_TYPE_STEERING:
                return steering;
        }

        return null;
    }

    /**
     * Function that returns the four wheel mass points of the vehicle
     *
     * @return Four wheel mass points of the vehicle
     */
    public MassPoint[] getWheelMassPoints() {
        return wheelMassPoints;
    }

    /**
     * Function that sets the four wheel mass points of the vehicle
     *
     * @param wheelMassPoints New MassPoint array with four objects of the four wheel mass points
     */
    public void setWheelMassPoints(MassPoint[] wheelMassPoints) {
        this.wheelMassPoints = wheelMassPoints;
    }

    /**
     * Function that returns the maximum approximate velocity of vehicle
     *
     * @return Maximum approximate velocity of vehicle
     */
    public double getApproxMaxTotalVelocity() {
        return approxMaxTotalVelocity;
    }

    /**
     * Function that returns the mass of simulated car
     *
     * @return Mass of simulated car
     */
    public double getMass() {
        return mass;
    }

    /**
     * Function that returns the length of simulated car
     *
     * @return Length of simulated car
     */
    public double getLength() {
        return length;
    }

    /**
     * Function that returns the width of simulated car
     *
     * @return Width of simulated car
     */
    public double getWidth() {
        return width;
    }

    /**
     * Function that returns the height of simulated car
     *
     * @return Height of simulated car
     */
    public double getHeight() {
        return height;
    }

    /**
     * Function that returns the wheelRadius of simulated car
     *
     * @return Wheel radius of simulated car
     */
    public double getWheelRadius() {
        return wheelRadius;
    }

    /**
     * Function that returns the distance between left and right wheels of simulated car
     *
     * @return Distance between left and right wheels of simulated car
     */
    double getWheelDistLeftRight() {
        return wheelDistLeftRight;
    }

    /**
     * Function that returns the distance between front and back wheels of simulated car
     *
     * @return Distance between front and back wheels of simulated car
     */
    double getWheelDistFrontBack() {
        return wheelDistFrontBack;
    }

    /**
     * Function that returns the maximum temporary allowed velocity of simulated car
     *
     * @return Maximum temporary allowed velocity
     */
    public double getMaxTemporaryAllowedVelocity() {
        return maxTemporaryAllowedVelocity;
    }

    /**
     * Function that sets the maximum temporary allowed velocity of simulated car
     *
     * @param maxTemporaryAllowedVelocity New maximum temporary allowed velocity of simulated car
     */
    public void setMaxTemporaryAllowedVelocity(double maxTemporaryAllowedVelocity) {
        this.maxTemporaryAllowedVelocity = maxTemporaryAllowedVelocity;
    }

    /**
     * Function that returns the status logging module of the vehicle
     *
     * @return Status logging module of the vehicle
     */
    public StatusLogger getStatusLogger() {
        return statusLogger;
    }

    /**
     * Setter for internal fault memory of vehicle
     *
     * @param statusLogger Fault memory
     */
    public void setStatusLogger(StatusLogger statusLogger) {
        this.statusLogger = statusLogger;
    }

    /**
     * Add a sensor the the vehicle
     *
     * @param sensor the sensor to be added
     */
    public void addSensor(Sensor sensor) {
        this.sensorList.add(sensor);
    }

    /**
     * Return a optional sensor of the requested type. If no such sensor is available return an <code>Optional.empty()</code>
     *
     * @param type the type of the sensor to be requested
     * @return a concrete sensor
     */
    public Optional<Sensor> getSensorByType(BusEntry type) {
        for (Sensor sensor : sensorList) {
            if (sensor.getType() == type) {
                return Optional.of(sensor);
            }
        }
        return Optional.empty();
    }

    /**
     * Getter function for the camera image
     *
     * @return Optional of the current camera image
     */
    public Optional<Image> getCameraImage() {
        return cameraImage;
    }

    /**
     * Setter function for the camera image
     *
     * @param cameraImage Optional of the new camera image to be stored
     */
    public void setCameraImage(Optional<Image> cameraImage) {
        this.cameraImage = cameraImage;
    }

    /**
     * Function that initiates or updates navigation of the vehicle to a specified point in the map
     * Controller is periodically called such that setting these values in the function here should work without issues
     *
     * @param node Target node for navigation
     */
    public void navigateTo(IControllerNode node) {
        navigateTo(node, Collections.synchronizedList(new LinkedList<RealVector>()));
    }

    /**
     * Function that initiates or updates navigation of the vehicle to a specified point in the map
     * Controller is periodically called such that setting these values in the function here should work without issues
     * Tries to avoid list of coordinates, might not be possible if all ways to target are affected. Then avoiding coordinates is not possible.
     *
     * @param node Target node for navigation
     * @param avoidCoordinates List of coordinates which should be avoided in path finding, if possible
     */
    public void navigateTo(IControllerNode node, List<RealVector> avoidCoordinates) {
        // Check for valid objects
        if (!navigation.isPresent() || !controllerBus.isPresent() || !getSensorByType(BusEntry.SENSOR_GPS_COORDINATES).isPresent()) {
            Log.warning("Vehicle: navigateTo called without valid navigation or controllerBus or GPS sensor");
            return;
        }

        // Set last navigation target
        this.lastNavigationTarget = Optional.of(node);

        // Get current GPS coordinates from sensor
        getSensorByType(BusEntry.SENSOR_GPS_COORDINATES).get().update();
        Object gpsCoordinates = getSensorByType(BusEntry.SENSOR_GPS_COORDINATES).get().getValue();

        // Process navigation target without avoiding coordinates for reference
        Map<String, Object> navigationInputs = new LinkedHashMap<>();
        navigationInputs.put(NavigationEntry.MAP_ADJACENCY_LIST.toString(), WorldModel.getInstance().getControllerMap().getAdjacencies());
        navigationInputs.put(NavigationEntry.CONSTANT_WHEELBASE.toString(), getWheelDistFrontBack());
        navigationInputs.put(NavigationEntry.GPS_COORDINATES.toString(), gpsCoordinates);
        navigationInputs.put(NavigationEntry.TARGET_NODE.toString(), node);
        navigation.get().setInputs(navigationInputs);
        navigation.get().execute();

        // Stop processing if trajectory or avoiding coordinate list is empty
        if (navigation.get().getOutputs().get(NavigationEntry.DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString()) == null) {
            return;
        }

        List<Vertex> trajectoryWithoutAvoiding = (List<Vertex>)(navigation.get().getOutputs().get(NavigationEntry.DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString()));
        if (trajectoryWithoutAvoiding.isEmpty() || avoidCoordinates.isEmpty()) {
            controllerBus.get().setData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString(), trajectoryWithoutAvoiding);
            afterTrajectoryUpdate();
            return;
        }

        // Compare distance to final destination to compare quality of trajectories
        RealVector endTarget = new ArrayRealVector(new double[]{node.getPoint().getX(), node.getPoint().getY(), node.getPoint().getZ()});
        double endTargetDistanceWithoutAvoiding = trajectoryWithoutAvoiding.get(trajectoryWithoutAvoiding.size() - 1).getPosition().getDistance(endTarget);

        // Compute trajectory with avoiding coordinates on a copied adjacency list
        ArrayList<IAdjacency> adjacencyFiltered = new ArrayList<>(WorldModel.getInstance().getControllerMap().getAdjacencies());
        ArrayList<IAdjacency> adjacencyRemove = new ArrayList<>();
        Set<Long> filterOsmIds = new HashSet<>();

        // Find OSM IDs with minimal distance to coordinates to be avoided
        for (RealVector pos : avoidCoordinates) {
            double minDistSq = Double.MAX_VALUE;
            long minOsmId = -1L;

            for (IAdjacency adjacency : adjacencyFiltered) {
                RealVector posAdjacency1 = new ArrayRealVector(new double[]{adjacency.getNode1().getPoint().getX(), adjacency.getNode1().getPoint().getY(), adjacency.getNode1().getPoint().getZ()});
                RealVector posAdjacency2 = new ArrayRealVector(new double[]{adjacency.getNode2().getPoint().getX(), adjacency.getNode2().getPoint().getY(), adjacency.getNode2().getPoint().getZ()});

                // Compute square distances manually here, cheaper than distance since no sqrt is needed for minimum
                double distSq1 = (pos.getEntry(0) - posAdjacency1.getEntry(0)) * (pos.getEntry(0) - posAdjacency1.getEntry(0)) + (pos.getEntry(1) - posAdjacency1.getEntry(1)) * (pos.getEntry(1) - posAdjacency1.getEntry(1)) + (pos.getEntry(2) - posAdjacency1.getEntry(2)) * (pos.getEntry(2) - posAdjacency1.getEntry(2));
                double distSq2 = (pos.getEntry(0) - posAdjacency2.getEntry(0)) * (pos.getEntry(0) - posAdjacency2.getEntry(0)) + (pos.getEntry(1) - posAdjacency2.getEntry(1)) * (pos.getEntry(1) - posAdjacency2.getEntry(1)) + (pos.getEntry(2) - posAdjacency2.getEntry(2)) * (pos.getEntry(2) - posAdjacency2.getEntry(2));

                if (distSq1 < minDistSq) {
                    minDistSq = distSq1;
                    minOsmId = adjacency.getNode1().getOsmId();
                }

                if (distSq2 < minDistSq) {
                    minDistSq = distSq2;
                    minOsmId = adjacency.getNode2().getOsmId();
                }
            }

            if (minOsmId > 0) {
                filterOsmIds.add(minOsmId);
            }
        }

        // Find adjacency entries with OSM Ids to be removed
        for (IAdjacency adjacency : adjacencyFiltered) {
            if (filterOsmIds.contains(adjacency.getNode1().getOsmId()) || filterOsmIds.contains(adjacency.getNode2().getOsmId())) {
                adjacencyRemove.add(adjacency);
            }
        }

        // Remove all adjacency entries to be filtered out
        adjacencyFiltered.removeAll(adjacencyRemove);

        // Process navigation target without avoiding coordinates for reference
        Map<String, Object> navigationInputsFiltered = new LinkedHashMap<>();
        navigationInputsFiltered.put(NavigationEntry.MAP_ADJACENCY_LIST.toString(), adjacencyFiltered);
        navigationInputsFiltered.put(NavigationEntry.CONSTANT_WHEELBASE.toString(), getWheelDistFrontBack());
        navigationInputsFiltered.put(NavigationEntry.GPS_COORDINATES.toString(), gpsCoordinates);
        navigationInputsFiltered.put(NavigationEntry.TARGET_NODE.toString(), node);
        navigation.get().setInputs(navigationInputsFiltered);
        navigation.get().execute();

        // If trajectory with avoiding is null or empty, just set original result without avoiding
        if (navigation.get().getOutputs().get(NavigationEntry.DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString()) == null) {
            controllerBus.get().setData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString(), trajectoryWithoutAvoiding);
            afterTrajectoryUpdate();
            return;
        }

        List<Vertex> trajectoryWithAvoiding = (List<Vertex>)(navigation.get().getOutputs().get(NavigationEntry.DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString()));
        if (trajectoryWithAvoiding.isEmpty()) {
            controllerBus.get().setData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString(), trajectoryWithoutAvoiding);
            afterTrajectoryUpdate();
            return;
        }

        // Compare distance to final destination to compare quality of trajectories
        double endTargetDistanceWithAvoiding = trajectoryWithAvoiding.get(trajectoryWithAvoiding.size() - 1).getPosition().getDistance(endTarget);

        // Check if end target distance with avoiding is roughly as good as without avoiding
        // If yes then set new trajectory with avoiding, otherwise use old one without avoiding
        if (endTargetDistanceWithAvoiding - 5.0 <= endTargetDistanceWithoutAvoiding) {
            controllerBus.get().setData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString(), trajectoryWithAvoiding);
            afterTrajectoryUpdate();
            return;
        }

        controllerBus.get().setData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString(), trajectoryWithoutAvoiding);
        afterTrajectoryUpdate();
    }

    /**
     * Function that returns lastNavigationTarget
     *
     * @return Value for lastNavigationTarget
     */
    public Optional<IControllerNode> getLastNavigationTarget() {
        return lastNavigationTarget;
    }

    /**
     * Get current trajectory of the vehicle, if available. Otherwise return empty list.
     *
     * @return Current trajectory of the vehicle, if not available return empty list
     */
    public List<Vertex> getTrajectory() {
        // Check if trajectory is available and return copy if valid
        if (controllerBus.isPresent()) {
            if (controllerBus.get().getData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString()) != null) {
                ArrayList<Vertex> originalList = (ArrayList<Vertex>)(controllerBus.get().getData(NAVIGATION_DETAILED_PATH_WITH_MAX_STEERING_ANGLE.toString()));
                return new ArrayList<>(originalList);
            }
        }

        // Fallback to empty list
        return new ArrayList<>();
    }

    /**
     * Get nearest position that is located on the ordered trajectory
     *
     * @param inputTrajectory Input trajectory for which the nearest position should be computed
     * @param position Source position from which the nearest trajectory position should to be computed
     * @param accuracy Positive integer, the higher the more accurate the result is, 20 is recommended
     * @return Map entry containing: Integer of nextVertex index in trajectory and RealVector of nearest position that is located on the input trajectory
     * @throws IllegalArgumentException if input trajectory is empty
     */
    public static Map.Entry<Integer, RealVector> getNearestPositionOnTrajectory(List<Vertex> inputTrajectory, RealVector position, int accuracy) {
        // Exception for empty input trajectory
        if (inputTrajectory.isEmpty()) {
            throw new IllegalArgumentException("Vehicle - getNearestPositionOnTrajectory: inputTrajectory is empty!");
        }

        // Exception for invalid accuracy value
        if (accuracy <= 0) {
            throw new IllegalArgumentException("Vehicle - getNearestPositionOnTrajectory: Accuracy has invalid value " + accuracy);
        }

        List<RealVector> positionList = Collections.synchronizedList(new LinkedList<>());
        for (int i = 0; i < inputTrajectory.size(); ++i) {
            RealVector pos = inputTrajectory.get(i).getPosition();
            positionList.add(pos.copy());
        }

        return getNearestPositionOnPositionList(positionList, position, accuracy);
    }

    /**
     * Get nearest position that is located on the ordered input positions list
     *
     * @param inputPositions Input list of positions for which the nearest position should be computed
     * @param position Source position from which the nearest trajectory position should to be computed
     * @param accuracy Positive integer, the higher the more accurate the result is, 20 is recommended
     * @return Map entry containing: Integer of nextVertex index in trajectory and RealVector of nearest position that is located on the input trajectory
     * @throws IllegalArgumentException if input trajectory is empty
     */
    public static Map.Entry<Integer, RealVector> getNearestPositionOnPositionList(List<RealVector> inputPositions, RealVector position, int accuracy) {
        // Exception for empty input positions
        if (inputPositions.isEmpty()) {
            throw new IllegalArgumentException("Vehicle - getNearestPositionOnTrajectory: inputTrajectory is empty!");
        }

        // Exception for invalid accuracy value
        if (accuracy <= 0) {
            throw new IllegalArgumentException("Vehicle - getNearestPositionOnTrajectory: Accuracy has invalid value " + accuracy);
        }

        // Find index of nearest trajectory vertex
        int nearestVertex = 0;
        double minDist = Double.MAX_VALUE;

        for (int i = 0; i < inputPositions.size(); ++i) {
            RealVector v = inputPositions.get(i);
            double distance = ((v.getEntry(0) - position.getEntry(0)) * (v.getEntry(0) - position.getEntry(0)))
                            + ((v.getEntry(1) - position.getEntry(1)) * (v.getEntry(1) - position.getEntry(1)))
                            + ((v.getEntry(2) - position.getEntry(2)) * (v.getEntry(2) - position.getEntry(2)));

            if (distance < minDist) {
                minDist = distance;
                nearestVertex = i;
            }
        }

        // Find second nearest trajectory vertex
        int secondNearestVertex = 0;
        double secondMinDist = Double.MAX_VALUE;

        for (int i = 0; i < inputPositions.size(); ++i) {
            RealVector v = inputPositions.get(i);
            double distance = ((v.getEntry(0) - position.getEntry(0)) * (v.getEntry(0) - position.getEntry(0)))
                            + ((v.getEntry(1) - position.getEntry(1)) * (v.getEntry(1) - position.getEntry(1)))
                            + ((v.getEntry(2) - position.getEntry(2)) * (v.getEntry(2) - position.getEntry(2)));

            if (distance < secondMinDist && i != nearestVertex) {
                secondMinDist = distance;
                secondNearestVertex = i;
            }
        }

        // Compute next and prev vertex on trajectory and use them to get nearest point on trajectory
        int nextVertex = Math.max(nearestVertex, secondNearestVertex);
        int prevVertex = Math.min(nearestVertex, secondNearestVertex);
        RealVector nextVertexPos = inputPositions.get(nextVertex);
        RealVector prevVertexPos = inputPositions.get(prevVertex);
        RealVector fromPrevToNext = nextVertexPos.subtract(prevVertexPos);

        RealVector nearestPosOnTrajectory = prevVertexPos.copy();
        double minDistNearestPos = Double.MAX_VALUE;

        for (int i = 0; i <= accuracy; ++i) {
            double factor = (double)(i) / (double)(accuracy);
            RealVector possibleNearestPoint = prevVertexPos.add(fromPrevToNext.mapMultiply(factor));

            double distance = ((possibleNearestPoint.getEntry(0) - position.getEntry(0)) * (possibleNearestPoint.getEntry(0) - position.getEntry(0)))
                            + ((possibleNearestPoint.getEntry(1) - position.getEntry(1)) * (possibleNearestPoint.getEntry(1) - position.getEntry(1)))
                            + ((possibleNearestPoint.getEntry(2) - position.getEntry(2)) * (possibleNearestPoint.getEntry(2) - position.getEntry(2)));

            if (distance < minDistNearestPos) {
                minDistNearestPos = distance;
                nearestPosOnTrajectory = possibleNearestPoint.copy();
            }
        }

        // Return final result
        return new AbstractMap.SimpleEntry<>(nextVertex, nearestPosOnTrajectory);
    }

    /**
     * Internal function that is called after an trajectory update was performed
     */
    private void afterTrajectoryUpdate() {
        // Get current trajectory
        List<Vertex> trajectory = getTrajectory();
        if (trajectory.isEmpty()) {
            return;
        }

        // Add intersection node information to each vertex in the trajectory
        Set<OsmNode> intersectionNodes = IntersectionFinder.getInstance().getIntersections();
        for (Vertex vertex : trajectory) {
            for (OsmNode intersectionNode : intersectionNodes) {
                if (vertex.getOsmId() == intersectionNode.getId()) {
                    vertex.setIntersectionNode(true);
                }
            }
        }
    }

    /**
     * Function that updates all sensor data, should be called before data exchange with controller
     */
    public void updateAllSensors() {
        for (Sensor sensor : sensorList) {
            sensor.update();
        }
    }

    /**
     * Overwrite toString() to get a nice output for vehicles
     *
     * @return String that contains all information of vehicles
     */
    @Override
    public String toString() {
        return "Vehicle " + hashCode() + ": length: " + length +
                " , width: " + width +
                " , height: " + height +
                " , approxMaxTotalVelocity: " + approxMaxTotalVelocity +
                " , maxTemporaryAllowedVelocity: " + maxTemporaryAllowedVelocity +
                " , wheelRadius: " + wheelRadius +
                " , wheelDistLeftRight: " + wheelDistLeftRight +
                " , wheelDistFrontBack: " + wheelDistFrontBack +
                " , mass: " + mass +
                " , constantBusDataSent: " + constantBusDataSent +
                " , wheelMassPoints[0]: " + wheelMassPoints[0] +
                " , wheelMassPoints[1]: " + wheelMassPoints[1] +
                " , wheelMassPoints[2]: " + wheelMassPoints[2] +
                " , wheelMassPoints[3]: " + wheelMassPoints[3] +
                " , motor: " + motor +
                " , brakesFrontLeft: " + brakesFrontLeft +
                " , brakesFrontRight: " + brakesFrontRight +
                " , brakesBackLeft: " + brakesBackLeft+
                " , brakesBackRight: " + brakesBackRight +
                " , steering: " + steering +
                " , sensorList:" + sensorList +
                " , controllerBus:" + controllerBus +
                " , controller:" + controller +
                " , navigation:" + navigation +
                " , cameraImage:" + cameraImage;
    }
}
