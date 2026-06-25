package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class JoinSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public JoinSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("join")
                .requires(src -> src.getSender().hasPermission("guild.user.join"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild join <tag-gildii>"))
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(helper.guildTags())
                        .executes(this::handleJoin));
    }

    private int handleJoin(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().acceptInviteAsync(player, StringArgumentType.getString(ctx, "tag"));
        return Command.SINGLE_SUCCESS;
    }
}
