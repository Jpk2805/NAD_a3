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

    private static void printMenu() {
        System.out.println("\nLogging Test Client Menu");
        System.out.println("1. Manual Testing");
        System.out.println("2. Automated Testing");
        System.out.println("3. Exit");
        System.out.print("Choose an option (1/2/3): ");
    }

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

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

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
    
    private static String getInput(String prompt) {
            System.out.print(prompt);
            return scanner.nextLine();
        }
    
    private static String getFormatInput() {
            while (true) {
                String format = getInput("Enter log format (text/json): ").trim().toLowerCase();
                if (format.equals("text") || format.equals("json")) {
                    return format;
                }
                System.out.println("Invalid format. Please enter 'text' or 'json'.");
            }
        }
    
    private static boolean confirm(String prompt) {
            System.out.print(prompt + " (yes/no): ");
            return scanner.nextLine().trim().equalsIgnoreCase("yes");
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(getInput(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Please try again.");
            }
        }
    }

    private static float getFloatInput(String prompt) {
        while (true) {
            try {
                return Float.parseFloat(getInput(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number! Please try again.");
            }
        }
    }

    private static void sleep(float seconds) {
        try {
            Thread.sleep((long)(seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}
