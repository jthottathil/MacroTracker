package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.DatabaseConnection;

public class SetGoalsController {

    @FXML
    private TextField caloriesGoalField;

    @FXML
    private TextField proteinGoalField;

    @FXML
    private TextField carbsGoalField;

    @FXML
    private TextField fatGoalField;

    @FXML
    private Label messageLabel;

    private final int userId = 1;

    @FXML
    public void initialize() {
        loadGoals();
    }

    @FXML
    private void handleSaveGoals() {
        try {
            int calories = Integer.parseInt(caloriesGoalField.getText().trim());
            double protein = Double.parseDouble(proteinGoalField.getText().trim());
            double carbs = Double.parseDouble(carbsGoalField.getText().trim());
            double fat = Double.parseDouble(fatGoalField.getText().trim());

            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO goals (user_id, calories_goal, protein_goal, carbs_goal, fat_goal) "
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT(user_id) DO UPDATE SET "
                    + "calories_goal = excluded.calories_goal, "
                    + "protein_goal = excluded.protein_goal, "
                    + "carbs_goal = excluded.carbs_goal, "
                    + "fat_goal = excluded.fat_goal";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, calories);
            stmt.setDouble(3, protein);
            stmt.setDouble(4, carbs);
            stmt.setDouble(5, fat);

            stmt.executeUpdate();

            messageLabel.setText("Goals saved successfully.");

        } catch (NumberFormatException e) {
            messageLabel.setText("Please enter valid numbers.");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error saving goals.");
        }
    }

    private void loadGoals() {
        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT calories_goal, protein_goal, carbs_goal, fat_goal "
                    + "FROM goals WHERE user_id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                caloriesGoalField.setText(String.valueOf(rs.getInt("calories_goal")));
                proteinGoalField.setText(String.valueOf(rs.getDouble("protein_goal")));
                carbsGoalField.setText(String.valueOf(rs.getDouble("carbs_goal")));
                fatGoalField.setText(String.valueOf(rs.getDouble("fat_goal")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Could not load goals.");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) caloriesGoalField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Could not return to dashboard.");
        }
    }
}