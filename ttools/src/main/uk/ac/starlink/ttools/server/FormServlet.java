package uk.ac.starlink.ttools.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.task.StiltsServer;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Servlet which provides some example forms to drive the STILTS server.
 * These are provided mainly by way of example.
 *
 * @author   Mark Taylor
 * @since    8 Oct 2008
 */
public class FormServlet extends HttpServlet {

    private ObjectFactory taskFactory_;
    private StarTableFactory tableFactory_;
    private String taskBase_;
    private Map formWriterMap_;

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        StiltsContext sContext =
            new StiltsContext( config.getServletContext() );
        taskBase_ = sContext.getTaskBase();
        taskFactory_ = Stilts.getTaskFactory();
        tableFactory_ = sContext.getTableFactory();

        Map fwmap = new HashMap();
        FormWriter[] fwriters;
        try { 
            fwriters = getFormWriters();
        }
        catch ( LoadException e ) {
            throw new ServletException( e );
        }
        for ( int iw = 0; iw < fwriters.length; iw++ ) {
            FormWriter fw = fwriters[ iw ];
            fwmap.put( fw.getName(), fw );
        }
        formWriterMap_ = Collections.unmodifiableMap( fwmap );
    }

    public void destroy() {
        super.destroy();
    }

    public String getServletInfo() {
        return "STILTS plot form generator";
    }

    protected void doGet( HttpServletRequest request,
                          HttpServletResponse response )
            throws IOException, ServletException {
        try {
            process( request, response );
        }
        catch ( Throwable e ) {
            replyError( response, 500, e );
        }
    }

    /**
     * Does the work for processing a form request.
     *
     * @param  request  request
     * @param  response response
     */
    protected void process( HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException {
        response.setHeader( "STILTS-Version", Stilts.getVersion() );
        String subpath = request.getPathInfo();
        String baseUrl = request.getServletPath();

        /* Get a FormWriter corresponding to the request. */
        String taskName = subpath == null ? ""
                                          : subpath.replaceAll( "^/*", "" );
        FormWriter fwriter = (FormWriter) formWriterMap_.get( taskName );

        /* If the request has no task part, send back a menu page 
         * with OK status. */
        if ( taskName.length() == 0 ) {
            response.setStatus( 200 );
            response.setContentType( "text/html" );
            ServletOutputStream out = response.getOutputStream();
            out.println( "<html>" );
            out.println( "<head><title>Sample STILTS Forms</title></head>" );
            out.println( "<body>" );
            out.println( "<h2>Sample STILTS Forms</h2>" );
            out.println( "<dl>" );
            out.println( "<dt><b>Histogram</b></dt>" );
            out.println( "<dd>" );
            writeSampleForm( baseUrl, out, "plothist" );
            out.println( "</dd>" );
            out.println( "<dt><b>2D Scatter Plot</b></dt>" );
            out.println( "<dd>" );
            writeSampleForm( baseUrl, out, "plot2d" );
            out.println( "</dd>" );
            out.println( "<dt><b>3D Scatter Plot</b></dt>" );
            out.println( "<dd>" );
            writeSampleForm( baseUrl, out, "plot3d" );
            out.println( "</dd>" );
            out.println( "</dl>" );
            out.println( "</body>" );
            out.println( "</html>" );
            out.close();
        }

        /* If the request is for a form not provided, send back a menu with
         * error status. */
        if ( fwriter == null ) {
            response.setStatus( 400 );
            response.setContentType( "text/html" );
            ServletOutputStream out = response.getOutputStream();
            out.println( "<html>" );
            out.println( "<head><title>No such form</title></head>" );
            out.println( "<body>" );
            out.println( "<h2>No such form</h2>" );
            out.println( "<h3>Known forms:</h3>" );
            out.println( "<ul>" );
            for ( Iterator it = formWriterMap_.keySet().iterator();
                  it.hasNext(); ) {
                String name = (String) it.next();
                out.println( "<li><a href='" + baseUrl + "/" + name + "'>"
                           + name + "</a></li>" );
            }
            out.println( "</ul>" );
            out.println( "</body>" );
            out.println( "</html>" );
            out.close();
        }

        /* Otherwise, try to provide a suitable form. */
        else {

            /* Identify the table requested.  This is required so that 
             * column names can be filled in. */
            String tableName = request.getParameter( "table" );
            StarTable table;
            if ( tableName == null ) {
                response.setStatus( 400 );
                response.setContentType( "text/plain" );
                ServletOutputStream out = response.getOutputStream();
                out.println( "Parameter table not specified" );
                out.close();
                return;
            }
            try {
                table = tableFactory_.makeStarTable( tableName );
            }
            catch ( IOException e ) {
                replyError( response, 400, e );
                return;
            }

            /* Generate and return a form appropriate to the request. */
            response.setStatus( 200 );
            response.setContentType( "text/html" );
            ServletOutputStream out = response.getOutputStream();
            String head = taskName + " form for table " + tableName;
            out.println( "<html>" );
            out.println( "<head><title>" + head + "</title></head>" );
            out.println( "<body>" );
            out.println( "<h2>" + head + "</h2>" );
            fwriter.writeForm( out, table, tableName );
            out.println( "</body>" );
            out.println( "</html>" );
            out.close();
        }
    }

    /**
     * Outputs a short form which can invoke one of the forms provided by 
     * this servlet.  It selects a table and invokes the right form.
     *
     * @param   baseUrl  base URL for this servlet
     * @param   out   response output stream
     * @param   taskName   name of task form will invoke
     */
    private final void writeSampleForm( String baseUrl, ServletOutputStream out,
                                        String taskName )
            throws IOException {
        out.println( "<form action='" + baseUrl + "/" + taskName + "'"
                   + " method='GET'>" );
        out.println( "Table: " );
        out.println( "<input type='text' name='table'/>" );
        out.println( "<input type='submit' value='" + taskName + " form'/>" );
        out.println( "</form>" );
    }

    /**
     * Defines an object which can write a form for invoking one of the
     * STILTS server tasks.
     */
    private abstract class FormWriter {
        private final String taskName_;
        private final Task task_;
        private final Map paramMap_;

        /**
         * Constructor.
         *
         * @param   taskName  name of the task
         */
        public FormWriter( String taskName ) throws LoadException {
            taskName_ = taskName;
            task_ = (Task) taskFactory_.createObject( taskName );
            paramMap_ = new HashMap();
            Parameter[] params = task_.getParameters();
            for ( int ip = 0; ip < params.length; ip++ ) {
                paramMap_.put( params[ ip ].getName(), params[ ip ] );
            }
        }

        /**
         * Writes the form.
         *
         * @param   out  response output stream
         * @param   table   table on which the form will operate
         * @param   tableName  name by which the table can be identified
         */
        public void writeForm( ServletOutputStream out, StarTable table,
                               String tableName )
                throws IOException {
            out.println( "<form action='" + taskBase_ + "/" + taskName_ + "'"
                       + " method='GET'" + ">" );  // could use POST
            out.println( "<input type='hidden' name='in'"
                       + " value='" + tableName + "'/>" );
            writeControls( out, table );
            out.println( "<br />" );
            out.println( "<input type='submit' value='Plot'/>" );
            out.println( "</form>" );
        }

        /**
         * Writes the body of the form.
         * All the controls apart from the table identifier should be
         * written here.
         *
         * @param   out  response output stream
         * @param   table   table on which the form will operate
         */
        public abstract void writeControls( ServletOutputStream out,
                                            StarTable table )
                throws IOException;

        /**
         * Writes a control to select a column/JEL expression value.
         *
         * @param   out  response output stream
         * @param   name  parameter name provided by control
         * @param   table  table on which the control will operate
         * @param   clazz  required content class superclass for column
         */
        protected void writeColumnControl( ServletOutputStream out, String name,
                                           StarTable table, Class clazz )
                throws IOException {
            out.println( "<dt><b>" + name + ":</b> <i>(required)</i></dt>" );
            out.println( "<dd>" );
            ColumnInfo[] infos = Tables.getColumnInfos( table );
            out.println( "Select column " );
            out.println( "<select name='" + name + "'"
                       + " id='" + name + "'" + ">" );
            out.println( "<option></option>" );
            for ( int ic = 0; ic < infos.length; ic++ ) {
                ColumnInfo info = infos[ ic ];
                if ( clazz.isAssignableFrom( info.getContentClass() ) ) {
                    out.println( "<option>" + info.getName() + "</option>" );
                }
            }
            out.println( "</select>" );
            out.println( " or enter expression " );
            out.println( "<input type='text' name='" + name + "'" + ">" );
            out.println( "</dd>" );
        }

        /**
         * Writes a control to select a text value.
         *
         * @param   out   servlet output stream
         * @param   name   parameter name provided by control
         * @param   param  parameter object being specified
         */
        protected void writeTextControl( ServletOutputStream out, String name,
                                         Parameter param )
                throws IOException {
            String dflt = param.getDefault();
            out.println( "<dt><b>" + name + ":</b></dt>" );
            out.println( "<dd>" );
            out.print( "<input type='text' name='" + name + "'" );
            if ( dflt != null && dflt.length() > 0 ) {
                out.print( " value='" + dflt + "'" );
            }
            out.println( "/>" );
            out.println( "</dd>" );
        }

        /**
         * Writes a control to select a multiple choice value.
         *
         * @param   out   servlet output stream
         * @param   name   parameter name provided by control
         * @param   param  parameter object being specified
         */
        protected void writeOptionControl( ServletOutputStream out, String name,
                                           ChoiceParameter param )
                throws IOException {
            String dflt = param.getDefault();
            out.println( "<dt><b>" + name + ":</b></dt>" );
            out.println( "<dd>" );
            out.print( "<select name='" + name + "'" );
            if ( dflt != null && dflt.length() > 0 ) {
                out.print( " value='" + dflt + "'" );
            }
            out.println( "/>" );
            String[] options = param.getOptionNames();
            for ( int io = 0; io < options.length; io++ ) {
                out.println( "<option>" + options[ io ] + "</option>" );
            }
            out.println( "</select>" );
            out.println( "</dd>" );
        }

        /**
         * Writes a control to select a boolean value.
         *
         * @param   out   servlet output stream
         * @param   name   parameter name provided by control
         * @param   param  parameter object being specified
         */
        protected void writeBooleanControl( ServletOutputStream out,
                                            String name, Parameter param )
                throws IOException {
            out.println( "<dt><b>" + name + ":</b></dt>" );
            out.println( "<dd>" );
            out.print( "<input type='checkbox' value='true'"
                     + " name='" + name + "'" );
            if ( "true".equalsIgnoreCase( param.getDefault() ) ) {
                out.print( " checked='checked'" );
            }
            out.println( "/>" );
            out.println( "</dd>" );
        }

        /**
         * Returns a parameter owned by this object's associated task
         * and identified by a given name.
         *
         * @param  paramName  parameter name as known by task
         * @return   parameter object
         */
        protected Parameter getParameter( String paramName ) {
            return (Parameter) paramMap_.get( paramName );
        }

        /**
         * Returns the name of the task associated with this object.
         *
         * @return   task name
         */
        public String getName() {
            return taskName_;
        }
    }

    /**
     * Returns all known form writers.
     *
     * @return   new list of writers
     */
    private FormWriter[] getFormWriters() throws LoadException {
        return new FormWriter[] {
            new Plot2dFormWriter(),
            new Plot3dFormWriter(),
            new HistogramFormWriter(),
        };
    }

    /**
     * Writes error information to the response.
     *
     * @param   response  destination
     * @param   code   3-digit HTTP response code
     * @param   error  exception to be passed to caller
     */
    private void replyError( HttpServletResponse response, int code,
                             Throwable error )
            throws IOException, ServletException {
        if ( response.isCommitted() ) {
            throw new ServletException( "Error after response commit", error );
        }
        else {
            response.setStatus( code );
            response.setContentType( "text/plain" );
            PrintStream pout = new PrintStream( response.getOutputStream() );
            error.printStackTrace( pout );
            pout.flush();
            pout.close();
        }
    }

    /**
     * FormWriter for plot2d task.
     */
    private class Plot2dFormWriter extends FormWriter {
        public Plot2dFormWriter() throws LoadException {
            super( "plot2d" );
        }

        public void writeControls( ServletOutputStream out, StarTable table )
                throws IOException {
            out.println( "<dl>" );
            out.println( "<dt><b>X Axis</b></dt>" );
            out.println( "<dd><dl>" );
            writeColumnControl( out, "xdata", table, Number.class );
            writeTextControl( out, "xlo", getParameter( "xlo" ) );
            writeTextControl( out, "xhi", getParameter( "xhi" ) );
            writeBooleanControl( out, "xlog", getParameter( "xlog" ) );
            writeBooleanControl( out, "xflip", getParameter( "xflip" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Y Axis</b></dt>" );
            out.println( "<dd><dl>" );
            writeColumnControl( out, "ydata", table, Number.class );
            writeTextControl( out, "ylo", getParameter( "ylo" ) );
            writeTextControl( out, "yhi", getParameter( "yhi" ) );
            writeBooleanControl( out, "ylog", getParameter( "ylog" ) );
            writeBooleanControl( out, "yflip", getParameter( "yflip" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Image dimensions</b></dt>" );
            out.println( "<dd><dl>" );
            writeTextControl( out, "xpix", getParameter( "xpix" ) );
            writeTextControl( out, "ypix", getParameter( "ypix" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Output Format</b></dt>" );
            out.println( "<dl><dd>" );
            ChoiceParameter fmtParam = (ChoiceParameter) getParameter( "ofmt" );
            fmtParam.setDefaultOption( fmtParam.getOptions()[ 0 ] );
            writeOptionControl( out, "ofmt", fmtParam );
            out.println( "</dd></dl>" );
            out.println( "</dd>" );
            out.println( "</dl>" );
        }
    }

    /**
     * FormWriter for plot3d task.
     */
    private class Plot3dFormWriter extends FormWriter {
        public Plot3dFormWriter() throws LoadException {
            super( "plot3d" );
        }

        public void writeControls( ServletOutputStream out, StarTable table )
                throws IOException {
            out.println( "<dl>" );

            out.println( "<dt><b>X Axis</b></dt>" );
            out.println( "<dd><dl>" );
            writeColumnControl( out, "xdata", table, Number.class );
            writeTextControl( out, "xlo", getParameter( "xlo" ) );
            writeTextControl( out, "xhi", getParameter( "xhi" ) );
            writeBooleanControl( out, "xlog", getParameter( "xlog" ) );
            writeBooleanControl( out, "xflip", getParameter( "xflip" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Y Axis</b></dt>" );
            out.println( "<dd><dl>" );
            writeColumnControl( out, "ydata", table, Number.class );
            writeTextControl( out, "ylo", getParameter( "ylo" ) );
            writeTextControl( out, "yhi", getParameter( "yhi" ) );
            writeBooleanControl( out, "ylog", getParameter( "ylog" ) );
            writeBooleanControl( out, "yflip", getParameter( "yflip" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Z Axis</b></dt>" );
            out.println( "<dd><dl>" );
            writeColumnControl( out, "zdata", table, Number.class );
            writeTextControl( out, "zlo", getParameter( "zlo" ) );
            writeTextControl( out, "zhi", getParameter( "zhi" ) );
            writeBooleanControl( out, "zlog", getParameter( "zlog" ) );
            writeBooleanControl( out, "zflip", getParameter( "zflip" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Image Rotation</b></dt>" );
            out.println( "<dd><dl>" );
            writeTextControl( out, "phi", getParameter( "phi" ) );
            writeTextControl( out, "theta", getParameter( "theta" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Image dimensions</b></dt>" );
            out.println( "<dd><dl>" );
            writeTextControl( out, "xpix", getParameter( "xpix" ) );
            writeTextControl( out, "ypix", getParameter( "ypix" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Output Format</b></dt>" );
            out.println( "<dl><dd>" );
            ChoiceParameter fmtParam = (ChoiceParameter) getParameter( "ofmt" );
            fmtParam.setDefaultOption( fmtParam.getOptions()[ 0 ] );
            writeOptionControl( out, "ofmt", fmtParam );
            out.println( "</dd></dl>" );
            out.println( "</dd>" );
            out.println( "</dl>" );
        }
    }

    /**
     * FormWriter for plothist task.
     */
    private class HistogramFormWriter extends FormWriter {
        public HistogramFormWriter() throws LoadException {
            super( "plothist" );
        }

        public void writeControls( ServletOutputStream out, StarTable table )
                throws IOException {
            out.println( "<dl>" );
            out.println( "<dt><b>X Axis</b></dt>" );
            out.println( "<dd><dl>" );
            writeColumnControl( out, "xdata", table, Number.class );
            writeTextControl( out, "xlo", getParameter( "xlo" ) );
            writeTextControl( out, "xhi", getParameter( "xhi" ) );
            writeBooleanControl( out, "xlog", getParameter( "xlog" ) );
            writeBooleanControl( out, "xflip", getParameter( "xflip" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Y Axis</b></dt>" );
            out.println( "<dd><dl>" );
            Parameter yloParam = getParameter( "ylo" );
            yloParam.setDefault( null );
            writeTextControl( out, "ylo", getParameter( "ylo" ) );
            writeTextControl( out, "yhi", getParameter( "yhi" ) );
            writeBooleanControl( out, "ylog", getParameter( "ylog" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Image dimensions</b></dt>" );
            out.println( "<dd><dl>" );
            writeTextControl( out, "xpix", getParameter( "xpix" ) );
            writeTextControl( out, "ypix", getParameter( "ypix" ) );
            out.println( "</dl></dd>" );
            out.println( "<dt><b>Output Format</b></dt>" );
            out.println( "<dl><dd>" );
            ChoiceParameter fmtParam = (ChoiceParameter) getParameter( "ofmt" );
            fmtParam.setDefaultOption( fmtParam.getOptions()[ 0 ] );
            writeOptionControl( out, "ofmt", fmtParam );
            out.println( "</dd></dl>" );
            out.println( "</dd>" );
            out.println( "</dl>" );
        }
    }
}
