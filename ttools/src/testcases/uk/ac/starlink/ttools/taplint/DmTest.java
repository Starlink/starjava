package uk.ac.starlink.ttools.taplint;

import ari.ucidy.UCD;
import ari.ucidy.UCDParser;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import uk.me.nxg.unity.Syntax;
import uk.me.nxg.unity.UnitExpr;
import uk.me.nxg.unity.UnitParser;
import uk.me.nxg.unity.UnitParserException;

public class DmTest extends TestCase {

    private static final Syntax SYNTAX = Syntax.VOUNITS;

    public DmTest() {
        Logger.getLogger( "ari.ucidy" ).setLevel( Level.OFF );
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

    private void checkUcd( String ucd ) {
        if ( ucd != null ) {
            UCD pucd = UCDParser.parseUCD( ucd );
            assertTrue( ucd, UCDParser.parseUCD( ucd ).isFullyValid() );
        }
    }

    private void checkUnit( String unit ) {
        if ( unit != null ) {
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
}
