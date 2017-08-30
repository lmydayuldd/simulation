package simulation.environment.geometry.height;

import simulation.environment.visualisationadapter.interfaces.EnvBounds;

/**
 * Created by lukas on 23.02.17.
 *
 * uses fixed slopes. This functionality has to be removed from the ConcentricCircleGenerator and put in here
 */
public class StaticHeightGenerator implements HeightGenerator {

    private ConcentricCircleGenerator generator;

    public StaticHeightGenerator(EnvBounds bounds) {
        ConcentricCircleGenerator.init(bounds,true);
        this.generator = ConcentricCircleGenerator.getInstance();
    }

    @Override
    public double getGround(double x, double y) {
        return generator.getGround(x,y);
    }

    @Override
    public double[][] toHeightMap() {
        return this.generator.toHeightMap();
    }
}
