package telegram;

import database.DatabaseManager;
import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.util.UUID;

public class ChatContext {

    // Identifier and bot
    private final LeckerSchmeckerBot bot;
    private final UUID userID;
    private final long chatID;

    // State information
    private BotAction returnToAction; // Action to return to, once the internal actions are done
    private BotAction currentAction;

    // Passthrough information
    private Canteen selectedCanteen;
    private Canteen defaultCanteen;
    private LocalDate selectedDate;
    private Boolean defaultValueSet; // Couldn't come up with a better name... Sorry :(


    public ChatContext(LeckerSchmeckerBot bot, UUID userID, long chatID, Canteen defaultCanteen){
        this.bot = bot;
        this.userID = userID;
        this.chatID = chatID;
        this.defaultCanteen = defaultCanteen;
    }


    public void resetTemporaryInformation(){
        this.returnToAction = null;
        this.selectedCanteen = null;
        this.selectedDate = null;
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

    public Canteen getDefaultCanteen() {
        return defaultCanteen;
    }

    public void sendMessage(String text){
        bot.sendTextMessage(chatID, text);
    }

    public void sendMessage(SendMessage message){
        bot.sendMessage(chatID, message);
    }





}
