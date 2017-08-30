package simulation.util;

import org.junit.*;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Notification Center
 */
public class NotificationCenterTest {

    @BeforeClass
    public static void setUpClass() {
        Log.setLogEnabled(false);
    }

    @AfterClass
    public static void tearDownClass() {
        Log.setLogEnabled(true);
    }

    @Test
    public void postNotificationWithContext () {
        TestReceiver rec = new TestReceiver();
        NotificationCenter.getSharedInstance().registerListener("Test", rec::receiveMsg, rec);

        //Assure that content is correct
        NotificationCenter.getSharedInstance().postNotification("Test", NotificationCenter.getSharedInstance());
        assertTrue(rec.lastNotificationContext == System.identityHashCode(NotificationCenter.getSharedInstance()));
    }

    @Test
    public void postNotificationToObject() {
        //Set up test receivers
        TestReceiver rec1 = new TestReceiver();
        TestReceiver rec2 = new TestReceiver();

        //Add only one of the receivers notification center
        NotificationCenter.getSharedInstance().registerListener("Test", rec1::receiveMsg, rec1);

        //Post notification
        NotificationCenter.getSharedInstance().postNotification("Test", null);

        //Only rec1 should have received the message
        assertTrue(rec1.timesReceived == 1);
        assertTrue(rec2.timesReceived == 0);

        //Change roles of receivers
        NotificationCenter.getSharedInstance().registerListener("Test", rec2::receiveMsg, rec1);
        NotificationCenter.getSharedInstance().removeListener("Test", rec1::receiveMsg, rec1);

        //Post notification
        NotificationCenter.getSharedInstance().postNotification("Test", null);

        //Now both receivers should have been informed exactly once
        assertTrue(rec1.timesReceived == 1);
        assertTrue(rec2.timesReceived == 1);

        //Message for wrong key should not be received
        NotificationCenter.getSharedInstance().postNotification("XYZ", null);
        assertTrue(rec1.timesReceived == 1);
        assertTrue(rec2.timesReceived == 1);
    }

    private class TestReceiver {
        public int timesReceived = 0;
        public int lastNotificationContext = 0;

        public void receiveMsg(Object o) {
            timesReceived++;
            lastNotificationContext = System.identityHashCode(o);
        }
    }
}


