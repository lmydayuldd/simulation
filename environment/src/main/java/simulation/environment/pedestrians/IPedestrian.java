package simulation.environment.pedestrians;

/**
 * Created by lukas on 13.02.17.
 *
 * An interface that specifies a getter and a setter for the Parameters needed in the movement computation
 */
public interface IPedestrian {
    public abstract PedestrianStreetParameters getStreetParameters();

    public abstract void setStreetParameters(PedestrianStreetParameters newParameters);
}
