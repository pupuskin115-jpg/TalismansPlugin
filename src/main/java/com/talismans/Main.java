package com.talismans;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.io.File;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private Map<UUID, List<PotionEffect>> activeEffects = new HashMap<>();
    private Map<String, TalismanData> talismans = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadTalismans();
        
        getCommand("talisman").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Только для игроков!");
                return true;
            }
            
            Player p = (Player) sender;
            
            if (!p.isOp()) {
                p.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            
            if (args.length == 0) {
                p.sendMessage(ChatColor.YELLOW + "Команды:");
                p.sendMessage(ChatColor.GRAY + "/talisman create <id> <название> <материал> <эффект> <уровень>");
                p.sendMessage(ChatColor.GRAY + "/talisman give <id> <игрок>");
                p.sendMessage(ChatColor.GRAY + "/talisman list");
                p.sendMessage(ChatColor.GRAY + "Пример: /talisman create speed &aСкорость DIAMOND SPEED 2");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("list")) {
                if (talismans.isEmpty()) {
                    p.sendMessage(ChatColor.RED + "Нет созданных талисманов!");
                } else {
                    p.sendMessage(ChatColor.GOLD + "Талисманы:");
                    for (String id : talismans.keySet()) {
                        p.sendMessage(ChatColor.GREEN + "- " + id);
                    }
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("create") && args.length >= 6) {
                String id = args[1];
                String name = args[2].replace("&", "§");
                String material = args[3].toUpperCase();
                String effectType = args[4].toUpperCase();
                int level = Integer.parseInt(args[5]) - 1;
                
                if (talismans.containsKey(id)) {
                    p.sendMessage(ChatColor.RED + "Талисман с таким ID уже существует!");
                    return true;
                }
                
                if (Material.getMaterial(material) == null) {
                    p.sendMessage(ChatColor.RED + "Неверный материал! Пример: DIAMOND, GOLD_INGOT, NETHER_STAR");
                    return true;
                }
                
                PotionEffectType type = PotionEffectType.getByName(effectType);
                if (type == null) {
                    p.sendMessage(ChatColor.RED + "Неверный эффект! Доступны: SPEED, STRENGTH, REGENERATION, NIGHT_VISION, JUMP, FIRE_RESISTANCE");
                    return true;
                }
                
                List<PotionEffect> effects = new ArrayList<>();
                effects.add(new PotionEffect(type, Integer.MAX_VALUE, level, true, false));
                
                saveTalisman(id, name, material, id, effects);
                p.sendMessage(ChatColor.GREEN + "Талисман " + id + " создан!");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
                String id = args[1];
                Player target = getServer().getPlayer(args[2]);
                
                if (!talismans.containsKey(id)) {
                    p.sendMessage(ChatColor.RED + "Талисман не найден!");
                    return true;
                }
                
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "Игрок не найден!");
                    return true;
                }
                
                giveTalisman(target, id);
                p.sendMessage(ChatColor.GREEN + "Талисман выдан игроку " + target.getName());
                return true;
            }
            
            p.sendMessage(ChatColor.RED + "Неверная команда! Используй /talisman");
            return true;
        });
        
        getLogger().info("=====================================");
        getLogger().info("TalismansPlugin v1.0 ВКЛЮЧЕН!");
        getLogger().info("Команда: /talisman");
        getLogger().info("=====================================");
    }
    
    @Override
    public void onDisable() {
        for (UUID uuid : activeEffects.keySet()) {
            Player p = getServer().getPlayer(uuid);
            if (p != null) {
                for (PotionEffect effect : activeEffects.get(uuid)) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }
        getLogger().info("TalismansPlugin выключен!");
    }
    
    private void loadTalismans() {
        File file = new File(getDataFolder(), "talismans.yml");
        if (!file.exists()) {
            saveResource("talismans.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        for (String key : config.getKeys(false)) {
            try {
                String name = config.getString(key + ".name");
                String material = config.getString(key + ".material");
                String lore = config.getString(key + ".lore");
                List<String> effectsList = config.getStringList(key + ".effects");
                
                List<PotionEffect> effects = new ArrayList<>();
                for (String effectStr : effectsList) {
                    String[] parts = effectStr.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    if (type != null) {
                        int level = Integer.parseInt(parts[1]);
                        effects.add(new PotionEffect(type, Integer.MAX_VALUE, level, true, false));
                    }
                }
                
                talismans.put(key, new TalismanData(name, material, lore, effects));
            } catch (Exception e) {
                getLogger().warning("Ошибка загрузки талисмана " + key);
            }
        }
        getLogger().info("Загружено " + talismans.size() + " талисманов");
    }
    
    private void saveTalisman(String id, String name, String material, String lore, List<PotionEffect> effects) {
        File file = new File(getDataFolder(), "talismans.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        config.set(id + ".name", name);
        config.set(id + ".material", material);
        config.set(id + ".lore", lore);
        
        List<String> effectsList = new ArrayList<>();
        for (PotionEffect effect : effects) {
            effectsList.add(effect.getType().getName() + ":" + effect.getAmplifier());
        }
        config.set(id + ".effects", effectsList);
        
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        talismans.put(id, new TalismanData(name, material, lore, effects));
    }
    
    private void giveTalisman(Player target, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        
        ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(data.material)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', data.name));
        
        List<String> lore = new ArrayList<>();
        lore.add(data.lore);
        for (PotionEffect effect : data.effects) {
            lore.add(ChatColor.GRAY + "Эффект: " + effect.getType().getName() + " " + (effect.getAmplifier() + 1));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        target.getInventory().addItem(item);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        removeEffects(p);
        
        ItemStack item = p.getInventory().getItem(event.getNewSlot());
        checkAndApply(p, item);
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        checkAndApply(p, p.getInventory().getItemInMainHand());
    }
    
    private void checkAndApply(Player p, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return;
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;
        
        String loreLine = lore.get(0);
        for (TalismanData data : talismans.values()) {
            if (data.lore.equals(loreLine)) {
                applyEffects(p, data);
                break;
            }
        }
    }
    
    private void removeEffects(Player p) {
        if (activeEffects.containsKey(p.getUniqueId())) {
            for (PotionEffect effect : activeEffects.get(p.getUniqueId())) {
                p.removePotionEffect(effect.getType());
            }
            activeEffects.remove(p.getUniqueId());
        }
    }
    
    private void applyEffects(Player p, TalismanData data) {
        List<PotionEffect> effects = new ArrayList<>();
        for (PotionEffect effect : data.effects) {
            PotionEffect newEffect = new PotionEffect(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier(), true, false);
            p.addPotionEffect(newEffect);
            effects.add(newEffect);
        }
        activeEffects.put(p.getUniqueId(), effects);
    }
    
    class TalismanData {
        String name;
        String material;
        String lore;
        List<PotionEffect> effects;
        
        TalismanData(String name, String material, String lore, List<PotionEffect> effects) {
            this.name = name;
            this.material = material;
            this.lore = lore;
            this.effects = effects;
        }
    }
}
