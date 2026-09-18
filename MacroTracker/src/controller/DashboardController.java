package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardController {

    @FXML private Label welcomeLabel;

    @FXML private Label caloriesLabel;
    @FXML private Label proteinLabel;
    @FXML private Label carbsLabel;
    @FXML private Label fatLabel;

    @FXML private Label calorieGoalLabel;
    @FXML private Label proteinGoalLabel;
    @FXML private Label carbsGoalLabel;
    @FXML private Label fatGoalLabel;

    @FXML private TextField caloriesBurnedField;
    @FXML private Label burnedTodayLabel;

    @FXML private TextArea latestFoodArea;

    @FXML private TextArea breakfastArea;
    @FXML private TextArea lunchArea;
    @FXML private TextArea dinnerArea;
    @FXML private TextArea snackArea;

    private final int userId = 1;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, Joe!");
        loadDashboardData();
    }

    private void loadDashboardData() {
        loadTodayMacros();
        loadGoals();
        loadCaloriesBurned();
        loadLatestFood();
        loadMealLogs();
    }

    private void loadTodayMacros() {
        String sql =
                "SELECT " +
                "COALESCE(f.total_calories, 0) - COALESCE(b.total_burned, 0) AS net_calories, " +
                "COALESCE(f.total_protein, 0) AS total_protein, " +
                "COALESCE(f.total_carbs, 0) AS total_carbs, " +
                "COALESCE(f.total_fats, 0) AS total_fats " +
                "FROM " +
                "(SELECT " +
                "   COALESCE(SUM(calories), 0) AS total_calories, " +
                "   COALESCE(SUM(protein), 0) AS total_protein, " +
                "   COALESCE(SUM(carbs), 0) AS total_carbs, " +
                "   COALESCE(SUM(fats), 0) AS total_fats " +
                " FROM food_entries " +
                " WHERE user_id = ? AND entry_date = DATE('now')) f " +
                "CROSS JOIN " +
                "(SELECT COALESCE(SUM(calories_burned), 0) AS total_burned " +
                " FROM calories_burned " +
                " WHERE user_id = ? AND burned_date = DATE('now')) b";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int netCalories = rs.getInt("net_calories");

                if (netCalories < 0) {
                    netCalories = 0;
                }

                caloriesLabel.setText(String.valueOf(netCalories));
                proteinLabel.setText(String.format("%.1f g", rs.getDouble("total_protein")));
                carbsLabel.setText(String.format("%.1f g", rs.getDouble("total_carbs")));
                fatLabel.setText(String.format("%.1f g", rs.getDouble("total_fats")));
            } else {
                caloriesLabel.setText("0");
                proteinLabel.setText("0.0 g");
                carbsLabel.setText("0.0 g");
                fatLabel.setText("0.0 g");
            }

        } catch (Exception e) {
            caloriesLabel.setText("0");
            proteinLabel.setText("0.0 g");
            carbsLabel.setText("0.0 g");
            fatLabel.setText("0.0 g");
            e.printStackTrace();
        }
    }

    private void loadGoals() {
        String sql = "SELECT calories_goal, protein_goal, carbs_goal, fat_goal FROM goals WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                calorieGoalLabel.setText(String.valueOf(rs.getInt("calories_goal")));
                proteinGoalLabel.setText(String.format("%.1f g", rs.getDouble("protein_goal")));
                carbsGoalLabel.setText(String.format("%.1f g", rs.getDouble("carbs_goal")));
                fatGoalLabel.setText(String.format("%.1f g", rs.getDouble("fat_goal")));
            } else {
                calorieGoalLabel.setText("0");
                proteinGoalLabel.setText("0.0 g");
                carbsGoalLabel.setText("0.0 g");
                fatGoalLabel.setText("0.0 g");
            }

        } catch (Exception e) {
            calorieGoalLabel.setText("0");
            proteinGoalLabel.setText("0.0 g");
            carbsGoalLabel.setText("0.0 g");
            fatGoalLabel.setText("0.0 g");
            e.printStackTrace();
        }
    }

    private void loadCaloriesBurned() {
        String sql = "SELECT COALESCE(SUM(calories_burned), 0) AS total_burned " +
                     "FROM calories_burned " +
                     "WHERE user_id = ? AND burned_date = DATE('now')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                burnedTodayLabel.setText(String.valueOf(rs.getInt("total_burned")));
            } else {
                burnedTodayLabel.setText("0");
            }

        } catch (Exception e) {
            burnedTodayLabel.setText("0");
            e.printStackTrace();
        }
    }

    private void loadLatestFood() {
        String sql = "SELECT food_name, meal_type, calories, protein, carbs, fats " +
                     "FROM food_entries " +
                     "WHERE user_id = ? AND entry_date = DATE('now') " +
                     "ORDER BY entry_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String text = "Food: " + rs.getString("food_name") + "\n"
                        + "Meal: " + rs.getString("meal_type") + "\n"
                        + "Calories: " + rs.getInt("calories") + "\n"
                        + "Protein: " + String.format("%.1f", rs.getDouble("protein")) + " g\n"
                        + "Carbs: " + String.format("%.1f", rs.getDouble("carbs")) + " g\n"
                        + "Fat: " + String.format("%.1f", rs.getDouble("fats")) + " g";
                latestFoodArea.setText(text);
            } else {
                latestFoodArea.setText("No food added today.");
            }

        } catch (Exception e) {
            latestFoodArea.setText("No food added today.");
            e.printStackTrace();
        }
    }

    private void loadMealLogs() {
        breakfastArea.setText(loadMealFoods("Breakfast"));
        lunchArea.setText(loadMealFoods("Lunch"));
        dinnerArea.setText(loadMealFoods("Dinner"));
        snackArea.setText(loadMealFoods("Snack"));
    }

    private String loadMealFoods(String mealType) {
        StringBuilder foods = new StringBuilder();

        String sql = "SELECT food_name, calories, protein, carbs, fats " +
                     "FROM food_entries " +
                     "WHERE user_id = ? AND entry_date = DATE('now') AND meal_type = ? " +
                     "ORDER BY entry_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, mealType);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                foods.append(rs.getString("food_name")).append("\n");
                foods.append("Cal: ").append(rs.getInt("calories")).append("\n");
                foods.append("P: ").append(String.format("%.1f", rs.getDouble("protein"))).append(" g\n");
                foods.append("C: ").append(String.format("%.1f", rs.getDouble("carbs"))).append(" g\n");
                foods.append("F: ").append(String.format("%.1f", rs.getDouble("fats"))).append(" g\n\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (foods.length() == 0) {
            return "No " + mealType.toLowerCase() + " added.";
        }

        return foods.toString().trim();
    }

    @FXML
    private void handleAddBurned() {
        String input = caloriesBurnedField.getText();

        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            int burned = Integer.parseInt(input.trim());

            String sql = "INSERT INTO calories_burned (user_id, calories_burned, burned_date) VALUES (?, ?, DATE('now'))";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setInt(2, burned);
                stmt.executeUpdate();
            }

            caloriesBurnedField.clear();
            loadDashboardData();

        } catch (NumberFormatException e) {
            caloriesBurnedField.clear();
            caloriesBurnedField.setPromptText("Enter a number");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        String sql = "DELETE FROM calories_burned WHERE user_id = ? AND burned_date = DATE('now')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();

            caloriesBurnedField.clear();
            loadDashboardData();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleResetDay() {
        String deleteFoodSql = "DELETE FROM food_entries WHERE user_id = ? AND entry_date = DATE('now')";
        String deleteBurnedSql = "DELETE FROM calories_burned WHERE user_id = ? AND burned_date = DATE('now')";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement foodStmt = conn.prepareStatement(deleteFoodSql);
                 PreparedStatement burnedStmt = conn.prepareStatement(deleteBurnedSql)) {

                foodStmt.setInt(1, userId);
                foodStmt.executeUpdate();

                burnedStmt.setInt(1, userId);
                burnedStmt.executeUpdate();

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }

            clearDashboardFields();
            loadDashboardData();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearDashboardFields() {
        caloriesLabel.setText("0");
        proteinLabel.setText("0.0 g");
        carbsLabel.setText("0.0 g");
        fatLabel.setText("0.0 g");

        burnedTodayLabel.setText("0");

        latestFoodArea.setText("No food added today.");

        breakfastArea.setText("No breakfast added.");
        lunchArea.setText("No lunch added.");
        dinnerArea.setText("No dinner added.");
        snackArea.setText("No snack added.");
    }

    @FXML
    private void handleAddFood() {
        openPage("/view/addfood.fxml", "Add Food");
    }

    @FXML
    private void handleViewHistory() {
        openPage("/view/viewhistory.fxml", "View History");
    }

    @FXML
    private void handleSetGoals() {
        openPage("/view/setgoals.fxml", "Set Goals");
    }

    private void openPage(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) caloriesLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}