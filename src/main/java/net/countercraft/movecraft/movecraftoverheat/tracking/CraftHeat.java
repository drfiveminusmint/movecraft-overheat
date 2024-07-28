package net.countercraft.movecraft.movecraftoverheat.tracking;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.datatag.CraftDataTagContainer;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.movecraftoverheat.Keys;
import net.countercraft.movecraft.movecraftoverheat.MovecraftOverheat;
import net.countercraft.movecraft.movecraftoverheat.config.Settings;
import net.countercraft.movecraft.movecraftoverheat.disaster.DisasterType;
import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.Counter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.jetbrains.annotations.NotNull;

public class CraftHeat {
    public static final CraftDataTagKey<Double> HEAT_CAPACITY = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft-overheat", "heat-capacity"), craft -> (double) craft.getOrigBlockCount() * craft.getType().getDoubleProperty(Keys.HEAT_CAPACITY_PER_BLOCK));
    public static final CraftDataTagKey<Double> HEAT = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft-overheat", "heat"), craft -> 0D);
    public static final CraftDataTagKey<Double> DISSIPATION = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft-overheat", "dissipation"), craft -> (double) craft.getOrigBlockCount() * craft.getType().getDoubleProperty(Keys.HEAT_DISSIPATION_PER_BLOCK));
    public static final CraftDataTagKey<Boolean> SILENCED = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft-overheat", "silenced"), craft -> false);
    private final Craft craft;
    private long lastUpdate;
    private long lastDisaster;

    private boolean firedThisTick;
    private int explosionsThisTick;
    private final BossBar bossBar;

    public CraftHeat (@NotNull Craft c) {
        this.craft = c;
        this.bossBar = Bukkit.createBossBar("Heat", BarColor.GREEN, BarStyle.SEGMENTED_6);
        bossBar.setVisible(false);
        if (craft instanceof PlayerCraft) {
            this.bossBar.addPlayer(((PlayerCraft) this.craft).getPilot());
        }
    }

    public void recalculate () {
        double newCapacity = craft.getType().getDoubleProperty(Keys.BASE_HEAT_CAPACITY);
        double newDissipation = craft.getType().getDoubleProperty(Keys.BASE_HEAT_DISSIPATION);
        double cPerBlock = craft.getType().getDoubleProperty(Keys.HEAT_CAPACITY_PER_BLOCK);
        double dPerBlock = craft.getType().getDoubleProperty(Keys.HEAT_DISSIPATION_PER_BLOCK);

        Counter<Material> materials = craft.getDataTag(Craft.MATERIALS);
        if (materials.isEmpty())
            return;

        for (Material m : materials.getKeySet()) {
            if (Settings.HeatSinkBlocks.containsKey(m)) {
                newCapacity += Settings.HeatSinkBlocks.get(m) * cPerBlock * materials.get(m);
            } else {
                newCapacity += cPerBlock * materials.get(m);
            }
            if (Settings.RadiatorBlocks.containsKey(m)) {
                newDissipation += Settings.RadiatorBlocks.get(m) * dPerBlock * materials.get(m);
            } else {
                newDissipation += dPerBlock * materials.get(m);
            }
        }

        if (newCapacity <= 1.0) {
            newCapacity = 1.0;
        }
        newCapacity = Math.round(newCapacity);

        double heat = craft.getDataTag(HEAT);
        if (Math.abs(heat) >= 0.01) {
            heat *= newCapacity / craft.getDataTag(HEAT_CAPACITY);
            craft.setDataTag(HEAT, heat);
        }
        craft.setDataTag(HEAT_CAPACITY, newCapacity);
        craft.setDataTag(DISSIPATION, newDissipation);

        updateBossBar();
    }

    public void processDissipation () {
        double heat = craft.getDataTag(HEAT);
        if (heat > 0.0) {
            heat -= craft.getDataTag(DISSIPATION);
        }
        if (heat < 0.0) {
            heat = 0.0;
        }
        craft.setDataTag(HEAT, heat);
        updateBossBar();
    }

    public void checkDisasters () {
        // Update whether the craft is silenced
        if (craft.getDataTag(SILENCED)) {
            if (craft.getDataTag(HEAT) < craft.getDataTag(HEAT_CAPACITY) * Settings.SilenceHeatThreshold) {
                craft.getAudience().sendMessage(Component.text(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "No longer silenced!"));
                craft.setDataTag(SILENCED, false);
            }
        } else {
            if (Settings.SilenceOverheatedCrafts && craft.getDataTag(HEAT) > craft.getDataTag(HEAT_CAPACITY) * Settings.SilenceHeatThreshold) {
                craft.getAudience().sendMessage(Component.text(ChatUtils.MOVECRAFT_COMMAND_PREFIX + ChatColor.RED + "Silenced! Your guns are too hot to fire!"));
                craft.getAudience().playSound(Sound.sound(Key.key("entity.blaze.death"), Sound.Source.BLOCK, 2.0f, 1.0f));
                craft.setDataTag(SILENCED, true);
            }
        }
        for (DisasterType type : MovecraftOverheat.getDisasterTypes()) {
            if (type.getHeatThreshold() * craft.getDataTag(HEAT_CAPACITY) > craft.getDataTag(HEAT)) continue;
            if ((1-type.getRandomChance()) * (Math.exp(-1 * type.getRandomChancePowerFactor() * ((craft.getDataTag(HEAT)/craft.getDataTag(HEAT_CAPACITY))-type.getHeatThreshold()))) > Math.random()) continue;
            MovecraftOverheat.getInstance().getHeatManager().addDisaster(type.createNew(this));
            lastDisaster = System.currentTimeMillis();
        }
    }

    public Craft getCraft() {
        return craft;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long l) {
        lastUpdate = l;
    }

    public void addHeat (double heatToAdd) {
        double heat = craft.getDataTag(HEAT);
        heat += heatToAdd;
        craft.setDataTag(HEAT, heat);
        updateBossBar();
    }

    public long getLastDisaster() {
        return lastDisaster;
    }

    private void updateBossBar() {
        bossBar.setTitle(String.format("Heat: %.1f / %.1f", craft.getDataTag(HEAT), craft.getDataTag(HEAT_CAPACITY)));
        if (craft.getDataTag(HEAT) >= craft.getDataTag(HEAT_CAPACITY)*1.5) {
            bossBar.setColor(BarColor.RED);
        } else if (craft.getDataTag(HEAT) >= craft.getDataTag(HEAT_CAPACITY)) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
        bossBar.setProgress(Math.min(1.0, craft.getDataTag(HEAT) / craft.getDataTag(HEAT_CAPACITY)));
        bossBar.setVisible(craft.getDataTag(HEAT) != 0.0);
    }

    public void removeBossBar () {
        bossBar.removeAll();
    }

    public void reportExplosion() {
        if (++explosionsThisTick >= Settings.ExplosionsPerGunshot && !firedThisTick) {
            addHeat(Settings.HeatPerGunshot);
            firedThisTick = true;
        }
    }

    public void resetGunshotDetection() {
        firedThisTick = false;
        explosionsThisTick = 0;
    }
}
