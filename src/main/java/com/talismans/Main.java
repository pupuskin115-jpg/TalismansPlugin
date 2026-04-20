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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.*;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    
    private Map<UUID, List<PotionEffect>> activeEffects = new HashMap<>();
    private Map<String, Talisman> talismans = new HashMap<>();
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        loadTalismans();
        if (getCommand("talisman") != null) {
            getCommand("talisman").setExecutor(new TalismanCommand(this));
        }
        getLogger().info("TalismansPlugin включен!");
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
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        for (String key : config.getKeys(false)) {
            try {
                talismans.put(key, new Talisman(config.getConfigurationSection(key)));
            } catch (Exception e) {
                getLogger().warning("Ошибка загрузки талисмана " + key + ": " + e.getMessage());
            }
        }
        getLogger().info("Загружено " + talismans.size() + " талисманов");
    }
    
    public void saveTalisman(String id, Talisman talisman) {
        File file = new File(getDataFolder(), "talismans.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        config.set(id + ".name", talisman.getName());
        config.set(id + ".material", talisman.getMaterial());
        config.set(id + ".lore", talisman.getLore());
        
        List<String> effects = new ArrayList<>();
        for (PotionEffect effect : talisman.getEffects()) {
            effects.add(effect.getType().getName() + ":" + effect.getAmplifier());
        }
        config.set(id + ".effects", effects);
        
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        talismans.put(id, talisman);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        removeEffects(p);
        
        ItemStack item = p.getInventory().getItem(event.getNewSlot());
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                String loreLine = lore.get(0);
                for (Talisman talisman : talismans.values()) {
                    if (talisman.getLore().equals(loreLine)) {
                        addEffects(p, talisman);
                        break;
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                String loreLine = lore.get(0);
                for (Talisman talisman : talismans.values()) {
                    if (talisman.getLore().equals(loreLine)) {
                        addEffects(p, talisman);
                        break;
                    }
                }
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
    
    private void addEffects(Player p, Talisman talisman) {
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
}

class Talisman {
    private String name;
    private String material;
    private String lore;
    private List<PotionEffect> effects;
    
    public Talisman(ConfigurationSection section) {
        this.name = ChatColor.translateAlternateColorCodes('&', section.getString("name", "&aТалисман"));
        this.material = section.getString("material", "DIAMOND");
        this.lore = section.getString("lore", "talisman_" + System.currentTimeMillis());
        this.effects = new ArrayList<>();
        
        List<String> effectsList = section.getStringList("effects");
        for (String effectStr : effectsList) {
            String[] parts = effectStr.split(":");
            if (parts.length == 2) {
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                if (type != null) {
                    int amplifier = Integer.parseInt(parts[1]);
                    effects.add(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
                }
            }
        }
    }
    
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
