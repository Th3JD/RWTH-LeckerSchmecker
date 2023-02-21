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
    private final float rating;
    private final int numVotes;
    private final boolean isEstimated;

    public RatingInfo(MainMeal meal, float rating, int numVotes, boolean isEstimated) {
        this.meal = meal;
        this.rating = rating;
        this.numVotes = numVotes;
        this.isEstimated = isEstimated;
    }

    public MainMeal getMeal() {
        return meal;
    }

    public float getRating() {
        return rating;
    }

    public int getNumVotes() {
        return numVotes;
    }

    public boolean isEstimated() {
        return isEstimated;
    }

    @Override
    public String toString() {
        return "RatingInfo{" +
                "meal=" + meal +
                ", rating=" + rating +
                ", numVotes=" + numVotes +
                ", isEstimated=" + isEstimated +
                '}';
    }
}
