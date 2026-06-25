package pl.Ljimmex.fractionCore.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.command.guild.*;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

import java.util.List;

public class GuildCommand {

    private final JavaPlugin plugin;
    private final GuildAdminCommand adminCommand;
    private final GuildService guildService;
    private final LangManager langManager;

    public GuildCommand(JavaPlugin plugin, ModuleManager moduleManager, GuildService guildService, LangManager langManager) {
        this.plugin = plugin;
        this.adminCommand = new GuildAdminCommand(plugin, moduleManager, langManager);
        this.guildService = guildService;
        this.langManager = langManager;
    }

    public void register(io.papermc.paper.command.brigadier.Commands commands) {
        commands.register(buildNode().build(), "Main FractionCore guild command", List.of("g"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        CommandHelper helper = new CommandHelper(plugin, guildService, langManager);

        CreateSubcommand create = new CreateSubcommand(helper);
        InviteSubcommand invite = new InviteSubcommand(helper);
        JoinSubcommand join = new JoinSubcommand(helper);
        LeaveSubcommand leave = new LeaveSubcommand(helper);
        KickSubcommand kick = new KickSubcommand(helper);
        PromoteSubcommand promote = new PromoteSubcommand(helper);
        DemoteSubcommand demote = new DemoteSubcommand(helper);
        LeaderSubcommand leader = new LeaderSubcommand(helper);
        BanSubcommand ban = new BanSubcommand(helper);
        InfoSubcommand info = new InfoSubcommand(helper);
        HomeSubcommand home = new HomeSubcommand(helper);
        DescriptionSubcommand description = new DescriptionSubcommand(helper);
        FlagSubcommand flag = new FlagSubcommand(helper);
        RequestsSubcommand requests = new RequestsSubcommand(helper);
        DisbandSubcommand disband = new DisbandSubcommand(helper);
        AllySubcommand ally = new AllySubcommand(helper);
        EnemySubcommand enemy = new EnemySubcommand(helper);
        NeutralSubcommand neutral = new NeutralSubcommand(helper);
        RelationsSubcommand relations = new RelationsSubcommand(helper);
        CuboidFlagSubcommand cuboidflag = new CuboidFlagSubcommand(helper);
        HelpSubcommand help = new HelpSubcommand(helper);

        return Commands.literal("guild")
                .requires(src -> src.getSender().hasPermission("fractioncore.command.guild"))
                .executes(ctx -> {
                    helper.renderHelpMain(helper.sender(ctx));
                    return Command.SINGLE_SUCCESS;
                })
                .then(adminCommand.buildNode())
                .then(create.buildNode())
                .then(invite.buildNode())
                .then(join.buildNode())
                .then(leave.buildNode())
                .then(kick.buildNode())
                .then(promote.buildNode())
                .then(demote.buildNode())
                .then(leader.buildNode())
                .then(ban.buildNode())
                .then(ban.buildUnbanNode())
                .then(ban.buildBanlistNode())
                .then(info.buildNode())
                .then(home.buildNode())
                .then(home.buildHomeNode())
                .then(description.buildNode())
                .then(flag.buildNode())
                .then(requests.buildNode())
                .then(requests.buildJoinAcceptNode())
                .then(requests.buildJoinDeclineNode())
                .then(disband.buildNode())
                .then(ally.buildNode())
                .then(ally.buildAllyAcceptNode())
                .then(ally.buildAllyDeclineNode())
                .then(enemy.buildNode())
                .then(neutral.buildNode())
                .then(relations.buildNode())
                .then(cuboidflag.buildNode())
                .then(help.buildNode());
    }
}
