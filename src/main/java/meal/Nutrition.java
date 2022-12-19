package meal;

import org.jsoup.nodes.Element;

import java.util.LinkedList;

public enum Nutrition {

    VEGAN("vegan", "\uD83C\uDF31"),
    VEGETARIAN("olv", "\uD83E\uDDC0"),
    PORK("schwein", "\uD83D\uDC37"),
    POULTRY("geflügel", "\uD83D\uDC14"),
    BEEF("rind", "\uD83D\uDC2E"),
    FISH("fisch", "\uD83D\uDC1F"),
    SPICY("scharf", "\uD83C\uDF36"),
    SWEET(null, "\uD83C\uDF6E");

    private final String htmlKey;
    private final String symbol;

    Nutrition(String htmlKey, String symbol) {
        this.htmlKey = htmlKey;
        this.symbol = symbol;
    }

    public String getHtmlKey() {
        return htmlKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public static LinkedList<Nutrition> searchNutrientsFor(Element e) {
        LinkedList<Nutrition> nutritions = new LinkedList<>();

        for (Element image : e.getElementsByClass("content-image")) {
            String html = image.attr("src").toLowerCase();
            for (Nutrition nutrition : Nutrition.values()) {
                if (nutrition.getHtmlKey() != null && html.contains(nutrition.getHtmlKey())) {
                    nutritions.add(nutrition);
                }
            }

        }

        if (e.getElementsByClass("expand-nutr").get(0).text().toLowerCase().contains(SPICY.getHtmlKey())) {
            nutritions.addLast(SPICY);
        }

        return nutritions;
    }
}
