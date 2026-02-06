import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

public class WordleGame {

    // Game constants
    private static final int WORD_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 6;
    private static final String WORDS_FILE = "wordle-words.txt";
    private static final String STATS_FILE = "stats.csv";
    private static final String DEFAULT_USERNAME = "Unknown";
    private static final String WIN_RESULT = "win";
    private static final String LOSS_RESULT = "loss";
    
    // ANSI color codes
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String WHITE = "\u001B[37m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        WordleGame game = new WordleGame();
        game.start(args);
    }

    public void start(String[] args) {
        if (!hasValidArguments(args)) {
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            // Küsime sõna pikkust
            System.out.print("Enter desired word length (e.g. 5): ");
            int tempLength = WORD_LENGTH;
            String lenInput = scanner.nextLine().trim();
            if (!lenInput.isEmpty()) {
                try {
                    tempLength = Integer.parseInt(lenInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input, using default length " + WORD_LENGTH + ".");
                    tempLength = WORD_LENGTH;
                }
            }
            final int chosenLength = tempLength;

            List<String> wordList = loadWordList();
            if (wordList.isEmpty()) {
                System.out.println("Word list file '" + WORDS_FILE + "' is missing or empty. Cannot start the game.");
                waitForUserExit(scanner);
                return;
            }

            // Filtreeri sõnaloend valitud pikkusega sõnadele
            java.util.List<String> filteredList = new java.util.ArrayList<>();
            for (String w : wordList) {
                String trimmed = w.trim();
                if (trimmed.length() == chosenLength) {
                    filteredList.add(trimmed);
                }
            }
            if (filteredList.isEmpty()) {
                System.out.println("No words of length " + chosenLength + " found in the word list. Cannot start the game.");
                waitForUserExit(scanner);
                return;
            }

            // Valime salajase sõna juhuslikult
            String secretWord = filteredList.get((int)(Math.random() * filteredList.size()));

            play(scanner, secretWord, filteredList, chosenLength);
        }
    }

    private boolean hasValidArguments(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a number as command line argument");
            return false;
        }
        return true;
    }


    private void waitForUserExit(Scanner scanner) {
        System.out.println("Press Enter to exit...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    private List<String> loadWordList() {
        try {
            return Files.readAllLines(Paths.get(WORDS_FILE));
        } catch (IOException e) {
            return List.of();
        }
    }


    private void play(Scanner scanner, String secretWord, List<String> wordList, int wordLength) {
        String username = getUserName(scanner);
        System.out.println("Welcome to Wordle! Guess the " + wordLength + "-letter word.");

        GameState gameState = new GameState();

        while (gameState.canContinuePlaying()) {
            String guess = getUserGuess(scanner);

            if (guess == null) {
                return; // EOF encountered
            }

            if (guess.equalsIgnoreCase("exit")) {
                System.out.println("Game exited by user.");
                return;
            }

            if (guess.length() != wordLength) {
                System.out.println("Your guess must be exactly " + wordLength + " letters long.");
                continue;
            }

            if (!isValidGuess(guess, wordList)) {
                continue;
            }

            gameState.incrementAttempts();

            if (isCorrectGuess(guess, secretWord)) {
                handleWin();
                gameState.setWon(true);
                break;
            }

            displayFeedback(guess, secretWord, gameState);
        }

        handleGameEnd(scanner, username, secretWord, gameState);
    }

    private String getUserName(Scanner scanner) {
        System.out.print("Enter your username and press Enter: ");
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return DEFAULT_USERNAME;
            }
            return input;
        } else {
            return DEFAULT_USERNAME;
        }
    }


    private String getUserGuess(Scanner scanner) {
        System.out.print("Enter your guess:  ");
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

    private boolean isCorrectGuess(String guess, String secretWord) {
        return guess.equalsIgnoreCase(secretWord);
    }

    private void handleWin() {
        System.out.println("Congratulations! You've guessed the word correctly.");
    }

    private void displayFeedback(String guess, String secretWord, GameState gameState) {
        String feedback = getFeedback(guess, secretWord);
        System.out.println("Feedback: " + feedback);
        
        updateEliminatedLetters(guess, secretWord, gameState.getEliminatedLetters());
        showRemainingLetters(gameState.getEliminatedLetters());
        System.out.println("Attempts remaining: " + gameState.getRemainingAttempts());
    }

    private void updateEliminatedLetters(String guess, String secretWord, Set<Character> eliminatedLetters) {
        String guessUpper = guess.toUpperCase();
        String secretUpper = secretWord.toUpperCase();
        
        for (char c : guessUpper.toCharArray()) {
            if (!secretUpper.contains(String.valueOf(c))) {
                eliminatedLetters.add(c);
            }
        }
    }

    private void handleGameEnd(Scanner scanner, String username, String secretWord, GameState gameState) {
        if (!gameState.hasWon()) {
            System.out.println("Game over. The correct word was: " + secretWord.toLowerCase());
        }

        String result = gameState.hasWon() ? WIN_RESULT : LOSS_RESULT;
        saveStats(username, secretWord, gameState.getAttempts(), result);
        
        offerStatsDisplay(scanner, username);
    }

    private void offerStatsDisplay(Scanner scanner, String username) {
        System.out.print("Do you want to see your stats? (yes/no): ");
        if (scanner.hasNextLine() && scanner.nextLine().equalsIgnoreCase("yes")) {
            showStats(username);
        }
    }

    // Inner class to manage game state
    private class GameState {
        private int attempts = 0;
        private boolean won = false;
        private final Set<Character> eliminatedLetters = new HashSet<>();

        public void incrementAttempts() {
            attempts++;
        }

        public int getAttempts() {
            return attempts;
        }

        public int getRemainingAttempts() {
            return MAX_ATTEMPTS - attempts;
        }

        public boolean canContinuePlaying() {
            return attempts < MAX_ATTEMPTS && !won;
        }

        public void setWon(boolean won) {
            this.won = won;
        }

        public boolean hasWon() {
            return won;
        }

        public Set<Character> getEliminatedLetters() {
            return eliminatedLetters;
        }
    }

    /**
     * Validates if the guess meets all requirements:
     * - Exactly 5 letters long
     * - Only lowercase letters
     * - Present in word list
     */
    private boolean isValidGuess(String guess, List<String> wordList) {
        if (!hasCorrectLength(guess)) {
            System.out.println("Your guess must be exactly " + WORD_LENGTH + " letters long.");
            return false;
        }
        
        if (!hasOnlyLowercaseLetters(guess)) {
            System.out.println("Your guess must only contain lowercase letters.");
            return false;
        }
        
        if (!isInWordList(guess, wordList)) {
            System.out.println("Word not in list. Please enter a valid word.");
            return false;
        }
        
        return true;
    }

    private boolean hasCorrectLength(String guess) {
        return guess.length() == WORD_LENGTH;
    }

    private boolean hasOnlyLowercaseLetters(String guess) {
        return guess.matches("[a-z]{" + WORD_LENGTH + "}");
    }

    private boolean isInWordList(String guess, List<String> wordList) {
        return wordList.stream()
                .anyMatch(word -> word.trim().equalsIgnoreCase(guess));
    }

    /**
     * Generates colored feedback for the guess:
     * - Green: correct letter in correct position
     * - Yellow: correct letter in wrong position
     * - White: letter not in word
     */
    private String getFeedback(String guess, String secret) {
        String guessUpper = guess.toUpperCase();
        String secretUpper = secret.toUpperCase();
        
        StringBuilder feedback = new StringBuilder();
        
        for (int i = 0; i < WORD_LENGTH; i++) {
            char guessChar = guessUpper.charAt(i);
            String coloredChar = getColoredCharacter(guessChar, i, secretUpper);
            feedback.append(coloredChar);
        }
        
        return feedback.toString();
    }

    private String getColoredCharacter(char guessChar, int position, String secretWord) {
        if (isCorrectPosition(guessChar, position, secretWord)) {
            return colorize(guessChar, GREEN);
        }
        
        if (isWrongPosition(guessChar, secretWord)) {
            return colorize(guessChar, YELLOW);
        }
        
        return colorize(guessChar, WHITE);
    }

    private boolean isCorrectPosition(char guessChar, int position, String secretWord) {
        return guessChar == secretWord.charAt(position);
    }

    private boolean isWrongPosition(char guessChar, String secretWord) {
        return secretWord.contains(String.valueOf(guessChar));
    }

    private String colorize(char character, String color) {
        return color + character + RESET;
    }
    
    private void showRemainingLetters(Set<Character> eliminatedLetters) {
        StringBuilder remaining = new StringBuilder("Remaining letters: ");
        
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            if (!eliminatedLetters.contains(letter)) {
                if (remaining.length() > "Remaining letters: ".length()) {
                    remaining.append(" ");
                }
                remaining.append(letter);
            }
        }
        
        System.out.println(remaining.toString());
    }

    private void saveStats(String username, String word, int attempts, String result) {
        try {
            // Kontrolli, kas fail eksisteerib, kui ei, loo see
            java.io.File statsFile = new java.io.File(STATS_FILE);
            if (!statsFile.exists()) {
                statsFile.createNewFile();
            }
            try (FileWriter writer = new FileWriter(statsFile, true)) {
                String statLine = formatStatLine(username, word, attempts, result);
                writer.write(statLine);
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not save game statistics to '" + STATS_FILE + "'.");
        }
    }

    private String formatStatLine(String username, String word, int attempts, String result) {
        return username + "," + word + "," + attempts + "," + result + "\n";
    }

    private void showStats(String username) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(STATS_FILE));
            UserStats stats = calculateUserStats(lines, username);
            
            if (stats.hasGames()) {
                displayStats(username, stats);
            } else {
                System.out.println("No stats available yet.");
            }
        } catch (IOException e) {
            System.out.println("No stats available yet.");
        }
    }

    private UserStats calculateUserStats(List<String> lines, String targetUsername) {
        UserStats stats = new UserStats();
        
        for (String line : lines) {
            String[] parts = line.split(",");
            if (isValidStatLine(parts)) {
                String username = parts[0];
                if (username.equals(targetUsername)) {
                    int attempts = Integer.parseInt(parts[2]);
                    String result = parts[3];
                    stats.addGame(attempts, result);
                }
            }
        }
        
        return stats;
    }

    private boolean isValidStatLine(String[] parts) {
        return parts.length >= 4;
    }

    private void displayStats(String username, UserStats stats) {
        System.out.println("Stats for " + username + ":");
        System.out.println("Games played: " + stats.getGamesPlayed());
        System.out.println("Games won: " + stats.getGamesWon());
        System.out.printf(Locale.US, "Average attempts per game: %.1f\n", stats.getAverageAttempts());
    }

    // Inner class to manage user statistics
    private class UserStats {
        private int gamesPlayed = 0;
        private int gamesWon = 0;
        private int totalAttempts = 0;

        public void addGame(int attempts, String result) {
            gamesPlayed++;
            totalAttempts += attempts;
            
            if (WIN_RESULT.equals(result)) {
                gamesWon++;
            }
        }

        public boolean hasGames() {
            return gamesPlayed > 0;
        }

        public int getGamesPlayed() {
            return gamesPlayed;
        }

        public int getGamesWon() {
            return gamesWon;
        }

        public double getAverageAttempts() {
            return gamesPlayed > 0 ? (double) totalAttempts / gamesPlayed : 0.0;
        }
    }
}
