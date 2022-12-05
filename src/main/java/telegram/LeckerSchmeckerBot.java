package telegram;

import meal.Canteen;
import meal.DailyOffer;
import meal.Meal;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

public class LeckerSchmeckerBot extends TelegramLongPollingBot {

	private BotAction botAction;

	/**
	 * Return username of this bot
	 */
	@Override
	public String getBotUsername() {
		return "rwth_leckerschmecker_bot";
	}

	/**
	 * Returns the token of the bot to be able to perform Telegram Api Requests
	 *
	 * @return Token of the bot
	 */
	@Override
	public String getBotToken() {
		return "";
	}

	/**
	 * This method is called when receiving updates via GetUpdates method
	 *
	 * @param update Update received
	 */
	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasMessage() && update.getMessage().getText().startsWith("exit")) {
			this.botAction = null;
		}

		if (this.botAction != null) {
			boolean finishAction = this.botAction.onUpdate(this, update);
			if (finishAction) {
				if (this.botAction != BotAction.START) BotAction.START.init(this, update);
				this.botAction = null;
			}
		} else if (update.hasMessage()) {
			Message msg = update.getMessage();
			this.botAction = Arrays.stream(BotAction.values())
					.filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].trim().toLowerCase()))
					.findFirst().orElse(null);
			if (this.botAction != null) {
				boolean finishAction = this.botAction.init(this, update);
				if (finishAction) {
					if (this.botAction != BotAction.START) BotAction.START.init(this, update);
					this.botAction = null;
				}
			}
		}
	}

	public void sendTextMessage(Long chatId, String message) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.setText(message);
		this.sendMessage(chatId, sendMessage);
	}

	public void sendTextMessage(Update update, String message) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.setText(message);
		this.sendMessage(update, sendMessage);
	}

	public void sendMessage(Long chatId, SendMessage message) {
		message.setChatId(chatId);
		message.enableMarkdown(true);
		try {
			this.execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(Update update, SendMessage message) {
		this.sendMessage(update.getMessage().getChatId(), message);
	}

	public String getMealsText(Canteen canteen, LocalDate date) {
		Optional<DailyOffer> offerOpt = canteen.getDailyOffer(date);

		if (offerOpt.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Meal meal : offerOpt.get().getMeals()) {
			sb.append("*").append(meal.getType().getDisplayName()).append("*").append("\n")
					.append(meal.text()).append("\n\n");
		}

		return sb.toString();
	}
}
