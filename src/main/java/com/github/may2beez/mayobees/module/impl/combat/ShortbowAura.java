package com.github.may2beez.mayobees.module.impl.combat;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.*;
import com.github.may2beez.mayobees.util.helper.Rotation;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShortbowAura implements IModuleActive {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static ShortbowAura instance;

    public static ShortbowAura getInstance() {
        if (instance == null) {
            instance = new ShortbowAura();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Shortbow Aura";
    }

    @Getter
    private boolean enabled = false;

    private Optional<EntityLivingBase> currentTarget = Optional.empty();
    private final CopyOnWriteArrayList<EntityLivingBase> possibleTargets = new CopyOnWriteArrayList<>();

    private final List<Tuple<Entity, Long>> hitTargets = new ArrayList<>();

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        nextRotationSpeed = MayOBeesConfig.getRandomizedRotationSpeed();
        LogUtils.info("Shortbow Aura enabled!");
    }

    @Override
    public void onDisable() {
        enabled = false;
        currentTarget = Optional.empty();
        hitTargets.clear();
        possibleTargets.clear();
        lastHit = 0;
        currentRandomDelay = MayOBeesConfig.getRandomizedCooldown();
        if (!MayOBeesConfig.shortBowAuraRotationType)
            RotationHandler.getInstance().easeBackFromServerRotation();
        else
            RotationHandler.getInstance().reset();
        LogUtils.info("Shortbow Aura disabled!");
    }

    private long lastHit = 0;
    private long currentRandomDelay = MayOBeesConfig.getRandomizedCooldown();
    private long nextRotationSpeed = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (!enabled) return;
        hitTargets.removeIf(tuple -> System.currentTimeMillis() - tuple.getSecond() > 1000);
        if (currentTarget.isPresent() && currentTarget.get().isDead) {
            currentTarget = Optional.empty();
            return;
        }
        List<String> playerOnTab = TablistUtils.getTabListPlayersSkyblock();
        List<EntityLivingBase> tempPossibleTarget = mc.theWorld.getEntities(EntityLivingBase.class, entity ->
                entity != mc.thePlayer &&
                        mc.thePlayer.getDistanceToEntity(entity) < MayOBeesConfig.shortBowAuraRange &&
                        EntityUtils.isEntityInFOV(entity, MayOBeesConfig.shortBowAuraFOV) &&
                        this.isValidTarget(entity) &&
                        !EntityUtils.isNPC(entity) &&
                        !EntityUtils.isPlayer(entity, playerOnTab) &&
                        hitTargets.stream().noneMatch(tuple -> tuple.getFirst() == entity));
        possibleTargets.clear();
        possibleTargets.addAll(tempPossibleTarget);

        Rotation startRotation;
        if (!MayOBeesConfig.shortBowAuraRotationType && RotationHandler.getInstance().isRotating()) {
            startRotation = new Rotation(RotationHandler.getInstance().getServerSideYaw(), RotationHandler.getInstance().getServerSidePitch());
        } else {
            startRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        if (!currentTarget.isPresent()) {
            if (possibleTargets.size() == 1) {
                currentTarget = Optional.of(possibleTargets.get(0));
            } else if (possibleTargets.size() > 1) {
                currentTarget = possibleTargets.stream().min((entity1, entity2) -> {
                    Rotation rot1 = RotationHandler.getInstance().getNeededChange(startRotation, RotationHandler.getInstance().getRotation(entity1));
                    Rotation rot2 = RotationHandler.getInstance().getNeededChange(startRotation, RotationHandler.getInstance().getRotation(entity2));
                    return Float.compare(Math.abs(rot1.getYaw()), Math.abs(rot2.getYaw()));
                });
            }
            if (!currentTarget.isPresent()) {
                return;
            }
        }

        if (currentTarget.get().isDead || currentTarget.get().getDistanceToEntity(mc.thePlayer) > MayOBeesConfig.shortBowAuraRange || currentTarget.get().getHealth() <= 0 || !mc.thePlayer.canEntityBeSeen(currentTarget.get())) {
            currentTarget = Optional.empty();
            if (RotationHandler.getInstance().isRotating()) {
                RotationHandler.getInstance().reset();
            }
            return;
        }

        int slot = InventoryUtils.getSlotIdOfItemInHotbar(MayOBeesConfig.shortBowAuraItemName);

        if (MayOBeesConfig.shortBowAuraItemName.isEmpty() || slot == -1 || slot != mc.thePlayer.inventory.currentItem) {
            return;
        }

        if (System.currentTimeMillis() < lastHit + currentRandomDelay - nextRotationSpeed + 75) {
            return;
        }

        if (RotationHandler.getInstance().isRotating() && !RotationHandler.getInstance().getConfiguration().goingBackToClientSide()) {
            return;
        }
        Rotation neededChange = RotationHandler.getInstance().getNeededChange(startRotation, RotationHandler.getInstance().getRotation(currentTarget.get()));
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(currentTarget.get()),
                        nextRotationSpeed,
                        MayOBeesConfig.shortBowAuraRotationType ? RotationConfiguration.RotationType.CLIENT : RotationConfiguration.RotationType.SERVER,
                        this::shootEntity
                ).bowRotation(!MayOBeesConfig.shortBowAuraRotationMode).easeOutBack(Math.abs(neededChange.getYaw()) > 60).followTarget(true)
        );
    }

    private void shootEntity() {
        if (MayOBeesConfig.shortBowAuraMouseButton) {
            KeyBindUtils.rightClick();
        } else {
            KeyBindUtils.leftClick();
        }
        if (!MayOBeesConfig.shortBowAuraAttackUntilDead) {
            currentTarget.ifPresent(entityLivingBase -> hitTargets.add(new Tuple<>(entityLivingBase, System.currentTimeMillis())));
            currentTarget = Optional.empty();
        }
        lastHit = System.currentTimeMillis();
        currentRandomDelay = MayOBeesConfig.getRandomizedCooldown();
        nextRotationSpeed = MayOBeesConfig.getRandomizedRotationSpeed();
        if (!MayOBeesConfig.shortBowAuraRotationType)
            RotationHandler.getInstance().easeBackFromServerRotation();
        else
            RotationHandler.getInstance().reset();
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity.isInvisible()) {
            return false;
        }
        if (entity instanceof EntityArmorStand) {
            return false;
        }
        if (!mc.thePlayer.canEntityBeSeen(entity)) {
            return false;
        }

        if (entity.getHealth() <= 0.0f) {
            return false;
        }

        if (!MayOBeesConfig.shortBowAuraAttackMobs && (entity instanceof EntityMob || entity instanceof EntityAmbientCreature || entity instanceof EntityWaterMob || entity instanceof EntityAnimal || entity instanceof EntitySlime)) {
            return false;
        }

        if (entity instanceof EntityPlayer) {
            return !EntityUtils.isTeam(entity);
        }


        return !(entity instanceof EntityVillager);
    }

    private final Color possibleTargetColor = new Color(180, 0, 0, 100);

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!enabled) return;
        if (currentTarget.isPresent()) {
            AxisAlignedBB bb = currentTarget.get().getEntityBoundingBox();
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, MayOBeesConfig.shortBowAuraTargetColor.toJavaColor());
        }

        for (Entity entity : possibleTargets) {
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, possibleTargetColor);
        }

        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 playerLeftLooking = AngleUtils.getVectorForRotation(0, mc.thePlayer.rotationYaw - MayOBeesConfig.shortBowAuraFOV / 2f);
        Vec3 playerRightLooking = AngleUtils.getVectorForRotation(0, mc.thePlayer.rotationYaw + MayOBeesConfig.shortBowAuraFOV / 2f);
        Vec3 playerLeftLookingEnd = playerPos.addVector(playerLeftLooking.xCoord * MayOBeesConfig.shortBowAuraRange, playerLeftLooking.yCoord * MayOBeesConfig.shortBowAuraRange, playerLeftLooking.zCoord * MayOBeesConfig.shortBowAuraRange);
        Vec3 playerRightLookingEnd = playerPos.addVector(playerRightLooking.xCoord * MayOBeesConfig.shortBowAuraRange, playerRightLooking.yCoord * MayOBeesConfig.shortBowAuraRange, playerRightLooking.zCoord * MayOBeesConfig.shortBowAuraRange);
        RenderUtils.drawTracer(new Vec3(0, 0, 0), playerLeftLookingEnd, Color.WHITE);
        RenderUtils.drawTracer(new Vec3(0, 0, 0), playerRightLookingEnd, Color.WHITE);
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        currentTarget = Optional.empty();
        possibleTargets.clear();
        if (enabled)
            onDisable();
    }
}
