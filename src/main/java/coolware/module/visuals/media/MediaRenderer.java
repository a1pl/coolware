package coolware.module.visuals.media;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class MediaRenderer {
    private static final int W = 272, H = 76;
    private final Minecraft mc = Minecraft.getMinecraft();
    private MediaProvider provider;

    private BufferedImage lastCover;
    private DynamicTexture coverTex;
    private ResourceLocation coverLoc;

    public MediaRenderer(MediaProvider provider) { this.provider = provider; }

    public void render(float x, float y) {
        MediaState s = provider.get();
        if (s == null || s.title == null) return;
        FontRenderer fr = mc.fontRendererObj;
        updateCover(s.cover);

        long pos = s.positionMs;
        if (s.playing) pos += System.currentTimeMillis() - s.updatedAtMs;
        pos = Math.max(0, Math.min(pos, s.durationMs));
        float progress = s.durationMs > 0 ? (float) pos / s.durationMs : 0f;

        roundedRect(x - 1, y - 1, W + 2, H + 2, 13, 0x14FFFFFF);
        roundedRect(x, y, W, H, 12, 0x940E1016);

        if (coverLoc != null) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(coverLoc);
            Gui.drawModalRectWithCustomSizedTexture((int) x + 10, (int) y + 10, 0, 0, 56, 56, 56, 56);
        } else {
            roundedRect(x + 10, y + 10, 56, 56, 8, 0xFF2B1B3D);
        }

        float tx = x + 78, maxW = 180;
        drawScaled(fr, fit(fr, "\u00a7l" + s.title, 1.3f, maxW), tx, y + 12, 1.3f, 0xFFFFFFFF);
        drawScaled(fr, fit(fr, s.artist == null ? "" : s.artist, 1.1f, maxW), tx, y + 29, 1.1f, 0xA8FFFFFF);

        String cur = fmt(pos), dur = fmt(s.durationMs);
        float timeW = fr.getStringWidth(dur) * 0.9f;
        drawScaled(fr, cur, tx, y + 53, 0.9f, 0xA8FFFFFF);
        drawScaled(fr, dur, x + 258 - fr.getStringWidth(dur) * 0.9f, y + 53, 0.9f, 0xA8FFFFFF);

        float bx = tx + timeW + 7, bw = (x + 258) - timeW - 7 - bx;
        roundedRect(bx, y + 55, bw, 4, 2, 0x2EFFFFFF);
        float fw = bw * progress;
        if (fw >= 1f) roundedRect(bx, y + 55, fw, 4, Math.min(2f, fw / 2f), 0xFFFF5D8F);

        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawScaled(FontRenderer fr, String t, float x, float y, float sc, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(sc, sc, 1f);
        fr.drawString(t, x / sc, y / sc, color, false);
        GlStateManager.popMatrix();
    }

    private String fit(FontRenderer fr, String t, float sc, float maxW) {
        if (fr.getStringWidth(t) * sc <= maxW) return t;
        while (t.length() > 1 && fr.getStringWidth(t + "...") * sc > maxW) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    private static String fmt(long ms) {
        long sec = Math.max(0, ms / 1000);
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }

    private void updateCover(BufferedImage cover) {
        if (cover == lastCover) return;
        lastCover = cover;
        if (coverTex != null) { coverTex.deleteGlTexture(); coverTex = null; coverLoc = null; }
        if (cover == null) return;
        coverTex = new DynamicTexture(prepare(cover));
        coverLoc = mc.getTextureManager().getDynamicTextureLocation("mediacover", coverTex);
        GlStateManager.bindTexture(coverTex.getGlTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    // center-crop to square, resize to 112x112, round corners (radius 16 = 8 GUI px at 2x)
    private static BufferedImage prepare(BufferedImage src) {
        int n = 112;
        BufferedImage out = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.fill(new RoundRectangle2D.Float(0, 0, n, n, 32, 32));
        g.setComposite(AlphaComposite.SrcIn);
        int side = Math.min(src.getWidth(), src.getHeight());
        int sx = (src.getWidth() - side) / 2, sy = (src.getHeight() - side) / 2;
        g.drawImage(src, 0, 0, n, n, sx, sy, sx + side, sy + side, null);
        g.dispose();
        return out;
    }

    public static void roundedRect(float x, float y, float w, float h, float r, int color) {
        r = Math.min(r, Math.min(w, h) / 2f);
        float a = (color >> 24 & 255) / 255f, rr = (color >> 16 & 255) / 255f,
              gg = (color >> 8 & 255) / 255f, bb = (color & 255) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(rr, gg, bb, a);
        GL11.glBegin(GL11.GL_POLYGON);
        float[][] c = {{x + r, y + r, 180}, {x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}};
        for (float[] k : c) {
            for (int i = 0; i <= 12; i++) {
                double t = Math.toRadians(k[2] + i * 90.0 / 12.0);
                GL11.glVertex2d(k[0] + Math.cos(t) * r, k[1] + Math.sin(t) * r);
            }
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
