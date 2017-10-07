package de.bluplayz.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import de.bluplayz.BungeePE;
import de.bluplayz.api.LocaleAPI;
import de.bluplayz.data.PEServer;
import lombok.Getter;

public class ServerlistCommand extends Command {
    @Getter
    private BungeePE plugin;

    public ServerlistCommand( BungeePE plugin ) {
        super( "Servers", "", "/servers" );

        this.plugin = plugin;
    }

    @Override
    public boolean execute( CommandSender sender, String label, String[] args ) {
        if ( !sender.hasPermission( "network.command.servers" ) ) {
            LocaleAPI.sendTranslatedMessage( sender, "command_no_permissions" );
            return false;
        }

        LocaleAPI.sendTranslatedMessage( sender, "command_servers_online_servers" );

        for ( PEServer server : PEServer.servers ) {
            if ( !server.isOnline() ) {
                continue;
            }

            if ( server.getPermission().equalsIgnoreCase( "" ) ) {
                sender.sendMessage( "§7- §a" + server.getName() + "§7(§aPlayers§7: §b" + server.getPlayers().toString() + "§7)" );
            } else {
                sender.sendMessage( "§7- §a" + server.getName() + "§7(§aPermission§7: §b" + server.getPermission() + "§7, §aPlayers§7: §b" + server.getPlayers().toString() + "§7)" );
            }
        }
        return false;
    }
}
