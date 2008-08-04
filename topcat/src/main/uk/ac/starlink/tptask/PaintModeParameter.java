package uk.ac.starlink.tptask;

import java.awt.event.ActionEvent;
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
import uk.ac.starlink.tplot.GraphicExporter;
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

    private static final String OUT_MODE = "out";
    private static final String CGI_MODE = "cgi";
    private static final String SWING_MODE = "swing";
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
        super( name, new String[] { SWING_MODE, OUT_MODE, CGI_MODE, } );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "Output file for graphics" );
        outParam_.setDefault( null );
        outParam_.setNullPermitted( false );

        formatParam_ = new ChoiceParameter( "ofmt", EXPORTERS );
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
              .append( outParam_.getName() )
              .append( "</code>.\n" )
              .append( "</p>" );
        formatParam_.setDescription( fmtbuf.toString() );

        setPrompt( "Mode for graphical output" );
        setDescription( new String[] {
            "<p>Determines how the drawn plot will be output.",
            "<ul>",
            "<li><code>" + SWING_MODE + "</code>:",
            "Plot will be displayed in a window on the screen",
            "</li>",
            "<li><code>" + OUT_MODE + "</code>:",
            "Plot will be written to a file given by",
            "<code>" + outParam_.getName() + "</code>",
            "using the graphics format given by",
            "<code>" + formatParam_.getName() + "</code>.",
            "</li>",
            "<li><code>" + CGI_MODE + "</code>:",
            "Plot will be written in a way suitable for CGI use direct from",
            "a web server.",
            "The output is in the graphics format given by",
            "<code>" + formatParam_.getName() + "</code>,",
            "preceded by a suitable \"Content-type\" declaration.",
            "</li>",
            "</ul>",
            "</p>",
        } );
        setDefault( OUT_MODE );
    }

    /**
     * Returns parameters associated with this one.
     *
     * @return  parameter array
     */
    public Parameter[] getAssociatedParameters() {
        return new Parameter[] { outParam_, formatParam_ };
    }

    public void setValueFromString( Environment env, String stringVal )
            throws TaskException {
        super.setValueFromString( env, stringVal );
        String sval = (String) objectValue( env );
        painterValue_ = getPainter( env, sval );
    }

    /**
     * Returns a new painter object given an execution environment and string
     * value.
     * 
     * @param  env  execution environment
     * @param  sval  string representation of paint mode parameter value
     * @return  painter object
     */
    private Painter getPainter( Environment env, String sval ) 
            throws TaskException {
        if ( SWING_MODE.equals( sval ) ) {
            return new SwingPainter();
        }
        else if ( OUT_MODE.equals( sval ) ) {
            Destination dest = outParam_.destinationValue( env );
            String out = outParam_.stringValue( env );
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
                formatParam_.setDefault( dfltExp.getName() );
            }
            GraphicExporter exporter =
                (GraphicExporter) formatParam_.objectValue( env );
            return new OutputPainter( exporter, dest );
        }
        else if ( CGI_MODE.equals( sval ) ) {
            GraphicExporter exporter =
                (GraphicExporter) formatParam_.objectValue( env );
            return new CgiPainter( exporter, env.getOutputStream() );
        }
        else {
            assert false;
            throw new TaskException( "Unknown graphics output mode " + sval );
        }
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
     * Painter implementation which draws the plot in a Swing window.
     */
    private static class SwingPainter implements Painter {

        public void paintPlot( JComponent plot ) {
            final JFrame frame = new JFrame();
            frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
            frame.getContentPane().add( plot );
            Object quitKey = "quit";
            plot.getInputMap().put( KeyStroke.getKeyStroke( 'q' ), quitKey );
            plot.getActionMap().put( quitKey, new AbstractAction() {
                public void actionPerformed( ActionEvent evt ) {
                    frame.dispose();
                }
            } );
            frame.pack();
            frame.setVisible( true );
        }
        public boolean hasOutput() {
            return false;
        }
        public String toString() {
            return "swing";
        }
    }

    /**
     * Painter implementation which draws the plot to an external file.
     */
    private static class OutputPainter implements Painter {
        private final GraphicExporter exporter_;
        private final Destination dest_;

        /**
         * Constructor.
         *
         * @param  exporter  graphics file format-specific writer
         * @param  dest     destination output stream
         */
        OutputPainter( GraphicExporter exporter, Destination dest ) {
            exporter_ = exporter;
            dest_ = dest;
        }

        public void paintPlot( JComponent plot ) throws IOException {
            OutputStream out = new BufferedOutputStream( dest_.createStream() );
            try {
                exporter_.exportGraphic( plot, out );
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
    }

    /**
     * Painter implementation that writes a graphics file to an output stream
     * in a way suitable for CGI-bin operation.
     */
    private static class CgiPainter implements Painter {
        private final GraphicExporter exporter_;
        private final OutputStream out_;

        /**
         * Constructor.
         *
         * @param  exporter  graphics file format-specific writer
         * @param  out   output stream
         */
        CgiPainter( GraphicExporter exporter, OutputStream out ) {
            exporter_ = exporter;
            out_ = out;
        }

        public void paintPlot( JComponent plot ) throws IOException {
            String header = "Content-type: " + exporter_.getMimeType() + "\n\n";
            out_.write( header.getBytes( "UTF-8" ) );
            exporter_.exportGraphic( plot, out_ );
        }
    }
}
