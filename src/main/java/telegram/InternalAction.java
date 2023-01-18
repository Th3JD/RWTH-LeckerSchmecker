/*
 * RWTH-LeckerSchmecker
 * Copyright (c) 2023 Th3JD, ekansemit, 3dde
 *
 * This file is part of RWTH-LeckerSchmecker.
 *
 * RWTH-LeckerSchmecker is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with RWTH-LeckerSchmecker.
 * If not, see <https://www.gnu.org/licenses/>.
 */

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

public abstract class InternalAction implements BotAction {

    public static final InternalAction SELECT_DATE = new InternalAction() {

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
    };

    public static final InternalAction SELECT_CANTEEN = new InternalAction() {
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
    };

    public static final InternalAction SELECT_DIET_TYPE = new InternalAction() {
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
    };

    public static final InternalAction SELECT_MEAL = new InternalAction() {
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
    };

    public static final InternalAction SELECT_LOCALE = new InternalAction() {
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
    };

    public static final InternalAction RATE_MEAL = new InternalAction() {
        private final int ratingThreshold = Config.getInt("botaction.rate_meal.tutorial_threshold");

        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            context.sendMessage(passthroughMessage);
            context.setCurrentAction(this);

            MainMeal meal = context.getSelectedMeal();

            SendMessage msg = new SendMessage();
            if (context.getNumberOfVotes() < ratingThreshold) {
                msg.setText(context.getLocalizedString("rate_meal_tutorial", meal.getDisplayName(context.getLocale())));
            } else {
                msg.setText(context.getLocalizedString("rate_meal", meal.getDisplayName(context.getLocale())));
            }

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
    };

    public static final InternalAction[] VALUES = {SELECT_DATE, SELECT_CANTEEN, SELECT_DIET_TYPE,
            SELECT_MEAL, SELECT_LOCALE, RATE_MEAL};

    public static InternalAction[] values() {
        return VALUES;
    }
}
