package pl.repsmc.ranks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Locale;

public final class NickPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        // Używamy main scoreboard, bo inne pluginy rzadziej go podmieniają.
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // LuckPerms (opcjonalnie)
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        // zastosuj dla graczy online (np. po /reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyNametagOnly(p);
        }

        getLogger().info("REPSMC Ranks enabled (nametag only).");
    }

    @Override
    public void onDisable() {
        getLogger().info("REPSMC Ranks disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        applyNametagOnly(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removeFromOurTeams(e.getPlayer());
    }

    private void applyNametagOnly(Player p) {
        String group = resolveGroup(p);

        // Prefixy widoczne NAD GŁOWĄ
        // Jeśli chcesz “ramki” z resource packa -> zamiast tekstu wpisujesz glyph np. " " itp.
        String prefix = switch (group) {
            case "owner" -> "§6§lOWNER §f";
            case "coowner" -> "§e§lCO-OWNER §f";
            case "admin" -> "§c§lADMIN §f";
            case "dev" -> "§9§lDEV §f";
            case "mod" -> "§5§lMOD §f";
            case "helper" -> "§a§lHELPER §f";
            default -> "§7GRACZ §f";
        };

        // Waga do sortowania (mniejsze = wyżej)
        int weight = switch (group) {
            case "owner" -> 0;
            case "coowner" -> 1;
            case "admin" -> 2;
            case "dev" -> 3;
            case "mod" -> 4;
            case "helper" -> 5;
            default -> 50;
        };

        String teamName = teamNameFor(weight, group);

        // wyczyść gracza z naszych teamów, żeby nie siedział w złym
        removeFromOurTeams(p);

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.setPrefix(prefix);
        team.addEntry(p.getName());

        // WAŻNE: NIE ruszamy czatu
        // WAŻNE: NIE dodajemy prefixu w TAB (czyścimy tablist name)
        try {
            p.setPlayerListName(p.getName());
        } catch (Throwable ignored) {}

        // Jeśli jakiś plugin podmienia scoreboard gracza, to wymuś:
        // (możesz to zostawić – to gwarantuje widoczność nad głową)
        try {
            p.setScoreboard(scoreboard);
        } catch (Throwable ignored) {}
    }

    private void removeFromOurTeams(Player p) {
        for (Team t : scoreboard.getTeams()) {
            if (t.getName().startsWith("rp_")) {
                t.removeEntry(p.getName());
            }
        }
    }

    private String resolveGroup(Player p) {
        // 1) LuckPerms primary group
        if (luckPerms != null) {
            try {
                User u = luckPerms.getUserManager().getUser(p.getUniqueId());
                if (u != null) {
                    return u.getPrimaryGroup().toLowerCase(Locale.ROOT);
                }
            } catch (Throwable ignored) {}
        }

        // 2) fallback permissiony (jeśli nie masz LP albo nie ma usera w cache)
        if (p.hasPermission("tab.group.owner")) return "owner";
        if (p.hasPermission("tab.group.coowner")) return "coowner";
        if (p.hasPermission("tab.group.admin")) return "admin";
        if (p.hasPermission("tab.group.dev")) return "dev";
        if (p.hasPermission("tab.group.mod")) return "mod";
        if (p.hasPermission("tab.group.helper")) return "helper";

        return "gracz";
    }

    private String teamNameFor(int weight, String group) {
        // team name limit: 16
        String w = String.format("%02d", Math.max(0, Math.min(99, weight)));
        String g = group.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (g.length() > 9) g = g.substring(0, 9);
        String name = "rp_" + w + "_" + g; // np. rp_00_owner
        if (name.length() > 16) name = name.substring(0, 16);
        return name;
    }
}
