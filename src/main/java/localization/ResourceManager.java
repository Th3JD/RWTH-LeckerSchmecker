package localization;

import java.util.List;
import java.util.Locale;

public class ResourceManager {

    protected static ResourceManager instance;

    public static final Locale DEFAULTLOCALE = new Locale("en", "GB");
    public static final List<Locale> LOCALES = List.of(DEFAULTLOCALE,
            new Locale("de", "DE"));

    private static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    // Static function declarations //////////////////////////////////////////////////////////

    // ///////////////////////////////////////////////////////////////////////////////////////

    // protected implementations /////////////////////////////////////////////////////////////

    // ///////////////////////////////////////////////////////////////////////////////////////

}
