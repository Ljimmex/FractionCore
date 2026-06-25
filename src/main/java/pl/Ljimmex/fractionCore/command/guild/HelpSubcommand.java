package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public HelpSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("help")
                .executes(ctx -> {
                    helper.renderHelpMain(helper.sender(ctx));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("kategoria", StringArgumentType.word())
                        .suggests(helper.helpCategories())
                        .executes(this::handleHelp));
    }

    private int handleHelp(CommandContext<CommandSourceStack> ctx) {
        String category = StringArgumentType.getString(ctx, "kategoria").toLowerCase();
        CommandSender sender = helper.sender(ctx);
        switch (category) {
            case "guild", "gildia", "g" -> helper.renderHelpGuild(sender);
            case "admin", "administrator", "a" -> {
                if (!sender.hasPermission("fractioncore.admin")) {
                    if (sender instanceof Player player) {
                        helper.sendNoPermission(player);
                    } else {
                        helper.sendRaw(sender, "guild.error.no_permission");
                    }
                    return Command.SINGLE_SUCCESS;
                }
                helper.renderHelpAdmin(sender);
            }
            default -> helper.renderHelpMain(sender);
        }
        return Command.SINGLE_SUCCESS;
    }
}
