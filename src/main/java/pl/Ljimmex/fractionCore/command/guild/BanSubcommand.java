package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class BanSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public BanSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("ban")
                .requires(src -> src.getSender().hasPermission("guild.user.ban"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild ban <nick> [powod]"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(ctx -> handleBan(ctx, null))
                        .then(Commands.argument("powod", StringArgumentType.greedyString())
                                .executes(ctx -> handleBan(ctx, StringArgumentType.getString(ctx, "powod")))));
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildUnbanNode() {
        return Commands.literal("unban")
                .requires(src -> src.getSender().hasPermission("guild.user.unban"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild unban <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(this::handleUnban));
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildBanlistNode() {
        return Commands.literal("banlist")
                .requires(src -> src.getSender().hasPermission("guild.user.ban"))
                .executes(this::handleBanList);
    }

    private int handleBan(CommandContext<CommandSourceStack> ctx, String reason) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().banPlayerAsync(player, StringArgumentType.getString(ctx, "nick"), reason);
        return Command.SINGLE_SUCCESS;
    }

    private int handleUnban(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().unbanPlayerAsync(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleBanList(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().sendBanListAsync(player);
        return Command.SINGLE_SUCCESS;
    }
}
