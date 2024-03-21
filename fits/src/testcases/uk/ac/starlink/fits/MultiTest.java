package uk.ac.starlink.fits;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.FileDataSource;

public class MultiTest extends TestCase {

    public void setUp() {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.SEVERE );
    }

    public void testMultiWrite() throws IOException {
        multiWriteTrial( new FileIOer( File.createTempFile( "multi",
                                                            ".fits" ) ) );
        multiWriteTrial( new ZFileIOer( File.createTempFile( "multi",
                                                             ".fits.gz" ) ) );
    }

    private void multiWriteTrial( IOer ioer ) throws IOException {
        OutputStream out = ioer.createOutputStream();
        FitsUtil.writeEmptyPrimary( out );
        FitsTableWriter writer = new FitsTableWriter();
        writer.setPrimaryType( FitsTableWriter.PrimaryType.NONE );
        int nt = 3;
        StarTable[] outTables = new StarTable[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            outTables[ i ] = createTestTable( i * 8 );
            writer.writeStarTable( outTables[ i ], out );
        }
        out.close();

        FitsTableBuilder builder = new FitsTableBuilder();
        StarTable[] inTables =
            Tables.tableArray( builder
                              .makeStarTables( ioer.createDataSource(),
                                               StoragePolicy.PREFER_MEMORY ) );
        assertEquals( outTables.length, nt );
        for ( int i = nt - 1; i >= 0; i-- ) {
            assertTableEquals( outTables[ i ], inTables[ i ] );
        }

        for ( int i = 0; i < nt; i++ ) {
            DataSource datsrc = ioer.createDataSource();
            datsrc.setPosition( Integer.toString( i + 1 ) );
            assertTableEquals(
                outTables[ i ],
                builder.makeStarTable( datsrc, false,
                                       StoragePolicy.PREFER_MEMORY ) );
        }

    }

    private void assertTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        t1 = Tables.randomTable( t1 );
        t2 = Tables.randomTable( t2 );
        int nc = t1.getColumnCount();
        int nr = (int) t1.getRowCount();
        assertEquals( nc, t2.getColumnCount() );
        assertEquals( nr, t2.getRowCount() );
        for ( int ic = 0; ic < nc; ic++ ) {
            for ( long ir = 0; ir < nr; ir++ ) {
                assertEquals( t1.getCell( ir, ic ), t2.getCell( ir, ic ) );
            }
        }
    }

    public static StarTable createTestTable( int nrow ) {
        ColumnInfo c1 = new ColumnInfo( "I", Integer.class, null );
        ColumnInfo c2 = new ColumnInfo( "D", Double.class, null );
        RowListStarTable table =
            new RowListStarTable( new ColumnInfo[] { c1, c2 } ) ;
        for ( int i = 0; i < nrow; i++ ) {
            table.addRow( new Object[] { new Integer( i ),
                                         new Double( (double) i ), } );
        }
        return table;
    }

    private static abstract class IOer {
        abstract OutputStream createOutputStream() throws IOException;
        abstract DataSource createDataSource() throws IOException;
    }

    private static class FileIOer extends IOer {
        private final File file_;
        FileIOer( File file ) {
            file_ = file;
        }
        public OutputStream createOutputStream() throws IOException {
            file_.deleteOnExit();
            return new FileOutputStream( file_ );
        }
        public DataSource createDataSource() throws IOException {
            return new FileDataSource( file_ );
        }
    }

    private static class ZFileIOer extends FileIOer {
        ZFileIOer( File file ) {
            super( file );
        }
        public OutputStream createOutputStream() throws IOException {
            return new GZIPOutputStream( super.createOutputStream() );
        }
    }
}
