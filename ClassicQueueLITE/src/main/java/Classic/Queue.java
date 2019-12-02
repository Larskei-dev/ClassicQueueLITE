package Classic;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Queue extends Command {
    private ClassicMain main;

    public Queue(ClassicMain main){
        super("join");
        this.main = main;
    }

    @Override
    public void execute(CommandSender sender, String[] args){
        if(args.length == 0){
            sender.sendMessage(new TextComponent(ChatColor.DARK_RED + "You must enter a server name"));
        }
        if(args.length > 0 && args.length < 2){
            if(main.serverNameValid(args[0])){
                if(sender instanceof ProxiedPlayer) {
                    ProxiedPlayer player = main.getProxiedPlayer(sender.getName());
                    main.enterQueue(player, args[0]);
                }else {
                    sender.sendMessage(new TextComponent(ChatColor.DARK_RED + "You must be a player to do this!"));
                }
            } else{
                sender.sendMessage(new TextComponent(ChatColor.DARK_RED + "The server name you have entered is invalid"));
            }

        } else {
            sender.sendMessage(new TextComponent(ChatColor.DARK_RED + "This is not valid use " + ChatColor.GREEN + "/join (server name)"));
        }

    }

}
