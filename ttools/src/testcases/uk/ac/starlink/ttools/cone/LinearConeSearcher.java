package uk.ac.starlink.ttools.cone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;

/**
 * Test ConeSearcher implementation.  Each cone search returns objects 
 * at the same
 * RA as the request but at a variety of Decs.  A fixed number of results
 * within and without the area is returned.
 */
public class LinearConeSearcher implements ConeSearcher {

    private final int nIn_;
    private final int nOut_;

    private static final ValueInfo ID_INFO =
         new DefaultValueInfo( "ID", Integer.class );
    private static final ValueInfo RA_INFO =
         new DefaultValueInfo( "RA", Double.class, null );
    private static final ValueInfo DEC_INFO =
         new DefaultValueInfo( "Dec", Double.class, null );

    /**
     * Constructor.
     *
     * @param  nIn  number of results at each point within search radius
     * @param  nOut number of results at each point without search radius
     *              (this is permitted by contract)
     */
    public LinearConeSearcher( int nIn, int nOut ) {
        nIn_ = nIn;
        nOut_ = nOut;
    }

    public int getRaIndex( StarTable result ) {
        return 1;
    }

    public int getDecIndex( StarTable result ) {
        return 2;
    }

    public StarTable performSearch( double ra, double dec, double sr ) {
        List rowList = new ArrayList();
        for ( int i = 0; i < nIn_; i++ ) {
            rowList.add( new Object[] { Integer.valueOf( i + 1 ),
                                        Double.valueOf( ra ),
                                        Double.valueOf( dec + sr * i / nIn_ )});
        }
        for ( int i = 0; i < nOut_; i++ ) {
            rowList.add( new Object[] { Integer.valueOf( - i - 1 ),
                                        Double.valueOf( ra ),
                                        Double.valueOf( dec + sr * 1.01
                                                      + sr * i / nOut_ ) } );
        }
        Collections.shuffle( rowList, new Random( 11223344556677L ) );

        RowListStarTable table =
            new RowListStarTable( new ColumnInfo[] {
                                      new ColumnInfo( ID_INFO ),
                                      new ColumnInfo( RA_INFO ),
                                      new ColumnInfo( DEC_INFO ), } ); 
        for ( Iterator it = rowList.iterator(); it.hasNext(); ) {
            table.addRow( (Object[]) it.next() );
        }
        return table;
    }

    public void close() {
    }
}
