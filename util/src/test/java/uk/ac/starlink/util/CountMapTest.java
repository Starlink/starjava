package uk.ac.starlink.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CountMapTest {

    @Test
    public void testCountMap() {
        CountMap<Number> cm = new CountMap<Number>();
        Number[] nums = new Number[] {
            BigDecimal.ONE,
            Long.valueOf( 2L ),
            Short.valueOf( (short) 3 ),
            Integer.valueOf( 4 ),
            Float.valueOf( 5f ),
        };
        assertTrue( cm.keySet().isEmpty() );
        for ( int in = 0; in < nums.length; in++ ) {
            Number num = nums[ in ];
            assertTrue( ! cm.keySet().contains( num ) );
            int count = num.intValue();
            for ( int j = 0; j < count; j++ ) {
                assertEquals( j, cm.getCount( num ) );
                cm.addItem( num );
            }
            assertTrue( cm.keySet().contains( num ) );
            assertEquals( count, cm.keySet().size() );
            assertEquals( count, cm.getCount( num ) );
        }
        for ( int in = 0; in < nums.length; in++ ) {
            Number num = nums[ in ];
            assertTrue( cm.keySet().contains( num ) );
            assertEquals( num.intValue(), cm.getCount( num ) );
        }
        assertEquals( 0, cm.getCount( Double.NaN ) );
        cm.clear();
        assertTrue( cm.keySet().isEmpty() );
        for ( int in = 0; in < nums.length; in++ ) {
            assertEquals( 0, cm.getCount( nums[ in ] ) );
        }
    }
}
