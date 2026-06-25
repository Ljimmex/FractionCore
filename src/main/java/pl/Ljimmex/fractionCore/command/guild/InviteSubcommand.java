package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

public class InviteSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public InviteSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("invite")
                .requires(src -> src.getSender().hasPermission("guild.user.invite"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild invite <nick>"))
                .then(Commands.literal("cancel").executes(this::handleInviteCancel))
                .then(Commands.literal("decline")
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .suggests(helper.guildTags())
                                .executes(this::handleInviteDecline)))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(this::handleInvite));
    }

    private int handleInvite(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        String nick = StringArgumentType.getString(ctx, "nick");
        Player target = Bukkit.getPlayerExact(nick);
        if (target == null || !target.isOnline()) {
            player.sendMessage(helper.langManager().getMessage("guild.error.player_not_online", MessageType.ERROR, PlaceholderContext.of(player)));
            return Command.SINGLE_SUCCESS;
        }
        helper.guildService().invitePlayerAsync(player, target);
        return Command.SINGLE_SUCCESS;
    }

    private int handleInviteCancel(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().cancelInvites(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleInviteDecline(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().declineInvite(player, StringArgumentType.getString(ctx, "tag"));
        return Command.SINGLE_SUCCESS;
    }
}
