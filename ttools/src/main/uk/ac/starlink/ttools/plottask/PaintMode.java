package uk.ac.starlink.ttools.plottask;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.PdfGraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.util.Destination;

/**
 * Defines a mode for disposing of a plot.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2008
 */
public abstract class PaintMode {

    private final String name_;

    /** Mode used for displaying a live plot on the display. */
    public static final PaintMode SWING_MODE = new SwingPaintMode();

    /** Mode used for standard output in CGI form. */
    public static final PaintMode CGI_MODE = new CgiPaintMode();

    /** Mode used to perform the plot internally, but produce no output. */
    public static final PaintMode DISCARD_MODE = new DiscardPaintMode();

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
     * Returns a list of all available paint modes, given a list of
     * available GraphicExporter objects.
     *
     * @param  exporters  known graphic exporters
     * @return  known paint modes
     */
    public static PaintMode[] getKnownModes( GraphicExporter[] exporters ) {
        OutputPaintMode outMode = new OutputPaintMode( exporters );
        return new PaintMode[] {
            SWING_MODE,
            outMode,
            CGI_MODE,
            DISCARD_MODE,
            new AutoPaintMode( outMode ),
        };
    }

    /**
     * PaintMode implementation which displays the plot on the screen.
     */
    private static class SwingPaintMode extends PaintMode {
        SwingPaintMode() {
            super( "swing" );
        }

        public String getDescription( PaintModeParameter modeParam ) {
            StringBuffer sbuf = new StringBuffer();
            return new StringBuffer()
                .append( "Plot will be displayed in a window on the screen.\n" )
                .append( "This plot is \"live\"; " )
                .append( "it can be resized and " )
                .append( "(except for old-style plots)\n" )
                .append( "navigated around with mouse actions " )
                .append( "in the same way as plots in TOPCAT." )
                .toString();
        }

        public String getModeUsage( PaintModeParameter modeParam ) {
            return "";
        }

        public Painter createPainter( Environment env,
                                      PaintModeParameter param ) {
            return new SwingPainter( "STILTS Plot" );
        }
    }

    /**
     * PaintMode implementation which writes the plot to an external file
     * in some graphics format.
     */
    private static class OutputPaintMode extends PaintMode {
        private final GraphicExporter[] exporters_;

        /**
         * Constructor.
         *
         * @param  exporters   list of available graphic exporters
         */
        OutputPaintMode( GraphicExporter[] exporters ) {
            super( "out" );
            exporters_ = exporters;
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Plot will be written to a file given by "
                 + "<code>" + modeParam.getOutputParameter().getName()
                 + "</code> "
                 + "using the graphics format given by "
                 + "<code>"
                 + "<ref id='graphicExporter'>"
                 + modeParam.getFormatParameter().getName()
                 + "</ref>"
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
            final Destination dest = outParam.objectValue( env );
            String out = outParam.stringValue( env );
            GraphicExporter dfltExp = null;
            if ( out != null ) {
                for ( int ie = 0; ie < exporters_.length; ie++ ) {
                    String[] fss = exporters_[ ie ].getFileSuffixes();
                    for ( int is = 0; is < fss.length; is++ ) {
                        if ( out.toLowerCase()
                                .endsWith( fss[ is ].toLowerCase() ) ) {
                            dfltExp = exporters_[ ie ];
                        }
                    }
                }
            }
            if ( dfltExp != null ) {
                formatParam.setStringDefault( dfltExp.getName() );
            }
            final GraphicExporter exporter =
                (GraphicExporter) formatParam.objectValue( env );
            return new Painter() {
                public void paintPicture( Picture picture ) throws IOException {
                    OutputStream out =
                        new BufferedOutputStream( dest.createStream() );
                    try {
                        exporter.exportGraphic( picture, out );
                    }
                    finally {
                        out.close();
                    }
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
                 + "<code>"
                 + "<ref id='graphicExporter'>"
                 + modeParam.getFormatParameter().getName()
                 + "</ref>"
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
                public void paintPicture( Picture picture ) throws IOException {
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
                    exporter.exportGraphic( picture, bout );
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
                public void paintPicture( Picture picture ) throws IOException {
                    Image image = createImage( picture.getPictureWidth(),
                                               picture.getPictureHeight() );
                    Graphics2D g2 = (Graphics2D) image.getGraphics();
                    picture.paintPicture( g2 );
                    g2.dispose();
                    image.flush();
                }
                private Image createImage( int w, int h ) {
                    GraphicsEnvironment genv =
                        GraphicsEnvironment.getLocalGraphicsEnvironment();
                    return genv.isHeadless()
                         ? (Image)
                           new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB )
                         : (Image) genv.getDefaultScreenDevice()
                                       .getDefaultConfiguration()
                                       .createCompatibleVolatileImage( w, h );
                }
            };
        }
    }

    /**
     * Automatic paint mode.  Works like a supplied OutputPaintMode
     * if output file is given, else works like SwingPaintMode.
     */
    private static class AutoPaintMode extends PaintMode {
        private final OutputPaintMode outMode_;

        /**
         * Constructor.
         *
         * @param  outMode  output mode for use if output file is given
         */
        protected AutoPaintMode( OutputPaintMode outMode ) {
            super( "auto" );
            outMode_ = outMode;
        }

        public String getDescription( PaintModeParameter modeParam ) {
            return "Behaves as "
                 + "<code><ref id='paintmode-swing'>" + SWING_MODE
                                                      + "</ref></code> or "
                 + "<code><ref id='paintmode-out'>" + outMode_
                                                    + "</ref></code> "
                 + " mode depending on presence of "
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
                return outMode_.createPainter( env, param );
            }
        }
    }
}
