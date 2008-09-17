package uk.ac.starlink.topcat.interop;

import java.io.IOException;

/**
 * Activity for indicating a point on the sky.
 *
 * @author   Mark Taylor
 * @since    17 Sep 2008
 */
public interface SkyPointActivity extends Activity {

    /**
     * Sends a message to point at a given sky position.
     *
     * @param   ra  right ascension in degrees
     * @param   dec  declination in degrees
     */
    void pointAtSky( double ra, double dec ) throws IOException;
}
