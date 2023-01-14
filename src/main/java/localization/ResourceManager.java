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

package localization;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceManager {

    protected static ResourceManager instance;

    protected static final String BASENAME = "localization.bundle";
    public static final Locale DEFAULTLOCALE = new Locale("en", "GB");
    public static final List<Locale> LOCALES = List.of(DEFAULTLOCALE,
            new Locale("de", "DE"),
            new Locale("es", "ES"));

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
