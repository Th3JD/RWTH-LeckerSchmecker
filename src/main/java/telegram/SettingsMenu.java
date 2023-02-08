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

import java.util.LinkedList;
import java.util.List;
import localization.ResourceManager;
import meal.LeckerSchmecker;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class SettingsMenu {

    private final String setting;
    private int messageID;
    private final boolean singleChoice;
    private final int elementsInRow;
    private final ChatContext context;
    private final List<String> buttons;
    private final List<String> selected;
    private boolean done;

    public SettingsMenu(String setting, boolean singleChoice, int elementsInRow, ChatContext context, List<String> buttons, List<String> selected) {
        this.setting = setting;
        this.singleChoice = singleChoice;
        this.elementsInRow = elementsInRow;
        this.context = context;
        this.buttons = filterButtons(buttons);
        this.done = false;

        // Check if the list of selected buttons is valid
        if (singleChoice && selected.size() > 1) {
            this.selected = List.of(selected.get(0));
            LeckerSchmecker.getLogger().warning("Too many preselected buttons. Using first!");
        } else {
            this.selected = new LinkedList<>(selected);
        }
    }

    public void init(SendMessage message) {
        message.setReplyMarkup(generateMarkup());
        messageID = context.sendMessage(message);
    }

    public void delete() {
        EditMessageReplyMarkup editMessage = new EditMessageReplyMarkup();
        editMessage.setChatId(context.getChatID());
        editMessage.setMessageId(messageID);
        editMessage.setReplyMarkup(null);

        try {
            LeckerSchmeckerBot.getInstance().execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void updateSettings(Update update) {
        if (!update.hasCallbackQuery()) {
            LeckerSchmecker.getLogger().warning("SettingsMenu received update without callbackquery. Ignoring...");
            return;
        }

        boolean settingsModified = false;
        CallbackQuery query = update.getCallbackQuery();
        String pressedButton = query.getData();

        // Check if "Done" Button was pressed
        if (pressedButton.equals(ResourceManager.getString("done", context.getLocale()))) {
            done = true;
        } else {

            // Another button was pressed, adjust the list of selected buttons
            if (singleChoice) {
                if (!selected.contains(pressedButton)) {
                    selected.clear();
                    selected.add(pressedButton);
                    settingsModified = true;
                }
            } else {

                if (selected.contains(pressedButton)) {
                    selected.remove(pressedButton);
                } else {
                    selected.add(pressedButton);
                }
            }
        }

        if (settingsModified || done) {
            // Edit inline keyboard
            InlineKeyboardMarkup markup = null;
            if (!done) {
                markup = generateMarkup();
            }

            EditMessageReplyMarkup editMessage = new EditMessageReplyMarkup();
            editMessage.setChatId(context.getChatID());
            editMessage.setMessageId(messageID);
            editMessage.setReplyMarkup(markup);

            try {
                LeckerSchmeckerBot.getInstance().execute(editMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

    }

    private InlineKeyboardMarkup generateMarkup() {
        InlineKeyboardMarkup res = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> buttonLayout = new LinkedList<>();
        buttonLayout.add(new LinkedList<>());

        int currentElementsInRow = 0;
        int currentRow = 0;
        for (String buttonName : buttons) {
            if (currentElementsInRow == elementsInRow) {
                currentRow++;
                currentElementsInRow = 0;
                buttonLayout.add(new LinkedList<>());
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(getButtonDisplayString(buttonName));
            button.setCallbackData(buttonName);
            buttonLayout.get(currentRow).add(button);
            currentElementsInRow++;
        }

        InlineKeyboardButton doneButton = new InlineKeyboardButton();
        String doneText = ResourceManager.getString("done", context.getLocale());
        doneButton.setText(doneText);
        doneButton.setCallbackData(doneText);
        buttonLayout.add(new LinkedList<>(List.of(doneButton)));

        res.setKeyboard(buttonLayout);
        return res;
    }

    private String getButtonDisplayString(String buttonName) {
        if (singleChoice) {
            return (selected.contains(buttonName) ? "\uD83D\uDD18" : "⚪") + buttonName;
        } else {
            return buttonName + (selected.contains(buttonName) ? " ✅" : "");
        }
    }

    private LinkedList<String> filterButtons(List<String> buttons) {
        LinkedList<String> res = new LinkedList<>();

        for (String button : buttons) {

            // Check if a button is named "Done"
            if (button.equals(ResourceManager.getString("done", context.getLocale()))) {
                LeckerSchmecker.getLogger().warning("Removed invalid button '" + button + "' from the list of buttons (done)");
            }

            // Check if a button with the same name already exists
            if (res.contains(button)) {
                LeckerSchmecker.getLogger().warning("Removed invalid button '" + button + "' from the list of buttons (duplicate)");
                continue;
            }

            res.add(button);
        }
        return res;
    }

    public String getSetting() {
        return setting;
    }

    public List<String> getSelected() {
        return selected;
    }

    public boolean isDone() {
        return done;
    }
}
