package telegram;

import database.DatabaseManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import localization.ResourceManager;
import meal.Canteen;
import meal.MainMeal;
import meal.MealType;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;

public class ChatContext {

    // Identifier and bot
    private final LeckerSchmeckerBot bot;
    private final UUID userID;
    private final long chatID;
    private final Map<String, Integer> messageIdByPollId = new HashMap<>();

    // Info about the user
    private Canteen defaultCanteen;
    private Locale locale;

    // State information
    private BotAction returnToAction; // Action to return to, once the internal actions are done
    private BotAction currentAction;

    // Passthrough information
    private Locale selectedLocale;
    private Canteen selectedCanteen;

    private MealType selectedMealType;
    private MealType defaultMealType;

    private LocalDate selectedDate;

    private MainMeal selectedMeal;

    private Integer ratedPoints;

    private Boolean defaultValueSet; // Couldn't come up with a better name... Sorry :(


    public ChatContext(LeckerSchmeckerBot bot, UUID userID, long chatID, Canteen defaultCanteen, MealType mealType,
                       Locale locale) {
        this.bot = bot;
        this.userID = userID;
        this.chatID = chatID;
        this.defaultCanteen = defaultCanteen;
        this.defaultMealType = mealType;
        this.locale = locale;
    }


    public void resetPassthroughInformation() {
        this.returnToAction = null;
        this.selectedCanteen = null;
        this.selectedMealType = null;
        this.selectedDate = null;
        this.selectedMeal = null;
        this.ratedPoints = null;
        this.defaultValueSet = null;
        this.selectedLocale = null;
    }

    // Custom Getter & Setter /////////////////////////////////////////////////
    public void setDefaultCanteen(Canteen defaultCanteen) {
        this.defaultCanteen = defaultCanteen;
        DatabaseManager.setDefaultCanteen(userID, defaultCanteen);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        DatabaseManager.setLanguage(userID, locale);
    }

    public void setDefaultMealType(MealType mealType) {
        this.defaultMealType = mealType;
        DatabaseManager.setDefaultMealType(userID, mealType);
    }

    // Generated Getter & Setter //////////////////////////////////////////////
    public Locale getLocale() {
        return locale;
    }

    public Boolean getDefaultValueSet() {
        return defaultValueSet;
    }

    public void setDefaultValueSet(Boolean defaultValueSet) {
        this.defaultValueSet = defaultValueSet;
    }

    public UUID getUserID() {
        return userID;
    }

    public long getChatID() {
        return chatID;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public boolean hasDateSelected() {
        return this.getSelectedDate() != null;
    }

    public LeckerSchmeckerBot getBot() {
        return bot;
    }

    public boolean hasCurrentAction() {
        return currentAction != null;
    }

    public BotAction getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(BotAction currentAction) {
        this.currentAction = currentAction;
    }

    public BotAction getReturnToAction() {
        return returnToAction;
    }

    public void setReturnToAction(BotAction returnToAction) {
        this.returnToAction = returnToAction;
    }

    public void setSelectedLocale(Locale selectedLocale) {
        this.selectedLocale = selectedLocale;
    }

    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    public Canteen getSelectedCanteen() {
        return selectedCanteen;
    }

    public void setSelectedCanteen(Canteen selectedCanteen) {
        this.selectedCanteen = selectedCanteen;
    }

    public boolean hasCanteen() {
        return this.getDefaultCanteen() != null || this.getSelectedCanteen() != null;
    }

    public Canteen getCanteen() {
        return this.getDefaultCanteen() != null ? this.getDefaultCanteen() : this.getSelectedCanteen();
    }

    public Canteen getDefaultCanteen() {
        return defaultCanteen;
    }

    public MealType getSelectedMealType() {
        return selectedMealType;
    }

    public void setSelectedMealType(MealType selectedMealType) {
        this.selectedMealType = selectedMealType;
    }

    public boolean hasMealType() {
        return this.getDefaultMealType() != null || this.getSelectedMealType() != null;
    }

    public MealType getMealType() {
        return this.getDefaultMealType() != null ? this.getDefaultMealType() : this.getSelectedMealType();
    }

    public MealType getDefaultMealType() {
        return defaultMealType;
    }

    public MainMeal getSelectedMeal() {
        return selectedMeal;
    }

    public void setSelectedMeal(MainMeal selectedMeal) {
        this.selectedMeal = selectedMeal;
    }

    public boolean hasMealSelected() {
        return this.selectedMeal != null;
    }

    public boolean hasMealTypeSelected() {
        return this.selectedMealType != null;
    }

    public Integer getRatedPoints() {
        return ratedPoints;
    }

    public void setRatedPoints(Integer ratedPoints) {
        this.ratedPoints = ratedPoints;
    }

    public boolean hasRated() {
        return this.ratedPoints != null;
    }

    public void sendLocalizedMessage(String key, Object... objects) {
        bot.sendTextMessage(chatID, ResourceManager.getString(key, this.locale, objects));
    }

    public void sendLocalizedMessage(String key) {
        bot.sendTextMessage(chatID, ResourceManager.getString(key, this.locale));
    }

    public String getLocalizedString(String key) {
        return ResourceManager.getString(key, this.locale);
    }

    public String getLocalizedString(String key, Object... objects) {
        return ResourceManager.getString(key, this.locale, objects);
    }

    public void sendMessage(String text) {
        bot.sendTextMessage(chatID, text);
    }

    public void sendMessage(SendMessage message) {
        bot.sendMessage(chatID, message);
    }

    public String sendPoll(SendPoll poll) {
        Message msg = bot.sendPoll(chatID, poll);
        String pollId = msg.getPoll().getId();
        this.messageIdByPollId.put(pollId, msg.getMessageId());
        return pollId;
    }

    public void deleteMessage(Integer msgId) {
        bot.deleteMessage(chatID, new DeleteMessage(String.valueOf(chatID), msgId));
    }

    public void deletePoll(Poll poll) {
        bot.deletePoll(chatID,
                new DeleteMessage(String.valueOf(chatID), this.messageIdByPollId.get(poll.getId())),
                poll);
        this.messageIdByPollId.remove(poll.getId());
    }
}
