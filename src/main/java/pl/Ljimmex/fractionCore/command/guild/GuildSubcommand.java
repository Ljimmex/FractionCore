package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public interface GuildSubcommand {

    LiteralArgumentBuilder<CommandSourceStack> buildNode();

    default String getName() {
        return getClass().getSimpleName();
    }
}
