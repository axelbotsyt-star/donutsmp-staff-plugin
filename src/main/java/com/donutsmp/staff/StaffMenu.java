package com.donutsmp.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DonutSMPStaffPlugin extends JavaPlugin implements Listener {

    private static final long VANISH_DURATION = 200000L;

    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> staffMode = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> muted = new HashSet<>();
    private final Set<UUID> godMode = new HashSet<>();
    private final Map<UUID, Location> frozenLocations = new HashMap<>();
    private final Map<UUID, String> muteReasons = new HashMap<>();
    private final Map<UUID, Integer> warnings = new HashMap<>();
    private StaffMenu staffMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.staffMenu = new StaffMenu(this);
        getServer().getPluginManager().registerEvents(this, this);

        registerCommand("vanish", this::onVanishCommand);
        registerCommand("sfmode", this::onStaffModeCommand);
        registerCommand("staffchat", this::onStaffChatCommand);
        registerCommand("freeze", this::onFreezeCommand);
        registerCommand("mute", this::onMuteCommand);
        registerCommand("heal", this::onHealCommand);
        registerCommand("feed", this::onFeedCommand);
        registerCommand("god", this::onGodCommand);
        registerCommand("tp", this::onTeleportCommand);
        registerCommand("tphere", this::onTeleportHereCommand);
        registerCommand("inspect", this::onInspectCommand);
        registerCommand("fly", this::onFlyCommand);
        registerCommand("speed", this::onSpeedCommand);
        registerCommand("kick", this::onKickCommand);
        registerCommand("ban", this::onBanCommand);
        registerCommand("warn", this::onWarnCommand);
        registerCommand("invsee", this::onInventorySeeCommand);
        registerCommand("report", this::onReportCommand);
        registerCommand("clearchat", this::onClearChatCommand);
        registerCommand("staffmenu", (sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use /staffmenu.", NamedTextColor.RED));
                return true;
            }
            if (!player.hasPermission("donutsmp.staff.menu")) {
                player.sendMessage(Component.text("You do not have permission to use /staffmenu.", NamedTextColor.RED));
                return true;
            }
            player.openInventory(staffMenu.createMainInventory());
            return true;
        });

        getLogger().info("VerdantSMP Staff enabled.");
    }

    private void registerCommand(String name, CommandExecutor executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
        }
    }

    @FunctionalInterface
    private interface CommandExecutor {
        boolean onCommand(CommandSender sender, Command command, String label, String[] args);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (vanished.remove(player.getUniqueId())) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            if (staffMode.remove(player.getUniqueId())) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            if (godMode.remove(player.getUniqueId())) {
                player.setInvulnerable(false);
            }
        }

        vanished.clear();
        frozen.clear();
        muted.clear();
        godMode.clear();
        frozenLocations.clear();
        muteReasons.clear();
        warnings.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (vanished.remove(uuid)) {
            updateVanishVisibility(event.getPlayer(), false);
        }
        staffMode.remove(uuid);
        frozen.remove(uuid);
        frozenLocations.remove(uuid);
        muted.remove(uuid);
        muteReasons.remove(uuid);
        godMode.remove(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (vanished.contains(player.getUniqueId())) {
            updateVanishVisibility(player, true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozen.contains(player.getUniqueId())) {
            return;
        }
        if (event.hasChangedPosition()) {
            Location original = frozenLocations.get(player.getUniqueId());
            if (original != null) {
                player.teleport(original);
            }
            player.sendActionBar(Component.text("You are frozen by staff.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!muted.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String reason = muteReasons.getOrDefault(player.getUniqueId(), "No reason provided");
        player.sendMessage(Component.text("You are muted. Reason: " + reason, NamedTextColor.RED));
    }

    private boolean onVanishCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players may use /vanish.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.vanish")) {
            sender.sendMessage(Component.text("You do not have permission to use /vanish.", NamedTextColor.RED));
            return true;
        }

        toggleVanish(player);
        return true;
    }

    private void toggleVanish(Player player) {
        UUID id = player.getUniqueId();
        if (vanished.contains(id)) {
            vanished.remove(id);
            updateVanishVisibility(player, false);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(Component.text("You joined the server.", NamedTextColor.GREEN));
            return;
        }

        vanished.add(id);
        updateVanishVisibility(player, true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) VANISH_DURATION, 0, false, false, false));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(Component.text("You left the server.", NamedTextColor.GRAY));
    }

    private boolean onStaffModeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /sfmode.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.staffmode")) {
            sender.sendMessage(Component.text("You do not have permission to use /sfmode.", NamedTextColor.RED));
            return true;
        }

        UUID id = player.getUniqueId();
        if (staffMode.contains(id)) {
            staffMode.remove(id);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.setAllowFlight(false);
            player.setFlying(false);
            if (vanished.contains(id)) {
                toggleVanish(player);
            }
            player.sendMessage(Component.text("Staff mode disabled.", NamedTextColor.YELLOW));
            return true;
        }

        staffMode.add(id);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, (int) VANISH_DURATION, 0, false, false, false));
        player.setAllowFlight(true);
        player.setFlying(true);
        if (!vanished.contains(id)) {
            toggleVanish(player);
        }
        player.sendMessage(Component.text("Staff mode enabled.", NamedTextColor.GOLD));
        return true;
    }

    private boolean onStaffChatCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /staffchat <message>", NamedTextColor.YELLOW));
            return true;
        }
        if (sender instanceof Player player && !player.hasPermission("donutsmp.staff.chat")) {
            sender.sendMessage(Component.text("You do not have permission to use /staffchat.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player) && !sender.hasPermission("donutsmp.staff.chat")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", args);
        String formatted = "§8[§6STAFF§8] §f" + sender.getName() + "§7: §f" + message;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("donutsmp.staff.chat")) {
                online.sendMessage(formatted);
            }
        }
        return true;
    }

    private boolean onFreezeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /freeze <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.freeze")) {
            sender.sendMessage(Component.text("You do not have permission to use /freeze.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        UUID id = target.getUniqueId();
        if (frozen.contains(id)) {
            frozen.remove(id);
            frozenLocations.remove(id);
            target.sendMessage(Component.text("You have been unfrozen.", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("You unfroze " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }

        frozen.add(id);
        frozenLocations.put(id, target.getLocation().clone());
        target.sendMessage(Component.text("You have been frozen by a staff member.", NamedTextColor.RED));
        sender.sendMessage(Component.text("You froze " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onMuteCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /mute <player> [reason]", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.mute")) {
            sender.sendMessage(Component.text("You do not have permission to use /mute.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        UUID id = target.getUniqueId();
        if (muted.contains(id)) {
            muted.remove(id);
            muteReasons.remove(id);
            target.sendMessage(Component.text("You have been unmuted.", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("You unmuted " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }

        muted.add(id);
        muteReasons.put(id, reason);
        target.sendMessage(Component.text("You have been muted. Reason: " + reason, NamedTextColor.RED));
        sender.sendMessage(Component.text("You muted " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onHealCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.heal")) {
            sender.sendMessage(Component.text("You do not have permission to use /heal.", NamedTextColor.RED));
            return true;
        }

        Player target = sender instanceof Player player ? player : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage(Component.text("Usage: /heal [player]", NamedTextColor.YELLOW));
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(Component.text("You have been healed.", NamedTextColor.GREEN));
        if (sender != target) {
            sender.sendMessage(Component.text("Healed " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onFeedCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.feed")) {
            sender.sendMessage(Component.text("You do not have permission to use /feed.", NamedTextColor.RED));
            return true;
        }

        Player target = sender instanceof Player player ? player : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage(Component.text("Usage: /feed [player]", NamedTextColor.YELLOW));
            return true;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(Component.text("You have been fed.", NamedTextColor.GREEN));
        if (sender != target) {
            sender.sendMessage(Component.text("Fed " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onGodCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.god")) {
            sender.sendMessage(Component.text("You do not have permission to use /god.", NamedTextColor.RED));
            return true;
        }

        Player target = sender instanceof Player player ? player : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage(Component.text("Usage: /god [player]", NamedTextColor.YELLOW));
            return true;
        }

        if (godMode.contains(target.getUniqueId())) {
            godMode.remove(target.getUniqueId());
            target.setInvulnerable(false);
            target.sendMessage(Component.text("God mode disabled.", NamedTextColor.YELLOW));
            if (sender != target) {
                sender.sendMessage(Component.text("God mode disabled for " + target.getName() + ".", NamedTextColor.YELLOW));
            }
            return true;
        }

        godMode.add(target.getUniqueId());
        target.setInvulnerable(true);
        target.sendMessage(Component.text("God mode enabled.", NamedTextColor.GOLD));
        if (sender != target) {
            sender.sendMessage(Component.text("God mode enabled for " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onTeleportCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /tp <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /tp.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.tp")) {
            sender.sendMessage(Component.text("You do not have permission to use /tp.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        player.teleport(target.getLocation());
        player.sendMessage(Component.text("Teleported to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean onTeleportHereCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /tphere <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /tphere.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.tp")) {
            sender.sendMessage(Component.text("You do not have permission to use /tphere.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        target.teleport(player.getLocation());
        target.sendMessage(Component.text("You were teleported to " + player.getName() + ".", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Teleported " + target.getName() + " to you.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onInspectCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /inspect <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.inspect")) {
            sender.sendMessage(Component.text("You do not have permission to use /inspect.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("=== Player Check ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Name: " + target.getName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("UUID: " + target.getUniqueId(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("GameMode: " + target.getGameMode(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Health: " + target.getHealth() + "/" + target.getMaxHealth(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Food: " + target.getFoodLevel(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Location: " + target.getLocation().getWorld().getName() + " " + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ(), NamedTextColor.YELLOW));
        return true;
    }

    private boolean onFlyCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.fly")) {
            sender.sendMessage(Component.text("You do not have permission to use /fly.", NamedTextColor.RED));
            return true;
        }

        Player target = sender instanceof Player player ? player : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage(Component.text("Usage: /fly [player]", NamedTextColor.YELLOW));
            return true;
        }

        boolean fly = !target.getAllowFlight();
        target.setAllowFlight(fly);
        target.setFlying(fly);
        target.sendMessage(Component.text("Flight toggled to " + fly + ".", NamedTextColor.GREEN));
        if (sender != target) {
            sender.sendMessage(Component.text("Flight toggled for " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onSpeedCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.speed")) {
            sender.sendMessage(Component.text("You do not have permission to use /speed.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /speed [player] [value]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = sender instanceof Player player ? player : null;
        float speed = 1.0F;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[0]);
            try {
                speed = Float.parseFloat(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Component.text("Speed must be a number.", NamedTextColor.RED));
                return true;
            }
        } else {
            try {
                speed = Float.parseFloat(args[0]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Component.text("Speed must be a number.", NamedTextColor.RED));
                return true;
            }
        }

        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        target.setWalkSpeed(speed / 10.0F);
        target.setFlySpeed(speed / 10.0F);
        sender.sendMessage(Component.text("Set speed for " + target.getName() + " to " + speed + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onKickCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /kick <player> [reason]", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.kick")) {
            sender.sendMessage(Component.text("You do not have permission to use /kick.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by staff";
        target.kick(Component.text("You were kicked. Reason: " + reason, NamedTextColor.RED));
        sender.sendMessage(Component.text("Kicked " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onBanCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /ban <player> [reason]", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.ban")) {
            sender.sendMessage(Component.text("You do not have permission to use /ban.", NamedTextColor.RED));
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Banned by staff";
        String targetName = args[0];
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, reason, null, sender.getName());
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            target.kick(Component.text("You have been banned. Reason: " + reason, NamedTextColor.RED));
        }
        sender.sendMessage(Component.text("Banned " + targetName + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onWarnCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /warn <player> <reason>", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.warn")) {
            sender.sendMessage(Component.text("You do not have permission to use /warn.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        int count = warnings.getOrDefault(target.getUniqueId(), 0) + 1;
        warnings.put(target.getUniqueId(), count);
        target.sendMessage(Component.text("You were warned. Warning #" + count + ": " + reason, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Warned " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onInventorySeeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /invsee <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /invsee.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.invsee")) {
            sender.sendMessage(Component.text("You do not have permission to use /invsee.", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, target.getName() + "'s Inventory");
        inventory.setContents(target.getInventory().getContents());
        player.openInventory(inventory);
        return true;
    }

    private boolean onReportCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /report <player> <reason>", NamedTextColor.YELLOW));
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.report")) {
            sender.sendMessage(Component.text("You do not have permission to use /report.", NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Bukkit.getLogger().info("Staff report: " + sender.getName() + " reported " + targetName + " for: " + reason);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("donutsmp.staff.chat")) {
                online.sendMessage(Component.text("[REPORT] " + sender.getName() + " reported " + targetName + " for: " + reason, NamedTextColor.RED));
            }
        }
        sender.sendMessage(Component.text("Report submitted for " + targetName + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean onClearChatCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.clearchat")) {
            sender.sendMessage(Component.text("You do not have permission to use /clearchat.", NamedTextColor.RED));
            return true;
        }

        for (int i = 0; i < 60; i++) {
            Bukkit.broadcastMessage(" ");
        }
        Bukkit.broadcast(Component.text("Chat cleared by " + sender.getName() + ".", NamedTextColor.GOLD));
        return true;
    }

    private void updateVanishVisibility(Player player, boolean hidden) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (hidden) {
                if (!other.hasPermission("donutsmp.staff.seevanish")) {
                    other.hidePlayer(this, player);
                }
            } else {
                other.showPlayer(this, player);
            }
        }
    }
}
