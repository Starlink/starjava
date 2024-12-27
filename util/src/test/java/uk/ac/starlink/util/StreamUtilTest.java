package uk.ac.starlink.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamUtilTest {

    @Test
    public void testKeepInstances() {
        List<Object> numbers = Arrays.asList( new Object[] {
            Integer.valueOf( 1 ),
            Double.valueOf( 2 ),
            Integer.valueOf( 3 ),
            null,
            "five",
        } );
        assertEquals( 3, countInstances( numbers, Number.class ) );
        assertEquals( 2, countInstances( numbers, Integer.class ) );
        assertEquals( 0, countInstances( numbers, Thread.class ) );
        assertEquals( 4, countInstances( numbers, Object.class ) );
        assertEquals( "five",
                      numbers.stream()
                     .flatMap( StreamUtil.keepInstances( String.class ) )
                     .findFirst().get() );
    }

    private static int countInstances( Collection<?> collection,
                                       Class<?> keepClazz ) {
        return (int) 
               collection.stream()
              .flatMap( StreamUtil.keepInstances( keepClazz ) )
              .count();
    }
}
