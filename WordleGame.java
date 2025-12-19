import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class WordleGame {

    // ANSI color code
    private final String GREEN = "\u001B[32m";
    private final String YELLOW = "\u001B[33m";
    private final String WHITE = "\u001B[37m";
    private final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        // Since static is disabled, we create an object and run the game from there
        WordleGame game = new WordleGame();
        game.start(args);
    }

    public void start(String[] args) {
        // 1. Check the command line argument (index)
        if (args.length == 0) {
            System.out.println("Please provide a number as command line argument");
            return;
        }

        // 2. Read the word from the file wordle-words.txt
        String secretWord = loadSecretWord(args[0]);

        // 3. Create one Scanner for the entire life of the program
        Scanner scanner = new Scanner(System.in);

        // If the secret word couldn't be loaded (invalid index), print expected error and exit
        if (secretWord == null) {
            System.out.println("Invalid command-line argument. Please launch with a valid number.");
            return;
        }

        // 4. Load word list for validation
        List<String> wordList = loadWordList();

        // 5. Run the game logic 
        play(scanner, secretWord, wordList);
    }

    private List<String> loadWordList() {
        try {
            return Files.readAllLines(Paths.get("wordle-words.txt"));
        } catch (IOException e) {
            return List.of();
        }
    }

    private String loadSecretWord(String indexArg) {
        try {
            List<String> words = Files.readAllLines(Paths.get("wordle-words.txt"));
            int index = Integer.parseInt(indexArg);
            if (index >= 0 && index < words.size()) {
                return words.get(index).trim();
            }
        } catch (IOException | NumberFormatException e) {
            // Be polite: a missing file or an incorrect index should not crash the program
        }
        return null;
    }

    private void play(Scanner scanner, String secretWord, List<String> wordList) {
        int attempts = 0;
        final int MAX_ATTEMPTS = 6;
        boolean won = false;
        Set<Character> guessedLetters = new HashSet<>();
        boolean eofEncountered = false;

        // Ask for username for statistics
        System.out.print("Enter your username:");
        System.out.print(" Welcome to Wordle! Guess the 5-letter word.");
        System.out.println();
        String username = scanner.hasNextLine() ? scanner.nextLine() : "Unknown";

        while (attempts < MAX_ATTEMPTS && !won) {
            System.out.print("Enter your guess:  ");

            if (!scanner.hasNextLine()) { eofEncountered = true; break; }
            String guess = scanner.nextLine().trim();

            // Validate input - print error messages on same line after prompt
            if (guess.length() != 5) {
                System.out.println(" Your guess must be exactly 5 letters long.");
                continue;
            }

            if (!guess.matches("[a-z]{5}")) {
                System.out.println(" Your guess must only contain lowercase letters.");
                continue;
            }

            // Check word-in-list
            if (!isInWordList(guess, wordList)) {
                System.out.println(" Word not in list. Please enter a valid word.");
                continue;
            }

            // At this point guess is valid
            attempts++;

            // If correct guess - congratulate and end the game (do not print feedback)
            if (guess.equalsIgnoreCase(secretWord)) {
                System.out.println("Congratulations! You've guessed the word correctly.");
                won = true;
                break;
            }

            String feedback = getFeedback(guess, secretWord);
            System.out.println("Feedback: " + feedback);

            // Track eliminated letters (letters guessed that are NOT in secret)
            String guessUpper = guess.toUpperCase();
            String secretUpper = secretWord.toUpperCase();
            for (char c : guessUpper.toCharArray()) {
                if (secretUpper.indexOf(c) == -1) {
                    guessedLetters.add(c);
                }
            }

            // Show remaining letters
            showRemainingLetters(guessedLetters);
            System.out.println("Attempts remaining: " + (MAX_ATTEMPTS - attempts));
        }

        if (eofEncountered) {
            return;
        }

        if (!won) {
            System.out.println("Game over. The correct word was: " + secretWord.toLowerCase());
        }

        // Save statistics
        saveStats(username, secretWord, attempts, won ? "win" : "loss");
        
        // Offer the ability to view statistics
        System.out.print("Do you want to see your stats? (yes/no): ");
        if (scanner.hasNextLine() && scanner.nextLine().equalsIgnoreCase("yes")) {
            showStats(username);
        }
    }

    private boolean isValidGuess(String guess, List<String> wordList) {
        if (guess.length() != 5) {
            System.out.println("Your guess must be exactly 5 letters long.");
            return false;
        }

        if (!guess.matches("[a-z]{5}")) {
            System.out.println("Your guess must only contain lowercase letters.");
            return false;
        }

        // Convert word list to lowercase for comparison
        String guessLower = guess.toLowerCase();
        for (String word : wordList) {
            if (word.trim().equalsIgnoreCase(guessLower)) {
                return true;
            }
        }

        System.out.println("Word not in list. Please enter a valid word.");
        return false;
    }

    private boolean isInWordList(String guess, List<String> wordList) {
        String guessLower = guess.toLowerCase();
        for (String word : wordList) {
            if (word.trim().equalsIgnoreCase(guessLower)) return true;
        }
        return false;
    }

    private String getFeedback(String guess, String secret) {
        // Green: guess[i] == secret[i]
        // Yellow: secret contains guess[i], but in the wrong place
        // White: not in secret at all
        String guessUpper = guess.toUpperCase();
        String secretUpper = secret.toUpperCase();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char c = guessUpper.charAt(i);
            if (c == secretUpper.charAt(i)) {
                sb.append(GREEN).append(c).append(RESET);
            } else if (secretUpper.contains(String.valueOf(c))) {
                sb.append(YELLOW).append(c).append(RESET);
            } else {
                sb.append(WHITE).append(c).append(RESET);
            }
        }
        return sb.toString();
    }
    
    private void showRemainingLetters(Set<Character> guessedLetters) {
        StringBuilder remaining = new StringBuilder();
        remaining.append("Remaining letters: ");
        
        boolean first = true;
        for (char c = 'A'; c <= 'Z'; c++) {
            if (!guessedLetters.contains(c)) {
                if (!first) {
                    remaining.append(" ");
                }
                remaining.append(c);
                first = false;
            }
        }
        System.out.println(remaining.toString());
    }

    private void saveStats(String user, String word, int att, String res) {
        try (FileWriter fw = new FileWriter("stats.csv", true)) {
            fw.write(user + "," + word + "," + att + "," + res + "\n");
        } catch (IOException e) {
            // Handle gracefully
        }
    }

    private void showStats(String username) {
        try {
            List<String> lines = Files.readAllLines(Paths.get("stats.csv"));
            Map<String, Integer> gamesPlayed = new HashMap<>();
            Map<String, Integer> gamesWon = new HashMap<>();
            Map<String, Integer> totalAttempts = new HashMap<>();

            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String user = parts[0];
                    String result = parts[3];
                    int attempts = Integer.parseInt(parts[2]);

                    gamesPlayed.put(user, gamesPlayed.getOrDefault(user, 0) + 1);
                    totalAttempts.put(user, totalAttempts.getOrDefault(user, 0) + attempts);

                    if ("win".equals(result)) {
                        gamesWon.put(user, gamesWon.getOrDefault(user, 0) + 1);
                    }
                }
            }

            if (gamesPlayed.containsKey(username)) {
                int played = gamesPlayed.get(username);
                int won = gamesWon.getOrDefault(username, 0);
                double average = (double) totalAttempts.get(username) / played;

                System.out.println("Stats for " + username + ":");
                System.out.println("Games played: " + played);
                System.out.println("Games won: " + won);
                System.out.printf(Locale.US, "Average attempts per game: %.1f\n", average);
            } else {
                System.out.println("No stats available yet.");
            }
        } catch (IOException e) {
            System.out.println("No stats available yet.");
        }
    }
}
