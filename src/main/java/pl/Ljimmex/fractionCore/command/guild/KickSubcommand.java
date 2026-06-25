package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class KickSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public KickSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("kick")
                .requires(src -> src.getSender().hasPermission("guild.user.kick"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild kick <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(this::handleKick));
    }

    private int handleKick(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().kickPlayerAsync(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }
}
