package tzer0.PayDay;

import java.util.LinkedList;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;

public class PayDayPlayerListener extends PlayerListener {
    Configuration conf;
    public PermissionHandler permissions;
    PayDay plugin;

    public PayDayPlayerListener (PayDay plugin) {
        this.plugin = plugin;
        
    }
    public void onPlayerJoin(PlayerJoinEvent event) {
        String pl = event.getPlayer().getName();
        if (!plugin.onlineLastPeriod.contains(pl)) {
            plugin.onlineLastPeriod.add(pl);
        }
    }
}
