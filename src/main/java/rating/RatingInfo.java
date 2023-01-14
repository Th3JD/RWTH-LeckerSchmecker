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

package rating;

import meal.MainMeal;

public class RatingInfo {

    private final MainMeal meal;
    private final float averageRating;
    private final int numVotes;

    public RatingInfo(MainMeal meal, float averageRating, int numVotes) {
        this.meal = meal;
        this.averageRating = averageRating;
        this.numVotes = numVotes;
    }

    public MainMeal getMeal() {
        return meal;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public int getNumVotes() {
        return numVotes;
    }

    @Override
    public String toString() {
        return "RatingInfo{" +
                "meal=" + meal +
                ", averageRating=" + averageRating +
                ", numVotes=" + numVotes +
                '}';
    }
}
