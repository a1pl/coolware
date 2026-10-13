package coolware.module.visuals;

import coolware.module.Category;
import coolware.module.Module;
import coolware.module.visuals.media.FakeMediaProvider;
import coolware.module.visuals.media.MediaProvider;
import coolware.module.visuals.media.MediaRenderer;
import org.lwjgl.input.Keyboard;

public class MediaInfoModule extends Module {
    public float x = 16.0f;
    public float y = 16.0f;

    private final MediaProvider provider;
    private final MediaRenderer renderer;

    public MediaInfoModule() {
        super("Media Info", "Displays currently playing media track on the HUD", Category.VISUALS, Keyboard.KEY_NONE);
        this.provider = new FakeMediaProvider();
        this.renderer = new MediaRenderer(this.provider);
        this.setToggled(true);
    }

    @Override
    public void onRender2D() {
        renderer.render(x, y);
    }
}
