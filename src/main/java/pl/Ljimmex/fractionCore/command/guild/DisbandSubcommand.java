package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class DisbandSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public DisbandSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("disband")
                .requires(src -> src.getSender().hasPermission("guild.user.disband"))
                .executes(this::handleDisbandPrepare)
                .then(Commands.literal("confirm").executes(this::handleDisbandConfirm));
    }

    private int handleDisbandPrepare(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().prepareDisbandAsync(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleDisbandConfirm(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().confirmDisbandAsync(player);
        return Command.SINGLE_SUCCESS;
    }
}
