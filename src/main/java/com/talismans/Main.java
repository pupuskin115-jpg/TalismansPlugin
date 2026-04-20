package com.talismans;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.ChatColor;

public class Main extends JavaPlugin implements Listener {
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        // Простая команда для проверки
        if (getCommand("talisman") != null) {
            getCommand("talisman").setExecutor((sender, cmd, label, args) -> {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    p.sendMessage(ChatColor.GREEN + "Плагин TalismansPlugin работает!");
                } else {
                    sender.sendMessage("Плагин работает!");
                }
                return true;
            });
        }
        
        getLogger().info("=====================================");
        getLogger().info("TalismansPlugin v1.0 ВКЛЮЧЕН!");
        getLogger().info("Команда: /talisman");
        getLogger().info("=====================================");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("TalismansPlugin выключен!");
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(ChatColor.GREEN + "Добро пожаловать! Плагин талисманов активен.");
    }
}
