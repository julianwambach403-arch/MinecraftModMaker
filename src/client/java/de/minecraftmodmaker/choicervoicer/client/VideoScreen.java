package de.minecraftmodmaker.choicervoicer.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class VideoScreen extends Screen {
    private static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath("choicer_voicer", "dub_video");

    private final FfmpegVideoDecoder decoder;
    private final long durationMillis;
    private final long startedAt = System.currentTimeMillis();
    private final DynamicTexture texture;
    private boolean closed;

    VideoScreen(FfmpegVideoDecoder decoder, long durationMillis) {
        super(Component.literal("Choicer Voicer Dub"));
        this.decoder = decoder;
        this.durationMillis = durationMillis;
        this.texture = new DynamicTexture("Choicer Voicer video",
                FfmpegVideoDecoder.WIDTH, FfmpegVideoDecoder.HEIGHT, false);
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
    }

    @Override
    public void tick() {
        if (decoder.ended() || durationMillis > 0L
                && System.currentTimeMillis() - startedAt >= durationMillis + 500L) {
            onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        byte[] frame = decoder.takeLatestFrame();
        if (frame != null) {
            upload(frame);
        }
        graphics.fill(0, 0, width, height, 0xFF000000);
        double scale = Math.min(width / (double) FfmpegVideoDecoder.WIDTH,
                height / (double) FfmpegVideoDecoder.HEIGHT);
        int drawWidth = Math.max(1, (int) Math.round(FfmpegVideoDecoder.WIDTH * scale));
        int drawHeight = Math.max(1, (int) Math.round(FfmpegVideoDecoder.HEIGHT * scale));
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0F, 0F,
                drawWidth, drawHeight, FfmpegVideoDecoder.WIDTH, FfmpegVideoDecoder.HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closeVideo();
        if (minecraft != null && minecraft.screen == this) {
            minecraft.setScreen(null);
        }
    }

    void closeVideo() {
        if (closed) {
            return;
        }
        closed = true;
        decoder.close();
        Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
    }

    private void upload(byte[] frame) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        for (int y = 0; y < FfmpegVideoDecoder.HEIGHT; y++) {
            for (int x = 0; x < FfmpegVideoDecoder.WIDTH; x++) {
                int offset = (y * FfmpegVideoDecoder.WIDTH + x) * 4;
                int red = frame[offset] & 0xFF;
                int green = frame[offset + 1] & 0xFF;
                int blue = frame[offset + 2] & 0xFF;
                int alpha = frame[offset + 3] & 0xFF;
                pixels.setPixelABGR(x, y, alpha << 24 | blue << 16 | green << 8 | red);
            }
        }
        texture.upload();
    }
}
