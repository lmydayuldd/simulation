package sensors.abstractsensors;

import commons.simulation.Sensor;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Aklima Zaman on 1/20/2017.
 */
public abstract class AbstractSensor implements Sensor {

    private PhysicalVehicle physicalVehicle;

    public AbstractSensor(PhysicalVehicle physicalVehicle) {
        this.physicalVehicle = physicalVehicle;
    }


    @Override
    public void update() {
        calculateValue();
    }

    public PhysicalVehicle getPhysicalVehicle() {
        return this.physicalVehicle;
    }

    /**
     * This method do the sensor calculations
     */
    protected abstract void calculateValue();

}
