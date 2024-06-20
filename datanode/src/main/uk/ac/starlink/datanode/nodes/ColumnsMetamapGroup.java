package uk.ac.starlink.datanode.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.Tables;

/**
 * A MetamapGroup which describes the columns of a StarTable.
 *
 * @author  Mark Taylor (Starlink)
 */
public class ColumnsMetamapGroup extends ValueInfoMetamapGroup {

    public ColumnsMetamapGroup( StarTable startable ) {

        /* Superclass constructor. */
        super( Arrays.asList( Tables.getColumnInfos( startable ) ) );

        /* Set up the natural ordering for keys. */
        List order = getKeyOrder();
        for ( Iterator it = startable.getColumnAuxDataInfos().iterator();
              it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof ValueInfo ) {
                ValueInfo info = (ValueInfo) item;
                order.add( info.getName() );
            }
        }
        setKeyOrder( order );

        /* Add the metadata for each column. */
        int ncol = startable.getColumnCount();
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo colinfo = startable.getColumnInfo( i );
            addEntry( i, INDEX_KEY, Integer.valueOf( i + 1 ) );
            for ( Iterator it = colinfo.getAuxData().iterator();
                  it.hasNext(); ) {
                Object item = it.next();
                if ( item instanceof DescribedValue ) {
                    DescribedValue dval = (DescribedValue) item;
                    addEntry( i, dval.getInfo().getName(),
                              dval.getValueAsString( 250 ) );
                }
            }
        }
    }
}
