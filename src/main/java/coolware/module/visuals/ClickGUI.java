package coolware.module.visuals;

import coolware.Coolware;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.NumberSetting;
import org.lwjgl.input.Keyboard;

public class ClickGUI extends Module {
    private final NumberSetting fontSizeSetting = new NumberSetting("Font Size", this, 8.0, 6.0, 12.0, 1.0);

    public ClickGUI() {
        super("ClickGUI", "Displays the graphical module menu", Category.CLIENT, Keyboard.KEY_RSHIFT);
        addSetting(fontSizeSetting);
    }

    public int getFontSize() {
        return fontSizeSetting.getValueInt();
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.displayGuiScreen(Coolware.getInstance().getClickGuiScreen());
        }
        setToggled(false);
    }
}
