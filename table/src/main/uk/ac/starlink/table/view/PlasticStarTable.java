package uk.ac.starlink.table.view;

import java.io.IOException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * A StarTable which is initialised from an existing table but can have
 * columns added to it.  Currently, this table cannot have its number
 * of rows changed.
 */
public class PlasticStarTable extends ColumnStarTable {

    private long nrow;

    public static final ValueInfo COLID_INFO = 
        new DefaultValueInfo( "$ID", String.class, "Unique column ID" );

    /**
     * Constructs a <tt>PlasticStarTable</tt> based on an existing 
     * <tt>StarTable</tt> object.  The metadata are copied from the base
     * table and ColumnData objects constructed to wrap each of its
     * columns.  
     *
     * @param  baseTable  the table to initialise this one from
     */
    public PlasticStarTable( final StarTable baseTable ) {

        /* Ensure that we have a random access table to use. */
        if ( ! baseTable.isRandom() ) {
            throw new IllegalArgumentException(
                "Table " + baseTable + " does not have random access" );
        }
        nrow = baseTable.getRowCount();
        if ( nrow < 0 ) {
            throw new IllegalArgumentException(
                "Random table has negative number of rows " + nrow );
        }

        /* Copy metadata. */
        setName( baseTable.getName() );
        setParameters( baseTable.getParameters() );

        /* Set up ColumnData objects for each of the columns in the
         * given StarTable. */
        for ( int icol = 0; icol < baseTable.getColumnCount(); icol++ ) {
            ColumnInfo colinfo = baseTable.getColumnInfo( icol );
            final int ficol = icol;
            ColumnData coldat = new ColumnData( colinfo ) {
                public Object readValue( long lrow ) throws IOException {
                    return baseTable.getCell( lrow, ficol );
                }
            };
            addColumn( coldat );
        }
    }

    public long getRowCount() {
        return nrow;
    }

    public void addColumn( ColumnData coldata ) {
        String colid = "$" + ( getColumnCount() + 1 );
        coldata.getColumnInfo()
               .setAuxDatum( new DescribedValue( COLID_INFO, colid ) );
        super.addColumn( coldata );
    }
}

