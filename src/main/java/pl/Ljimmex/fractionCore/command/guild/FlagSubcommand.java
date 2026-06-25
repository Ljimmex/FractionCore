package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public class FlagSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public FlagSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("flag")
                .requires(src -> src.getSender().hasPermission("guild.user.flag"))
                .executes(ctx -> helper.flagUsage(helper.sender(ctx)))
                .then(Commands.argument("flaga", StringArgumentType.word())
                        .suggests(CommandHelper.suggest(List.of("public", "allow-join-requests", "show-home")))
                        .executes(ctx -> helper.flagUsage(helper.sender(ctx)))
                        .then(Commands.argument("wartosc", StringArgumentType.word())
                                .suggests(CommandHelper.suggest(List.of("true", "false")))
                                .executes(this::handleFlag)));
    }

    private int handleFlag(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().setGuildFlagAsync(player, StringArgumentType.getString(ctx, "flaga"), StringArgumentType.getString(ctx, "wartosc"));
        return Command.SINGLE_SUCCESS;
    }
}
