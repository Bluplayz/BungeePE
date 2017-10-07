package de.bluplayz.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import de.bluplayz.BungeePE;
import de.bluplayz.Callback;
import de.bluplayz.api.LocaleAPI;
import de.bluplayz.data.PEServer;
import de.bluplayz.network.packet.PlayerTransferPacket;
import de.bluplayz.networkhandler.netty.NettyHandler;
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

        PlayerTransferPacket playerTransferPacket = new PlayerTransferPacket();
        playerTransferPacket.setPlayername( targetname );
        playerTransferPacket.setHost( targetServer.getHost() );
        playerTransferPacket.setPort( targetServer.getPort() );

        getPlugin().getNettyHandler().addPacketCallback( playerTransferPacket, new Callback() {
            @Override
            public void accept( Object... objects ) {
                PlayerTransferPacket packet = (PlayerTransferPacket) objects[0];
                if ( !packet.isSuccess() ) {
                    LocaleAPI.sendTranslatedMessage( sender, "network_player_not_found", targetname );
                } else {
                    //TODO success message
                }
            }
        } );

        getPlugin().getPacketHandler().sendPacket( playerTransferPacket, NettyHandler.getClients().get( "Lobby-1" ) );
        return true;
    }
}
