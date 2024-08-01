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
public class PaintModeParameter extends ChoiceParameter<PaintMode> {

    private final OutputStreamParameter outParam_;
    private final ChoiceParameter<GraphicExporter> formatParam_;

    /**
     * Constructor.
     *
     * @param  name   parameter name
     * @param  exporters   list of graphic exporters for file output options
     */
    @SuppressWarnings("this-escape")
    public PaintModeParameter( String name, GraphicExporter[] exporters ) {
        super( name, PaintMode.class, PaintMode.getKnownModes( exporters ) );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "Output file for graphics" );
        outParam_.setStringDefault( null );
        outParam_.setNullPermitted( false );

        formatParam_ =
            new ChoiceParameter<GraphicExporter>( "ofmt", exporters );
        formatParam_.setPrompt( "Graphics format for plot output" );
        StringBuffer fmtbuf = new StringBuffer()
            .append( "<p>Graphics format in which the plot is written to\n" )
            .append( "the output file, see <ref id='graphicExporter'/>.\n" )
            .append( "One of:\n" )
            .append( "<ul>\n" );
        for ( int ie = 0; ie < exporters.length; ie++ ) {
            GraphicExporter exporter = exporters[ ie ];
            fmtbuf.append( "<li><code>" )
                  .append( exporter.getName() )
                  .append( "</code>: " )
                  .append( exporter.getDescription() );
            fmtbuf.append( "</li>" )
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

        PaintMode[] modes = PaintMode.getKnownModes( exporters );
        StringBuffer modebuf = new StringBuffer()
            .append( "<p>Determines how the drawn plot will be output, " )
            .append( "see <ref id='paintMode'/>.\n" )
            .append( "<ul>\n" );
        for ( int im = 0; im < modes.length; im++ ) {
            PaintMode mode = modes[ im ];
            modebuf.append( "<li><code>" )
                   .append( "<ref id='paintmode-" )
                   .append( mode.getName() )
                   .append( "'>" )
                   .append( mode.getName() )
                   .append( "</ref>" )
                   .append( "</code>:\n" )
                   .append( mode.getDescription( this ) )
                   .append( "</li>" )
                   .append( "\n" );
        }
        modebuf.append( "</ul>\n" )
               .append( "</p>" );
        setDescription( modebuf.toString() );
        setPrompt( "Mode for graphical output" );

        /* Set the default as the final value.  This happens to be "auto"
         * at present.  If it wasn't, choosing the last one wouldn't
         * necessarily be a good choice. */
        setStringDefault( modes[ modes.length - 1 ].getName() );
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
    public ChoiceParameter<GraphicExporter> getFormatParameter() {
        return formatParam_;
    }

    /**
     * Sets the value for this parameter directly from a painter object.
     *
     * @param  painter  value for parameter
     */
    public void setValueFromPainter( Environment env, final Painter painter )
            throws TaskException {
        setValueFromObject( env, new PaintMode( painter.toString() ) {
            public Painter createPainter( Environment env,
                                          PaintModeParameter param ) {
                return painter;
            }
            public String getDescription( PaintModeParameter param ) {
                return null;
            }
            public String getModeUsage( PaintModeParameter param ) {
                return null;
            }
        } );
    }

    /**
     * Returns the value of this parameter as a Painter object.
     *
     * @param  env  execution environment
     * @return  painter
     */
    public Painter painterValue( Environment env ) throws TaskException {
        return objectValue( env ).createPainter( env, this );
    }
}
