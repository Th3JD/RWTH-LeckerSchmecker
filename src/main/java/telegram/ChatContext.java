package telegram;

import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class ChatContext {

    private final LeckerSchmeckerBot bot;
    private final long chatID;
    private BotAction returnToAction; // Action to return to, once the internal actions are done
    private BotAction currentAction;
    private Canteen currentCanteen;
    private Canteen defaultCanteen;

    public ChatContext(LeckerSchmeckerBot bot, long chatID){
        this.bot = bot;
        this.chatID = chatID;
        returnToAction = null;
        currentAction = null;
        currentCanteen = null;
        defaultCanteen = null;

        //TODO: Load default values like language and defaultCanteen from database
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

    public Canteen getCurrentCanteen() {
        return currentCanteen;
    }

    public void setCurrentCanteen(Canteen currentCanteen) {
        this.currentCanteen = currentCanteen;
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
