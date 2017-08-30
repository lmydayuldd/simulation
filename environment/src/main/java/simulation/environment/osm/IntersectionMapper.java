package simulation.environment.osm;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import gnu.trove.map.TLongObjectMap;
import simulation.environment.visualisationadapter.implementation.Intersection2D;
import simulation.environment.visualisationadapter.interfaces.EnvIntersection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by lukas on 15.12.16.
 * Maps each OsmWay to a set of EnvIntersections and a set of OsmNodes
 */
public class IntersectionMapper {

    HashMap<OsmWay, Set<EnvIntersection>> wayToIntersection;
    HashMap<OsmWay, Set<OsmNode>> wayToIntersectionOsm;

    InMemoryMapDataSet dataSet;
    Set<OsmNode> intersections;


    public IntersectionMapper(InMemoryMapDataSet data, Set<OsmNode> intersections) {
            this.wayToIntersection = new HashMap<>();
            this.wayToIntersectionOsm = new HashMap<>();
            this.dataSet = data;
            this.intersections = intersections;
            map();
    }

    /**
     * check for each node in all ways if it is an intersection and put it in the set of intersections
     */
    private void map() {
        if(dataSet == null || intersections == null) {
            return;
        }

        TLongObjectMap<OsmNode> nodes = dataSet.getNodes();
        for(OsmWay way : dataSet.getWays().valueCollection()) {
            Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
            String highway = tags.get("highway");
            //filter buildings
            if(highway != null) {
                for(int i = 0; i < way.getNumberOfNodes(); i++) {
                    OsmNode n = nodes.get(way.getNodeId(i));
                    if(intersections.contains(n)) {
                        if(!wayToIntersection.containsKey(way)) {
                            wayToIntersection.put(way, new HashSet<EnvIntersection>());
                        }

                        wayToIntersection.get(way).add(new Intersection2D(n.getLongitude(), n.getLatitude(), n.getId()));
                    }


                    if(intersections.contains(n)) {
                        if(!wayToIntersectionOsm.containsKey(way)) {
                            wayToIntersectionOsm.put(way, new HashSet<OsmNode>());
                        }

                        wayToIntersectionOsm.get(way).add(n);
                    }
                }
            }
        }


    }

    public Set<EnvIntersection> getIntersectionsForWay(OsmWay w) {
        return wayToIntersection.get(w);
    }

    public Set<OsmNode> getOsmIntersectionsForWay(OsmWay w) { return wayToIntersectionOsm.get(w);}

}
