package meal;

import database.DatabaseManager;
import org.jsoup.nodes.Element;
import telegram.LeckerSchmeckerBot;

import java.time.DayOfWeek;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainMeal extends Meal {

    private final Type type;
    private final float price;
    private final List<Nutrition> nutritions;
    private Integer id;

    public MainMeal(String name, String displayName, Type type, float price, List<Nutrition> nutritions, Integer id) {
        super(name, displayName);
        this.type = type;
        this.price = price;
        this.nutritions = nutritions;
        this.id = id;
    }

    public static MainMeal parseMeal(DailyOffer offer, Element element) {

        // parse from html
        String displayName = element.getElementsByClass("expand-nutr").get(0).ownText();
        String category = element.getElementsByClass("menue-category").get(0).ownText();
        Type type = Type.getMealTypeFromCategory(category, element);

        if (type == null) {
            LeckerSchmecker.getLogger().warning("Could not parse type of meal '" + displayName + "'");
            return null;
        }

        String priceStr = element.getElementsByClass("menue-price").get(0).ownText();
        float price = Float.parseFloat(priceStr.split(" ")[0].replace(',', '.'));

        String name = compress(displayName);

        LinkedList<Nutrition> nutritions = Nutrition.searchNutrientsFor(element);

        if ((type.equals(Type.TELLERGERICHT) || type.equals(Type.TELLERGERICHT_VEGETARISCH))
                && offer.getDate().getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            nutritions.addFirst(Nutrition.SWEET);
        }

        MainMeal meal = new MainMeal(name, displayName, type, price, nutritions, null);

        // database
        Integer id = DatabaseManager.loadMealID(meal);

        // meal already exists
        if (id != null) {
            meal.id = id;
            return meal;
        }

        Set<Integer> similarIDs = DatabaseManager.loadMealIDsByShortAlias(meal);

        // if no short name matches,then insert meal into database, else ask admins
        if (similarIDs.isEmpty()) {
            meal.id = DatabaseManager.addMeal(meal);
        } else {
            LeckerSchmecker.getLogger()
                    .info("Found similar meal for '" + meal.getName() + "', asking admins");
            LeckerSchmeckerBot.getInstance().askAdmins(meal, similarIDs);
        }
        return meal;
    }

    public String getShortAlias() {
        if (displayName.contains("|")) {
            return compress(displayName.split("\\|")[0]);
        }
        return name;
    }

    public Type getType() {
        return type;
    }

    public float getPrice() {
        return price;
    }

    public String text() {
        String symbols = this.getSymbols();
        return this.getDisplayName() + (symbols.isEmpty() ? "" : " ") + symbols;
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
                ", displayName='" + displayName + '\'' +
                '}';
    }

    public enum Type {

        VEGETARISCH("Vegetarisch", 2.2f),
        KLASSIKER("Klassiker", 2.8f),
        TELLERGERICHT_VEGETARISCH("Tellergericht Vegetarisch", 2.0f),
        TELLERGERICHT("Tellergericht", 2.0f),
        WOK_VEGETARISCH("Wok Vegetarisch", 3.8f),
        WOK("Wok", 3.8f),
        EMPFEHLUNG_DES_TAGES("Gericht des Tages", 4.1f),
        PASTA("Pasta", 3.7f),
        PIZZA_DES_TAGES("Pizza des Tages", 3.7f),
        PIZZA_CLASSICS("Pizza Classics", 3.7f),
        ;

        private final String displayName;
        private final float price;

        Type(String displayName, float price) {
            this.displayName = displayName;
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
                    if (nutritions.contains(Nutrition.VEGAN) || nutritions.contains(Nutrition.VEGETARIAN)) {
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
            }
            return null;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getPrice() {
            return price;
        }
    }
}