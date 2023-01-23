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

import java.util.LinkedList;
import org.jsoup.nodes.Element;

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

        if (nutritions.contains(VEGAN)) {
            nutritions.remove(VEGETARIAN);
        }

        if (e.getElementsByClass("expand-nutr").get(0).text().toLowerCase()
                .contains(SPICY.getHtmlKey())) {
            nutritions.addLast(SPICY);
        }

        return nutritions;
    }
}
