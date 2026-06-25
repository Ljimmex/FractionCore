package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class AllySubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public AllySubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return helper.relationNode("ally", "guild.user.ally", "/guild ally <tag-gildii>", this::handleAlly);
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildAllyAcceptNode() {
        return helper.relationNode("allyaccept", "guild.user.allyaccept", "/guild allyaccept <tag-gildii>", this::handleAllyAccept);
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildAllyDeclineNode() {
        return helper.relationNode("allydecline", "guild.user.allydecline", "/guild allydecline <tag-gildii>", this::handleAllyDecline);
    }

    private int handleAlly(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().sendAllyRequestAsync(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAllyAccept(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().acceptAllyRequestAsync(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAllyDecline(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().declineAllyRequestAsync(player, tag);
        return Command.SINGLE_SUCCESS;
    }
}
