package meal;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.jsoup.nodes.Element;

public class SideMeal extends Meal {

    private final Type type;

    public SideMeal(String name, String displayNameDE, String displayNameEN, Type type) {
        super(name, displayNameDE, displayNameEN);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static List<SideMeal> parseSideMeals(Element elementDE, Element elementEN) {

        Type type = null;

        Element extraType = elementDE.getElementsByClass("menue-item extra menue-category").get(0);
        if (extraType.text().equals("Hauptbeilagen")) {
            type = Type.MAIN;
        } else if (extraType.text().equals("Nebenbeilage")) {
            type = Type.SIDE;
        } else {
            LeckerSchmecker.getLogger().warning("Could not parse type of side-meal");
        }
        Type finalType = type;

        String htmlDE = elementDE.getElementsByClass("menue-item extra menue-desc").get(0).html();
        String htmlEN = elementEN.getElementsByClass("menue-item extra menue-desc").get(0).html();

        List<String> displayNameDE = Arrays.stream(htmlDE.replace("<span class=\"menue-nutr\">+</span>", "")
                .replace("<br>", "")
                .replaceAll("<sup>.*?</sup>", "")
                .replace("|", "")
                .split("<span class=\"seperator\">oder</span>")).map(String::trim).toList();

        List<String> displayNameEN = Arrays.stream(htmlEN.replace("<span class=\"menue-nutr\">+</span>", "")
                .replace("<br>", "")
                .replaceAll("<sup>.*?</sup>", "")
                .replace("|", "")
                .split("<span class=\"seperator\">or</span>")).map(String::trim).toList();

        List<SideMeal> res = new LinkedList<>();
        for (int i = 0; i < displayNameDE.size(); i++) {
            res.add(new SideMeal(displayNameDE.get(i).toLowerCase().replace(' ', '_'), displayNameDE.get(i), displayNameEN.get(i), finalType));
        }
        return res;
    }

    @Override
    public String toString() {
        return "SideMeal{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", displayName='" + displayNameDE + '\'' +
                '}';
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
