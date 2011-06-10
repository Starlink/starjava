package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.vo.TapQuery;

/**
 * Minimal synchronous implementation of TapRunner.
 *
 * @author   Mark Taylor
 * @since    10 Jun 2011
 */
public class BasicTapRunner extends TapRunner {

    /**
     * Constructor.
     */
    public BasicTapRunner() {
        super( "basic" );
    }

    @Override
    protected StarTable executeQuery( Reporter reporter, TapQuery tq )
            throws IOException {
        return tq.executeSync( StoragePolicy.getDefaultPolicy() );
    }
}
