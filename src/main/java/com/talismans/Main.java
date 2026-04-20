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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private Map<UUID, List<PotionEffect>> activeEffects = new HashMap<>();
    private Map<String, Talisman> talismans = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadTalismans();
        
        if (getCommand("talisman") != null) {
            getCommand("talisman").setExecutor(new TalismanCommand(this));
        }
        
        getLogger().info("=====================================");
        getLogger().info("TalismansPlugin v1.0 ВКЛЮЧЕН!");
        getLogger().info("Команда: /talisman");
        getLogger().info("=====================================");
    }
    
    @Override
    public void onDisable() {
        for (UUID uuid : activeEffects.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                for (PotionEffect effect : activeEffects.get(uuid)) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }
        getLogger().info("TalismansPlugin выключен!");
    }
    
    public void loadTalismans() {
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
                    if (parts.length == 2) {
                        PotionEffectType type = PotionEffectType.getByName(parts[0]);
                        if (type != null) {
                            int level = Integer.parseInt(parts[1]);
                            effects.add(new PotionEffect(type, Integer.MAX_VALUE, level, true, false));
                        }
                    }
                }
                
                talismans.put(key, new Talisman(name, material, lore, effects));
            } catch (Exception e) {
                getLogger().warning("Ошибка загрузки талисмана " + key);
            }
        }
        getLogger().info("Загружено " + talismans.size() + " талисманов");
    }
    
    public void saveTalisman(String id, String name, String material, String lore, List<PotionEffect> effects) {
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
        
        talismans.put(id, new Talisman(name, material, lore, effects));
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
        for (Talisman talisman : talismans.values()) {
            if (talisman.getLore().equals(loreLine)) {
                applyEffects(p, talisman);
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
    
    private void applyEffects(Player p, Talisman talisman) {
        List<PotionEffect> effects = new ArrayList<>();
        for (PotionEffect effect : talisman.getEffects()) {
            PotionEffect newEffect = new PotionEffect(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier(), true, false);
            p.addPotionEffect(newEffect);
            effects.add(newEffect);
        }
        activeEffects.put(p.getUniqueId(), effects);
    }
    
    public Map<String, Talisman> getTalismans() {
        return talismans;
    }
    
    public void giveTalisman(Player target, String id) {
        Talisman talisman = talismans.get(id);
        if (talisman == null) return;
        
        ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(talisman.getMaterial())));
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
    }
}

class Talisman {
    private String name;
    private String material;
    private String lore;
    private List<PotionEffect> effects;
    
    public Talisman(String name, String material, String lore, List<PotionEffect> effects) {
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.effects = effects;
    }
    
    public String getName() { return name; }
    public String getMaterial() { return material; }
    public String getLore() { return lore; }
    public List<PotionEffect> getEffects() { return effects; }
}
