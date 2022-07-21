package uk.ac.starlink.ttools.taplint;

import ari.ucidy.UCD;
import ari.ucidy.UCDParser;
import ari.ucidy.UCDSyntax;
import ari.ucidy.UCDWord;
import ari.ucidy.UCDWordList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import uk.ac.starlink.util.LogUtils;
import uk.me.nxg.unity.Syntax;
import uk.me.nxg.unity.UnitExpr;
import uk.me.nxg.unity.UnitParser;
import uk.me.nxg.unity.UnitParserException;

public class DmTest extends TestCase {

    private static final Syntax SYNTAX = Syntax.VOUNITS;
    private static final UCDSyntax UCD_P = UCDSyntax.PRIMARY;
    private static final UCDSyntax UCD_S = UCDSyntax.SECONDARY;
    private static final UCDSyntax UCD_Q = UCDSyntax.BOTH;

    public DmTest() {
        LogUtils.getLogger( "ari.ucidy" ).setLevel( Level.OFF );
    }

    public void testObsTap() throws UnitParserException {
        boolean is11 = true;
        List<ObsTapStage.ObsCol> cols = new ArrayList<>();
        cols.addAll( ObsTapStage.createMandatoryColumns( is11 ).values() );
        cols.addAll( ObsTapStage.createOptionalColumns( is11 ).values() );
        for ( ObsTapStage.ObsCol col : cols ) {
            checkUcd( col.ucd_ );
            checkUnit( col.unit_ );
        }
    }

    public void testObsLocTap() throws UnitParserException {
        for ( ObsLocStage.PlanCol col :
              ObsLocStage.createRequiredColumns().values() ) {
            checkUcd( col.ucd_ );
            checkUnit( col.unit_ );
        }
    }

    public void testEpnTap() throws UnitParserException {
        UCDParser ucdParser = createUCDParser();
        boolean isRequested15 = true;
        ucdParser.knownWords
                 .add( new UCDWord( UCD_Q, "pos.projection",
                       "Geometric projection", isRequested15 ) );
        Pattern minmaxRegex = Pattern.compile( ".*(.)(min|max)" );
        Set<String> cnames = new HashSet<>();
        for ( EpnTapStage.SingleCol col :
              EpnTapStage.toSingleCols( EpnTapStage.getAllColumns() ) ) {
            String cname = col.name_;
            assertTrue( cnames.add( cname ) );
            checkUcd( col.ucd_, ucdParser );
            checkUnitEpn( col.unit_ );
            Matcher nameMatcher = minmaxRegex.matcher( cname );
            if ( nameMatcher.matches() ) {
                char preMinmaxChar = nameMatcher.group( 1 ).charAt( 0 );
                assertTrue( cname,
                            preMinmaxChar == '_' ||
                            preMinmaxChar == '1' ||
                            preMinmaxChar == '2' ||
                            preMinmaxChar == '3' );
            }
        }

        // Table 2.
        Set<String> ucdSet = new LinkedHashSet<>();
        Set<String> unitSet = new LinkedHashSet<>();
        for ( EpnTapStage.FrameType ftype : EpnTapStage.FrameType.values() ) {
            for ( String[] ucdOpts : ftype.ucds_ ) {
                for ( String ucd : ucdOpts ) {
                    ucdSet.add( ucd );
                }
            }
            for ( String unit : ftype.units_ ) {
                unitSet.add( unit );
            }
            for ( int i = 0; i < 3; i++ ) {
                assertEquals( ftype.isAngular_[ i ],
                              "deg".equals( ftype.units_[ i ] ) );
                ucdSet.add( ftype.resolUcd( i ) );
            }
        }
        ucdSet.remove( null );
        unitSet.remove( null );
        for ( String ucd : ucdSet ) {
            checkUcd( EpnTapStage.toMinUcd( ucd ), ucdParser );
            checkUcd( EpnTapStage.toMaxUcd( ucd ), ucdParser );
        }
        for ( String unit : unitSet ) {
            checkUnitEpn( unit );
        }
    }

    private void checkUcd( String ucd ) {
        checkUcd( ucd, UCDParser.defaultParser );
    }

    private void checkUcd( String ucd, UCDParser parser ) {
        if ( ucd != null ) {
            UCD pucd = parser.parse( ucd );
            if ( !pucd.isFullyValid() ) {
                for ( Iterator<String> errIt = pucd.getErrors();
                      errIt.hasNext(); ) {
                    System.err.println( "    " + errIt.next() );
                }
            }
            assertTrue( ucd, pucd.isFullyValid() );
        }
    }

    private void checkUnitEpn( String unit ) {
        if ( "bar".equals( unit ) ) {
            // Don't report.  EPN-TAP explicitly endorses the non-VOUnit
            // unit "bar".
        }
        else {
            checkUnit( unit );
        }
    }

    private void checkUnit( String unit ) {
        if ( unit != null && unit.length() > 0 ) {
            try {
                UnitExpr punit = new UnitParser( SYNTAX, unit ).getParsed();
                assertTrue( unit, punit.allUnitsRecognised( SYNTAX ) );
                assertTrue( unit, punit.allUnitsRecommended( SYNTAX ) );
                assertTrue( unit, punit.allUsageConstraintsSatisfied( SYNTAX ));
            }
            catch ( UnitParserException e ) {
                fail( "Bad unit \"" + unit + "\": " + e );
            }
        }
    }

    /**
     * Creates a UCDParser with default content, but which can be manipulated
     * without interfering with globals.
     */
    private static UCDParser createUCDParser() {
        UCDParser baseParser = UCDParser.defaultParser;
        UCDWordList words = new UCDWordList();
        for ( UCDWord w : baseParser.knownWords ) {
            words.add( w );
        }
        return new UCDParser( words, baseParser.deprecatedWords );
    }
}
