package simulation.environment.weather;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by lukas on 02.02.17.
 */
public class WeatherTest extends TestCase {
    public WeatherTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(WeatherTest.class);
    }


    public void testApp() throws Exception {
        Weather w = new Weather(new WeatherSettings(10l));

        double oldWeather = w.getWeather();
        w.executeLoopIteration(10l);
        assertFalse((oldWeather - w.getWeather() == 0));

        w = new Weather(new WeatherSettings(0.2));
        assertFalse(w.isRain());
        w.executeLoopIteration(Long.MAX_VALUE);
        assertFalse(w.isRain());
        assertEquals(0.2, w.getWeather());

        w = new Weather(new WeatherSettings());
        assertTrue(w.getNextWeatherChange() > 0);
    }
}
