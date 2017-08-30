package simulation.environment.osm;

import java.io.InputStream;

/**
 * Created by lukas on 16.02.17.
 *
 * A container that contains the input stream and the strategy to generate z-Coordinates
 */
public class ParserSettings {
    public enum ZCoordinates {
        ALLZERO, RANDOM, STATIC;
    }

    public InputStream in;
    public ZCoordinates z;

    public ParserSettings(String in, ZCoordinates z) {
        this.in = getClass().getResourceAsStream(in);
        this.z = z;
    }

    @Deprecated
    public ParserSettings(InputStream in, ZCoordinates z) {
        this.in = in;
        this.z = z;
    }
}
