package telegram;

import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;

public enum CallableAction implements BotAction{

    MAIN_MENU("Hauptmenü", List.of("/start", "start", "exit", "menu", "menü")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);

            // Reset context in case the user quit while in an internal state
            context.resetTemporaryInformation();

            SendMessage message = passthroughMessage;
            if (message == null) {
                message = new SendMessage();
                message.setText("Wähle eine Aktion!");
            }

            message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
                    Arrays.stream(CallableAction.values())
                            .map(CallableAction::getDisplayName).toList()));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            Message msg = update.getMessage();
            Arrays.stream(CallableAction.values())
                    .filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].toLowerCase()))
                    .findFirst()
                    .ifPresent(action -> action.init(context, null));
        }
    },

    LIST_MEALS("Gerichte", List.of("gerichte", "essen")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);
            context.setReturnToAction(this); // Needed to make the internal action return to this action

            // Select date, selecting the canteen is done in onUpdate
            InternalAction.SELECT_DATE.init(context, null);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if(!update.hasMessage()){
                return;
            }

            // Check if we returned from an internal state (should always be true)
            if(context.getReturnToAction() == this){

                // Check if we already know about the canteen (the date should always be set at this point)
                if(context.getDefaultCanteen() != null || context.getSelectedCanteen() != null){
                    Canteen selectedCanteen = context.getDefaultCanteen() != null ? context.getDefaultCanteen() : context.getSelectedCanteen();

                    SendMessage message = new SendMessage();
                    message.enableMarkdownV2(true);
                    message.setText("--------    *Gerichte (" + selectedCanteen.getDisplayName() + ")*    --------\n\n"
                            + context.getBot().getMealsText(selectedCanteen, context.getSelectedDate()));

                    // Reset everything prior to exiting the state
                    context.resetTemporaryInformation();

                    MAIN_MENU.init(context, message);
                } else {

                    // No canteen chosen so far
                    InternalAction.SELECT_CANTEEN.init(context, null);
                }

            }
        }
    },

    RATING("Kritik", List.of("bewertung", "kritik", "rating")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);
            context.sendMessage(passthroughMessage);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setText("Bald verfügbar!");

            MAIN_MENU.init(context, sendMessage);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {

        }
    },

    SELECT_DEFAULTS("Standardwerte", List.of("standardwerte", "defaults")){
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText("Soll ein Standardwert gesetzt oder gelöscht werden?");

            message.setReplyMarkup(BotAction.createKeyboardMarkup(1, "Setzen", "Löschen"));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if(!update.hasMessage()){
                return;
            }

            // Check if we returned from an internal state
            if(context.getReturnToAction() == this){

                SendMessage message = new SendMessage();

                // Check if the user selected a canteen
                if(context.getSelectedCanteen() != null){
                    context.setDefaultCanteen(context.getSelectedCanteen());
                    message.setText(context.getSelectedCanteen().getDisplayName() + " \u2705");
                }

                // Reset everything prior to exiting the state
                context.resetTemporaryInformation();
                MAIN_MENU.init(context, message);
                return;
            }

            String text = update.getMessage().getText();
            switch (text){
                case "Setzen", "Löschen" -> {
                    boolean shouldBeSet = text.equals("Setzen");
                    context.setDefaultValueSet(shouldBeSet);

                    SendMessage message = new SendMessage();
                    message.setText("Welcher Standardwert soll " + (shouldBeSet ? "gesetzt" : "gelöscht") + " werden?");
                    message.setReplyMarkup(BotAction.createKeyboardMarkup(1, "Mensa"));
                    context.sendMessage(message);
                }
                case "Mensa" -> {

                    // Check if the default canteen needs to be set or unset
                    if(context.getDefaultValueSet()){
                        // Canteen should be set
                        context.setReturnToAction(this); // Needed to make the internal action return to this action

                        InternalAction.SELECT_CANTEEN.init(context, null);
                    } else {
                        // Canteen should be unset
                        context.setDefaultCanteen(null);

                        SendMessage message = new SendMessage();
                        message.setText("Deine Standardmensa wurde zurückgesetzt!");
                        MAIN_MENU.init(context, message);
                    }
                    return;
                }
                default -> {
                    context.sendMessage("Ungültige Option. Wähle eine Option oder kehre mit /start zum Hauptmenü zurück.");
                    this.init(context, null);
                }
            }

        }
    };

    private final String displayName;
    private final List<String> cmds;

    CallableAction(String displayName, List<String> cmds) {
        this.displayName = displayName;
        this.cmds = cmds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getCmds() {
        return cmds;
    }

}