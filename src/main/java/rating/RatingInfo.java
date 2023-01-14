/*
 * Copyright (c)  RWTH-LeckerSchmecker
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
