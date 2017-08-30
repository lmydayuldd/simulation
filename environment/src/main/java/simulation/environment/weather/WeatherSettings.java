package simulation.environment.weather;

/**
 * Created by lukas on 02.02.17.
 *
 * A container for the WeatherSettings
 */
public class WeatherSettings {

    public long fixedWeatherChanges;

    public double fixedWeather;

    /**
     * call this constructor if you want to apply weather completely random
     */
    public WeatherSettings() {
        this.fixedWeatherChanges = -1;
        this.fixedWeather = -1;
    }

    /**
     * Call this constructor if you want to change the weather in fixed intervals
     * @param fixedWeatherChanges
     */
    public WeatherSettings(long fixedWeatherChanges) {
       this.fixedWeatherChanges = fixedWeatherChanges;
        this.fixedWeather = -1;
    }

    /**
     * Call this constructor if you don't want to change the weather
     * @param fixedWeather
     */
    public WeatherSettings(double fixedWeather) {
       this.fixedWeather = fixedWeather;
        this.fixedWeatherChanges = -1;
    }
}
