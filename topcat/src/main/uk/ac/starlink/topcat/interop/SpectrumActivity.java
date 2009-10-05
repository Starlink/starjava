package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.util.Map;

/**
 * Activity for sending a spectrum to load.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2009
 */
public interface SpectrumActivity extends Activity {

    /**
     * Sends a message to load a spectrum.
     *
     * @param   location   URL of spectrum
     * @param   metadata   ucd/utype->value map giving spectrum metadata
     */
    void displaySpectrum( String location, Map metadata ) throws IOException;
}
