package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public class CuboidFlagSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public CuboidFlagSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("cuboidflag")
                .requires(src -> src.getSender().hasPermission("guild.user.cuboidflag"))
                .executes(this::handleCuboidFlagList)
                .then(Commands.argument("flaga", StringArgumentType.word())
                        .suggests(CommandHelper.suggest(List.of("BUILD", "DESTROY", "USE", "TNT", "INTERACT", "FRIENDLY_FIRE")))
                        .executes(ctx -> helper.cuboidUsage(helper.sender(ctx)))
                        .then(Commands.argument("wartosc", StringArgumentType.word())
                                .suggests(CommandHelper.suggest(List.of("ALLOW", "DENY", "MEMBERS", "ALLIES", "LEADER")))
                                .executes(this::handleCuboidFlag)));
    }

    private int handleCuboidFlagList(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        return helper.guildService().sendCuboidFlagList(player) ? Command.SINGLE_SUCCESS : 0;
    }

    private int handleCuboidFlag(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().setCuboidFlagAsync(player, StringArgumentType.getString(ctx, "flaga"), StringArgumentType.getString(ctx, "wartosc"));
        return Command.SINGLE_SUCCESS;
    }
}
