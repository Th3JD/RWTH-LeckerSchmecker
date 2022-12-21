package telegram;


import config.Config;
import database.DatabaseManager;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import meal.Canteen;
import meal.DailyOffer;
import meal.LeckerSchmecker;
import meal.MainMeal;
import meal.SideMeal;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import util.MultiKeyMap;

public class LeckerSchmeckerBot extends TelegramLongPollingBot {

    private final Map<Long, ChatContext> chatContextById = new HashMap<>();
    private final Map<String, Long> chatIDByPollID = new HashMap<>();

    private final MultiKeyMap<String, String, MealPollInfo> mealPollInfoByMealNameOrPollId = new MultiKeyMap<>();

    private static LeckerSchmeckerBot instance;

    public static LeckerSchmeckerBot getInstance() {
        return instance;
    }

    public LeckerSchmeckerBot() {
        instance = this;
        // Load admin into database
        chatContextById.put(Config.getAdminChatID(),
                DatabaseManager.loadUser(this, Config.getAdminChatID()));

    }

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
        Long chatId = getChatID(update);

        if (chatId == null) {
            return;
        }

        // Check if user is allowed to take part in the closed beta
        if (!Config.isAllowedUser(chatId)) {
            sendTextMessage(chatId, "Dieser Bot steht momentan noch nicht zur Verfügung!");
            LeckerSchmecker.getLogger()
                    .info("Unerlaubter Nutzer mit chatID " + chatId + " hat ein Update ausgelöst.");
            return;
        }

        // Create ChatContext if it does not exist
        ChatContext context = getContext(chatId);

        // Check if the message is from an admin
        if (Config.isAdmin(chatId)) {
            AdminAction.processUpdate(context, update);
            return;
        }

        // Reset the current action if the user wants to access the main menu
        if (update.hasMessage() && CallableAction.MAIN_MENU.getCmds()
                .contains(update.getMessage().getText().split(" ")[0].toLowerCase())) {
            context.setCurrentAction(null);
        }

        // An action is currently running -> update
        if (context.hasCurrentAction()) {
            context.getCurrentAction().onUpdate(context, update);
            // No action is currently running -> start new action
        } else if (update.hasMessage()) {
            Message msg = update.getMessage();

            // Find action requested by the user
            Optional<CallableAction> action = Arrays.stream(CallableAction.values())
                    .filter(a -> a.getCmds().contains(msg.getText().split(" ")[0].toLowerCase()))
                    .findFirst();

            if (action.isPresent()) {
                action.get().init(context, null);
            } else {
                CallableAction.MAIN_MENU.init(context, null);
            }
        } else {
            CallableAction.MAIN_MENU.init(context, null);
        }
    }

    public void sendTextMessage(Long chatId, String message) {
        if (message == null) return;
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(message);
        this.sendMessage(chatId, sendMessage);
    }

    public void sendMessage(Long chatId, SendMessage message) {
        if (message == null) {
            return;
        }
        message.setChatId(chatId);
        message.enableMarkdown(true);
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void deleteMessage(Long chatId, DeleteMessage message) {
        if (message == null) {
            return;
        }
        message.setChatId(chatId);
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public Message sendPoll(Long chatId, SendPoll poll) {
        if (poll == null) {
            return null;
        }
        poll.setChatId(chatId);
        try {
            Message msg = this.execute(poll);
            this.chatIDByPollID.put(msg.getPoll().getId(), chatId);
            return msg;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deletePoll(Long chatId, DeleteMessage message, Poll poll) {
        if (message == null) {
            return;
        }
        message.setChatId(chatId);
        this.chatIDByPollID.remove(poll.getId());
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String getMealsText(Canteen canteen, LocalDate date, ChatContext context) {

        DecimalFormat priceFormat = new DecimalFormat("0.00");
        DecimalFormat ratingFormat = new DecimalFormat("0.0");
        Optional<DailyOffer> offerOpt = canteen.getDailyOffer(date);

        if (offerOpt.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
            return canteen.getDisplayName() + " bietet am " + date.format(formatter)
                    + " keine Gerichte an!";
        }

        DailyOffer offer = offerOpt.get();

        StringBuilder sb = new StringBuilder();
        for (MainMeal meal : offer.getMainMeals()) {
            Float globalRating = DatabaseManager.getGlobalRating(meal);
            Float userRating = DatabaseManager.getUserRating(context, meal);

            sb.append("*").append(meal.getType().getDisplayName()).append("*")
                    .append(" _").append(priceFormat.format(meal.getPrice()))
                    .append("€").append("_")
                    .append("\n")
                    .append(meal.text()).append("\n")
                    .append("Globales Rating: ").append(globalRating == null ? "_Nicht bewertet_"
                            : ratingFormat.format(globalRating)).append("\n")
                    .append("Dein Rating:         ").append(userRating == null ? "_Nicht bewertet_"
                            : ratingFormat.format(userRating)).append("\n\n");
        }

        sb.append("*Hauptbeilagen*").append("\n");
        sb.append(String.join(" oder ",
                offer.getSideMeals(SideMeal.Type.MAIN).stream().map(SideMeal::getDisplayName)
                        .toList()));
        sb.append("\n\n");

        sb.append("*Nebenbeilagen*").append("\n");
        sb.append(String.join(" oder ",
                offer.getSideMeals(SideMeal.Type.SIDE).stream().map(SideMeal::getDisplayName)
                        .toList()));

        return sb.toString();
    }

    private ChatContext getContext(long chatId) {
        ChatContext context;
        if (!chatContextById.containsKey(chatId)) {
            context = DatabaseManager.loadUser(this, chatId);
            chatContextById.put(chatId, context);
        } else {
            context = chatContextById.get(chatId);
        }
        return context;
    }

    public Long getChatID(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        if (update.hasPollAnswer()) {
            return chatIDByPollID.get(update.getPollAnswer().getPollId());
        }
        if (update.hasPoll()) {
            return chatIDByPollID.get(update.getPoll().getId());
        }
        return null;
    }

    public void askAdmins(MainMeal meal, Set<Integer> similarMeals) {
        if (mealPollInfoByMealNameOrPollId.containsKey1(meal.getName())) {
            mealPollInfoByMealNameOrPollId.get1(meal.getName()).addMeal(meal);
            return;
        }

        SendPoll poll = new SendPoll();
        poll.setIsAnonymous(false);
        poll.setQuestion("Das Gericht '" + meal.getDisplayName()
                + "' ist ähnlich zu den folgenden Gerichten. Bitte wähle eine Option:");

        List<String> options = new ArrayList<>(
                similarMeals.stream().map(a -> a + ": " + DatabaseManager.getMealAliases(a).get(0))
                        .toList());
        options.add("Neues Gericht");
        poll.setOptions(options);

        Long adminId = Config.getAdminChatID();
        String pollID = chatContextById.get(adminId).sendPoll(poll);

        MealPollInfo info = new MealPollInfo();
        info.addMeal(meal);

        mealPollInfoByMealNameOrPollId.put(meal.getName(), pollID, info);
    }

    public MealPollInfo getMealPollInfo(Poll poll) {
        return this.mealPollInfoByMealNameOrPollId.get2(poll.getId());
    }

    public void removeMealPollInfo(Poll poll) {
        String mealName = this.mealPollInfoByMealNameOrPollId.get2(poll.getId()).getMealName();
        this.mealPollInfoByMealNameOrPollId.remove(mealName, poll.getId());
    }

}
