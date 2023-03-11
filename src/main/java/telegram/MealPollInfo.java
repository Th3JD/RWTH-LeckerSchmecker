/*
 * RWTH-LeckerSchmecker
 * Copyright (c) 2023 Th3JD, ekansemit, 3dde
 *
 * This file is part of RWTH-LeckerSchmecker.
 *
 * RWTH-LeckerSchmecker is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with RWTH-LeckerSchmecker.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package telegram;

import database.DatabaseManager;
import java.util.HashSet;
import java.util.Set;
import meal.MainMeal;

public class MealPollInfo {

    private final Set<MainMeal> mealsWaitingForID = new HashSet<>();

    public void addMeal(MainMeal meal) {
        mealsWaitingForID.add(meal);
    }

    public int updateMeals(String chosenMealID) {
        int id = Integer.parseInt(chosenMealID.trim());
        if (id == 0) {
            id = DatabaseManager.addMeal(mealsWaitingForID.iterator().next());
        } else {
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
