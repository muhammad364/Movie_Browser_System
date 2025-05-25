package org.example;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class Main extends JFrame {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private ObjectId currentUserId;
    private JTable movieTable;
    private JTextField searchField;
    private DefaultTableModel tableModel;
    private JButton searchButton;
    private JButton addMovieButton;
    private JButton addToWatchlistButton;
    private JButton rateMovieButton;
    private JButton showWatchlistButton;
    private JButton showRatedMoviesButton;

    public Main() {
        initializeDatabase();
        showLoginScreen();
        initializeGUI();
    }

    private void initializeDatabase() {
        try {
            // Connect to MongoDB
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("Movie-Browser");

            // Ensure indexes exist
            database.getCollection("Users").createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
            database.getCollection("Users").createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));

            // Initialize collections if they don't exist
            if (!database.listCollectionNames().into(new ArrayList<>()).contains("Movies")) {
                database.createCollection("Movies");
            }
            if (!database.listCollectionNames().into(new ArrayList<>()).contains("Users")) {
                database.createCollection("Users");
            }
            if (!database.listCollectionNames().into(new ArrayList<>()).contains("Ratings")) {
                database.createCollection("Ratings");
            }
            if (!database.listCollectionNames().into(new ArrayList<>()).contains("Watchlist")) {
                database.createCollection("Watchlist");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeGUI() {
        setTitle("Movie Browser");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Create the table model with columns
        String[] columns = {"ID", "Title", "Release Date", "Genre", "Director", "Rating"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movieTable = new JTable(tableModel);
        movieTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(30);
        searchButton = new JButton("Search");
        searchPanel.add(new JLabel("Search Movies: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        addMovieButton = new JButton("Add Movie");
        addToWatchlistButton = new JButton("Add to Watchlist");
        rateMovieButton = new JButton("Rate Movie");
        showWatchlistButton = new JButton("Show Watchlist");
        showRatedMoviesButton = new JButton("Show Rated Movies");

        buttonPanel.add(addMovieButton);
        buttonPanel.add(addToWatchlistButton);
        buttonPanel.add(rateMovieButton);
        buttonPanel.add(showWatchlistButton);
        buttonPanel.add(showRatedMoviesButton);

        // Add components to frame
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(movieTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        searchButton.addActionListener(e -> searchMovies());
        addMovieButton.addActionListener(e -> showAddMovieDialog());
        addToWatchlistButton.addActionListener(e -> addToWatchlist());
        rateMovieButton.addActionListener(e -> showRateDialog());
        showWatchlistButton.addActionListener(e -> showWatchlist());
        showRatedMoviesButton.addActionListener(e -> showRatedMovies());

        // Set minimum sizes for better appearance
        searchField.setPreferredSize(new Dimension(200, 25));
        movieTable.setRowHeight(25);
    }

    private void showLoginScreen() {
        JDialog loginDialog = new JDialog(this, "Login", true);
        loginDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JTextField emailField = new JTextField(20);
        JButton loginButton = new JButton("Login");
        JButton signupButton = new JButton("Sign Up");

        // Layout components
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginDialog.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        loginDialog.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        loginDialog.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        loginDialog.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        loginDialog.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        loginDialog.add(emailField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginDialog.add(buttonPanel, gbc);

        // Login button action
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog,
                        "Username and password are required!",
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (loginUser(username, password)) {
                loginDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(loginDialog,
                        "Invalid username or password!",
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Sign up button action
        signupButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String email = emailField.getText();

            if (username.trim().isEmpty() || password.trim().isEmpty() || email.trim().isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog,
                        "All fields are required!",
                        "Sign Up Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (signupUser(username, password, email)) {
                JOptionPane.showMessageDialog(loginDialog,
                        "Sign-up successful! Please log in.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                usernameField.setText("");
                passwordField.setText("");
                emailField.setText("");
            }
        });

        loginDialog.setSize(400, 250);
        loginDialog.setLocationRelativeTo(this);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setVisible(true);
    }

    private boolean loginUser(String username, String password) {
        try {
            MongoCollection<Document> users = database.getCollection("Users");
            // In loginUser() method
            Document user = users.find(
                    and(
                            eq("username", username),
                            eq("password", password)  // Comparing plain text passwords
                    )
            ).first();

            if (user != null) {
                currentUserId = user.getObjectId("_id");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Login error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private boolean signupUser(String username, String password, String email) {
        try {
            MongoCollection<Document> users = database.getCollection("Users");

            // Check for existing user
            Document existingUser = users.find(
                    or(
                            eq("username", username),
                            eq("email", email)
                    )
            ).first();

            if (existingUser != null) {
                JOptionPane.showMessageDialog(this,
                        "Username or Email already exists!",
                        "Sign Up Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // In signupUser() method
            Document newUser = new Document()
                    .append("username", username)
                    .append("password", password)  // Storing password in plain text
                    .append("email", email)
                    .append("createdAt", new Date());

            users.insertOne(newUser);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error during signup: " + e.getMessage(),
                    "Sign Up Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private void searchMovies() {
        String searchTerm = searchField.getText().trim();
        tableModel.setRowCount(0);

        try {
            MongoCollection<Document> movies = database.getCollection("Movies");
            Document searchQuery = new Document();

            if (!searchTerm.isEmpty()) {
                searchQuery.append("title",
                        new Document("$regex", searchTerm)
                                .append("$options", "i"));
            }

            FindIterable<Document> results = movies.find(searchQuery);
            for (Document movie : results) {
                Object[] row = {
                        movie.getObjectId("_id").toString(),
                        movie.getString("title"),
                        movie.getString("releaseDate"),
                        movie.getString("genre"),
                        movie.getString("director"),
                        getAverageRating(movie.getObjectId("_id"))
                };
                tableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error searching movies: " + e.getMessage(),
                    "Search Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Double getAverageRating(ObjectId movieId) {
        try {
            MongoCollection<Document> ratings = database.getCollection("Ratings");
            List<Document> pipeline = Arrays.asList(
                    new Document("$match", new Document("movieId", movieId)),
                    new Document("$group", new Document("_id", null)
                            .append("averageRating", new Document("$avg", "$rating")))
            );

            Document result = ratings.aggregate(pipeline).first();
            if (result != null) {
                return Math.round(result.getDouble("averageRating") * 10.0) / 10.0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void showAddMovieDialog() {
        JDialog dialog = new JDialog(this, "Add Movie", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = new JTextField(20);
        JTextField releaseDateField = new JTextField(20);
        JTextField genreField = new JTextField(20);
        JTextField directorField = new JTextField(20);

        // Add components to dialog
        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        dialog.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Release Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dialog.add(releaseDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("Genre:"), gbc);
        gbc.gridx = 1;
        dialog.add(genreField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dialog.add(new JLabel("Director:"), gbc);
        gbc.gridx = 1;
        dialog.add(directorField, gbc);

        JButton saveButton = new JButton("Save");
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        dialog.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                Document movie = new Document()
                        .append("title", titleField.getText())
                        .append("releaseDate", releaseDateField.getText())
                        .append("genre", genreField.getText())
                        .append("director", directorField.getText())
                        .append("addedDate", new Date());

                database.getCollection("Movies").insertOne(movie);
                dialog.dispose();
                searchMovies(); // Refresh the movie list
                JOptionPane.showMessageDialog(this, "Movie added successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                        "Error adding movie: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addToWatchlist() {
        int selectedRow = movieTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a movie first!");
            return;
        }

        try {
            String movieIdStr = (String) movieTable.getValueAt(selectedRow, 0);
            ObjectId movieId = new ObjectId(movieIdStr);

            MongoCollection<Document> watchlist = database.getCollection("Watchlist");

            // Check if movie is already in watchlist
            Document existing = watchlist.find(
                    and(
                            eq("userId", currentUserId),
                            eq("movieId", movieId)
                    )
            ).first();

            if (existing != null) {
                JOptionPane.showMessageDialog(this, "Movie is already in your watchlist!");
                return;
            }

            Document watchlistEntry = new Document()
                    .append("userId", currentUserId)
                    .append("movieId", movieId)
                    .append("addedDate", new Date());

            watchlist.insertOne(watchlistEntry);
            JOptionPane.showMessageDialog(this, "Movie added to watchlist successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error adding to watchlist: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRateDialog() {
        int selectedRow = movieTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a movie to rate!");
            return;
        }

        JDialog dialog = new JDialog(this, "Rate Movie", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        String[] ratings = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        JComboBox<String> ratingCombo = new JComboBox<>(ratings);
        JTextArea reviewArea = new JTextArea(3, 20);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Rating:"), gbc);
        gbc.gridx = 1;
        dialog.add(ratingCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Review:"), gbc);
        gbc.gridx = 1;
        dialog.add(new JScrollPane(reviewArea), gbc);

        JButton submitButton = new JButton("Submit Rating");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        dialog.add(submitButton, gbc);

        submitButton.addActionListener(e -> {
            try {
                String movieIdStr = (String) movieTable.getValueAt(selectedRow, 0);
                ObjectId movieId = new ObjectId(movieIdStr);
                MongoCollection<Document> ratings1 = database.getCollection("Ratings");

                // Check if user has already rated this movie
                Document existingRating = ratings1.find(
                        and(
                                eq("userId", currentUserId),
                                eq("movieId", movieId)
                        )
                ).first();

                if (existingRating != null) {
                    JOptionPane.showMessageDialog(dialog, "You have already rated this movie!");
                    return;
                }

                Document rating = new Document()
                        .append("userId", currentUserId)
                        .append("movieId", movieId)
                        .append("rating", Integer.parseInt((String) ratingCombo.getSelectedItem()))
                        .append("review", reviewArea.getText())
                        .append("ratedDate", new Date());

                ratings1.insertOne(rating);
                dialog.dispose();
                searchMovies(); // Refresh to show updated rating
                JOptionPane.showMessageDialog(this, "Rating submitted successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                        "Error submitting rating: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showWatchlist() {
        tableModel.setRowCount(0);

        try {
            MongoCollection<Document> watchlist = database.getCollection("Watchlist");
            MongoCollection<Document> movies = database.getCollection("Movies");

            for (Document entry : watchlist.find(eq("userId", currentUserId))) {
                Document movie = movies.find(eq("_id", entry.getObjectId("movieId"))).first();
                if (movie != null) {
                    Object[] row = {
                            movie.getObjectId("_id").toString(),
                            movie.getString("title"),
                            movie.getString("releaseDate"),
                            movie.getString("genre"),
                            movie.getString("director"),
                            getAverageRating(movie.getObjectId("_id"))
                    };
                    tableModel.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading watchlist: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRatedMovies() {
        tableModel.setRowCount(0);

        try {
            MongoCollection<Document> ratings = database.getCollection("Ratings");
            MongoCollection<Document> movies = database.getCollection("Movies");

            for (Document rating : ratings.find(eq("userId", currentUserId))) {
                Document movie = movies.find(eq("_id", rating.getObjectId("movieId"))).first();
                if (movie != null) {
                    Object[] row = {
                            movie.getObjectId("_id").toString(),
                            movie.getString("title"),
                            movie.getString("releaseDate"),
                            movie.getString("genre"),
                            movie.getString("director"),
                            rating.getInteger("rating")
                    };
                    tableModel.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading rated movies: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main browser = new Main();
            browser.setVisible(true);
        });
    }
}