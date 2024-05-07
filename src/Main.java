import bridges.base.Edge;
import bridges.base.GraphAdjList;
import bridges.connect.Bridges;
import bridges.connect.DataSource;
import bridges.data_src_dependent.City;
import bridges.validation.RateLimitException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class Main {
    public static GraphAdjList<String, String, Double> graph = new GraphAdjList<>();
    public static Vector<City> cities;
    public static HashMap<String, Integer> cityMap = new HashMap<>();

    public static void main(String[] args) throws IOException, RateLimitException {
        // Initialize Bridges object
        Bridges bridges = new Bridges(26, "rmirand1", "693627861258");

        bridges.setCoordSystemType("albersusa");
        bridges.setMapOverlay(true);
        HashMap<String, Boolean> visited = new HashMap<>();

        // Collect the data
        DataSource ds = bridges.getDataSource();
        HashMap<String, String> params = new HashMap<>();
        params.put("min_pop", "300000");
        cities = ds.getUSCitiesData(params);

        int counter = 0;
        // Plot all the points
        for (City city : cities) {
            cityMap.put(city.getCity(), counter);
            visited.put(city.getCity(), false);
            graph.addVertex(city.getCity(), city.getCity());
            graph.getVertex(city.getCity()).setLocation(city.getLongitude(), city.getLatitude());
            graph.getVertex(city.getCity()).setSize(1.0f);
            counter += 1;
        }

        // Generate all possible distances
        for (City A : cities) {
            for (City B : cities) {
                if (A != B) {
                    graph.addEdge(A.getCity(), B.getCity(), getDist(A.getLatitude(), A.getLongitude(), B.getLatitude(), B.getLongitude()));
                }
            }
        }

        // Get the MST
        HashMap<HashMap<String, String>, Double> MST = Prim(graph);

        GraphAdjList<String, String, Double> mstGraph = new GraphAdjList<>();
        for (String g : graph.getVertices().keySet()) {
            mstGraph.addVertex(g, g);
            mstGraph.getVertex(g).setLocation(graph.getVertex(g).getLocationX(), graph.getVertex(g).getLocationY());
        }
        for (HashMap<String, String> edge : MST.keySet()) {
            mstGraph.addEdge(edge.keySet().toArray(new String[0])[0], edge.values().toArray(new String[0])[0], MST.get(edge));
        }

        double[][] distanceMatrix = new double[graph.getVertices().size()][graph.getVertices().size()];

        // Initialize the Distance Matrix
        for (int k = 0; k < distanceMatrix.length; k++) {
            for (int i = 0; i < distanceMatrix.length; i++) {
                if (k == i) {
                    distanceMatrix[k][i] = 0.0;
                } else {
                    distanceMatrix[k][i] = Double.MAX_VALUE;
                }
            }
        }

        // Initialize the Pathing Matrix
        int[][] Next = new int[distanceMatrix.length][distanceMatrix.length];
        for (int k = 0; k < distanceMatrix.length; k++) {
            for (int i = 0; i < distanceMatrix.length; i++) {
                Next[i][k] = -1;
            }
        }

        // Floyd's Path Algorithm
        DecimalFormat df = new DecimalFormat("#.###");
        for (int k = 0; k < distanceMatrix.length; k++) {
            for (int i = 0; i < distanceMatrix.length; i++) {
                for (int j = 0; j < distanceMatrix.length; j++) {
                    if (distanceMatrix[i][k] + distanceMatrix[k][j] < distanceMatrix[i][j]) {
                        distanceMatrix[i][j] = Double.parseDouble(df.format(distanceMatrix[i][k] + distanceMatrix[k][j]));
                        Next[i][j] = Next[i][k];
                    }
                }
            }
        }

        // Perform DFS traversal on the MST
        List<String> tour = dfsTraversal(mstGraph);

        // Remove duplicate vertices from the tour
        Set<String> uniqueVertices = new LinkedHashSet<>(tour);
        tour.clear();
        tour.addAll(uniqueVertices);

        // Visualize the tour (highlight vertices and edges)
        visualizeTour(mstGraph, tour);
    }

    static String minKey(HashMap<String, Double> keys, HashMap<String, Boolean> mstSet) {
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

    static HashMap<HashMap<String, String>, Double> Prim(GraphAdjList<String, String, Double> g) {
        HashMap<String, String> parent = new HashMap<>();
        HashMap<String, Double> key = new HashMap<>();
        HashMap<String, Boolean> mstSet = new HashMap<>();

        // set default distance to infinity
        for (String x : g.getVertices().keySet()) {
            key.put(x, Double.MAX_VALUE);
            mstSet.put(x, false);
        }
        // initialize first values
        key.replace("Chicago", 0.0);
        parent.put("Chicago", "");

        for (String ignored : g.getVertices().keySet()) {
            String u = minKey(key, mstSet);
            mstSet.replace(u, true);
            for (String v : g.getVertices().keySet()) {
                // Check if the city has been visited before
                if (!mstSet.get(v)) {
                    // Calculate Distance
                    double distance = getDist(g.getVertex(u).getLocationX(), g.getVertex(u).getLocationY(), g.getVertex(v).getLocationX(), g.getVertex(v).getLocationY());
                    // If it's the new shortest distance, update the parent hash and the distance
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
        // Remove the first entry because it's always null
        parent.remove("Chicago");

        // Format the data for output: HashMap<<StartCity : EndCity> : Distance>
        HashMap<HashMap<String, String>, Double> output = new HashMap<>();
        for (String x : parent.keySet()) {
            HashMap<String, String> temp = new HashMap<>();
            temp.put(x, parent.get(x));
            output.put(temp, getDist(g.getVertex(x).getLocationX(), g.getVertex(x).getLocationY(), g.getVertex(parent.get(x)).getLocationX(), g.getVertex(parent.get(x)).getLocationY()));
        }
        printMST(output);
        return output;
    }

    static List<String> dfsTraversal(GraphAdjList<String, String, Double> g) {
        List<String> tour = new ArrayList<>();
        HashMap<String, Boolean> visited = new HashMap<>();

        for (String vertex : g.getVertices().keySet()) {
            visited.put(vertex, false);
        }

        // Start DFS traversal from a random vertex
        for (String vertex : g.getVertices().keySet()) {
            if (!visited.get(vertex)) {
                dfsVisit(vertex, g, visited, tour);
            }
        }

        return tour;
    }

    static void dfsVisit(String vertex, GraphAdjList<String, String, Double> g, HashMap<String, Boolean> visited, List<String> tour) {
        visited.replace(vertex, true);
        tour.add(vertex);

        for (Edge neighbor : g.outgoingEdgeSetOf(vertex)) {
            if (!visited.get(neighbor.getTo())) {
                dfsVisit(neighbor.getTo().toString(), g, visited, tour);
            }
        }
    }

    public static void printMST(HashMap<HashMap<String, String>, Double> output) {
        System.out.println("Edge \t \tWeight");
        for (HashMap<String, String> edge : output.keySet()) {
            System.out.println((edge.keySet().toArray(new String[0])[0]) + " - " + (edge.values().toArray(new String[0])[0]) + "\t \t"
                    + (output.get(edge)));
        }
    }

    public static double getDist(double lat1, double long1, double lat2, double long2) {
        // uses the haversine formula
        final int R = 6371000; // meters
        final double phi1 = Math.toRadians(lat1);
        final double phi2 = Math.toRadians(lat2);
        final double delPhi = Math.toRadians((lat2 - lat1));
        final double delLambda = Math.toRadians((long2 - long1));

        final double a = Math.sin(delPhi / 2) * Math.sin(delPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(delLambda / 2) * Math.sin(delLambda / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // meters
    }

    static void visualizeTour(GraphAdjList<String, String, Double> graph, List<String> tour) throws RateLimitException, IOException {
        // Highlight vertices and edges based on the tour
        for (int i = 0; i < tour.size() - 1; i++) {
            graph.addEdge(tour.get(i), tour.get(i + 1));
            graph.getVertex(tour.get(i)).setColor("red");
        }
        graph.addEdge(tour.getLast(), tour.getFirst());
        graph.getVertex(tour.getLast()).setColor("red");

        // Visualize the tour
        Bridges bridges = new Bridges(21, "duckydee", "348122572003");
        bridges.setCoordSystemType("albersusa");
        bridges.setMapOverlay(true);
        bridges.setDataStructure(graph);
        bridges.visualize();
    }
}
