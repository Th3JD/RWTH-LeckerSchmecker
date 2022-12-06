package meal;

import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SideMeal extends Meal {

    private final Type type;

    public SideMeal(String name, String displayName, Type type) {
        super(name, displayName);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String text() {
        return this.displayName;
    }

    public static List<SideMeal> parseSideMeals(Element element) {

        Type type = null;

        Element extraType = element.getElementsByClass("menue-item extra menue-category").get(0);
        if (extraType.text().equals("Hauptbeilagen")) {
            type = Type.MAIN;
        } else if (extraType.text().equals("Nebenbeilage")) {
            type = Type.SIDE;
        } else {
            LeckerSchmecker.getLogger().warning("Could not parse type of side-meal");
        }
        Type finalType = type;

        String html = element.getElementsByClass("menue-item extra menue-desc").get(0).html();
        return Arrays.stream(html.replace("<span class=\"menue-nutr\">+</span>", "")
                .replace("<br>", "")
                .replaceAll("<sup>.*</sup>", "")
                .split("<span class=\"seperator\">oder</span>")).map(String::trim)
                .map(name -> new SideMeal(name.toLowerCase().replace(' ', '_'), name, finalType))
                .toList();
    }

    public enum Type {
        MAIN("Hauptbeilage"), SIDE("Nebenbeilage");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
