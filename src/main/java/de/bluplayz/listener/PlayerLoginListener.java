package de.bluplayz.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.scheduler.NukkitRunnable;
import de.bluplayz.BungeePE;
import de.bluplayz.data.PEServer;

import java.net.InetSocketAddress;

public class PlayerLoginListener extends SimpleListener {
    public PlayerLoginListener( BungeePE plugin ) {
        super( plugin );
    }

    @EventHandler( priority = EventPriority.LOW )
    public void onLogin( PlayerLoginEvent e ) {
        Player player = e.getPlayer();

        PEServer targetServer = null;

        for ( int i = 0; i < PEServer.serverJoinPriority.size(); i++ ) {
            PEServer server = PEServer.serverJoinPriority.getOrDefault( i, null );
            if ( server == null ) {
                continue;
            }

            if ( server.isOnline() && player.hasPermission( server.getPermission() ) ) {
                targetServer = server;
                break;
            }
        }

        if ( targetServer == null ) {
            e.setKickMessage( "§cNo fallback Server was found!" );
            e.setCancelled();
            return;
        }

        String host = targetServer.getHost();
        int port = targetServer.getPort();
        new NukkitRunnable() {
            @Override
            public void run() {
                player.transfer( new InetSocketAddress( host, port ) );
            }
        }.runTask( getPlugin() );
    }
}
