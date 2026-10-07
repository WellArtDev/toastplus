package com.wellartdev.toastplus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Custom toast notifications with sounds for the moments that matter.
 *
 * <ul>
 *   <li><b>Welcome-back toast</b> when joining a world or server,
 *       with a warm chime.</li>
 *   <li><b>Death toast</b> the moment the player dies, with a dramatic
 *       (but quiet) wither rumble.</li>
 *   <li><b>Level-up fanfare</b> whenever the XP level increases, with
 *       the classic level-up jingle.</li>
 * </ul>
 *
 * Client-side only. No mixins, no networking, nothing persisted.
 */
public class ToastPlus implements ClientModInitializer {
    public static final String MOD_ID = "toastplus";

    /** How long each toast stays on screen, in milliseconds. */
    private static final long TOAST_DURATION_MS = 5000L;

    private int tick = 0;
    private boolean wasDead = false;
    private int lastLevel = -1;

    @Override
    public void onInitializeClient() {
        // Welcome-back toast once the client finishes joining a world/server.
        ClientPlayConnectionEvents.JOIN.register((listener, sender, mc) -> {
            if (mc.player == null) {
                return;
            }
            String name = mc.player.getName().getString();
            showToast(mc,
                    Component.literal("Welcome back, " + name + "!"),
                    Component.literal("Have a great adventure."),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f);
            // Fresh session: start the death/level trackers from a clean
            // state so a respawned player isn't treated as "just died".
            wasDead = false;
            lastLevel = mc.player.experienceLevel;
        });

        // Poll every 5 ticks for death and level-up events.
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        tick++;
        if (mc.player == null || tick % 5 != 0) {
            return;
        }

        // Death toast on the rising edge of the dead flag — fires once
        // per death instead of every tick while the death screen is up.
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead) {
            showToast(mc,
                    Component.literal("You died!"),
                    Component.literal("Better luck next time."),
                    SoundEvents.WITHER_SPAWN, 0.5f);
        }
        wasDead = dead;

        // Level-up fanfare whenever the XP level goes up.
        int level = mc.player.experienceLevel;
        if (lastLevel >= 0 && level > lastLevel) {
            showToast(mc,
                    Component.literal("Level up! Level " + level),
                    Component.literal("Time to enchant something."),
                    SoundEvents.PLAYER_LEVELUP, 1.0f);
        }
        lastLevel = level;
    }

    /** Pushes a toast onto the vanilla toast manager and plays its sound. */
    private void showToast(Minecraft mc, Component title, Component subtitle,
                           SoundEvent sound, float volume) {
        mc.gui.toastManager().addToast(new SimpleToast(title, subtitle, TOAST_DURATION_MS));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, volume));
    }

    /**
     * A minimal custom toast: dark vanilla-style box, a gold accent bar on
     * the left edge, title plus subtitle. No textures, drawn purely with
     * {@link GuiGraphicsExtractor} calls. Shows for a fixed duration, then
     * hides.
     */
    private static final class SimpleToast implements Toast {
        private static final int WIDTH = 160;
        private static final int HEIGHT = 32;

        private final Component title;
        private final Component subtitle;
        private final long durationMs;
        private final long createdAt = System.currentTimeMillis();
        private Visibility visibility = Visibility.SHOW;

        SimpleToast(Component title, Component subtitle, long durationMs) {
            this.title = title;
            this.subtitle = subtitle;
            this.durationMs = durationMs;
        }

        @Override
        public Visibility getWantedVisibility() {
            return visibility;
        }

        @Override
        public void update(ToastManager manager, long delta) {
            // Hide once the display window has elapsed.
            if (System.currentTimeMillis() - createdAt >= durationMs) {
                visibility = Visibility.HIDE;
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long delta) {
            // Dark vanilla-style box. Coordinates are local: (0, 0) is the
            // toast's top-left corner.
            graphics.fill(0, 0, WIDTH, HEIGHT, 0xFF212121);
            // Gold accent bar along the left edge.
            graphics.fill(0, 0, 4, HEIGHT, 0xFFFFAA00);
            // Title and subtitle in vanilla toast positions.
            graphics.text(font, title, 30, 7, 0xFFFFFFFF);
            graphics.text(font, subtitle, 30, 18, 0xFFAAAAAA);
        }

        @Override
        public int width() {
            return WIDTH;
        }

        @Override
        public int height() {
            return HEIGHT;
        }
    }
}
