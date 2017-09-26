package sensors;

import commons.controller.commons.BusEntry;
import commons.simulation.PhysicalObject;
import sensors.abstractsensors.AbstractSensor;
import simulation.vehicle.PhysicalVehicle;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Marius on 12.09.2017.
 */
public class ObstacleSensor extends AbstractSensor {

    private List<PhysicalObject> result = Collections.synchronizedList(new LinkedList<>());
    private Object[] value = new Object[2];

    public ObstacleSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    protected void calculateValue() {
        result.clear();
        //TODO: result.addAll Collections.synchronizedList (Alle physikalischen Objekte in einer Liste brauchen wir)
        value[0] = Double.MAX_VALUE;
        for(PhysicalObject k : result){
            Double t = getPhysicalVehicle().getGeometryPos().getDistance(k.getGeometryPos());
            if(((Double) value[0]) < t) {
                value[0]=t;
                value[1]=k.getVelocity();
            }
        }
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_OBSTACLE;
    }

    @Override
    public String getTypeName() {
        return Double.class.getTypeName();
    }

    @Override
    public Object[] getValue() {
        return value;
    }
}