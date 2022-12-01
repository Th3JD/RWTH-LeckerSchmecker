package meal;

import org.jsoup.nodes.Element;

public class Meal {

    private static final String NUTR_VEGAN = "vegan",
                                NUTR_VEGETARIAN = "OLV",
                                NUTR_PORK = "Schwein",
                                NUTR_POULTRY = "Geflügel",
                                NUTR_BEEF = "Rind";


    private final String name;
    private final String displayName;
    private final MealType type;
    private final boolean vegetarian;
    private final boolean vegan;

    public Meal(String name, String displayName, MealType type, boolean vegetarian, boolean vegan) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.vegetarian = vegetarian;
        this.vegan = vegan;
    }

    public static Meal parseMeal(Element element) {

        String displayName = element.getElementsByClass("expand-nutr").get(0).ownText();
        String category = element.getElementsByClass("menue-category").get(0).ownText();
        MealType mealType = MealType.getMealTypeFromCategory(category, element);
        boolean vegetarian = searchNutrientFor(element, NUTR_VEGETARIAN);
        boolean vegan = searchNutrientFor(element, NUTR_VEGAN);

        String name = displayName.toLowerCase().replace(' ', '_');

        Meal res = new Meal(name, displayName, mealType, vegetarian, vegan);

        return res;
    }

    private static boolean searchNutrientFor(Element e, String string){
        for(Element image : e.getElementsByClass("content-image")){
            if(image.attr("src").contains(string)){
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MealType getType() {
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
}