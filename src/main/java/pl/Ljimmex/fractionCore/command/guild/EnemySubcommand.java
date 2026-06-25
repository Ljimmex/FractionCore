package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

public class EnemySubcommand implements GuildSubcommand {

    private final CommandHelper helper;

    public EnemySubcommand(CommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return helper.relationNode("enemy", "guild.user.enemy", "/guild enemy <tag-gildii>", this::handleEnemy);
    }

    private int handleEnemy(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = helper.player(ctx);
        if (player == null) {
            return 0;
        }
        helper.guildService().setEnemyAsync(player, tag);
        return Command.SINGLE_SUCCESS;
    }
}
