package meal;

import org.jsoup.nodes.Element;

public enum MealType {

    TELLERGERICHT_VEGETARISCH("Tellergericht Vegetarisch", 2.0f),
    TELLERGERICHT("Tellergericht", 2.0f),
    VEGETARISCH("Vegetarisch", 2.2f),
    KLASSIKER("Klassiker", 2.8f),
    WOK_VEGETARISCH("Wok Vegetarisch", 3.8f),
    WOK("Wok", 3.8f);

    private final String displayName;
    private final float price;

    MealType(String displayName, float price) {
        this.displayName = displayName;
        this.price = price;
    }

    public static MealType getMealTypeFromCategory(String category, Element e){
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
                if(e.getElementsByClass("content-image").get(0).attr("src").contains("vegan") ||
                e.getElementsByClass("content-image").get(0).attr("src").contains("OLV")){
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
