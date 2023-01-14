/*
 * Copyright (c)  RWTH-LeckerSchmecker
 */

package meal;

import database.DatabaseManager;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import localization.ResourceManager;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import telegram.LeckerSchmeckerBot;

public class MainMeal extends Meal {

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

        String category = elementDE.getElementsByClass("menue-category").get(0).ownText();
        Type type = Type.getMealTypeFromCategory(category, elementDE);

        if (type == null) {
            LeckerSchmecker.getLogger()
                    .warning("Could not parse type of meal '" + htmlNameDE + "'");
            return null;
        }

        Elements priceElements = elementDE.getElementsByClass("menue-price");
        float price;
        if (priceElements.size() == 1) {
            String priceStr = elementDE.getElementsByClass("menue-price").get(0).ownText();
            price = Float.parseFloat(priceStr.split(" ")[0].replace(',', '.'));
        } else {
            LeckerSchmecker.getLogger().info("Meal " + htmlNameDE + " does not have a price. Using default price instead.");
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

        // build meal basis
        MainMeal.Builder builder = new Builder()
                .setType(type)
                .setPrice(price)
                .setNutritions(nutritions);

        List<MainMeal> meals = new ArrayList<>(1);

        // create meals for each derived name
        for (int i = 0; i < displayNamesDE.length; i++) {
            String displayNameDE = displayNamesDE[i];

            // try to get EN name, if not present use DE as fallback
            String displayNameEN;
            try {
                displayNameEN = displayNamesEN[i];
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
                .toLowerCase()
                .replace("ß", "ss")
                .replace(" mit ", " ")
                .replace(" und ", " ")
                .replace(" oder ", " ")
                .replace("   ", " ")
                .replace("  ", " ")
                .trim()
                .replace(" ", "_");
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
}