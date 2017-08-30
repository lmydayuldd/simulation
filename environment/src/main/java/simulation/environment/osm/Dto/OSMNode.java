package simulation.environment.osm.Dto;

import java.util.Map;

/**
 * Created by Julian on 21.05.2017.
 */
public class OSMNode {
    public OSMNode(String Id, String Latitude, String Longitude, String Version, Map<String,String>Tags)
    {
        id = Id;
        lat = Latitude;
        lon = Longitude;
        version = Version;
        tags = Tags;
    }


    private String id;

    private String lat;

    private String lon;

    private final Map<String, String> tags;

    private String version;

}

