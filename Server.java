/*
 *   <Temperarture Server>
 * ** UPDATED server CUZ IM DONE!**
 * 
 *   <Group Members>                 Section: 02
 *
 * 
 * [NOTES + CHANGES]:
 * i used streams() instead of loops for better effiency 
 * and smoothess + shorter than loops
 * for command calculations
 * 
 * + i changed some naming conventions just in case of plagiarism with others
 * + added seperate methods for managing each command ( makes it easier to track down ) 
 * + 
 * 
 */

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server{

    private static final int LISTENING_PORT = 12352; // server port
    private static final double WARNING_THRESHOLD = 30.0; // max threshold
    private static  List<Socket> activeClients = Collections.synchronizedList(new ArrayList<>()); // to store clients
    private static PrintWriter logger; // writes to log file
    private static List<Double> temperatureData = Collections.synchronizedList(new ArrayList<>()); // to store temp
                                                                                                   // readings
    

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(LISTENING_PORT)) { // new server socket just for accepting requests
            logger = new PrintWriter(new FileWriter("server_activity.log", false), true); // log file
            logActivity("Server Ready and waiting for connections...");

            while (true) { // loop to keep server running
                Socket client = server.accept(); // server listens for connectins/requests from clients
                activeClients.add(client);
                logActivity("client connected: " + client.getRemoteSocketAddress());
                new Thread(new ClientRequestHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientRequestHandler implements Runnable { // manages each client indivually

        private Socket clientSocket;
        private BufferedReader inputReader;
        private PrintWriter outputWriter;

        public ClientRequestHandler(Socket client) {
            this.clientSocket = client;
        }

        @Override
        public void run() {
            try {
                inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // receive/read from server
                outputWriter = new PrintWriter(clientSocket.getOutputStream(), true); // send/write to server
        
                String clientCommand;
                while ((clientCommand = inputReader.readLine()) != null) {
                    processClientCommand(clientCommand);
                }
            } catch (IOException e) {
                logActivity("Client Disconnected" + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    activeClients.remove(clientSocket);
                } catch (IOException e) {
                    logActivity("Failed to close connection for client " + clientSocket.getRemoteSocketAddress());
                }
            }
        }
        
        /*
         * ************************* managing the commands from clients************************
         * 
         */

            private void processClientCommand(String command) { // sepeerate method for processing the 5 commands
                                                            // (average,recent,send,max,min)
            String[] parts = command.split(" ", 2); // used to split the command line into 2 parts : command +
                                                    // temperature
            String action = parts[0]; // command part
            String argument = parts.length > 1 ? parts[1] : ""; // temperature part

            if (action.equalsIgnoreCase("Send")) {
                TemperatureSubmission(argument);
            } else if (action.equalsIgnoreCase("Average")) {
                AverageRequest(argument);
            } else if (action.equalsIgnoreCase("Max")) {
                MaxTemperatureRequest();
            } else if (action.equalsIgnoreCase("Min")) {
                MinTemperatureRequest();
            } else if (action.equalsIgnoreCase("Recent")) {
                RecentReadingsRequest(argument);
            } else {
                outputWriter.println("Unknown command: " + action);
                logActivity("Unrecognized command from client: " + action);
            }
        }
        // -- ADDING THE TEMPERATURE READINGS

        private void TemperatureSubmission(String tempData) {
            try {
                double temperature = Double.parseDouble(tempData);
                temperatureData.add(temperature);
                logActivity("Temperature Received: " + temperature + "from" + clientSocket.getRemoteSocketAddress());
                outputWriter.println("Temperature Received: " + temperature);
                // Check for immediate alerts
                if (temperature > WARNING_THRESHOLD) {
                    broadcastToClients("Warning! Temperature exceeded threshold: " + WARNING_THRESHOLD + " (Temperature: " + temperature + ")");
                }
                //evaluateTemperatureForAlerts();
            } catch (NumberFormatException e) {
                logActivity("Invalid temperature format: " + tempData);
                outputWriter.println("Invalid temperature format: " + tempData);
            }
        }
        

        // -- AVERAGE 'n' READINGS
        private void AverageRequest(String countStr) {
            try {
                int count = Integer.parseInt(countStr);
                double avg = calculateAverageTemperature(count);
                outputWriter.println("Average of last " + count + " readings: " + avg);
                logActivity("Sent average: " + avg);
            } catch (NumberFormatException e) {
                outputWriter.println("Invalid number for average: " + countStr);
                logActivity("Invalid number for average request: " + countStr);
            }
        }

        // --ALERT METHOD
        private void evaluateTemperatureForAlerts() {
            double averageTemperature = calculateAverageTemperature(temperatureData.size());
            if (averageTemperature > WARNING_THRESHOLD) {
                broadcastToClients("Warning! Average temperature exceeded threshold: " + WARNING_THRESHOLD + " (Average: " + averageTemperature + ")");
            }
        }
        
        
          // BROADCAST ALERTS FOR ALL
        private void broadcastToClients(String alertMessage) {
            logActivity("Broadcasting alert: " + alertMessage);
            String boldAlert = "\u001B[1;97m" + alertMessage + "\u001B[0m";

            for (Socket client : activeClients) {
                try {
                    PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                    clientOut.println("ALERT: " + boldAlert);
                } catch (IOException e) {
                    logActivity("Failed to send alert to client: " + client.getRemoteSocketAddress());
                }
            }
        }

       

        // -- MIN REQUEST
        private void MinTemperatureRequest() {
            try {
                double minTemp = Collections.min(temperatureData);
                outputWriter.println("Min temperature: " + minTemp);
                logActivity("Sent min temperature: " + minTemp);
            } catch (NoSuchElementException e) {
                outputWriter.println("No temperature data available.");
                logActivity("Min temperature request failed: no data.");
            }
        }
         // -- MAX REQUEST
         private void MaxTemperatureRequest() {
            try {
                double maxTemp = Collections.max(temperatureData);
                outputWriter.println("Max temperature: " + maxTemp);
                logActivity("Sent max temperature: " + maxTemp);
            } catch (NoSuchElementException e) {
                outputWriter.println("No temperature data available.");
                logActivity("Max temperature request failed: no data.");
            }
        }

        // --RECENT REQUEST
        private void RecentReadingsRequest(String countStr) {
            try {
                int count = Integer.parseInt(countStr);
                List<Double> recentData = fetchRecentReadings(count);
                outputWriter.println("Recent temperatures: " + recentData);
                logActivity("Sent recent data: " + recentData);
            } catch (NumberFormatException e) {
                outputWriter.println("Invalid number for recent data request: " + countStr);
                logActivity("Invalid recent data request: " + countStr);
            }
        }

        

        /*
         * ******************************** CALCULATION METHODSS ********************************
         * 
         */
        private synchronized double calculateAverageTemperature(int count) { // 
            return temperatureData.stream()
                    .skip(Math.max(0, temperatureData.size() - count))
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }

        private synchronized List<Double> fetchRecentReadings(int count) {
            return temperatureData.stream()
                    .skip(Math.max(0, temperatureData.size() - count))
                    .toList();
        }
    }

    private static void logActivity(String message) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date() );
        logger.println("[" + timeStamp + "] " + message);
        System.out.println("[" + timeStamp + "] " + message);
    }
}
