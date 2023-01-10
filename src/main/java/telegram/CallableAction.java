package telegram;

import database.DatabaseManager;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import localization.ResourceManager;
import meal.Canteen;
import meal.MainMeal;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public enum CallableAction implements BotAction {

    LIST_MEALS("callableaction_list_meals", List.of("gerichte", "essen", "meals", "comidas")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);
            context.setReturnToAction(
                    this); // Needed to make the internal action return to this action

            // Select date, selecting the canteen is done in onUpdate
            InternalAction.SELECT_DATE.init(context, null);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (!update.hasMessage()) {
                return;
            }

            // Check if we returned from an internal state (should always be true)
            if (context.getReturnToAction() == this) {

                // Check if we already know about the canteen (the date should always be set at this point)
                if (context.hasCanteen()) {
                    Canteen selectedCanteen = context.getCanteen();

                    SendMessage message = new SendMessage();
                    message.enableMarkdown(true);
                    message.setText("*" + context.getLocalizedString("meals") + " ("
                            + selectedCanteen.getDisplayName() + ")*\n\n"
                            + context.getBot()
                            .getMealsText(selectedCanteen, context.getSelectedDate(), context));

                    // Reset everything prior to exiting the state
                    context.resetPassthroughInformation();

                    MAIN_MENU.init(context, message);
                } else {

                    // No canteen chosen so far
                    InternalAction.SELECT_CANTEEN.init(context, null);
                }

            }
        }
    },

    RATING("callableaction_rate", List.of("bewertung", "kritik", "rating", "evaluar")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);
            context.sendMessage(passthroughMessage);

            context.setReturnToAction(this);

            // Check if user already rated a meal today
            if (DatabaseManager.hasRatedToday(context)) {
                context.sendLocalizedMessage("already_rated");
            }

            if (context.hasCanteen()) {
                InternalAction.SELECT_MEAL.init(context, null);
            } else {
                InternalAction.SELECT_CANTEEN.init(context, null);
            }
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (!context.hasCanteen()) {
                context.setReturnToAction(this);
                InternalAction.SELECT_CANTEEN.init(context, null);
                return;
            }

            Canteen canteen = context.getCanteen();
            LocalTime currentTime = LocalTime.now();

            if (currentTime.isBefore(canteen.getOpeningTime())) {
                MAIN_MENU.init(context, new SendMessage(String.valueOf(context.getChatID()),
                        context.getLocalizedString("canteen_not_open_yet",
                                canteen.getDisplayName())));
                context.resetPassthroughInformation();
                return;
            }

            if (currentTime.isAfter(canteen.getClosingTime().plusMinutes(30))) {
                context.resetPassthroughInformation();
                MAIN_MENU.init(context, new SendMessage(String.valueOf(context.getChatID()),
                        context.getLocalizedString("canteen_already_closed",
                                canteen.getDisplayName())));
                return;
            }

            if (!context.hasMealSelected()) {
                context.setReturnToAction(this);
                InternalAction.SELECT_MEAL.init(context, null);
                return;
            }

            MainMeal meal = context.getSelectedMeal();

            if (meal.getId() == null) {
                context.resetPassthroughInformation();
                SendMessage msg = new SendMessage(String.valueOf(context.getChatID()),
                        context.getLocalizedString("meal_not_ratable", meal.getDisplayName(context.getLocale())));
                msg.enableMarkdown(true);
                MAIN_MENU.init(context, msg);
                return;
            }

            if (!context.hasRated()) {
                context.setReturnToAction(this);
                InternalAction.RATE_MEAL.init(context, null);
                return;
            }

            int ratedPoints = context.getRatedPoints();

            DatabaseManager.rateMeal(context, meal, ratedPoints);

            context.resetPassthroughInformation();
            SendMessage msg = new SendMessage(String.valueOf(context.getChatID()),
                    "_" + meal.getDisplayName(context.getLocale()) + "_: *" + ratedPoints + "*");
            msg.enableMarkdown(true);
            MAIN_MENU.init(context, msg);
        }
    },

    MAIN_MENU("callableaction_main_menu", List.of("/start", "start", "exit", "menu", "menü", "hauptmenü",
            "inicio")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);

            // Reset context in case the user quit while in an internal state
            context.resetPassthroughInformation();

            SendMessage message = passthroughMessage;
            if (message == null) {
                message = new SendMessage();
                message.setText(context.getLocalizedString("choose_action"));
            }

            message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
                    Arrays.stream(CallableAction.values())
                            .map(a -> a.getDisplayName(context.getLocale())).toList()));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            Message msg = update.getMessage();
            Arrays.stream(CallableAction.values())
                    .filter(a -> a.getCmds().contains(msg.getText().toLowerCase()) || a.getDisplayName(context.getLocale())
                            .equalsIgnoreCase(msg.getText()))
                    .findFirst()
                    .ifPresent(action -> action.init(context, null));
        }
    },

    SELECT_DEFAULTS("callableaction_select_defaults", List.of("standardwerte", "defaults")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText(context.getLocalizedString("set_delete_default_value"));

            message.setReplyMarkup(BotAction.createKeyboardMarkupWithMenu(2, context.getLocale(),
                    context.getLocalizedString("callableaction_set_defaults_set"),
                    context.getLocalizedString("callableaction_set_defaults_delete")));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (!update.hasMessage()) {
                return;
            }

            // Check if we returned from an internal state
            if (context.getReturnToAction() == this) {

                SendMessage message = new SendMessage();

                // Check if the user selected a canteen
                if (context.getSelectedCanteen() != null) {
                    context.setDefaultCanteen(context.getSelectedCanteen());
                    message.setText(context.getSelectedCanteen().getDisplayName() + " ✅");
                } else if (context.getSelectedLocale() != null) {
                    context.setLocale(context.getSelectedLocale());
                    message.setText(context.getSelectedLocale().getDisplayName() + " ✅");
                } else if (context.getSelectedDietType() != null) {
                    context.setDefaultDietType(context.getSelectedDietType());
                    message.setText(context.getSelectedDietType().getDisplayName(context.getLocale()) + " ✅");
                }

                // Reset everything prior to exiting the state
                context.resetPassthroughInformation();
                MAIN_MENU.init(context, message);
                return;
            }

            String text = update.getMessage().getText();
            if (text.equals(context.getLocalizedString("callableaction_set_defaults_set")) ||
                    text.equals(context.getLocalizedString("callableaction_set_defaults_delete"))) {

                boolean shouldBeSet = text.equals(context.getLocalizedString("callableaction_set_defaults_set"));
                context.setDefaultValueSet(shouldBeSet);

                SendMessage message = new SendMessage();
                message.setText(
                        context.getLocalizedString(shouldBeSet ? "which_default_value_should_be_set" : "which_default_value_should_be_deleted"));
                message.setReplyMarkup(BotAction.createKeyboardMarkupWithMenu(1, context.getLocale(),
                        context.getLocalizedString("canteen"),
                        context.getLocalizedString("language"),
                        context.getLocalizedString("diet")));
                context.sendMessage(message);

            } else if (text.equals(context.getLocalizedString("canteen"))) {

                // Check if the default canteen needs to be set or unset
                if (context.getDefaultValueSet()) {
                    // Canteen should be set
                    context.setReturnToAction(this);

                    InternalAction.SELECT_CANTEEN.init(context, null);
                } else {
                    // Canteen should be unset
                    context.setDefaultCanteen(null);

                    SendMessage message = new SendMessage();
                    message.setText(context.getLocalizedString("deleted_default_canteen"));
                    MAIN_MENU.init(context, message);
                }

            } else if (text.equals(context.getLocalizedString("language"))) {

                if (context.getDefaultValueSet()) {
                    context.setReturnToAction(this);

                    InternalAction.SELECT_LOCALE.init(context, null);
                } else {
                    context.setLocale(ResourceManager.DEFAULTLOCALE);

                    SendMessage message = new SendMessage();
                    message.setText(context.getLocalizedString("reset_selected_language"));
                    MAIN_MENU.init(context, message);
                }

            } else if (text.equals(context.getLocalizedString("diet"))) {
                // Check if the default diet needs to be set or unset
                if (context.getDefaultValueSet()) {
                    // Diet should be set
                    context.setReturnToAction(this); // Needed to make the internal action return to this action

                    InternalAction.SELECT_DIET_TYPE.init(context, null);
                } else {
                    // Diet should be unset
                    context.setDefaultDietType(null);

                    SendMessage message = new SendMessage();
                    message.setText(context.getLocalizedString("reset_selected_diet"));
                    MAIN_MENU.init(context, message);
                }
            } else {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null);
            }
        }
    },

    TUTORIAL("tutorial_name", List.of("/tutorial", "anleitung")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            SendMessage message = new SendMessage();
            message.enableMarkdownV2(true);
            message.setText(context.getLocalizedString("tutorial"));
            MAIN_MENU.init(context, message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {

        }
    };

    private final String bundleKey;
    private final List<String> cmds;

    CallableAction(String bundleKey, List<String> cmds) {
        this.bundleKey = bundleKey;
        this.cmds = cmds;
    }

    public String getDisplayName(Locale locale) {
        return ResourceManager.getString(bundleKey, locale);
    }

    public List<String> getCmds() {
        return cmds;
    }

}
