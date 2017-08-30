package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import org.apache.commons.math3.linear.RealVector;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Aklima Zaman on 20-Jan-17.
 */
public class LocationSensor extends AbstractSensor {

    private RealVector value;

    public LocationSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);

    }

    @Override
    public RealVector getValue() {
        return this.value;
    }

    @Override
    protected void calculateValue() {
        RealVector pos = getPhysicalVehicle().getPos().copy();
        double x_axis = pos.getEntry(0);
        double y_axis = pos.getEntry(1);
        double z_axis = pos.getEntry(2); // we are ignoring z axis for now

        //NormalDistribution normalDistribution_x = new NormalDistribution(x_axis, 0.1);
        //x_axis = normalDistribution_x.sample();

        //NormalDistribution normalDistribution_y = new NormalDistribution(y_axis, 0.1);
        //y_axis = normalDistribution_y.sample();

        pos.setEntry(0, x_axis);
        pos.setEntry(1, y_axis);
        pos.setEntry(2, z_axis);

        this.value = pos;

    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_GPS_COORDINATES;
    }

    @Override
    public String getTypeName() {
        return RealVector.class.getTypeName();
    }
}
