package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class LeaveSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public LeaveSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("leave")
                .requires(src -> src.getSender().hasPermission("guild.user.leave"))
                .executes(this::handleLeave);
    }

    private int handleLeave(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().leaveGuildAsync(player);
        return Command.SINGLE_SUCCESS;
    }
}
