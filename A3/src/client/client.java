import java.util.List;
import java.util.Scanner;

public class client {
    private static final Scanner scanner = new Scanner(System.in);

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
}
