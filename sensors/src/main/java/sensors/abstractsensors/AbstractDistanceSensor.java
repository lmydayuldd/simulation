package sensors.abstractsensors;

import commons.simulation.PhysicalObject;
import org.apache.commons.math3.linear.RealVector;

import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Aklima Zaman on 2/8/2017.
 */
public abstract class AbstractDistanceSensor extends AbstractSensor {
    private Double value;

    public AbstractDistanceSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    protected void calculateValue() {
        RealVector pos = getPhysicalVehicle().getPos().copy();
        double x_axis = pos.getEntry(0);
        double y_axis = pos.getEntry(1);
        double z_axis = pos.getEntry(2); // we are ignoring z axis for now

        this.value = calculateDistance(getPhysicalVehicle());
    }

    protected abstract Double calculateDistance(PhysicalObject o);

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getTypeName() {
        return Double.class.getTypeName();
    }
}
