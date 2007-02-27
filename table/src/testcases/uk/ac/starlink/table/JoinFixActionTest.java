package uk.ac.starlink.table;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import junit.framework.TestCase;

public class JoinFixActionTest extends TestCase {

    static final Collection NAMES =
        Collections.unmodifiableSet( new HashSet( Arrays.asList( new String[] {
            "RA2000", "dec2000", "RMAG", "bmag",
        } ) ) );

    public JoinFixActionTest( String name ) {
        super( name );
    }

    public void testIsDuplicate() {
        assertTrue( JoinFixAction.isDuplicate( "RA2000", NAMES, true ) );
        assertTrue( JoinFixAction.isDuplicate( "RA2000", NAMES, false ) );
        assertTrue( ! JoinFixAction.isDuplicate( "ra2000", NAMES, true ) );
        assertTrue( JoinFixAction.isDuplicate( "ra2000", NAMES, false ) );
        assertTrue( ! JoinFixAction.isDuplicate( "nope", NAMES, true ) );
        assertTrue( ! JoinFixAction.isDuplicate( "nope", NAMES, false ) );
    }

    public void testEnsureUnique() {
        assertEquals( "uniq_",
                      JoinFixAction.ensureUnique( "uniq_", NAMES, true ) );
        assertEquals( "uniq_",
                      JoinFixAction.ensureUnique( "uniq_", NAMES, false ) );
        assertEquals( "BMAG",
                      JoinFixAction.ensureUnique( "BMAG", NAMES, true ) );
        assertEquals( "BMAGa",
                      JoinFixAction.ensureUnique( "BMAG", NAMES, false ) );
        Collection nn = new HashSet( NAMES );
        nn.add( "X_" );
        nn.add( "X_a" );
        nn.add( "X_b" );
        nn.add( "X_c" );
        nn.add( "X_e" );
        assertEquals( "X_d", JoinFixAction.ensureUnique( "X_", nn, true ) );
        assertEquals( "X_d", JoinFixAction.ensureUnique( "X_", nn, false ) );
    }

    public void testToLetters() {
        assertEquals( "a", JoinFixAction.toLetters( 0 ) );
        assertEquals( "z", JoinFixAction.toLetters( 25 ) );
        assertEquals( "ba", JoinFixAction.toLetters( 26 ) );
        assertEquals( "bb", JoinFixAction.toLetters( 27 ) );
        assertEquals( "bc", JoinFixAction.toLetters( 28 ) );
        assertEquals( "zzz", JoinFixAction.toLetters( 26 * 26 * 26 - 1 ) );
    }

    public void testNoAction() {
        assertEquals( "RA2000",
                      JoinFixAction.NO_ACTION.getFixedName( "RA2000", NAMES ) );
        assertEquals( "photoZ",
                      JoinFixAction.NO_ACTION.getFixedName( "photoZ", NAMES ) );
    }

    public void testRenameAll() {
        assertEquals( "RA2000_2",
                      JoinFixAction.makeRenameAllAction( "_2", true, true )
                     .getFixedName( "RA2000", NAMES ) );
        Collection nn = new HashSet( NAMES );
        nn.add( "RMAG_X" );
        assertEquals( "RMAG_X",
                      JoinFixAction.makeRenameAllAction( "_X", true, false )
                     .getFixedName( "RMAG", nn ) );
        assertEquals( "RMAG_Xa",
                      JoinFixAction.makeRenameAllAction( "_X", true, true )
                     .getFixedName( "RMAG", nn ) );
        assertEquals( "rmag_Xa",
                      JoinFixAction.makeRenameAllAction( "_X", false, true )
                     .getFixedName( "rmag", nn ) );
        nn.add( "RMAG_Xa" );
        assertEquals( "RMAG_Xb",
                      JoinFixAction.makeRenameAllAction( "_X", true, true )
                     .getFixedName( "RMAG", nn ) );
    }

    public void testRenameDuplicates() {
        assertEquals( "RA2000_2",
              JoinFixAction.makeRenameDuplicatesAction( "_2", true, true )
                           .getFixedName( "RA2000", NAMES ) );
        assertEquals( "RA3000",
              JoinFixAction.makeRenameDuplicatesAction( "_2", true, true )
                           .getFixedName( "RA3000", NAMES ) );
        Collection nn = new HashSet( NAMES );
        nn.add( "RMAG_X" );
        assertEquals( "RMAG_X",
              JoinFixAction.makeRenameDuplicatesAction( "_X", true, false )
                           .getFixedName( "RMAG", nn ) );
        assertEquals( "RMAG_Xa",
              JoinFixAction.makeRenameDuplicatesAction( "_X", true, true )
                           .getFixedName( "RMAG", nn ) );
        assertEquals( "rmag_Xa",
              JoinFixAction.makeRenameDuplicatesAction( "_X", false, true )
                           .getFixedName( "rmag", nn ) );
        nn.add( "RMAG_Xa" );
        assertEquals( "RMAG_Xb",
              JoinFixAction.makeRenameDuplicatesAction( "_X", true, true )
                           .getFixedName( "RMAG", nn ) );
    }

    public void testNumericDeduplication() {
        JoinFixAction dedupAct =
            JoinFixAction.makeNumericDeduplicationAction( "_", false );
        Collection names = new HashSet( Arrays.asList( new String[] { 
            "tt_1", "tt_2", "tt_4",
            "t",
        } ) );
        assertEquals( "tt_5", dedupAct.getFixedName( "tt_1", names ) );
        assertEquals( "t_1", dedupAct.getFixedName( "t", names ) );
        assertEquals( "T_1", dedupAct.getFixedName( "T", names ) );
    }
}
