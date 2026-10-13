package coolware;

import coolware.command.CommandManager;
import coolware.config.ConfigManager;
import coolware.font.FontManager;
import coolware.gui.clickgui.ClickGuiScreen;
import coolware.gui.hud.HUDOverlay;
import coolware.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class Coolware {
    public static final String NAME = "Coolware";
    public static final String VERSION = "2.0";

    private static Coolware instance;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ClickGuiScreen clickGuiScreen;
    private coolware.util.shader.ShaderRenderManager shaderRenderManager;

    public static Coolware getInstance() {
        if (instance == null) {
            instance = new Coolware();
        }
        return instance;
    }

    public void init() {
        System.out.println("[" + NAME + "] Initializing " + NAME + " v" + VERSION + "...");
        FontManager.init();
        this.shaderRenderManager = new coolware.util.shader.ShaderRenderManager();
        this.moduleManager = new ModuleManager();
        this.commandManager = new CommandManager();
        this.clickGuiScreen = new ClickGuiScreen();
        // Load default configuration if present
        ConfigManager.loadConfig(ConfigManager.DEFAULT_CONFIG);
        System.out.println("[" + NAME + "] Loaded " + moduleManager.getModules().size() + " modules.");
    }

    public void onKeyPress(int key) {
        if (key == Keyboard.KEY_RSHIFT || key == 54) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) {
                if (clickGuiScreen == null) {
                    clickGuiScreen = new ClickGuiScreen();
                }
                mc.displayGuiScreen(clickGuiScreen);
            }
            return;
        }

        if (moduleManager != null) {
            moduleManager.onKeyPress(key);
        }
    }

    public void onTick() {
        coolware.util.MovementUtil.onClientTick();
        if (moduleManager != null) {
            moduleManager.onTick();
        }
    }

    public void onRender2D() {
        HUDOverlay.render();

        if (moduleManager != null) {
            moduleManager.onRender2D();
        }
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ClickGuiScreen getClickGuiScreen() {
        return clickGuiScreen;
    }

    public coolware.util.shader.ShaderRenderManager getShaderRenderManager() {
        return shaderRenderManager;
    }
}
