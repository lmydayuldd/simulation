package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Marius on 17.05.2017.
 */

// Deprecated because computervision was removed
@Deprecated
public class ObjectSensor extends AbstractSensor {
    private Object value=null;
    public ObjectSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_OBJECT;
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


