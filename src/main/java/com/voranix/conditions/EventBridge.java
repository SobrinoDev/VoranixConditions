package com.voranix.conditions;

import com.voranix.conditions.mob.CustomMob;
import com.voranix.conditions.mob.MobManager;
import com.voranix.conditions.script.Context;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Convierte los eventos de Bukkit en eventos de scripts ("damage", "hurt", "death", ...)
 * y de mobs personalizados ("attack", "hurt", "death", "target", "interact").
 */
public final class EventBridge implements Listener {

    private final VoranixConditions plugin;

    public EventBridge(VoranixConditions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = null;
        Entity projectile = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            attacker = byEntity.getDamager();
            if (attacker instanceof Projectile p) {
                projectile = p;
                attacker = p.getShooter() instanceof Entity shooter ? shooter : null;
            }
        }
        String cause = event.getCause().name();

        plugin.scripts().fire("hurt", context(event, cause).role("victim", victim).role("attacker", attacker)
                .role("projectile", projectile));
        if (attacker != null) {
            plugin.scripts().fire("damage", context(event, cause).role("victim", victim).role("attacker", attacker)
                    .role("projectile", projectile));
            if (MobManager.idOf(attacker) != null) {
                plugin.mobs().fire(attacker, "attack", context(event, cause).role("victim", victim));
            }
        }
        if (MobManager.idOf(victim) != null) {
            plugin.mobs().fire(victim, "hurt", context(event, cause).role("attacker", attacker));
        }
    }

    private static Context context(EntityDamageEvent event, String cause) {
        return new Context().event(event).var("damage", event.getDamage()).var("cause", cause);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        // 1.20.1 no tiene DamageSource: se usa el último golpe recibido
        Entity killer = victim.getKiller();
        if (killer == null && victim.getLastDamageCause() instanceof EntityDamageByEntityEvent last) {
            killer = last.getDamager();
            if (killer instanceof Projectile p) killer = p.getShooter() instanceof Entity shooter ? shooter : null;
        }

        plugin.scripts().fire("death", new Context().event(event).role("victim", victim).role("killer", killer));
        CustomMob mob = plugin.mobs().of(victim);
        if (mob != null) {
            if (!mob.dropsVanilla()) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            plugin.mobs().fire(victim, "death", new Context().event(event).role("killer", killer));
            plugin.mobs().untrack(victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM && MobManager.idOf(entity) == null) {
            if (plugin.mobs().tryReplace(entity)) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.scripts().fire("spawn", new Context().event(event).role("entity", entity)
                .var("reason", event.getSpawnReason().name()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() == null || MobManager.idOf(event.getEntity()) == null) return;
        plugin.mobs().fire(event.getEntity(), "target", new Context().event(event).role("target", event.getTarget()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity target = event.getRightClicked();
        plugin.scripts().fire("interact", new Context().event(event).role("player", event.getPlayer())
                .role("target", target));
        if (MobManager.idOf(target) != null) {
            plugin.mobs().fire(target, "interact", new Context().event(event).role("player", event.getPlayer()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.scripts().fire("join", new Context().event(event).role("player", event.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Un tick después, para que los efectos de poción no se pierdan al reaparecer
        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.scripts().fire("respawn", new Context().role("player", event.getPlayer())));
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity e : event.getEntities()) {
            if (e instanceof LivingEntity l && MobManager.idOf(l) != null) plugin.mobs().track(l);
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity e : event.getEntities()) {
            if (MobManager.idOf(e) != null) plugin.mobs().untrack(e);
        }
    }
}
