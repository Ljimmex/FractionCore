package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class RequestsSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public RequestsSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("requests")
                .requires(src -> src.getSender().hasPermission("guild.user.requests"))
                .executes(this::handleRequests);
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildJoinAcceptNode() {
        return Commands.literal("joinaccept")
                .requires(src -> src.getSender().hasPermission("guild.user.joinaccept"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild joinaccept <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(this::handleJoinAccept));
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildJoinDeclineNode() {
        return Commands.literal("joindecline")
                .requires(src -> src.getSender().hasPermission("guild.user.joindecline"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild joindecline <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(helper.onlinePlayers())
                        .executes(this::handleJoinDecline));
    }

    private int handleRequests(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().sendRequestListAsync(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleJoinAccept(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().acceptJoinRequest(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleJoinDecline(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().declineJoinRequest(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }
}
