package telegram;

import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public enum BotAction {

	START("Menu", List.of("/start", "start", "exit", "menu")) {
		@Override
		public boolean init(LeckerSchmeckerBot bot, Update update) {
			SendMessage sendMessage = new SendMessage();

			ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
			sendMessage.setReplyMarkup(replyKeyboardMarkup);
			replyKeyboardMarkup.setSelective(true);
			replyKeyboardMarkup.setResizeKeyboard(true);
			replyKeyboardMarkup.setOneTimeKeyboard(false);

			List<KeyboardRow> keyboard = new ArrayList<>();
			KeyboardRow keyboardRow = new KeyboardRow();

			int elementsInRow = 0;
			for (BotAction action : BotAction.values()) {
				if (elementsInRow == 2) {
					keyboard.add(keyboardRow);
					keyboardRow = new KeyboardRow();
				}
				keyboardRow.add(new KeyboardButton(action.getDisplayName()));
				elementsInRow++;
			}
			keyboard.add(keyboardRow);

			replyKeyboardMarkup.setKeyboard(keyboard);

			bot.sendMessage(update, sendMessage);
			return true;
		}

		@Override
		public boolean onUpdate(LeckerSchmeckerBot bot, Update update) {
			return true;
		}
	},

	LIST_MEALS("Gerichte", List.of("gerichte", "essen")) {
		@Override
		public boolean init(LeckerSchmeckerBot bot, Update update) {
			SendMessage sendMessage = new SendMessage();
			sendMessage.setText("Wähle eine Mensa!");

			ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
			sendMessage.setReplyMarkup(replyKeyboardMarkup);
			replyKeyboardMarkup.setSelective(true);
			replyKeyboardMarkup.setResizeKeyboard(true);
			replyKeyboardMarkup.setOneTimeKeyboard(false);

			List<KeyboardRow> keyboard = new ArrayList<>();
			KeyboardRow keyboardRow = new KeyboardRow();

			int elementsInRow = 0;
			for (Canteen canteen : Canteen.TYPES) {
				if (elementsInRow == 2) {
					keyboard.add(keyboardRow);
					keyboardRow = new KeyboardRow();
				}
				keyboardRow.add(new KeyboardButton(canteen.getDisplayName()));
				elementsInRow++;
			}
			keyboard.add(keyboardRow);

			replyKeyboardMarkup.setKeyboard(keyboard);

			bot.sendMessage(update, sendMessage);
			return false;
		}

		@Override
		public boolean onUpdate(LeckerSchmeckerBot bot, Update update) {
			if (!update.hasMessage()) {
				return false;
			}

			Message msg = update.getMessage();

			Optional<Canteen> canteenOpt = Canteen.getByDisplayName(msg.getText());
			if (canteenOpt.isEmpty()) {
				bot.sendTextMessage(update, "Unbekannte Mensa!");
				return this.init(bot, update);
			}

			SendMessage sendMessage = new SendMessage();
			sendMessage.enableMarkdown(true);

			sendMessage.setText("*Gerichte:*\n" + bot.getMealsText(canteenOpt.get(), LocalDate.now()));
			bot.sendMessage(update, sendMessage);

			return true;
		}
	};

	private final String displayName;
	private final List<String> cmds;

	BotAction(String displayName, List<String> cmds) {
		this.displayName = displayName;
		this.cmds = cmds;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getCmds() {
		return cmds;
	}

	public abstract boolean init(LeckerSchmeckerBot bot, Update update);

	public abstract boolean onUpdate(LeckerSchmeckerBot bot, Update update);
}
