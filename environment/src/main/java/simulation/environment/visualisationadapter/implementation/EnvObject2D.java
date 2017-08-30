package simulation.environment.visualisationadapter.implementation;

import simulation.environment.visualisationadapter.interfaces.EnvNode;
import simulation.environment.visualisationadapter.interfaces.EnvObject;
import simulation.environment.visualisationadapter.interfaces.EnvTag;

import java.util.List;

/**
 * Created by lukas on 08.01.17.
 *
 * A superclass for all EnvironmentObjects
 */
public class EnvObject2D implements EnvObject {

    protected List<EnvNode> nodes;
    protected EnvTag tag;

    protected long osmId;


    public EnvObject2D(List<EnvNode> nodes, EnvTag tag) {
        this.nodes = nodes;
        this.tag = tag;
    }

    public EnvObject2D(List<EnvNode> nodes, EnvTag tag, long osmId) {
        this(nodes, tag);
        this.osmId = osmId;
    }

    @Override
    public List<EnvNode> getNodes() {
        return this.nodes;
    }

    @Override
    public EnvTag getTag() {
        return this.tag;
    }

    public long getOsmId() {
        return this.osmId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnvObject2D that = (EnvObject2D) o;

        if (getOsmId() != that.getOsmId()) return false;
        if (!getNodes().equals(that.getNodes())) return false;
        return getTag() == that.getTag();

    }

    @Override
    public int hashCode() {
        int result = getNodes().hashCode();
        result = 31 * result + getTag().hashCode();
        result = 31 * result + (int) (getOsmId() ^ (getOsmId() >>> 32));
        return result;
    }
}
