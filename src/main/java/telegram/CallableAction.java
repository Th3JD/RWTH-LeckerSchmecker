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
import database.DatabaseManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javassist.bytecode.LocalVariableTypeAttribute;
import localization.ResourceManager;
import meal.Canteen;
import meal.DietType;
import meal.LeckerSchmecker;
import meal.MainMeal;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import util.DateUtils;

public abstract class CallableAction implements BotAction {

    public static final CallableAction LIST_MEALS = new CallableAction("callableaction_list_meals",
            List.of("gerichte", "essen", "meals", "comidas")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage, Update update) {
            context.setCurrentAction(this);
            context.setReturnToAction(
                    this); // Needed to make the internal action return to this action

            // Select date, selecting the canteen is done in onUpdate
            InternalAction.SELECT_DATE.init(context, null, update);
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

                    MAIN_MENU.init(context, message, update);
                } else {

                    // No canteen chosen so far
                    InternalAction.SELECT_CANTEEN.init(context, null, update);
                }

            }
        }
    };

    public static final CallableAction RATING = new CallableAction("callableaction_rate",
            List.of("bewertung", "kritik", "rating", "evaluar")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage, Update update) {
            context.setCurrentAction(this);
            context.sendMessage(passthroughMessage);

            context.setReturnToAction(this);

            // Check if user already used up their votes for today
            if (DatabaseManager.numberOfRatingsByDate(context, LocalDate.now()) >= Config.getMaxVotesPerDay()) {
                SendMessage message = new SendMessage();
                message.setText(ResourceManager.getString("already_rated", context.getLocale(), Config.getMaxVotesPerDay()));
                message.setReplyMarkup(BotAction.createKeyboardMarkupWithMenu(1,
                        context.getLocale(), context.getLocalizedString("continue")));
                message.enableMarkdown(true);
                context.sendMessage(message);
                return;
            }

            if (context.hasCanteen()) {

                // Check if the canteen offers meals today
                if (context.getCanteen().getDailyOffer(LocalDate.now()).isEmpty()) {
                    SendMessage msg = new SendMessage();
                    msg.setText(
                            ResourceManager.getString("canteen_offers_no_meals_today", context.getLocale(), context.getCanteen().getDisplayName()));
                    msg.enableMarkdown(true);
                    MAIN_MENU.init(context, msg, update);
                    return;
                }

                InternalAction.SELECT_MEAL.init(context, null, update);
            } else {
                InternalAction.SELECT_CANTEEN.init(context, null, update);
            }
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {

            //Check if the user agreed to delete all ratings from today
            if (update.hasMessage() && update.getMessage().getText().equals(context.getLocalizedString("continue"))) {
                DatabaseManager.deleteRatingsAtDate(context, LocalDate.now());
                RATING.init(context, null, update);
                return;
            }

            if (!context.hasCanteen()) {
                context.setReturnToAction(this);
                InternalAction.SELECT_CANTEEN.init(context, null, update);
                return;
            }

            Canteen canteen = context.getCanteen();
            LocalTime currentTime = LocalTime.now();

            if (currentTime.isBefore(canteen.getOpeningTime())) {
                MAIN_MENU.init(context, new SendMessage(String.valueOf(context.getChatID()),
                        context.getLocalizedString("canteen_not_open_yet",
                                canteen.getDisplayName())), update);
                context.resetPassthroughInformation();
                return;
            }

            // Check if the canteen offers meals today
            if (canteen.getDailyOffer(LocalDate.now()).isEmpty()) {
                SendMessage msg = new SendMessage();
                msg.setText(ResourceManager.getString("canteen_offers_no_meals_today", context.getLocale(), context.getCanteen().getDisplayName()));
                msg.enableMarkdown(true);
                MAIN_MENU.init(context, msg, update);
                return;
            }

            if (!context.hasMealSelected()) {
                context.setReturnToAction(this);
                InternalAction.SELECT_MEAL.init(context, null, update);
                return;
            }

            MainMeal meal = context.getSelectedMeal();

            if (meal.getId() == null) {
                context.resetPassthroughInformation();
                SendMessage msg = new SendMessage(String.valueOf(context.getChatID()),
                        context.getLocalizedString("meal_not_ratable", meal.getDisplayName(context.getLocale())));
                msg.enableMarkdown(true);
                MAIN_MENU.init(context, msg, update);
                return;
            }

            if (!context.hasRated()) {
                context.setReturnToAction(this);
                InternalAction.RATE_MEAL.init(context, null, update);
                return;
            }

            int ratedPoints = context.getRatedPoints();

            DatabaseManager.rateMeal(context, meal, ratedPoints);
            context.incrementNumberOfVotes();

            context.resetPassthroughInformation();
            SendMessage msg = new SendMessage(String.valueOf(context.getChatID()),
                    "_" + meal.getDisplayName(context.getLocale()) + "_: *" + ratedPoints + "*");
            msg.enableMarkdown(true);
            MAIN_MENU.init(context, msg, update);
        }
    };

    public static final CallableAction MAIN_MENU = new CallableAction("callableaction_main_menu",
            List.of("/start", "start", "exit", "menu", "menü", "hauptmenü", "inicio", "reset")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage, Update update) {
            context.setCurrentAction(this);

            // Reset context in case the user quit while in an internal state
            context.resetPassthroughInformation();

            SendMessage message = passthroughMessage;
            if (message == null) {
                message = new SendMessage();
                message.setText(context.getLocalizedString("choose_action"));
            }

            message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
                    Arrays.stream(CallableAction.MAIN_MENU_ACTIONS)
                            .map(a -> a.getDisplayName(context.getLocale())).toList()));

            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            Message msg = update.getMessage();
            Arrays.stream(CallableAction.values())
                    .filter(a -> a.getCmds().contains(msg.getText().toLowerCase())
                            || a.getDisplayName(context.getLocale())
                            .equalsIgnoreCase(msg.getText()))
                    .findFirst()
                    .ifPresent(action -> action.init(context, null, update));
        }
    };

    public static final CallableAction SETTINGS_MENU = new CallableAction("callableaction_settings",
            List.of("settings", "einstellungen")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage, Update update) {
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText(context.getLocalizedString("which_setting_to_change"));

            ReplyKeyboardMarkup markup = BotAction.createKeyboardMarkupWithMenu(2,
                    context.getLocale(),
                    context.getLocalizedString("canteen"),
                    context.getLocalizedString("language"),
                    context.getLocalizedString("diet"),
                    context.getLocalizedString("compact_layout"),
                    context.getLocalizedString("automated_query"));
            message.setReplyMarkup(markup);
            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            if (update.hasCallbackQuery()) {
                SettingsMenu menu = context.getSettingsMenu();
                menu.updateSettings(update);

                if (menu.isDone()) {
                    context.setSettingsMenu(null);

                    SendMessage message = new SendMessage();

                    // Check which setting was changed
                    String setting = menu.getSetting();
                    if (setting.equals(context.getLocalizedString("canteen"))) {

                        // Check if user wants to reset the default canteen
                        if (menu.getSelected().get(0).equals(ResourceManager.getString("no_default_canteen", context.getLocale()))) {
                            context.setDefaultCanteen(null);
                            message.setText(ResourceManager.getString("no_default_canteen", context.getLocale()) + " ✅");
                        } else {
                            Optional<Canteen> canteenOpt = Canteen.getByDisplayName(menu.getSelected().get(0));
                            if (canteenOpt.isEmpty()) {
                                LeckerSchmecker.getLogger().warning("Invalid canteen returned by setting menu. Ignoring...");
                                context.sendLocalizedMessage("invalid_option");
                                this.init(context, null, update);
                                return;
                            }
                            context.setDefaultCanteen(canteenOpt.get());
                            message.setText(context.getDefaultCanteen().getDisplayName() + " ✅");
                        }

                    } else if (setting.equals(context.getLocalizedString("language"))) {
                        Optional<Locale> localeOpt = ResourceManager.LOCALES.stream()
                                .filter(a -> a.getDisplayName().equals(menu.getSelected().get(0)))
                                .findFirst();
                        if (localeOpt.isEmpty()) {
                            LeckerSchmecker.getLogger().warning("Invalid locale returned by setting menu. Ignoring...");
                            context.sendLocalizedMessage("invalid_option");
                            this.init(context, null, update);
                            return;
                        }
                        context.setLocale(localeOpt.get());
                        message.setText(context.getLocale().getDisplayName() + " ✅");

                    } else if (setting.equals(context.getLocalizedString("diet"))) {
                        Optional<DietType> dietTypeOpt = DietType.getByDisplayName(menu.getSelected().get(0), context.getLocale());
                        if (dietTypeOpt.isEmpty()) {
                            LeckerSchmecker.getLogger().warning("Invalid dietType returned by setting menu. Ignoring...");
                            context.sendLocalizedMessage("invalid_option");
                            this.init(context, null, update);
                            return;
                        }
                        context.setDefaultDietType(dietTypeOpt.get());
                        message.setText(context.getDefaultDietType().getDisplayName(context.getLocale()) + " ✅");
                    } else if (setting.equals(context.getLocalizedString("automated_query"))) {
                        if (menu.getSelected().get(0).equals(ResourceManager.getString("off", context.getLocale()))) {
                            context.setAutomatedQueryTime(null);
                            message.setText(ResourceManager.getString("off", context.getLocale()) + " ✅");
                        } else {
                            Optional<LocalTime> timeOpt = DateUtils.TIME_OPTIONS.stream()
                                    .filter(a -> a.toString().equals(menu.getSelected().get(0)))
                                    .findFirst();
                            if (timeOpt.isEmpty()) {
                                LeckerSchmecker.getLogger().warning("Invalid time returned by setting menu. Ignoring...");
                                context.sendLocalizedMessage("invalid_option");
                                this.init(context, null, update);
                                return;
                            }
                            context.setAutomatedQueryTime(timeOpt.get());
                            message.setText(context.getAutomatedQueryTime().toString() + " ✅");
                        }
                    }
                    MAIN_MENU.init(context, message, update);
                }
                return;
            }

            // User is selecting a setting to change
            if (!update.hasMessage()) {
                return;
            }

            // Check if user is already in a menu
            if (context.getSettingsMenu() != null) {
                context.getSettingsMenu().delete();
                context.setSettingsMenu(null);
            }

            String text = update.getMessage().getText();
            if (text.equals(context.getLocalizedString("canteen"))) {
                List<String> options = new LinkedList<>(Canteen.TYPES.stream().map(Canteen::getDisplayName).toList());
                options.add(ResourceManager.getString("no_default_canteen", context.getLocale()));
                SettingsMenu menu = new SettingsMenu(text, true, 2, context,
                        options,
                        context.getDefaultCanteen() == null ? List.of() : List.of(context.getDefaultCanteen().getDisplayName()));
                context.setSettingsMenu(menu);

                SendMessage message = new SendMessage();
                message.setText(context.getLocalizedString("select_a_canteen"));
                menu.init(message);

            } else if (text.equals(context.getLocalizedString("language"))) {
                SettingsMenu menu = new SettingsMenu(text, true, 1, context,
                        ResourceManager.LOCALES.stream().map(Locale::getDisplayName).toList(),
                        List.of(context.getLocale().getDisplayName()));
                context.setSettingsMenu(menu);

                SendMessage message = new SendMessage();
                message.setText(context.getLocalizedString("select_a_language"));
                menu.init(message);

            } else if (text.equals(context.getLocalizedString("diet"))) {
                SettingsMenu menu = new SettingsMenu(text, true, 2, context,
                        DietType.TYPES.stream().map(a -> a.getDisplayName(context.getLocale())).toList(),
                        List.of(context.getDietType().getDisplayName(context.getLocale())));
                context.setSettingsMenu(menu);

                SendMessage message = new SendMessage();
                message.setText(context.getLocalizedString("choose_diet"));
                menu.init(message);

            } else if (text.equals(context.getLocalizedString("compact_layout"))) {
                context.setCompactLayout(!context.isCompactLayout());
                if (context.isCompactLayout()) {
                    context.sendLocalizedMessage("set_compact_layout");
                } else {
                    context.sendLocalizedMessage("reset_compact_layout");
                }
                MAIN_MENU.init(context, null, update);
            } else if (text.equals(context.getLocalizedString("automated_query"))) {
                List<String> options = new LinkedList<>(DateUtils.TIME_OPTIONS.stream().map(LocalTime::toString).toList());
                options.add(ResourceManager.getString("off", context.getLocale()));
                SettingsMenu menu = new SettingsMenu(text, true, 2, context,
                        options,
                        context.getAutomatedQueryTime() == null ?
                                List.of(ResourceManager.getString("off", context.getLocale()))
                                : List.of(context.getAutomatedQueryTime().toString()));
                context.setSettingsMenu(menu);

                SendMessage message = new SendMessage();
                message.setText(context.getLocalizedString("choose_automated_query"));
                menu.init(message);
            } else {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null, update);
            }


        }
    };

    public static final CallableAction TUTORIAL = new CallableAction("tutorial_name",
            List.of("/tutorial", "anleitung")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage, Update update) {
            context.setCurrentAction(this);

            SendMessage message = new SendMessage();
            message.setText(context.getLocalizedString("which_tutorial"));

            ReplyKeyboardMarkup markup = BotAction.createKeyboardMarkupWithMenu(2,
                    context.getLocale(),
                    context.getLocalizedString("query_tutorial_name"),
                    context.getLocalizedString("rating_tutorial_name"),
                    context.getLocalizedString("settings_tutorial_name"));
            message.setReplyMarkup(markup);
            context.sendMessage(message);
        }

        @Override
        public void onUpdate(ChatContext context, Update update) {
            String text = update.getMessage().getText();
            SendMessage message = new SendMessage();
            message.enableMarkdownV2(true);
            if (text.equals(context.getLocalizedString("query_tutorial_name"))) {
                message.setText(context.getLocalizedString("query_tutorial"));
                MAIN_MENU.init(context, message, update);

            } else if (text.equals(context.getLocalizedString("rating_tutorial_name"))) {
                message.setText(context.getLocalizedString("rating_tutorial"));
                MAIN_MENU.init(context, message, update);

            } else if (text.equals(context.getLocalizedString("settings_tutorial_name"))) {
                message.setText(context.getLocalizedString("settings_tutorial"));
                MAIN_MENU.init(context, message, update);

            } else {
                context.sendLocalizedMessage("invalid_option");
                this.init(context, null, update);
            }
        }
    };

    public static final CallableAction[] VALUES = {LIST_MEALS, RATING, MAIN_MENU, TUTORIAL, SETTINGS_MENU};
    public static final CallableAction[] MAIN_MENU_ACTIONS = {LIST_MEALS, RATING, SETTINGS_MENU, TUTORIAL};

    public static CallableAction[] values() {
        return VALUES;
    }

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
