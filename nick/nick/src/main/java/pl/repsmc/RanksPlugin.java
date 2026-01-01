package pl.repsmc;

import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class RanksPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;
    private EventSubscription<UserDataRecalculateEvent> luckPermsSubscription;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<LuckPerms> provider =
            Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("LuckPerms not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        luckPerms = provider.getProvider();
        getServer().getPluginManager().registerEvents(this, this);

        luckPermsSubscription = luckPerms.getEventBus().subscribe(
            this,
            UserDataRecalculateEvent.class,
            event -> {
                Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
                if (player == null) {
                    return;
                }
                Bukkit.getScheduler().runTask(this, () -> applyNameTag(player, event.getUser()));
            }
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNameTag(player);
        }
    }

    @Override
    public void onDisable() {
        if (luckPermsSubscription != null) {
            luckPermsSubscription.close();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateNameTag(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(getTeamName(player.getUniqueId()));
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private void updateNameTag(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            applyNameTag(player, user);
            return;
        }

        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAcceptAsync(
            loadedUser -> Bukkit.getScheduler().runTask(this, () -> applyNameTag(player, loadedUser))
        );
    }

    private void applyNameTag(Player player, User user) {
        if (user == null || !player.isOnline()) {
            return;
        }

        CachedMetaData meta = user.getCachedData().getMetaData();
        String prefix = translate(meta.getPrefix());
        String suffix = translate(meta.getSuffix());

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = getTeamName(player.getUniqueId());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.setPrefix(limit(prefix, 64));
        team.setSuffix(limit(suffix, 64));
        team.addEntry(player.getName());
    }

    private static String translate(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static String limit(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String getTeamName(UUID uuid) {
        String compact = uuid.toString().replace("-", "");
        return "nick" + compact.substring(0, 12);
    }
}
