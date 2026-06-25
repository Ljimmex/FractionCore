package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class InfoSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public InfoSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("info")
                .executes(ctx -> handleInfo(ctx, null))
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(helper.guildTags())
                        .executes(ctx -> handleInfo(ctx, StringArgumentType.getString(ctx, "tag"))));
    }

    private int handleInfo(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().sendGuildInfoAsync(player, tag);
        return Command.SINGLE_SUCCESS;
    }
}
