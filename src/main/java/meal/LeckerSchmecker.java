package meal;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.LeckerSchmeckerBot;

import java.util.Date;
import java.util.logging.*;

public class LeckerSchmecker {

	private static Logger logger;

	static {
		logger = Logger.getLogger("RWTH-LS");
		logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {
			private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format,
						new Date(lr.getMillis()),
						lr.getLevel().getLocalizedName(),
						lr.getMessage()
				);
			}
		});
		logger.addHandler(handler);
	}

	public static void main(String[] args) {
		logger.setLevel(Level.INFO);

		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(new LeckerSchmeckerBot());
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}

		updateOffers();
	}

	public static void updateOffers() {
		Canteen.TYPES.forEach(Canteen::fetchDailyOffers);
		logger.info("Updated canteen offers");
	}

	public static Logger getLogger() {
		return logger;
	}
}
