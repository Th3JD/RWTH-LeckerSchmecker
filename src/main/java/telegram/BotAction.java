package telegram;

import meal.Canteen;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public enum BotAction {

	SELECT_DATE("Datum", List.of(), true){

		private final int LOOKAHEAD_DAYS = 3;
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
	SELECT_CANTEEN("Kantinen", List.of(), true){
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
	},

	MAIN_MENU("Hauptmenü", List.of("/start", "start", "exit", "menu", "menü"), false) {
		@Override
		public void init(ChatContext context, SendMessage passthroughMessage) {
			context.setCurrentAction(this);

			// Reset context in case the user quit while in an internal state
			context.resetTemporaryInformation();

			SendMessage message = passthroughMessage;
			if (message == null) {
				message = new SendMessage();
				message.setText("Wähle eine Aktion!");
			}

			message.setReplyMarkup(BotAction.createKeyboardMarkup(2,
					Arrays.stream(BotAction.values())
							.filter(a -> !a.isInternal())
							.map(BotAction::getDisplayName).toList()));

			context.sendMessage(message);
		}

		@Override
		public void onUpdate(ChatContext context, Update update) {
			Message msg = update.getMessage();
			Arrays.stream(BotAction.values())
					.filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].toLowerCase()))
					.findFirst()
					.ifPresent(action -> action.init(context, null));
		}
	},

	LIST_MEALS("Gerichte", List.of("gerichte", "essen"), false) {
		@Override
		public void init(ChatContext context, SendMessage passthroughMessage) {
			context.setCurrentAction(this);
			context.setReturnToAction(this); // Needed to make the internal action return to this action

			// Select date, selecting the canteen is done in onUpdate
			SELECT_DATE.init(context, null);
		}

		@Override
		public void onUpdate(ChatContext context, Update update) {
			if(!update.hasMessage()){
				return;
			}

			// Check if we returned from an internal state (should always be true)
			if(context.getReturnToAction() == this){

				// Check if we already know about the canteen (the date should always be set at this point)
				if(context.getDefaultCanteen() != null || context.getSelectedCanteen() != null){
					Canteen selectedCanteen = context.getDefaultCanteen() != null ? context.getDefaultCanteen() : context.getSelectedCanteen();

					SendMessage message = new SendMessage();
					message.enableMarkdownV2(true);
					message.setText("--------    *Gerichte*    --------\n\n" + context.getBot().getMealsText(selectedCanteen, context.getSelectedDate()));

					// Reset everything prior to exiting the state
					context.resetTemporaryInformation();

					MAIN_MENU.init(context, message);
				} else {

					// No canteen chosen so far
					SELECT_CANTEEN.init(context, null);
				}

			}
		}
	},

	RATING("Kritik", List.of("bewertung", "kritik", "rating"), false) {
		@Override
		public void init(ChatContext context, SendMessage passthroughMessage) {
			context.setCurrentAction(this);
			context.sendMessage(passthroughMessage);

			SendMessage sendMessage = new SendMessage();
			sendMessage.setText("Bald verfügbar!");

			BotAction.MAIN_MENU.init(context, sendMessage);
		}

		@Override
		public void onUpdate(ChatContext context, Update update) {

		}
	},

	SELECT_DEFAULTS("Standardwerte", List.of("standardwerte", "defaults"), false){
		@Override
		public void init(ChatContext context, SendMessage passthroughMessage) {
			context.setCurrentAction(this);

			SendMessage message = new SendMessage();
			message.setText("Soll ein Standardwert gesetzt oder gelöscht werden?");

			message.setReplyMarkup(BotAction.createKeyboardMarkup(1, "Setzen", "Löschen"));

			context.sendMessage(message);
		}

		@Override
		public void onUpdate(ChatContext context, Update update) {
			if(!update.hasMessage()){
				return;
			}

			// Check if we returned from an internal state
			if(context.getReturnToAction() == this){

				SendMessage message = new SendMessage();

				// Check if the user selected a canteen
				if(context.getSelectedCanteen() != null){
					context.setDefaultCanteen(context.getSelectedCanteen());
					message.setText(context.getSelectedCanteen().getDisplayName() + " \u2705");
				}

				// Reset everything prior to exiting the state
				context.resetTemporaryInformation();
				MAIN_MENU.init(context, message);
				return;
			}

			String text = update.getMessage().getText();
			switch (text){
				case "Setzen", "Löschen" -> {
					boolean shouldBeSet = text.equals("Setzen");
					context.setDefaultValueSet(shouldBeSet);

					SendMessage message = new SendMessage();
					message.setText("Welcher Standardwert soll " + (shouldBeSet ? "gesetzt" : "gelöscht") + " werden?");
					message.setReplyMarkup(BotAction.createKeyboardMarkup(1, "Mensa"));
					context.sendMessage(message);
				}
				case "Mensa" -> {

					// Check if the default canteen needs to be set or unset
					if(context.getDefaultValueSet()){
						// Canteen should be set
						context.setReturnToAction(this); // Needed to make the internal action return to this action

						SELECT_CANTEEN.init(context, null);
					} else {
						// Canteen should be unset
						context.setDefaultCanteen(null);

						SendMessage message = new SendMessage();
						message.setText("Deine Standardmensa wurde zurückgesetzt!");
						MAIN_MENU.init(context, message);
					}
					return;
				}
				default -> {
					context.sendMessage("Ungültige Option. Wähle eine Option oder kehre mit /start zum Hauptmenü zurück.");
					this.init(context, null);
				}
			}

		}
	};


	private final String displayName;
	private final List<String> cmds;
	private final boolean isInternal;

	BotAction(String displayName, List<String> cmds, boolean isInternal) {
		this.displayName = displayName;
		this.cmds = cmds;
		this.isInternal = isInternal;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getCmds() {
		return cmds;
	}

	public boolean isInternal() {
		return isInternal;
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

	public abstract void init(ChatContext context, SendMessage passthroughMessage);

	public abstract void onUpdate(ChatContext context, Update update);
}
