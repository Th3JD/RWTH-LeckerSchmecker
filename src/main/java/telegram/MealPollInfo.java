package telegram;

import database.DatabaseManager;
import meal.MainMeal;

import java.util.HashSet;
import java.util.Set;

public class MealPollInfo {

    private final Set<MainMeal> mealsWaitingForID = new HashSet<>();

    public void addMeal(MainMeal meal) {
        mealsWaitingForID.add(meal);
    }

    public int updateMeals(String chosenMealName) {
        int id;
        if (chosenMealName.equals("Neues Gericht")) {
            id = DatabaseManager.addMeal(mealsWaitingForID.iterator().next());
        } else {
            id = Integer.parseInt(chosenMealName.trim());
            DatabaseManager.addAliasToMeal(id, this.getMealName());
        }
        for (MainMeal meal : mealsWaitingForID) {
            meal.setId(id);
        }
        return mealsWaitingForID.size();
    }

    public String getMealName() {
        return mealsWaitingForID.iterator().next().getName();
    }


}
