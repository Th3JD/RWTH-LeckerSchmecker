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
