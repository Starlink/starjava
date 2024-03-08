package uk.ac.starlink.votable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.fits.ColFitsTableWriter;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.fits.HduFitsTableWriter;
import uk.ac.starlink.fits.VariableFitsTableWriter;
import uk.ac.starlink.table.LoopTableScheme;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TestTableScheme;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class UnifiedFitsTableWriterTest extends TestCase {

    private static final StarTableOutput sto_ = new StarTableOutput();

    public UnifiedFitsTableWriterTest() {
        LogUtils.getLogger( "uk.ac.starlink.table.storage" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.SEVERE );
    }

    public void testEquivalence() throws IOException {

        // Check that the UnifiedFitsTableWriter with suitable options
        // behaves the same (bytewise identical output) to the corresponding
        // deprecated legacy output handlers.
        WriterPair[] pairs = new WriterPair[] {
            createPair( FitsTableWriter.class.getName(),
                        "primarytype=basic" ),
            createPair( VariableFitsTableWriter.class.getName(),
                        "primarytype=basic,vararray=true" ),
            createPair( FitsPlusTableWriter.class.getName() + "(date=false)",
                        "primarytype=votable,date=false" ),
            createPair( ColFitsPlusTableWriter.class.getName() + "(date=false)",
                        "colfits=true,date=false" ),
            createPair( ColFitsTableWriter.class.getName(),
                        "colfits=true,primarytype=basic" ),
            createPair( FitsPlusTableWriter.class.getName()
                        + "(votableversion=V12,date=false)",
                        "primarytype=votable1.2,date=false" ),
            createPair( FitsTableWriter.class.getName() + "(wide=default)",
                        "primarytype=basic,wide=default" ),
            createPair( HduFitsTableWriter.class.getName(),
                        "primarytype=none" ),
        };
        StarTable[] tables = new StarTable[] {
            new LoopTableScheme().createTable( "100" ),
            new TestTableScheme().createTable( "10,sgw" ),
            new TestTableScheme().createTable( "2,sk" ),
        };
        for ( StarTable table : tables ) {
            for ( WriterPair pair : pairs ) {
                byte[] out1 = writeFits( table, pair.tw1_ );
                byte[] out2 = writeFits( table, pair.tw2_ );
                assertArrayEquals( out1, out2 );
            }
        }
    }

    public void testEquivalenceNotWide() throws IOException {
        WriterPair pair =
            createPair( FitsTableWriter.class.getName() +"(wide=)",
                        "primarytype=basic,wide=" );

        StarTable thinTable = new TestTableScheme().createTable( "23,sf" );
        assertArrayEquals( writeFits( thinTable, pair.tw1_ ),
                           writeFits( thinTable, pair.tw2_ ) );

        // Can't do quite the same thing for a failed attempt to write
        // a wide table, but at least check that they both fail in the
        // same way.
        StarTable wideTable = new TestTableScheme().createTable( "2,sk" );
        for ( StarTableWriter w : pair.pairArray() ) {
            try {
                writeFits( wideTable, w );
                fail();
            }
            catch ( IOException err ) {
                assertTrue( err.getMessage().indexOf( "999" ) > 0 );
            }
        }
    }

    private static WriterPair createPair( String handlerName1,
                                          String unifiedConfig2 )
            throws IOException {
        return new WriterPair( handlerName1,
                               UnifiedFitsTableWriter.class.getName()
                             + "(" + unifiedConfig2 + ")" ) {
            @Override
            public String toString() {
                return handlerName1 + " + (" + unifiedConfig2 + ")";
            }
        };
    }

    private static byte[] writeFits( StarTable table, StarTableWriter tw )
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tw.writeStarTable( table, out );
        byte[] buf = out.toByteArray();
        for ( int i = 0; i < buf.length; i += 80 ) {
            if ( "STILCLAS= ".equals( new String( buf, i, 10, "UTF-8" ) ) ) {
                for ( int j = i; j < i + 80; j++ ) {
                    buf[ j ] = ' ';
                }
            }
        }
        return buf;
    }

    private static void dumpBuffer( byte[] buf, String fname )
            throws IOException {
        System.out.println( " -> " + fname );
        try ( java.io.OutputStream out =
                  new java.io.FileOutputStream( fname ) ) {
            out.write( buf );
        }
    }

    private static class WriterPair {
        final AbstractFitsTableWriter tw1_;
        final AbstractFitsTableWriter tw2_;
        WriterPair( StarTableWriter tw1, StarTableWriter tw2 ) {
            tw1_ = (AbstractFitsTableWriter) tw1;
            tw2_ = (AbstractFitsTableWriter) tw2;
        }
        WriterPair( String fmt1, String fmt2 ) throws IOException {
            this( sto_.getHandler( fmt1 ), sto_.getHandler( fmt2 ) );
            tw1_.setWriteDate( false );
            tw2_.setWriteDate( false );
        }
        AbstractFitsTableWriter[] pairArray() {
            return new AbstractFitsTableWriter[] { tw1_, tw2_ };
        }
    }
}
