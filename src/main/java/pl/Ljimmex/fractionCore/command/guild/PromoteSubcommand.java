package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class PromoteSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public PromoteSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("promote")
                .requires(src -> src.getSender().hasPermission("guild.user.promote"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild promote <nick> [ranga]"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(ctx -> handlePromote(ctx, null))
                        .then(Commands.argument("ranga", StringArgumentType.greedyString())
                                .suggests(helper.rankNames())
                                .executes(ctx -> handlePromote(ctx, StringArgumentType.getString(ctx, "ranga")))));
    }

    private int handlePromote(CommandContext<CommandSourceStack> ctx, String rank) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().promotePlayerAsync(player, StringArgumentType.getString(ctx, "nick"), rank);
        return Command.SINGLE_SUCCESS;
    }
}
