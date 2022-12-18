package telegram;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;

public enum AdminAction implements BotAction {

    TEST("Test", List.of("test")) {
        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {

        }

        @Override
        public void onUpdate(ChatContext context, Update update) {

        }
    };

    public static void processUpdate(ChatContext context, Update update) {
        if (update.hasMessage()) {

            if (context.hasCurrentAction()) {
                context.getCurrentAction().onUpdate(context, update);
            } else {
                Message msg = update.getMessage();

                Arrays.stream(AdminAction.values())
                        .filter(a -> a.getCmds()
                                .contains(msg.getText().split(" ")[0].toLowerCase()))
                        .findFirst()
                        .ifPresent(adminAction -> adminAction.init(context, null));
            }
        } else if (update.hasPoll()) {

            Poll poll = update.getPoll();

            String votedMeal = poll.getOptions().stream()
                    .max(Comparator.comparingInt(PollOption::getVoterCount))
                    .get().getText();
            int updatedMeals = LeckerSchmeckerBot.getInstance().getMealPollInfo(poll)
                    .updateMeals(votedMeal.split(":")[0]);

            LeckerSchmeckerBot.getInstance().removeMealPollInfo(poll);

            context.sendMessage("*" + votedMeal + ":* " + updatedMeals + " Gerichte aktualisiert");

            context.deletePoll(poll);
        }
    }

    private final String displayName;
    private final List<String> cmds;

    AdminAction(String displayName, List<String> cmds) {
        this.displayName = displayName;
        this.cmds = cmds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getCmds() {
        return cmds;
    }


}
