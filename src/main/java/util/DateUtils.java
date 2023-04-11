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

package util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

public class DateUtils {

    public static final List<LocalTime> TIME_OPTIONS = List.of(
            LocalTime.of(8,0),
            LocalTime.of(9,0),
            LocalTime.of(10,0),
            LocalTime.of(11,0),
            LocalTime.of(12,0));

    /**
     * Adds a set amount of days to a given date, ignoring weekends. WARNING: If the param date is a weekend, setting days to zero will return the
     * next monday, and so on...
     *
     * @param date Starting date
     * @param days Days to add
     */
    public static LocalDate addDaysSkippingWeekends(LocalDate date, int days) {
        LocalDate result = date;

        while (isWeekend(result)) {
            result = result.plusDays(1);
        }

        int addedDays = 0;
        while (addedDays < days) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                ++addedDays;
            }
        }
        return result;
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }


}
