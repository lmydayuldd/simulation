package simulation.environment.geometry.height;

/**
 * Created by lukas on 23.02.17.
 *
 * A height Generator that returns always zero
 */
public class AllZeroGenerator implements HeightGenerator{
    @Override
    public double getGround(double x, double y) {
        return 0;
    }

    @Override
    public double[][] toHeightMap() {
        double[][] result = new double[3][1];
        result[0][0] = 0;
        result[1][0] = 0;
        result[2][0] = 0;
        return result;
    }
}
