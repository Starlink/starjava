package uk.ac.starlink.array;

import java.util.List;
import junit.framework.TestCase;

public class OrderTest extends TestCase {

    public OrderTest( String name ) {
        super( name );
    }

    public void testOrder() {
        List orders = Order.allOrders();
        assertEquals( 2, orders.size() );
        assertTrue( orders.contains( Order.COLUMN_MAJOR ) );
        assertTrue( orders.contains( Order.ROW_MAJOR ) );
        assertTrue( Order.ROW_MAJOR != Order.COLUMN_MAJOR );
        assertTrue( Order.COLUMN_MAJOR.isFitsLike() );
        assertTrue( ! Order.ROW_MAJOR.isFitsLike() );
    }
}
