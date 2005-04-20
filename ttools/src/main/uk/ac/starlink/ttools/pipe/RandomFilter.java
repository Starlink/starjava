package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Processing filter that makes sure any RowSequence taken out on a
 * table uses calls to random table access methods.  This will obviously
 * cause an error if the table being filtered does not have random access.
 * Only really likely to be used for debugging purposes.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class RandomFilter implements ProcessingFilter, ProcessingStep {

    public String getName() {
        return "random";
    }

    public String getFilterUsage() {
        return null;
    }

    public ProcessingStep createStep( Iterator argIt ) {
        return this;
    }

    public StarTable wrap( final StarTable base ) throws IOException {
        if ( base.isRandom() ) {
            return new WrapperStarTable( base ) {
                public RowSequence getRowSequence() {
                    return new RandomRowSequence( base );
                }
            };
        }
        else {
            throw new IOException( "Table is not random" );
        }
    }

}
