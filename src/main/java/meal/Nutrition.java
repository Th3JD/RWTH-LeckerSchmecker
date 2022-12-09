package meal;

import org.jsoup.nodes.Element;

import java.util.HashSet;
import java.util.Set;

public enum Nutrition {

	VEGAN("vegan", "\uD83E\uDDC0"),
	VEGETARIAN("OLV", "\uD83C\uDF31"),
	PORK("Schwein", "\uD83D\uDC37"),
	POULTRY("Geflügel", "\uD83D\uDC14"),
	BEEF("Rind", "\uD83D\uDC2E"),
	FISH("Fisch", "\uD83D\uDC1F");

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

	public static Set<Nutrition> searchNutrientsFor(Element e) {
		Set<Nutrition> nutritions = new HashSet<>();

		for (Element image : e.getElementsByClass("content-image")) {
			String html = image.attr("src");
			for (Nutrition nutrition : Nutrition.values()) {
				if (html.contains(nutrition.getHtmlKey())) {
					nutritions.add(nutrition);
				}
			}

		}
		return nutritions;
	}
}
