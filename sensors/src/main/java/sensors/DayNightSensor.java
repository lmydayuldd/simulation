package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Johannes on 12.09.2017.
 */
public class DayNightSensor extends AbstractSensor {
    public enum Daytime{
        Day,Night
    }

    private Daytime value;

    public DayNightSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    protected void calculateValue() {
        this.value = Daytime.Day;
    }

    @Override
    public Daytime getValue() {
        return this.value;
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_DAYNIGHT;
    }

    @Override
    public String getTypeName() {
        return Daytime.class.getTypeName();
    }
}