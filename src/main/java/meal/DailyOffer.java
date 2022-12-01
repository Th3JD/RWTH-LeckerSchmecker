package meal;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DailyOffer {

    private Set<Meal> meals = new HashSet<>();
    private final LocalDate date;


    public DailyOffer(LocalDate date) {
        this.date = date;
    }

    public void addMeal(Meal meal){
        this.meals.add(meal);
    }

    public static DailyOffer parseOffer(Element element){

        Locale locale = Locale.GERMANY;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", locale);
        String fullDate = element.getElementsByClass("default-headline").get(0).text();
        LocalDate date = LocalDate.parse(fullDate, formatter);

        DailyOffer res = new DailyOffer(date);

        Elements htmlMeals = element.getElementsByClass("menues").get(0)
                .getElementsByClass("menue-wrapper");

        for(Element hmtlMeal : htmlMeals){
            Meal meal = Meal.parseMeal(hmtlMeal);
            res.addMeal(meal);
        }
        return res;
    }



    public LocalDate getDate() {
        return date;
    }

}
