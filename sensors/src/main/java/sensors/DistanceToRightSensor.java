package sensors;

import commons.controller.commons.BusEntry;
import commons.simulation.PhysicalObject;
import sensors.abstractsensors.AbstractDistanceSensor;
import org.apache.commons.math3.distribution.NormalDistribution;
import simulation.environment.World;
import simulation.environment.WorldModel;
import simulation.vehicle.PhysicalVehicle;

/**
 * Created by Zaman on 2/7/2017.
 */
public class DistanceToRightSensor extends AbstractDistanceSensor {

    public DistanceToRightSensor(PhysicalVehicle physicalVehicle) {
        super(physicalVehicle);
    }

    @Override
    protected Double calculateDistance(PhysicalObject o) {
        World world = WorldModel.getInstance();
        double calculatedValue = world.getDistanceToRightStreetBorder(o).doubleValue();
        NormalDistribution normalDistribution = new NormalDistribution(calculatedValue, 0.01);
        return calculatedValue;//new Double(normalDistribution.sample());
    }

    @Override
    public BusEntry getType() {
        return BusEntry.SENSOR_DISTANCE_TO_RIGHT;
    }


}
