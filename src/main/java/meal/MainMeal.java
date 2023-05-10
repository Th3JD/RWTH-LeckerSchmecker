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

import database.DatabaseManager;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import localization.ResourceManager;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import telegram.LeckerSchmeckerBot;
import util.Triple;
import util.Tuple;

public class MainMeal extends Meal {

    private static final float MIN_MATCH = 0.5f;
    private static final float MIN_LONGEST_MATCH = 2f;

    private static final Set<String> INVALID_NAMES = Set.of("geschlossen");

    private static final String NO_NUTRITION_SYMBOL = "\uD83C\uDF74";

    private static final String MEAL_SEPARATOR_DE = "ODER";
    private static final String MEAL_SEPARATOR_EN = "OR";

    private final Type type;
    private final float price;
    private final List<Nutrition> nutritions;
    private Integer id;

    public MainMeal(Builder builder) {
        super(builder.name, builder.displayNameDE, builder.displayNameEN);
        this.type = builder.type;
        this.price = builder.price;
        this.nutritions = builder.nutritions;
        this.id = builder.id;
    }

    public static List<MainMeal> parseMeal(DailyOffer offer, Element elementDE, Element elementEN) {

        // parse from html
        String htmlNameDE = elementDE.getElementsByClass("expand-nutr").get(0).ownText();
        String htmlNameEN = elementEN.getElementsByClass("expand-nutr").get(0).ownText();

        if (htmlNameDE.isBlank() || htmlNameDE.strip().startsWith("|")
                || INVALID_NAMES.stream().anyMatch(s -> s.equalsIgnoreCase(htmlNameDE))) {
            LeckerSchmecker.getLogger()
                    .warning("Encountered meal with an invalid name: " + htmlNameDE);
            return List.of();
        }

        String category = elementDE.getElementsByClass("menue-category").get(0).ownText();
        Type type = Type.getMealTypeFromCategory(category, elementDE);

        if (type == null) {
            LeckerSchmecker.getLogger()
                    .warning("Could not parse type of meal '" + htmlNameDE + "'");
            return List.of();
        }

        Elements priceElements = elementDE.getElementsByClass("menue-price");
        float price;
        if (priceElements.size() == 1) {
            String priceStr = elementDE.getElementsByClass("menue-price").get(0).ownText();
            price = Float.parseFloat(priceStr.split(" ")[0].replace(',', '.'));
        } else {
            LeckerSchmecker.getLogger().info("Meal " + htmlNameDE + " does not have a price. "
                    + "Using default price instead.");
            price = type.getPrice();
        }

        LinkedList<Nutrition> nutritions = Nutrition.searchNutrientsFor(elementDE);

        if ((type.equals(Type.TELLERGERICHT) || type.equals(Type.TELLERGERICHT_VEGETARISCH))
                && offer.getDate().getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            nutritions.addFirst(Nutrition.SWEET);
        }

        // split name by separator
        String[] displayNamesDE = htmlNameDE.split(MEAL_SEPARATOR_DE);
        String[] displayNamesEN = htmlNameEN.split(MEAL_SEPARATOR_EN);

        // nutrition specification not possible if meal is a multi meal
        if (displayNamesDE.length > 1) {
            nutritions.clear();
        }

        // build meal basis
        MainMeal.Builder builder = new Builder()
                .setType(type)
                .setPrice(price)
                .setNutritions(nutritions);

        List<MainMeal> meals = new ArrayList<>(1);

        // create meals for each derived name
        for (int i = 0; i < displayNamesDE.length; i++) {
            String displayNameDE = displayNamesDE[i].trim();

            // try to get EN name, if not present use DE as fallback
            String displayNameEN;
            try {
                displayNameEN = displayNamesEN[i].trim();
            } catch (IndexOutOfBoundsException e) {
                LeckerSchmecker.getLogger().warning("");
                displayNameEN = displayNameDE;
            }

            String name = compress(displayNameDE);

            // set name and display names
            builder.setName(name)
                    .setDisplayNameDE(displayNameDE)
                    .setDisplayNameEN(displayNameEN);

            // database
            Integer id = DatabaseManager.loadMealIDByName(name);

            // meal already exists
            if (id != null) {
                // set id and create meal
                builder.setId(id);
                meals.add(builder.createMainMeal());
                continue;
            }

            // create meal with builder
            MainMeal meal = builder.createMainMeal();
            meals.add(meal);

            Set<Integer> similarIDs = DatabaseManager.loadMealIDsByShortAlias(meal);

            // if no short name matches,then insert meal into database, else ask admins
            if (similarIDs.isEmpty()) {
                meal.id = DatabaseManager.addMeal(meal);
            } else {
                LeckerSchmecker.getLogger()
                        .info("Found similar meal for '" + meal.getName() + "', asking admins");
                LeckerSchmeckerBot.getInstance().askAdmins(meal, similarIDs);
            }

        }

        return meals;
    }

    public String getShortAlias() {
        if (displayNameDE.contains("|")) {
            return compress(displayNameDE.split("\\|")[0]);
        }
        return name;
    }

    public Type getType() {
        return type;
    }

    public float getPrice() {
        return price;
    }

    public String text(Locale locale) {
        String symbols = this.getSymbols();
        return this.getDisplayName(locale) + (symbols.isEmpty() ? "" : " ") + symbols;
    }

    public String getSymbols() {
        if (this.nutritions.isEmpty()) {
            return NO_NUTRITION_SYMBOL;
        }
        return this.nutritions.stream().map(Nutrition::getSymbol).collect(Collectors.joining());
    }

    public List<Nutrition> getNutritions() {
        return nutritions;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    private static String compress(String displayName) {
        String res = displayName;
        res = res.replace("|", " ")
                .replace("-", " ")
                .replace(":", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replace("&", " ")
                .replace("/", " ")
                .replace("\\", " ")
                .toLowerCase()
                .replace("ß", "ss")
                .replace(" mit ", " ")
                .replace(" und ", " ")
                .replace(" oder ", " ")
                .trim()
                .replaceAll(" +", "_");
        return res;
    }

    @Override
    public String toString() {
        return "MainMeal{" +
                "type=" + type +
                ", price=" + price +
                ", name='" + name + '\'' +
                ", displayName='" + displayNameDE + '\'' +
                '}';
    }

    public enum Type {

        VEGETARISCH("mainmealtype_vegetarian", 2.2f),
        KLASSIKER("mainmealtype_classics", 2.8f),
        TELLERGERICHT_VEGETARISCH("mainmealtype_vegetarian_table_dish", 2.0f),
        TELLERGERICHT("mainmealtype_table_dish", 2.0f),
        WOK_VEGETARISCH("mainmealtype_vegetarian_wok", 3.8f),
        WOK("mainmealtype_wok", 3.8f),
        EMPFEHLUNG_DES_TAGES("mainmealtype_meal_of_the_day", 4.1f),
        PASTA("mainmealtype_pasta", 3.7f),
        PIZZA_DES_TAGES("mainmealtype_pizza_of_the_day", 3.7f),
        PIZZA_CLASSICS("mainmealtype_pizza_classics", 3.7f),
        BURGER_CLASSICS("mainmealtype_burger_classics", 4.9f),
        BURGER_DER_WOCHE("mainmealtype_burger_of_the_week", 1.8f),
        ;

        private final String bundleKey;
        private final float price;

        Type(String bundleKey, float price) {
            this.bundleKey = bundleKey;
            this.price = price;
        }

        public static Type getMealTypeFromCategory(String category, Element e) {
            switch (category) {
                case "Tellergericht vegetarisch" -> {
                    return TELLERGERICHT_VEGETARISCH;
                }
                case "Tellergericht" -> {
                    return TELLERGERICHT;
                }
                case "Vegetarisch" -> {
                    return VEGETARISCH;
                }
                case "Klassiker" -> {
                    return KLASSIKER;
                }
                case "Wok" -> {
                    List<Nutrition> nutritions = Nutrition.searchNutrientsFor(e);
                    if (nutritions.contains(Nutrition.VEGAN) || nutritions.contains(
                            Nutrition.VEGETARIAN)) {
                        return WOK_VEGETARISCH;
                    }
                    return WOK;
                }
                case "Empfehlung des Tages" -> {
                    return EMPFEHLUNG_DES_TAGES;
                }
                case "Pasta" -> {
                    return PASTA;
                }
                case "Pizza Classics" -> {
                    return PIZZA_CLASSICS;
                }
                case "Pizza des Tages" -> {
                    return PIZZA_DES_TAGES;
                }
                case "Burger Classics" -> {
                    return BURGER_CLASSICS;
                }
                case "Burger der Woche" -> {
                    return BURGER_DER_WOCHE;
                }
            }
            return null;
        }

        public String getDisplayName(Locale locale) {
            return ResourceManager.getString(bundleKey, locale);
        }

        public float getPrice() {
            return price;
        }
    }

    public static class Builder {

        private String name;
        private String displayNameDE;
        private String displayNameEN;
        private Type type;
        private float price;
        private List<Nutrition> nutritions;
        private Integer id;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDisplayNameDE(String displayNameDE) {
            this.displayNameDE = displayNameDE;
            return this;
        }

        public Builder setDisplayNameEN(String displayNameEN) {
            this.displayNameEN = displayNameEN;
            return this;
        }

        public Builder setType(Type type) {
            this.type = type;
            return this;
        }

        public Builder setPrice(float price) {
            this.price = price;
            return this;
        }

        public Builder setNutritions(List<Nutrition> nutritions) {
            this.nutritions = nutritions;
            return this;
        }

        public Builder setId(Integer id) {
            this.id = id;
            return this;
        }

        public MainMeal createMainMeal() {
            return new MainMeal(this);
        }
    }

    public static String calcNameDiff(MainMeal meal, Integer mealId) {
        String alias = DatabaseManager.getMealAliases(mealId).get(0);

        List<String> nameDiff = new LinkedList<>();

        List<String> nameParts = new LinkedList<>(Arrays.asList(meal.getName().split("_")));
        List<String> aliasParts = new LinkedList<>(Arrays.asList(alias.split("_")));

        while (!nameParts.isEmpty() || !aliasParts.isEmpty()) {
            Triple<Integer, Integer, String> matchResult = findFirstWordMatch(nameParts,
                    aliasParts);

            for (int i = 0; i < matchResult.getA(); i++) {
                nameDiff.add("<s>" + nameParts.get(i) + "</s>");
            }

            for (int i = 0; i < matchResult.getB(); i++) {
                nameDiff.add("<u>" + aliasParts.get(i) + "</u>");
            }

            if (matchResult.getC() != null) {
                nameDiff.add(aliasParts.get(matchResult.getB()));
            }

            if (matchResult.getB() < aliasParts.size()) {
                aliasParts.subList(0, matchResult.getB() + 1).clear();
            } else {
                aliasParts.clear();
            }

            if (matchResult.getA() < nameParts.size()) {
                nameParts.subList(0, matchResult.getA() + 1).clear();
            } else {
                nameParts.clear();
            }
        }

        return String.join("_", nameDiff);
    }

    private static Triple<Integer, Integer, String> findFirstWordMatch(List<String> name,
            List<String> alias) {
        for (int j = 0; j < name.size(); j++) {
            String namePart = name.get(j);
            for (int i = 0; i < alias.size(); i++) {
                String aliasPart = alias.get(i);
                Tuple<Boolean, String> matchResult = areWordsMatching(namePart, aliasPart);
                if (matchResult.getA()) {
                    return new Triple<>(j, i, matchResult.getB());
                }
            }
        }
        return new Triple<>(name.size(), alias.size(), null);
    }

    private static Tuple<Boolean, String> areWordsMatching(String a, String b) {
        if (a.equals(b)) {
            return new Tuple<>(true, a);
        }

        StringBuilder result = new StringBuilder();

        LinkedList<Diff> diffs = new DiffMatchPatch().diffMain(a, b);

        int longestMatch = 0;
        int matches = 0;

        for (Diff diff : diffs) {
            switch (diff.operation) {
                case EQUAL -> {
                    result.append(diff.text);
                    longestMatch++;
                    matches++;
                }
                case DELETE -> {
                    result.append("<s>").append(diff.text).append("</s>");
                    longestMatch = 0;
                }
                case INSERT -> {
                    result.append("<u>").append(diff.text).append("</u>");
                    longestMatch = 0;
                }
            }
        }

        if (matches < MIN_MATCH * 0.5 * (a.length() + b.length())) {
            return new Tuple<>(false, null);
        } else if (longestMatch < MIN_LONGEST_MATCH) {
            return new Tuple<>(false, null);
        }

        return new Tuple<>(true, result.toString());
    }
}