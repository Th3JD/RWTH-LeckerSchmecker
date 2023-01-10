package telegram;

import config.Config;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import localization.ResourceManager;
import meal.Canteen;
import meal.DietType;
import meal.MainMeal;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public enum InternalAction implements BotAction {

    SELECT_DATE {

        private final int LOOKAHEAD_DAYS = Config.getInt("botaction.select_date.daysToPresent");

        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", context.getLocale());

            SendMessage message = new SendMessage();
            message.setText(context.getLocalizedString("select_a_date"));

            List<String> queryDates = new LinkedList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < LOOKAHEAD_DAYS; i++) {
                queryDates.add(today.plusDays(i).format(formatter));
            }

            message.setReplyMarkup(BotAction.createKeyboardMarkup(1, queryDates));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (!update.hasMessage()) { // User did not answer with a text message
                this.init(context, null);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", context.getLocale());

            String text = update.getMessage().getText();
            try {
                LocalDate selectedDate = LocalDate.parse(text, formatter);
                if (!selectedDate.isBefore(LocalDate.now().plusDays(LOOKAHEAD_DAYS))) {
                    context.sendLocalizedMessage("invalid_option");
                    this.init(context, null);
                    return;
                }

                context.setSelectedDate(selectedDate);
                context.getReturnToAction().onUpdate(context, update);

            } catch (DateTimeParseException e) {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null);
            }


        }
    },
    SELECT_CANTEEN {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText(context.getLocalizedString("select_a_canteen"));

            message.setReplyMarkup(BotAction.createKeyboardMarkupWithMenu(2, context.getLocale(),
                    Canteen.TYPES.stream().map(Canteen::getDisplayName).toList()));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (!update.hasMessage()) {
                return;
            }

            Message msg = update.getMessage();
            Optional<Canteen> canteenOpt = Canteen.getByDisplayName(msg.getText());
            if (canteenOpt.isEmpty()) {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null);
                return;
            }

            context.setSelectedCanteen(canteenOpt.get());
            context.getReturnToAction().onUpdate(context, update);
        }
    },
    SELECT_DIET_TYPE {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText(context.getLocalizedString("choose_diet"));
            message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
                    DietType.TYPES.stream().map(a -> a.getDisplayName(context.getLocale())).toList()));
            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (!update.hasMessage()) {
                return;
            }

            Message msg = update.getMessage();
            Optional<DietType> dietTypeOpt = DietType.getByDisplayName(msg.getText(), context.getLocale());
            if (dietTypeOpt.isEmpty()) {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null);
                return;
            }

            context.setSelectedDietType(dietTypeOpt.get());
            context.getReturnToAction().onUpdate(context, update);
        }
    },
    SELECT_MEAL {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);
            SendMessage msg = new SendMessage();
            msg.setText(context.getLocalizedString("select_a_meal"));
            msg.setReplyMarkup(BotAction.createKeyboardMarkup(1,
                    context.getCanteen().getDailyOffer(LocalDate.now()).get()
                            .getMainMeals().stream().map(a -> a.getDisplayName(context.getLocale())).toList()));
            context.sendMessage(msg);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            context.setSelectedMeal(context.getCanteen()
                    .getDailyOffer(LocalDate.now()).get()
                    .getMainMealByDisplayName(update.getMessage().getText(), context.getLocale()).get());
            context.getReturnToAction().onUpdate(context, update);
        }
    },
    SELECT_LOCALE {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            SendMessage msg = new SendMessage();
            msg.setText(context.getLocalizedString("select_a_language"));
            msg.setReplyMarkup(BotAction.createKeyboardMarkup(1,
                    ResourceManager.LOCALES.stream().map(Locale::getDisplayName).toList()));
            context.sendMessage(msg);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            Optional<Locale> loc = ResourceManager.LOCALES.stream()
                    .filter(a -> a.getDisplayName().equals(update.getMessage().getText()))
                    .findFirst();
            if (loc.isPresent()) {
                context.setSelectedLocale(loc.get());
                context.getReturnToAction().onUpdate(context, update);
            } else {
                this.init(context, null);
            }
        }
    },
    RATE_MEAL() {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            MainMeal meal = context.getSelectedMeal();

            SendMessage msg = new SendMessage();
            msg.setText(context.getLocalizedString("rate_meal", meal.getDisplayName(context.getLocale())));
            msg.enableMarkdown(true);
            msg.setReplyMarkup(BotAction.createKeyboardMarkupWithMenu(5, context.getLocale(),
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));

            context.sendMessage(msg);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            String updateMsg = update.getMessage().getText();

            try {
                context.setRatedPoints(Integer.parseInt(updateMsg));
            } catch (NumberFormatException ignored) {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null);
            }
            context.getReturnToAction().onUpdate(context, update);
        }
    }


}
