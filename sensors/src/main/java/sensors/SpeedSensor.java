package sensors;

import commons.controller.commons.BusEntry;
import sensors.abstractsensors.AbstractSensor;
import org.apache.commons.math3.linear.RealVector;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Aklima Zaman on 18-Dec-16.
 */

public class SpeedSensor extends AbstractSensor {

    private Double value;

    public SpeedSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    /**
     * Calculated velocity cannot be negative
     */
    @Override
    protected void calculateValue() {
        RealVector velocity = getPhysicalVehicle().getVelocity().copy();
        double velocityValue = velocity.getNorm();
        //NormalDistribution normalDistribution = new NormalDistribution(velocityValue, 0.1);
        //velocityValue = normalDistribution.sample();
        while (velocityValue < 0) {
            //velocityValue = normalDistribution.sample();
        }

        this.value = new Double(velocityValue);
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_VELOCITY;
    }

    @Override
    public String getTypeName() {
        return Double.class.getTypeName();
    }
}
