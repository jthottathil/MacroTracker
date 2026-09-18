package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import model.DatabaseConnection;
import model.FoodEntry;
import model.ParsedFoodItem;
import service.ClaudeFoodParser;

public class AddFoodController {

    private static final List<String> MEAL_TYPES = List.of("Breakfast", "Lunch", "Dinner", "Snack");

    @FXML
    private TextField foodNameField;

    @FXML
    private TextField caloriesField;

    @FXML
    private TextField proteinField;

    @FXML
    private TextField carbsField;

    @FXML
    private TextField fatsField;

    @FXML
    private ComboBox<String> mealTypeComboBox;

    @FXML
    private DatePicker entryDatePicker;

    @FXML
    private Label messageLabel;

    // AI-assisted natural-language logging

    @FXML
    private TextArea mealDescriptionField;

    @FXML
    private Button parseAiButton;

    @FXML
    private Button addAllButton;

    @FXML
    private TableView<ParsedFoodItem> parsedItemsTable;

    @FXML
    private TableColumn<ParsedFoodItem, String> parsedFoodNameColumn;

    @FXML
    private TableColumn<ParsedFoodItem, Integer> parsedCaloriesColumn;

    @FXML
    private TableColumn<ParsedFoodItem, Double> parsedProteinColumn;

    @FXML
    private TableColumn<ParsedFoodItem, Double> parsedCarbsColumn;

    @FXML
    private TableColumn<ParsedFoodItem, Double> parsedFatColumn;

    @FXML
    private TableColumn<ParsedFoodItem, String> parsedMealTypeColumn;

    private final int userId = 1;

    private boolean editMode = false;
    private int editingEntryId;

    private final ClaudeFoodParser foodParser = new ClaudeFoodParser();

    @FXML
    public void initialize() {
        mealTypeComboBox.getItems().addAll(MEAL_TYPES);
        setUpParsedItemsTable();
    }

    private void setUpParsedItemsTable() {
        parsedItemsTable.setItems(FXCollections.observableArrayList());
        parsedItemsTable.setEditable(true);

        parsedFoodNameColumn.setCellValueFactory(new PropertyValueFactory<>("foodName"));
        parsedCaloriesColumn.setCellValueFactory(new PropertyValueFactory<>("calories"));
        parsedProteinColumn.setCellValueFactory(new PropertyValueFactory<>("protein"));
        parsedCarbsColumn.setCellValueFactory(new PropertyValueFactory<>("carbs"));
        parsedFatColumn.setCellValueFactory(new PropertyValueFactory<>("fat"));
        parsedMealTypeColumn.setCellValueFactory(new PropertyValueFactory<>("mealType"));

        parsedFoodNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        parsedFoodNameColumn.setOnEditCommit(event -> {
            event.getRowValue().setFoodName(event.getNewValue());
            parsedItemsTable.refresh();
        });

        parsedCaloriesColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        parsedCaloriesColumn.setOnEditCommit(event -> {
            event.getRowValue().setCalories(event.getNewValue());
            parsedItemsTable.refresh();
        });

        parsedProteinColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        parsedProteinColumn.setOnEditCommit(event -> {
            event.getRowValue().setProtein(event.getNewValue());
            parsedItemsTable.refresh();
        });

        parsedCarbsColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        parsedCarbsColumn.setOnEditCommit(event -> {
            event.getRowValue().setCarbs(event.getNewValue());
            parsedItemsTable.refresh();
        });

        parsedFatColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        parsedFatColumn.setOnEditCommit(event -> {
            event.getRowValue().setFat(event.getNewValue());
            parsedItemsTable.refresh();
        });

        parsedMealTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(MEAL_TYPES)));
        parsedMealTypeColumn.setOnEditCommit(event -> {
            event.getRowValue().setMealType(event.getNewValue());
            parsedItemsTable.refresh();
        });
    }

    public void setFoodEntryToEdit(FoodEntry entry) {
        editMode = true;
        editingEntryId = entry.getEntryId();

        foodNameField.setText(entry.getFoodName());
        caloriesField.setText(String.valueOf(entry.getCalories()));
        proteinField.setText(String.valueOf(entry.getProtein()));
        carbsField.setText(String.valueOf(entry.getCarbs()));
        fatsField.setText(String.valueOf(entry.getFat()));
        mealTypeComboBox.setValue(entry.getMealType());
        entryDatePicker.setValue(LocalDate.parse(entry.getEntryDate()));

        messageLabel.setStyle("-fx-text-fill: blue;");
        messageLabel.setText("Editing food entry.");
    }

    // ---------- AI-assisted natural-language logging ----------

    @FXML
    private void handleParseWithAi() {
        String description = mealDescriptionField.getText();

        if (description == null || description.trim().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Describe what you ate first.");
            return;
        }

        parseAiButton.setDisable(true);
        addAllButton.setDisable(true);
        messageLabel.setStyle("-fx-text-fill: #1f3349;");
        messageLabel.setText("Asking Claude to parse that meal...");

        Task<List<ParsedFoodItem>> parseTask = new Task<>() {
            @Override
            protected List<ParsedFoodItem> call() throws Exception {
                return foodParser.parse(description);
            }
        };

        parseTask.setOnSucceeded(event -> {
            ObservableList<ParsedFoodItem> items = FXCollections.observableArrayList(parseTask.getValue());
            parsedItemsTable.setItems(items);

            parseAiButton.setDisable(false);
            addAllButton.setDisable(false);

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Parsed " + items.size() + " item(s). Review and edit below, then Add All to Log.");
        });

        parseTask.setOnFailed(event -> {
            parseAiButton.setDisable(false);
            addAllButton.setDisable(false);

            Throwable ex = parseTask.getException();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(ex != null ? ex.getMessage() : "Could not parse that meal description.");
        });

        Thread thread = new Thread(parseTask, "claude-food-parser");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleAddAllParsed() {
        List<ParsedFoodItem> items = parsedItemsTable.getItems();

        if (items.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Nothing to add yet — parse a meal description first.");
            return;
        }

        if (entryDatePicker.getValue() == null) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Pick a date before adding these to your log.");
            return;
        }

        try {
            insertParsedItems(items, entryDatePicker.getValue());

            int count = items.size();
            parsedItemsTable.setItems(FXCollections.observableArrayList());
            mealDescriptionField.clear();

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Added " + count + " item(s) to your log.");

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Error saving parsed items.");
        }
    }

    private void insertParsedItems(List<ParsedFoodItem> items, LocalDate entryDate) throws Exception {
        String sql = "INSERT INTO food_entries "
                + "(user_id, food_name, calories, protein, carbs, fats, meal_type, entry_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (ParsedFoodItem item : items) {
                stmt.setInt(1, userId);
                stmt.setString(2, item.getFoodName());
                stmt.setInt(3, item.getCalories());
                stmt.setDouble(4, item.getProtein());
                stmt.setDouble(5, item.getCarbs());
                stmt.setDouble(6, item.getFat());
                stmt.setString(7, item.getMealType());
                stmt.setString(8, entryDate.toString());
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    // ---------- Manual entry (unchanged) ----------

    @FXML
    private void handleSaveFood() {
        try {
            String foodName = foodNameField.getText().trim();
            String mealType = mealTypeComboBox.getValue();

            if (foodName.isEmpty()
                    || caloriesField.getText().trim().isEmpty()
                    || proteinField.getText().trim().isEmpty()
                    || carbsField.getText().trim().isEmpty()
                    || fatsField.getText().trim().isEmpty()
                    || mealType == null
                    || entryDatePicker.getValue() == null) {

                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Please fill in all fields.");
                return;
            }

            int calories = Integer.parseInt(caloriesField.getText().trim());
            double protein = Double.parseDouble(proteinField.getText().trim());
            double carbs = Double.parseDouble(carbsField.getText().trim());
            double fats = Double.parseDouble(fatsField.getText().trim());

            if (editMode) {
                updateFood(foodName, calories, protein, carbs, fats, mealType);
            } else {
                insertFood(foodName, calories, protein, carbs, fats, mealType);
            }

        } catch (NumberFormatException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Please enter valid numbers.");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Error saving food.");
        }
    }

    private void insertFood(String foodName, int calories, double protein,
                            double carbs, double fats, String mealType) throws Exception {

        Connection conn = DatabaseConnection.getConnection();

        String sql = "INSERT INTO food_entries "
                + "(user_id, food_name, calories, protein, carbs, fats, meal_type, entry_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = conn.prepareStatement(sql);

        stmt.setInt(1, userId);
        stmt.setString(2, foodName);
        stmt.setInt(3, calories);
        stmt.setDouble(4, protein);
        stmt.setDouble(5, carbs);
        stmt.setDouble(6, fats);
        stmt.setString(7, mealType);
        stmt.setString(8, entryDatePicker.getValue().toString());

        stmt.executeUpdate();

        stmt.close();
        conn.close();

        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText("Food added successfully.");

        clearFields();
    }

    private void updateFood(String foodName, int calories, double protein,
                            double carbs, double fats, String mealType) throws Exception {

        Connection conn = DatabaseConnection.getConnection();

        String sql = "UPDATE food_entries "
                + "SET food_name = ?, calories = ?, protein = ?, carbs = ?, fats = ?, meal_type = ?, entry_date = ? "
                + "WHERE entry_id = ? AND user_id = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);

        stmt.setString(1, foodName);
        stmt.setInt(2, calories);
        stmt.setDouble(3, protein);
        stmt.setDouble(4, carbs);
        stmt.setDouble(5, fats);
        stmt.setString(6, mealType);
        stmt.setString(7, entryDatePicker.getValue().toString());
        stmt.setInt(8, editingEntryId);
        stmt.setInt(9, userId);

        stmt.executeUpdate();

        stmt.close();
        conn.close();

        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText("Food updated successfully.");
    }

    private void clearFields() {
        foodNameField.clear();
        caloriesField.clear();
        proteinField.clear();
        carbsField.clear();
        fatsField.clear();
        mealTypeComboBox.setValue(null);
        entryDatePicker.setValue(null);
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) foodNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Could not return to dashboard.");
        }
    }
}
