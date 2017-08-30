package sensors.util;

import commons.controller.commons.BusEntry;
import sensors.factory.SensorFactory;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Aklima Zaman on 2/15/2017.
 */
public class SensorUtil {
    public static PhysicalVehicle sensorAdder(PhysicalVehicle physicalVehicle) {

        SensorFactory sensorFactory = new SensorFactory(physicalVehicle);

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_VELOCITY));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_GPS_COORDINATES));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_DISTANCE_TO_RIGHT));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_DISTANCE_TO_LEFT));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_STEERING));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_WEATHER));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_CAMERA));
        
        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_COMPASS));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_LEFT_FRONT_WHEEL_DISTANCE_TO_STREET_SENSOR));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_RIGHT_FRONT_WHEEL_DISTANCE_TO_STREET_SENSOR));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_RIGHT_BACK_WHEEL_DISTANCE_TO_STREET_SENSOR));

        physicalVehicle.getSimulationVehicle().addSensor(sensorFactory.getSensor(BusEntry.SENSOR_LEFT_BACK_WHEEL_DISTANCE_TO_STREET_SENSOR));
        return physicalVehicle;
    }
}
