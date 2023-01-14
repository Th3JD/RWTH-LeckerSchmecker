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

package meal;

import localization.ResourceManager;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum DietType {

    VEGAN("vegan", "vegan"),
    VEGETARIAN("vegetarian", "vegetarian"),
    NOPORK("no_pork", "nopork"),
    NOFISH("no_fish", "nofish"),
    EVERYTHING("eating_everything", "all");

    public static final List<DietType> TYPES = List.of(VEGAN, VEGETARIAN, NOPORK, NOFISH,
            EVERYTHING);

    private final String bundleKey;
    private final String id;

    DietType(String bundleKey, String id) {
        this.bundleKey = bundleKey;
        this.id = id;
    }

    public static Optional<DietType> getById(String name) {
        return TYPES.stream().filter(c -> c.getId().equalsIgnoreCase(name)).findFirst();
    }

    public static Optional<DietType> getByDisplayName(String displayName, Locale locale) {
        return TYPES.stream().filter(c -> c.getDisplayName(locale).equalsIgnoreCase(displayName)).findFirst();
    }

    public static boolean filterDietType(List<Nutrition> nutritions, DietType dietType) {
        if (dietType != null) {
            boolean hasVegan = false;
            boolean hasVegetarian = false;
            for (Nutrition nutrition : nutritions) {
                switch (dietType) {
                    case EVERYTHING:
                        return false; //Do not need to check any further
                    case NOPORK:
                        if (nutrition.equals(Nutrition.PORK)) {
                            return true;
                        }
                        break;
                    case NOFISH:
                        if (nutrition.equals(Nutrition.FISH)) {
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                if (nutrition.equals(Nutrition.VEGAN)) { hasVegan = true; }
                if (nutrition.equals(Nutrition.VEGETARIAN)) { hasVegetarian = true; }
            }
            // Still need to check for vegan and vegetarian
            switch (dietType) {
                case VEGAN:
                    if (!hasVegan) {
                        return true;
                    }
                    break;
                case VEGETARIAN:
                    if (!hasVegetarian) {
                        return true;
                    }
                    break;
                default:
                    break; //Do not skip this meal
            }
            return false;
        } else {
            return false;
        }
    }
    public String getDisplayName(Locale locale) {
        return ResourceManager.getString(bundleKey, locale);
    }
    public String getId() {
        return id;
    }
}
