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
 * Parameter which obtains a Painter object.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2008
 */
public class PaintModeParameter extends ChoiceParameter {

    private final OutputStreamParameter outParam_;
    private final ChoiceParameter formatParam_;
    private Painter painterValue_;

    private static final String OUTPARAM_NAME = "out";
    private static final String FORMATPARAM_NAME = "ofmt";
    private static final PaintMode SWING_MODE;
    private static final PaintMode OUTPUT_MODE;
    private static final PaintMode DEFAULT_MODE;
    private static final PaintMode[] MODES = new PaintMode[] {
        SWING_MODE = new SwingPaintMode(),
        OUTPUT_MODE = new OutputPaintMode(),
        new CgiPaintMode(),
        new DiscardPaintMode(),
        DEFAULT_MODE = new AutoPaintMode(),
    };
    private static final GraphicExporter[] EXPORTERS = new GraphicExporter[] {
        GraphicExporter.EPS,
        GraphicExporter.PNG,
        GraphicExporter.GIF,
        GraphicExporter.JPEG,
    };

    /**
     * Constructor.
     *
     * @param  name   parameter name
     */
    public PaintModeParameter( String name ) {
        super( name, MODES );
        StringBuffer modebuf = new StringBuffer()
            .append( "<p>Determines how the drawn plot will be output.\n" )
            .append( "<ul>\n" );
        for ( int im = 0; im < MODES.length; im++ ) {
            PaintMode mode = MODES[ im ];
            modebuf.append( "<li><code>" )
                   .append( mode.getName() )
                   .append( "</code>:\n" )
                   .append( mode.getDescription() )
                   .append( "</li>" )
                   .append( "\n" );
        }
        modebuf.append( "</ul>\n" )
               .append( "</p>" );
        setPrompt( "Mode for graphical output" );
        setDescription( modebuf.toString() );
        setDefault( DEFAULT_MODE.getName() );

        outParam_ = new OutputStreamParameter( OUTPARAM_NAME );
        outParam_.setPrompt( "Output file for graphics" );
        outParam_.setDefault( null );
        outParam_.setNullPermitted( false );

        formatParam_ = new ChoiceParameter( FORMATPARAM_NAME, EXPORTERS );
        formatParam_.setPrompt( "Graphics format for plot output" );
        StringBuffer fmtbuf = new StringBuffer()
            .append( "<p>Graphics format in which the plot is written to\n" )
            .append( "the output file.\n" )
            .append( "One of:\n" )
            .append( "<ul>\n" );
        for ( int ie = 0; ie < EXPORTERS.length; ie++ ) {
            GraphicExporter exporter = EXPORTERS[ ie ];
            fmtbuf.append( "<li><code>" )
                  .append( exporter.getName() )
                  .append( "</code>: " )
                  .append( exporter.getMimeType() )
                  .append( " format</li>" )
                  .append( "\n" );
        }
        fmtbuf.append( "</ul>\n" )
              .append( "May default to a sensible value depending on the\n" )
              .append( "filename given by " )
              .append( "<code>" )
              .append( OUTPARAM_NAME )
              .append( "</code>.\n" )
              .append( "</p>" );
        formatParam_.setDescription( fmtbuf.toString() );
    }

    /**
     * Returns the parameter determining the output stream (if any) 
     * to use for the graphics output.
     * Not relevant for all modes.
     *
     * @return   output parameter
     */
    public OutputStreamParameter getOutputParameter() {
        return outParam_;
    }

    /**
     * Returns the parameter giving the graphics format to use.
     * Not relevant for all modes.
     *
     * @return  format parameter
     */
    public ChoiceParameter getFormatParameter() {
        return formatParam_;
    }

    public void setValueFromString( Environment env, String stringVal )
            throws TaskException {
        super.setValueFromString( env, stringVal );
        PaintMode mode = (PaintMode) objectValue( env );
        painterValue_ = mode.createPainter( env, this );
    }

    /**
     * Sets the value for this parameter directly from a painter object.
     *
     * @param  painter  value for parameter
     */
    public void setValueFromPainter( Painter painter ) {
        painterValue_ = painter;
        setStringValue( painter.toString() );
        setGotValue( true );
    }

    /**
     * Returns the value of this parameter as a Painter object.
     *
     * @param  env  execution environment
     * @return  painter
     */
    public Painter painterValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return painterValue_;
    }

    /**
     * Defines a mode for disposing of a plot.
     */
    private static abstract class PaintMode {
        private final String name_;

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
         * @return   PCDATA
         */
        public abstract String getDescription();

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
    }

    /**
     * PaintMode implementation which displays the plot on the screen.
     */
    private static class SwingPaintMode extends PaintMode {
        SwingPaintMode() {
            super( "swing" );
        }

        public String getDescription() {
            return "Plot will be displayed in a window on the screen.";
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

        public String getDescription() {
            return "Plot will be written to a file given by "
                 + "<code>" + OUTPARAM_NAME + "</code> "
                 + "using the graphics format given by "
                 + "<code>" + FORMATPARAM_NAME + "</code>.";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            final Destination dest = param.outParam_.destinationValue( env );
            String out = param.outParam_.stringValue( env );
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
                param.formatParam_.setDefault( dfltExp.getName() );
            }
            final GraphicExporter exporter =
                (GraphicExporter) param.formatParam_.objectValue( env );
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

        public String getDescription() {
            return "Plot will be written in a way suitable for CGI use "
                 + "direct from a web server.\n"
                 + "The output is in the graphics format given by "
                 + "<code>" + FORMATPARAM_NAME + "</code>,\n"
                 + "preceded by a suitable \"Content-type\" declaration.";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            final GraphicExporter exporter =
                (GraphicExporter) param.formatParam_.objectValue( env );
            final OutputStream out = env.getOutputStream();
            return new Painter() {
                public void paintPlot( JComponent plot ) throws IOException {
                    BufferedOutputStream bout = new BufferedOutputStream( out );
                    String header = "Content-type: "
                                  + exporter.getMimeType()
                                  + "\n\n";
                    bout.write( header.getBytes( "UTF-8" ) );
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

        public String getDescription() {
            return "Plot is drawn, but discarded.  There is no output.";
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

        public String getDescription() {
            return "Behaves as "
                 + "<code>" + SWING_MODE + "</code> or "
                 + "<code>" + OUTPUT_MODE + "</code> mode"
                 + " depending on presence of "
                 + "<code>" + OUTPARAM_NAME + "</code> parameter";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param )
                throws TaskException {
            param.outParam_.setNullPermitted( true );
            param.formatParam_.setNullPermitted( true );
            if ( param.outParam_.stringValue( env ) == null ) {
                return SWING_MODE.createPainter( env, param );
            }
            else {
                return OUTPUT_MODE.createPainter( env, param );
            }
        }
    }
}
