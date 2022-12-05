package meal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Canteen {

    public static final Canteen ACADEMICA = new Canteen("Mensa Academica", "academica");
    public static final Canteen AHORNSTRASSE = new Canteen("Mensa Ahornstraße", "ahornstrasse");
    public static final Canteen VITA = new Canteen("Mensa Vita", "vita");

    public static final List<Canteen> TYPES = List.of(ACADEMICA, AHORNSTRASSE, VITA);

    public static Optional<Canteen> getByDisplayName(String displayName) {
        return TYPES.stream().filter(c -> c.getDisplayName().equalsIgnoreCase(displayName)).findFirst();
    }

    private final String displayName;
    private final String urlName;
    private final Map<LocalDate, DailyOffer> dailyOffers = new HashMap<>();

    private Canteen(String displayName, String urlName) {
        this.displayName = displayName;
        this.urlName = urlName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrlName() {
        return urlName;
    }

    public void fetchDailyOffers(){
        dailyOffers.clear();

        Document doc;
        try {
            doc = Jsoup.connect("https://www.studierendenwerk-aachen.de/speiseplaene/" + urlName + "-w.html").get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Elements elements = doc.getElementsByClass("preventBreak");
        for(Element e : elements) {
            DailyOffer offer = DailyOffer.parseOffer(e);
            dailyOffers.put(offer.getDate(), offer);
        }
        LeckerSchmecker.getLogger().info("Fetched meals for canteen '" + this.getDisplayName() + "'");
    }

    public Map<LocalDate, DailyOffer> getDailyOffers() {
        return dailyOffers;
    }

    public Optional<DailyOffer> getDailyOffer(LocalDate date) {
        return Optional.ofNullable(this.dailyOffers.get(date));
    }
}
