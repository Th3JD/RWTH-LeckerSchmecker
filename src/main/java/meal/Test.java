package meal;

import database.DatabaseManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Test {

    public static void main(String[] args) {

        Document doc;
        try {
            doc = Jsoup.connect("https://www.studierendenwerk-aachen.de/speiseplaene/academica-w.html").get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements elements = doc.getElementsByClass("preventBreak");
        System.out.println("Anzahl der Tage: " + elements.size());

        Element montag = elements.get(0);
        String fullDate = montag.getElementsByClass("default-headline").get(0).text();

        Locale locale = Locale.GERMANY;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", locale);
        LocalDate date = LocalDate.parse("Montag, 28.11.2022", formatter);
        System.out.println("Datum geparsed: " + date);



        Element menues = montag.getElementsByClass("menues").get(0);
        Elements meals = menues.getElementsByClass("menue-wrapper");
        System.out.println("Anzahl der Gerichte: " + meals.size());

        Element first = meals.get(0);
        System.out.println("Erstes Gericht: " + Meal.parseMeal(first));

        DatabaseManager.connect();
        DatabaseManager.setupTables();
        DatabaseManager.disconnect();

    }

}
