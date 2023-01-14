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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Canteen {

    public static final Canteen ACADEMICA = new Canteen("Mensa Academica", "academica",
            LocalTime.of(11, 30), LocalTime.of(14, 30));
    public static final Canteen AHORNSTRASSE = new Canteen("Mensa Ahornstraße", "ahornstrasse",
            LocalTime.of(11, 30), LocalTime.of(14, 30));
    public static final Canteen VITA = new Canteen("Mensa Vita", "vita",
            LocalTime.of(11, 30), LocalTime.of(14, 30));
    public static final Canteen TEMPLERGRABEN = new Canteen("Bistro Templergraben", "templergraben",
            LocalTime.of(11, 30), LocalTime.of(15, 00));
    public static final Canteen BAYERNALLEE = new Canteen("Mensa Bayernallee", "bayernallee",
            LocalTime.of(11, 30), LocalTime.of(14, 30));
    public static final Canteen EUPENERSTRASSE = new Canteen("Mensa Eupener Straße", "eupenerstrasse",
            LocalTime.of(11, 15), LocalTime.of(14, 15));
    public static final Canteen KMAC = new Canteen("Mensa KMAC", "kmac",
            LocalTime.of(11, 30), LocalTime.of(14, 30));
    public static final Canteen JUELICH = new Canteen("Mensa Jülich", "juelich",
            LocalTime.of(11, 30), LocalTime.of(14, 30));

    //public static final Canteen SUEDPARK = new Canteen("Mensa Südpark", "suedpark");

    public static final List<Canteen> TYPES = List.of(ACADEMICA, AHORNSTRASSE, VITA, TEMPLERGRABEN, BAYERNALLEE,
            EUPENERSTRASSE, KMAC, JUELICH);

    public static Optional<Canteen> getByDisplayName(String displayName) {
        return TYPES.stream().filter(c -> c.getDisplayName().equalsIgnoreCase(displayName)).findFirst();
    }

    public static Optional<Canteen> getByURLName(String urlName) {
        return TYPES.stream().filter(c -> c.getUrlName().equalsIgnoreCase(urlName)).findFirst();
    }

    private final String displayName;
    private final String urlName;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final Map<LocalDate, DailyOffer> dailyOffers = new HashMap<>();

    private Canteen(String displayName, String urlName, LocalTime openingTime, LocalTime closingTime) {
        this.displayName = displayName;
        this.urlName = urlName;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrlName() {
        return urlName;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void fetchDailyOffers() {
        dailyOffers.clear();

        Document docDE;
        Document docEN;
        try {
            docDE = Jsoup.connect("https://www.studierendenwerk-aachen.de/speiseplaene/" + urlName + "-w.html").get();
            docEN = Jsoup.connect("https://www.studierendenwerk-aachen.de/speiseplaene/" + urlName + "-w-en.html").get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements elementsDE = docDE.getElementsByClass("preventBreak");
        Elements elementsEN = docEN.getElementsByClass("preventBreak");
        for (int i = 0; i < elementsDE.size(); i++) {
            DailyOffer offer = DailyOffer.parseOffer(elementsDE.get(i), elementsEN.get(i));
            if (offer != null) {
                dailyOffers.put(offer.getDate(), offer);
            }
        }
        LeckerSchmecker.getLogger().info("Fetched " + this.dailyOffers.values().stream()
                .mapToInt(d -> d.getMainMeals().size()).sum() + " meals for canteen '" + this.getDisplayName() + "'");
    }

    public Map<LocalDate, DailyOffer> getDailyOffers() {
        return dailyOffers;
    }

    public Optional<DailyOffer> getDailyOffer(LocalDate date) {
        return Optional.ofNullable(this.dailyOffers.get(date));
    }
}
