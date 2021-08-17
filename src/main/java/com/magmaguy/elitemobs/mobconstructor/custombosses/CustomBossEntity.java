package com.magmaguy.elitemobs.mobconstructor.custombosses;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.api.EliteMobEnterCombatEvent;
import com.magmaguy.elitemobs.api.EliteMobExitCombatEvent;
import com.magmaguy.elitemobs.api.internal.RemovalReason;
import com.magmaguy.elitemobs.config.EventsConfig;
import com.magmaguy.elitemobs.config.MobCombatSettingsConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.events.CustomEvent;
import com.magmaguy.elitemobs.mobconstructor.CustomSpawn;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.mobconstructor.SimplePersistentEntity;
import com.magmaguy.elitemobs.mobconstructor.SimplePersistentEntityInterface;
import com.magmaguy.elitemobs.playerdata.ElitePlayerInventory;
import com.magmaguy.elitemobs.powers.ElitePower;
import com.magmaguy.elitemobs.powers.bosspowers.CustomSummonPower;
import com.magmaguy.elitemobs.thirdparty.discordsrv.DiscordSRVAnnouncement;
import com.magmaguy.elitemobs.utils.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CustomBossEntity extends EliteEntity implements Listener, SimplePersistentEntityInterface {

    @Getter
    protected static HashSet<CustomBossEntity> trackableCustomBosses = new HashSet<>();
    @Getter
    protected CustomBossesConfigFields customBossesConfigFields;
    protected CustomBossEntity customBossMount = null;
    protected LivingEntity livingEntityMount = null;
    protected CustomBossEntity mount;
    protected SimplePersistentEntity simplePersistentEntity;
    protected Location persistentLocation;
    @Getter
    @Setter
    protected Location spawnLocation;
    protected CustomBossTrail customBossTrail;
    @Getter
    protected CustomBossBossBar customBossBossBar;
    protected Integer escapeMechanism;
    @Getter
    protected PhaseBossEntity phaseBossEntity = null;
    @Getter
    protected String worldName;
    protected boolean chunkLoad = false;
    private long lastTick = 0;
    private int attemptsCounter = 1;
    private List<BukkitTask> globalReinforcements = new ArrayList<>();
    //For use by the phase switcher, which requires passing a specific spawn location for the phase, but when it's for a
    //regional boss this causes issues such as reinforcements getting shifted over to the new spawn location
    @Getter
    @Setter
    private Location phaseSwitchTempSpawnLocation;

    /**
     * Uses a builder pattern in order to construct a CustomBossEntity at an arbitrary point in the future. Does not
     * spawn an entity until the spawn method is invoked. The customBossesConfigFields value isn't final.
     * <p>
     * The {@link CustomBossEntity#setSpawnLocation(Location)} can be assigned at an arbitrary point in the future before the spawn is invoked.
     * Uses:
     * - {@link CustomEvent} queueing through the {@link CustomSpawn} system
     */
    public CustomBossEntity(CustomBossesConfigFields customBossesConfigFields) {
        //This creates a placeholder empty EliteMobEntity to be filled in later
        super();
        //This stores everything that will need to be initialized for the EliteMobEntity
        setCustomBossesConfigFields(customBossesConfigFields);
        super.setPersistent(customBossesConfigFields.isPersistent());
        //Phases are final
        if (!customBossesConfigFields.getPhases().isEmpty())
            this.phaseBossEntity = new PhaseBossEntity(this);
        if (this instanceof RegionalBossEntity)
            super.bypassesProtections = true;
    }

    @Nullable
    public static CustomBossEntity createCustomBossEntity(String filename) {
        CustomBossesConfigFields customBossesConfigFields = CustomBossesConfig.getCustomBoss(filename);
        if (customBossesConfigFields == null)
            return null;
        return new CustomBossEntity(customBossesConfigFields);
    }

    /**
     * The customBossesConfigFields is not final, and changes if the boss has phases, loading the configuration from the phases as the fight goes along.
     *
     * @param customBossesConfigFields Configuration file to be used as the basis of the values of the boss.
     */
    public void setCustomBossesConfigFields(CustomBossesConfigFields customBossesConfigFields) {
        this.customBossesConfigFields = customBossesConfigFields;
        super.setDamageMultiplier(customBossesConfigFields.getDamageMultiplier());
        super.setHealthMultiplier(customBossesConfigFields.getHealthMultiplier());
        super.setHasSpecialLoot(customBossesConfigFields.isDropsEliteMobsLoot());
        super.setVanillaLoot(customBossesConfigFields.isDropsVanillaLoot());
        super.setLevel(customBossesConfigFields.getLevel());
        setPluginName();
        super.elitePowers = ElitePowerParser.parsePowers(customBossesConfigFields.getPowers());
    }

    public void spawn(Location spawnLocation, int level, boolean silent) {
        super.level = level;
        this.spawnLocation = spawnLocation;
        spawn(spawnLocation, silent);
    }

    public void spawn(Location spawnLocation, boolean silent) {
        if (lastTick == spawnLocation.getWorld().getFullTime())
            attemptsCounter++;
        else
            attemptsCounter = 1;
        lastTick = spawnLocation.getWorld().getFullTime();
        if (livingEntity != null) {
            new WarningMessage("Warning: " + customBossesConfigFields.getFilename() + " attempted to double spawn " + attemptsCounter + " times!");
            return;
        }
        this.spawnLocation = spawnLocation;
        spawn(silent);
    }

    public void spawn(boolean silent) {
        if (livingEntity != null) {
            new WarningMessage("Warning: " + customBossesConfigFields.getFilename() + " attempted to double spawn!");
            return;
        }

        if (spawnLocation == null) {
            new WarningMessage("Boss " + customBossesConfigFields.getFilename() + " has a null location! This is probably due to an incorrectly configured regional location!");
            return;
        }

        if (level == -1 && this instanceof RegionalBossEntity)
            new WarningMessage("Boss " + getCustomBossesConfigFields().getFilename() + " is regional and has a dynamic level!" +
                    " This is a terrible idea. Change this as soon as possible!");

        //This is a bit dumb but -1 is reserved for dynamic levels
        if (level == -1)
            if (!(this instanceof RegionalBossEntity) || spawnLocation.getWorld() != null)
                getDynamicLevel(spawnLocation);
            else
                level = 1;

        if (chunkLoad || ChunkLocationChecker.locationIsLoaded(spawnLocation)) {
            chunkLoad = false;
            super.livingEntity = new CustomBossMegaConsumer(this).spawn();
            simplePersistentEntity = null;
        } else
            simplePersistentEntity = new SimplePersistentEntity(this, getLocation());

        if (livingEntity == null && simplePersistentEntity == null) {
            new WarningMessage("EliteMobs tried and failed to spawn " + customBossesConfigFields.getFilename());
            return;
        }

        //It isn't worth initializing things that will notify players or spawn additional entities until we are certain that the boss has actually spawned
        if (livingEntity != null) {
            startBossTrails();
            mountEntity();
            //Prevent custom bosses from getting removed when far away, this is important for mounts and reinforcements in large arenas
            getLivingEntity().setRemoveWhenFarAway(false);
        } else
            persistentLocation = spawnLocation;

        if (!silent)
            announceSpawn();

        if (summoningEntity != null)
            summoningEntity.addReinforcement(this);

        if (spawnLocation.getWorld() != null)
            for (ElitePower elitePower : elitePowers)
                if (elitePower instanceof CustomSummonPower)
                    ((CustomSummonPower) elitePower).getCustomBossReinforcements().forEach((customBossReinforcement -> {
                        if (customBossReinforcement.summonType.equals(CustomSummonPower.SummonType.GLOBAL))
                            globalReinforcements.add(CustomSummonPower.summonGlobalReinforcement(customBossReinforcement, this));
                    }));

        CommandRunner.runCommandFromList(customBossesConfigFields.getOnSpawnCommands(), new ArrayList<>());
    }

    private void setPluginName() {
        if (this.level == -1)
            super.setName(ChatColorConverter.convert(customBossesConfigFields.getName().replace("$level", "?" + "")
                            .replace("$normalLevel", ChatColorConverter.convert("&2[&a" + "?" + "&2]&f"))
                            .replace("$minibossLevel", ChatColorConverter.convert("&6〖&e" + "?" + "&6〗&f"))
                            .replace("$bossLevel", ChatColorConverter.convert("&4『&c" + "?" + "&4』&f"))
                            .replace("$reinforcementLevel", ChatColorConverter.convert("&8〔&7") + "?" + "&8〕&f")
                            .replace("$eventBossLevel", ChatColorConverter.convert("&4「&c" + "?" + "&4」&f"))),
                    false);
        else
            super.setName(ChatColorConverter.convert(customBossesConfigFields.getName().replace("$level", this.level + "")
                            .replace("$normalLevel", ChatColorConverter.convert("&2[&a" + this.level + "&2]&f"))
                            .replace("$minibossLevel", ChatColorConverter.convert("&6〖&e" + this.level + "&6〗&f"))
                            .replace("$bossLevel", ChatColorConverter.convert("&4『&c" + this.level + "&4』&f"))
                            .replace("$reinforcementLevel", ChatColorConverter.convert("&8〔&7") + this.level + "&8〕&f")
                            .replace("$eventBossLevel", ChatColorConverter.convert("&4「&c" + this.level + "&4」&f"))),
                    false);
    }

    public void announceSpawn() {
        setTracking();
        spawnMessage();
        startEscapeMechanismDelay();
    }

    private void setTracking() {
        if (customBossesConfigFields.getAnnouncementPriority() < 1 ||
                !MobCombatSettingsConfig.showCustomBossLocation ||
                customBossesConfigFields.getLocationMessage() == null)
            return;
        trackableCustomBosses.add(this);
        customBossBossBar = new CustomBossBossBar(this);
    }

    private void spawnMessage() {
        if (customBossesConfigFields.getSpawnMessage() == null) return;
        if (customBossesConfigFields.getAnnouncementPriority() < 1) return;
        if (!EventsConfig.ANNOUNCEMENT_BROADCAST_WORLD_ONLY)
            Bukkit.broadcastMessage(ChatColorConverter.convert(customBossesConfigFields.getSpawnMessage()));
        else
            for (Player player : livingEntity.getWorld().getPlayers())
                player.sendMessage(ChatColorConverter.convert(customBossesConfigFields.getSpawnMessage()));
        if (customBossesConfigFields.getAnnouncementPriority() < 3) return;
        new DiscordSRVAnnouncement(ChatColorConverter.convert(customBossesConfigFields.getSpawnMessage()));
    }

    public void getDynamicLevel(Location bossLocation) {
        int bossLevel = 1;
        for (Entity entity : bossLocation.getWorld().getNearbyEntities(bossLocation, Math.max(Bukkit.getViewDistance() * 16, 5 * 16), 256, Bukkit.getViewDistance() * 16))
            if (entity instanceof Player)
                if (ElitePlayerInventory.playerInventories.get(entity.getUniqueId()) != null)
                    if (ElitePlayerInventory.playerInventories.get(entity.getUniqueId()).getNaturalMobSpawnLevel(true) > bossLevel)
                        bossLevel = ElitePlayerInventory.playerInventories.get(entity.getUniqueId()).getNaturalMobSpawnLevel(false);
        super.setLevel(bossLevel);
    }

    private void startBossTrails() {
        customBossTrail = new CustomBossTrail(this);
    }

    private void mountEntity() {
        mount = CustomBossMount.generateMount(this);
    }

    private void startEscapeMechanismDelay() {
        escapeMechanism = CustomBossEscapeMechanism.startEscape(customBossesConfigFields.getTimeout(), this);
    }

    private void cullReinforcements(boolean cullGlobal) {
        if (cullGlobal)
            globalReinforcementEntities.forEach(customBossEntity -> customBossEntity.remove(RemovalReason.REINFORCEMENT_CULL));
        for (CustomBossEntity customBossEntity : eliteReinforcementEntities)
            if (customBossEntity != null)
                customBossEntity.remove(RemovalReason.REINFORCEMENT_CULL);
        for (Entity entity : nonEliteReinforcementEntities)
            if (entity != null)
                entity.remove();

        if (cullGlobal)
            globalReinforcements.clear();
        eliteReinforcementEntities.clear();
        nonEliteReinforcementEntities.clear();
    }

    public Location getLocation() {
        if (getLivingEntity() != null) return getLivingEntity().getLocation();
        else return persistentLocation;
    }

    public double getDamageModifier(Material material) {
        return customBossesConfigFields.getDamageModifier(material);
    }

    @Override
    public boolean exists() {
        return super.exists() || simplePersistentEntity != null;
    }

    @Override
    public void fullHeal() {
        if (phaseBossEntity == null || phaseBossEntity.isInFirstPhase()) {
            super.fullHeal();
            return;
        }
        phaseBossEntity.resetToFirstPhase();
    }

    @Override
    public void remove(RemovalReason removalReason) {
        if (livingEntity != null) persistentLocation = livingEntity.getLocation();
        //Remove the living entity
        super.remove(removalReason);
        //Remove things tied to the living entity
        if (customBossTrail != null) customBossTrail.terminateTrails();
        if (livingEntityMount != null) livingEntityMount.remove();
        if (customBossMount != null) customBossMount.remove(RemovalReason.REINFORCEMENT_CULL);
        if (customBossesConfigFields.isCullReinforcements() || removalReason.equals(RemovalReason.PHASE_BOSS_RESET))
            cullReinforcements(false);

        if (removalReason.equals(RemovalReason.PHASE_BOSS_PHASE_END))
            if(inCombat)
                new EventCaller(new EliteMobExitCombatEvent(this, EliteMobExitCombatEvent.EliteMobExitCombatReason.PHASE_SWITCH));

        boolean bossInstanceEnd = removalReason.equals(RemovalReason.KILL_COMMAND) ||
                removalReason.equals(RemovalReason.DEATH) ||
                removalReason.equals(RemovalReason.BOSS_TIMEOUT) ||
                removalReason.equals(RemovalReason.WORLD_UNLOAD) ||
                removalReason.equals(RemovalReason.SHUTDOWN);

        if (!isPersistent) bossInstanceEnd = true;

        if (bossInstanceEnd) {
            if (escapeMechanism != null) Bukkit.getScheduler().cancelTask(escapeMechanism);
            if (removalReason.equals(RemovalReason.BOSS_TIMEOUT))
                CustomBossEscapeMechanism.doEscapeMessage(this);
            trackableCustomBosses.remove(this);
            if (simplePersistentEntity != null)
                simplePersistentEntity.remove();
            if (customBossBossBar != null)
                customBossBossBar.remove();
            if (phaseBossEntity != null)
                phaseBossEntity.deathReset();
            globalReinforcements.forEach((bukkitTask -> {
                if (bukkitTask != null)
                    bukkitTask.cancel();
            }));
            globalReinforcements.clear();
            if (!removalReason.equals(RemovalReason.REINFORCEMENT_CULL))
                if (summoningEntity != null)
                    summoningEntity.removeReinforcement(this);

            if (isPersistent && removalReason.equals(RemovalReason.WORLD_UNLOAD)) {
                //if the world unloads, the location objects cease to be valid
                spawnLocation.setWorld(null);
                persistentLocation = spawnLocation;
                simplePersistentEntity = new SimplePersistentEntity(this, spawnLocation);
            }

            if (customBossesConfigFields.isCullReinforcements()) cullReinforcements(true);

        } else
            simplePersistentEntity = new SimplePersistentEntity(this, getLocation());

    }

    @Override
    public void chunkLoad() {
        chunkLoad = true;
        spawn(persistentLocation, true);
    }

    @Override
    public void chunkUnload() {
        remove(RemovalReason.CHUNK_UNLOAD);
    }

    @Override
    public void worldLoad() {
        if (spawnLocation != null)
            spawnLocation.setWorld(Bukkit.getWorld(worldName));
        spawn(true);
    }

    @Override
    public void worldUnload() {
        remove(RemovalReason.WORLD_UNLOAD);
    }

    public static class CustomBossEntityEvents implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void removeSlowEvent(EliteMobEnterCombatEvent eliteMobEnterCombatEvent) {
            if (!(eliteMobEnterCombatEvent.getEliteMobEntity() instanceof CustomBossEntity)) return;
            if (eliteMobEnterCombatEvent.getEliteMobEntity().getLivingEntity().getPotionEffect(PotionEffectType.SLOW) == null)
                return;
            if (eliteMobEnterCombatEvent.getEliteMobEntity().getLivingEntity().getPotionEffect(PotionEffectType.SLOW).getAmplifier() == 10)
                return;
            eliteMobEnterCombatEvent.getEliteMobEntity().getLivingEntity().removePotionEffect(PotionEffectType.SLOW);
        }

        @EventHandler(ignoreCancelled = true)
        public void onExitCombat(EliteMobExitCombatEvent event) {
            if (!(event.getEliteMobEntity() instanceof CustomBossEntity)) return;
            if (!((CustomBossEntity) event.getEliteMobEntity()).customBossesConfigFields.isCullReinforcements())
                return;
            ((CustomBossEntity) event.getEliteMobEntity()).cullReinforcements(false);
        }
    }

}
