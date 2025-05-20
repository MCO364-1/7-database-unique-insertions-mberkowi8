import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    public static void main(String[] args) {
        //Load database configuration from properties file
        loadProperties();

        Scanner scanner = new Scanner(System.in);

        // Prompt user for input
        System.out.print("Enter your first name please: ");
        String firstName = scanner.nextLine().trim();

        System.out.print("Enter your last name please: ");
        String lastName = scanner.nextLine().trim();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            //check if the Name already exists and insert if not
            if (insertPerson(conn, firstName, lastName)) {
                System.out.println("Person inserted successfully.");
            } else {
                System.out.println("Person already exists.");
            }

            //display all persons
            displayNames(conn);

        } catch (SQLException e) {
            System.err.println("DB error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    //Didn't store names in a set but could've used it because then we don't need to query for names
    //already put in and then at the end to get all of the first letters without checking the DB.

    private static void displayNames(Connection conn) throws SQLException {
        //get total number of persons
        int total = 0;
        String countQuery = "SELECT COUNT(*) FROM People";

        try (PreparedStatement prepState = conn.prepareStatement(countQuery);
            ResultSet results = prepState.executeQuery()) {
            if (results.next()) {
                total = results.getInt(1);
            }
        }

        System.out.println("Total number of persons: " + total);

        //Make Arrays for the first letter of
        // the first name count and for the last name too
        int[] fNameLetterCount = new int[26];
        int[] lNameLetterCount = new int[26];

        //Query for first name counts
        String firstNameQuery = "SELECT UPPER(LEFT(firstName, 1)) as firstLetter, " +
                                "COUNT(*) as count FROM People " +
                                "GROUP BY UPPER(LEFT(firstName, 1))";

        try (PreparedStatement fNameState = conn.prepareStatement(firstNameQuery);
            ResultSet results = fNameState.executeQuery()) {
            while (results.next()) {
                String firstLetter = results.getString("firstLetter");
                if (firstLetter != null && !firstLetter.isEmpty()) {
                    char letter = firstLetter.charAt(0);
                    if (letter >= 'A' && letter <= 'Z') {
                        fNameLetterCount[letter - 'A'] = results.getInt("count");
                    }
                }
            }
        }
        //Query for last name counts
        String lastNameQuery = "SELECT UPPER(LEFT(lastName, 1)) as firstLetter, " +
                            "COUNT(*) as count FROM People " +
                            "GROUP BY UPPER(LEFT(lastName, 1))";

        try (PreparedStatement lNameState = conn.prepareStatement(lastNameQuery);
            ResultSet results = lNameState.executeQuery()) {
            while (results.next()) {
                String firstLetter = results.getString("firstLetter");
                if (firstLetter != null && !firstLetter.isEmpty()) {
                    char letter = firstLetter.charAt(0);
                    if (letter >= 'A' && letter <= 'Z') {
                        lNameLetterCount[letter - 'A'] = results.getInt("count");
                    }
                }
            }
        }
        //Display the first name counts
        System.out.println("\nFirst Name First letter Count:");
        for (int i = 0; i < 26; i++) {
            char letter = (char) ('A' + i);
            System.out.println(letter + ": " + fNameLetterCount[i]);
        }

        //Display the last name counts
        System.out.println("\nLast Name First letter Count:");
        for (int i = 0; i < 26; i++) {
            char letter = (char) ('A' + i);
            System.out.println(letter + ": " + lNameLetterCount[i]);
        }
    }

    private static boolean insertPerson(Connection conn, String firstName, String lastName) throws SQLException {
        // Check if the person already exists
        String checkQuery = "SELECT COUNT(*) FROM People WHERE firstName = ? AND lastName = ?";

        try (PreparedStatement checkState = conn.prepareStatement(checkQuery)) {
            checkState.setString(1, firstName);
            checkState.setString(2, lastName);

            try (ResultSet results = checkState.executeQuery()) {
                if (results.next() && results.getInt(1) > 0) {
                    return false; // Person already exists
                }
            }
        }

        // Insert the new person
        String insertQuery = "INSERT INTO People (firstName, lastName) VALUES (?, ?)";

        try (PreparedStatement insertState = conn.prepareStatement(insertQuery)) {
            insertState.setString(1, firstName);
            insertState.setString(2, lastName);
            insertState.executeUpdate();
            return true;
        }
    }

    private static void loadProperties() {
        try {
            Properties props = new Properties();
            File propsFile = new File("database.properties");
            if (propsFile.exists()) {
                try (FileInputStream inputStream = new FileInputStream(propsFile)) {
                    props.load(inputStream);

                    DB_URL = props.getProperty("DB_URL");
                    DB_USER = props.getProperty("DB_USER");
                    DB_PASSWORD = props.getProperty("DB_PASSWORD");

                    if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
                        throw new RuntimeException("Missing database configuration properties.");
                    }
                }
            } else {
                throw new RuntimeException("Properties file not found!");
            }
        } catch (Exception e) {
            System.err.println("Error loading the properties: " + e.getMessage());
            System.exit(1);
        }
    }
}