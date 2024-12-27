package uk.ac.starlink.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiplexTest {

    @Test
    public void testProxy() {
        List<Number> l1 = new ArrayList<Number>();
        List<Number> l2 = new LinkedList<Number>();

        // You could use the parameterised type <List<Number>>, but it wouldn't
        // help; the method createMultiplexer takes a Class<T> parameter,
        // and you can't use a parameterised type for that T.
        MultiplexInvocationHandler<List> mih =
            new MultiplexInvocationHandler<List>( new List[] { l1, l2 } );
        List mList = mih.createMultiplexer( List.class );
        @SuppressWarnings( "unchecked" )
        List<Number> nmList = (List<Number>) mList;
        checkLists( nmList, l1, l2 );
    }

    private void checkLists( Collection<? super Number> multiList,
                             Collection<? super Number> l1,
                             Collection<? super Number> l2  ) {
        Double d1 = Double.valueOf( 10 );
        Double d2 = Double.valueOf( 20 );
        multiList.add( d1 );
        multiList.add( d2 );
        assertEquals( Arrays.asList( new Number[] { d1, d2 } ), multiList );
        assertEquals( multiList, l1 );
        assertEquals( multiList, l2 );
        multiList.clear();
        assertTrue( multiList.isEmpty() );
        assertTrue( l1.isEmpty() );
        assertTrue( l2.isEmpty() );
        l1.add( Integer.valueOf( 23 ) );
        assertTrue( ! l1.isEmpty() );
        assertTrue( l2.isEmpty() );
    }
}
