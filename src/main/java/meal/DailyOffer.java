package meal;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DailyOffer {

    private final Set<Meal> meals = new HashSet<>();
    private final Set<SideMeal> sideMeals = new HashSet<>();
    private final LocalDate date;


    public DailyOffer(LocalDate date) {
        this.date = date;
    }

    public void addMeal(Meal meal){
        this.meals.add(meal);
    }

    public void addSideMeal(SideMeal meal) {
        this.sideMeals.add(meal);
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

        Elements htmlExtras = element.getElementsByClass("extras").get(0).getElementsByClass("menue-wrapper");
        for (Element htmlExtra : htmlExtras) {
            SideMeal.parseSideMeals(htmlExtra).forEach(res::addSideMeal);
        }

        return res;
    }

    public Set<Meal> getMeals() {
        return meals;
    }

    public Set<SideMeal> getSideMeals() {
        return sideMeals;
    }

    public Set<SideMeal> getSideMeals(SideMeal.Type type) {
        return sideMeals.stream().filter(m -> m.getType().equals(type)).collect(Collectors.toSet());
    }

    public LocalDate getDate() {
        return date;
    }

}
