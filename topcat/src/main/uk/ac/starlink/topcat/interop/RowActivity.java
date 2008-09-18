package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Activity for making single row selections.
 * 
 * @author   Mark Taylor
 * @since    17 Sep 2008
 */
public interface RowActivity extends Activity {

    /**
     * Sends a message to highlight a given table row.
     *
     * @param   tcModel   table in question
     * @param   lrow    zero-indexed row index within table to highlight
     */
    void highlightRow( TopcatModel tcModel, long lrow ) throws IOException;
}
