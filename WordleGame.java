import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class WordleGame {

    // ANSI color code
    private final String GREEN = "\u001B[32m";
    private final String YELLOW = "\u001B[33m";
    private final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        // Since static is disabled, we create an object and run the game from there
        WordleGame game = new WordleGame();
        game.start(args);
    }

    public void start(String[] args) {
        // 1. Check the command line argument (index)
        if (args.length == 0) {
            return; // Or display an error message and exit politely
        }

        // 2. Read the word from the file wordle-words.txt
        String secretWord = loadSecretWord(args[0]);
        if (secretWord == null) {
            return; // File missing or index incorrect
        }

        // 3. Create one Scanner for the entire life of the program
        Scanner scanner = new Scanner(System.in);

        // 4. Run the game logic
        play(scanner, secretWord);
    }

    private String loadSecretWord(String indexArg) {
        try {
            List<String> words = Files.readAllLines(Paths.get("wordle-words.txt"));
            int index = Integer.parseInt(indexArg);
            if (index >= 0 && index < words.size()) {
                return words.get(index).toUpperCase();
            }
        } catch (IOException | NumberFormatException e) {
            // Be polite: a missing file or an incorrect index should not crash the program
        }
        return null;
    }

    private void play(Scanner scanner, String secretWord) {
        int attempts = 0;
        final int MAX_ATTEMPTS = 6;
        boolean won = false;

        // Ask for username for statistics
        System.out.println("Enter username:");
        String username = scanner.hasNextLine() ? scanner.nextLine() : "Unknown";

        while (attempts < MAX_ATTEMPTS && !won) {
            System.out.println("Attempts remaining: " + (MAX_ATTEMPTS - attempts));
            System.out.println("Enter your guess:");

            if (!scanner.hasNextLine()) break;
            String guess = scanner.nextLine().toUpperCase();

            if (guess.length() != 5) {
                System.out.println("Please enter a 5-letter word.");
                continue;
            }

            attempts++;
            String feedback = getFeedback(guess, secretWord);
            System.out.println(feedback);

            if (guess.equals(secretWord)) {
                won = true;
                System.out.println("Correct!");
            }
        }

        if (!won) {
            System.out.println("Game over! The word was: " + secretWord);
        }

        // Save statistics
        saveStats(username, secretWord, attempts, won ? "win" : "loss");
        
        // Offer the ability to view statistics
        System.out.println("View stats? (yes/no)");
        if (scanner.hasNextLine() && scanner.nextLine().equalsIgnoreCase("yes")) {
            showStats();
        }
    }

    private String getFeedback(String guess, String secret) {
        // This is where the coloring logic happens
        // Green: guess[i] == secret[i]
        // Yellow: secret contains guess[i], but in the wrong place
        // White: none at all
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char c = guess.charAt(i);
            if (c == secret.charAt(i)) {
                sb.append(GREEN).append(c).append(RESET);
            } else if (secret.contains(String.valueOf(c))) {
                sb.append(YELLOW).append(c).append(RESET);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void saveStats(String user, String word, int att, String res) {
        try (FileWriter fw = new FileWriter("stats.csv", true)) {
            fw.write(user + "," + word + "," + att + "," + res + "\n");
        } catch (IOException e) {
            // Handle gracefully
        }
    }

    private void showStats() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("stats.csv"));
            for (String line : lines) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("No stats available yet.");
        }
    }
}
