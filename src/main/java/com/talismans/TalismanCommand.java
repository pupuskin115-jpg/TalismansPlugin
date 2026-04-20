package com.talismans;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class TalismanCommand implements CommandExecutor {
    
    private Main plugin;
    
    public TalismanCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков!");
            return true;
        }
        
        Player p = (Player) sender;
        
        if (!p.isOp()) {
            p.sendMessage(ChatColor.RED + "Нет прав! Требуется OP.");
            return true;
        }
        
        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Команды:");
            p.sendMessage(ChatColor.GRAY + "/talisman create <id> <название> <материал> <эффект> <уровень>");
            p.sendMessage(ChatColor.GRAY + "/talisman give <id> <игрок>");
            p.sendMessage(ChatColor.GRAY + "/talisman list");
            p.sendMessage(ChatColor.GRAY + "Пример: /talisman create speed1 &aСкорость DIAMOND SPEED 2");
            p.sendMessage(ChatColor.GRAY + "Доступные эффекты: SPEED, STRENGTH, REGENERATION, NIGHT_VISION, JUMP");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("list")) {
            p.sendMessage(ChatColor.GOLD + "Талисманы:");
            for (String id : plugin.getTalismans().keySet()) {
                p.sendMessage(ChatColor.GREEN + "- " + id);
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("create") && args.length >= 6) {
            String id = args[1];
            String name = args[2].replace("&", "§");
            String material = args[3].toUpperCase();
            String effectType = args[4].toUpperCase();
            int level = Integer.parseInt(args[5]) - 1;
            
            if (plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман с таким ID уже существует!");
                return true;
            }
            
            if (Material.getMaterial(material) == null) {
                p.sendMessage(ChatColor.RED + "Неверный материал! Пример: DIAMOND, GOLD_INGOT, NETHER_STAR");
                return true;
            }
            
            PotionEffectType type = PotionEffectType.getByName(effectType);
            if (type == null) {
                p.sendMessage(ChatColor.RED + "Неверный эффект! Доступны: SPEED, STRENGTH, REGENERATION, NIGHT_VISION, JUMP");
                return true;
            }
            
            List<PotionEffect> effects = new ArrayList<>();
            effects.add(new PotionEffect(type, Integer.MAX_VALUE, level, true, false));
            
            plugin.saveTalisman(id, name, material, id, effects);
            p.sendMessage(ChatColor.GREEN + "Талисман " + id + " создан! Используй /talisman give " + id + " <игрок>");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            String id = args[1];
            Player target = plugin.getServer().getPlayer(args[2]);
            
            if (!plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман не найден! Используй /talisman list");
                return true;
            }
            
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Игрок не найден!");
                return true;
            }
            
            plugin.giveTalisman(target, id);
            p.sendMessage(ChatColor.GREEN + "Талисман выдан игроку " + target.getName());
            return true;
        }
        
        p.sendMessage(ChatColor.RED + "Неверная команда! Используй /talisman");
        return true;
    }
}
