package pl.Ljimmex.fractionCore.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

import java.util.List;

public class GuildCommand {

    private final JavaPlugin plugin;
    private final GuildAdminCommand adminCommand;
    private final GuildService guildService;
    private final LangManager langManager;

    public GuildCommand(JavaPlugin plugin, ModuleManager moduleManager, GuildService guildService, LangManager langManager) {
        this.plugin = plugin;
        this.adminCommand = new GuildAdminCommand(plugin, moduleManager);
        this.guildService = guildService;
        this.langManager = langManager;
    }

    public void register(Commands commands) {
        commands.register(buildNode().build(), "Main FractionCore guild command", List.of("g"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("guild")
                .requires(src -> src.getSender().hasPermission("fractioncore.command.guild"))
                .executes(ctx -> { renderHelpMain(sender(ctx)); return Command.SINGLE_SUCCESS; })
                .then(adminCommand.buildNode())
                .then(createNode())
                .then(inviteNode())
                .then(joinNode())
                .then(leaveNode())
                .then(kickNode())
                .then(promoteNode())
                .then(demoteNode())
                .then(leaderNode())
                .then(banNode())
                .then(unbanNode())
                .then(banlistNode())
                .then(infoNode())
                .then(sethomeNode())
                .then(homeNode())
                .then(descriptionNode())
                .then(flagNode())
                .then(requestsNode())
                .then(joinacceptNode())
                .then(joindeclineNode())
                .then(disbandNode())
                .then(allyNode())
                .then(allyacceptNode())
                .then(allydeclineNode())
                .then(enemyNode())
                .then(neutralNode())
                .then(relationsNode())
                .then(cuboidflagNode())
                .then(helpNode());
    }

    private LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("create")
                .executes(ctx -> createUsage(sender(ctx)))
                .then(Commands.literal("confirm").executes(this::handleCreateConfirm))
                .then(Commands.argument("nazwa", StringArgumentType.string())
                        .executes(ctx -> createUsage(sender(ctx)))
                        .then(Commands.argument("tag", StringArgumentType.string())
                                .executes(this::handleCreate)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> inviteNode() {
        return Commands.literal("invite")
                .requires(src -> src.getSender().hasPermission("guild.user.invite"))
                .executes(ctx -> usage(sender(ctx), "/guild invite <nick>"))
                .then(Commands.literal("cancel").executes(this::handleInviteCancel))
                .then(Commands.literal("decline")
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .suggests(guildTags())
                                .executes(this::handleInviteDecline)))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(this::handleInvite));
    }

    private LiteralArgumentBuilder<CommandSourceStack> joinNode() {
        return Commands.literal("join")
                .requires(src -> src.getSender().hasPermission("guild.user.join"))
                .executes(ctx -> usage(sender(ctx), "/guild join <tag-gildii>"))
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(guildTags())
                        .executes(this::handleJoin));
    }

    private LiteralArgumentBuilder<CommandSourceStack> leaveNode() {
        return Commands.literal("leave")
                .requires(src -> src.getSender().hasPermission("guild.user.leave"))
                .executes(this::handleLeave);
    }

    private LiteralArgumentBuilder<CommandSourceStack> kickNode() {
        return Commands.literal("kick")
                .requires(src -> src.getSender().hasPermission("guild.user.kick"))
                .executes(ctx -> usage(sender(ctx), "/guild kick <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(this::handleKick));
    }

    private LiteralArgumentBuilder<CommandSourceStack> promoteNode() {
        return Commands.literal("promote")
                .requires(src -> src.getSender().hasPermission("guild.user.promote"))
                .executes(ctx -> usage(sender(ctx), "/guild promote <nick> [ranga]"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(ctx -> handlePromote(ctx, null))
                        .then(Commands.argument("ranga", StringArgumentType.greedyString())
                                .suggests(rankNames())
                                .executes(ctx -> handlePromote(ctx, StringArgumentType.getString(ctx, "ranga")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> demoteNode() {
        return Commands.literal("demote")
                .requires(src -> src.getSender().hasPermission("guild.user.demote"))
                .executes(ctx -> usage(sender(ctx), "/guild demote <nick> [ranga]"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(ctx -> handleDemote(ctx, null))
                        .then(Commands.argument("ranga", StringArgumentType.greedyString())
                                .suggests(rankNames())
                                .executes(ctx -> handleDemote(ctx, StringArgumentType.getString(ctx, "ranga")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> leaderNode() {
        return Commands.literal("leader")
                .requires(src -> src.getSender().hasPermission("guild.user.leader"))
                .executes(ctx -> usage(sender(ctx), "/guild leader <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(this::handleLeader));
    }

    private LiteralArgumentBuilder<CommandSourceStack> banNode() {
        return Commands.literal("ban")
                .requires(src -> src.getSender().hasPermission("guild.user.ban"))
                .executes(ctx -> usage(sender(ctx), "/guild ban <nick> [powod]"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(ctx -> handleBan(ctx, null))
                        .then(Commands.argument("powod", StringArgumentType.greedyString())
                                .executes(ctx -> handleBan(ctx, StringArgumentType.getString(ctx, "powod")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> unbanNode() {
        return Commands.literal("unban")
                .requires(src -> src.getSender().hasPermission("guild.user.unban"))
                .executes(ctx -> usage(sender(ctx), "/guild unban <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(this::handleUnban));
    }

    private LiteralArgumentBuilder<CommandSourceStack> banlistNode() {
        return Commands.literal("banlist")
                .requires(src -> src.getSender().hasPermission("guild.user.ban"))
                .executes(this::handleBanList);
    }

    private LiteralArgumentBuilder<CommandSourceStack> infoNode() {
        return Commands.literal("info")
                .executes(ctx -> handleInfo(ctx, null))
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(guildTags())
                        .executes(ctx -> handleInfo(ctx, StringArgumentType.getString(ctx, "tag"))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> sethomeNode() {
        return Commands.literal("sethome")
                .requires(src -> src.getSender().hasPermission("guild.user.sethome"))
                .executes(this::handleSetHome);
    }

    private LiteralArgumentBuilder<CommandSourceStack> homeNode() {
        return Commands.literal("home")
                .requires(src -> src.getSender().hasPermission("guild.user.home"))
                .executes(this::handleHome);
    }

    private LiteralArgumentBuilder<CommandSourceStack> descriptionNode() {
        return Commands.literal("description")
                .requires(src -> src.getSender().hasPermission("guild.user.description"))
                .executes(ctx -> usage(sender(ctx), "/guild description <tekst>"))
                .then(Commands.argument("tekst", StringArgumentType.greedyString())
                        .executes(this::handleDescription));
    }

    private LiteralArgumentBuilder<CommandSourceStack> flagNode() {
        return Commands.literal("flag")
                .requires(src -> src.getSender().hasPermission("guild.user.flag"))
                .executes(ctx -> flagUsage(sender(ctx)))
                .then(Commands.argument("flaga", StringArgumentType.word())
                        .suggests(suggest(List.of("public", "allow-join-requests", "show-home")))
                        .executes(ctx -> flagUsage(sender(ctx)))
                        .then(Commands.argument("wartosc", StringArgumentType.word())
                                .suggests(suggest(List.of("true", "false")))
                                .executes(this::handleFlag)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> requestsNode() {
        return Commands.literal("requests")
                .requires(src -> src.getSender().hasPermission("guild.user.requests"))
                .executes(this::handleRequests);
    }

    private LiteralArgumentBuilder<CommandSourceStack> joinacceptNode() {
        return Commands.literal("joinaccept")
                .requires(src -> src.getSender().hasPermission("guild.user.joinaccept"))
                .executes(ctx -> usage(sender(ctx), "/guild joinaccept <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(this::handleJoinAccept));
    }

    private LiteralArgumentBuilder<CommandSourceStack> joindeclineNode() {
        return Commands.literal("joindecline")
                .requires(src -> src.getSender().hasPermission("guild.user.joindecline"))
                .executes(ctx -> usage(sender(ctx), "/guild joindecline <nick>"))
                .then(Commands.argument("nick", StringArgumentType.word())
                        .suggests(onlinePlayers())
                        .executes(this::handleJoinDecline));
    }

    private LiteralArgumentBuilder<CommandSourceStack> disbandNode() {
        return Commands.literal("disband")
                .requires(src -> src.getSender().hasPermission("guild.user.disband"))
                .executes(this::handleDisbandPrepare)
                .then(Commands.literal("confirm").executes(this::handleDisbandConfirm));
    }

    private LiteralArgumentBuilder<CommandSourceStack> allyNode() {
        return relationNode("ally", "guild.user.ally", "/guild ally <tag-gildii>", this::handleAlly);
    }

    private LiteralArgumentBuilder<CommandSourceStack> allyacceptNode() {
        return relationNode("allyaccept", "guild.user.allyaccept", "/guild allyaccept <tag-gildii>", this::handleAllyAccept);
    }

    private LiteralArgumentBuilder<CommandSourceStack> allydeclineNode() {
        return relationNode("allydecline", "guild.user.allydecline", "/guild allydecline <tag-gildii>", this::handleAllyDecline);
    }

    private LiteralArgumentBuilder<CommandSourceStack> enemyNode() {
        return relationNode("enemy", "guild.user.enemy", "/guild enemy <tag-gildii>", this::handleEnemy);
    }

    private LiteralArgumentBuilder<CommandSourceStack> neutralNode() {
        return relationNode("neutral", "guild.user.neutral", "/guild neutral <tag-gildii>", this::handleNeutral);
    }

    private LiteralArgumentBuilder<CommandSourceStack> relationNode(String name, String permission, String usage, RelationExecutor executor) {
        return Commands.literal(name)
                .requires(src -> src.getSender().hasPermission(permission))
                .executes(ctx -> usage(sender(ctx), usage))
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(guildTags())
                        .executes(ctx -> executor.run(ctx, StringArgumentType.getString(ctx, "tag"))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> relationsNode() {
        return Commands.literal("relations")
                .requires(src -> src.getSender().hasPermission("guild.user.relations"))
                .executes(this::handleRelations);
    }

    private LiteralArgumentBuilder<CommandSourceStack> cuboidflagNode() {
        return Commands.literal("cuboidflag")
                .requires(src -> src.getSender().hasPermission("guild.user.cuboidflag"))
                .executes(this::handleCuboidFlagList)
                .then(Commands.argument("flaga", StringArgumentType.word())
                        .suggests(suggest(List.of("BUILD", "DESTROY", "USE", "TNT", "INTERACT", "FRIENDLY_FIRE")))
                        .executes(ctx -> cuboidUsage(sender(ctx)))
                        .then(Commands.argument("wartosc", StringArgumentType.word())
                                .suggests(suggest(List.of("ALLOW", "DENY", "MEMBERS", "ALLIES", "LEADER")))
                                .executes(this::handleCuboidFlag)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> helpNode() {
        return Commands.literal("help")
                .executes(ctx -> { renderHelpMain(sender(ctx)); return Command.SINGLE_SUCCESS; })
                .then(Commands.argument("kategoria", StringArgumentType.word())
                        .suggests(helpCategories())
                        .executes(this::handleHelp));
    }

    private int handleCreate(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        String name = StringArgumentType.getString(ctx, "nazwa");
        String tag = StringArgumentType.getString(ctx, "tag");
        GuildCreateResult result = guildService.prepareCreation(player, name, tag);
        if (result == GuildCreateResult.SUCCESS) {
            player.sendMessage(guildService.buildPreview(player, name, tag));
        } else {
            player.sendActionBar(guildService.getResultMessage(player, result, name, tag));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleCreateConfirm(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        GuildCreateResult result = guildService.confirmCreation(player);
        if (result == GuildCreateResult.SUCCESS) {
            player.sendMessage(langManager.getMessage("guild.create.success", MessageType.SUCCESS, PlaceholderContext.of(player)));
        } else {
            player.sendActionBar(guildService.getResultMessage(player, result, null, null));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleInvite(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        String nick = StringArgumentType.getString(ctx, "nick");
        Player target = Bukkit.getPlayerExact(nick);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Gracz nie jest online.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        guildService.invitePlayer(player, target);
        return Command.SINGLE_SUCCESS;
    }

    private int handleInviteCancel(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.cancelInvites(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleInviteDecline(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.declineInvite(player, StringArgumentType.getString(ctx, "tag"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleJoin(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.acceptInvite(player, StringArgumentType.getString(ctx, "tag"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleLeave(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.leaveGuild(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleKick(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.kickPlayer(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handlePromote(CommandContext<CommandSourceStack> ctx, String rank) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.promotePlayer(player, StringArgumentType.getString(ctx, "nick"), rank);
        return Command.SINGLE_SUCCESS;
    }

    private int handleDemote(CommandContext<CommandSourceStack> ctx, String rank) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.demotePlayer(player, StringArgumentType.getString(ctx, "nick"), rank);
        return Command.SINGLE_SUCCESS;
    }

    private int handleLeader(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.transferLeadership(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleBan(CommandContext<CommandSourceStack> ctx, String reason) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.banPlayer(player, StringArgumentType.getString(ctx, "nick"), reason);
        return Command.SINGLE_SUCCESS;
    }

    private int handleUnban(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.unbanPlayer(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleBanList(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.sendBanList(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleInfo(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.sendGuildInfo(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleSetHome(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.setGuildHome(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleHome(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.teleportHome(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleDescription(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.setGuildDescription(player, StringArgumentType.getString(ctx, "tekst"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleFlag(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.setGuildFlag(player, StringArgumentType.getString(ctx, "flaga"), StringArgumentType.getString(ctx, "wartosc"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleRequests(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.sendRequestList(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleJoinAccept(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.acceptJoinRequest(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleJoinDecline(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.declineJoinRequest(player, StringArgumentType.getString(ctx, "nick"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleDisbandPrepare(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.prepareDisband(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleDisbandConfirm(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.confirmDisband(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAlly(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.sendAllyRequest(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAllyAccept(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.acceptAllyRequest(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAllyDecline(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.declineAllyRequest(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleEnemy(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.setEnemy(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleNeutral(CommandContext<CommandSourceStack> ctx, String tag) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.setNeutral(player, tag);
        return Command.SINGLE_SUCCESS;
    }

    private int handleRelations(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.sendRelationsList(player);
        return Command.SINGLE_SUCCESS;
    }

    private int handleCuboidFlagList(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        return guildService.sendCuboidFlagList(player) ? Command.SINGLE_SUCCESS : 0;
    }

    private int handleCuboidFlag(CommandContext<CommandSourceStack> ctx) {
        Player player = player(ctx);
        if (player == null) return 0;
        guildService.setCuboidFlag(player, StringArgumentType.getString(ctx, "flaga"), StringArgumentType.getString(ctx, "wartosc"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleHelp(CommandContext<CommandSourceStack> ctx) {
        String category = StringArgumentType.getString(ctx, "kategoria").toLowerCase();
        CommandSender sender = sender(ctx);
        switch (category) {
            case "guild", "gildia", "g" -> renderHelpGuild(sender);
            case "admin", "administrator", "a" -> {
                if (!sender.hasPermission("fractioncore.admin")) {
                    if (sender instanceof Player player) {
                        sendNoPermission(player);
                    } else {
                        sender.sendMessage(Component.text("Nie masz uprawnien.").color(NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }
                renderHelpAdmin(sender);
            }
            default -> renderHelpMain(sender);
        }
        return Command.SINGLE_SUCCESS;
    }

    private void renderHelpMain(CommandSender sender) {
        sendLine(sender, "&8&m----------------------------------------");
        sendLine(sender, "&6&lFractionCore &8» &ePomoc");
        sendLine(sender, "&7Witaj w panelu pomocy pluginu!");
        sendLine(sender, "&6Dostępne komendy:");
        sendLine(sender, "&e/guild help &8- &7Wyświetla tę pomoc");
        sendLine(sender, "&e/guild help guild &8- &7Komendy gildii");
        sendLine(sender, "&e/guild help admin &8- &7Komendy administratora");
        sendLine(sender, "&e/guild create <nazwa> <tag> &8- &7Załóż gildię");
        sendLine(sender, "&e/guild invite <nick> &8- &7Zaproś gracza");
        sendLine(sender, "&e/guild join <tag> &8- &7Dołącz do gildii");
        sendLine(sender, "&e/guild leave &8- &7Opuść gildię");
        sendLine(sender, "&e/guild info [tag] &8- &7Informacje o gildii");
        sendLine(sender, "&e/guild home &8- &7Teleport do domu gildii");
        sendLine(sender, "&6Informacje:");
        sendLine(sender, "&7Plugin: &eFractionCore");
        sendLine(sender, "&7Wersja: &e" + plugin.getPluginMeta().getVersion());
        sendLine(sender, "&7Autor: &e" + getAuthor());
        sendLine(sender, "&8&m----------------------------------------");
    }

    private void renderHelpGuild(CommandSender sender) {
        sendLine(sender, "&8&m----------------------------------------");
        sendLine(sender, "&6&lFractionCore &8» &ePomoc Gildii");
        sendLine(sender, "&7Komendy dostępne dla członków gildii.");
        sendLine(sender, "&6Zakładanie:");
        sendLine(sender, "&e/guild create <nazwa> <tag> &8- &7Rozpocznij zakładanie gildii");
        sendLine(sender, "&e/guild create confirm &8- &7Potwierdź założenie gildii");
        sendLine(sender, "&6Członkowie:");
        sendLine(sender, "&e/guild invite <nick> &8- &7Zaproś gracza");
        sendLine(sender, "&e/guild invite cancel &8- &7Anuluj wysłane zaproszenia");
        sendLine(sender, "&e/guild invite decline <tag> &8- &7Odrzuć zaproszenie");
        sendLine(sender, "&e/guild join <tag> &8- &7Dołącz do gildii");
        sendLine(sender, "&e/guild leave &8- &7Opuść gildię");
        sendLine(sender, "&e/guild kick <nick> &8- &7Wyrzuć członka");
        sendLine(sender, "&6Rangi:");
        sendLine(sender, "&e/guild promote <nick> [ranga] &8- &7Awansuj członka");
        sendLine(sender, "&e/guild demote <nick> [ranga] &8- &7Degraduj członka");
        sendLine(sender, "&e/guild leader <nick> &8- &7Przekaż przywództwo");
        sendLine(sender, "&6Zarządzanie:");
        sendLine(sender, "&e/guild sethome &8- &7Ustaw dom gildii");
        sendLine(sender, "&e/guild home &8- &7Teleportuj do domu gildii");
        sendLine(sender, "&e/guild description <tekst> &8- &7Ustaw opis gildii");
        sendLine(sender, "&e/guild flag <flaga> <true/false> &8- &7Zmień flagę gildii");
        sendLine(sender, "&e/guild ban <nick> [powód] &8- &7Zbanuj gracza");
        sendLine(sender, "&e/guild unban <nick> &8- &7Odbanuj gracza");
        sendLine(sender, "&e/guild banlist &8- &7Lista zbanowanych");
        sendLine(sender, "&e/guild requests &8- &7Lista próśb o dołączenie");
        sendLine(sender, "&e/guild joinaccept <nick> &8- &7Przyjmij prośbę");
        sendLine(sender, "&e/guild joindecline <nick> &8- &7Odrzuć prośbę");
        sendLine(sender, "&e/guild disband &8- &7Rozwiąż gildię");
        sendLine(sender, "&e/guild disband confirm &8- &7Potwierdź rozwiązanie");
        sendLine(sender, "&6Relacje:");
        sendLine(sender, "&e/guild ally <tag> &8- &7Wyślij prośbę o sojusz");
        sendLine(sender, "&e/guild allyaccept <tag> &8- &7Zaakceptuj sojusz");
        sendLine(sender, "&e/guild allydecline <tag> &8- &7Odrzuć prośbę o sojusz");
        sendLine(sender, "&e/guild enemy <tag> &8- &7Ustal wrogość");
        sendLine(sender, "&e/guild neutral <tag> &8- &7Ustal relację neutralną");
        sendLine(sender, "&e/guild relations &8- &7Lista sojuszników i wrogów");
        sendLine(sender, "&6Cuboid:");
        sendLine(sender, "&e/guild cuboidflag [flaga] [wartość] &8- &7Zarządzaj flagami cuboidu");
        sendLine(sender, "&e/guild info [tag] &8- &7Informacje o gildii");
        sendLine(sender, "&8&m----------------------------------------");
    }

    private void renderHelpAdmin(CommandSender sender) {
        sendLine(sender, "&8&m----------------------------------------");
        sendLine(sender, "&6&lFractionCore &8» &ePomoc Administratora");
        sendLine(sender, "&7Komendy zarządzania pluginem.");
        sendLine(sender, "&e/guild admin module <list|enable|disable|reload> [moduł] &8- &7Zarządzaj modułami");
        sendLine(sender, "&e/guild admin lang <reload|reset> &8- &7Przeładuj / resetuj języki");
        sendLine(sender, "&e/guild admin reload &8- &7Przeładuj konfigurację");
        sendLine(sender, "&e/guild admin debug <true|false> &8- &7Tryb debug");
        sendLine(sender, "&e/guild admin plugin reload &8- &7Przeładuj plugin (wymaga PlugMan)");
        sendLine(sender, "&8&m----------------------------------------");
    }

    private void sendLine(CommandSender sender, String text) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }

    private String getAuthor() {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        return authors.isEmpty() ? "Ljimex" : authors.get(0);
    }

    private void sendNoPermission(Player player) {
        player.sendMessage(langManager.getMessage("general.no_permission", MessageType.ERROR, PlaceholderContext.of(player)));
    }

    private Player player(CommandContext<CommandSourceStack> ctx) {
        if (sender(ctx) instanceof Player p) return p;
        sender(ctx).sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
        return null;
    }

    private static CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    private static int usage(CommandSender sender, String text) {
        sender.sendMessage(Component.text("Uzycie: " + text).color(NamedTextColor.YELLOW));
        return 0;
    }

    private static int createUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uzycie: /guild create <nazwa> <tag>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Nastepnie: /guild create confirm").color(NamedTextColor.YELLOW));
        return 0;
    }

    private static int flagUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uzycie: /guild flag <flaga> <true/false>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Dostepne flagi: public, allow-join-requests, show-home").color(NamedTextColor.YELLOW));
        return 0;
    }

    private static int cuboidUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uzycie: /guild cuboidflag <flaga> <wartosc>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Dostepne flagi: BUILD, DESTROY, USE, TNT, INTERACT, FRIENDLY_FIRE").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Dostepne wartosci: ALLOW, DENY, MEMBERS, ALLIES, LEADER").color(NamedTextColor.YELLOW));
        return 0;
    }

    private SuggestionProvider<CommandSourceStack> onlinePlayers() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(remaining)) builder.suggest(p.getName());
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> rankNames() {
        return suggest(List.of("Lider", "Co-Lider", "Moderator", "Czlonek", "Rekrut"));
    }

    private SuggestionProvider<CommandSourceStack> guildTags() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            try {
                for (var guild : guildService.getGuildDao().findAll()) {
                    String tag = guild.getTag();
                    if (tag.toLowerCase().startsWith(remaining)) builder.suggest(tag);
                }
            } catch (Exception ignored) {
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> helpCategories() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            suggestIf(builder, remaining, "guild", "gildia", "g");
            if (ctx.getSource().getSender().hasPermission("fractioncore.admin")) {
                suggestIf(builder, remaining, "admin", "administrator", "a");
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggest(List<String> values) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String value : values) {
                if (value.toLowerCase().startsWith(remaining)) builder.suggest(value);
            }
            return builder.buildFuture();
        };
    }

    private static void suggestIf(com.mojang.brigadier.suggestion.SuggestionsBuilder builder, String remaining, String... values) {
        for (String value : values) {
            if (value.startsWith(remaining)) builder.suggest(value);
        }
    }

    @FunctionalInterface
    private interface RelationExecutor {
        int run(CommandContext<CommandSourceStack> ctx, String tag);
    }
}
