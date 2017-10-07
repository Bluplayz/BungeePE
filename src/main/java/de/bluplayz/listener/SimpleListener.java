package de.bluplayz.listener;

import cn.nukkit.event.Listener;
import de.bluplayz.BungeePE;
import lombok.Getter;

public class SimpleListener implements Listener {
    @Getter
    private BungeePE plugin;

    public SimpleListener( BungeePE plugin ) {
        this.plugin = plugin;
    }
}
