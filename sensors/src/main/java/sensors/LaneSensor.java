package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Marius on 17.05.2017.
 */
public class LaneSensor extends AbstractSensor {
    private Object value=null;
    public LaneSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_LANE;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getTypeName() {
        return Object.class.getTypeName();
    }

    @Override
    protected void calculateValue() {
        //TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }
}

