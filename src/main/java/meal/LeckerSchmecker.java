package meal;

import config.Config;
import database.DatabaseManager;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.LeckerSchmeckerBot;

import java.util.Date;
import java.util.logging.*;

public class LeckerSchmecker {

	private static final Logger logger;

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
		Config.readAllowedUsers();
		logger.setLevel(Level.INFO);

		DatabaseManager.connect();
		DatabaseManager.setupTables();

		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(new LeckerSchmeckerBot());
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}

		updateOffers();

		/* This is supposed to be executed before the program terminates. However, the program now doesn't terminate at all.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LeckerSchmecker.getLogger().info("LeckersSchmecker is shutting down. Cleaning up...");
			DatabaseManager.disconnect();
			LeckerSchmecker.getLogger().info("Bye.");
			System.exit(0);
		}));
		 */
	}

	public static void updateOffers() {
		Canteen.TYPES.forEach(Canteen::fetchDailyOffers);
		logger.info("Updated canteen offers");
	}

	public static Logger getLogger() {
		return logger;
	}
}
