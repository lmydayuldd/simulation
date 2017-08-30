package simulation.environment.osm;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

import java.util.*;

/**
 * Created by lukas on 15.12.16.
 * Determines all intersections for given imported OSM-Dataset
 */
public class IntersectionFinder {

    private Set<OsmNode> intersections = Collections.synchronizedSet(new HashSet<OsmNode>());

    private static IntersectionFinder instance = null;

    private IntersectionFinder() {}

    public static IntersectionFinder getInstance() {
        if (instance == null) {
            instance = new IntersectionFinder();
        }

        return instance;
    }

    /**
     * finds intersections by counting how often each node occurs in the dataset
     * Note: Streets can cross themselves
     *
     * @param dataSet
     */
    public void findIntersections(InMemoryMapDataSet dataSet) {
        if(dataSet == null) {
            return;
        }

        HashMap<OsmNode, Integer> nodes = new HashMap<OsmNode, Integer>();
        for(OsmWay way : dataSet.getWays().valueCollection()) {
            Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
            String highway = tags.get("highway");
            //filter buildings
            if(highway != null) {
                try{
                    countNodes(dataSet, way, nodes);
                    filterNonIntersections(nodes);
                } catch (EntityNotFoundException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }



    }

    /**
     * @param dataSet
     * @param way
     * @param nodes
     * @throws EntityNotFoundException
     * simply increments the counter in the nodes map for all nodes specified by way
     */
    private void countNodes(InMemoryMapDataSet dataSet, OsmWay way, HashMap<OsmNode, Integer> nodes) throws EntityNotFoundException {
        for(int i = 0; i < way.getNumberOfNodes(); i++) {
            OsmNode node = dataSet.getNode(way.getNodeId(i));

            if(nodes.containsKey(node)) {
                nodes.put(node, nodes.get(dataSet.getNode(way.getNodeId(i))) + 1);
            } else {
                nodes.put(node, 1);
            }

        }
    }

    /**
     * @param nodes
     * computes the final Set of intersections by adding all nodes with a higher count than one
     */
    private void filterNonIntersections(HashMap<OsmNode, Integer> nodes) {
        this.intersections = new HashSet<OsmNode>();
        for(OsmNode node : nodes.keySet()) {
            if(nodes.get(node) > 1) {
                intersections.add(node);
            }
        }
    }

    public int getNumberOfIntersections() {
        return this.intersections.size();
    }

    public Set<OsmNode> getIntersections(){
        return Collections.synchronizedSet(new HashSet<OsmNode>(this.intersections));
    }

}
