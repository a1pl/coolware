package coolware.module.visuals.media;

import java.awt.image.BufferedImage;

public class MediaState {
    public String title;
    public String artist;
    public BufferedImage cover; // may be null
    public long positionMs;
    public long durationMs;
    public boolean playing;
    public long updatedAtMs; // System.currentTimeMillis() when positionMs was read
}
