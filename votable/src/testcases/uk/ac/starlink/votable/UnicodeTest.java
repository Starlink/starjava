package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class UnicodeTest extends TestCase {

    private final StarTable utable_;

    static {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.votable" )
                .setLevel( Level.WARNING );
    }

    public UnicodeTest() throws IOException {
        URL votloc = getClass().getResource( "unicode.vot" );
        utable_ = new StarTableFactory()
                 .makeStarTable( DataSource.makeDataSource( votloc ) );
    }

    public void testUnicode() throws IOException {
        checkUnicodeContent( utable_ );
    }

    public void testRoundTrips() throws IOException {
        VOTableBuilder inHandler = new VOTableBuilder();
        for ( Charset encoding :
              new Charset[] { StandardCharsets.UTF_8,
                              StandardCharsets.UTF_16 } ) {
            for ( DataFormat fmt :
                  new DataFormat[] { DataFormat.TABLEDATA,
                                     DataFormat.BINARY,
                                     DataFormat.BINARY2, } ) {
                VOTableWriter writer = new VOTableWriter();
                writer.setVotableVersion( VOTableVersion.V16 );
                writer.setEncoding( encoding );
                writer.setDataFormat( fmt );
                StarTable t1 =
                    SerializerTest.roundTrip( utable_, writer, inHandler );
                checkUnicodeContent( t1 );
            }
        }
    }

    public void testPreV16() throws IOException {
        if ( ! Encoder.PRE_V16_LEGACY_CHAR_ENCODING ) {
            VOTableBuilder inHandler = new VOTableBuilder();
            for ( Charset encoding :
                  new Charset[] { StandardCharsets.UTF_8,
                                  StandardCharsets.UTF_16 } ) {
                for ( DataFormat fmt :
                      new DataFormat[] { DataFormat.TABLEDATA,
                                         DataFormat.BINARY,
                                         DataFormat.BINARY2, } ) {
                    VOTableWriter writer = new VOTableWriter();
                    writer.setVotableVersion( VOTableVersion.V14 );
                    writer.setEncoding( encoding );
                    writer.setDataFormat( fmt );
                    StarTable t1 =
                        SerializerTest.roundTrip( utable_, writer, inHandler );
                    checkUnicodeContent14( t1 );
                }
            }
        }
    }

    private void checkUnicodeContent( StarTable table ) throws IOException {
        table = Tables.randomTable( table );
        String beta = "\u0392\u03b7\u03c4\u03b1";
        assertEquals( beta, table.getCell( 1, 1 ) );
        assertEquals( beta + "..", table.getCell( 1, 2 ) );
        assertArrayEquals( new String[] { beta + "..", "beta......", },
                           table.getCell( 1, 3 ) );
        assertEquals( beta, table.getCell( 1, 5 ) );
        assertEquals( beta + ".", table.getCell( 1, 6 ) );
        assertArrayEquals( new String[] { beta + ".", "beta.", },
                           table.getCell( 1, 7 ) );

        String gamma = "\u0393\u03b1\u03bc\u03bc\u03b1";
        char gammaChr = '\u0393';
        char multocular = '\ua66e';
        String smiley = "\uD83D\uDE00";
        assertEquals( "" + gammaChr, table.getCell( 2, 1 ) );
        assertEquals( "" + gammaChr + multocular + "." + smiley,
                      table.getCell( 2, 2 ) );
        assertArrayEquals( new String[] { "" + gammaChr + multocular + smiley,
                                          "ABCDEFGHIJ" },
                           table.getCell( 2, 3 ) );
        assertEquals( "" + gammaChr, "" + table.getCell( 2, 4 ) );
        assertEquals( gamma, table.getCell( 2, 5 ) );
        assertEquals( "" + gammaChr + multocular + "...",
                      table.getCell( 2, 6 ) );
        assertArrayEquals( new String[] { "" + gammaChr + multocular + "...",
                                          "ABCDE" }, table.getCell( 2, 7 ) );

        String borscht = "\u0411\u043e\u0440\u0449";
        String omega = "\u03a9\u03bc\u03b5\u03b3\u03b1";
        assertEquals( "oxtail",
                      table.getParameterByName( "soup1" ).getValue() );
        assertEquals( borscht,
                      table.getParameterByName( "soup2" ).getValue() );
        assertEquals( omega + " " + multocular + " " + smiley,
                      table.getParameterByName( "misc" ).getValue() );

        String socrates = "\u03a3\u03c9\u03ba\u03c1\u03b1\u03c4\u03b7\u03c2";
        String confucius = "\u5b54\u5b50";
        String[] philosophers = table.getParameterByName( "philosophers" )
                                     .getTypedValue( String[].class );
        assertArrayEquals( new String[] { confucius, socrates, "Wittgenstein" },
                           philosophers );
    }

    private void checkUnicodeContent14( StarTable table ) throws IOException {

        // If uk.ac.starlink.votable.Encoder.CharWriter.ASCII mapped
        // single non-ASCII code points rather than single non-ASCII
        // char primitives to '?', this would be false.
        final boolean asciiByChar = true;

        table = Tables.randomTable( table );
        String beta = "\u0392\u03b7\u03c4\u03b1";
        String betaAscii = "????";
        assertEquals( betaAscii, table.getCell( 1, 1 ) );
        assertEquals( betaAscii + "..", table.getCell( 1, 2 ) );
        assertArrayEquals( new String[] { betaAscii + "..", "beta......", },
                           table.getCell( 1, 3 ) );
        assertEquals( beta, table.getCell( 1, 5 ) );
        assertEquals( beta + ".", table.getCell( 1, 6 ) );
        assertArrayEquals( new String[] { beta + ".", "beta.", },
                           table.getCell( 1, 7 ) );

        String gamma = "\u0393\u03b1\u03bc\u03bc\u03b1";
        String gammaAscii = "?????";
        char gammaChr = '\u0393';
        char gammaChrAscii = '?';
        char multocular = '\ua66e';
        char multocularAscii = '?';
        String smiley = "\uD83D\uDE00";
        String smileyAscii = asciiByChar ? "??" : "?";
        assertEquals( "" + gammaChrAscii, table.getCell( 2, 1 ) );
        assertEquals( "" + gammaChrAscii + multocularAscii + "." + smileyAscii,
                      table.getCell( 2, 2 ) );
        assertArrayEquals( new String[] {
                           "" + gammaChrAscii + multocularAscii + smileyAscii,
                           "ABCDEFGHIJ",
                           }, table.getCell( 2, 3 ) );
        assertEquals( "" + gammaChr, "" + table.getCell( 2, 4 ) );
        assertEquals( gamma, table.getCell( 2, 5 ) );
        assertEquals( "" + gammaChr + multocular + "...",
                      table.getCell( 2, 6 ) );
        assertArrayEquals( new String[] { "" + gammaChr + multocular + "...",
                                          "ABCDE" }, table.getCell( 2, 7 ) );
    }
}
