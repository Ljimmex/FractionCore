package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class RelationsSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public RelationsSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("relations")
                .requires(src -> src.getSender().hasPermission("guild.user.relations"))
                .executes(this::handleRelations);
    }

    private int handleRelations(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().sendRelationsListAsync(player);
        return Command.SINGLE_SUCCESS;
    }
}
