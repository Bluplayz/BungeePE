package de.bluplayz.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import de.bluplayz.BungeePE;
import de.bluplayz.api.LocaleAPI;
import de.bluplayz.data.PEServer;
import de.bluplayz.network.packet.PlayerTransferPacket;
import lombok.Getter;

public class ServerTransferCommand extends Command {
    @Getter
    private BungeePE plugin;

    public ServerTransferCommand( BungeePE plugin ) {
        super( "Server", "", "/server", new String[]{ "transfer" } );

        this.plugin = plugin;
    }

    @Override
    public boolean execute( CommandSender sender, String label, String[] args ) {
        if ( sender instanceof Player ) {
            return false;
        }
        if ( args.length <= 1 ) {
            LocaleAPI.sendTranslatedMessage( sender, "command_server_usage" );
            return false;
        }

        String targetname = args[0];
        String servername = args[1];

        PEServer targetServer = PEServer.getServerByName( servername );
        if ( targetServer == null ) {
            LocaleAPI.sendTranslatedMessage( sender, "network_server_not_found", servername );
            return false;
        }

        if ( !this.getPlugin().playerOnline( targetname ) ) {
            LocaleAPI.sendTranslatedMessage( sender, "network_player_not_found", targetname );
        } else {
            PlayerTransferPacket playerTransferPacket = new PlayerTransferPacket();
            playerTransferPacket.setPlayername( targetname );
            playerTransferPacket.setHost( targetServer.getHost() );
            playerTransferPacket.setPort( targetServer.getPort() );

            this.getPlugin().getPacketHandler().sendPacket( playerTransferPacket );
            LocaleAPI.sendTranslatedMessage( sender, "network_server_player_moved", this.getPlugin().getPlayername( targetname ), targetServer.getName() );
        }
        return true;
    }
}
