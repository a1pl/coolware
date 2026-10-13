package coolware.module.visuals;

import coolware.config.ConfigManager;
import coolware.font.moonlight.Fonts;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class SessionInfo extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Cool", "Cool");
    private final NumberSetting posX = new NumberSetting("Position X", this, 10.0D, 0.0D, 4000.0D, 1.0D);
    private final NumberSetting posY = new NumberSetting("Position Y", this, 80.0D, 0.0D, 4000.0D, 1.0D);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", this, "Theme", "Theme", "Custom", "Rainbow");
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", this, 120.0D, 0.0D, 255.0D, 1.0D);

    public int killed = 0;
    public int won = 0;
    public int lost = 0;
    public int match = 0;
    public int matchKilled = 0;
    public int prevMatchKilled = 0;

    private float renderedX;
    private float renderedY;
    private float renderedWidth;
    private float renderedHeight;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean positionChanged;

    private float smoothX = -1;
    private float smoothY = -1;
    private long lastAnimTime = System.currentTimeMillis();

    public SessionInfo() {
        super("SessionInfo", "Displays session statistics and match information", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(mode);
        addSetting(posX);
        addSetting(posY);
        addSetting(colorMode);
        addSetting(backgroundAlpha);
        setToggled(true);
    }

    public void onChatPacket(S02PacketChat packet) {
        if (packet == null || packet.getChatComponent() == null || mc.thePlayer == null) return;
        String text = packet.getChatComponent().getUnformattedText();
        if (text.contains("was killed by " + mc.thePlayer.getName())) {
            ++this.killed;
            prevMatchKilled = matchKilled;
            ++matchKilled;
        }
        if (text.contains("You Died! Want to play again?")) {
            ++lost;
        }
    }

    public void onTitlePacket(S45PacketTitle packet) {
        if (packet == null || packet.getType() != S45PacketTitle.Type.TITLE || packet.getMessage() == null) return;
        String text = packet.getMessage().getUnformattedText();
        if (text.contains("VICTORY!")) {
            ++this.won;
        }
        if (text.contains("GAME OVER!") || text.contains("DEFEAT!") || text.contains("YOU DIED!")) {
            ++this.lost;
        }
    }

    public void onWorldChange() {
        prevMatchKilled = matchKilled;
        matchKilled = 0;
        match += 1;
        if (match > 6) {
            match = 6;
        }
    }

    public int getColor(int counter) {
        if ("Rainbow".equalsIgnoreCase(colorMode.getMode())) {
            double rainbowState = Math.ceil((System.currentTimeMillis() - (long) counter * 20L) / 8.0);
            rainbowState %= 360.0;
            return Color.HSBtoRGB((float) (rainbowState / 360.0), 0.8F, 1.0F);
        }
        coolware.module.visuals.Theme theme = coolware.module.visuals.Theme.getInstance();
        return theme != null ? theme.getColor(counter) : 0xFF55AAFF;
    }

    public int getBgColor() {
        int alpha = (int) backgroundAlpha.getValue();
        return new Color(21, 21, 24, Math.max(0, Math.min(255, alpha))).getRGB();
    }

    @Override
    public void onRender2D() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float targetX = posX.getValueFloat();
        float targetY = posY.getValueFloat();

        if (smoothX < 0 || smoothY < 0) {
            smoothX = targetX;
            smoothY = targetY;
            lastAnimTime = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            float delta = Math.min(1.0F, (now - lastAnimTime) / 1000.0F);
            lastAnimTime = now;
            float factor = 1.0F - (float) Math.pow(0.0001, delta);
            smoothX += (targetX - smoothX) * factor;
            smoothY += (targetY - smoothY) * factor;
        }

        float renderX = smoothX;
        float renderY = smoothY;
        float width = 140;
        float height = Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight() * 3 + 3;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // "Cool" style (Novo 2 mode)
        RenderUtil.drawRect(renderX, renderY, renderX + width, renderY + height, getBgColor());
        RenderUtil.drawHorizontalGradientSideways(renderX, renderY, width, 1, getColor(0), getColor(90));

        float left = renderX + 2;
        float right = renderX + width - 2;
        float textY = renderY + 4;

        Fonts.interSemiBold.get(20).drawStringWithShadow("Current Session", left, textY, -1);

        Fonts.sfui.get(18).drawStringWithShadow("Play Time:", left, textY + Fonts.interSemiBold.get(20).getHeight(), -1);
        Fonts.sfui.get(18).drawStringWithShadow(RenderUtil.sessionTime(), right - Fonts.sfui.get(18).getStringWidth(RenderUtil.sessionTime()), textY + Fonts.interSemiBold.get(20).getHeight(), -1);

        Fonts.sfui.get(18).drawStringWithShadow("Games Won:", left, textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight(), -1);
        Fonts.sfui.get(18).drawStringWithShadow(won + "", right - Fonts.sfui.get(18).getStringWidth(won + ""), textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight(), -1);

        Fonts.sfui.get(18).drawStringWithShadow("Players Killed:", left, textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight() * 2, -1);
        Fonts.sfui.get(18).drawStringWithShadow(killed + "", right - Fonts.sfui.get(18).getStringWidth(killed + ""), textY + Fonts.interSemiBold.get(20).getHeight() + Fonts.sfui.get(18).getHeight() * 2, -1);

        renderedX = renderX;
        renderedY = renderY;
        renderedWidth = width;
        renderedHeight = height;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public boolean beginDrag(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !isToggled()) {
            return false;
        }
        if (mouseX >= renderedX && mouseX <= renderedX + renderedWidth &&
                mouseY >= renderedY && mouseY <= renderedY + renderedHeight) {
            dragging = true;
            dragOffsetX = mouseX - posX.getValueFloat();
            dragOffsetY = mouseY - posY.getValueFloat();
            positionChanged = false;
            return true;
        }
        return false;
    }

    public boolean drag(int mouseX, int mouseY, int mouseButton) {
        if (!dragging || mouseButton != 0) {
            return false;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        float newX = mouseX - dragOffsetX;
        float newY = mouseY - dragOffsetY;
        newX = Math.max(0.0F, Math.min(newX, resolution.getScaledWidth() - renderedWidth));
        newY = Math.max(0.0F, Math.min(newY, resolution.getScaledHeight() - renderedHeight));
        posX.setValue(newX);
        posY.setValue(newY);
        positionChanged = true;
        return true;
    }

    public boolean endDrag(int mouseButton) {
        if (!dragging || mouseButton != 0) {
            return false;
        }
        dragging = false;
        if (positionChanged) {
            ConfigManager.saveConfig(ConfigManager.DEFAULT_CONFIG);
            positionChanged = false;
        }
        return true;
    }

    public ModeSetting getMode() {
        return mode;
    }
}
