package uk.ac.starlink.ttools.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.jdbc.SequentialResultSetStarTable;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.StringElementSizer;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOSerializerConfig;
import uk.ac.starlink.votable.VOTableVersion;

/**
 * Writes SQL ResultSets to VOTable with the INFO elements appropriate
 * for TAP output.
 * It's all streamed, so no appreciable amount of memory should be required,
 * and a maximum record count can be imposed.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2013
 */
public class TapWriter {

    private final DataFormat dfmt_;
    private final VOTableVersion version_;
    private final long maxrec_;
    private final StringElementSizer stringSizer_;

    /**
     * Constructor.
     *
     * @param  dfmt  selects VOTable serialization format
     *               (TABLEDATA, BINARY, BINARY2, FITS)
     * @param  version  selects VOTable version
     * @param  maxrec   maximum record count before overflow;
     *                  negative value means no limit
     */
    public TapWriter( DataFormat dfmt, VOTableVersion version, long maxrec ) {
        dfmt_ = dfmt;
        version_ = version;
        maxrec_ = maxrec;

        // When writing VOTable columns containing arrays of strings,
        // the length of each string has to be known up front.
        // This setting determines how that length will be determined
        // in the case that that information is not already available
        // from the column metadata (via a non-negative
        // ValueInfo.getElementSize() value).
        // The ERROR_IF_USED value will cause a write error if this happens,
        // so that it's no possible to write string arrays with elements
        // of unknown length.
        // An alternative is to detect the value from the table data
        // by setting it to StringElementSizer.READ instead,
        // but that would inhibit output streaming.
        // If you don't have any string-array-valued columns,
        // you don't need to worry about this setting.
        stringSizer_ = StringElementSizer.ERROR_IF_USED;
    }

    /**
     * Writes a result set to an output stream as a VOTable.
     *
     * @param   rset  result set
     * @param   ostrm  destination stream
     */
    public void writeVOTable( ResultSet rset, OutputStream ostrm )
            throws IOException, SQLException {

        /* Turns the result set into a table. */
        LimitedResultSetStarTable table =
            new LimitedResultSetStarTable( rset, maxrec_ );

        /* Prepares the object that will do the serialization work. */
        VOSerializerConfig config =
            new VOSerializerConfig( dfmt_, version_, stringSizer_ );
        VOSerializer voser = VOSerializer.makeSerializer( config, table );
        BufferedWriter out =
            new BufferedWriter( new OutputStreamWriter( ostrm ) );

        /* Write header. */
        out.write( "<VOTABLE"
                 + VOSerializer.formatAttribute( "version",
                                                 version_.getVersionNumber() )
                 + VOSerializer.formatAttribute( "xmlns",
                                                 version_.getXmlNamespace() )
                 + ">" );
        out.newLine();
        out.write( "<RESOURCE>" );
        out.newLine();
        out.write( "<INFO name='QUERY_STATUS' value='OK'/>" );
        out.newLine();

        /* Write table element. */
        voser.writeInlineTableElement( out );

        /* Check for overflow and write INFO if required. */
        if ( table.lastSequenceOverflowed() ) {
            out.write( "<INFO name='QUERY_STATUS' value='OVERFLOW'/>" );
            out.newLine();
        }

        /* Write footer. */
        out.write( "</RESOURCE>" );
        out.newLine();
        out.write( "</VOTABLE>" );
        out.newLine();
        out.flush();
    }

    /**
     * StarTable implementation which is based on a ResultSet, and which
     * is limited to a fixed number of rows when its row iterator is used.
     * Note this implementation is OK for one-pass table output handlers
     * like VOTable, but won't work for ones which require two passes
     * such as FITS (which needs row count up front).
     */
    private static class LimitedResultSetStarTable
            extends SequentialResultSetStarTable {

        private final long maxrec_;
        private boolean overflow_;

        /**
         * Constructor.
         *
         * @param   rset  result set supplying the data
         * @param   maxrec   maximum number of rows that will be iterated over;
         *                   negative value means no limit
         */
        LimitedResultSetStarTable( ResultSet rset, long maxrec )
                throws SQLException {
            super( rset );
            maxrec_ = maxrec;
        }

        /**
         * Indicates whether the last row sequence dispensed by
         * this table's getRowSequence method was truncated at maxrec rows.
         *
         * @return   true iff the last row sequence overflowed
         */
        public boolean lastSequenceOverflowed() {
            return overflow_;
        }

        @Override
        public RowSequence getRowSequence() throws IOException {
            overflow_ = false;
            RowSequence baseSeq = super.getRowSequence();
            if ( maxrec_ < 0 ) {
                return baseSeq;
            }
            else {
                return new WrapperRowSequence( baseSeq ) {
                    long irow = -1;
                    @Override
                    public boolean next() throws IOException {
                        irow++;
                        if ( irow < maxrec_ ) {
                            return super.next();
                        }
                        if ( irow == maxrec_ ) {
                            overflow_ = super.next();
                        }
                        return false;
                    }
                };
            }
        }
    }

    /**
     * Test harness.  Run with -help for usage.
     * Don't forget to put a JDBC driver on the classpath and
     * set the system property jdbc.drivers.
     */
    public static void main( String[] args ) throws IOException, SQLException {

        /* Get arguments. */
        String usage = "\nUsage: "
                     + TapWriter.class.getName()
                     + " [-maxrec <n>]"
                     + " <jdbc-url>"
                     + " <sql-query>"
                     + "\n";
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        long maxrec = -1;
        String url = null;
        String sql = null;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            it.remove();
            if ( arg.startsWith( "-h" ) ) {
                System.err.println( usage );
                return;
            }
            else if ( "-maxrec".equals( arg ) ) {
                maxrec = Long.parseLong( it.next() );
                it.remove();
            }
            else if ( url == null ) {
                url = arg;
            }
            else if ( sql == null ) {
                sql = arg;
            }
        }
        if ( url == null || sql == null || ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Get result set. */
        Connection conn = DriverManager.getConnection( url );
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery( sql );

        /* Write output. */
        TapWriter writer =
            new TapWriter( DataFormat.TABLEDATA, VOTableVersion.V12, maxrec );
        try {
            writer.writeVOTable( rset, System.out );
        }
        finally {
            rset.close();
        }
    }
}
