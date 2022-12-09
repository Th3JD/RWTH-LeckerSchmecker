package meal;

import org.jsoup.nodes.Element;

import java.util.LinkedList;

public enum Nutrition {

	VEGAN("vegan", "\uD83E\uDDC0"),
	VEGETARIAN("OLV", "\uD83C\uDF31"),
	PORK("Schwein", "\uD83D\uDC37"),
	POULTRY("Geflügel", "\uD83D\uDC14"),
	BEEF("Rind", "\uD83D\uDC2E"),
	FISH("Fisch", "\uD83D\uDC1F"),
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
			String html = image.attr("src");
			for (Nutrition nutrition : Nutrition.values()) {
				if (nutrition.getHtmlKey() != null && html.contains(nutrition.getHtmlKey())) {
					nutritions.add(nutrition);
				}
			}

		}
		return nutritions;
	}
}
