package telegram;

import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.util.*;

public enum BotAction {

	LIST_MEALS("Gerichte", List.of("gerichte", "essen")) {
		@Override
		public void init(LeckerSchmeckerBot bot, Long chatId, SendMessage passthroughMessage) {
			bot.setState(chatId, this);
			bot.sendMessage(chatId, passthroughMessage);

			SendMessage message = new SendMessage();
			message.setText("Wähle eine Mensa!");

			message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
					Canteen.TYPES.stream().map(Canteen::getDisplayName).toList()));

			bot.sendMessage(chatId, message);
		}

		@Override
		public void onUpdate(LeckerSchmeckerBot bot, Update update) {
			if (!update.hasMessage()) {
				return;
			}

			Message msg = update.getMessage();

			Optional<Canteen> canteenOpt = Canteen.getByDisplayName(msg.getText());
			if (canteenOpt.isEmpty()) {
				bot.sendTextMessage(update, "Unbekannte Mensa!");
				this.init(bot, msg.getChatId(), null);
				return;
			}

			SendMessage sendMessage = new SendMessage();
			sendMessage.enableMarkdownV2(true);

			sendMessage.setText("--------    *Gerichte*    --------\n\n" + bot.getMealsText(canteenOpt.get(), LocalDate.now()));

			BotAction.START.init(bot, msg.getChatId(), sendMessage);
		}
	},

	RATING("Kritik", List.of("bewertung", "kritik", "rating")) {
		@Override
		public void init(LeckerSchmeckerBot bot, Long chatId, SendMessage passthroughMessage) {
			bot.setState(chatId, this);
			bot.sendMessage(chatId, passthroughMessage);

			SendMessage sendMessage = new SendMessage();
			sendMessage.setText("Bald verfügbar!");

			BotAction.START.init(bot, chatId, sendMessage);
		}

		@Override
		public void onUpdate(LeckerSchmeckerBot bot, Update update) {

		}
	},

	START("Hauptmenü", List.of("/start", "start", "exit", "menu", "menü")) {
		@Override
		public void init(LeckerSchmeckerBot bot, Long chatId, SendMessage passthroughMessage) {
			bot.setState(chatId, this);

			SendMessage message = passthroughMessage;
			if (message == null) {
				message = new SendMessage();
				message.setText("Wähle eine Aktion!");
			}

			message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
					Arrays.stream(BotAction.values()).map(BotAction::getDisplayName).toList()));

			bot.sendMessage(chatId, message);
		}

		@Override
		public void onUpdate(LeckerSchmeckerBot bot, Update update) {
			Message msg = update.getMessage();
			Arrays.stream(BotAction.values())
					.filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].toLowerCase()))
					.findFirst()
					.ifPresent(action -> action.init(bot, update.getMessage().getChatId(), null));
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

	private static ReplyKeyboardMarkup createKeyboardMarkup(int elementsInRow, String... elements){
		return createKeyboardMarkup(elementsInRow, List.of(elements));
	}

	private static ReplyKeyboardMarkup createKeyboardMarkup(int elementsInRow, List<String> elements){
		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
		replyKeyboardMarkup.setSelective(true);
		replyKeyboardMarkup.setResizeKeyboard(true);
		replyKeyboardMarkup.setOneTimeKeyboard(false);


		List<KeyboardRow> keyboardRows = new ArrayList<>();
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

	public abstract void init(LeckerSchmeckerBot bot, Long chatId, SendMessage passthroughMessage);

	public abstract void onUpdate(LeckerSchmeckerBot bot, Update update);
}
