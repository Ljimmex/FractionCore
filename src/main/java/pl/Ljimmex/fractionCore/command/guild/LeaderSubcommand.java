package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class LeaderSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public LeaderSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("leader")
                .requires(src -> src.getSender().hasPermission("guild.user.leader"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild leader <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(this::handleLeader));
    }

    private int handleLeader(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().transferLeadershipAsync(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }
}
