package uk.ac.starlink.frog.util;

import uk.ac.starlink.frog.Frog;
import uk.ac.starlink.frog.iface.DebugConsole;

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
     * Is the debugging console visible
     */
    protected boolean debugConsole;
       
    /**
     * Reference to the main Frog object
     */ 
    protected Frog frogMain = null;

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
        debugConsole = false;  // by default debugging is to the debug pane
    }
    
    /**
     * Constructor specifing the value of the debug flag
     *   
     *   @param debug If true debugging is on, otherwise debug message
     *                are not printed.
     *   @param debugConsole If true debugging messages are sent to the
     *                       console, otherwise messages are sent to the
     *                       debugging widget if it exists.
     */
    public FrogDebug( boolean b, boolean c)
    {
        debug = b;  
        debugConsole = c; 
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
     * Get the value of the package wide console flag
     *
     * @return debug Return the whether messages are sent to the console.
     */
    public boolean getConsoleFlag()
    {
        return debugConsole;
    }    
    
    /**
     * Get a reference to the instance of the main Frog class.
     */
     public Frog getFrog() 
     {
         return frogMain;
     }  
          
    /**
     * Print a debugging message
     *
     * @param string String will be printed only if debugging is on.
     */
     public void print( String s )
     {
         if( debug ) {
           
            if( debugConsole ) {
                System.out.println( s );
            } else {
            
                // get a refernce to the widget to which we need to send 
                // debugging information. This is a single instance class.
                DebugConsole consoleInstance = DebugConsole.getReference();           
                
                if( !consoleInstance.isVisible() ) {
                    consoleInstance.setVisible( true );
                }
                
                // send the message
                consoleInstance.write( s + "\n" );
            
            
            }    
                
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
     * Set a value for the package wide console flag
     *
     * @param flag Set the package wide console flag.
     */
    public void setConsoleFlag( boolean c)
    {
        debugConsole = c;
        
    }  
  
    /**
     * Set a reference to the instance of the main Frog class.
     */
     public void setFrog( Frog f) 
     {
         frogMain = f;
     }      
          
   /**  
     * Do garbage collection and output memory statistics.
     *
     * @param flag Report the amount of freed memory
     * @see RunTime
     */
    public void printMemoryStatistics( boolean report )
    {
        Runtime rt = Runtime.getRuntime();
        long isFree = rt.freeMemory();
        long origFree = isFree;
        long wasFree = 0;
        
        this.print("\nReporting memory statistics..." );
        do {
            wasFree = isFree;
            rt.gc();
            isFree = rt.freeMemory();
        } while ( isFree > wasFree );
        rt.runFinalization();
        if ( report ) {
            this.print( "\nGarbage Collection" );
            this.print( "------------------" );
            this.print( "  Freed memory = " + ( isFree - origFree ) );
            memStats();
        }
    }

    /**
     * Show current memory statistics.
     */
    public void memStats()
    {
        Runtime rt = Runtime.getRuntime();
        this.print( "\nMemory Statistics" );
        this.print( "-----------------" );
        this.print( "  Free memory = " + rt.freeMemory() );
        this.print( "  Used memory = " + ( rt.totalMemory() -
                                  rt.freeMemory() ) );
        this.print( "  Total memory = " + rt.totalMemory() );
    }
    

}
