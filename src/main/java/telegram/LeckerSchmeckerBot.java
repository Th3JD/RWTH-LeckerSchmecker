package telegram;

import config.Config;
import meal.Canteen;
import meal.DailyOffer;
import meal.LeckerSchmecker;
import meal.Meal;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.*;

public class LeckerSchmeckerBot extends TelegramLongPollingBot {

	private Map<Long, BotAction> botActionByChatId = new HashMap<>();

	/**
	 * Return username of this bot
	 */
	@Override
	public String getBotUsername() {
		return Config.getString("telegram.name");
	}

	/**
	 * Returns the token of the bot to be able to perform Telegram Api Requests
	 *
	 * @return Token of the bot
	 */
	@Override
	public String getBotToken() {
		return Config.getString("telegram.token");
	}

	/**
	 * This method is called when receiving updates via GetUpdates method
	 *
	 * @param update Update received
	 */
	@Override
	public void onUpdateReceived(Update update) {
		Long chatId = update.getMessage().getChatId();

		// Check if user is allowed to take part in the closed beta
		if(!Config.isAllowedUser(chatId)){
			sendTextMessage(chatId, "Dieser Bot steht momentan noch nicht zur Verfügung!");
			LeckerSchmecker.getLogger().info("Unerlaubter Nutzer mit chatID " + chatId + " hat ein Update ausgelöst.");
			return;
		}


		if (update.hasMessage() && BotAction.START.getCmds().contains(update.getMessage().getText().split(" ")[0].toLowerCase())) {
			this.botActionByChatId.remove(chatId);
		}

		// an action is running
		if (this.botActionByChatId.get(chatId) != null) {
			this.botActionByChatId.get(chatId).onUpdate(this, update);
		// start new action
		} else if (update.hasMessage()) {
			Message msg = update.getMessage();

			// find action
			BotAction action = Arrays.stream(BotAction.values())
					.filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].toLowerCase()))
					.findFirst().orElse(null);

			if (action != null) {
				action.init(this, update.getMessage().getChatId(), null);
			} else {
				BotAction.START.init(this, update.getMessage().getChatId(), null);
			}
		} else {
			BotAction.START.init(this, update.getMessage().getChatId(), null);
		}
	}

	public void sendTextMessage(Long chatId, String message) {
		if (message == null) return;
		SendMessage sendMessage = new SendMessage();
		sendMessage.setText(message);
		this.sendMessage(chatId, sendMessage);
	}

	public void sendTextMessage(Update update, String message) {
		if (message == null) return;
		SendMessage sendMessage = new SendMessage();
		sendMessage.setText(message);
		this.sendMessage(update, sendMessage);
	}

	public void sendMessage(Long chatId, SendMessage message) {
		if (message == null) return;
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

	public void setState(Long chatId, BotAction botAction) {
		this.botActionByChatId.put(chatId, botAction);
	}
}
