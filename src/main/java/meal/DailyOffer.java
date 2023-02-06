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

import config.Config;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.DateUtils;

public class DailyOffer {

    private final SortedSet<MainMeal> meals = new TreeSet<>(Comparator.comparing(MainMeal::getType)
            .thenComparing(Meal::getName));
    private final Set<SideMeal> sideMeals = new HashSet<>();
    private final LocalDate date;


    public DailyOffer(LocalDate date) {
        this.date = date;
    }

    public void addMeal(MainMeal meal) {
        if (meal == null) return;
        this.meals.add(meal);
    }

    public void addMeals(Collection<MainMeal> meals) {
        meals.forEach(this::addMeal);
    }

    public void addSideMeal(SideMeal meal) {
        if (meal == null) return;
        this.sideMeals.add(meal);
    }

    public static DailyOffer parseOffer(Element elementDE, Element elementEN) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy",
                Locale.GERMANY);

        Elements headLineElements = elementDE.getElementsByClass("active-headline");
        if (headLineElements.isEmpty()) {
            headLineElements = elementDE.getElementsByClass("default-headline");
        }
        String fullDate = headLineElements.get(0).text();
        LocalDate date = LocalDate.parse(fullDate, formatter);

        // Check if the date is already in the past
        if (date.isBefore(LocalDate.now())) {
            return null;
        }

        // Check if the date is too far in the future
        if (!date.isBefore(DateUtils.addDaysSkippingWeekends(LocalDate.now(), Config.getInt("meals.daysToFetch")))) {
            // Meals this far in the future are prone to contain typos and are oftentimes subject to change
            return null;
        }

        DailyOffer res = new DailyOffer(date);

        Elements htmlMealsDE = elementDE.getElementsByClass("menues").get(0)
                .getElementsByClass("menue-wrapper");
        Elements htmlMealsEN = elementEN.getElementsByClass("menues").get(0)
                .getElementsByClass("menue-wrapper");

        for (int i = 0; i < htmlMealsDE.size(); i++) {
            res.addMeals(MainMeal.parseMeal(res, htmlMealsDE.get(i), htmlMealsEN.get(i)));
        }

        Elements htmlExtrasDE = elementDE.getElementsByClass("extras").get(0)
                .getElementsByClass("menue-wrapper");
        Elements htmlExtrasEN = elementEN.getElementsByClass("extras").get(0)
                .getElementsByClass("menue-wrapper");
        for (int i = 0; i < htmlExtrasDE.size(); i++) {
            SideMeal.parseSideMeals(htmlExtrasDE.get(i), htmlExtrasEN.get(i))
                    .forEach(res::addSideMeal);
        }

        return res;
    }

    public Set<MainMeal> getMainMeals() {
        return meals;
    }

    public Set<SideMeal> getSideMeals() {
        return sideMeals;
    }

    public Set<SideMeal> getSideMeals(SideMeal.Type type) {
        return sideMeals.stream().filter(m -> m.getType().equals(type)).collect(Collectors.toSet());
    }

    public Optional<MainMeal> getMainMealByDisplayName(String displayName, Locale locale) {
        if (displayName == null) {
            return Optional.empty();
        }
        return this.getMainMeals().stream()
                .filter(m -> displayName.equals(m.getDisplayName(locale)))
                .findFirst();
    }

    public LocalDate getDate() {
        return date;
    }

}
