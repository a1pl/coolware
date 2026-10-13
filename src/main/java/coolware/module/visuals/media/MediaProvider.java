package coolware.module.visuals.media;

public interface MediaProvider {
    MediaState get(); // returns null if nothing is playing; must be fast and non-blocking
}
