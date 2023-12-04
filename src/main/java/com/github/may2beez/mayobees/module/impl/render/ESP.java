package com.github.may2beez.mayobees.module.impl.render;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.github.may2beez.mayobees.MayOBees;
import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.event.ClickEvent;
import com.github.may2beez.mayobees.handler.GameStateHandler;
import com.github.may2beez.mayobees.module.IModule;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ESP implements IModule {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<Tuple<String, BlockPos>> clickedFairySouls = new ArrayList<>();
    private final List<BlockPos> clickedGifts = new ArrayList<>();
    private final File clickedFairySoulsFile = new File(mc.mcDataDir + "/config/mayobees/clickedFairySouls.json");
    private static ESP instance;

    private final String FAIRY_SOUL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjk2OTIzYWQyNDczMTAwMDdmNmFlNWQzMjZkODQ3YWQ1Mzg2NGNmMTZjMzU2NWExODFkYzhlNmIyMGJlMjM4NyJ9fX0=";
    private final String[] GIFT_TEXTURES = new String[]{
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBmNTM5ODUxMGIxYTA1YWZjNWIyMDFlYWQ4YmZjNTgzZTU3ZDcyMDJmNTE5M2IwYjc2MWZjYmQwYWUyIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ5N2Y0ZjQ0ZTc5NmY3OWNhNDMw0TdmYWE3YjRmZTkxYzQ0NWM3NmU1YzI2YTVhZDc5NGY1ZTQ3OTgzNyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjczYTIxMTQxMzZiOGVlNDkyNmNhYTUxNzg1NDE0MD2M2YTJiNzZlNGYxNjY4Y2I4OWQ5OTcxNmM0MjEifX19"
    };

    public static ESP getInstance() {
        if (instance == null) {
            instance = new ESP();
        }
        return instance;
    }

    public ESP() {
        try {
            if (!clickedFairySoulsFile.getParentFile().exists()) {
                clickedFairySoulsFile.getParentFile().mkdirs();
            }
            if (!clickedFairySoulsFile.exists()) {
                clickedFairySoulsFile.createNewFile();
                // fill it with empty array
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(clickedFairySoulsFile);
                    String json = MayOBees.GSON.toJson(clickedFairySouls);
                    fileOutputStream.write(json.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (fileOutputStream != null) try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(clickedFairySoulsFile);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            String json = new String(bytes);
            if (json.isEmpty()) return;
            Type type = new TypeToken<Tuple<String, BlockPos>[]>() {
            }.getType();
            Tuple<String, BlockPos>[] locations = MayOBees.GSON.fromJson(json, type);
            clickedFairySouls.addAll(Arrays.asList(locations));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileInputStream != null) try {
                fileInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveClickedFairySouls() {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(clickedFairySoulsFile);
            String json = MayOBees.GSON.toJson(clickedFairySouls);
            fileOutputStream.write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileOutputStream != null) try {
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void resetClickedFairySouls() {
        clickedFairySouls.clear();
        saveClickedFairySouls();
    }

    public void resetClickedGifts() {
        clickedGifts.clear();
    }

    @Override
    public boolean isRunning() {
        return MayOBeesConfig.chestESP || MayOBeesConfig.fairySoulESP || MayOBeesConfig.giftESP;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        if (MayOBeesConfig.chestESP) {
            for (TileEntity tileEntityChest : mc.theWorld.loadedTileEntityList.stream().filter(tileEntity -> tileEntity instanceof TileEntityChest).collect(Collectors.toList())) {
                Block block = mc.theWorld.getBlockState(tileEntityChest.getPos()).getBlock();
                block.setBlockBoundsBasedOnState(mc.theWorld, tileEntityChest.getPos());
                AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, tileEntityChest.getPos()).expand(0.002, 0.002, 0.002).offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
                RenderUtils.drawBox(bb, MayOBeesConfig.chestESPColor.toJavaColor());
                if (MayOBeesConfig.chestESPTracers) {
                    RenderUtils.drawTracer(new Vec3(tileEntityChest.getPos().getX() + 0.5, tileEntityChest.getPos().getY() + 0.5, tileEntityChest.getPos().getZ() + 0.5), MayOBeesConfig.chestESPColor.toJavaColor());
                }
            }
        }

        AxisAlignedBB closestFairySoulBb = null;

        for (EntityArmorStand entityArmorStand : mc.theWorld.loadedEntityList.stream().filter(entity -> entity instanceof EntityArmorStand).map(entity -> (EntityArmorStand) entity).collect(Collectors.toList())) {
            ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
            if (helmet == null || !helmet.hasTagCompound()) continue;

            if (MayOBeesConfig.fairySoulESP) {
                closestFairySoulBb = fairySoulEntityCheck(entityArmorStand, closestFairySoulBb);
            }

            if (MayOBeesConfig.giftESP && (!MayOBeesConfig.giftESPShowOnlyOnJerryWorkshop || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.JERRY_WORKSHOP)) {
                giftEntityCheck(entityArmorStand);
            }
        }

        drawClosest(closestFairySoulBb, MayOBeesConfig.fairySoulESPColor, MayOBeesConfig.fairySoulESPTracers, MayOBeesConfig.fairySoulESPShowDistance);
    }

    private void drawClosest(AxisAlignedBB closestFairySoulBb, OneColor fairySoulESPColor, boolean fairySoulESPTracers, boolean fairySoulESPShowDistance) {
        if (closestFairySoulBb != null) {
            RenderUtils.drawBox(closestFairySoulBb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ), fairySoulESPColor.toJavaColor());
            double x = closestFairySoulBb.minX + (closestFairySoulBb.maxX - closestFairySoulBb.minX) / 2;
            double y = closestFairySoulBb.minY + (closestFairySoulBb.maxY - closestFairySoulBb.minY) / 2;
            double z = closestFairySoulBb.minZ + (closestFairySoulBb.maxZ - closestFairySoulBb.minZ) / 2;
            if (fairySoulESPTracers) {
                RenderUtils.drawTracer(new Vec3(x, y, z), fairySoulESPColor.toJavaColor());
            }
            if (fairySoulESPShowDistance) {
                double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(closestFairySoulBb.minX + 0.5, closestFairySoulBb.minY + 0.5, closestFairySoulBb.minZ + 0.5)));
                RenderUtils.drawText(String.format("%.2fm", distance), x, y, z, 1);
            }
        }
    }

    private AxisAlignedBB fairySoulEntityCheck(EntityArmorStand entityArmorStand, AxisAlignedBB closestBb) {
        ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return closestBb;
        if (!helmet.getTagCompound().toString().contains(FAIRY_SOUL_TEXTURE)) return closestBb;
        if (clickedFairySouls.stream().anyMatch(cfs -> cfs.getFirst().equals(GameStateHandler.getInstance().getLocation().getName()) && cfs.getSecond().equals(entityArmorStand.getPosition())))
            return closestBb;
        AxisAlignedBB bb = new AxisAlignedBB(entityArmorStand.posX - 0.5, entityArmorStand.posY + entityArmorStand.getEyeHeight() - 0.5, entityArmorStand.posZ - 0.5, entityArmorStand.posX + 0.5, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ + 0.5).expand(0.002, 0.002, 0.002);
        if (MayOBeesConfig.fairySoulESPShowOnlyClosest) {
            if (closestBb == null) {
                closestBb = bb;
            } else {
                if (mc.thePlayer.getDistanceSqToCenter(entityArmorStand.getPosition()) < mc.thePlayer.getDistanceSqToCenter(new BlockPos(closestBb.minX + 0.5, closestBb.minY + 0.5, closestBb.minZ + 0.5))) {
                    closestBb = bb;
                }
            }
        } else {
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, MayOBeesConfig.fairySoulESPColor.toJavaColor());
            if (MayOBeesConfig.fairySoulESPTracers) {
                RenderUtils.drawTracer(new Vec3(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ), MayOBeesConfig.fairySoulESPColor.toJavaColor());
            }
            if (MayOBeesConfig.fairySoulESPShowDistance) {
                double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ)));
                RenderUtils.drawText(String.format("%.2fm", distance), entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ, 1);
            }
        }

        return closestBb;
    }

    private void giftEntityCheck(EntityArmorStand entityArmorStand) {
        ItemStack helmet = entityArmorStand.getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return;
        if (helmet.getTagCompound().toString().contains(GIFT_TEXTURES[0]) || helmet.getTagCompound().toString().contains(GIFT_TEXTURES[1]) || helmet.getTagCompound().toString().contains(GIFT_TEXTURES[2])) {
            if (clickedGifts.contains(entityArmorStand.getPosition())) return;
            AxisAlignedBB bb = new AxisAlignedBB(entityArmorStand.posX - 0.5, entityArmorStand.posY + entityArmorStand.getEyeHeight() - 0.5, entityArmorStand.posZ - 0.5, entityArmorStand.posX + 0.5, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ + 0.5).expand(0.002, 0.002, 0.002);
            bb = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
            RenderUtils.drawBox(bb, MayOBeesConfig.giftESPColor.toJavaColor());
            if (MayOBeesConfig.giftESPTracers) {
                RenderUtils.drawTracer(new Vec3(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ), MayOBeesConfig.giftESPColor.toJavaColor());
            }
            if (MayOBeesConfig.giftESPShowDistance) {
                double distance = Math.sqrt(mc.thePlayer.getDistanceSqToCenter(new BlockPos(entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight(), entityArmorStand.posZ)));
                RenderUtils.drawText(String.format("%.2fm", distance), entityArmorStand.posX, entityArmorStand.posY + entityArmorStand.getEyeHeight() + 0.5, entityArmorStand.posZ, 1);
            }
        }
    }

    @SubscribeEvent
    public void onLeftClick(ClickEvent.Left event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entity == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        fairySoulClick(event);
    }

    @SubscribeEvent
    public void onRightClick(ClickEvent.Right event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entity == null) return;
        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.PRIVATE_ISLAND)
            return;

        fairySoulClick(event);
        giftClick(event);
    }


    private void fairySoulClick(ClickEvent event) {
        if (!MayOBeesConfig.fairySoulESP) return;
        if (!(event.entity instanceof EntityArmorStand)) return;
        ItemStack helmet = ((EntityArmorStand) event.entity).getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return;
        if (helmet.getTagCompound().toString().contains(FAIRY_SOUL_TEXTURE)) {
            if (clickedFairySouls.stream().anyMatch(cfs -> cfs.getFirst().equals(GameStateHandler.getInstance().getLocation().getName()) && cfs.getSecond().equals(event.entity.getPosition())))
                return;
            clickedFairySouls.add(new Tuple<>(GameStateHandler.getInstance().getLocation().getName(), event.entity.getPosition()));
            saveClickedFairySouls();
        }
    }

    private void giftClick(ClickEvent event) {
        if (!MayOBeesConfig.giftESP) return;
        if (!(event.entity instanceof EntityArmorStand)) return;
        ItemStack helmet = ((EntityArmorStand) event.entity).getEquipmentInSlot(4);
        if (helmet == null || !helmet.hasTagCompound()) return;
        if (helmet.getTagCompound().toString().contains(GIFT_TEXTURES[0]) || helmet.getTagCompound().toString().contains(GIFT_TEXTURES[1]) || helmet.getTagCompound().toString().contains(GIFT_TEXTURES[2])) {
            if (clickedGifts.contains(event.entity.getPosition())) return;
            clickedGifts.add(event.entity.getPosition());
        }
    }
}
