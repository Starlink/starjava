package uk.ac.starlink.ttools.plottask;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.util.Destination;

/**
 * Defines a mode for disposing of a plot.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2008
 */
public abstract class PaintMode {

    private final String name_;

    /** Mode used by default. */
    public static final PaintMode DEFAULT_MODE;

    private static final PaintMode SWING_MODE;
    private static final PaintMode OUTPUT_MODE;
    private static final PaintMode[] MODES = new PaintMode[] {
        SWING_MODE = new SwingPaintMode(),
        OUTPUT_MODE = new OutputPaintMode(),
        new CgiPaintMode(),
        new DiscardPaintMode(),
        DEFAULT_MODE = new AutoPaintMode(),
    };
    private static final GraphicExporter[] EXPORTERS = new GraphicExporter[] {
        GraphicExporter.PNG,
        GraphicExporter.GIF,
        GraphicExporter.JPEG,
        GraphicExporter.PDF,
        GraphicExporter.EPS,
        GraphicExporter.EPS_GZIP,
        // Note there is another option for postscript - net.sf.epsgraphics.
        // On brief tests seems to work, may or may not produce more compact
        // output than jibble implementation.  Requires J2SE5 though.
    };

    /**
     * Constructor.
     *
     * @param  name  mode name
     */
    protected PaintMode( String name ) {
        name_ = name;
    }

    /**
     * Constructs a new painter object given the state of the environment.
     *
     * @param  env  execution environment
     * @param  param  paint mode parameter instance
     */
    public abstract Painter createPainter( Environment env,
                                           PaintModeParameter param )
            throws TaskException;

    /**
     * Returns a short XML description (no enclosing tag) of this mode's
     * behaviour.
     *
     * @param    modeParam  mode parameter for context
     * @return   PCDATA
     */
    public abstract String getDescription( PaintModeParameter modeParam );

    /**
     * Returns a short text usage message describing usage of associated
     * parameters, if any.  If no other parameters are referenced, 
     * an empty string should be returned.
     *
     * @param   modeParam  mode parameter for context
     * @return   plain text
     */
    public abstract String getModeUsage( PaintModeParameter modeParam );

    /**
     * Returns this mode's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    public String toString() {
        return getName();
    }

    /**
     * Returns a list of all available paint modes.
     *
     * @return  known paint modes
     */
    public static PaintMode[] getKnownModes() {
        return (PaintMode[]) MODES.clone();
    }

    /**
     * Returns a list of all available graphics exporters.
     *
     * @return   known graphic exporters
     */
    public static GraphicExporter[] getKnownExporters() {
        return (GraphicExporter[]) EXPORTERS.clone();
    }

    /**
     * PaintMode implementation which displays the plot on the screen.
     */
    private static class SwingPaintMode extends PaintMode {
        SwingPaintMode() {
            super( "swing" );
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Plot will be displayed in a window on the screen.";
        }

        public String getModeUsage( PaintModeParameter modeParam ) {
            return "";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param ) {
            return new Painter() {
                public void paintPlot( JComponent plot ) {

                    /* The mode may have got set to single-buffered by other
                     * bits of code - reverse this. */
                    // (I admit I'm not quite sure about double buffering)
                    plot.setDoubleBuffered( true );
                    plot.setPreferredSize( plot.getSize() );
                    final JFrame frame = new JFrame( "STILTS Plot" );
                    frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
                    frame.getContentPane().add( plot );
                    Object quitKey = "quit";
                    plot.getInputMap().put( KeyStroke.getKeyStroke( 'q' ),
                                            quitKey );
                    plot.getActionMap().put( quitKey, new AbstractAction() {
                        public void actionPerformed( ActionEvent evt ) {
                            frame.dispose();
                        }
                    } );
                    frame.pack();
                    frame.setVisible( true );
                }
            };
        }
    }

    /**
     * PaintMode implementation which writes the plot to an external file
     * in some graphics format.
     */
    private static class OutputPaintMode extends PaintMode {
        OutputPaintMode() {
            super( "out" );
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Plot will be written to a file given by "
                 + "<code>" + modeParam.getOutputParameter().getName()
                 + "</code> "
                 + "using the graphics format given by "
                 + "<code>" + modeParam.getFormatParameter().getName()
                 + "</code>.";
        }

        public String getModeUsage( PaintModeParameter modeParam ) {
            Parameter outParam = modeParam.getOutputParameter();
            Parameter formatParam = modeParam.getFormatParameter();
            return " " + outParam.getName() + "=" + outParam.getUsage()
                 + " " + formatParam.getName() + "=" + formatParam.getUsage();
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            OutputStreamParameter outParam = param.getOutputParameter();
            ChoiceParameter formatParam = param.getFormatParameter();
            final Destination dest = outParam.destinationValue( env );
            String out = outParam.stringValue( env );
            GraphicExporter dfltExp = null;
            if ( out != null ) {
                for ( int ie = 0; ie < EXPORTERS.length; ie++ ) {
                    String[] fss = EXPORTERS[ ie ].getFileSuffixes();
                    for ( int is = 0; is < fss.length; is++ ) {
                        if ( out.toLowerCase()
                                .endsWith( fss[ is ].toLowerCase() ) ) {
                            dfltExp = EXPORTERS[ ie ];
                        }
                    }
                }
            }
            if ( dfltExp != null ) {
                formatParam.setDefault( dfltExp.getName() );
            }
            final GraphicExporter exporter =
                (GraphicExporter) formatParam.objectValue( env );
            return new Painter() {
                public void paintPlot( JComponent plot ) throws IOException {
                    OutputStream out =
                        new BufferedOutputStream( dest.createStream() );
                    try {
                        exporter.exportGraphic( plot, out );
                    }
                    catch ( IOException e ) {
                        try {
                            out.close();
                        }
                        catch ( IOException e2 ) {
                            throw e;
                        }
                    }
                    out.close();
                }
            };
        }
    }

    /**
     * PaintMode implementation which writes the plot to the environment's
     * standard output in a way suitable for CGI-bin operation.
     */
    private static class CgiPaintMode extends PaintMode {
        CgiPaintMode() {
            super( "cgi" );
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Plot will be written in a way suitable for CGI use "
                 + "direct from a web server.\n"
                 + "The output is in the graphics format given by "
                 + "<code>" + modeParam.getFormatParameter().getName()
                 + "</code>,\n"
                 + "preceded by a suitable \"Content-type\" declaration.";
        }

        public String getModeUsage( PaintModeParameter modeParam ) {
            Parameter formatParam = modeParam.getFormatParameter();
            return " " + formatParam.getName() + "=" + formatParam.getUsage();
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            final GraphicExporter exporter =
                (GraphicExporter) param.getFormatParameter().objectValue( env );
            final OutputStream out = env.getOutputStream();
            return new Painter() {
                public void paintPlot( JComponent plot ) throws IOException {
                    BufferedOutputStream bout = new BufferedOutputStream( out );
                    StringBuffer hbuf = new StringBuffer();
                    hbuf.append( "Content-Type: " )
                        .append( exporter.getMimeType() )
                        .append( '\n' );
                    String encoding = exporter.getContentEncoding();
                    if ( encoding != null ) {
                        hbuf.append( "Content-Encoding: " )
                            .append( encoding )
                            .append( '\n' );
                    }
                    hbuf.append( '\n' );
                    bout.write( hbuf.toString().getBytes( "UTF-8" ) );
                    exporter.exportGraphic( plot, bout );
                    bout.flush();
                }
            };
        }
    }

    /**
     * PaintMode implementation which draws the plot but discards the result.
     */
    private static class DiscardPaintMode extends PaintMode {
        DiscardPaintMode() {
            super( "discard" );
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Plot is drawn, but discarded.  There is no output.";
        }

        public String getModeUsage( PaintModeParameter modeParam ) {
            return "";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            return new Painter() {
                public void paintPlot( JComponent plot ) {

                    /* Get a graphics context to render to, so that we actually
                     * do the plot. */
                    BufferedImage image =
                        new BufferedImage( 1, 1, BufferedImage.TYPE_INT_RGB );
                    Graphics2D g2 = image.createGraphics();
                    plot.print( g2 );
                    g2.dispose();
                }
            };
        }
    }

    /**
     * Automatic paint mode.  Works like OutputPaintMode if output file is
     * given, else works like SwingPaintMode.
     */
    private static class AutoPaintMode extends PaintMode {
        protected AutoPaintMode() {
            super( "auto" );
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Behaves as "
                 + "<code>" + SWING_MODE + "</code> or "
                 + "<code>" + OUTPUT_MODE + "</code> mode"
                 + " depending on presence of "
                 + "<code>" + modeParam.getOutputParameter().getName()
                            + "</code> parameter";
        }

        public String getModeUsage( PaintModeParameter modeParam ) {
            Parameter outParam = modeParam.getOutputParameter();
            return " " + "["
                       + outParam.getName() + "=" + outParam.getUsage()
                       + "]";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            OutputStreamParameter outParam = param.getOutputParameter();
            ChoiceParameter formatParam = param.getFormatParameter();
            outParam.setNullPermitted( true );
            if ( outParam.stringValue( env ) == null ) {
                return SWING_MODE.createPainter( env, param );
            }
            else {
                return OUTPUT_MODE.createPainter( env, param );
            }
        }
    }
}
