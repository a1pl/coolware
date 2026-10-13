package coolware.module.visuals.media;

public class FakeMediaProvider implements MediaProvider {
    private final MediaState state;

    public FakeMediaProvider() {
        // TODO: Replace with real Windows media (SMTC) provider code
        state = new MediaState();
        state.title = "Houdini";
        state.artist = "Dua Lipa";
        state.durationMs = 185000L;
        state.positionMs = 72000L;
        state.playing = true;
        state.updatedAtMs = System.currentTimeMillis();
        state.cover = null;
    }

    @Override
    public MediaState get() {
        return state;
    }
}
