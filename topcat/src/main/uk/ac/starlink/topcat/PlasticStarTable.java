package uk.ac.starlink.topcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private final StarTable baseTable;

    /**
     * Constructs a <code>PlasticStarTable</code> based on an existing 
     * <code>StarTable</code> object.  The metadata are copied from the base
     * table and ColumnData objects constructed to wrap each of its
     * columns.  
     *
     * @param  baseTable  the table to initialise this one from
     */
    @SuppressWarnings("this-escape")
    public PlasticStarTable( final StarTable baseTable ) {
        super( baseTable );
        this.baseTable = baseTable;

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

        /* Clone table metadata so that changes to this table don't affect
         * derived tables. */
        List<DescribedValue> paramList = new ArrayList<DescribedValue>();
        for ( DescribedValue dval : baseTable.getParameters() ) {
            Object value = dval.getValue();
            ValueInfo info = dval.getInfo();
            if ( info instanceof DefaultValueInfo ) {
                final ValueInfo info0 = info;
                info = new DefaultValueInfo( info0 ) {
                    @Override
                    public String formatValue( Object value, int maxLeng ) {
                        return info0.formatValue( value, maxLeng );
                    }
                    @Override
                    public Object unformatString( String rep ) {
                        return info0.unformatString( rep );
                    }
                };
            }
            paramList.add( new DescribedValue( info, value ) );
        }
        setParameters( paramList );

        /* Set up ColumnData objects for each of the columns in the
         * given StarTable. */
        for ( int icol = 0; icol < baseTable.getColumnCount(); icol++ ) {
            ColumnInfo colinfo =
                new ColumnInfo( baseTable.getColumnInfo( icol ) );
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
        String colid = TopcatJELRowReader.COLUMN_ID_CHAR + 
                       Integer.toString( getColumnCount() + 1 );
        coldata.getColumnInfo()
               .setAuxDatum( new DescribedValue( TopcatUtils.COLID_INFO,
                                                 colid ) );
        super.addColumn( coldata );
    }

    public void setColumn( int icol, ColumnData coldata ) {
        String colid = TopcatJELRowReader.COLUMN_ID_CHAR +
                       Integer.toString( icol + 1 );
        coldata.getColumnInfo()
               .setAuxDatum( new DescribedValue( TopcatUtils.COLID_INFO,
                                                 colid ) );
        super.setColumn( icol, coldata );
    }

    /**
     * Returns the StarTable table on which this PlasticStarTable is based.
     *
     * @return  base table
     */
    public StarTable getBaseTable() {
        return baseTable;
    }
}

