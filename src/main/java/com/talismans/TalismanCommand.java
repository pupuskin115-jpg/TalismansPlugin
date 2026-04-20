package com.talismans;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TalismanCommand implements CommandExecutor {
    
    private Main plugin;
    
    public TalismanCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }
        
        Player p = (Player) sender;
        
        if (!p.hasPermission("talismans.admin")) {
            p.sendMessage(ChatColor.RED + "Нет прав!");
            return true;
        }
        
        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Команды:");
            p.sendMessage(ChatColor.GRAY + "/talisman list" + ChatColor.WHITE + " - список талисманов");
            p.sendMessage(ChatColor.GRAY + "/talisman give <id> <игрок>" + ChatColor.WHITE + " - выдать талисман");
            p.sendMessage(ChatColor.GRAY + "/talisman create <id> <название>" + ChatColor.WHITE + " - создать талисман");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("list")) {
            p.sendMessage(ChatColor.GOLD + "Талисманы:");
            for (String id : plugin.getTalismans().keySet()) {
                p.sendMessage(ChatColor.GREEN + "- " + id);
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            String id = args[1];
            Player target = plugin.getServer().getPlayer(args[2]);
            
            if (!plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман не найден!");
                return true;
            }
            
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Игрок не найден!");
                return true;
            }
            
            Talisman talisman = plugin.getTalismans().get(id);
            ItemStack item = new ItemStack(Material.getMaterial(talisman.getMaterial()));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', talisman.getName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(talisman.getLore());
            for (PotionEffect effect : talisman.getEffects()) {
                lore.add(ChatColor.GRAY + "Эффект: " + effect.getType().getName() + " " + (effect.getAmplifier() + 1));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            target.getInventory().addItem(item);
            p.sendMessage(ChatColor.GREEN + "Талисман выдан!");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
            String id = args[1];
            String name = args[2].replace("&", "§");
            
            if (plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман с таким ID уже существует!");
                return true;
            }
            
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                p.sendMessage(ChatColor.RED + "Возьми предмет в руку!");
                return true;
            }
            
            p.sendMessage(ChatColor.GREEN + "Создание талисмана " + name);
            p.sendMessage(ChatColor.YELLOW + "Напиши в чат эффекты через запятую:");
            p.sendMessage(ChatColor.GRAY + "Доступные: SPEED, STRENGTH, REGENERATION, NIGHT_VISION, LUCK, JUMP, FIRE_RESISTANCE, WATER_BREATHING");
            p.sendMessage(ChatColor.GRAY + "Пример: SPEED:2,STRENGTH:1");
            
            // TODO: GUI или чат ввод для выбора эффектов
            // Пока создаём простой талисман со скоростью
            List<PotionEffect> effects = new ArrayList<>();
            effects.add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
            
            Talisman talisman = new Talisman(name, item.getType().name(), id, effects);
            plugin.saveTalisman(id, talisman);
            
            p.sendMessage(ChatColor.GREEN + "Талисман создан! Используй /talisman give " + id + " <игрок>");
            return true;
        }
        
        return true;
    }
}
