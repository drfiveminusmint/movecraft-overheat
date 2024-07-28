package net.countercraft.movecraft.movecraftoverheat.listener;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.features.tracking.events.CraftFireWeaponEvent;
import net.countercraft.movecraft.combat.features.tracking.types.Fireball;
import net.countercraft.movecraft.combat.features.tracking.types.TNTCannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.movecraftoverheat.Keys;
import net.countercraft.movecraft.movecraftoverheat.MovecraftOverheat;
import net.countercraft.movecraft.movecraftoverheat.config.Settings;
import net.countercraft.movecraft.movecraftoverheat.tracking.CraftHeat;
import net.countercraft.movecraft.util.MathUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;

import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class WeaponFireListener implements Listener {

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onWeaponFire (CraftFireWeaponEvent event) {
        Craft craft = event.getCraft();
        if (craft == null) return;
        CraftHeat craftHeat = MovecraftOverheat.getInstance().getHeatManager().getHeat(craft);
        if (craftHeat == null) {
            return;
        }
        if (event.getWeaponType() instanceof Fireball) {
            double multiplier;
            try {
                multiplier = craft.getType().getDoubleProperty(Keys.FIREBALL_HEAT_MULTIPLIER);
            } catch (IllegalStateException e) {
                multiplier = 1.0;
            }
            craftHeat.addHeat(Settings.HeatPerFireball *multiplier);
        } else if (event.getWeaponType() instanceof TNTCannon) {
            double multiplier;
            try {
                multiplier = craft.getType().getDoubleProperty(Keys.TNT_HEAT_MULTIPLIER);
            } catch (IllegalStateException e) {
                multiplier = 1.0;
            }
            craftHeat.addHeat(Settings.HeatPerTNT*multiplier);
        }
    }

    @EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense (BlockDispenseEvent event) {
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), event.getBlock().getLocation());
        if (craft == null) return;
        CraftHeat heat = MovecraftOverheat.getInstance().getHeatManager().getHeat(craft);
        if (heat != null) {
            if (craft.getDataTag(CraftHeat.SILENCED) && craft.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(event.getBlock().getLocation()))) {
                event.setCancelled(true);
                craft.getAudience().playSound(Sound.sound(Key.key("block.dispenser.fail"), Sound.Source.BLOCK, 1f, 1f));
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode (EntityExplodeEvent event) {
        // Don't run this check if there's no per shot heat cost
        if (Settings.HeatPerGunshot <= 0) return;
        // Only detect explosions in ballistic water or lava for the purposes of catching cannon shots
        Location location = event.getLocation();
        // Detect waterlogged blocks
        if (!location.getBlock().isLiquid()) {
            BlockData data = location.getBlock().getBlockData();
            if (!(data instanceof Waterlogged)) return;
            if (!((Waterlogged) data).isWaterlogged()) return;
        }
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), location);
        if (craft == null) return;
        // Check if the location is within the craft's hitbox

        if (!craft.getHitBox().contains(new MovecraftLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ())))
            return;
        CraftHeat craftHeat = MovecraftOverheat.getInstance().getHeatManager().getHeat(craft);
        if (craftHeat == null) {
            return;
        }
        // Report an explosion occurring in liquid
        craftHeat.reportExplosion();
    }
}
