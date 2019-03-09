package github.scarsz.requirelinkedforgameplay;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.core.entities.ISnowflake;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Member;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (check(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (check(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && check((Player) event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!DiscordSRV.isReady) return;
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getPlayer().getUniqueId());
        if (discordId != null) return;

        String root = event.getMessage().replace("/", "").split(" ")[0].toLowerCase();
        if (root.equalsIgnoreCase("discord")) return;
        boolean match = getConfig().getStringList("RestrictedCommands").stream().map(String::toLowerCase).anyMatch(s -> s.equals(root));
        boolean whitelist = getConfig().getBoolean("RestrictedCommandsIsWhitelist");
        if ((match && !whitelist) || (!match && whitelist)) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Message")));
            event.setCancelled(true);
        }
    }

    private boolean check(Player player) {
        if (!DiscordSRV.isReady) return false;

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Message")));
            return true;
        }

        Member member = DiscordSRV.getPlugin().getMainGuild().getMemberById(discordId);
        if (member == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Message")));
            return true;
        }

        if (getConfig().getBoolean("SubscriberRoleRequired")) {
            boolean subscribed = member.getRoles().stream().map(ISnowflake::getId).anyMatch(s -> s.equals(getConfig().getString("SubscriberRoleId")));
            if (!subscribed) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("SubscriberMessage")));
                return true;
            }
        }

        return false;
    }

}
