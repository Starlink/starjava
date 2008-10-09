package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.LineWord;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.util.LoadException;

/**
 * Programmatically generates example text and images for STILTS plotting tasks.
 * Used in generating the documentation.
 *
 * @author   Mark Taylor
 * @since    9 Oct 2008
 */
public class PlotExample {

    private final String name_;
    private final String taskName_;
    private final Executable exec_;
    private final String outFile_;
    private final String[] params_;
    private final String[] comments_;
    private static final Map INFILE_MAP = createInputFileMap();

    /**
     * Constructor.
     *
     * @param  name   example name
     * @param  taskName  name of the STILTS task being used
     * @param  params   array of name=value pairs giving parameters;
     *                  a null in the list signifies line break for formatting
     * @param  comments array of lines consituting the example description;
     *                  concatenated must form one or more XML &lt;p&gt;
     *                  elements
     */
    public PlotExample( String name, String taskName,
                        String[] params, String[] comments )
            throws TaskException, LoadException {
        name_ = name;
        taskName_ = taskName;
        params_ = params;
        comments_ = comments;

        Task task = (Task) Stilts.getTaskFactory().createObject( taskName );

        List wordList = new ArrayList();
        String omode = null;
        String out = null;
        String ofmt = null;
        for ( int ip = 0; ip < params.length; ip++ ) {
            if ( params[ ip ] != null && params[ ip ].trim().length() > 0 ) {
                String param = params[ ip ];
                param = param.trim();
                param = param.replaceAll( "'", "" );
                LineWord word = new LineWord( param );
                String paramName = word.getName();
                String paramValue = word.getValue();
                if ( "omode".equals( paramName ) ) {
                    omode = paramValue;
                }
                else if ( "out".equals( paramName ) ) {
                    out = paramValue;
                }
                else if ( "ofmt".equals( paramName ) ) {
                    ofmt = paramValue;
                }
                else if ( paramName.startsWith( "in" ) &&
                          INFILE_MAP.containsKey( paramValue ) ) {
                    word = new LineWord( paramName + "="
                                       + INFILE_MAP.get( paramValue ) );
                    wordList.add( word );
                }
                else {
                    wordList.add( word );
                }
            }
        }
        omode = "out";
        if ( out == null ) {
            if ( ofmt == null ) {
                ofmt = "png";
            }
            out = name_ + "." + ofmt;
        }
        wordList.add( new LineWord( "omode=" + omode ) );
        if ( ofmt != null ) {
            wordList.add( new LineWord( "ofmt=" + ofmt ) );
        }
        wordList.add( new LineWord( "out=" + out ) );

        LineWord[] words = (LineWord[]) wordList.toArray( new LineWord[ 0 ] );
        LineTableEnvironment env = new LineTableEnvironment();
        env.setWords( words );
        exec_ = task.createExecutable( env );
        outFile_ = out;
    }

    /**
     * Outputs the XML for this example as a &lt;dt&gt;&lt;dd&gt; element
     * pair.
     *
     * @param  out  destination stream
     */
    public void writeXml( PrintStream out ) throws IOException {
        String prefix = "stilts " + taskName_;
        String pad = prefix.replaceAll( ".", " " );
        out.println( "<dt><verbatim>" );
        out.print( prefix );
        for ( int ip = 0; ip < params_.length; ip++ ) {
            String param = params_[ ip ];
            if ( param == null || param.length() == 0 ) {
                out.println();
                out.print( pad );
            }
            else {
                out.print( ' ' );
                out.print( param );
            }
        }
        out.println();
        out.println( "</verbatim></dt>" );
        out.print( "<dd>" );
        for ( int i = 0; i < comments_.length; i++ ) {
            out.println( comments_[ i ] );
        }
        out.println( "<p>The generated plot is "
                   + "<webref url='../" + outFile_ + "'>here</webref>.</p>" );
        out.println( "</dd>" );
    }

    /**
     * Generates and outputs the image file showing the result of this
     * example.
     *
     * @return   name of the written file in the current directory
     */
    public String writeImage() throws IOException, TaskException {
        exec_.execute();
        return outFile_;
    }

    /**
     * Returns a map which indicates where to find input files referenced
     * in the table input parameters used by the example commands.
     * Map key is the name in the example, map value is the actual location.
     *
     * @return  name->location map
     */
    private static Map createInputFileMap() {
        Map map = new HashMap();
        map.put( "cat.xml", "/mbt/devel/text/cambridge2008/6dfgs_mini.xml" );
        map.put( "6dfgs_mini.xml",
                 "/mbt/devel/text/cambridge2008/6dfgs_mini.xml" );
        map.put( "2mass_xsc.fits", "/d2/scratch2/colfits/2mass_xsc.colfits" );
        map.put( "iras_psc.fits", "/mbt/data/survey/iras_psc.fits" );
        map.put( "messier.xml",
                 "/mbt/starjava/java/source/topcat/src/etc/demo/votable/" 
                 + "messier.xml" );
        return map;
    }

    /**
     * Returns a list of examples for the plot2d task.
     *
     * @return  example array
     */
    public static PlotExample[] createPlot2dExamples()
            throws TaskException, LoadException {
        return new PlotExample[] {
            new PlotExample( "xyplot", "plot2d", new String[] {
                    "in=cat.xml", "xdata=RMAG-BMAG", "ydata=BMAG", null,
                    "ofmt=eps", "out=xyplot.eps",
                },
                new String[] {
                    "<p>Plots a colour-magnitude diagram",
                    "writing the result to an Encapsulated Postscript file.",
                    "</p>",
                }
            ),

            new PlotExample( "xyplot2", "plot2d", new String[] {
                    "in=6dfgs_mini.xml", "xdata=RMAG-BMAG", "ydata=BMAG", null,
                    "subset1=SGFLAG==1", "name1=galaxy", "colour1=blue  ",
                           "shape1=open_circle", null,
                    "subset2=SGFLAG==2", "name2=star  ", "colour2=e010f0",
                           "shape2=x", "size2=3", null,
                    "xpix=500", "ypix=250", "xlo=-1", "xhi=4.5",
                           "ylo=10", "yhi=20", null,
                    "out=xyplot2.png"
                },
                new String[] {
                    "<p>Plots a colour-magnitude diagram with multiple",
                    "subsets.",
                    "</p>",
                }
            ),

            new PlotExample( "fatplot", "plot2d", new String[] {
                    "in1=iras_psc.fits",
                         "cmd1='addskycoords fk5 galactic RA DEC GLON GLAT'",
                         null, 
                    "xdata1=GLON", "ydata1=GLAT", null,
                    "auxdata1=FNU_100", "auxlog=true", "auxflip=true",
                         "size1=0", "transparency1=3", null,
                    "in2=messier.xml  ",
                          "cmd2='addskycoords fk5 galactic RA DEC GLON GLAT'",
                          null,
                    "xdata2=GLON", "ydata2=GLAT", null,
                    "txtlabel2=RADIUS>16?(\"M\"+ID):\"\"",
                          "cmd2='addcol SIZE sqrt(RADIUS/2)'", null,
                    "xerror2=SIZE", "yerror2=SIZE", null,
                    "subset2a=true", "hide2a=true", "colour2a=black",
                          "errstyle2a=ellipse", null,
                    "subset2b=true", "hide2b=true", "colour2b=black",
                          "errstyle2b=filled_ellipse", null,
                    "              transparency2b=6", null,
                    "xlabel='Galactic Longitude'", "ylabel='Galactic Latitude'",
                          "title='The Sky'", null,
                    "legend=false", "grid=false",
                          "fontsize=16", "fontstyle=bold-italic", null,
                    "xlo=0", "xhi=360", "ylo=-90", "yhi=+90", 
                          "xpix=800", "ypix=400", null,
                    "out=skyplot.eps",
                },
                new String[] {
                    "<p>You can do quite complicated things.",
                    "</p>",
                }
            ),
        };
    }

    /**
     * Writes a given set of examples.
     *
     * @param  name   base name of output file
     * @param  examples  array of examples to use
     */
    private static void writeExamples( String name, PlotExample[] examples )
            throws Exception {
        String filename = name + "-examples.xml";
        System.out.println( filename + ":" );
        OutputStream out = new FileOutputStream( filename );
        PrintStream pout = new PrintStream( new BufferedOutputStream( out ) );
        for ( int ie = 0; ie < examples.length; ie++ ) {
            PlotExample examp = examples[ ie ];
            examp.writeXml( pout );
            String gfile = examp.writeImage();
            System.out.println( "\t" + gfile );
        }
        pout.close();
    }

    /**
     * Writes example files ready for incorporation into documentation.
     */
    public static void main( String[] args ) throws Exception {
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.ttools.plot" )
              .setLevel( Level.SEVERE );
        writeExamples( "plot2d", createPlot2dExamples() );
    }
}
