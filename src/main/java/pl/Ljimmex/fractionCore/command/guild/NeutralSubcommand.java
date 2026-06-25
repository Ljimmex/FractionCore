package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class NeutralSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public NeutralSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return helper.relationNode("neutral", "guild.user.neutral", "/guild neutral <tag-gildii>", this::handleNeutral);
    }

    private int handleNeutral(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().setNeutralAsync(player, tag);
        return Command.SINGLE_SUCCESS;
    }
}
