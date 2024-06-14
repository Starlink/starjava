package uk.ac.starlink.ttools.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.xml.sax.Locator;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.votlint.SaxMessager;
import uk.ac.starlink.ttools.votlint.VotLintCode;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

public class VotLintTest extends TestCase {

    public void testSilent() throws Exception {
        Executor ex = new Executor();
        DataSource datsrc =
            new URLDataSource( getClass().getResource( "no-errors.vot.gz" ) );
        Message[] msgs = ex.execute( datsrc );
        assertEquals( 0, msgs.length );
    }

    public void testErrors() throws Exception {
        Executor ex = new Executor();
        DataSource datsrc =
            new URLDataSource( getClass().getResource( "with-errors.vot" ) );

        /* When validating, the details of the error messages will depend
         * on the XML parser being used - which changes according to what
         * JVM you're using (e.g. J2SE1.4 or 1.5).  So just check we've got
         * the right number of errors here. */
        ex.validate_ = true;
        assertEquals( 5, ex.execute( datsrc ).length );

        /* In other cases we can check the content of the error messages
         * themselves, since they are output by votlint itself. */
        ex.validate_ = false;
        assertCodes( new String[] { "I-AR1", "W-CH1", "W-TR9", "E-NRM" },
                     ex.execute( datsrc ) );

        ex.forceVersion_ = VOTableVersion.V11;
        assertCodes( new String[] { "I-AR1", "W-CH1", "W-TR9", "E-NRM" },
                     ex.execute( datsrc ) );

        ex.forceVersion_ = VOTableVersion.V10;
        assertCodes( new String[] { "W-VRM", "W-NSX",
                                    "I-AR1", "W-CH1", "W-TR9", "E-NRM" },
                     ex.execute( datsrc ) );
    }

    public void testRead() throws Exception {
        int nrow = 3;
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( nrow );
        ColumnInfo intervalInfo =
            new ColumnInfo( "interval", double[].class, null );
        intervalInfo.setXtype( "interval" );
        intervalInfo.setShape( new int[] { 2 } );
        ColumnInfo fpInfo = new ColumnInfo( "f_poly", float[].class, null );
        fpInfo.setXtype( "polygon" );
        ColumnInfo dpInfo = new ColumnInfo( "d_poly", double[].class, null );
        dpInfo.setXtype( "polygon" );
        ColumnInfo tsInfo = new ColumnInfo( "time", String.class, null );
        tsInfo.setXtype( "timestamp" );
        table.addColumn( ArrayColumn.makeColumn( intervalInfo, new double[][] {
            { 1, 10 },
            {},
            { Double.NEGATIVE_INFINITY, -4 },
        } ) );
        table.addColumn( ArrayColumn.makeColumn( fpInfo, new float[][] {
            { 1,1, 1,2, 2,2, 2,1, },
            {},
            { 0.1f,0.1f, 0.1f,1.5f, 1.5f,0.1f, },
        } ) );
        table.addColumn( ArrayColumn.makeColumn( dpInfo, new double[][] {
            { 1,1, 1,2, 2,2, 2,1, },
            {},
            { 0.1,0.1, 0.1,1.5, 1.5,0.1, },
        } ) );
        table.addColumn( ArrayColumn.makeColumn( tsInfo, new String[] {
            "1987-06-21T22:12:00", 
            null, 
            "2112-xx-xx",
        } ) );

        for ( DataFormat format : new DataFormat[] {
                  DataFormat.TABLEDATA, DataFormat.BINARY, DataFormat.BINARY2,
                  DataFormat.FITS,
              } ) {
            boolean isFits = format == DataFormat.FITS;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            new VOTableWriter( format, true ).writeStarTable( table, bout );
            bout.close();
            byte[] buf = bout.toByteArray();
            Executor ex = new Executor();
            assertCodes( isFits ? new String[] { "I-FTZ" }
                                : new String[] { "E-TSR" },
                         ex.execute( new ByteArrayInputStream( buf ) ) );
        }
    }

    private void assertCodes( String[] codes, Message[] msgs ) {
        try {
            assertArrayEquals( codes, codes( msgs ) );
        }
        catch ( AssertionFailedError e ) {
            for ( Message msg : msgs ) { 
                System.out.println( msg );
            }
            throw e;
        }
    }

    private static String[] codes( Message[] msgs ) {
        return Arrays.stream( msgs )
              .map( msg -> msg.getLevelCode() )
              .toArray( n -> new String[ n ] );
    }

    private static class Executor {
        VOTableVersion forceVersion_;
        boolean ucd_ = true;
        Boolean unitPref_;
        boolean validate_ = true;
        String sysid_;

        Message[] execute( DataSource datsrc )
                throws IOException, TaskException {
            return execute( datsrc.getInputStream() );
        }

        Message[] execute( InputStream in ) throws IOException, TaskException {
            List<Message> msgList = new ArrayList<>();
            SaxMessager messager = new SaxMessager() {
                public void reportMessage( SaxMessager.Level level,
                                           VotLintCode code, String txt,
                                           Locator locator ) {
                    msgList.add( new Message( level, code, txt, locator ) );
                }
            };
            new VotLint.VotLintExecutable( in, forceVersion_, ucd_, unitPref_,
                                           validate_, sysid_, messager )
               .execute();
            return msgList.toArray( new Message[ 0 ] );
        }
    }

    private static class Message {
        final SaxMessager.Level level_;
        final VotLintCode code_;
        final String txt_;
        final Locator locator_;
        Message( SaxMessager.Level level, VotLintCode code, String txt,
                 Locator locator ) {
            level_ = level;
            code_ = code;
            txt_ = txt;
            locator_ = locator;
        }
        String getLevelCode() {
            return level_.toString().charAt( 0 ) + "-" + code_;
        }
        @Override
        public String toString() {
            return getLevelCode() + " "
              // + "(l." + locator_.getLineNumber() + ", "
              //         + locator_.getColumnNumber() + ") "
                 + txt_;
        }
    }
}
