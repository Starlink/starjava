package uk.ac.starlink.frog.util;

/**
 * Wrapper for the package wide debugging information. The class holds the
 * debugging flag, which is by default turned on. WIth the flag set to true
 * debugging messsages are printed to the standard output and the Debug menu
 * is enabled in the <CODE>Frog</CODE> class.
 *
 * @since $Date$
 * @author Alasdair Allan
 * @version $Id$
 * @see Frog
 * @see "The Singleton Design Pattern"
 */
public class FrogDebug
{
   /**
     *  Create the single class instance.
     */
    private static final FrogDebug instance = new FrogDebug();

    /**
     * Package wide debug flag
     */
    protected boolean debug;

    /**
     * Default constructor, by default debugging is on.
     */
     public FrogDebug()
     {
        this( true );
     }
    
    /**
     * Constructor specifing the value of the debug flag
     *   
     *   @param debug If true debugging is on, otherwise debug message
     *                are not printed.
     */
    public FrogDebug( boolean b)
    {
        debug = b;   
    }

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference reference to the instance of this class.
     */
    public static FrogDebug getReference()
    {
        return instance;
    }
        
    /**
     * Get the value of the package wide debugging flag
     *
     * @return debug Return the debug status.
     */
    public boolean getDebugFlag()
    {
        return debug;
    }
    
    /**
     * Print a debugging message
     *
     * @param string String will be printed only if debugging is on.
     */
     public void print( String s )
     {
         if( debug ) {
            System.out.println( s );
         }   
     }
    
    /**
     * Set a value for the package wide debugging flag
     *
     * @param flag Set the package wide debugging flag.
     */
    public void setDebugFlag( boolean b)
    {
        debug = b;
    }
    
   /**  
     * Do garbage collection and output memory statistics.
     *
     * @param flag Report the amount of freed memory
     * @see RunTime
     */
    public static void printMemoryStatistics( boolean report )
    {
        Runtime rt = Runtime.getRuntime();
        long isFree = rt.freeMemory();
        long origFree = isFree;
        long wasFree = 0;
        
        System.out.println("\nReporting memory statistics..." );
        do {
            wasFree = isFree;
            rt.gc();
            isFree = rt.freeMemory();
        } while ( isFree > wasFree );
        rt.runFinalization();
        if ( report ) {
            System.out.println( "\nGarbage Collection" );
            System.out.println( "------------------" );
            System.out.println( "  Freed memory = " + ( isFree - origFree ) );
            memStats();
        }
    }

    /**
     * Show current memory statistics.
     */
    public static void memStats()
    {
        Runtime rt = Runtime.getRuntime();
        System.out.println( "\nMemory Statistics" );
        System.out.println( "-----------------" );
        System.out.println( "  Free memory = " + rt.freeMemory() );
        System.out.println( "  Used memory = " + ( rt.totalMemory() -
                                                 rt.freeMemory() ) );
        System.out.println( "  Total memory = " + rt.totalMemory() );
    }
    

}
