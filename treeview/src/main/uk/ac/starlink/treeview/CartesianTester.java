package uk.ac.starlink.treeview;

import uk.ac.starlink.util.Tester;
import java.util.*;
import java.io.*;

/**
 * Tests the {@link Cartesian} class.
 * The <code>main</code> method can be used to perform the test.
 */
class CartesianTester extends Tester {

    public static void main( String[] args ) {
        CartesianTester tester = new CartesianTester();
        tester.logMessage( "Testing Cartesian" );
        tester.doTest();
    }

    public void testScript() throws Throwable {
        setComponent( "constructor" );
        Cartesian c1 = new Cartesian( 1 );
        Cartesian c7 = new Cartesian( 7 );
        Cartesian oblong = new Cartesian( new long[] { 2, 3 } );
        Cartesian monolith = new Cartesian( 3 );

        setComponent( "setCoord" );
        monolith.setCoord( 0, 1 );
        monolith.setCoord( 1, 4 );
        monolith.setCoord( 2, 9 );

        setComponent( "getCoord" );
        assertEqual( c1.getCoord( 0 ), 0 );

        setComponent( "getCoords" );
        long[] dims = oblong.getCoords();
        assertEqual( dims[ 0 ], 2 );
        assertEqual( dims[ 1 ], 3 );

        setComponent( "toString" );
        assertTrue( monolith.toString().equals( "( 1, 4, 9 )" ) );

        setComponent( "equals" );
        assertTrue( monolith
                   .equals( new Cartesian( new long[] { 1, 4, 9 } ) ) );
        assertTrue( ! monolith.equals( oblong ) );

        setComponent( "clone" );
        Cartesian twin = (Cartesian) monolith.clone();
        assertTrue( monolith.equals( twin ) );
        twin.setCoord( 2, 23 );
        assertTrue( ! monolith.equals( twin ) );

        setComponent( "numCells" );
        assertEqual( oblong.numCells(),
                     oblong.getCoord( 0 ) * oblong.getCoord( 1 ) );
        assertEqual( monolith.numCells(),
                     1 * 4 * 9 );

        setComponent( "cellIterator" );
        Cartesian[] cells = new Cartesian[ (int) oblong.numCells() ];
        Iterator cellIt = oblong.cellIterator();
        for ( int i = 0; cellIt.hasNext(); i++ ) {
            cells[ i ] = (Cartesian) cellIt.next();
        }
        assertTrue( cells[ 0 ].equals( new Cartesian( new long[] { 1, 1 } ) ) );
        assertTrue( cells[ 1 ].equals( new Cartesian( new long[] { 2, 1 } ) ) );
        assertTrue( cells[ 2 ].equals( new Cartesian( new long[] { 1, 2 } ) ) );
        assertTrue( cells[ 3 ].equals( new Cartesian( new long[] { 2, 2 } ) ) );
        assertTrue( cells[ 4 ].equals( new Cartesian( new long[] { 1, 3 } ) ) );
        assertTrue( cells[ 5 ].equals( new Cartesian( new long[] { 2, 3 } ) ) );
        assertTrue( ! cellIt.hasNext() );
        oblong.setCoord( 1, -3 );
        assertEqual( oblong.numCells(), -1 );
        assertTrue( ! oblong.cellIterator().hasNext() );
    }
}
