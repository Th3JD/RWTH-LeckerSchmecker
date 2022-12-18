package telegram;

import config.Config;
import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum InternalAction implements BotAction{

    SELECT_DATE{

        private final int LOOKAHEAD_DAYS = Config.getInt("botaction.select_date.daysToPresent");
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE',' dd.MM.yyyy", Locale.GERMANY);
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText("Wähle ein Datum!");

            List<String> queryDates = new LinkedList<>();
            LocalDate today = LocalDate.now();
            for(int i = 0; i < LOOKAHEAD_DAYS; i++) {
                queryDates.add(today.plusDays(i).format(formatter));
            }

            message.setReplyMarkup(BotAction.createKeyboardMarkup(1, queryDates));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if(!update.hasMessage()){ // User did not answer with a text message
                this.init(context, null);
            }

            String text = update.getMessage().getText();
            try {
                LocalDate selectedDate = LocalDate.parse(text, formatter);
                if(!selectedDate.isBefore(LocalDate.now().plusDays(LOOKAHEAD_DAYS))){
                    context.sendMessage("Ungültiges Datum!");
                    this.init(context, null);
                    return;
                }

                context.setSelectedDate(selectedDate);
                context.getReturnToAction().onUpdate(context, update);

            } catch (DateTimeParseException e){
                context.sendMessage("Ungültiges Datum!");
                this.init(context, null);
            }


        }
    },
    SELECT_CANTEEN{
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
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
    };





}
