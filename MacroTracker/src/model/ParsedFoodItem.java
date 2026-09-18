package model;

/**
 * A single food item extracted from a free-text meal description by
 * ClaudeFoodParser, before the user has reviewed/edited it and it has
 * been written to food_entries. Unlike FoodEntry, this has no entryId
 * or entryDate yet — those are assigned at save time.
 */
public class ParsedFoodItem {

    private String foodName;
    private int calories;
    private double protein;
    private double carbs;
    private double fat;
    private String mealType;

    public ParsedFoodItem(String foodName, int calories, double protein,
                          double carbs, double fat, String mealType) {
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.mealType = mealType;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getFat() {
        return fat;
    }

    public void setFat(double fat) {
        this.fat = fat;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
}
