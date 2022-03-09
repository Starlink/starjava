package uk.ac.starlink.ndtools;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.array.Combiner;
import uk.ac.starlink.task.TerminalInvoker;

/**
 * Utility applications for working with NDXs.
 * <p>
 * The <tt>main</tt> method of this class serves as a kind of monolith
 * for invoking utility methods.  It's a temporary measure pending 
 * having a proper parameter system, but makes it possible to write
 * and use some utility-type code for playing with the NDX system.
 * <p>
 * General usage is 
 * <pre>
 *    NDTools taskname args
 * <pre>
 * The taskname identifies a method of this class, and is not case sensitive.
 * The <tt>args</tt> on the command line are passed to that method
 * as an array of strings.  Where an argument refers to an NDX or NDArray,
 * its filename or URL may be used.  The format of the resource in
 * this case is determined by the form of the filename/URL - for instance
 * one ending with '.sdf' is treated as an NDF or HDS file.
 * The string "<tt>-</tt>" used for an output file means that the output
 * is XML read from standard input or written to standard output
 * respectively (this usage may not be available in all cases).
 * <p>
 * NDX URLs are resolved using the {@link uk.ac.starlink.ndx.NdxIO} class 
 * and whatever handlers are installed in it, which subject to availability
 * will be
 * <ul>
 * <li>{@link uk.ac.starlink.hds.NDFNdxHandler}
 * <li>{@link uk.ac.starlink.oldfits.FitsNdxHandler}
 * <li>{@link uk.ac.starlink.ndx.XMLNdxHandler}
 * </ul>
 * - see the documentation of those classes for URL formats.
 * <p>
 *
 * @author   Mark Taylor (Starlink)
 */
class NDTools {

    /**
     * Main method.
     * First args element is the taskname, subsequent elements are 
     * task-specific parameters.
     */
    public static void main( String[] args ) throws Exception {
        Combiner adder = new Combiner() {
            public double combination( double x, double y ) { return x + y; }
        };
        Combiner subtractor = new Combiner() {
            public double combination( double x, double y ) { return x - y; }
        };
        SumDoer cAdd = new SumDoer() {
            public double doSum( double var, double konst ) {
                return var + konst;
            }
        };
        SumDoer cSub = new SumDoer() {
            public double doSum( double var, double konst ) {
                return var - konst;
            }
        };
        SumDoer cMult = new SumDoer() {
            public double doSum( double var, double konst ) {
                return var * konst;
            }
        };
        SumDoer cDiv = new SumDoer() {
            public double doSum( double var, double konst ) {
                return var / konst;
            }
        };
 
        Map tasks = new HashMap();
        tasks.put( "copy", new Copy() );
        tasks.put( "stats", new Stats() );
        tasks.put( "settype", new SetType() );
        tasks.put( "add", new Combine( "Sum", adder, adder ) );
        tasks.put( "sub", new Combine( "Difference", subtractor, adder ) );
        tasks.put( "window", new Window() );
        tasks.put( "cadd", new ConstArithmetic( cAdd, null,
                                                "Adds a constant to an NDX" ) );
        tasks.put( "csub", new ConstArithmetic( cSub, null,
                                                "Subtracts a constant " +
                                                "from an NDX" ) );
        tasks.put( "cmult", new ConstArithmetic( cMult, cMult,
                                                 "Multiplies an NDX " +
                                                 "by a constant" ) );
        tasks.put( "cdiv", new ConstArithmetic( cDiv, cDiv,
                                                "Divides an NDX " +
                                                "by a constant" ) );
        tasks.put( "diff", new Diff() );
        // tasks.put( "block", new SlowBlock() );

        new TerminalInvoker( "NDTools", tasks ).invoke( args );
    }
}
