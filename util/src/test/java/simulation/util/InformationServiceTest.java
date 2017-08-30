package simulation.util;

import org.junit.*;

import static org.junit.Assert.assertTrue;


/**
 * Unit tests for Information Service
 */
public class InformationServiceTest {

    @BeforeClass
    public static void setUpClass() {
        Log.setLogEnabled(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Log.setLogEnabled(true);
    }

    @Test
    public void stopProviding() {
        //Provide some information
        TestProvider provider = new TestProvider();
        InformationService.getSharedInstance().offerInformation("Test", provider::provide);

        //Verify offering works
        assertTrue((Integer)InformationService.getSharedInstance().requestInformation("Test") == 0);

        //Stop providing
        InformationService.getSharedInstance().offerInformation("Test", null);

        //Verify offering does not work anymore
        assertTrue((Integer)InformationService.getSharedInstance().requestInformation("Test") == null);

    }

    @Test
    public void offerAndRequestInformation() {
        //Cannot receive anything before setting it up
        assertTrue(null == InformationService.getSharedInstance().requestInformation("Test"));

        //Provide some information
        TestProvider provider = new TestProvider();
        InformationService.getSharedInstance().offerInformation("Test", provider::provide);

        //Try to request it, it should change with every try
        assertTrue((Integer)InformationService.getSharedInstance().requestInformation("Test") == 0);
        assertTrue((Integer)InformationService.getSharedInstance().requestInformation("Test") == 1);
        assertTrue((Integer)InformationService.getSharedInstance().requestInformation("Test") == 2);
    }

    private class TestProvider {
        public Integer information = 0;
        public Integer provide () {
            return this.information++;
        }
    }
}
