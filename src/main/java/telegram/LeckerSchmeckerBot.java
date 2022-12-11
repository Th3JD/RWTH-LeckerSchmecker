package telegram;

import config.Config;
import meal.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.*;

public class LeckerSchmeckerBot extends TelegramLongPollingBot {

	private Map<Long, ChatContext> chatContextById = new HashMap<>();

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

		// Create ChatContext if it does not exist
		ChatContext context;
		if(!chatContextById.containsKey(chatId)){
			context = new ChatContext(this, chatId);
			chatContextById.put(chatId, context);
		} else {
			context = chatContextById.get(chatId);
		}

		// Reset the current action if the user wants to access the main menu
		if (update.hasMessage() && BotAction.MAIN_MENU.getCmds().contains(update.getMessage().getText().split(" ")[0].toLowerCase())) {
			context.setCurrentAction(null);
		}

		// An action is currently running -> update
		if (context.hasCurrentAction()){
			context.getCurrentAction().onUpdate(context, update);
		// No action is currently running -> start new action
		} else if (update.hasMessage()) {
			Message msg = update.getMessage();

			// Find action requested by the user
			BotAction action = Arrays.stream(BotAction.values())
					.filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].toLowerCase()))
					.findFirst().orElse(null);

			if (action != null) {
				action.init(context, null);
			} else {
				BotAction.MAIN_MENU.init(context, null);
			}
		} else {
			BotAction.MAIN_MENU.init(context, null);
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

		DailyOffer offer = offerOpt.get();

		StringBuilder sb = new StringBuilder();
		for (MainMeal meal : offer.getMainMeals()) {
			sb.append("*").append(meal.getType().getDisplayName()).append("*").append("\n")
					.append(meal.text()).append("\n\n");
		}

		sb.append("*Hauptbeilagen*").append("\n");
		sb.append(String.join(" oder ",
				offer.getSideMeals(SideMeal.Type.MAIN).stream().map(SideMeal::getDisplayName).toList()));
		sb.append("\n\n");

		sb.append("*Nebenbeilagen*").append("\n");
		sb.append(String.join(" oder ",
				offer.getSideMeals(SideMeal.Type.SIDE).stream().map(SideMeal::getDisplayName).toList()));

		return sb.toString();
	}

}
