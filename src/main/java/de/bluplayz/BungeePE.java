package de.bluplayz;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;
import cn.nukkit.utils.ConfigSection;
import de.bluplayz.api.LocaleAPI;
import de.bluplayz.command.ServerTransferCommand;
import de.bluplayz.command.ServerlistCommand;
import de.bluplayz.data.MySQL;
import de.bluplayz.data.PEServer;
import de.bluplayz.listener.PlayerJoinListener;
import de.bluplayz.listener.PlayerLoginListener;
import de.bluplayz.listener.QueryRegenerateListener;
import de.bluplayz.locale.Locale;
import de.bluplayz.network.packet.*;
import de.bluplayz.networkhandler.netty.ConnectionListener;
import de.bluplayz.networkhandler.netty.NettyHandler;
import de.bluplayz.networkhandler.netty.PacketHandler;
import de.bluplayz.networkhandler.netty.packet.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BungeePE extends PluginBase {
    public static boolean LOG_COMMANDS = false;
    public static int PORT = 19132;
    public static int MAXPLAYERS = 0;
    public static String MOTD = "";

    @Getter
    private static BungeePE instance;

    // Key: Playername
    // Value: Current Servername
    @Getter
    private HashMap<String, PEServer> players = new HashMap<>();

    @Getter
    private LocaleManager localeManager;

    @Getter
    private Locale consoleLocale;

    @Getter
    private MySQL mySQL;

    @Getter
    private NettyHandler nettyHandler;

    @Getter
    private PacketHandler packetHandler;

    @Getter
    private ConnectionListener connectionListener;

    @Getter
    private ArrayList<Channel> verifiedChannel = new ArrayList<>();

    public BungeePE() {
        // Save instance for further use
        BungeePE.instance = this;
    }

    @Override
    public void onEnable() {
        // Check configdata
        this.initConfig();

        // Initialize locale system
        this.initLocales();

        // Loading start
        LocaleAPI.log( "console_loading_message_start", this.getName(), getDescription().getVersion() );

        // Check for update
        this.checkForUpdate();

        // Initialize DataHandler
        this.initData();

        // Register commands
        this.registerCommands();

        // Register events
        this.registerEvents();

        // Initialize netty connection
        this.initNetwork();

        // Loading finished
        LocaleAPI.log( "console_loading_message_finish", this.getName(), getDescription().getVersion() );

        // Send current Locale message
        LocaleAPI.log( "console_language_set_success" );

        // Test
        new NukkitRunnable() {
            @Override
            public void run() {
                //BungeePE.this.getServer().getLogger().info( "§bServers: " + PEServer.servers.toString() );
            }
        }.runTaskTimer( this, 0, 20 );
    }

    @Override
    public void onDisable() {
        if ( this.getMySQL() != null ) {
            //getMySQL().disconnect();
        }
    }

    /**
     * check for update
     */
    private void checkForUpdate() {
        try {
            LocaleAPI.log( "updater_check_message" );
            String version = "error";
            String updateMessage = "update message was not found";

            URL url = new URL( "https://raw.githubusercontent.com/Bluplayz/BungeePE/master/src/main/resources/plugin.yml" );
            URLConnection connection = url.openConnection();

            BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
            String line;
            while ( ( line = in.readLine() ) != null ) {
                if ( line.startsWith( "version: " ) ) {
                    version = line.substring( 9 );
                    break;
                }
            }

            in.close();

            url = new URL( "https://raw.githubusercontent.com/Bluplayz/BungeePE/master/UpdateNotes.yml" );
            connection = url.openConnection();

            in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
            while ( ( line = in.readLine() ) != null ) {
                if ( line.startsWith( version + ": " ) ) {
                    updateMessage = line.substring( version.length() + 2 );
                    break;
                }
            }

            in.close();

            if ( !version.equalsIgnoreCase( getDescription().getVersion() ) ) {
                LocaleAPI.log( "updater_new_version_available", version, updateMessage, "https://github.com/Bluplayz/BungeePE" );
            } else {
                LocaleAPI.log( "updater_already_up_to_date" );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        if ( getConfig().getBoolean( "autoupdater.activated" ) ) {
            new NukkitRunnable() {
                @Override
                public void run() {
                    checkForUpdate();
                }
            }.runTaskLater( this, getConfig().getInt( "autoupdater.checkForUpdate" ) * 20 );
        }
    }

    /**
     * init config files
     */
    private void initConfig() {
        this.getConfig().reload();

        boolean edited = false;

        // GLOBAL
        if ( !this.getConfig().exists( "maxplayers" ) ) {
            this.getConfig().set( "maxplayers", 200 );
            edited = true;
        }
        if ( !this.getConfig().exists( "log_commands" ) ) {
            this.getConfig().set( "log_commands", true );
            edited = true;
        }
        if ( !this.getConfig().exists( "motd" ) ) {
            this.getConfig().set( "motd", "&cProxy made by &bBluplayz! :)" );
            edited = true;
        }

        // LANGUAGE
        if ( !this.getConfig().exists( "language.console" ) ) {
            this.getConfig().set( "language.console", "de_DE" );
            edited = true;
        }
        if ( !this.getConfig().exists( "language.fallback" ) ) {
            this.getConfig().set( "language.fallback", "en_EN" );
            edited = true;
        }

        // UPDATER
        if ( !this.getConfig().exists( "autoupdater" ) ) {
            this.getConfig().set( "autoupdater.activated", true );
            this.getConfig().set( "autoupdater.checkForUpdate", 30 * 60 );
            edited = true;
        }

        // DATA
        if ( !this.getConfig().exists( "data.mysql" ) ) {
            this.getConfig().set( "data.mysql.host", "localhost" );
            this.getConfig().set( "data.mysql.port", 3306 );
            this.getConfig().set( "data.mysql.username", "root" );
            this.getConfig().set( "data.mysql.database", "bungeepe" );
            this.getConfig().set( "data.mysql.password", "" );
            edited = true;
        }

        // SERVER
        if ( !this.getConfig().exists( "server" ) ) {
            this.getConfig().set( "server.options.priorities", new String[]{ "SilentLobby-1", "Lobby-1" } );
            this.getConfig().set( "server.Lobby-1.address", "localhost:19133" );
            this.getConfig().set( "server.Lobby-1.permission", "" );
            this.getConfig().set( "server.SilentLobby-1.address", "localhost:19140" );
            this.getConfig().set( "server.SilentLobby-1.permission", "bungeepe.silentlobby" );
            edited = true;
        }
        if ( !this.getConfig().exists( "server.options.priorities" ) ) {
            this.getConfig().set( "server.options.priorities", new String[]{ "SilentLobby-1", "Lobby-1" } );
            edited = true;
        }

        if ( edited ) {
            this.getConfig().save();
            this.getConfig().reload();
        }

        // Init some data from config
        BungeePE.PORT = this.getServer().getPort();
        BungeePE.MAXPLAYERS = this.getConfig().getInt( "maxplayers" );
        BungeePE.MOTD = this.getConfig().getString( "motd" ).replaceAll( "&", "§" );
        BungeePE.LOG_COMMANDS = this.getConfig().getBoolean( "log_commands" );

        getServer().getNetwork().setName( BungeePE.MOTD );
        getServer().getQueryInformation().setMaxPlayerCount( BungeePE.MAXPLAYERS );
    }

    /**
     * register commands
     */
    private void registerCommands() {
        getServer().getCommandMap().register( "blu", new ServerlistCommand( this ) );
        getServer().getCommandMap().register( "blu", new ServerTransferCommand( this ) );
    }

    /**
     * register events
     */
    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents( new PlayerLoginListener( this ), this );
        this.getServer().getPluginManager().registerEvents( new PlayerJoinListener( this ), this );
        this.getServer().getPluginManager().registerEvents( new QueryRegenerateListener( this ), this );
    }

    /**
     * init datahandler and default data
     */
    private void initData() {
        this.mySQL = new MySQL();
    }

    /**
     * init locales with default translations
     */
    private void initLocales() {
        HashMap<String, String> translations = new HashMap<>();

        // Initialize LocaleManager
        this.localeManager = new LocaleManager( getDataFolder() + "/locales" );

        /** GERMAN */
        Locale germanLocale = getLocaleManager().createLocale( "de_DE" );

        translations.clear();
        translations.put( "prefix", "§7[§3BungeePE§7]§r" );
        translations.put( "updater_check_message", "{PREFIX} §aSuche nach Updates..." );
        translations.put( "updater_already_up_to_date", "{PREFIX} §aDu hast bereits die neuste Version!" );
        translations.put( "updater_new_version_available", "{PREFIX}\n" +
                "{PREFIX} §aEine neue Version ist verfuegbar! \n" +
                "{PREFIX} §aVersion§7: §b{0} \n" +
                "{PREFIX} §aUpdates§7: §b{1} \n" +
                "{PREFIX} \n" +
                "{PREFIX} §aDen Downloadlink gibt es hier: §b{2}" +
                "\n{PREFIX}" );
        translations.put( "console_loading_message_start", "{PREFIX} §a{0} v{1} wird geladen..." );
        translations.put( "console_loading_message_finish", "{PREFIX} §a{0} v{1} wurde erfolgreich geladen!" );
        translations.put( "console_language_set_success", "{PREFIX} §7Die Sprache der Konsole ist §bDeutsch§7." );
        translations.put( "data_mysql_driver_not_found", "{PREFIX} §cDie MySQL Treiber wurden nicht gefunden!" );
        translations.put( "data_mysql_connected_successfully", "{PREFIX} §aDie Verbindung zu der MySQL Datenbank wurde hergestellt." );
        translations.put( "data_netty_start_connecting", "{PREFIX} §aStarte Netty Server..." );
        translations.put( "data_netty_connected_successfully", "{PREFIX} §aEs wurde erfolgreich ein Netty Server auf Port {0} erstellt." );
        translations.put( "data_peserver_connected", "{PREFIX} §a{0} hat sich verbunden." );
        translations.put( "data_peserver_disconnected", "{PREFIX} §a{0} hat die Verbindung getrennt." );
        translations.put( "network_server_not_found", "{PREFIX} §cDer Server {0} wurde nicht gefunden!" );
        translations.put( "network_player_not_found", "{PREFIX} §cDer Spieler {0} wurde nicht gefunden!" );
        translations.put( "network_player_switch_server", "{PREFIX} §aDer Spieler §b{0} §aging von Server §b{1} §azu Server §b{2}" );
        translations.put( "network_player_connect", "{PREFIX} §aDer Spieler §b{0} §ahat den Server §b{1} §abetreten" );
        translations.put( "network_player_disconnect", "{PREFIX} §aDer Spieler §b{0} §ahat den Server §b{1} §averlassen" );
        translations.put( "command_no_permissions", "{PREFIX} §cDu hast keine Berechtigung diesen Command auszuführen!" );
        translations.put( "command_servers_online_servers", "{PREFIX} §3Online Server:" );
        translations.put( "command_server_usage", "{PREFIX} §eBenutzung: /server <spieler> <servername>" );

        germanLocale.addTranslations( translations, false );
        /** GERMAN */

        /** ENGLISH */
        Locale englishLocale = getLocaleManager().createLocale( "en_EN" );

        translations.clear();
        translations.put( "prefix", "§7[§3BungeePE§7]§r" );
        translations.put( "updater_check_message", "{PREFIX} §aChecking for update..." );
        translations.put( "updater_already_up_to_date", "{PREFIX} §aYou already have the newest Version!" );
        translations.put( "updater_new_version_available", "{PREFIX}\n" +
                "{PREFIX} §aA new Version is Available! \n" +
                "{PREFIX} §aVersion§7: §b{0} \n" +
                "{PREFIX} §aUpdates§7: §b{1} \n" +
                "{PREFIX} \n" +
                "{PREFIX} §aYou can download it here: §b{2}" +
                "\n{PREFIX}" );
        translations.put( "console_loading_message_start", "{PREFIX} §aLoading {0} v{1}..." );
        translations.put( "console_loading_message_finish", "{PREFIX} §aSuccessfully loaded {0} v{1}!" );
        translations.put( "console_language_set_success", "{PREFIX} §7The Language of the Console is §bEnglish§7." );
        translations.put( "data_mysql_driver_not_found", "{PREFIX} §cDriver for MySQL was not found!" );
        translations.put( "data_mysql_connected_successfully", "{PREFIX} §aSuccessfully connected to the MySQL Database." );
        translations.put( "data_netty_start_connecting", "{PREFIX} §aStarting Netty Connection..." );
        translations.put( "data_netty_connected_successfully", "{PREFIX} §aSuccessfully started Netty Connection on Port {0}" );
        translations.put( "data_peserver_connected", "{PREFIX} §a{0} connected." );
        translations.put( "data_peserver_disconnected", "{PREFIX} §a{0} disconnected." );
        translations.put( "network_server_not_found", "{PREFIX} §cThe Server {0} was not found!" );
        translations.put( "network_player_not_found", "{PREFIX} §cThe Player {0} was not found!" );
        translations.put( "network_player_switch_server", "{PREFIX} §aThe Player §b{0} §amoved from Server §b{1} §ato Server §b{2}" );
        translations.put( "network_player_connect", "{PREFIX} §aThe Player §b{0} §ahas entered the Server §b{1}" );
        translations.put( "network_player_disconnect", "{PREFIX} §aThe Player §b{0} §ahas leaved the Server §b{1}" );
        translations.put( "command_no_permissions", "{PREFIX} §cYou don't have the permission to perform this command!" );
        translations.put( "command_servers_online_servers", "{PREFIX} §3Online Servers:" );
        translations.put( "command_server_usage", "{PREFIX} §eUsage: /server <player> <servername>" );

        englishLocale.addTranslations( translations, false );
        /** ENGLISH */

        // Set Console locale
        this.consoleLocale = getLocaleManager().getLocale( getConfig().getString( "language.console" ) );

        // Set default locale
        this.getLocaleManager().setDefaultLocale( getLocaleManager().getLocale( getConfig().getString( "language.fallback" ) ) );
    }

    private void initNetwork() {
        this.nettyHandler = new NettyHandler();
        this.nettyHandler.startServer( PORT, new Callback() {
            @Override
            public void accept( Object... args ) {
                // Loading message finish
                LocaleAPI.log( "data_netty_connected_successfully", PORT );
            }
        } );

        // Loading message start
        LocaleAPI.log( "data_netty_start_connecting" );

        this.getNettyHandler().registerPacketHandler( this.packetHandler = new PacketHandler() {
            @Override
            public void incomingPacket( Packet packet, Channel channel ) {
                if ( packet instanceof VerifyPacket ) {
                    if ( getVerifiedChannel().contains( channel ) ) {
                        return;
                    }

                    VerifyPacket verifyPacket = (VerifyPacket) packet;
                    if ( verifyPacket.getHost().equalsIgnoreCase( "0.0.0.0" ) ) {
                        verifyPacket.setHost( "localhost" );
                    }

                    PEServer peServer = PEServer.getServerByName( verifyPacket.getServername() );
                    if ( peServer == null ) {
                        //getServer().getLogger().info( "Server with the name " + verifyPacket.getServername() + " was not found!" );
                        //getServer().getLogger().info( "Available Servers: " + PEServer.servers.toString() );

                        verifyPacket.setSuccess( false );
                        sendPacket( packet, channel );
                        return;
                    }


                    InetAddress address1;
                    InetAddress address2;
                    try {
                        address1 = InetAddress.getByName( verifyPacket.getHost() );
                        address2 = InetAddress.getByName( peServer.getHost() );

                        if ( address1 != address2 ) {
                            //getServer().getLogger().info( "Server with the host address " + address1 + " doesnt match with " + address2 + "!" );

                            verifyPacket.setSuccess( false );
                            sendPacket( packet, channel );
                            return;
                        }
                    } catch ( UnknownHostException e ) {
                        e.printStackTrace();
                        return;
                    }

                    if ( peServer.getPort() != verifyPacket.getPort() ) {
                        //getServer().getLogger().info( "Server with the port " + verifyPacket.getPort() + " doesnt match with " + peServer.getPort() + "!" );

                        verifyPacket.setSuccess( false );
                        sendPacket( packet, channel );
                        return;
                    }

                    if ( !peServer.isOnline() ) {
                        peServer.setOnline( true );
                    }

                    getVerifiedChannel().add( channel );
                    LocaleAPI.log( "data_peserver_connected", verifyPacket.getServername() );

                    verifyPacket.setSuccess( true );
                    sendPacket( packet, channel );
                }

                if ( !getVerifiedChannel().contains( channel ) ) {
                    return;
                }

                if ( packet instanceof ServerDataRequestPacket ) {
                    ServerDataRequestPacket serverDataRequestPacket = (ServerDataRequestPacket) packet;
                    serverDataRequestPacket.getServers().clear();
                    serverDataRequestPacket.getServers().addAll( PEServer.servers );

                    sendPacket( serverDataRequestPacket, channel );
                    return;
                }

                if ( packet instanceof PlayerCommandEnteredPacket ) {
                    PlayerCommandEnteredPacket playerCommandEnteredPacket = (PlayerCommandEnteredPacket) packet;
                    if ( !LOG_COMMANDS ) {
                        return;
                    }

                    getServer().getLogger().info( "§7[§3" + playerCommandEnteredPacket.getServername() + "§7] §a" + playerCommandEnteredPacket.getPlayername() + " performed Command §7'§b" + playerCommandEnteredPacket.getCommandMessage() + "§7'" );
                }

                if ( packet instanceof PEServerDataPacket ) {
                    PEServerDataPacket peServerDataPacket = (PEServerDataPacket) packet;

                    PEServer peServer = PEServer.getServerByName( peServerDataPacket.getServername() );
                    if ( peServer == null ) {
                        return;
                    }

                    // Nothing yet
                }

                if ( packet instanceof PlayerConnectPacket ) {
                    PlayerConnectPacket playerConnectPacket = (PlayerConnectPacket) packet;

                    PEServer peServer = PEServer.getServerByName( playerConnectPacket.getServername() );
                    if ( peServer == null ) {
                        return;
                    }

                    PEServer oldServer = null;

                    for ( PEServer server : PEServer.servers ) {
                        if ( server.getPlayers().contains( playerConnectPacket.getPlayername() ) ) {
                            server.getPlayers().remove( playerConnectPacket.getPlayername() );
                            oldServer = server;
                        }
                    }

                    if ( !peServer.getPlayers().contains( playerConnectPacket.getPlayername() ) ) {
                        peServer.getPlayers().add( playerConnectPacket.getPlayername() );

                        if ( oldServer == null ) {
                            LocaleAPI.log( "network_player_connect", playerConnectPacket.getPlayername(), peServer.getName() );
                        } else {
                            LocaleAPI.log( "network_player_switch_server", playerConnectPacket.getPlayername(), oldServer.getName(), peServer.getName() );
                        }
                    }
                }

                if ( packet instanceof PlayerDisconnectPacket ) {
                    PlayerDisconnectPacket playerDisconnectPacket = (PlayerDisconnectPacket) packet;

                    PEServer peServer = PEServer.getServerByName( playerDisconnectPacket.getServername() );
                    if ( peServer == null ) {
                        return;
                    }

                    if ( peServer.getPlayers().contains( playerDisconnectPacket.getPlayername() ) ) {
                        peServer.getPlayers().remove( playerDisconnectPacket.getPlayername() );
                        LocaleAPI.log( "network_player_disconnect", playerDisconnectPacket.getPlayername(), peServer.getName() );
                    }
                }
            }

            @Override
            public void registerPackets() {
                registerPacket( VerifyPacket.class );
                registerPacket( PEServerDataPacket.class );
                registerPacket( PlayerCommandEnteredPacket.class );
                registerPacket( ServerDataRequestPacket.class );
                registerPacket( PlayerTransferPacket.class );
                registerPacket( PlayerConnectPacket.class );
                registerPacket( PlayerDisconnectPacket.class );
            }
        } );

        getNettyHandler().registerConnectionListener( this.connectionListener = new ConnectionListener() {
            @Override
            public void channelConnected( ChannelHandlerContext ctx ) {
            }

            @Override
            public void channelDisconnected( ChannelHandlerContext ctx ) {
                if ( getVerifiedChannel().contains( ctx.channel() ) ) {
                    LocaleAPI.log( "data_peserver_disconnected", getNettyHandler().getClientnameByChannel( ctx.channel() ) );

                    PEServer peServer = PEServer.getServerByName( getNettyHandler().getClientnameByChannel( ctx.channel() ) );
                    if ( peServer != null ) {
                        peServer.setOnline( false );
                    }

                    getVerifiedChannel().remove( ctx.channel() );
                }
            }
        } );

        // Clear all Servers
        PEServer.servers = new ArrayList<>();

        List<String> priorities = new ArrayList<>();

        //this.getServer().getLogger().info( "Loaded Servers:" );
        ConfigSection section = this.getConfig().getSection( "server" );
        for ( Map.Entry servers : section.getAllMap().entrySet() ) {
            String servername = (String) servers.getKey();
            ConfigSection data = (ConfigSection) servers.getValue();

            PEServer.serverJoinPriority = new HashMap<>();
            if ( servername.equalsIgnoreCase( "options" ) ) {
                priorities = data.getStringList( "priorities" );
                continue;
            }

            String address = data.getString( "address" );
            String host = address.split( ":" )[0];
            int port = Integer.valueOf( address.split( ":" )[1] );
            String permission = data.getString( "permission" );

            //this.getServer().getLogger().info( "- " + servername + "(Address: " + address + ", permission: " + ( permission.equalsIgnoreCase( "" ) ? "-" : permission ) + ")" );

            PEServer server = new PEServer();
            server.setHost( host );
            server.setPort( port );
            server.setPermission( permission );
            server.setName( servername );
            server.setOnline( false );

            PEServer.servers.add( server );
        }

        // Initialize Priorities
        int i = 0;
        for ( String servername2 : priorities ) {
            PEServer.serverJoinPriority.put( i, PEServer.getServerByName( servername2 ) );
            i++;
        }
    }
}
