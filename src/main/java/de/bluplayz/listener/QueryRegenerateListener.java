package de.bluplayz.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.server.QueryRegenerateEvent;
import de.bluplayz.BungeePE;

public class QueryRegenerateListener extends SimpleListener {
    public QueryRegenerateListener( BungeePE plugin ) {
        super( plugin );
    }

    @EventHandler( priority = EventPriority.LOW )
    public void onQuery( QueryRegenerateEvent e ) {
        e.setServerName( BungeePE.MOTD );
        e.setMaxPlayerCount( BungeePE.MAXPLAYERS );
    }
}
