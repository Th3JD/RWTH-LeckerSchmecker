package meal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Canteen {

    public static final Canteen ACADEMICA = new Canteen("Mensa Academica", "academica");

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
        for(Element e : elements){
            DailyOffer offer = DailyOffer.parseOffer(e);
            dailyOffers.put(offer.getDate(), offer);
        }


    }


}
