package Classic;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ClassicMain extends Plugin {
    public File file;
    public Configuration config;
    ArrayList<ProxiedPlayer> regularQueuedPlayers = new ArrayList<>();
    ArrayList<String> regularQueueTargets = new ArrayList<>();
    ArrayList<ProxiedPlayer> donorQueuedPlayers = new ArrayList<>();
    ArrayList<String> donorQueueTargets = new ArrayList<>();
    Boolean runNow = true;
    Boolean regAlreadyClearRunning = false;
    Boolean alreadyMessageRunning = false;
    Boolean donorAlreadyClearRunning = false;
    Boolean runDonor = false;


    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new Queue(this));
        System.out.println(ChatColor.GREEN + "ClassicQueueLITE by larskei enabled");
        file = new File(getDataFolder(), "Config.yml");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                    config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                    config.set("QueueMessage", "&2You are in queue");
                    config.set("####", "%QUEUEPOS% is Case sensitive, also the placeholder ONLY works inside of this config!");
                    config.set("QueueRefreshMessage", "&dYou are %QUEUEPOS% in queue");
                    config.set("###", "How often it will send the QueueRefreshMessage to those still in queue, only in seconds, NO DECIMALS!!!");
                    config.set("MessageInterval", "20");
                    config.set("#", "Queue rate is the rate at which players can join a server in seconds, so 0.5 means a player will join every .5 seconds (2 per second) can support down too 4 decimal points, this is a network wide queue, so 2 players joining any of the servers per second at 0.5 setting");
                    config.set("QueueRate", "0.5");
                    config.set("##", "The rate that donors can join, they are two seperate queues");
                    config.set("DonorQueueRate", "1.0");
                    ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //------------------------------------------------------------------------------------------
    //method to call when the command is actually run, this handles sorting the players into the correct groups, and checking permission
    //------------------------------------------------------------------------------------------

    public void enterQueue(ProxiedPlayer player, String servertarget){
        if(player.hasPermission("classicqueue.donor")){
            donorQueuedPlayers.add(player);
            donorQueueTargets.add(servertarget);
            if (!donorAlreadyClearRunning) {
                donorQueueClean();
            }
        }
        if(!player.hasPermission("classicqueue.donor")) {
            regularQueuedPlayers.add(player);
            regularQueueTargets.add(servertarget);
            if (!regAlreadyClearRunning) {
                regQueueClean();
            }
        }
    }
    //------------------------------------------------------------------------------------------
    //This handles donor queues and the rate at which to insert them into the servers
    //------------------------------------------------------------------------------------------
    public void donorQueueClean(){
        while(donorQueuedPlayers.size() >= 1 && runDonor){
            runDonor = false;
            donorAlreadyClearRunning = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    donorQueuedPlayers.get(0).connect(getProxy().getServerInfo(regularQueueTargets.get(0)));
                    donorQueuedPlayers.remove(0);
                    donorQueueTargets.remove(0);
                    runDonor = true;
                    donorQueueClean();
                    refreshMessageQueue("donor");
                }
            };
            getProxy().getScheduler().schedule(this, runnable, getDonorRate() , TimeUnit.MILLISECONDS);

        }
        if(donorQueueTargets.size() == 0){
            donorAlreadyClearRunning = false;
        }

    }

    public int getDonorRate(){
        int rate = (int) (config.getDouble("DonorQueueRate") * 1000);
        return rate;
    }
    //------------------------------------------------------------------------------------------
    //this handles the refresh messages for donor and non donor queues
    //------------------------------------------------------------------------------------------

    public void refreshMessageQueue(String type){
        String configOriginal = config.getString("QueueRefreshMessage");
        if(regularQueuedPlayers.size() > 0 && type.equalsIgnoreCase("regular")){
            for(int i = 0; regularQueuedPlayers.size() > i; i++){
                String localPos = configOriginal;
                localPos.replaceAll("%QUEUEPOS", String.valueOf(i));
                regularQueuedPlayers.get(i).sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', localPos)));
            }
        }
        if(donorQueuedPlayers.size() > 0 && type.equalsIgnoreCase("donor")){
            for(int i = 0; donorQueuedPlayers.size() > i; i++){
                String localPos = configOriginal;
                localPos.replaceAll("%QUEUEPOS%", String.valueOf(i));
                donorQueuedPlayers.get(i).sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', localPos)));
            }
        }
    }
    //------------------------------------------------------------------------------------------
    //players without classicqueue.donor logic and queue clearing
    //------------------------------------------------------------------------------------------

    public void regQueueClean(){
        while(regularQueuedPlayers.size() >= 1 && runNow){
            regAlreadyClearRunning = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    regularQueuedPlayers.get(0).connect(getProxy().getServerInfo(regularQueueTargets.get(0)));
                    regularQueuedPlayers.remove(0);
                    regularQueueTargets.remove(0);
                    runNow = true;
                    regQueueClean();
                    refreshMessageQueue("regular");
                }
            };
            getProxy().getScheduler().schedule(this, runnable, getRegRate() , TimeUnit.MILLISECONDS);
            runNow = false;

        }
        if(regularQueuedPlayers.size() == 0){
            regAlreadyClearRunning = false;
        }

    }

    public int getRegRate(){
        int rate = (int) (config.getDouble("QueueRate") * 1000);
        return rate;
    }
    //------------------------------------------------------------------------------------------
    //Command basic logic for checks (only used in the Queue class)
    //------------------------------------------------------------------------------------------
    public boolean serverNameValid(String tester){
        if(getProxy().getServers().containsValue(tester)){
            return true;
        }
        else{
            return false;
        }

    }
    public ProxiedPlayer getProxiedPlayer(String player){
        return getProxy().getPlayer(player);
    }

    //------------------------------------------------------------------------------------------
    //Extra checks for people to use for addons (AKA an API) THIS IS FOR OTHER DEVELOPERS!!! READ HERE!!!
    //This should let you properly add/remove or check anything you want without having to really understand anything above or make changes
    //------------------------------------------------------------------------------------------

    public ArrayList getPlayersInQueue(){
        return regularQueuedPlayers;
    }
    public ArrayList getPlayersTargets(){
        return regularQueueTargets;
    }
    public ArrayList getDonorPlayersInQueue(){
        return donorQueuedPlayers;
    }
    public ArrayList getDonorPlayersTargets(){
        return donorQueueTargets;
    }
    public int getRegularPlayerQueuePos(ProxiedPlayer player){
        for(int i = 0; i <= regularQueuedPlayers.size(); i++){
            if(regularQueuedPlayers.get(i).equals(player)){
                return i;
            }
        }
        return -1;
    }
    public int getDonorPlayerQueuePos(ProxiedPlayer player){
        for(int i = 0; i <= donorQueuedPlayers.size(); i++){
            if(donorQueuedPlayers.get(i).equals(player)){
                return i;
            }
        }
        return -1;
    }
    public String getRegularPlayerTarget(ProxiedPlayer player){
        return regularQueueTargets.get(getRegularPlayerQueuePos(player));
    }
    public String getDonorPlayerTarget(ProxiedPlayer player){
        return donorQueueTargets.get(getDonorPlayerQueuePos(player));
    }
    public void addPlayerToQueue(ProxiedPlayer player, String serverTarget){
        enterQueue(player, serverTarget);
    }
    public void removePlayerFromQueue(ProxiedPlayer player){
        for(int i = 0; regularQueuedPlayers.size() >= i ; i++){
            if(regularQueuedPlayers.get(i).equals(player)){
                regularQueuedPlayers.remove(i);
                regularQueueTargets.remove(i);
            }
        }
        for(int i = 0; donorQueuedPlayers.size() >= i; i++){
            if(donorQueuedPlayers.get(i).equals(player)){
                donorQueuedPlayers.remove(i);
                donorQueueTargets.remove(i);
            }
        }
    }
    public boolean isPlayerInQueue(ProxiedPlayer player){
        for(int i = 0; regularQueuedPlayers.size() >= i ; i++){
            if(regularQueuedPlayers.get(i).equals(player)){
                return true;
            }
        }
        for(int i = 0; donorQueuedPlayers.size() >= i; i++){
            if(donorQueuedPlayers.get(i).equals(player)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {

    }
}
