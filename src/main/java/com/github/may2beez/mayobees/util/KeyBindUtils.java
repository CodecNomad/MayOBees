package com.github.may2beez.mayobees.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class KeyBindUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final KeyBinding[] allKeys = {
            mc.gameSettings.keyBindAttack,
            mc.gameSettings.keyBindUseItem,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindJump,
            mc.gameSettings.keyBindSneak,
            mc.gameSettings.keyBindSprint,
    };

    public static final KeyBinding[] allKeys2 = {
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindJump,
    };

    public static void rightClick() {
        if (!ReflectionUtils.invoke(mc, "func_147121_ag")) {
            ReflectionUtils.invoke(mc, "rightClickMouse");
        }
    }

    public static void leftClick() {
        if (!ReflectionUtils.invoke(mc, "func_147116_af")) {
            ReflectionUtils.invoke(mc, "clickMouse");
        }
    }

    public static void middleClick() {
        if (!ReflectionUtils.invoke(mc, "func_147112_ai")) {
            ReflectionUtils.invoke(mc, "middleClickMouse");
        }
    }

    public static void onTick(KeyBinding key) {
        if (mc.currentScreen == null) {
            KeyBinding.onTick(key.getKeyCode());
        }
    }

    public static void updateKeys(boolean forward, boolean back, boolean right, boolean left, boolean attack) {
        updateKeys(forward, back, right, left, attack, false, false);
    }

    public static void setKeyBindState(KeyBinding key, boolean pressed) {
        if (pressed) {
            if (mc.currentScreen != null) {
                realSetKeyBindState(key, false);
                return;
            }
        }
        realSetKeyBindState(key, pressed);
    }

    private static void realSetKeyBindState(KeyBinding key, boolean pressed) {
        if (pressed) {
            if (!key.isKeyDown()) {
                KeyBinding.onTick(key.getKeyCode());
            }
            KeyBinding.setKeyBindState(key.getKeyCode(), true);

        } else {
            KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }

    }


    public static void updateKeys(boolean forward, boolean back, boolean right, boolean left, boolean attack, boolean crouch, boolean space) {
        if (mc.currentScreen != null) {
            stopMovement();
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), forward);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), back);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), right);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), left);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), attack);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), crouch);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), space);
    }

    public static void updateKeys(boolean forward, boolean back, boolean right, boolean left, boolean attack, boolean crouch, boolean space, boolean sprint) {
        if (mc.currentScreen != null) {
            stopMovement();
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), forward);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), back);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), right);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), left);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), attack);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), crouch);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), space);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), sprint);
    }

    public static void stopMovement() {
        stopMovement(false);
    }

    public static void stopMovement(boolean ignoreAttack) {
        if (mc.gameSettings.keyBindForward.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        if (mc.gameSettings.keyBindBack.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        if (mc.gameSettings.keyBindRight.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
        if (mc.gameSettings.keyBindLeft.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        if (!ignoreAttack) {
            if (mc.gameSettings.keyBindAttack.isKeyDown())
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        }
        if (mc.gameSettings.keyBindUseItem.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        if (mc.gameSettings.keyBindSneak.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        if (mc.gameSettings.keyBindJump.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        System.out.println(mc.gameSettings.keyBindSprint.isKeyDown());
        if (mc.gameSettings.keyBindSprint.isKeyDown())
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
    }

    public static void holdThese(KeyBinding... keyBinding) {
        releaseAllExcept(keyBinding);
        for (KeyBinding key : keyBinding) {
            if (key != null)
                KeyBinding.setKeyBindState(key.getKeyCode(), true);
        }
    }

    public static void releaseAllExcept(KeyBinding... keyBinding) {
        for (KeyBinding key : allKeys) {
            if (key != null && !contains(keyBinding, key))
                KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }
    }

    public static boolean contains(KeyBinding[] keyBinding, KeyBinding key) {
        for (KeyBinding keyBind : keyBinding) {
            if (keyBind == key)
                return true;
        }
        return false;
    }

    public static boolean areAllKeybindsReleased() {
        for (KeyBinding key : allKeys2) {
            if (key != null && key.isKeyDown())
                return false;
        }
        return true;
    }
}