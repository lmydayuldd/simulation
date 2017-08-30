package sensors;

import commons.computervision.DetectedObject;
import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Marius on 17.05.2017.
 */
public class ObjectSensor extends AbstractSensor {
    private DetectedObject value=null;
    public ObjectSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_OBJECT;
    }

    @Override
    public DetectedObject getValue() {
        return value;
    }

    @Override
    public String getTypeName() {
        return DetectedObject.class.getTypeName();
    }

    @Override
    protected void calculateValue() {
        //TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }
}


