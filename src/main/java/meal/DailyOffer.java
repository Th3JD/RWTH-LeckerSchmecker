package meal;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DailyOffer {

    private final Set<Meal> meals = new HashSet<>();
    private final LocalDate date;


    public DailyOffer(LocalDate date) {
        this.date = date;
    }

    public void addMeal(Meal meal){
        this.meals.add(meal);
    }

    public static DailyOffer parseOffer(Element element){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", Locale.GERMANY);

        Elements headLineElements = element.getElementsByClass("active-headline");
        if (headLineElements.isEmpty()) {
            headLineElements = element.getElementsByClass("default-headline");
        }
        String fullDate = headLineElements.get(0).text();

        LocalDate date = LocalDate.parse(fullDate, formatter);

        DailyOffer res = new DailyOffer(date);

        Elements htmlMeals = element.getElementsByClass("menues").get(0)
                .getElementsByClass("menue-wrapper");

        for(Element htmlMeal : htmlMeals){
            Meal meal = Meal.parseMeal(htmlMeal);
            res.addMeal(meal);
        }
        return res;
    }

    public Set<Meal> getMeals() {
        return meals;
    }

    public LocalDate getDate() {
        return date;
    }

}
