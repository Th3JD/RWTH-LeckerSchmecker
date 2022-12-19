package meal;

import config.Config;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DailyOffer {

    private final SortedSet<MainMeal> meals = new TreeSet<>(Comparator.comparing(MainMeal::getType).thenComparing(Meal::getName));
    private final Set<SideMeal> sideMeals = new HashSet<>();
    private final LocalDate date;


    public DailyOffer(LocalDate date) {
        this.date = date;
    }

    public void addMeal(MainMeal meal){
        if (meal == null) return;
        this.meals.add(meal);
    }

    public void addSideMeal(SideMeal meal) {
        if (meal == null) return;
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

        // Check if the date is already in the past
        if(date.isBefore(LocalDate.now())){
            return null;
        }

        // Check if the date is too far in the future
        if(!date.isBefore(LocalDate.now().plusDays(Config.getInt("meals.daysToFetch")))){
            // Meals this far in the future are prone to contain typos and are oftentimes subject to change
            return null;
        }

        DailyOffer res = new DailyOffer(date);

        Elements htmlMeals = element.getElementsByClass("menues").get(0)
                .getElementsByClass("menue-wrapper");

        for(Element htmlMeal : htmlMeals){
            MainMeal meal = MainMeal.parseMeal(res, htmlMeal);
            res.addMeal(meal);
        }

        Elements htmlExtras = element.getElementsByClass("extras").get(0).getElementsByClass("menue-wrapper");
        for (Element htmlExtra : htmlExtras) {
            SideMeal.parseSideMeals(htmlExtra).forEach(res::addSideMeal);
        }

        return res;
    }

    public Set<MainMeal> getMainMeals() {
        return meals;
    }

    public Set<SideMeal> getSideMeals() {
        return sideMeals;
    }

    public Set<SideMeal> getSideMeals(SideMeal.Type type) {
        return sideMeals.stream().filter(m -> m.getType().equals(type)).collect(Collectors.toSet());
    }

    public Optional<MainMeal> getMainMealById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return this.getMainMeals().stream().filter(m -> id.equals(m.getId())).findFirst();
    }

    public Optional<MainMeal> getMainMealByDisplayName(String displayName) {
        if (displayName == null) {
            return Optional.empty();
        }
        return this.getMainMeals().stream().filter(m -> displayName.equals(m.getDisplayName())).findFirst();
    }

    public LocalDate getDate() {
        return date;
    }

}
