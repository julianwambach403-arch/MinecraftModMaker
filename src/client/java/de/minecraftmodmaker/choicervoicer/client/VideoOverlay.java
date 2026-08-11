package de.minecraftmodmaker.choicervoicer.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

final class VideoOverlay implements AutoCloseable {
    private static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath("choicer_voicer", "dub_video");

    private final FfmpegVideoDecoder decoder;
    private final long durationMillis;
    private final long startedAt = System.currentTimeMillis();
    private final DynamicTexture texture;
    private final ByteBuffer uploadBuffer;
    private boolean closed;
    private boolean sawFrame;
    private String failure;

    VideoOverlay(FfmpegVideoDecoder decoder, long durationMillis) {
        this.decoder = decoder;
        this.durationMillis = durationMillis;
        this.texture = new DynamicTexture("Choicer Voicer video",
                FfmpegVideoDecoder.WIDTH, FfmpegVideoDecoder.HEIGHT, false);
        this.uploadBuffer = MemoryUtil.memAlloc(FfmpegVideoDecoder.frameBytes());
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
    }

    boolean tick() {
        if (closed) {
            return false;
        }
        String decoderError = decoder.errorMessage();
        if (decoderError != null && !sawFrame) {
            failure = decoderError;
            return false;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        if (durationMillis > 0L && elapsed >= durationMillis + 750L) {
            return false;
        }
        if (decoder.finished() && elapsed >= 750L) {
            if (!sawFrame) {
                failure = decoderError != null ? decoderError
                        : "Kein Videobild empfangen. Ist FFmpeg installiert und im PATH?";
            }
            return false;
        }
        return true;
    }

    void render(GuiGraphicsExtractor graphics) {
        if (closed) {
            return;
        }
        byte[] frame = decoder.takeLatestFrame();
        if (frame != null) {
            upload(frame);
            sawFrame = true;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0xFF000000);
        double scale = Math.min(width / (double) FfmpegVideoDecoder.WIDTH,
                height / (double) FfmpegVideoDecoder.HEIGHT);
        int drawWidth = Math.max(1, (int) Math.round(FfmpegVideoDecoder.WIDTH * scale));
        int drawHeight = Math.max(1, (int) Math.round(FfmpegVideoDecoder.HEIGHT * scale));
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        // 12-arg blit: draw size + source UV size + texture size (10-arg wrongly uses draw size as UV).
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0F, 0F,
                drawWidth, drawHeight,
                FfmpegVideoDecoder.WIDTH, FfmpegVideoDecoder.HEIGHT,
                FfmpegVideoDecoder.WIDTH, FfmpegVideoDecoder.HEIGHT);
    }

    String failure() {
        return failure;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        decoder.close();
        MemoryUtil.memFree(uploadBuffer);
        Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
    }

    private void upload(byte[] frame) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null || frame.length != FfmpegVideoDecoder.frameBytes()) {
            return;
        }
        uploadBuffer.clear();
        uploadBuffer.put(frame);
        uploadBuffer.flip();
        MemoryUtil.memCopy(MemoryUtil.memAddress(uploadBuffer), pixels.getPointer(), frame.length);
        texture.upload();
    }
}
