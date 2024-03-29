/*
 * RWTH-LeckerSchmecker
 * Copyright (c) 2023 Th3JD, ekansemit, 3dde
 *
 * This file is part of RWTH-LeckerSchmecker.
 *
 * RWTH-LeckerSchmecker is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with RWTH-LeckerSchmecker.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package meal;

import config.Config;
import database.DatabaseManager;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import localization.ResourceManager;
import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.ChatContext;
import telegram.LeckerSchmeckerBot;
import util.DateUtils;

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
        timer.schedule(new UpdateOfferTask(), 0);

        LocalDateTime dateTime = nextAutomatedQueryTime();
        timer.schedule(new AutomatedQueryTask(), Date.from(dateTime.atZone(ZoneOffset.systemDefault()).toInstant()));
        logger.info("Scheduled automated queries until " +
                dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

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
        System.out.println("LeckerSchmecker is shutting down. Cleaning up...");
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

    public static class UpdateOfferTask extends TimerTask {
        @Override
        public void run() {
            try {
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

                timer.schedule(new UpdateOfferTask(), Date.from(dateTime.atZone(ZoneOffset.systemDefault()).toInstant()));
                logger.info("Scheduled update until " +
                    dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            } catch (Exception e) {
                logger.warning("Unexpected exception thrown");
                e.printStackTrace();
            }

        }
    }

    public static class AutomatedQueryTask extends TimerTask {
        @Override
        public void run() {
            try {
                for (UUID uuid : DatabaseManager.getAutomatedQueryIds(LocalTime.now()
                    .withMinute(0).withSecond(0).withNano(0))) {
                    // Send personalized meal queries
                    ChatContext context = DatabaseManager.loadUser(LeckerSchmeckerBot.getInstance(), DatabaseManager.getChatIdByUserId(uuid));

                    if (!context.hasCanteen()) {
                        SendMessage message = new SendMessage();
                        message.enableMarkdown(true);
                        message.setText(ResourceManager.getString("default_canteen_needed", context.getLocale()));
                        context.sendMessage(message);
                        continue;
                    }

                    Canteen userCanteen = context.getCanteen();
                    SendMessage message = new SendMessage();
                    message.enableMarkdown(true);
                    message.setText("*" + context.getLocalizedString("meals") + " ("
                        + userCanteen.getDisplayName() + ")*\n\n"
                        + context.getBot()
                        .getMealsText(userCanteen, LocalDate.now(), context));
                    context.sendMessage(message);

                }

                LocalDateTime dateTime = nextAutomatedQueryTime();

                timer.schedule(new AutomatedQueryTask(), Date.from(dateTime.atZone(ZoneOffset.systemDefault()).toInstant()));
                logger.info("Scheduled automated queries until " +
                    dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            } catch (Exception e) {
                logger.warning("Unexpected exception thrown");
                e.printStackTrace();
            }

        }
    }

    public static LocalDateTime nextAutomatedQueryTime() {
        LocalDateTime now = LocalDateTime.now();

        for (LocalTime time : DateUtils.TIME_OPTIONS) {
            if (!now.getDayOfWeek().equals(DayOfWeek.SATURDAY)
                    && !now.getDayOfWeek().equals(DayOfWeek.SUNDAY)
                    && time.isAfter(now.toLocalTime())) {
                return LocalDateTime.of(now.toLocalDate(), time);
            }
        }

        return LocalDateTime.of(now.toLocalDate().plusDays(now.getDayOfWeek().equals(DayOfWeek.FRIDAY) ? 3 : 1), DateUtils.TIME_OPTIONS.get(0));
    }
}
