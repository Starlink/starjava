package uk.ac.starlink.treeview.votable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import uk.ac.starlink.table.ColumnHeader;
import uk.ac.starlink.table.StarTable;

/**
 * A StarTable implementation based on a VOTable which permits random
 * access to its cells.  This is not suitable for a table which has
 * an aribtrarily large number of rows.
 */
public class RandomVOStarTable implements StarTable {

    private Table votable;
    private List rowList = new ArrayList();
    private int nrow;
    private int ncol;
    private Number[] badvals;

    /* Metadata keys. */
    private final static String ID_KEY = "ID";
    private final static String DATATYPE_KEY = "Datatype";
    private final static String ARRAYSIZE_KEY = "Arraysize";
    private final static String WIDTH_KEY = "Width";
    private final static String PRECISION_KEY = "Precision";
    private final static String TYPE_KEY = "Type";
    private final static List metadataKeyList =
        Collections.unmodifiableList( Arrays.asList( new String[] {
            ID_KEY, DATATYPE_KEY, ARRAYSIZE_KEY, WIDTH_KEY, PRECISION_KEY,
            TYPE_KEY,
        } ) );

    public RandomVOStarTable( Table votable ) {
        this.votable = votable;

        /* Get the number of rows. */
        nrow = votable.getNumRows();
        if ( nrow < 0 ) {
            nrow = 0;
            while ( votable.hasNextRow() ) {
                getRow( nrow++ );
            }
        }

        /* Get the number of columns. */
        ncol = votable.getNumColumns();

        /* Set up bad values. */
        badvals = new Number[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            badvals[ i ] = votable.getField( i ).getDatatype().getNull();
        }
    }

    public int getNumColumns() {
        return ncol;
    }

    public int getNumRows() {
        return nrow;
    }

    public Object getCell( int irow, int icol ) {
        Object cell = getRow( irow )[ icol ];
        if ( cell.equals( badvals[ icol ] ) ) {
            cell = null;
        }
        return cell;
    }

    public ColumnHeader getHeader( int icol ) {
        Field field = votable.getField( icol );
        ColumnHeader header = new ColumnHeader( field.getHandle() );
        header.setUnitString( field.getUnit() );
        header.setUCD( field.getUcd() );
        header.setDescription( field.getDescription() );

        /* Set up column metadata according to the attributes that
         * the FIELD element has. */
        Element fieldEl = field.getElement();
        Map metadata = header.metadata();

        String id = fieldEl.getAttribute( "ID" );
        if ( id.length() > 0 ) {
            metadata.put( ID_KEY, id );
        }

        String datatype = fieldEl.getAttribute( "datatype" );
        if ( datatype.length() > 0 ) {
            metadata.put( DATATYPE_KEY, datatype );
        }

        String arraysize = fieldEl.getAttribute( "arraysize" );
        if ( arraysize.length() > 0 ) {
            metadata.put( ARRAYSIZE_KEY, arraysize );
        }

        String width = fieldEl.getAttribute( "width" );
        if ( width.length() > 0 ) {
            metadata.put( WIDTH_KEY, width );
        }

        String precision = fieldEl.getAttribute( "precision" );
        if ( precision.length() > 0 ) {
            metadata.put( PRECISION_KEY, precision );
        }

        String type = fieldEl.getAttribute( "type" );
        if ( type.length() > 0 ) {
            metadata.put( TYPE_KEY, type );
        }

        return header;
    }

    public List getColumnMetadataKeys() {
        return metadataKeyList;
    }

    private Object[] getRow( int irow ) {

        /* If we haven't filled up our row list far enough yet, do it now. */
        while ( rowList.size() <= irow ) {
            Object[] nextRow = votable.nextRow();
            rowList.add( nextRow );
        }
        return (Object[]) rowList.get( irow );
    }
 
}
