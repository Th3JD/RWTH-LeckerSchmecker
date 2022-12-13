package telegram;

import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.util.UUID;

public class ChatContext {

    private final LeckerSchmeckerBot bot;
    private final UUID userID;
    private final long chatID;
    private BotAction returnToAction; // Action to return to, once the internal actions are done
    private BotAction currentAction;
    private Canteen selectedCanteen;
    private Canteen defaultCanteen;
    private LocalDate selectedDate;

    public ChatContext(LeckerSchmeckerBot bot, UUID userID, long chatID, Canteen defaultCanteen){
        this.bot = bot;
        this.userID = userID;
        this.chatID = chatID;
        this.defaultCanteen = defaultCanteen;

        this.returnToAction = null;
        this.currentAction = null;
        this.selectedDate = null;
        this.selectedCanteen = null;
    }


    public void resetTemporaryInformation(){
        this.returnToAction = null;
        this.selectedCanteen = null;
        this.selectedDate = null;
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

    public void setDefaultCanteen(Canteen defaultCanteen) {
        this.defaultCanteen = defaultCanteen;
    }

    public void sendMessage(String text){
        bot.sendTextMessage(chatID, text);
    }

    public void sendMessage(SendMessage message){
        bot.sendMessage(chatID, message);
    }





}
