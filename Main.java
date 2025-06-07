package OD;

import routing.algorithm.TransitGraph;

public class Main {
    public static void main(String[] args) {
        // Process GTFS data
        GTFSProcessor processor = new GTFSProcessor("gtfsNL/amsterdam");
        TransitGraph graph = processor.getGraph();
        double[][] populationData = processor.getPopulationData();

        // Generate OD Matrix
        ODMatrixGenerator odGenerator = new ODMatrixGenerator(graph, populationData);
        System.out.println("OD Matrix has been generated");

        // Calculate Edge Flows
        EdgeFlowCalculator flowCalculator = new EdgeFlowCalculator(graph, odGenerator.generateODMatrix(),
                odGenerator.getTotalTrips());
        System.out.println("Edge Flows have been generated");

        System.out.println("=== CSV Files Are Being Written ===");
        // Write the CSV files
        odGenerator.saveToCSV("ODMatrix.csv");
        flowCalculator.saveToCSV("EdgeFlow.csv");

        System.out.println("=== Procedure Is Over ===");
    }
}