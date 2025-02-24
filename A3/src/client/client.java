/*  
*  FILE          : client.java 
*  PROJECT       : Assignment #2 
*  PROGRAMMER    : Ayushkumar Rakholiya & jaykumar Patel
*  FIRST VERSION : 2025-02-20 
*  DESCRIPTION   : This file contains the client code for the logging system.
*/

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

public class client {
    private static final Scanner scanner = new Scanner(System.in);
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8089;

    record logMessage(
        String action,
        Parameters parameters
    ) {
        record Parameters(
            String timestamp,
            String level,
            String message,
            String fileName,
            int fileLine,
            List<String> tags,
            String format
        ) {}
    }

    public static void main(String[] args) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> manualTesting();
                case "2" -> automatedTesting();
                case "3" -> {
                    System.out.println("Exiting the client.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please select from 1, 2, or 3.");
            }
        }
    }

//  
// FUNCTION      : printMenu 
// DESCRIPTION   : 
//   This function prints the menu options to the console for the user to select. 
// PARAMETERS    : 
//   None 
// RETURNS       : 
//   void 
//
    private static void printMenu() {
        System.out.println("\nLogging Test Client Menu");
        System.out.println("1. Manual Testing");
        System.out.println("2. Automated Testing");
        System.out.println("3. Exit");
        System.out.print("Choose an option (1/2/3): ");
    }

//  
// FUNCTION      : manualTesting 
// DESCRIPTION   : 
//   This function handles manual testing by prompting the user to enter log details and sending the log messages to the server. 
// PARAMETERS    : 
//   None 
// RETURNS       : 
//   void 
//
    private static void manualTesting() {
        while (true) {
            String level = getInput("Enter log level (e.g., INFO, ERROR): ");
            String message = getInput("Enter log message: ");
            String format = getFormatInput();

            sendlogMessage(createlogMessage(level, message, format));

            if (!confirm("Do you want to send another log message?")) {
                break;
            }
        }
    }

//  
// FUNCTION      : automatedTesting 
// DESCRIPTION   : 
//   This function handles automated testing by sending a specified number of log messages to the server with a delay between each message. 
// PARAMETERS    : 
//   None 
// RETURNS       : 
//   void 
//
    private static void automatedTesting() {
        int numLogs = getIntInput("Enter the number of log messages to send: ");
        float delay = getFloatInput("Enter the delay between log messages in seconds: ");
        String format = getFormatInput();

        IntStream.range(0, numLogs).forEach(i -> {
            sendlogMessage(createlogMessage(
                "INFO",
                "Automated log message " + (i + 1),
                format
            ));
            sleep(delay);
        });
    }

//  
// FUNCTION      : toJson 
// DESCRIPTION   : 
//   This function converts an object to its JSON representation. 
// PARAMETERS    : 
//   Object obj : the object to convert to JSON 
// RETURNS       : 
//   String : the JSON representation of the object 
//
    private static String toJson(Object obj) {
        if (obj instanceof logMessage logMessage) {
            return String.format(
                "{\"Action\":%s,\"Parameters\":%s}",
                toJson(logMessage.action()),
                toJson(logMessage.parameters())
            );
        }
        if (obj instanceof logMessage.Parameters params) {
            return String.format(
                "{\"Timestamp\":%s,\"Level\":%s,\"Message\":%s,\"FileName\":%s," +
                "\"FileLine\":%d,\"Tags\":%s,\"Format\":%s}",
                toJson(params.timestamp()),
                toJson(params.level()),
                toJson(params.message()),
                toJson(params.fileName()),
                params.fileLine(),
                toJson(params.tags()),
                toJson(params.format())
            );
        }
        if (obj instanceof String str) {
            return "\"" + escapeJson(str) + "\"";
        }
        if (obj instanceof Number num) {
            return num.toString();
        }
        if (obj instanceof List<?> list) {
            return list.stream()
                     .map(client::toJson)
                     .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }
        throw new IllegalArgumentException("Unsupported type: " + obj.getClass());
    }

//  
// FUNCTION      : escapeJson 
// DESCRIPTION   : 
//   This function escapes special characters in a JSON string. 
// PARAMETERS    : 
//   String input : the input string to escape 
// RETURNS       : 
//   String : the escaped JSON string 
//
    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

//  
// FUNCTION      : toCsv 
// DESCRIPTION   : 
//   This function converts a logMessage to its CSV representation. 
// PARAMETERS    : 
//   logMessage msg : the log message to convert to CSV 
// RETURNS       : 
//   String : the CSV representation of the log message 
//
    private static String toCsv(logMessage msg) {
        logMessage.Parameters params = msg.parameters();
        // Join tags with a semicolon for CSV output
        String tagsCsv = escapeCsv(String.join(";", params.tags()));
        return escapeCsv(msg.action()) + "," +
               escapeCsv(params.timestamp()) + "," +
               escapeCsv(params.level()) + "," +
               escapeCsv(params.message()) + "," +
               escapeCsv(params.fileName()) + "," +
               params.fileLine() + "," +
               tagsCsv + "," +
               escapeCsv(params.format());
    }

//  
// FUNCTION      : escapeCsv 
// DESCRIPTION   : 
//   This function escapes special characters in a CSV string. 
// PARAMETERS    : 
//   String input : the input string to escape 
// RETURNS       : 
//   String : the escaped CSV string 
//
    private static String escapeCsv(String input) {
        if (input.contains(",") || input.contains("\"") || input.contains("\n") || input.contains("\r")) {
            input = input.replace("\"", "\"\"");
            return "\"" + input + "\"";
        }
        return input;
    }

//  
// FUNCTION      : toText 
// DESCRIPTION   : 
//   This function converts a logMessage to a simple text format. 
// PARAMETERS    : 
//   logMessage msg : the log message to convert to text 
// RETURNS       : 
//   String : the text representation of the log message 
//
    private static String toText(logMessage msg) {
        logMessage.Parameters params = msg.parameters();
        return "Action: " + msg.action() + "\n" +
               "Timestamp: " + params.timestamp() + "\n" +
               "Level: " + params.level() + "\n" +
               "Message: " + params.message() + "\n" +
               "File: " + params.fileName() + ":" + params.fileLine() + "\n" +
               "Tags: " + String.join(", ", params.tags()) + "\n" +
               "Format: " + params.format();
    }

//  
// FUNCTION      : sendlogMessage 
// DESCRIPTION   : 
//   This function sends the log message to the server and prints the server's response. 
// PARAMETERS    : 
//   logMessage logMessage : the log message to send 
// RETURNS       : 
//   void 
//
    private static void sendlogMessage(logMessage logMessage) {
        try (Socket clientSocket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream out = clientSocket.getOutputStream();
            InputStream in = clientSocket.getInputStream()) {

            String json = toJson(logMessage);
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();

            byte[] buffer = new byte[4096];
            int bytesRead = in.read(buffer);
            if (bytesRead != -1) {
                System.out.println("Server Response: " +
                    new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    
//  
// FUNCTION      : createlogMessage 
// DESCRIPTION   : 
//   This function creates a log message with the specified level, message, and format. 
// PARAMETERS    : 
//   String level   : the log level (e.g., INFO, ERROR) 
//   String message : the log message 
//   String format  : the log format (e.g., text, json) 
// RETURNS       : 
//   logMessage : the created log message 
//
    private static logMessage createlogMessage(String level, String message, String format) {
        return new logMessage(
            "Log",
            new logMessage.Parameters(
                LocalDateTime.now().toString(),
                level,
                message,
                "client.java",
                5,
                List.of("test", "client", "logging"),
                format.toLowerCase()
            )
        );
    }
    
//  
// FUNCTION      : getInput 
// DESCRIPTION   : 
//   This function prompts the user for input and returns the entered string. 
// PARAMETERS    : 
//   String prompt : the prompt message to display to the user 
// RETURNS       : 
//   String : the user's input 
//
    private static String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    
//  
// FUNCTION      : getFormatInput 
// DESCRIPTION   : 
//   This function prompts the user for a log format input and returns the entered format. 
// PARAMETERS    : 
//   None 
// RETURNS       : 
//   String : the user's input as a log format (text, json, or csv) 
//
    private static String getFormatInput() {
        while (true) {
            String format = getInput("Enter log format (text/json/csv): ").trim().toLowerCase();
            if (format.equals("text") || format.equals("json") || format.equals("csv")) {
                return format;
            }
            System.out.println("Invalid format. Please enter 'text', 'json' or 'csv'.");
        }
    }
    
//  
// FUNCTION      : confirm 
// DESCRIPTION   : 
//   This function prompts the user for a confirmation (yes/no) and returns the user's response. 
// PARAMETERS    : 
//   String prompt : the prompt message to display to the user 
// RETURNS       : 
//   boolean : true if the user confirms, false otherwise 
//
    private static boolean confirm(String prompt) {
        System.out.print(prompt + " (yes/no): ");
        return scanner.nextLine().trim().equalsIgnoreCase("yes");
    }

//  
// FUNCTION      : getIntInput 
// DESCRIPTION   : 
//   This function prompts the user for an integer input and returns the entered integer. 
// PARAMETERS    : 
//   String prompt : the prompt message to display to the user 
// RETURNS       : 
//   int : the user's input as an integer 
//
    private static int getIntInput(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(getInput(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Please try again.");
            }
        }
    }

//  
// FUNCTION      : getFloatInput 
// DESCRIPTION   : 
//   This function prompts the user for a float input and returns the entered float. 
// PARAMETERS    : 
//   String prompt : the prompt message to display to the user 
// RETURNS       : 
//   float : the user's input as a float 
//
    private static float getFloatInput(String prompt) {
        while (true) {
            try {
                return Float.parseFloat(getInput(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Please try again.");
            }
        }
    }

//  
// FUNCTION      : sleep 
// DESCRIPTION   : 
//   This function pauses the execution for a specified number of seconds. 
// PARAMETERS    : 
//   float seconds : the number of seconds to sleep 
// RETURNS       : 
//   void 
//
    private static void sleep(float seconds) {
        try {
            Thread.sleep((long)(seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
