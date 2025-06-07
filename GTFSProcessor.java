package OD;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import routing.algorithm.TransitGraph;
import routing.algorithm.StopsCoordinateFinder;
import routing.algorithm.GtfsDataset;
import routing.model.DataStore;
import routing.model.Stop;
import routing.model.StopTime;
import routing.model.Trip;

public class GTFSProcessor {
    private final String gtfsPath;
    private TransitGraph graph;
    private Map<String, Integer> stopIdToIndex;
    private double[][] populationData;
    private DataStore dataStore;

    public GTFSProcessor(String gtfsPath) {
        System.out.println("Initializing GTFSProcessor with path: " + gtfsPath);
        this.gtfsPath = gtfsPath;
        this.stopIdToIndex = new HashMap<>();
        this.dataStore = DataStore.getInstance();
        processGTFS();
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        
        return result.toArray(new String[0]);
    }

    private void processGTFS() {
        System.out.println("\n=== Starting GTFS Processing ===");
        
        // First read stops to create vertices
        System.out.println("\nReading stops.txt...");
        int stopCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(gtfsPath + "/stops.txt"))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCSVLine(line);
                String stopId = parts[0];
                String stopName = parts[1];
                double lat = Double.parseDouble(parts[3]);
                double lon = Double.parseDouble(parts[4]);
                
                Stop stop = new Stop(stopId, stopName, lat, lon);
                dataStore.addStop(stop);
                stopCount++;
            }
            System.out.println("Successfully processed " + stopCount + " stops");
        } catch (IOException e) {
            System.err.println("Error reading stops.txt: " + e.getMessage());
            e.printStackTrace();
        }

        // Read trips
        System.out.println("\nReading trips.txt...");
        int tripCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(gtfsPath + "/trips.txt"))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCSVLine(line);
                String tripId = parts[2];
                String routeId = parts[0];
                String serviceId = parts[1];
                String tripHeadsign = parts[3];
                Trip trip = new Trip(tripId, routeId, serviceId, tripHeadsign);
                dataStore.addTrip(trip);
                tripCount++;
            }
            System.out.println("Successfully processed " + tripCount + " trips");
        } catch (IOException e) {
            System.err.println("Error reading trips.txt: " + e.getMessage());
            e.printStackTrace();
        }

        // Read stop times
        System.out.println("\nReading stop_times.txt...");
        int stopTimeCount = 0;
        int errorCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(gtfsPath + "/stop_times.txt"))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCSVLine(line);
                    if (parts.length >= 11) {
                        String tripId = parts[0];
                        int stopSequence = Integer.parseInt(parts[1]);
                        String stopId = parts[2];
                        String arrivalTime = parts[4];
                        String departureTime = parts[5];
                        StopTime stopTime = new StopTime(tripId, arrivalTime, departureTime, stopId, stopSequence);
                        dataStore.addStopTime(stopTime);
                        stopTimeCount++;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing line: " + line);
                    errorCount++;
                }
            }
            System.out.println("Successfully processed " + stopTimeCount + " stop times");
            if (errorCount > 0) {
                System.err.println("Encountered " + errorCount + " parsing errors");
            }
        } catch (IOException e) {
            System.err.println("Error reading stop_times.txt: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\nBuilding GtfsDataset...");
        // Build the GtfsDataset from our DataStore
        GtfsDataset dataset = GtfsDataset.build(dataStore);
        // Set the dataset in the DataStore
        dataStore.setDataset(dataset);
        System.out.println("GtfsDataset built successfully");

        System.out.println("\nCreating transit graph...");
        // Create graph with vertices using StopsCoordinateFinder
        StopsCoordinateFinder scf = new StopsCoordinateFinder();
        double[][] stopCoordinates = scf.getStopCoordinates();
        graph = new TransitGraph(stopCoordinates.length);
        System.out.println("Created graph with " + stopCoordinates.length + " vertices");

        // Create edges from stop times
        System.out.println("\nCreating edges from stop times...");
        Map<String, List<StopTime>> tripStopTimes = new HashMap<>();
        for (StopTime st : dataStore.getStopTimes().values()) {
            tripStopTimes.computeIfAbsent(st.getTripId(), k -> new ArrayList<>()).add(st);
        }

        int edgeCount = 0;
        for (List<StopTime> stopTimes : tripStopTimes.values()) {
            Collections.sort(stopTimes, (a, b) -> a.getStopSequence() - b.getStopSequence());
            for (int i = 0; i < stopTimes.size() - 1; i++) {
                StopTime from = stopTimes.get(i);
                StopTime to = stopTimes.get(i + 1);
                graph.addEdge(
                    dataStore.getDataset().getStopIdToIndex().get(from.getStopId()),
                    dataStore.getDataset().getStopIdToIndex().get(to.getStopId()),
                    1,
                    from.getTripId()
                );
                edgeCount++;
            }
        }
        System.out.println("Created " + edgeCount + " edges in the graph");

        System.out.println("\nInitializing population data...");
        populationData = new double[stopCoordinates.length][stopCoordinates.length];
        for (int i = 0; i < stopCoordinates.length; i++) {
            for (int j = 0; j < stopCoordinates.length; j++) {
                if (i != j) {
                    populationData[i][j] = 1.0; // Default value, should be replaced with actual data
                }
            }
        }
        System.out.println("Population data initialized with default values");

        System.out.println("\n=== GTFS Processing Completed ===");
    }

    public TransitGraph getGraph() {
        return graph;
    }

    public double[][] getPopulationData() {
        return populationData;
    }
} 