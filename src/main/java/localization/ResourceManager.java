package localization;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceManager {

    protected static ResourceManager instance;

    protected static final String BASENAME = "localization.bundle";
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
    public static String getString(String key, Locale locale, Object... objects) {
        return getInstance()._getString(key, locale, objects);
    }

    public static String getString(String key, Locale locale) {
        return getInstance()._getString(key, locale);
    }
    // ///////////////////////////////////////////////////////////////////////////////////////

    // protected implementations /////////////////////////////////////////////////////////////
    protected String _getString(String key, Locale locale, Object... objects) {
        return String.format(ResourceBundle.getBundle(BASENAME, locale).getString(key), objects);
    }

    protected String _getString(String key, Locale locale) {
        return ResourceBundle.getBundle(BASENAME, locale).getString(key);
    }
    // ///////////////////////////////////////////////////////////////////////////////////////

}
