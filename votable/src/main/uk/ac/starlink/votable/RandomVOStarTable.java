package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnHeader;
import uk.ac.starlink.table.StarTable;

/**
 * A StarTable implementation based on a VOTable which permits random
 * access to its cells.  This is not suitable for a table which has
 * an aribtrarily large number of rows.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RandomVOStarTable implements StarTable {

    private Table votable;
    private RandomVOTable rtable;

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

    /**
     * Construct a RandomVOStarTable from a VOTable <tt>Table</tt> object.
     *
     * @param  votable the table object
     */
    public RandomVOStarTable( Table votable ) {
        this.votable = votable;
        this.rtable = new RandomVOTable( votable );
    }

    public int getColumnCount() {
        return rtable.getColumnCount();
    }

    public int getRowCount() {
        return rtable.getRowCount();
    }

    public Object getValueAt( int irow, int icol ) {
        return rtable.getValueAt( irow, icol );
    }

    public ColumnHeader getHeader( int icol ) {
        Field field = votable.getField( icol );
        ColumnHeader header = new ColumnHeader( field.getHandle() );
        header.setUnitString( field.getUnit() );
        header.setUCD( field.getUcd() );
        header.setDescription( field.getDescription() );

        /* Set up column metadata according to the attributes that
         * the FIELD element has. */
        Map metadata = header.getMetadata();

        String id = field.getAttribute( "ID" );
        if ( id != null ) {
            metadata.put( ID_KEY, id );
        }

        String datatype = field.getAttribute( "datatype" );
        if ( datatype != null ) {
            metadata.put( DATATYPE_KEY, datatype );
        }

        String arraysize = field.getAttribute( "arraysize" );
        if ( arraysize != null ) {
            metadata.put( ARRAYSIZE_KEY, arraysize );
        }

        String width = field.getAttribute( "width" );
        if ( width != null ) {
            metadata.put( WIDTH_KEY, width );
        }

        String precision = field.getAttribute( "precision" );
        if ( precision != null ) {
            metadata.put( PRECISION_KEY, precision );
        }

        String type = field.getAttribute( "type" );
        if ( type != null ) {
            metadata.put( TYPE_KEY, type );
        }

        return header;
    }

    public List getColumnMetadataKeys() {
        return metadataKeyList;
    }

}
