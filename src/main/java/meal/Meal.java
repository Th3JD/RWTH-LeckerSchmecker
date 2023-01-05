package meal;

import java.util.Locale;

public class Meal {

    protected final String name;
    protected final String displayNameDE;
    protected final String displayNameEN;

    public Meal(String name, String displayNameDE, String displayNameEN) {
        this.name = name;
        this.displayNameDE = displayNameDE;
        this.displayNameEN = displayNameEN;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName(Locale locale) {
        if (locale.equals(new Locale("de", "DE"))) {
            return displayNameDE;
        }
        return displayNameEN;
    }
}
