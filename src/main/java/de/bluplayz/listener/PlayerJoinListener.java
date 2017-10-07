package de.bluplayz.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.player.PlayerJoinEvent;
import de.bluplayz.BungeePE;

public class PlayerJoinListener extends SimpleListener {
    public PlayerJoinListener( BungeePE plugin ) {
        super( plugin );
    }

    @EventHandler( priority = EventPriority.LOW )
    public void onJoin( PlayerJoinEvent e ) {
        Player player = e.getPlayer();
        e.setJoinMessage( "" );
    }
}
