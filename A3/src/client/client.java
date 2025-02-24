import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;


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
    }

    private static void automatedTesting() {
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
}
