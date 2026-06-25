package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class DemoteSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public DemoteSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("demote")
                .requires(src -> src.getSender().hasPermission("guild.user.demote"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild demote <nick> [ranga]"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(ctx -> handleDemote(ctx, null))
                        .then(Commands.argument("ranga", StringArgumentType.greedyString())
                                .suggests(helper.rankNames())
                                .executes(ctx -> handleDemote(ctx, StringArgumentType.getString(ctx, "ranga")))));
    }

    private int handleDemote(CommandContext<CommandSourceStack> ctx, String rank) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().demotePlayerAsync(player, StringArgumentType.getString(ctx, "nick"), rank);
        return Command.SINGLE_SUCCESS;
    }
}
