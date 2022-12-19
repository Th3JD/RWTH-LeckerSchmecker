package telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public interface BotAction {

    void init(ChatContext context, SendMessage passthroughMessage);

    void onUpdate(ChatContext context, Update update);

    static ReplyKeyboardMarkup createKeyboardMarkup(int elementsInRow, String... elements){
        return createKeyboardMarkup(elementsInRow, List.of(elements));
    }

    static ReplyKeyboardMarkup createKeyboardMarkup(int elementsInRow, List<String> elements){
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);


        List<KeyboardRow> keyboardRows = new LinkedList<>();
        KeyboardRow keyboardRow = new KeyboardRow();

        int currentElementsInRow = 0;
        for (String s : elements) {
            if (currentElementsInRow == elementsInRow) {
                keyboardRows.add(keyboardRow);
                keyboardRow = new KeyboardRow();
                currentElementsInRow = 0;
            }
            keyboardRow.add(new KeyboardButton(s));
            currentElementsInRow++;
        }
        keyboardRows.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

}
