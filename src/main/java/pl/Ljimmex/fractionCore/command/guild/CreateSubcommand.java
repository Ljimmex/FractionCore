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
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;

public class CreateSubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public CreateSubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("create")
                .executes(ctx -> helper.createUsage(helper.sender(ctx)))
                .then(Commands.literal("confirm").executes(this::handleCreateConfirm))
                .then(Commands.argument("nazwa", StringArgumentType.string())
                        .executes(ctx -> helper.createUsage(helper.sender(ctx)))
                        .then(Commands.argument("tag", StringArgumentType.string())
                                .executes(this::handleCreate)));
    }

    private int handleCreate(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "nazwa");
        String tag = StringArgumentType.getString(ctx, "tag");
        helper.guildService().prepareCreationAsync(player, name, tag)
                .thenAccept(result -> Bukkit.getScheduler().runTask(helper.plugin(), () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (result == GuildCreateResult.SUCCESS) {
                        player.sendMessage(helper.guildService().buildPreview(player, name, tag));
                    } else {
                        player.sendActionBar(helper.guildService().getResultMessage(player, result, name, tag));
                    }
                }));
        return Command.SINGLE_SUCCESS;
    }

    private int handleCreateConfirm(CommandContext<CommandSourceStack> ctx) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().confirmCreationAsync(player)
                .thenAccept(result -> Bukkit.getScheduler().runTask(helper.plugin(), () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (result == GuildCreateResult.SUCCESS) {
                        player.sendMessage(helper.langManager().getMessage("guild.create.success", MessageType.SUCCESS, PlaceholderContext.of(player)));
                    } else {
                        player.sendActionBar(helper.guildService().getResultMessage(player, result, null, null));
                    }
                }));
        return Command.SINGLE_SUCCESS;
    }
}
