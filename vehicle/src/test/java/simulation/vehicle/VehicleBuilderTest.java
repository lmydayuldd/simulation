package simulation.vehicle;


import com.google.gson.Gson;
import org.junit.*;
import simulation.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by samuel on 15.12.16.
 */
public class VehicleBuilderTest {

    private PhysicalVehicleBuilder.ParsableVehicleProperties carProps;
    private String testFile = "car_test.json";
    private File testFileAsFile;


    @BeforeClass
    public static void setUpClass() {
        Log.setLogEnabled(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Log.setLogEnabled(true);
    }

    /**
     * In this setup method a valid JSON car file and String is created, which is later on used.
     * The created file is registered for deletion on VM exit.
     */
    @Before
    public void setUp() {
        PhysicalVehicle v = PhysicalVehicleBuilder.getInstance().buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        carProps = new PhysicalVehicleBuilder.ParsableVehicleProperties(v);

        testFileAsFile = new File(testFile);
        testFileAsFile.deleteOnExit();
    }

    /**
     * Testing the loading a JSON serialized car from a file and construct the object using the @{@link PhysicalVehicleBuilder}.
     *
     * @throws IOException
     */
    @Test
    public void testLoadPropertiesFromFile() throws IOException {

        Gson g = new Gson();
        String json = g.toJson(carProps, PhysicalVehicleBuilder.ParsableVehicleProperties.class);

        FileWriter fooWriter = new FileWriter(testFile, false);
        fooWriter.write(json);
        fooWriter.flush();
        fooWriter.close();

        PhysicalVehicleBuilder b = PhysicalVehicleBuilder.getInstance();

        b.loadPropertiesFromFile(new File(testFile));

        PhysicalVehicle v = b.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());

    }

    /**
     * Testing loading a car from a valid JSON String. This is also used in the loadFromFile method.
     */
    @Test
    public void testLoadPropertiesFromJSON() {

        Gson g = new Gson();
        String json = g.toJson(carProps, PhysicalVehicleBuilder.ParsableVehicleProperties.class);

        PhysicalVehicleBuilder b = PhysicalVehicleBuilder.getInstance();
        b.loadPropertiesFromJSON(json);

        PhysicalVehicle v = b.buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        checkTheCar(v);

    }


    /**
     * Testing storing a car in a JSON file.
     * This is done by first storing it in a file and then load it again and check if all properties remained the same.
     */
    @Test
    public void testStoreJSONInFile() throws IOException {
        PhysicalVehicleBuilder.getInstance().storeJSONInFile(testFileAsFile);
        PhysicalVehicleBuilder.getInstance().loadPropertiesFromFile(testFileAsFile);
        PhysicalVehicle v = PhysicalVehicleBuilder.getInstance().buildPhysicalVehicle(Optional.empty(), Optional.empty(), Optional.empty());
        checkTheCar(v);
    }

    /**
     * Checks various vehicle properties and compares them with the initially created one, to assure correct loading.
     *
     * @param v a vehicle to check against the one created in the setup
     */
    private void checkTheCar(PhysicalVehicle v) {

        Assert.assertEquals(carProps.getHeight(), v.getSimulationVehicle().getHeight(), 0);
        Assert.assertEquals(carProps.getWidth(), v.getSimulationVehicle().getWidth(), 0);
        Assert.assertEquals(carProps.getLength(), v.getSimulationVehicle().getLength(), 0);
        Assert.assertEquals(carProps.getApproxMaxTotalVelocity(), v.getSimulationVehicle().getApproxMaxTotalVelocity(), 0);
        Assert.assertEquals(carProps.getWheelRadius(), v.getSimulationVehicle().getWheelRadius(), 0);
        Assert.assertEquals(carProps.getWheelDistFrontBack(), v.getSimulationVehicle().getWheelDistFrontBack(), 0);
        Assert.assertEquals(carProps.getWheelDistLeftRight(), v.getSimulationVehicle().getWheelDistLeftRight(), 0);
        Assert.assertEquals(carProps.getWheelDistLeftRight(), v.getSimulationVehicle().getWheelDistLeftRight(), 0);

        Assert.assertSame(carProps.getType(), v.getPhysicalObjectType());

    }

}
