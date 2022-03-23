package uk.ac.starlink.fits;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.storage.DiscardByteStore;
import uk.ac.starlink.util.TestCase;

public class HeaderTest extends TestCase {

    private static final String TXT80 =
           "ABCDEFGHIJKLMNOPQRSTUVWXYZ[].0123456789_"
         + "abcdefghijklmnopqrstuvwxyz{}<>()-*+/~%&!";
    private Logger logger_ = Logger.getLogger( "uk.ac.starlink.fits" );

    public HeaderTest() {
        logger_.setLevel( Level.WARNING );
    }

    public void testCardFactories() {
        parseAs( CardFactory.END_CARD, CardType.END );
        exerciseCardFactory( CardFactory.CLASSIC );
        exerciseCardFactory( CardFactory.DEFAULT );
        exerciseCardFactory( CardFactory.HIERARCH );
        exerciseCardFactory( CardFactory.STRICT );

        String val69 = ltxt( 69 );
        assertRuntimeFail(
            () -> CardFactory.STRICT.createStringCard( "VALUE", val69, null ) );
        assertCardEquals( "VALUE", ltxt( 68 ), null,
                          parseAs( CardFactory.CLASSIC
                                  .createStringCard( "VALUE", val69, null ),
                                   CardType.STRING ) );
        String qval68 = "'" + ltxt( 67 );
        assertRuntimeFail(
            () -> CardFactory.STRICT.createStringCard( "VALUE", qval68, null ));
        assertCardEquals( "VALUE", "'" + ltxt( 66 ), null,
                          parseAs( CardFactory.CLASSIC
                                  .createStringCard( "VALUE", qval68, null ),
                                   CardType.STRING ) );
    }

    public void testTxt() {
        assertEquals( "ABCD", ltxt( 4 ) );
        assertEquals( 80, TXT80.length() );
        assertEquals( 23, ltxt( 23 ).length() );
        assertEquals( 99, ltxt( 99 ).length() );
    }

    public void testContinue() throws HeaderValueException {
        // Some of these lines are from the FITS standard
        FitsHeader hdr = headerFromLines( new String[] {
            "WEATHER = 'Partly cloudy during the evening f&'",
            "CONTINUE  'ollowed by cloudy skies overnight.&  '",
            "CONTINUE  ' Low 21C. Winds NNE at 5 to 10 mph.'",
            "NUMBER  = 23 / twenty-three",
            "AND     = ' Ampersand  &' / ampersand",
            "STRKEY  = 'This keyword value is continued &'",
            "CONTINUE  ' over multiple keyword records.&'",
            "CONTINUE  '&' / The comment field for this",
            "CONTINUE  '&' / keyword is also continued",
            "CONTINUE  '' / over multiple records.",
        } );
        assertEquals( CardType.STRING, hdr.getCards()[ 0 ].getType() );
        assertEquals( CardType.CONTINUE, hdr.getCards()[ 1 ].getType() );
        assertEquals( CardType.CONTINUE, hdr.getCards()[ 2 ].getType() );
        assertEquals( CardType.INTEGER, hdr.getCards()[ 3 ].getType() );
        assertEquals( "Partly cloudy during the evening followed by cloudy "
                    + "skies overnight. Low 21C. Winds NNE at 5 to 10 mph.",
                      hdr.getStringValue( "WEATHER" ) );
        assertEquals( 23, hdr.getRequiredIntValue( "NUMBER" ) );
        assertEquals( " Ampersand  &", hdr.getStringValue( "AND" ) );
        assertNull( hdr.getIntValue( "NONUMBER" ) );
        assertHeaderValueFail( () -> hdr.getRequiredIntValue( "NONUMBER" ) );
        assertEquals( "This keyword value is continued  over "
                    + "multiple keyword records.",
                      hdr.getStringValue( "STRKEY" ) );
        DescribedValue strk = hdr.getDescribedValue( "STRKEY" );
        assertEquals( "STRKEY", strk.getInfo().getName() );
        assertEquals( "This keyword value is continued  over "
                    + "multiple keyword records.", strk.getValue() );
        assertEquals( "The comment field for this keyword is also continued "
                    + "over multiple records.",
                      strk.getInfo().getDescription() );
        assertEquals( String.class, strk.getInfo().getContentClass() );
        assertEquals( "twenty-three", hdr.getDescribedValue( "NUMBER" )
                                         .getInfo().getDescription() );
    }

    public void testHierarch() {
        assertCardEquals( "HIERARCH ESO INS OPTI-3 ID", "ESO#427",
                          "Optical element identifier",
            parseAs( "HIERARCH ESO INS OPTI-3 ID = 'ESO#427 ' "
                   + "/ Optical element identifier", CardType.STRING_HIER )
        );
        assertCardEquals( "HIERARCH ESO TEL FOCU SCALE", Double.valueOf( 1.489),
                          "(deg/m) Focus length  = 5.36\"/mm",
            parseAs( "HIERARCH ESO TEL FOCU SCALE = 1.489 "
                    + "/ (deg/m) Focus length  = 5.36\"/mm",
                      CardType.REAL_HIER )
        );
        assertCardEquals( "HIERARCH LONGKEYWORD", BigInteger.valueOf( 47 ),
                          "Keyword has > 8 characters",
            parseAs( "HIERARCH LONGKEYWORD = 47 / Keyword has > 8 characters",
                     CardType.INTEGER_HIER )
        );
        assertCardEquals( "HIERARCH LONG-KEY_WORD2", Boolean.FALSE,
                          "Long keyword with hyphen, underscore and digit",
            parseAs( "HIERARCH LONG-KEY_WORD2 = F "
                   + "/ Long keyword with hyphen, underscore and digit",
                     CardType.LOGICAL_HIER )
        );

        assertCardEqualsExact(
            "HIERARCH ESO INS OPTI-3 ID = 'ESO#427 ' " +
            "/ Optical element identifier",
            "HIERARCH ESO INS OPTI-3 ID", "ESO#427",
            "Optical element identifier",
            parseAs( CardFactory.HIERARCH
                    .createStringCard( "HIERARCH ESO INS OPTI-3 ID",
                                       "ESO#427",
                                       "Optical element identifier" ),
                     CardType.STRING_HIER )
        );

        assertRuntimeFail( () ->
            CardFactory.CLASSIC
           .createStringCard( "HIERARCH ESO INS OPTI-3 ID", "ESO#427",
                              "Optical element identifier" )
        );
    }

    public void testArrayMeta() throws IOException {
        FitsHeader hdr = headerFromLines( new String[] {
            "XTENSION= 'BINTABLE'           / binary table extension",
            "BITPIX  =                    8 / array data type",
            "NAXIS   =                    2 / number of array dimensions",
            "NAXIS1  =                    0 / length of dimension 1",
            "NAXIS2  =                    0 / length of dimension 2",
            "PCOUNT  =                    0 / number of group parameters",
            "GCOUNT  =                    1 / number of groups",
            "TFIELDS =                    0 / number of table fields",
            "SAMPLING= '(336.0, 338.0, 340.0, 342.0, 344.0, &'",
            "CONTINUE  '570.0, 572.0, 574.0, 576.0, 578.0, &'",
            "CONTINUE  '1018.0, 1020.0)'",
            "",
            "DUMPLING=  '  (  1,  2, 3, 3.1415, 4, 5  )  '  ",
            "END",
        } );
        InputFactory dummyFact =
            InputFactory.createByteStoreFactory( new DiscardByteStore() );
        BintableStarTable table =
            BintableStarTable.createTable( hdr, dummyFact, (WideFits) null );
        DescribedValue samplingParam = table.getParameterByName( "SAMPLING" );
        DescribedValue dumplingParam = table.getParameterByName( "DUMPLING" );
        DescribedValue wimplingParam = table.getParameterByName( "WIMPLING" );
        assertNull( wimplingParam );
        assertEquals( int[].class, samplingParam.getInfo().getContentClass() );
        assertEquals( double[].class,
                      dumplingParam.getInfo().getContentClass() );
        int[] sampling = (int[]) samplingParam.getValue();
        double[] dumpling = (double[]) dumplingParam.getValue();
        assertArrayEquals( new double[] { 1, 2, 3, 3.1415, 4, 5 }, dumpling,
                           1e-10 );
        assertEquals( 12, sampling.length );
        assertEquals( 1020, sampling[ 11 ] );
    }

    private void exerciseCardFactory( CardFactory cf ) {
        assertEquals( "= comment text",
                      parseAs( cf.createPlainCard( "COMMENT = comment text" ),
                               CardType.COMMENT )
                     .getComment() );
        assertEquals( "= history text",
                      parseAs( cf.createPlainCard( "HISTORY = history text" ),
                               CardType.HISTORY )
                     .getComment() );
        assertEquals( "",
                      parseAs( cf.createPlainCard( "" ),
                               CardType.COMMENT_BLANK )
                     .getComment() );

        assertCardEqualsExact(
            "BITPIX  =                    8 / 8-bit bytes",
            "BITPIX", BigInteger.valueOf( 8 ), "8-bit bytes",
            parseAs( cf.createIntegerCard( "BITPIX", 8, "8-bit bytes" ),
                     CardType.INTEGER ) );
        assertCardEqualsExact(
            "TSCAL9  =                 0.25 / Quarter scaling",
            "TSCAL9", Double.valueOf( 0.25 ), "Quarter scaling",
            parseAs( cf.createRealCard( "TSCAL9", 0.25, "Quarter scaling" ),
                     CardType.REAL ) );
        assertCardEqualsExact(
            "SIMPLE  =                    T / Standard FITS",
            "SIMPLE", Boolean.TRUE, "Standard FITS",
            parseAs( cf.createLogicalCard( "SIMPLE", true, "Standard FITS" ),
                     CardType.LOGICAL ) );
        assertCardEqualsExact(
            "USUL    = 'Muad''dib'          / Kwisatz Haderach",
            "USUL", "Muad'dib", "Kwisatz Haderach",
            parseAs( cf.createStringCard( "USUL", "Muad'dib",
                                          "Kwisatz Haderach" ),
                     CardType.STRING ) );

        for ( int i = 0; i <= 72; i++ ) {
            String comm = ltxt( i );
            assertEquals( comm,
                          parseAs( cf.createCommentCard( comm ),
                                   CardType.COMMENT )
                         .getComment() );

            assertEquals( comm,
                          parseAs( cf.createPlainCard( "HISTORY " + comm ),
                                   CardType.HISTORY )
                         .getComment() );
            assertEquals( comm,
                          parseAs( cf.createPlainCard( "        " + comm ),
                                   CardType.COMMENT_BLANK )
                         .getComment() );

        }

        for ( int i = 1; i <= 80; i++ ) {
            String card = ltxt( i );
            assertEquals( card,
                          parseAs( cf.createPlainCard( card ),
                                   CardType.COMMENT_OTHER )
                         .getComment() );
        }

        for ( int i = 0; i <= 68; i++ ) {
            String value = ltxt( i );
            assertEquals( value,
                          parseAs( cf.createStringCard( "VALUE", value, null ),
                                   CardType.STRING )
                         .getValue() );
        }

        for ( int i = 1; i <= 60; i++ ) {
            String comm = ltxt( i );
            assertCardEquals(
                "DOG", "RADIO", comm,
                parseAs( cf.createStringCard( "DOG", "RADIO", comm ),
                         CardType.STRING ) );
            assertCardEquals(
                "NUM", BigInteger.valueOf( 9012500 ), comm,
                parseAs( cf.createIntegerCard( "NUM", 9012500, comm ),
                         CardType.INTEGER ) );
        }
    }

    private ParsedCard<?> parse( CardImage cardImage ) {
        return FitsUtil.parseCard( cardImage.getBytes() );
    }

    private <T> ParsedCard<T> parseAs( CardImage cardImage, CardType<T> type ) {
        ParsedCard<?> pc = parse( cardImage );
        assertEquals( type, pc.getType() );
        return (ParsedCard<T>) pc;
    }

    private <T> ParsedCard<T> parseAs( String txt, CardType<T> type ) {
        return parseAs( new CardImage( pad( txt, 80 ) ), type );
    }

    private <T> void assertCardEquals( String key, T value, String comment,
                                       ParsedCard<T> card ) {
        assertEquals( key, card.getKey() );
        assertEquals( value, card.getValue() );
        assertEquals( comment, card.getComment() );
    }

    private <T> void assertCardEqualsExact( String cardTxt,
                                            String key, T value, String comment,
                                            ParsedCard<T> card ) {
        assertCardEquals( key, value, comment, card );
        assertEquals( pad( cardTxt, 80 ), card.toString() );
    }

    private void assertRuntimeFail( Runnable runnable ) {
        try {
            runnable.run();
            fail( "Should have failed" );
        }
        catch ( RuntimeException e ) {
            // OK
        }
    }

    private void assertHeaderValueFail( Callable callable ) {
        try {
            callable.call();
            fail( "Should have failed" );
        }
        catch ( HeaderValueException e ) {
            // OK
        }
        catch ( Exception e ) {
            fail( "Wrong failure: " + e );
        }
    }

    private static FitsHeader headerFromLines( String[] lines ) {
        List<ParsedCard<?>> cards = new ArrayList<>();
        for ( String line : lines ) {
            cards.add( parseTxt( line ) );
        }
        return new FitsHeader( cards.toArray( new ParsedCard<?>[ 0 ] ) );
    }

    private static ParsedCard<?> parseTxt( String cardTxt ) {
        return FitsUtil.parseCard( pad( cardTxt, 80 )
                                  .getBytes( StandardCharsets.US_ASCII ) );
    }

    private static String ltxt( int nchr ) {
        StringBuffer sbuf = new StringBuffer();
        while ( nchr > TXT80.length() ) {
            sbuf.append( TXT80 );
            nchr -= TXT80.length();
        }
        sbuf.append( TXT80.substring( 0, nchr ) );
        return sbuf.toString();
    }

    private static String pad( String txt, int minLeng ) {
        StringBuffer sbuf = new StringBuffer( txt );
        while ( sbuf.length() < minLeng ) {
            sbuf.append( ' ' );
        }
        return sbuf.toString();
    }
}
