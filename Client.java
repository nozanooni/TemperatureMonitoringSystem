/*
 * <TemperatureClient>
 * ** UPDATED client ONE CUZ IM DONE!**
 * 
 *   <Group Members>                 Section: 02
 *
 */
import java.io.*;
import java.net.Socket;
import java.util.Random;
//import java.util.concurrent.ThreadLocalRandom;

public class Client {

    
    private static final int SEND_INTERVAL = 3000; // 3 seconds interval
    private static final int RUN_DURATION = 60000; // 60 seconds before sending readings
    private static final int REQUEST_INTERVAL = 2000; // 2 seconds between commands



    //-- Port / Host Info
    private static final String HOST = "localhost";
    private static final int PORT = 12352;




    public static void main(String[] args) {
        try (Socket clientSocket = new Socket(HOST, PORT);
                BufferedReader serverInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter serverOutput = new PrintWriter(clientSocket.getOutputStream(), true)) {

            System.out.println("Connected to the Server.");
            Random randomGenerator = new Random();

            // --creates listener for server responses
            createListenerThread(serverInput);

            while (true) {
                long sessionStart = System.currentTimeMillis();
                long elapsed = 0;
                int tempCount = 0;

                // Send temperatures periodically every 60 secons
                while (elapsed < RUN_DURATION) {
                    double tempValue = generateRandomTemp();
                    serverOutput.println("Send " + tempValue);
                    System.out.println("[Client] Sent temperature: " + tempValue);
                    tempCount++;

                    Thread.sleep(SEND_INTERVAL); // Pause before the next reading
                    elapsed = System.currentTimeMillis() - sessionStart;
                }

                // random 'n' readings to request recent and average
                int recentCount = randomGenerator.nextInt(tempCount) + 1;
                int averageCount = randomGenerator.nextInt(tempCount) + 1;

                // request statistics data from server
                RequestStats(serverOutput, recentCount, averageCount);
            }

        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    /* 
     this is just a method i tried; it works but i wanted smth simpler

    private static double generateRandomTemp() {
        double rawTemp = ThreadLocalRandom.current().nextDouble(T_MIN, T_MAX); 
        return Double.parseDouble(String.format("%.1f", rawTemp)); 
    }
    */ 

    
 //--GENERATES TEMPERATURE READINGS

   private static double generateRandomTemp() {
    // Generate a random value between 15.0 and 40.0
    double rawTemp = 15.0 + (Math.random() * (40.0 - 15.0));

    // Round to 1 decimal place
    return Double.parseDouble(String.format("%.1f", rawTemp));
}


    private static void RequestStats(PrintWriter serverOutput, int recent, int average) throws InterruptedException {
        System.out.println("[Client] Requesting avg,min,max,recent...");

        // --REQUESTS AVERAGE
        System.out.println("[Client] Average " + average);
        serverOutput.println("Average " + average);
        Thread.sleep(REQUEST_INTERVAL);

        // --REQUESTS MAX
        System.out.println("[Client] Max Readings");
        serverOutput.println("Max");
        Thread.sleep(REQUEST_INTERVAL);

        // REQUESTS MIN
        System.out.println("[Client] Min Readings");
        serverOutput.println("Min");
        Thread.sleep(REQUEST_INTERVAL);

        // REQUESTS RECENT 
        System.out.println("[Client] Recent " + recent);
        serverOutput.println("Recent " + recent);
        Thread.sleep(REQUEST_INTERVAL);
    }

    /**
     * Creates a new thread to listen for server responses.
     */
    private static void createListenerThread(BufferedReader serverInput) {
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = serverInput.readLine()) != null) {
                    System.out.println("[Server] " + serverMessage);
                }
            } catch (IOException ex) {
                System.out.println("Server connection lost.");
            }
        }).start();
    }

    

    

}
