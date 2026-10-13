package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.module.visuals.esp.ESPColor;
import coolware.module.visuals.esp.PlayerChams;
import coolware.module.visuals.esp.PlayerGlow;
import coolware.module.visuals.esp.PlayerSkeleton;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class ESP extends Module {
    public final BooleanSetting glow = new BooleanSetting("Glow", this, false);
    public final BooleanSetting chams = new BooleanSetting("Chams", this, false);
    public final BooleanSetting staticColor = new BooleanSetting("Static Color", this, false);
    public final BooleanSetting skeletal = new BooleanSetting("Skeletal", this, false);
    public final BooleanSetting whiteColor = new BooleanSetting("White Color", this, false);
    public final NumberSetting width = new NumberSetting("Width", this, 0.5, 0.1, 1.5, 0.1);

    private final ESPColor espColor = new ESPColor(new Color(0xFFFF3344), Color.RED, new Color(0xFFFF3344));
    private final PlayerChams playerChams = new PlayerChams(espColor);
    private final PlayerGlow playerGlow = new PlayerGlow(espColor);
    private final PlayerSkeleton playerSkeleton = new PlayerSkeleton(espColor);

    public ESP() {
        super("ESP", "Highlights players and entities through walls (Rise 6.9.5)", Category.VISUALS, Keyboard.KEY_NONE);
        addSetting(glow);
        addSetting(chams);
        addSetting(staticColor);
        addSetting(skeletal);
        addSetting(whiteColor);
        addSetting(width);
    }

    private void updateColor() {
        Color primary = new Color(Theme.getInstance().getColor(0));
        espColor.setNormalColor(primary);
        espColor.setTargetColor(primary);
        espColor.setHurtColor(Color.RED);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (!isToggled() || mc.theWorld == null || mc.thePlayer == null) return;

        updateColor();

        if (chams.isEnabled()) {
            playerChams.render3D(partialTicks, staticColor.isEnabled());
        }

        if (glow.isEnabled()) {
            playerGlow.render3D(partialTicks);
        }

        if (skeletal.isEnabled()) {
            playerSkeleton.render3D(partialTicks, width.getValueFloat(), whiteColor.isEnabled());
        }
    }
}
