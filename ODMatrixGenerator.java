package OD;

import java.io.FileWriter;
import java.io.IOException;

import com.opencsv.CSVWriter;

import routing.algorithm.TransitGraph;

public class ODMatrixGenerator {

    private final TransitGraph graph;
    private final double[][] populationData;
    private final double[][] distances;
    public final String filename = "ODMatrix.csv";
    private final double ddp = 0.15; // distance decay parameter
    public double totalTrips;
    public double attraction;

    public ODMatrixGenerator(TransitGraph graph, double[][] populationData) {
        this.graph = graph;
        this.populationData = populationData;
        this.distances = calculateDistances();
    }

    private double[][] calculateDistances() {
        int n = graph.getNumVertices();
        double[][] dist = new double[n][n];

        // calcualte distances for all the different vertex pairs
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    double[] coordI = graph.getStopCoordinates(i);
                    double[] coordJ = graph.getStopCoordinates(j);
                    dist[i][j] = haversineDistacne(coordI[0], coordI[1], coordJ[0], coordJ[1]);
                }
            }
        }

        return dist;

    }

    private double haversineDistacne(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private double calculateAttraction(int origin, int destination) {
        double distance = distances[origin][destination];
        attraction = Math.exp(-ddp * distance);
        return attraction;
    }

    public double[][] generateODMatrix() {
        int n = graph.getNumVertices();
        double[][] ODMatrix = new double[n][n];

        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                if(i!=j) {
                    totalTrips += populationData[i][j];
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    double trips = populationData[i][j];
                    attraction = calculateAttraction(i, j);
                    ODMatrix[i][j] = (trips * attraction) / totalTrips;
                }
            }
        }
        return ODMatrix;
    }

    public void saveToCSV(String filename) {

        double[][] ODMatrix = generateODMatrix();
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            writer.writeNext(new String[] { "origin_id", "destination_id", "demand" });
            for (int i = 0; i < ODMatrix.length; i++) {
                for (int j = 0; j < ODMatrix[i].length; j++) {
                    writer.writeNext(

                            new String[] { String.valueOf(i), String.valueOf(j), String.valueOf(ODMatrix[i][j]) });
                }
            }
            System.out.println("OD Matrix was saved in : "+filename);
        } catch (IOException e) {                     
            e.printStackTrace();
        }
    }

    public double getTotalTrips() {
        return totalTrips;
    }

    public double getAttraction() {
        return attraction;
    }
}
