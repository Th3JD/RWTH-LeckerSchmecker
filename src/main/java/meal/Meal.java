package meal;

public class Meal {
    protected final String name;
    protected final String displayName;

    public Meal(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
