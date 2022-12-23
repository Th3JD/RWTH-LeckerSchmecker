package meal;

import java.util.List;
import java.util.Optional;

public enum MealType {

    VEGAN("Vegan", "vegan", "vegan"),
    VEGETARIAN("Vegetarisch", "vetegarian", "vegetarian"),
    NOPORK("kein Schweinefleisch", "no pork", "nopork"),
    NOFISH("kein Fisch", "no fish", "nofish"),
    NOMILK("keine Milchprodukte", "no dairy products", "nomilk"),
    EVERYTHING("Ich esse alles!", "I eat everything!", "all");

    public static final List<MealType> TYPES = List.of(VEGAN, VEGETARIAN, NOPORK, NOFISH, NOMILK,
            EVERYTHING);

    private final String nameDE;
    private final String nameEN;
    private final String id;

    MealType(String nameDE, String nameEN, String id) {
        this.nameDE = nameDE;
        this.nameEN = nameEN;
        this.id = id;
    }

    public static Optional<MealType> getById(String name) {
        return TYPES.stream().filter(c -> c.getId().equalsIgnoreCase(name)).findFirst();
    }

    public static Optional<MealType> getByDisplayName(String displayName) {
        return TYPES.stream().filter(c -> c.getDisplayName().equalsIgnoreCase(displayName)).findFirst();
    }

    public String getDisplayName() {
        return nameDE;
    }

    public String getNameDE() {
        return nameDE;
    }

    public String getNameEN() {
        return nameEN;
    }
    public String getId() {
        return id;
    }
}
