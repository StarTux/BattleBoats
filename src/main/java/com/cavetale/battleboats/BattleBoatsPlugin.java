package com.cavetale.battleboats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Creeper;
import org.bukkit.event.*;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class BattleBoatsPlugin extends JavaPlugin implements Listener {
    final Json json = new Json(this);
    Game game = new Game();
    Random random = new Random();
    
    @Override
    public void onEnable() {
        getServer().getScheduler().runTaskTimer(this, this::timer, 1, 1);
        getServer().getPluginManager().registerEvents(this, this);
        load();
    }

    @Override
    public void onDisable() {
        save();
    }

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (args.length == 0) return false;
        return onCommand((Player) sender, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    boolean onCommand(Player player, String cmd, String[] args) {
        switch (cmd) {
        case "load": {
            load();
            player.sendMessage("loaded");
            return true;
        }
        case "save": {
            save();
            player.sendMessage("saved");
            return true;
        }
        case "spawnA": {
            game.spawnA = new Position(player.getLocation());
            player.sendMessage("spawn a set");
            return true;
        }
        case "spawnB": {
            game.spawnB = new Position(player.getLocation());
            player.sendMessage("spawn b set");
            return true;
        }
        case "start": {
            game.running = true;
            player.sendMessage("game started");
            return true;
        }
        case "stop": {
            game.running = false;
            player.sendMessage("game stopped");

            return true;
        }
        case "rollTeams": {
            rollTeams();
            player.sendMessage("teams rolled");
            return true;
        }
        case "gamemode": {
            GameMode gm = GameMode.valueOf(args[0].toUpperCase());
            player.sendMessage("GameMode=" + gm);
            for (Player p : getServer().getOnlinePlayers()) {
                if (!p.isOp()) {
                    p.setGameMode(gm);
                }
            }
            return true;
        }
        case "item": {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getAmount() == 0) {
                player.sendMessage("no item!");
                return true;
            }
            game.items.add(Items.serialize(item));
            player.sendMessage("Item added: " + item.getType());
            return true;
        }
        case "drop": {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getAmount() == 0) {
                player.sendMessage("no item!");
                return true;
            }
            game.drops.add(Items.serialize(item));
            player.sendMessage("drop added: " + item.getType());
            return true;
        }
        case "itemA": {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getAmount() == 0) {
                player.sendMessage("no item!");
                return true;
            }
            game.itemsA.add(Items.serialize(item));
            player.sendMessage("Item added: " + item.getType());
            return true;
        }
        case "itemB": {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getAmount() == 0) {
                player.sendMessage("no item!");
                return true;
            }
            game.itemsB.add(Items.serialize(item));
            player.sendMessage("Item added: " + item.getType());
            return true;
        }
        case "warp": {
            Location a = game.spawnA.toLocation();
            Location b = game.spawnB.toLocation();
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.isOp()) continue;
                if (game.teamA.contains(p.getUniqueId())) {
                    p.teleport(a);
                } else if (game.teamB.contains(p.getUniqueId())) {
                    p.teleport(b);
                }
            }
            player.sendMessage("Players warped");
            return true;
        }
        case "dish": {
            List<ItemStack> zs = game.items.stream().map(Items::deserialize).collect(Collectors.toList());
            List<ItemStack> as = game.itemsA.stream().map(Items::deserialize).collect(Collectors.toList());
            List<ItemStack> bs = game.itemsB.stream().map(Items::deserialize).collect(Collectors.toList());
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.isOp()) continue;
                for (ItemStack z : zs) p.getInventory().addItem(z.clone());
                if (game.teamA.contains(p.getUniqueId())) {
                    for (ItemStack a : as) p.getInventory().addItem(a.clone());
                } else if (game.teamB.contains(p.getUniqueId())) {
                    for (ItemStack b : bs) p.getInventory().addItem(b.clone());
                }
            }
            player.sendMessage("Items dished out");
            return true;
        }
        case "clear": {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.isOp()) continue;
                p.getInventory().clear();
            }
            player.sendMessage("Player inventories cleared");
            return true;
        }
        case "slow": {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.isOp()) continue;
                p.setWalkSpeed(0.0f);
            }
            player.sendMessage("Players slowed");
            return true;
        }
        case "unslow": {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.isOp()) continue;
                p.setWalkSpeed(0.2f);
            }
            player.sendMessage("Players unslowed");
            return true;
        }
        case "colorTeams": {
            Scoreboard scoreboard = getServer().getScoreboardManager().getMainScoreboard();
            Team a = scoreboard.getTeam("a");
            if (a != null) a.unregister();
            Team b = scoreboard.getTeam("b");
            if (b != null) b.unregister();
            a = scoreboard.registerNewTeam("a");
            b = scoreboard.registerNewTeam("b");
            a.setColor(ChatColor.RED);
            b.setColor(ChatColor.BLUE);
            a.setPrefix("" + ChatColor.RED);
            b.setPrefix("" + ChatColor.BLUE);
            a.setAllowFriendlyFire(false);
            b.setAllowFriendlyFire(false);
            a.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.ALWAYS);
            b.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.ALWAYS);
            for (Player p : getServer().getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();
                if (game.teamA.contains(uuid)) {
                    a.addPlayer(p);
                } else if (game.teamB.contains(uuid)) {
                    b.addPlayer(p);
                }
            }
            player.sendMessage("Teams colored");
            return true;
        }
        case "info": {
            player.sendMessage(json.gson.toJson(game));
            return true;
        }
        case "stats": {
            List<Player> a = getServer().getOnlinePlayers().stream().filter(p -> !game.dead.contains(p.getUniqueId()) && game.teamA.contains(p.getUniqueId())).collect(Collectors.toList());
            List<Player> b = getServer().getOnlinePlayers().stream().filter(p -> !game.dead.contains(p.getUniqueId()) && game.teamB.contains(p.getUniqueId())).collect(Collectors.toList());
            for (Player p : getServer().getOnlinePlayers()) {
                p.sendMessage(ChatColor.RED + "Team Red (" + a.size() + ") " + a.stream().map(Player::getName).map(s -> ChatColor.RED + s).collect(Collectors.joining(ChatColor.WHITE + ", ")));
                p.sendMessage(ChatColor.BLUE + "Team Blue (" + b.size() + ") " + b.stream().map(Player::getName).map(s -> ChatColor.BLUE + s).collect(Collectors.joining(ChatColor.WHITE + ", ")));
            }
            return true;
        }
        default: return false;
        }
    }

    void save() {
        json.save("game.json", game, true);
    }

    void load() {
        game = json.load("game.json", Game.class, Game::new);
    }

    void timer() {
        if (!game.running) return;
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.isOp()) continue;
            if (game.teamA.contains(player.getUniqueId())) {
            } else {
            }
            if (player.isDead()) {
                game.dead.add(player.getUniqueId());
                player.setHealth(20.0);
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
            }
            UUID uuid = player.getUniqueId();
            if (game.dead.contains(uuid)) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
            } else if (game.teamA.contains(uuid)) {
                player.setGameMode(GameMode.ADVENTURE);
            } else if (game.teamB.contains(uuid)) {
                player.setGameMode(GameMode.ADVENTURE);
            } else {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
        if (game.bombCooldown > 0) {
            game.bombCooldown -= 1;
            if (game.bombCooldown == 0) {
                List<Player> players;
                switch (game.bombTeam) {
                case "a":
                    players = getServer().getOnlinePlayers().stream().filter(p -> !game.dead.contains(p.getUniqueId()) && game.teamA.contains(p.getUniqueId())).collect(Collectors.toList());
                    break;
                case "b":
                    players = getServer().getOnlinePlayers().stream().filter(p -> !game.dead.contains(p.getUniqueId()) && game.teamB.contains(p.getUniqueId())).collect(Collectors.toList());
                    break;
                default: players = new ArrayList<>();
                }
                if (!players.isEmpty()) {
                    Player target = players.get(random.nextInt(players.size()));
                    if (random.nextBoolean()) {
                        target.getWorld().spawn(target.getLocation().add(0, 16.0, 0), TNTPrimed.class, tnt -> {
                                tnt.setFuseTicks(60);
                            });
                    } else {
                        target.getWorld().spawn(target.getLocation().add(0, 2.0, 0), Creeper.class, creeper -> {
                                creeper.setPowered(true);
                            });
                    }
                    for (Player player : getServer().getOnlinePlayers()) {
                        player.sendTitle("", ChatColor.YELLOW + "Bomb on " + target.getName() + "!");
                    }
                }
            } else if ((game.bombCooldown % 40) == 0) {
                for (Player player : getServer().getOnlinePlayers()) {
                    switch (game.bombTeam) {
                    case "a":
                        player.sendTitle("",
                                         ChatColor.RED + "Bomb on RED incoming");
                        break;
                    case "b":
                        player.sendTitle("",
                                         ChatColor.BLUE + "Bomb on BLUE incoming");
                        break;
                    default: break;
                    }
                }
            }
        }
    }

    void rollTeams() {
        game.teamA.clear();
        game.teamB.clear();
        game.dead.clear();
        List<Player> players = getServer().getOnlinePlayers().stream()
            .filter(p -> !p.isOp())
            .collect(Collectors.toList());
        Collections.shuffle(players);
        int len = players.size();
        for (int i = 0; i < len / 2; i += 1) {
            game.teamA.add(players.get(i).getUniqueId());
        }
        for (int i = len / 2; i < len; i += 1) {
            game.teamB.add(players.get(i).getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;
        if (!game.running) {
            player.setGameMode(GameMode.ADVENTURE);
            return;
        }
        UUID uuid = player.getUniqueId();
        if (game.dead.contains(uuid)) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        } else if (game.teamA.contains(uuid)) {
            player.setGameMode(GameMode.ADVENTURE);
        } else if (game.teamB.contains(uuid)) {
            player.setGameMode(GameMode.ADVENTURE);
        } else {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!game.running) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemPickupItem(PlayerPickupItemEvent event) {
        if (!game.running) return;
        Player player = event.getPlayer();
        if (event.getItem().getItemStack().getType() == Material.TNT) {
            event.getItem().remove();
            event.setCancelled(true);
            game.bombCooldown = 10 * 20;
            if (game.teamA.contains(player.getUniqueId())) {
                game.bombTeam = "b";
            } else if (game.teamB.contains(player.getUniqueId())) {
                game.bombTeam = "a";
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (game.running) {
            game.dead.add(event.getEntity().getUniqueId());
        }
    }
}
