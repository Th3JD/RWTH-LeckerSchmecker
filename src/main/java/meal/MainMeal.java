package meal;

import org.jsoup.nodes.Element;

public class MainMeal extends Meal {

    private static final String NUTR_VEGAN = "vegan",
                                NUTR_VEGETARIAN = "OLV",
                                NUTR_PORK = "Schwein",
                                NUTR_POULTRY = "Geflügel",
                                NUTR_BEEF = "Rind";


    private final Type type;
    private final boolean vegetarian;
    private final boolean vegan;

    public MainMeal(String name, String displayName, Type type, boolean vegetarian, boolean vegan) {
        super(name, displayName);
        this.type = type;
        this.vegetarian = vegetarian;
        this.vegan = vegan;
    }

    public static MainMeal parseMeal(Element element) {

        String displayName = element.getElementsByClass("expand-nutr").get(0).ownText();
        String category = element.getElementsByClass("menue-category").get(0).ownText();
        Type type = Type.getMealTypeFromCategory(category, element);

        if (type == null) {
            LeckerSchmecker.getLogger().warning("Could not parse type of meal '" + displayName + "'");
            return null;
        }

        boolean vegetarian = searchNutrientFor(element, NUTR_VEGETARIAN);
        boolean vegan = searchNutrientFor(element, NUTR_VEGAN);

        String name = displayName.toLowerCase().replace(' ', '_');

        return new MainMeal(name, displayName, type, vegetarian, vegan);
    }

    private static boolean searchNutrientFor(Element e, String string){
        for(Element image : e.getElementsByClass("content-image")){
            if(image.attr("src").contains(string)){
                return true;
            }
        }
        return false;
    }

    public Type getType() {
        return type;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public boolean isVegan() {
        return vegan;
    }

    @Override
    public String toString() {
        return "Meal{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                ", vegetarian=" + vegetarian +
                ", vegan=" + vegan +
                '}';
    }

    public enum Type {

        VEGETARISCH("Vegetarisch", 2.2f),
        KLASSIKER("Klassiker", 2.8f),
        TELLERGERICHT_VEGETARISCH("Tellergericht Vegetarisch", 2.0f),
        TELLERGERICHT("Tellergericht", 2.0f),
        WOK_VEGETARISCH("Wok Vegetarisch", 3.8f),
        WOK("Wok", 3.8f);

        private final String displayName;
        private final float price;

        Type(String displayName, float price) {
            this.displayName = displayName;
            this.price = price;
        }

        public static Type getMealTypeFromCategory(String category, Element e){
            switch (category){
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
                    if(searchNutrientFor(e, NUTR_VEGAN) || searchNutrientFor(e, NUTR_VEGETARIAN)){
                        return WOK_VEGETARISCH;
                    }
                    return WOK;
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