package telegram;

import database.DatabaseManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import meal.Canteen;
import meal.MainMeal;
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

    // State information
    private BotAction returnToAction; // Action to return to, once the internal actions are done
    private BotAction currentAction;

    // Passthrough information
    private Canteen selectedCanteen;
    private Canteen defaultCanteen;

    private LocalDate selectedDate;

    private MainMeal selectedMeal;

    private Integer ratedPoints;

    private Boolean defaultValueSet; // Couldn't come up with a better name... Sorry :(


    public ChatContext(LeckerSchmeckerBot bot, UUID userID, long chatID, Canteen defaultCanteen){
        this.bot = bot;
        this.userID = userID;
        this.chatID = chatID;
        this.defaultCanteen = defaultCanteen;
    }


    public void resetPassthroughInformation() {
        this.returnToAction = null;
        this.selectedCanteen = null;
        this.selectedDate = null;
        this.selectedMeal = null;
        this.ratedPoints = null;
        this.defaultValueSet = null;
    }

    // Custom Getter & Setter /////////////////////////////////////////////////
    public void setDefaultCanteen(Canteen defaultCanteen) {
        this.defaultCanteen = defaultCanteen;
        DatabaseManager.setDefaultCanteen(userID, defaultCanteen);
    }

    // Generated Getter & Setter //////////////////////////////////////////////
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

    public LeckerSchmeckerBot getBot(){
        return bot;
    }

    public boolean hasCurrentAction(){
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

    public MainMeal getSelectedMeal() {
        return selectedMeal;
    }

    public void setSelectedMeal(MainMeal selectedMeal) {
        this.selectedMeal = selectedMeal;
    }

    public boolean hasMealSelected() {
        return this.selectedMeal != null;
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

    public Integer getMessageIdByPollId(String pollId) {
        return this.messageIdByPollId.get(pollId);
    }


}
