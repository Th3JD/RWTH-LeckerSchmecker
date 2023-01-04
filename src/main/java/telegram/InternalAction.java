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
import meal.MainMeal;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public enum InternalAction implements BotAction {

    SELECT_DATE {

        private final int LOOKAHEAD_DAYS = Config.getInt("botaction.select_date.daysToPresent");
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", Locale.GERMANY);

        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText("Wähle ein Datum!");

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

            String text = update.getMessage().getText();
            try {
                LocalDate selectedDate = LocalDate.parse(text, formatter);
                if (!selectedDate.isBefore(LocalDate.now().plusDays(LOOKAHEAD_DAYS))) {
                    context.sendMessage("Ungültiges Datum!");
                    this.init(context, null);
                    return;
                }

                context.setSelectedDate(selectedDate);
                context.getReturnToAction().onUpdate(context, update);

            } catch (DateTimeParseException e) {
                context.sendMessage("Ungültiges Datum!");
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
            message.setText("Wähle eine Mensa!");

            message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
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
                context.sendMessage("Unbekannte Mensa!");
                this.init(context, null);
                return;
            }

            context.setSelectedCanteen(canteenOpt.get());
            context.getReturnToAction().onUpdate(context, update);
        }
    },
    SELECT_MEAL {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);
            SendMessage msg = new SendMessage();
            msg.setText("Wähle ein Gericht!");
            msg.setReplyMarkup(BotAction.createKeyboardMarkup(1,
                    context.getCanteen().getDailyOffer(LocalDate.now()).get()
                            .getMainMeals().stream().map(MainMeal::getDisplayName).toList()));
            context.sendMessage(msg);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            context.setSelectedMeal(context.getCanteen()
                    .getDailyOffer(LocalDate.now()).get()
                    .getMainMealByDisplayName(update.getMessage().getText()).get());
            context.getReturnToAction().onUpdate(context, update);
        }
    },

    SELECT_LOCALE {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            SendMessage msg = new SendMessage();
            msg.setText("Wähle eine Sprache!");
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
            msg.setText("Gib eine Bewertung für '_" + meal.getDisplayName() + "_' ab!");
            msg.setReplyMarkup(BotAction.createKeyboardMarkup(5,
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Hauptmenü"));

            context.sendMessage(msg);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            String updateMsg = update.getMessage().getText();

            try {
                context.setRatedPoints(Integer.parseInt(updateMsg));
            } catch (NumberFormatException ignored) {
                if (updateMsg.equals("Hauptmenü")) {
                    context.resetPassthroughInformation();
                    CallableAction.MAIN_MENU.init(context, null);
                    return;
                }
                context.sendMessage("Ungültige Bewertung!");
            }
            context.getReturnToAction().onUpdate(context, update);
        }
    }


}
