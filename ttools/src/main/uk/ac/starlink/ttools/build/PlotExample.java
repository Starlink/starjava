package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.LineWord;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.LogUtils;

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
    private static final Map<String,String> INFILE_MAP = createInputFileMap();

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

        Task task = Stilts.getTaskFactory().createObject( taskName );

        List<LineWord> wordList = new ArrayList<LineWord>();
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

        LineWord[] words = wordList.toArray( new LineWord[ 0 ] );
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
                   + "<webref url='" + outFile_ + "'>here</webref>.</p>" );
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
    private static Map<String,String> createInputFileMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put( "cat.xml", "/mbt/devel/text/cambridge2008/6dfgs_mini.xml" );
        map.put( "6dfgs_mini.xml",
                 "/mbt/devel/text/cambridge2008/6dfgs_mini.xml" );
        map.put( "2mass_xsc.fits", "/mbt/data/survey/2mass_xsc.fits" );
        map.put( "iras_psc.fits", "/mbt/data/survey/iras_psc.fits" );
        map.put( "messier.xml",
                 "/mbt/starjava/source/topcat/src/etc/demo/votable/" 
                 + "messier.xml" );
        map.put( "sim1.fits", "/mbt/data/table/gavo_g1.fits" );
        map.put( "sim2.fits", "/mbt/data/table/gavo_g2.fits" );
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
                    "in=cat.xml", "xdata=RMAG-BMAG", "ydata=BMAG",
                },
                new String[] {
                    "<p>Plots a colour-magnitude diagram.",
                    "Since no <code>omode</code> or <code>out</code> value",
                    "has been specified, the plot is posted directly",
                    "to the graphics display for inspection.",
                    "By adding the parameter",
                    "<code>out=xyplot.eps</code>",
                    "the plot could be written to an",
                    "Encapsulated Postscript file instead.",
                    "</p>",
                }
            ),

            new PlotExample( "xyplot2", "plot2d", new String[] {
                    "in=6dfgs_mini.xml", "xdata=RMAG-BMAG", "ydata=BMAG", null,
                    "subset1=SGFLAG==1", "name1=galaxy", "colour1=blue  ",
                           "shape1=open_circle", null,
                    "subset2=SGFLAG==2", "name2=star  ", "colour2=e010f0",
                           "shape2=x", "size2=3", null,
                    "xlo=-1", "xhi=4.5", "ylo=10", "yhi=20",
                           "xpix=500", "ypix=250", null,
                    "out=xyplot2.png"
                },
                new String[] {
                    "<p>Plots a colour-magnitude diagram with multiple",
                    "subsets.",
                    "The subsets are labelled",
                    "\"<code>1</code>\" and \"<code>2</code>\"",
                    "with separate sets of parameters applying to each.",
                    "The selections for the sets are given by the",
                    "<code>subset*</code> parameters;",
                    "set 1 is those rows with the SGFLAG column equal to 1 and",
                    "set 2 is those rows with the SGFLAG column equal to 2.",
                    "The boundaries of the plot in data coordinates",
                    "are set explicitly rather than being determined from",
                    "the data (this is faster)",
                    "and the plot size in pixels is also set explicitly",
                    "rather than taking the default values.",
                    "Output is to a PNG file.",
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
                          "fontsize=12", "fontstyle=bold-italic", null,
                    "xlo=0", "xhi=360", "ylo=-90", "yhi=+90", 
                          "xpix=600", "ypix=300", null,
                    "out=skyplot.png",
                },
                new String[] {
                    "<p>You can do quite complicated things.",
                    "</p>",
                }
            ),
        };
    }

    /**
     * Returns a list of examples for the plot3d task.
     *
     * @return  example array
     */
    public static PlotExample[] createPlot3dExamples()
            throws TaskException, LoadException {
        return new PlotExample[] {
            new PlotExample( "xyzplot", "plot3d", new String[] {
                    "in=cat.xml", "xdata=RMAG", "ydata=BMAG", "zdata=VEL",
                    "zlog=true",
                },
                new String[] {
                    "<p>Plots a 3-d scatter plot of red magnitude vs.",
                    "blue magnitude vs. velocity; the velocity is plotted",
                    "on a logarithmic scale.",
                    "Since no <code>omode</code> or <code>out</code> value",
                    "has been specified, the plot is posted directly",
                    "to the graphics display for inspection.",
                    "By adding the parameter",
                    "<code>out=xyplot.eps</code>",
                    "the plot could be written to an",
                    "Encapsulated Postscript file instead.",
                    "</p>",
                }
            ),

            new PlotExample( "gavo2", "plot3d", new String[] {
                    "in=sim1.fits", "xdata=x", "ydata=y", "zdata=z", null,
                    "cmd='addcol vel \"sqrt(velx*velx+vely*vely+velz*velz)\"'",
                    "auxdata=vel", "auxlog=true", null,
                    "xpix=500", "ypix=400", "phi=50", "theta=10",
                    "out=cube.jpeg",
                },
                new String[] {
                    "<p>Plots the x, y, z positions of particles from a",
                    "file containing the result of a simulation run.",
                    "Here an auxiliary axis is used to colour-code the",
                    "points according their velocity.",
                    "This is done by introducing a new <code>vel</code>",
                    "column to the table using the",
                    "<ref id='addcol'><code>addcol</code></ref>",
                    "filter command, so that the <code>vel</code> column",
                    "can be used as the value for the <code>auxdata</code>",
                    "parameter.",
                    "Alternatively, the given expression for the velocity",
                    "could have been used directly as the value of the",
                    "<code>auxdata</code> parameter.",
                    "</p>",
                    "<p>Additionally, the <code>phi</code> and",
                    "<code>theta</code> parameters are given",
                    "to adjust the orientation of the cube.",
                    "</p>",
                }
            ),
        };
    }

    /**
     * Returns a list of examples for the plothist task.
     *
     * @return  example array
     */
    public static PlotExample[] createPlotHistExamples()
            throws TaskException, LoadException {
        return new PlotExample[] {
            new PlotExample( "hist0", "plothist", new String[] {
                    "in=cat.xml", "xdata=RMAG-BMAG",
                },
                new String[] {
                    "<p>Plots a histogram of the R-B colour.",
                    "The plot is displayed directly on the screen.",
                    "</p>",
                }
            ),

            new PlotExample( "hist0", "plothist", new String[] {
                    "in=cat.xml", "xdata=RMAG-BMAG", "ofmt=eps-gzip",
                    "out=hist.eps.gz",
                },
                new String[] {
                    "<p>Makes the same plot as the previous example,",
                    "but writes it to a gzipped encapsulated postscript file",
                    "instead of displaying it on the screen.",
                    "</p>",
                }
            ),

            new PlotExample( "hist1", "plothist", new String[] {
                    "inJ=2mass_xsc.fits", "xdataJ=j_m_k20fe", "barstyleJ=tops",
                             null,
                    "inH=2mass_xsc.fits", "xdataH=h_m_k20fe", "barstyleH=tops",
                             null,
                    "inK=2mass_xsc.fits", "xdataK=k_m_k20fe", "barstyleK=tops",
                             null,
                    "binwidth=0.1", "xlo=12", "xhi=16", "xflip=true",
                             "xlabel=Magnitude", "xpix=500", null,
                    "out=2mass.png",
                },
                new String[] {
                    "<p>Overplots histograms of three different columns",
                    "from the same input table.",
                    "These are treated as three separate datasets which all",
                    "happen to use the same input file.",
                    "The different datasets are labelled",
                    "\"<code>J</code>\",",
                    "\"<code>H</code>\" and",
                    "\"<code>K</code>\"",
                    "so these suffixes appear on all the dataset-dependent",
                    "parameters which are supplied.",
                    "The binwidth and X range are specified explicitly",
                    "rather than leaving them to be chosen automatically",
                    "by examining the data.",
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
     * @return  array of graphics filenames which have been written
     */
    private static String[] writeExamples( String name, PlotExample[] examples )
            throws Exception {
        String filename = name + "-examples.xml";
        System.out.println( filename + ":" );
        OutputStream out = new FileOutputStream( filename );
        PrintStream pout = new PrintStream( new BufferedOutputStream( out ) );
        String[] gfiles = new String[ examples.length ];
        for ( int ie = 0; ie < examples.length; ie++ ) {
            PlotExample examp = examples[ ie ];
            examp.writeXml( pout );
            String gfile = examp.writeImage();
            System.out.println( "\t" + gfile );
            gfiles[ ie ] = gfile;
        }
        pout.close();
        return gfiles;
    }

    /**
     * Writes example files ready for incorporation into documentation.
     */
    public static void main( String[] args ) throws Exception {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot" )
                .setLevel( Level.SEVERE );
        String[] plot2dFiles =
            writeExamples( "plot2d", createPlot2dExamples() );
        String[] plot3dFiles =
            writeExamples( "plot3d", createPlot3dExamples() );
        String[] histFiles =
            writeExamples( "plothist", createPlotHistExamples() );
        List<String> gfileList = new ArrayList<String>();
        gfileList.addAll( Arrays.asList( plot3dFiles ) );
        gfileList.addAll( Arrays.asList( plot2dFiles ) );
        gfileList.addAll( Arrays.asList( histFiles ) );
        String[] gfiles = gfileList.toArray( new String[ 0 ] );
        String gfName = "plot-example-files.txt";
        System.out.println( gfName );
        PrintStream gfOut = new PrintStream( new FileOutputStream( gfName ) );
        for ( int i = 0; i < gfiles.length; i++ ) {
            gfOut.println( gfiles[ i ] );
        }
        gfOut.close();
    }
}
