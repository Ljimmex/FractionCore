package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import java.util.List;

final class GuildCreationHelper {

    private final GuildContext context;

    GuildCreationHelper(GuildContext context) {
        this.context = context;
    }

    List<CostItem> loadCostItems() {
        return context.loadCostItemsAtPath("foundation-cost.items");
    }
}
