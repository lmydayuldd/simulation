package sensors;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import commons.simulation.Sensor;
import sensors.SpeedSensor;
import simulation.vehicle.PhysicalVehicle;
import simulation.vehicle.PhysicalVehicleBuilder;

/**
 * Created by Aklima Zaman on 19-Dec-16.
 */
public class SpeedSensorTest {

    @Test
    public void SpeedSensorTest() {
        PhysicalVehicleBuilder physicalVehicleBuilder = PhysicalVehicleBuilder.getInstance();
        PhysicalVehicle physicalVehicle = physicalVehicleBuilder.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        Sensor speedSensor = new SpeedSensor(physicalVehicle);

        assertTrue(!physicalVehicle.getVelocity().equals(speedSensor.getValue()));

    }


}
