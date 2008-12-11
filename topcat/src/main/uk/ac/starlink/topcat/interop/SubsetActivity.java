package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Activity for making multiple row selections.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2008
 */
public interface SubsetActivity extends Activity {

    /**
     * Sends a message to select a given sequence of rows.
     *
     * @param   tcModel  table in question
     * @param   rset   row subset
     */
    void selectSubset( TopcatModel tcModel, RowSubset rset ) throws IOException;
}
