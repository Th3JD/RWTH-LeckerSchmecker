package meal;

import config.Config;
import database.DatabaseManager;
import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.LeckerSchmeckerBot;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.*;

public class LeckerSchmecker {

    private static Logger logger;

    private static final Object exit = Void.TYPE;

    private static final Timer timer = new Timer();

    public static void main(String[] args) {
        Config.readAllowedUsers();
        initLogger();
        logger.setLevel(Level.INFO);

        DatabaseManager.connect();
        DatabaseManager.setupTables();

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new LeckerSchmeckerBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // schedule update task without a delay
        timer.schedule(new UpdateTask(), 0);


        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(LeckerSchmecker::exit));


        // wait for notify on "exit"
        synchronized (exit) {
            try {
                exit.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                exit();
            }
        }

        // only reachable by notify on "exit" (currently not used)
        exit();
    }

    public static void exit() {
        System.out.println("LeckersSchmecker is shutting down. Cleaning up...");
        DatabaseManager.disconnect();
    }

    private static void initLogger() {
        logger = Logger.getLogger("RWTH-LS");
        logger.setUseParentHandlers(false);

        SimpleFormatter formatter = new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        };

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        try {
            File logFile = new File("logs/latest");
            FileUtils.createParentDirectories(logFile);
            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), Config.getLogLines(),
                    Config.getLogFiles(), true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateOffers() {
        Canteen.TYPES.forEach(Canteen::fetchDailyOffers);
        logger.info("Updated canteen offers");
    }

    public static Logger getLogger() {
        return logger;
    }

    public static class UpdateTask extends TimerTask {
        @Override
        public void run() {
            updateOffers();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dateTime;

            // fetch every 15 min between daily opening hours (excluding saturday and sunday)
            if (!now.getDayOfWeek().equals(DayOfWeek.SATURDAY)
                    && !now.getDayOfWeek().equals(DayOfWeek.SUNDAY)
                    && now.getHour() >= 10
                    && (now.getHour() < 14) || (now.getHour() == 14 && now.getMinute() <= 30)) {

                 dateTime = now.plusMinutes(15).withSecond(0);
            } else {
                dateTime = now.plusHours(4).withMinute(0).withSecond(0);
            }

            timer.schedule(new UpdateTask(), Date.from(dateTime.atZone(ZoneOffset.systemDefault()).toInstant()));
            logger.info("Scheduled update until " +
                    dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }
    }
}
