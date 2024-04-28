import bridges.base.GraphAdjList;
import bridges.base.GraphAdjMatrix;
import bridges.connect.Bridges;
import bridges.connect.DataSource;
import bridges.data_src_dependent.City;
import bridges.validation.RateLimitException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Main {

    public static void main(String[] args) throws IOException, RateLimitException {
        //Initalize bridges object
        Bridges bridges = new Bridges(21,"duckydee","348122572003");
        GraphAdjList<String, String, Double> graph = new GraphAdjList<>();
        bridges.setCoordSystemType("albersusa");
        bridges.setMapOverlay(true);

        //Collect the data
        DataSource ds = bridges.getDataSource();
        HashMap<String, String> params = new HashMap<>();
        params.put("min_pop", "300000");
        Vector<City> cities = ds.getUSCitiesData(params);


        //Plot all the points
        for (City city : cities){
            graph.addVertex(city.getCity(),city.getCity());
            graph.getVertex(city.getCity()).setLocation(city.getLongitude(),city.getLatitude());
            graph.getVertex(city.getCity()).setSize(1.0f);
        }

        //Generate all of the possible distances
        for (City A : cities){
            for (City B : cities){
                if (A!=B){
                    graph.addEdge(A.getCity(),B.getCity(),getDist(A.getLatitude(),A.getLongitude(),B.getLatitude(),B.getLongitude()));
                }
            }
        }

        //Get the MST
        HashMap<HashMap <String, String>,Double> MSTPath = Prim(graph);

        for (HashMap <String, String> x : MSTPath.keySet()){
            graph.addEdge(x.keySet().toArray(new String[0])[0],x.values().toArray(new String[0])[0],MSTPath.get(x));
        }


        //Visualize
        bridges.setDataStructure(graph);
        bridges.visualize();
    }
    static String minKey(HashMap <String, Double> keys, HashMap <String, Boolean> mstSet) {
        // Initialize min value
        double min = Double.MAX_VALUE;
        String min_index = "";

        for (Map.Entry<String, Double> key : keys.entrySet())
            if (!mstSet.get(key.getKey()) && key.getValue() < min) {
                min = key.getValue();
                min_index = key.getKey();
            }

        return min_index;
    }

    static HashMap<HashMap <String, String>,Double> Prim(GraphAdjList<String, String, Double> g){
        HashMap <String, String> parent = new HashMap<>();
        HashMap <String, Double> key = new HashMap<>();
        HashMap <String, Boolean> mstSet = new HashMap<>();

        //set default distance to infinity
        for (String x : g.getVertices().keySet()){
            key.put(x,Double.MAX_VALUE);
            mstSet.put(x,false);
        }
        //initialize first values
        key.replace("Chicago",0.0);
        parent.put("Chicago","");


        for (String ignored : g.getVertices().keySet()) {
            String u = minKey(key, mstSet);
            mstSet.replace(u,true);
            for (String v : g.getVertices().keySet()) {
                //Check if the city has been visited before
                if (!mstSet.get(v)) {
                    //Calculate Distance
                    double distance = getDist(g.getVertex(u).getLocationX(), g.getVertex(u).getLocationY(), g.getVertex(v).getLocationX(), g.getVertex(v).getLocationY());
                    //If it's the new shortest distance, update the parent hash and the distance
                    if (distance < key.get(v)) {
                        if (parent.replace(v, u) == null) {
                            parent.put(v, u);
                        } else {
                            parent.replace(v, u);
                        }
                        key.replace(v, distance);
                    }
                }
            }
        }
        //Remove the first entry because it's always null
        parent.remove("Chicago");

        //Format the data for output: HashMap<<StartCity : EndCity> : Distance>
        HashMap<HashMap <String, String>,Double> output = new HashMap<>();
        for (String x : parent.keySet()){
            HashMap<String,String> temp = new HashMap<>();
            temp.put(x,parent.get(x));
            output.put(temp,getDist(g.getVertex(x).getLocationX(),g.getVertex(x).getLocationY(),g.getVertex(parent.get(x)).getLocationX(),g.getVertex(parent.get(x)).getLocationY()));
        }
        printMST(output);
        return output;
    }

    public static void printMST(HashMap<HashMap <String, String>,Double> output)
    {
        System.out.println("Edge \t \tWeight");
        for (HashMap<String, String> edge : output.keySet()){
            System.out.println((edge.keySet().toArray(new String[0])[0])+" - "+(edge.values().toArray(new String[0])[0])+"\t \t"
                    +(output.get(edge)));
        }
    }

    public static double getDist(double lat1, double long1, double lat2, double long2) {
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
}
