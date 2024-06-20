package uk.ac.starlink.array;

import junit.framework.TestCase;

public class RequirementsTest extends TestCase {

    public RequirementsTest( String name ) {
        super( name );
    }

    public void testBlank() {
        Requirements req = new Requirements();
        assertNull( req.getBadHandler() );
        assertNull( req.getMode() );
        assertNull( req.getOrder() );
        assertTrue( ! req.getRandom() );
        assertNull( req.getType() );
        assertNull( req.getWindow() );
    }

    public void testSettings() {
        AccessMode mode = AccessMode.UPDATE; 
        Type type = Type.FLOAT;
        BadHandler bh = BadHandler.getHandler( Type.FLOAT,
                                               Float.valueOf( 1.5f ) );
        Order order = Order.ROW_MAJOR;
        NDShape window = new NDShape( new long[ 3 ], new long[] { 1, 4, 9 } );

        Requirements req = new Requirements( mode )
                          .setType( type )
                          .setBadHandler( bh )
                          .setOrder( order )
                          .setWindow( window )
                          .setRandom( true );
        checkValue( req, mode, type, bh, order, window, true );

        Requirements req2 = (Requirements) req.clone();
        checkValue( req2, mode, type, bh, order, window, true );
        assertTrue( ! ( req == req2 ) );

        req.setRandom( false );
        checkValue( req, mode, type, bh, order, window, false );

        req.setType( Type.SHORT );
        checkValue( req, mode, Type.SHORT, null, order, window, false );
 
        checkValue( req2, mode, type, bh, order, window, true );
        req2.setOrder( Order.COLUMN_MAJOR );
        checkValue( req2, mode, type, bh, Order.COLUMN_MAJOR, window, true );
        checkValue( req, mode, Type.SHORT, null, order, window, false );
    }

    public void testBad() {
        Type type = Type.SHORT;
        Number badval = Short.valueOf( (short) 99 );
        Requirements req = new Requirements()
                          .setType( Type.SHORT )
                          .setBadValue( badval );
        assertEquals( req.getBadHandler(),
                      BadHandler.getHandler( type, badval ) );
    }

    private void checkValue( Requirements req,
                             AccessMode mode, Type type, BadHandler bh,
                             Order order, NDShape window, boolean random ) {
        assertEquals( mode, req.getMode() );
        assertEquals( type, req.getType() );
        assertEquals( bh, req.getBadHandler() );
        assertEquals( order, req.getOrder() );
        assertEquals( window, req.getWindow() );
        assertEquals( random, req.getRandom() );
    }

    public void testExceptions() {
        Requirements req = new Requirements();
        BadHandler ibh = BadHandler.getHandler( Type.INT,
                                                Integer.valueOf( 23 ) );
        try {
            req.setBadHandler( ibh );
            fail();
        }
        catch ( IllegalStateException e ) {}

        req.setType( Type.FLOAT );
        try {
            req.setBadHandler( ibh );
            fail();
        }
        catch ( IllegalArgumentException e ) {}

        req.setType( Type.INT );
        req.setBadHandler( ibh );

        assertTrue( ibh == req.getBadHandler() );
    }
}
