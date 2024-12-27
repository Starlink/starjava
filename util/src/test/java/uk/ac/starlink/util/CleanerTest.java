package uk.ac.starlink.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class CleanerTest {

    public void testCleaner() {

        // Don't run any tests automatically.
        // The test will only pass if garbage collection occurs when we
        // ask it to, and that is never guaranteed.
        // You can try running this test manually; if it works sometimes,
        // that probably indicates that the Cleaner class is working correctly.
        if ( false ) {
            runCleanerTest();
        }
    }

    @Test
    public void runCleanerTest() {
        int nc = 6;
        AtomicBoolean[] flags = new AtomicBoolean[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            flags[ i ] = new AtomicBoolean( false );
        }
        Cleaner cleaner = Cleaner.getInstance();
        Cleaner.Cleanable[] cleanables = new Cleaner.Cleanable[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            Object o = new Object();
            cleanables[ i ] = cleaner.register( o, new State( i, flags ) );
        }
        cleanables[ nc - 1 ].clean();
        assertTrue( flags[ nc - 1 ].get() );
        for ( int i = 0; i < nc - 1; i++ ) {
            assertFalse( flags[ i ].get() );
        }
        System.gc();
        new Thread( () -> {
            for ( int i = 0; i < nc; i++ ) {
                assertEquals( "fail " + i, flags[ i ].get() );
            }
        } ).start();
    }

    private static class State implements Runnable {
        final int ic_;
        final AtomicBoolean[] flags_;
        State( int ic, AtomicBoolean[] flags ) {
            ic_ = ic;
            flags_ = flags;
        }
        public void run() {
            assertFalse( flags_[ ic_ ].get() );
            flags_[ ic_ ].set( true );
            System.out.println( "Cleaning " + ic_ );
        }
    }
}
