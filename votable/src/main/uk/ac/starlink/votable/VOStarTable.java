package uk.ac.starlink.votable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnHeader;
import uk.ac.starlink.table.SequentialStarTable;

/**
 * A {@link uk.ac.starlink.table.StarTable} implementation based on a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOStarTable extends SequentialStarTable {

    private Table votable;
    private VOStarValueAdapter[] adapters;

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
     * Construct a VOStarTable from a VOTable <tt>Table</tt> object.
     *
     * @param  votable  the table object
     */
    public VOStarTable( Table votable ) {
        this.votable = votable;
        adapters = new VOStarValueAdapter[ votable.getColumnCount() ];
        for ( int i = 0; i < adapters.length; i++ ) {
            adapters[ i ] = VOStarValueAdapter
                           .makeAdapter( votable.getField( i ) );
        }
    }

    public int getColumnCount() {
        return votable.getColumnCount();
    }

    public long getRowCount() {
        return (long) votable.getRowCount();
    }

    protected Object[] getNextRow() {
        Object[] row = votable.nextRow();
        for ( int i = 0; i < row.length; i++ ) {
            row[ i ] = adapters[ i ].adapt( row[ i ] );
        }
        return row;
    }

    protected boolean hasNextRow() {
        return votable.hasNextRow();
    }

    public ColumnHeader getHeader( int icol ) {
        Field field = votable.getField( icol );
        ColumnHeader header = new ColumnHeader( field.getHandle() );
        header.setUnitString( field.getUnit() );
        header.setUCD( field.getUcd() );
        header.setDescription( field.getDescription() );
        header.setContentClass( adapters[ icol ].getContentClass() );

        /* Set up column metadata according to the attributes that the 
         * FIELD element has. */
        Map colmeta = header.getMetadata();
        String id = field.getAttribute( "ID" );
        if ( id != null ) {
            colmeta.put( ID_KEY, id );
        }

        String datatype = field.getAttribute( "datatype" );
        if ( datatype != null ) {
            colmeta.put( DATATYPE_KEY, datatype );
        }

        String arraysize = field.getAttribute( "arraysize" );
        if ( arraysize != null ) {
            colmeta.put( ARRAYSIZE_KEY, arraysize );
        }

        String width = field.getAttribute( "width" );
        if ( width != null ) {
            colmeta.put( WIDTH_KEY, width );
        }

        String precision = field.getAttribute( "precision" );
        if ( precision != null ) {
            colmeta.put( PRECISION_KEY, precision );
        }

        String type = field.getAttribute( "type" );
        if ( type != null ) {
            colmeta.put( TYPE_KEY, type );
        }

        return header;
    }

    public List getColumnMetadataKeys() {
        return metadataKeyList;
    }

}
