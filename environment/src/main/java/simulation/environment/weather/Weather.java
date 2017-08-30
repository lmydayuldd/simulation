package simulation.environment.weather;

import commons.simulation.SimulationLoopExecutable;

import java.util.Random;

/**
 * Created by lukas on 02.02.17.
 *
 * This class represents the Weather in the Simulation
 *
 * The weather is represented as double between 0 and 1.
 * Every value > 0.5 represents rain and sunshine otherwise
 *
 * Weather can be initialised random. The weather changes randomly at random timesteps.
 * Time for the weather change is computed using an exponential distribution
 *
 * Fixed weather changes can be specified to in the WeatherSettings. The weather changes than in fixed timesteps
 *
 * Another possibility is to initialise fixed weather. The weather changes never if this option is specified
 */
public class Weather implements SimulationLoopExecutable{

    public static final double SUNSHINE = 0;
    public static final double RAIN = 1;

    private double weather;
    private double nextWeatherChange;


    private final double FIXED_WEATHER_CHANGE = 0.5;
    private final double MEAN_OF_EXPONENTIAL  = 1/300000d; //corresponds to 5 minutes

    private Random weatherRandom;
    private Random timeRandom;

    private double fixedWeatherChange;

    public Weather(WeatherSettings settings) {
        if(settings.fixedWeatherChanges != -1) {
            init(settings.fixedWeatherChanges);
        } else if(settings.fixedWeather != -1) {
            init(settings.fixedWeather);
        } else {
            init();
        }
    }

    /**
     * init the weather completely random
     */
    private void init() {
        this.weatherRandom = new Random();
        this.timeRandom = new Random();
        initWeather();
    }

    /**
     * @param fixedWeatherChanges weather will change every fixedWeatherChanges
     */
    private void init(long fixedWeatherChanges) {
        this.nextWeatherChange = fixedWeatherChanges;
        this.fixedWeatherChange = fixedWeatherChanges;
        this.weatherRandom = new Random();
        initWeather();
    }

    /**
     * @param fixedWeather set the weather to the value of fixedWeather and don't change it
     */
    private void init(double fixedWeather) {
        this.weather = fixedWeather;
        this.timeRandom = new Random();
        initWeather();
    }

    /**
     * compute the next weather and time to the next weather change
     */
    private void initWeather() {
        if(this.weatherRandom != null) {
            this.weather = weatherRandom.nextDouble();
        }

        if(this.timeRandom != null) {
            this.nextWeatherChange = getNextExpDouble();
        } else {
            this.nextWeatherChange = fixedWeatherChange;
        }
    }

    /**
     * @return the next exponentially distributed random number
     */
    private double getNextExpDouble() {
        return  Math.log(1-timeRandom.nextDouble())/(-MEAN_OF_EXPONENTIAL);
    }

    /**
     * @return true iff it is raining
     */
    public boolean isRain(){
        return this.weather > 0.5;
    }

    /**
     * @return return the time left to the next weather change
     */
    public double getNextWeatherChange() {
        return this.nextWeatherChange;
    }

    /**
     * @return the double representation of the weather
     */
    public double getWeather() {
        return this.weather;
    }

    @Override
    public void executeLoopIteration(long timeDiffMs) {
        this.nextWeatherChange -= timeDiffMs;

        if(this.nextWeatherChange <= 0) {

            initWeather();
        }
    }
}
