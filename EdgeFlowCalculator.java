package OD;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.opencsv.CSVWriter;
import routing.algorithm.TransitGraph;

public class EdgeFlowCalculator {

    private final TransitGraph graph;
    private final double[][] ODMatrix;

    public EdgeFlowCalculator(TransitGraph graph, double[][] ODMatrix, double totalTrips) {
        this.graph = graph;
        this.ODMatrix = ODMatrix;
    }

    public Map<TransitGraph.Edge, Double> calculateEdgeFlows() {
        long startTime = System.currentTimeMillis();
        Map<TransitGraph.Edge, Double> edgeFlows = new HashMap<>();
        int n = graph.getNumVertices();

        int processedPairs = 0;
        long totalPairs = (long)n * n;  
        long lastProgress = 0;

        for (int origin = 0; origin < n; origin++) {
            for (int destination = 0; destination < n; destination++) {
                if (origin != destination) {
                    double demand = ODMatrix[origin][destination];
                    demand = demand * 10000000;
                    TransitGraph.Edge edge = new TransitGraph.Edge(origin, destination, 1, "OD_FLOW");
                    edgeFlows.put(edge, demand);
                    processedPairs++;

                    // Show progress every 1000 pairs
                    if (processedPairs - lastProgress >= 1000) {
                        double progress = (processedPairs * 100.0) / totalPairs;
                        System.out.printf("Progress: %.2f%% (%d/%d pairs processed)\n", 
                            progress, processedPairs, totalPairs);
                        lastProgress = processedPairs;
                    }
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Edge flow calculation completed in %.1f minutes!\n", totalTime / 60000.0);
        return edgeFlows;
    }

    public void saveToCSV(String filename) {
        Map<TransitGraph.Edge, Double> edgeFlows = calculateEdgeFlows();
        int edgeID = 0;

        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            writer.writeNext(new String[] { "edge_id", "from_stop", "to_stop", "flow" });

            for (Map.Entry<TransitGraph.Edge, Double> entry : edgeFlows.entrySet()) {
                TransitGraph.Edge edge = entry.getKey();
                double flow = entry.getValue();
                writer.writeNext(new String[] {
                        String.valueOf(edgeID++),
                        String.valueOf(edge.getFrom()),
                        String.valueOf(edge.getTo()),
                        String.format("%.3f", flow) // Format with 9 decimal places
                });
            }
            System.out.println("Edge Flows were saved in: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing edge flows to CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
