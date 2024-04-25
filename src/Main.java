import bridges.base.GraphAdjList;
import bridges.base.GraphAdjMatrix;
import bridges.connect.Bridges;
import bridges.connect.DataSource;
import bridges.data_src_dependent.City;
import bridges.validation.RateLimitException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

public class Main {
    static double getDist(double lat1, double long1, double lat2, double long2) {
        //uses the haversine formula
        final int R = 6371000; // meters
        final double phi1 = Math.toRadians(lat1);
        final double phi2 = Math.toRadians(lat2);
        final double delPhi = Math.toRadians((lat2 - lat1));
        final double delLambda = Math.toRadians((long2 - long1));

        final double a = Math.sin(delPhi/2) * Math.sin(delPhi/2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(delLambda/2) * Math.sin(delLambda/2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c; //meters
    }
    public static void main(String[] args) throws IOException, RateLimitException {

        //Initalize bridges object
        Bridges bridges = new Bridges(21,"duckydee","348122572003");
        GraphAdjList<City, String, Double> graph = new GraphAdjList<>();
        bridges.setCoordSystemType("albersusa");
        bridges.setMapOverlay(true);

        //Collect the data
        DataSource ds = bridges.getDataSource();
        HashMap<String, String> params = new HashMap<>();
        params.put("min_pop", "100000");
        Vector<City> cities = ds.getUSCitiesData(params);

        //Plot all the points
        for (City city : cities){
            graph.addVertex(city,city.getCity());
            graph.getVertex(city).setLocation(city.getLongitude(),city.getLatitude());
        }

        //Get all the distances
        HashMap<HashMap<City, City>, Double> distances = new HashMap<>();

        for (City cityA : cities){
            for (City cityB : cities) {
                if (cityA != cityB) {
                    HashMap<City, City> pairing = new HashMap<>();
                    pairing.put(cityA,cityB);
                    distances.put(pairing, getDist(cityA.getLatitude(), cityA.getLongitude(), cityB.getLatitude(), cityB.getLongitude()));
                }
            }
        }



        //Visualize
        bridges.setDataStructure(graph);
        bridges.visualize();


    }
}