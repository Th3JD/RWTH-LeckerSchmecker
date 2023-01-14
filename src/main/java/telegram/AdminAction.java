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

package telegram;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;

public abstract class AdminAction implements BotAction {

    public static final AdminAction GENERATE_ACCESS_CODE = new AdminAction("Access Code",
            List.of("/access", "access")) {
        private final Random rnd = new Random();

        @Override
        public void init(ChatContext context, SendMessage passthroughMessage) {
            String code = String.format("%05d", rnd.nextInt(100000));
            while (!context.getBot().addAccessCode(code)) {
                code = String.format("%05d", rnd.nextInt(100000));
            }
            context.sendMessage("Generated access code: " + code);
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

            String votedMealID = poll.getOptions().stream()
                    .max(Comparator.comparingInt(PollOption::getVoterCount))
                    .get().getText().split(":")[0];
            int updatedMeals = LeckerSchmeckerBot.getInstance().getMealPollInfo(poll)
                    .updateMeals(votedMealID);

            LeckerSchmeckerBot.getInstance().removeMealPollInfo(poll);

            context.sendMessage(
                    updatedMeals + " Gericht" + (updatedMeals == 1 ? " wurde" : "e wurden") +
                            " angepasst. " + (updatedMeals == 1 ? "Es gehört " : "Sie gehören ")
                            + "nun zu: " + votedMealID);

            context.deletePoll(poll);
        }
    }

    public static final AdminAction[] VALUES = {GENERATE_ACCESS_CODE};

    public static AdminAction[] values() {
        return VALUES;
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
