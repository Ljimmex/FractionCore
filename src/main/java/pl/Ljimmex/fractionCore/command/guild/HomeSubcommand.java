package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class HomeSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public HomeSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("sethome")
                .requires(src -> src.getSender().hasPermission("guild.user.sethome"))
                .executes(this::handleSetHome);
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildHomeNode() {
        return Commands.literal("home")
                .requires(src -> src.getSender().hasPermission("guild.user.home"))
                .executes(this::handleHome);
    }

    private int handleSetHome(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().setGuildHomeAsync(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleHome(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().teleportHomeAsync(player);
        return Command.SINGLE_SUCCESS;
    }
}
