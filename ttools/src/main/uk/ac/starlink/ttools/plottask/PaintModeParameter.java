package uk.ac.starlink.ttools.plottask;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.GraphicExporter;

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

    /**
     * Constructor.
     *
     * @param  name   parameter name
     */
    public PaintModeParameter( String name ) {
        super( name, PaintMode.getKnownModes() );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "Output file for graphics" );
        outParam_.setDefault( null );
        outParam_.setNullPermitted( false );

        GraphicExporter[] exporters = PaintMode.getKnownExporters();
        formatParam_ = new ChoiceParameter( "ofmt", exporters );
        formatParam_.setPrompt( "Graphics format for plot output" );
        StringBuffer fmtbuf = new StringBuffer()
            .append( "<p>Graphics format in which the plot is written to\n" )
            .append( "the output file.\n" )
            .append( "One of:\n" )
            .append( "<ul>\n" );
        for ( int ie = 0; ie < exporters.length; ie++ ) {
            GraphicExporter exporter = exporters[ ie ];
            fmtbuf.append( "<li><code>" )
                  .append( exporter.getName() )
                  .append( "</code>: " )
                  .append( exporter.getMimeType() );
            String enc = exporter.getContentEncoding();
            if ( enc != null ) {
                fmtbuf.append( " (" )
                      .append( enc )
                      .append( ")" );
            }
            fmtbuf.append( " format</li>" )
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

        PaintMode[] modes = PaintMode.getKnownModes();
        StringBuffer modebuf = new StringBuffer()
            .append( "<p>Determines how the drawn plot will be output.\n" )
            .append( "<ul>\n" );
        for ( int im = 0; im < modes.length; im++ ) {
            PaintMode mode = modes[ im ];
            modebuf.append( "<li><code>" )
                   .append( mode.getName() )
                   .append( "</code>:\n" )
                   .append( mode.getDescription( this ) )
                   .append( "</li>" )
                   .append( "\n" );
        }
        modebuf.append( "</ul>\n" )
               .append( "</p>" );
        setDescription( modebuf.toString() );
        setPrompt( "Mode for graphical output" );
        setDefault( PaintMode.DEFAULT_MODE.getName() );
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
}
