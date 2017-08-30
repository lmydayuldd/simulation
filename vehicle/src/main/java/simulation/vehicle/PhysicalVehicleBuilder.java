package simulation.vehicle;

import com.google.gson.Gson;
import commons.controller.interfaces.Bus;
import commons.controller.interfaces.FunctionBlockInterface;
import commons.simulation.PhysicalObjectType;
import simulation.util.Log;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Builder class for a physicalVehicle to avoid complex physicalVehicle constructors
 */
public class PhysicalVehicleBuilder {

    /** Singleton instance of the class */
    private static PhysicalVehicleBuilder instance = null;

    /** The vehicle created by this class*/
    private PhysicalVehicle physicalVehicle = new PhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

    /**
     * Empty constructor, this is a singleton class
     */
    private PhysicalVehicleBuilder() {
    }

    /**
     * Function that creates or gets an instance of PhysicalVehicleBuilder
     *
     * @return PhysicalVehicleBuilder singleton class
     */
    public static PhysicalVehicleBuilder getInstance() {
        if (instance == null) {
            instance = new PhysicalVehicleBuilder();
        }

        return instance;
    }

    /**
     * Function that resets the instance of PhysicalVehicleBuilder
     */
    public static void resetInstance() {
        instance = null;
    }

    /**
     * Function that resets the physicalVehicle that is currently built in the builder class
     * Changes values to the default physicalVehicle values
     *
     * @return PhysicalVehicleBuilder singleton class
     */
    public PhysicalVehicleBuilder resetPhysicalVehicle() {
        Log.finest("PhysicalVehicleBuilder: resetPhysicalVehicle - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle = new PhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Log.finest("PhysicalVehicleBuilder: resetPhysicalVehicle - PhysicalVehicle at end: " + physicalVehicle);

        return getInstance();
    }

    /**
     * Function that returns the physicalVehicle that is currently built in the builder class
     *
     * @param controllerBus Optional bus for the controller of the vehicle
     * @param controller Optional controller of the vehicle
     * @param navigation Optional navigation of the vehicle
     * @return PhysicalVehicle that was built with the builder
     */
    public PhysicalVehicle buildPhysicalVehicle(Optional<Bus> controllerBus, Optional<FunctionBlockInterface> controller, Optional<FunctionBlockInterface> navigation) {
        Log.finest("PhysicalVehicleBuilder: buildPhysicalVehicle - PhysicalVehicle at start: " + physicalVehicle);

        physicalVehicle.initPhysicalVehicle(controllerBus, controller, navigation);
        PhysicalVehicle result = physicalVehicle;
        resetPhysicalVehicle();

        Log.finest("PhysicalVehicleBuilder: buildPhysicalVehicle - Returned physicalVehicle: " + result + " , reset physicalVehicle in builder:" + physicalVehicle);
        return result;
    }

    /**
     * Method takes a file that has to contain a valid JSON representation of a car.
     * It returns a @{@link PhysicalVehicleBuilder} instance according to the read JSON contents.
     *
     * @param file a file containing a valid JSON config for a car
     * @return {@link PhysicalVehicleBuilder} singleton class
     * @throws IOException thrown if the given file could either not be found or accessed/read.
     */
    public PhysicalVehicleBuilder loadPropertiesFromFile(File file) throws IOException {

        String jsonContents = new String(Files.readAllBytes(file.toPath()));
        return loadPropertiesFromJSON(jsonContents);
    }


    /**
     * Method to load car properties from a JSON String instead of manually setting them via the builder.
     * After loading from file, changes are still possible as usual via the methods the builder provides.
     *
     * @param json valid json configuration string
     * @return {@link PhysicalVehicleBuilder} singleton class
     */
    public PhysicalVehicleBuilder loadPropertiesFromJSON(String json) {

        ParsableVehicleProperties data = new Gson().fromJson(json, ParsableVehicleProperties.class); // contains the whole reviews list

        setDimensions(data.length, data.width, data.height);
        setGlobalPos(data.posX, data.posY, data.posY);
        setGlobalRotation(data.rotX, data.rotY, data.rotZ);
        setWheelProperties(data.massFront, data.massBack, data.wheelRadius, data.wheelDistLeftRight, data.wheelDistFrontBack);

        for (VehicleActuator a : data.actuators) {
            setActuatorProperties(a.getActuatorType(), a.getActuatorValueMin(), a.getActuatorValueMax(), a.getActuatorValueChangeRate());
        }

        return getInstance();
    }

    /**
     * Stores the currently build car JSON serialized in a File on the mass storage.
     * Be careful, default behavior is to overwrite existing files.
     *
     * @param whereToStore file to store the JSON
     * @return current instance of the Builder
     * @throws IOException thrown if the given path cannot be accessed.
     */
    public PhysicalVehicleBuilder storeJSONInFile(File whereToStore) throws IOException {

        Gson g = new Gson();
        PhysicalVehicle v = this.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        ParsableVehicleProperties carProps = new PhysicalVehicleBuilder.ParsableVehicleProperties(v);
        String json = g.toJson(carProps, PhysicalVehicleBuilder.ParsableVehicleProperties.class);

        FileWriter fooWriter = new FileWriter(whereToStore, false);

        fooWriter.write(json);
        fooWriter.flush();
        fooWriter.close();

        return getInstance();
    }

    /**
     * Function that sets dimensions to the physicalVehicle that is currently built in the builder class
     *
     * @param length Length of the physicalVehicle
     * @param width Width of the physicalVehicle
     * @param height Height of the physicalVehicle
     * @return PhysicalVehicleBuilder singleton class
     */
    public PhysicalVehicleBuilder setDimensions(double length, double width, double height) {
        Log.finest("PhysicalVehicleBuilder: setDimensions - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.getSimulationVehicle().setDimensions(length, width, height);
        Log.finest("PhysicalVehicleBuilder: setDimensions - PhysicalVehicle at end: " + physicalVehicle);

        return getInstance();
    }

    /**
     * Function that sets the global position to the physicalVehicle that is currently built in the builder class
     * Note: This refers to the cars geometry center position, not the center of mass position
     * @param x Global x position of physicalVehicle
     * @param y Global y position of physicalVehicle
     * @param z Global z position of physicalVehicle
     * @return PhysicalVehicleBuilder singleton class
     *
     */
    public PhysicalVehicleBuilder setGlobalPos(double x, double y, double z) {

        Log.finest("PhysicalVehicleBuilder: setGlobalPos - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.setGlobalPos(x, y, z);
        Log.finest("PhysicalVehicleBuilder: setGlobalPos - PhysicalVehicle at end: " + physicalVehicle);

        return getInstance();
    }

    /**
     * Function that sets the global rotation to the physicalVehicle that is currently built in the builder class
     *
     * @param rotX Global x rotation of physicalVehicle
     * @param rotY Global y rotation of physicalVehicle
     * @param rotZ Global z rotation of physicalVehicle
     * @return PhysicalVehicleBuilder singleton class
     *
     */
    public PhysicalVehicleBuilder setGlobalRotation(double rotX, double rotY, double rotZ) {
        Log.finest("PhysicalVehicleBuilder: setGlobalRotation - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.setGlobalRotation(rotX, rotY, rotZ);
        Log.finest("PhysicalVehicleBuilder: setGlobalRotation - PhysicalVehicle at end: " + physicalVehicle);
        return getInstance();
    }

    /**
     * Function that sets actuator properties to the physicalVehicle that is currently built in the builder class
     *
     * @param actuatorType Type of the actuator
     * @param actuatorValueMin Minimum allowed value of the actuator
     * @param actuatorValueMax Maximum allowed value of the actuator
     * @param actuatorChangeRate Change rate of the actuator
     * @return PhysicalVehicleBuilder singleton class
     *
     */
    public PhysicalVehicleBuilder setActuatorProperties(VehicleActuatorType actuatorType, double actuatorValueMin, double actuatorValueMax, double actuatorChangeRate) {
        Log.finest("PhysicalVehicleBuilder: setActuatorProperties - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.getSimulationVehicle().setActuatorProperties(actuatorType, actuatorValueMin, actuatorValueMax, actuatorChangeRate);
        Log.finest("PhysicalVehicleBuilder: setActuatorProperties - PhysicalVehicle at end: " + physicalVehicle);
        return getInstance();
    }

    /**
     * Function that sets wheel properties to the physicalVehicle that is currently built in the builder class
     *
     * @param massFront Sum of mass for both front wheels
     * @param massBack Sum of mass for both back wheels
     * @param wheelRadius Radius of wheels
     * @param wheelDistLeftRight Distance between left and right wheels
     * @param wheelDistFrontBack Distance between front and back wheels
     * @return PhysicalVehicleBuilder singleton class
     *
     */
    public PhysicalVehicleBuilder setWheelProperties(double massFront, double massBack, double wheelRadius, double wheelDistLeftRight, double wheelDistFrontBack) {
        Log.finest("PhysicalVehicleBuilder: setWheelProperties - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.getSimulationVehicle().setWheelProperties(massFront, massBack, wheelRadius, wheelDistLeftRight, wheelDistFrontBack);
        Log.finest("PhysicalVehicleBuilder: setWheelProperties - PhysicalVehicle at end: " + physicalVehicle);
        return getInstance();
    }

    /**
     * Function that sets the max approximate velocity to the physicalVehicle that is currently built in the builder class
     *
     * @param approxMaxTotalVelocity Maximum approximate velocity of the vehicle
     * @return PhysicalVehicleBuilder singleton class
     *
     */
    public PhysicalVehicleBuilder setApproxMaxTotalVelocity(double approxMaxTotalVelocity) {
        Log.finest("PhysicalVehicleBuilder: setApproxMaxTotalVelocity - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.getSimulationVehicle().setApproxMaxTotalVelocity(approxMaxTotalVelocity);
        Log.finest("PhysicalVehicleBuilder: setApproxMaxTotalVelocity - PhysicalVehicle at end: " + physicalVehicle);
        return getInstance();
    }

    /**
     * Function that sets the physical object type to the physicalVehicle that is currently built in the builder class
     *
     * @param physicalObjectType New physical object type of the physical vehicle
     * @return PhysicalVehicleBuilder singleton class
     *
     */
    public PhysicalVehicleBuilder setPhysicalObjectType(PhysicalObjectType physicalObjectType) {
        Log.finest("PhysicalVehicleBuilder: setPhysicalObjectType - PhysicalVehicle at start: " + physicalVehicle);
        physicalVehicle.setPhysicalObjectType(physicalObjectType);
        Log.finest("PhysicalVehicleBuilder: setPhysicalObjectType - PhysicalVehicle at end: " + physicalVehicle);
        return getInstance();
    }

    /**
     * Encapsulation class for all file-parsable data concerning a car.
     * Has to be used as a helper object, as plain serialization methods either fail
     * or are not human readable.
     * The values are just a one to one "copy" of the properties configurable in the builder
     * to be able to reuse the building process as is.
     *
     *
     */
    public static class ParsableVehicleProperties {

        private double width;
        private double height;
        private double length;

        private double approxMaxTotalVelocity;

        private double posX;
        private double posY;
        private double posZ;

        private double rotX;
        private double rotY;
        private double rotZ;

        private double massFront;
        private double massBack;
        private double wheelRadius;
        private double wheelDistLeftRight;
        private double wheelDistFrontBack;

        private ArrayList<VehicleActuator> actuators = new ArrayList<>();


        private PhysicalObjectType type;

        public ParsableVehicleProperties(PhysicalVehicle v) {


            width = v.getSimulationVehicle().getWidth();
            height = v.getSimulationVehicle().getHeight();
            length = v.getSimulationVehicle().getLength();

            approxMaxTotalVelocity = v.getSimulationVehicle().getApproxMaxTotalVelocity();

            posX = v.getPos().getEntry(0);
            posY = v.getPos().getEntry(1);
            posZ = v.getPos().getEntry(2);

            massFront = v.getSimulationVehicle().getWheelMassPoints()[0].getMass() + v.getSimulationVehicle().getWheelMassPoints()[1].getMass();
            massBack = v.getSimulationVehicle().getWheelMassPoints()[2].getMass() + v.getSimulationVehicle().getWheelMassPoints()[3].getMass();
            wheelRadius = v.getSimulationVehicle().getWheelRadius();

            wheelDistLeftRight = v.getSimulationVehicle().getWheelDistLeftRight();

            wheelDistFrontBack = v.getSimulationVehicle().getWheelDistFrontBack();

            type = v.getPhysicalObjectType();
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public double getLength() {
            return length;
        }

        public double getApproxMaxTotalVelocity() {
            return approxMaxTotalVelocity;
        }

        public double getPosX() {
            return posX;
        }

        public double getPosY() {
            return posY;
        }

        public double getPosZ() {
            return posZ;
        }

        public double getRotX() {
            return rotX;
        }

        public double getRotY() {
            return rotY;
        }

        public double getRotZ() {
            return rotZ;
        }

        public double getMassFront() {
            return massFront;
        }

        public double getMassBack() {
            return massBack;
        }

        public double getWheelRadius() {
            return wheelRadius;
        }

        public double getWheelDistLeftRight() {
            return wheelDistLeftRight;
        }

        public double getWheelDistFrontBack() {
            return wheelDistFrontBack;
        }

        public ArrayList<VehicleActuator> getActuators() {
            return actuators;
        }

        public PhysicalObjectType getType() {
            return type;
        }

        public void setType(PhysicalObjectType type) {
            this.type = type;
        }

    }
}
