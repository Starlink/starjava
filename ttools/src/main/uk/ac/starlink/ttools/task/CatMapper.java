package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.ConstantColumn;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * TableMapper which concatenates tables top to bottom.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2006
 */
public class CatMapper implements TableMapper {

    private final Parameter seqParam_;
    private final Parameter locParam_;
    private final Parameter ulocParam_;
    private MapperTask task_;

    private static final ValueInfo SEQ_INFO =
        new DefaultValueInfo( "iseq", Short.class,
                              "Sequence number of input table " +
                              "from concatenation operation" );
    private static final ValueInfo LOC_INFO =
        new DefaultValueInfo( "loc", String.class,
                              "Location of input table " +
                              "from concatenation operation" );
    private static final ValueInfo ULOC_INFO =
        new DefaultValueInfo( "uloc", String.class,
                              "Unique part of input table location " +
                              "from concatenation operation" );

    /**
     * Constructor.
     */
    public CatMapper() {
        seqParam_ = new Parameter( "seqcol" );
        seqParam_.setUsage( "<colname>" );
        seqParam_.setNullPermitted( true );
        seqParam_.setDefault( null );
        seqParam_.setDescription( new String[] {
            "Name of a column to be added to the output table",
            "which will contain the sequence number of the input table",
            "from which each row originated.",
            "This column will contain 1 for the rows from the first",
            "concatenated table, 2 for the second, and so on.",
        } );

        locParam_ = new Parameter( "loccol" );
        locParam_.setUsage( "<colname>" );
        locParam_.setNullPermitted( true );
        locParam_.setDefault( null );
        locParam_.setDescription( new String[] {
            "Name of a column to be added to the output table",
            "which will contain the location",
            "(as specified in the input parameter(s))",
            "of the input table from which each row originated.",
        } );

        ulocParam_ = new Parameter( "uloccol" );
        ulocParam_.setUsage( "<colname>" );
        ulocParam_.setNullPermitted( true );
        ulocParam_.setDefault( null );
        ulocParam_.setDescription( new String[] {
            "Name of a column to be added to the output table",
            "which will contain the unique part of the location",
            "(as specified in the input parameter(s))",
            "of the input table from which each row originated.",
            "If the input tables are, for instance \"/data/cat_a1.fits\"",
            "and \"/data/cat_b2.fits\" then rows from the first will",
            "contain \"a1\" and rows from the second will contain \"b2\".",
        } );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            seqParam_,
            locParam_,
            ulocParam_,
        };
    }

    public TableMapping createMapping( Environment env ) throws TaskException {
        String seqCol = seqParam_.stringValue( env );
        String locCol = locParam_.stringValue( env );
        String ulocCol = ulocParam_.stringValue( env );
        return new CatMapping( seqCol, locCol, ulocCol,
                               getInputLocations( env ) );
    }

    /**
     * Sets the task with which this mapper is associated.
     * Required so that we can interrogate it to find out input table
     * locations which are needed unders some circumstances.
     *
     * @param   task  mapper task
     */
    public void setTask( MapperTask task ) {
        task_ = task;
    }

    /**
     * Returns the locations of the input tables for this mapping.
     * The task must be set ({@link #setTask}) for this to work.
     *
     * @param   env  execution environment
     * @return  input table location strings
     */
    private String[] getInputLocations( Environment env ) throws TaskException {
        MapperTask.InputSpec[] inSpecs = task_.getInputSpecs( env );
        String[] locs = new String[ inSpecs.length ];
        for ( int i = 0; i < inSpecs.length; i++ ) {
            locs[ i ] = inSpecs[ i ].getLocation();
        }
        return locs;
    }

    /**
     * Returns the unique parts of a set of location strings.
     * Any part which is common to all strings at the start and end 
     * is stripped off.
     *
     * @param   locs  input strings
     * @return  output strings
     */
    static String[] getUniqueParts( String[] locs ) {

        /* Find minimum common length. */
        int nloc = locs.length;
        String loc0 = locs[ 0 ];
        int leng = loc0.length();
        for ( int iloc = 0; iloc < nloc; iloc++ ) {
            leng = Math.min( leng, locs[ iloc ].length() );
        }

        /* Find length of maximum common prefix string. */
        int npre = -1;
        for ( int ic = 0; ic < leng && npre < 0; ic++ ) {
            char c = loc0.charAt( ic );
            for ( int iloc = 0; iloc < nloc; iloc++ ) {
                if ( locs[ iloc ].charAt( ic ) != c ) {
                    npre = ic;
                }
            }
        }

        /* Find length of maximum common postfix string. */
        int npost = -1;
        for ( int ic = 0; ic < leng && npost < 0; ic++ ) {
            char c = loc0.charAt( loc0.length() - 1 - ic );
            for ( int iloc = 0; iloc < nloc; iloc++ ) {
                if ( locs[ iloc ].charAt( locs[ iloc ].length() - 1 - ic )
                     != c ) {
                    npost = ic;
                }
            }
        }

        /* Pick out and return unique parts. */
        String[] uparts = new String[ nloc ];
        for ( int iloc = 0; iloc < nloc; iloc++ ) {
            String loc = locs[ iloc ];
            uparts[ iloc ] = loc.substring( npre, loc.length() - npost );
        }
        return uparts;
    }

    /**
     * Mapping which concatenates the tables.
     */
    private static class CatMapping implements TableMapping {

        private final String seqCol_;
        private final String locCol_;
        private final String ulocCol_;
        private final String[] locations_;
        private final String[] ulocs_;

        /**
         * Constructor.
         *
         * @param  seqCol  name of sequence column to be added, or null
         * @param  locCol  name of location column to be added, or null
         * @param  ulocCol name of unique location to be added, or null
         * @param  locations  location strings for each of the input tables
         *         which will be presented
         */
        CatMapping( String seqCol, String locCol, String ulocCol,
                    String[] locations ) {
            seqCol_ = seqCol;
            locCol_ = locCol;
            ulocCol_ = ulocCol;
            locations_ = locations;
            ulocs_ = ulocCol == null ? null
                                     : getUniqueParts( locations );
        }

        public void mapTables( StarTable[] inTables, TableConsumer[] consumers )
                throws IOException {
            int nTable = inTables.length;

            /* Append additional columns to the input tables as required. */
            for ( int i = 0; i < nTable; i++ ) {
                final StarTable inTable = inTables[ i ];
                ColumnStarTable addTable = new ColumnStarTable( inTable ) {
                    public long getRowCount() {
                        return inTable.getRowCount();
                    }
                };
                if ( seqCol_ != null ) {
                    ColumnInfo seqInfo = new ColumnInfo( SEQ_INFO );
                    seqInfo.setName( seqCol_ );
                    Short iseq = new Short( (short) ( i + 1 ) );
                    addTable.addColumn( new ConstantColumn( seqInfo, iseq ) );
                }
                if ( locCol_ != null ) {
                    ColumnInfo locInfo = new ColumnInfo( LOC_INFO );
                    locInfo.setName( locCol_ );
                    String loc = locations_[ i ];
                    addTable.addColumn( new ConstantColumn( locInfo, loc ) );
                }
                if ( ulocCol_ != null ) {
                    ColumnInfo ulocInfo = new ColumnInfo( ULOC_INFO );
                    ulocInfo.setName( ulocCol_ );
                    String uloc = ulocs_[ i ];
                    addTable.addColumn( new ConstantColumn( ulocInfo, uloc ) );
                }
                if ( addTable.getColumnCount() > 0 ) {
                    inTables[ i ] =
                        new JoinStarTable( new StarTable[] { inTables[ i ],
                                                             addTable } );
                }
            }

            /* Perform the concatenation on the (possibly doctored) input 
             * tables. */
            consumers[ 0 ].consume( new ConcatStarTable( inTables[ 0 ],
                                                         inTables ) );
        }
    }
}
