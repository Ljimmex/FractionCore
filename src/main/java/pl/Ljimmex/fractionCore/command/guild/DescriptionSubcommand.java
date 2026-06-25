package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class DescriptionSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public DescriptionSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("description")
                .requires(src -> src.getSender().hasPermission("guild.user.description"))
                .executes(ctx -> helper.usage(helper.sender(ctx), "/guild description <tekst>"))
                .then(Commands.argument("tekst", StringArgumentType.greedyString())
                        .executes(this::handleDescription));
    }

    private int handleDescription(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().setGuildDescriptionAsync(player, StringArgumentType.getString(ctx, "tekst"));
        return Command.SINGLE_SUCCESS;
    }
}
