package pl.Ljimmex.fractionCore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "admin", "create", "invite", "join", "leave", "kick",
            "promote", "demote", "leader", "ban", "unban", "banlist", "info",
            "sethome", "home", "description",
            "flag", "requests", "joinaccept", "joindecline", "disband",
            "ally", "allyaccept", "allydecline", "enemy", "neutral", "relations", "help"
    );

    private final GuildAdminCommand adminCommand;
    private final GuildService guildService;
    private final LangManager langManager;

    public GuildCommand(JavaPlugin plugin, ModuleManager moduleManager, GuildService guildService, LangManager langManager) {
        this.adminCommand = new GuildAdminCommand(plugin, moduleManager);
        this.guildService = guildService;
        this.langManager = langManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "admin":
                String[] adminArgs = new String[args.length - 1];
                System.arraycopy(args, 1, adminArgs, 0, adminArgs.length);
                return adminCommand.onCommand(sender, command, label, adminArgs);
            case "create":
                return handleCreate(sender, args);
            case "invite":
                return handleInvite(sender, args);
            case "join":
                return handleJoin(sender, args);
            case "leave":
                return handleLeave(sender, args);
            case "kick":
                return handleKick(sender, args);
            case "promote":
                return handlePromote(sender, args);
            case "demote":
                return handleDemote(sender, args);
            case "leader":
                return handleLeader(sender, args);
            case "ban":
                return handleBan(sender, args);
            case "unban":
                return handleUnban(sender, args);
            case "banlist":
                return handleBanList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "sethome":
                return handleSetHome(sender, args);
            case "home":
                return handleHome(sender, args);
            case "description":
                return handleDescription(sender, args);
            case "flag":
                return handleFlag(sender, args);
            case "requests":
                return handleRequests(sender, args);
            case "joinaccept":
                return handleJoinAccept(sender, args);
            case "joindecline":
                return handleJoinDecline(sender, args);
            case "disband":
                return handleDisband(sender, args);
            case "ally":
                return handleAlly(sender, args);
            case "allyaccept":
                return handleAllyAccept(sender, args);
            case "allydecline":
                return handleAllyDecline(sender, args);
            case "enemy":
                return handleEnemy(sender, args);
            case "neutral":
                return handleNeutral(sender, args);
            case "relations":
                return handleRelations(sender, args);
            case "help":
                sendUsage(sender);
                return true;
            default:
                sender.sendMessage(Component.text("Nieznana komenda. Uzyj /guild help.").color(NamedTextColor.RED));
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 2 && "confirm".equalsIgnoreCase(args[1])) {
            GuildCreateResult result = guildService.confirmCreation(player);
            if (result == GuildCreateResult.SUCCESS) {
                sender.sendMessage(langManager.getMessage("guild.create.success", MessageType.SUCCESS, PlaceholderContext.of(player)));
            } else {
                player.sendActionBar(guildService.getResultMessage(player, result, null, null));
            }
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uzycie: /guild create <nazwa> <tag>").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Nastepnie: /guild create confirm").color(NamedTextColor.YELLOW));
            return true;
        }

        List<String> parsed = parseQuotedArgs(args, 1);
        if (parsed.size() < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild create <nazwa> <tag>").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Nastepnie: /guild create confirm").color(NamedTextColor.YELLOW));
            return true;
        }

        String tag = parsed.get(parsed.size() - 1);
        String name = String.join(" ", parsed.subList(0, parsed.size() - 1));

        GuildCreateResult result = guildService.prepareCreation(player, name, tag);
        if (result == GuildCreateResult.SUCCESS) {
            Component preview = guildService.buildPreview(player, name, tag);
            sender.sendMessage(preview);
        } else {
            player.sendActionBar(guildService.getResultMessage(player, result, name, tag));
        }
        return true;
    }

    private boolean handleInvite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 2 && "cancel".equalsIgnoreCase(args[1])) {
            if (!player.hasPermission("guild.user.invite")) {
                sendNoPermission(player);
                return true;
            }
            guildService.cancelInvites(player);
            return true;
        }
        if (args.length == 3 && "decline".equalsIgnoreCase(args[1])) {
            guildService.declineInvite(player, args[2]);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild invite <nick>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.invite")) {
            sendNoPermission(player);
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Gracz nie jest online.").color(NamedTextColor.RED));
            return true;
        }
        guildService.invitePlayer(player, target);
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild join <tag-gildii>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.join")) {
            sendNoPermission(player);
            return true;
        }
        guildService.acceptInvite(player, args[1]);
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.leave")) {
            sendNoPermission(player);
            return true;
        }
        guildService.leaveGuild(player);
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild kick <nick>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.kick")) {
            sendNoPermission(player);
            return true;
        }
        guildService.kickPlayer(player, args[1]);
        return true;
    }

    private boolean handlePromote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild promote <nick> [ranga]").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.promote")) {
            sendNoPermission(player);
            return true;
        }
        String rank = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
        guildService.promotePlayer(player, args[1], rank);
        return true;
    }

    private boolean handleDemote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild demote <nick> [ranga]").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.demote")) {
            sendNoPermission(player);
            return true;
        }
        String rank = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
        guildService.demotePlayer(player, args[1], rank);
        return true;
    }

    private boolean handleLeader(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild leader <nick>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.leader")) {
            sendNoPermission(player);
            return true;
        }
        guildService.transferLeadership(player, args[1]);
        return true;
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild ban <nick> [powod]").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.ban")) {
            sendNoPermission(player);
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
        guildService.banPlayer(player, args[1], reason);
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild unban <nick>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.unban")) {
            sendNoPermission(player);
            return true;
        }
        guildService.unbanPlayer(player, args[1]);
        return true;
    }

    private boolean handleBanList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.ban")) {
            sendNoPermission(player);
            return true;
        }
        guildService.sendBanList(player);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        String tag = args.length >= 2 ? args[1] : null;
        guildService.sendGuildInfo(player, tag);
        return true;
    }

    private boolean handleSetHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.sethome")) {
            sendNoPermission(player);
            return true;
        }
        guildService.setGuildHome(player);
        return true;
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.home")) {
            sendNoPermission(player);
            return true;
        }
        guildService.teleportHome(player);
        return true;
    }

    private boolean handleDescription(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild description <tekst>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.description")) {
            sendNoPermission(player);
            return true;
        }
        String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        guildService.setGuildDescription(player, text);
        return true;
    }

    private boolean handleFlag(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Uzycie: /guild flag <flaga> <true/false>").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Dostepne flagi: public, allow-join-requests, show-home").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.flag")) {
            sendNoPermission(player);
            return true;
        }
        guildService.setGuildFlag(player, args[1], args[2]);
        return true;
    }

    private void sendNoPermission(Player player) {
        player.sendMessage(langManager.getMessage("general.no_permission", MessageType.ERROR, PlaceholderContext.of(player)));
    }

    private boolean handleDisband(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.disband")) {
            sendNoPermission(player);
            return true;
        }
        if (args.length == 2 && "confirm".equalsIgnoreCase(args[1])) {
            guildService.confirmDisband(player);
            return true;
        }
        guildService.prepareDisband(player);
        return true;
    }

    private boolean handleRequests(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.requests")) {
            sendNoPermission(player);
            return true;
        }
        guildService.sendRequestList(player);
        return true;
    }

    private boolean handleJoinAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild joinaccept <nick>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.joinaccept")) {
            sendNoPermission(player);
            return true;
        }
        guildService.acceptJoinRequest(player, args[1]);
        return true;
    }

    private boolean handleJoinDecline(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild joindecline <nick>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.joindecline")) {
            sendNoPermission(player);
            return true;
        }
        guildService.declineJoinRequest(player, args[1]);
        return true;
    }

    private boolean handleAlly(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild ally <tag-gildii>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.ally")) {
            sendNoPermission(player);
            return true;
        }
        guildService.sendAllyRequest(player, args[1]);
        return true;
    }

    private boolean handleAllyAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild allyaccept <tag-gildii>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.allyaccept")) {
            sendNoPermission(player);
            return true;
        }
        guildService.acceptAllyRequest(player, args[1]);
        return true;
    }

    private boolean handleAllyDecline(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild allydecline <tag-gildii>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.allydecline")) {
            sendNoPermission(player);
            return true;
        }
        guildService.declineAllyRequest(player, args[1]);
        return true;
    }

    private boolean handleEnemy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild enemy <tag-gildii>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.enemy")) {
            sendNoPermission(player);
            return true;
        }
        guildService.setEnemy(player, args[1]);
        return true;
    }

    private boolean handleNeutral(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild neutral <tag-gildii>").color(NamedTextColor.YELLOW));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.neutral")) {
            sendNoPermission(player);
            return true;
        }
        guildService.setNeutral(player, args[1]);
        return true;
    }

    private boolean handleRelations(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tej komendy moze uzyc tylko gracz.").color(NamedTextColor.RED));
            return true;
        }
        if (!hasSubPermission(player, "guild.user.relations")) {
            sendNoPermission(player);
            return true;
        }
        guildService.sendRelationsList(player);
        return true;
    }

    private boolean hasSubPermission(Player player, String permission) {
        // Fallback: if the permission was not registered for some reason, allow the action.
        if (Bukkit.getPluginManager().getPermission(permission) == null) {
            return true;
        }
        return player.hasPermission(permission);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== FRACTIONCORE ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild help - pomoc").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild create <nazwa> <tag> - zaloz gildie").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild invite <nick> - zapros gracza").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild invite cancel - anuluj zaproszenia").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild invite decline <tag> - odrzuc zaproszenie").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild join <tag> - dolacz do gildii").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild leave - opusc gildie").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild kick <nick> - wyrzuc czlonka").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild promote <nick> [ranga] - awansuj (o 1 range lub na podana range)").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild demote <nick> [ranga] - degraduj (o 1 range lub na podana range)").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild leader <nick> - przekaz przywodztwo").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild ban <nick> [powod] - zbanuj").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild unban <nick> - odbanuj").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild banlist - lista zbanowanych").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild info [tag] - informacje").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild sethome - ustaw dom gildii").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild home - teleportuj do domu gildii").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild description <tekst> - ustaw opis gildii").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild flag <flaga> <true/false> - ustaw flage gildii").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild disband - rozwiaz gildie (lider, wymaga potwierdzenia)").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild disband confirm - potwierdz rozwiazanie gildii").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild requests - lista prosb o dolaczenie (lider/co-lider)").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild joinaccept <nick> - przyjmij prosbe o dolaczenie").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild joindecline <nick> - odrzuc prosbe o dolaczenie").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild ally <tag> - wyslij prosbe o sojusz").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild allyaccept <tag> - zaakceptuj prosbe o sojusz").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild allydecline <tag> - odrzuc prosbe o sojusz").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild enemy <tag> - ustal wrogosc").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild neutral <tag> - ustaw relacje neutralna").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild relations - lista sojuszy i wrogow").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin - komendy administratorskie").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
            String[] adminArgs = new String[args.length - 1];
            System.arraycopy(args, 1, adminArgs, 0, adminArgs.length);
            return adminCommand.onTabComplete(sender, command, label, adminArgs);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("disband"))) {
            return filter(List.of("confirm"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> options = new ArrayList<>(onlinePlayerNames());
            options.add("cancel");
            options.add("decline");
            return filter(options, args[1]);
        }

        if (args.length == 2 && List.of("kick", "promote", "demote", "leader", "ban", "unban", "joinaccept", "joindecline").contains(args[0].toLowerCase())) {
            return filter(onlinePlayerNames(), args[1]);
        }

        if (args.length == 3 && List.of("promote", "demote").contains(args[0].toLowerCase())) {
            return filter(rankNames(), args[2]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("flag")) {
            return filter(List.of("public", "allow-join-requests", "show-home"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            return filter(List.of("true", "false"), args[2]);
        }

        if (args.length == 2 && List.of("ally", "allyaccept", "allydecline", "enemy", "neutral").contains(args[0].toLowerCase())) {
            return filter(guildTags(), args[1]);
        }

        return List.of();
    }

    private List<String> rankNames() {
        return List.of("Lider", "Co-Lider", "Moderator", "Czlonek", "Rekrut");
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> guildTags() {
        List<String> tags = new ArrayList<>();
        try {
            for (var guild : guildService.getGuildDao().findAll()) {
                tags.add(guild.getTag());
            }
        } catch (Exception e) {
            // ignore
        }
        return tags;
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lower))
                .toList();
    }

    private List<String> parseQuotedArgs(String[] args, int start) {
        List<String> result = new ArrayList<>();
        StringBuilder current = null;
        char quote = 0;

        for (int i = start; i < args.length; i++) {
            String arg = args[i];
            if (current == null) {
                char first = arg.charAt(0);
                if (first == '"' || first == '\'') {
                    quote = first;
                    if (arg.length() > 1 && arg.charAt(arg.length() - 1) == quote) {
                        result.add(arg.substring(1, arg.length() - 1));
                    } else {
                        current = new StringBuilder(arg.substring(1));
                    }
                } else {
                    result.add(arg);
                }
            } else {
                current.append(' ').append(arg);
                if (arg.charAt(arg.length() - 1) == quote) {
                    result.add(current.substring(0, current.length() - 1));
                    current = null;
                    quote = 0;
                }
            }
        }

        if (current != null) {
            result.add(quote + current.toString());
        }

        return result;
    }
}
