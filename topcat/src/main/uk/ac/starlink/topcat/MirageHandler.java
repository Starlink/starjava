package uk.ac.starlink.topcat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import uk.ac.starlink.table.StarTable;

/**
 * Tests for and invokes the Mirage application if it is available.
 * This has to be done by reflection since (a) the uk.ac.starlink.mirage
 * package is dependent on the table package (this one) so that it 
 * will normally not be built before this one is being built,
 * and (b) the mirage.* classes themselves will not be available 
 * at all unless the user happens to have them on his classpath 
 * (they are not distributed with the starlink java system because
 * of licencing issues.
 * <p> 
 * If the viewer gets extracted into a separate package from the 
 * tables infrastructure, which might not be a bad idea, then only 
 * reason (b) above will apply.
 *
 * @author   Mark Taylor (Starlink)
 */
class MirageHandler {

    private static Method invokeMethod;

    /**
     * Returns true if calling the {@link #invokeMirage} method will work.
     */
    public static boolean isMirageAvailable() {
        if ( invokeMethod != null ) {
            return true;
        }
        try {

            /* Check that Mirage is on the path. */
            Class.forName( "mirage.Mirage", true, Thread.currentThread().getContextClassLoader() );

            /* Reflect as necessary. */
            Class driverClass = 
                Class.forName( "uk.ac.starlink.mirage.MirageDriver",
                               true, Thread.currentThread().getContextClassLoader());
            Class[] argTypes = new Class[] { StarTable.class, List.class };
            invokeMethod = driverClass.getMethod( "invokeMirage", argTypes );

            /* If we've got this far, we can invoke Mirage. */
            return true;
        }
        catch ( ClassNotFoundException e ) {
            return false;
        }
        catch ( NoSuchMethodException e ) {
            return false;
        }
        catch ( SecurityException e ) {
            return false;
        }
    }

    /**
     * Invokes Mirage.
     *
     * @param  startab  the StarTable to invoke it on
     * @param  margs  additional arguments for Mirage
     * @throws   UnsupportedOperationException   if {@link #isMirageAvailable}
     *           would return false
     */
    public static void invokeMirage( StarTable startab, List margs ) 
            throws Exception {
        if ( ! isMirageAvailable() ) {
            throw new UnsupportedOperationException( "No mirage" );
        }
        try {
            invokeMethod.invoke( null, new Object[] { startab, margs } );
        }
        catch ( InvocationTargetException e ) {
            Throwable target = e.getTargetException();
            if ( target instanceof Exception ) {
                throw (Exception) target;
            }
            else if ( target instanceof Error ) {
                throw (Error) target;
            }
            else {
                throw e;
            }
        }
    }
}
