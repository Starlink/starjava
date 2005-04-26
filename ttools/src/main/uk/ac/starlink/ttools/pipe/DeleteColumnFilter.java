package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;

/**
 * Table filter for deleting a single column.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public class DeleteColumnFilter implements ProcessingFilter {

    public String getName() {
        return "delcols";
    }

    public String getFilterUsage() {
        return "<colid-list>";
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        if ( argIt.hasNext() ) {
            String colIdList = (String) argIt.next();
            argIt.remove();
            return new DeleteColumnStep( colIdList.split( "\\s+" ) );
        }
        else {
            throw new ArgException( "Missing column list" );
        }
    }

    private static class DeleteColumnStep implements ProcessingStep {
        final String[] colIds_;

        DeleteColumnStep( String[] colIds ) {
            colIds_ = colIds;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            ColumnIdentifier identifier = new ColumnIdentifier( base );
            BitSet keep = new BitSet();
            keep.set( 0, base.getColumnCount() );
            for ( int i = 0; i < colIds_.length; i++ ) {
                keep.clear( identifier.getColumnIndex( colIds_[ i ] ) );
            }
            int[] colMap = new int[ keep.cardinality() ];
            int j = 0;
            for ( int i = keep.nextSetBit( 0 ); i >= 0; 
                  i = keep.nextSetBit( i + 1 ) ) {
                colMap[ j++ ] = i;
            }
            assert j == colMap.length;
            return new ColumnPermutedStarTable( base, colMap );
        }
    }
}
