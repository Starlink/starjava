package uk.ac.starlink.topcat.plot;

/**
 * Simple listener interface for registering surface change events
 * (typically zooms).
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Jun 2004
 */
public interface SurfaceListener {

    /**
     * Called when some change has happened to the plot surface.
     */
    void surfaceChanged();
}
