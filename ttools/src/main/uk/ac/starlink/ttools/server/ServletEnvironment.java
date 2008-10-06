package uk.ac.starlink.ttools.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JComponent;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.plottask.Painter;
import uk.ac.starlink.ttools.task.OutputFormatParameter;
import uk.ac.starlink.ttools.task.OutputModeParameter;
import uk.ac.starlink.ttools.task.OutputTableParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;

/**
 * Execution environment which can be used from a servlet.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2008
 */
public class ServletEnvironment implements TableEnvironment {

    private final ServletRequest request_;
    private final ServletResponse response_;
    private final Map paramMap_;
    private final PrintStream outStream_;
    private final StarTableFactory tableFactory_;
    private final StarTableOutput tableOutput_;
    private final JDBCAuthenticator jdbcAuth_;
    private boolean strictVot_;
    private boolean debug_;

    /**
     * Constructor.
     *
     * @param  request  servlet request
     * @param  response  servlet response
     * @param  tableFactory  table factory
     * @param  tableOutput   table output
     * @param  jdbcAuth   JDBC authenticator
     */
    public ServletEnvironment( ServletRequest request,
                               ServletResponse response,
                               StarTableFactory tableFactory,
                               StarTableOutput tableOutput,
                               JDBCAuthenticator jdbcAuth ) throws IOException {
        request_ = request;
        response_ = response;
        tableFactory_ = tableFactory;
        tableOutput_ = tableOutput;
        jdbcAuth_ = jdbcAuth;
        paramMap_ = new HashMap( request.getParameterMap() );
        outStream_ = new PrintStream( response.getOutputStream() );
    }

    public void acquireValue( Parameter param ) throws TaskException {
        final String pname = param.getName();

        /* Configure output tables to get written to servlet response. */
        boolean isDefault = ! paramMap_.containsKey( pname );
        if ( isDefault && param instanceof OutputTableParameter ) {
            OutputTableParameter outParam = (OutputTableParameter) param;
            OutputFormatParameter formatParam = outParam.getFormatParameter();
            String format = formatParam.stringValue( this );
            StarTableWriter writer;
            try {
                writer = getTableOutput().getHandler( format );
            }
            catch ( TableFormatException e ) {
                throw new ParameterValueException( param,
                    "Unknown table output format " + format, e );
            }
            outParam.setValueFromConsumer( new ServletTableConsumer( writer ) );
        }

        /* Configure output table consumer modes to cause tables to be written
         * to servlet response. */
        else if ( isDefault && param instanceof OutputModeParameter ) {
            OutputModeParameter outParam = (OutputModeParameter) param;
            OutputFormatParameter formatParam =
                new OutputFormatParameter( "ofmt" );
            formatParam.setDefault( "votable" );
            String format = formatParam.stringValue( this );
            StarTableWriter writer;
            try {
                writer = getTableOutput().getHandler( format );
            }
            catch ( TableFormatException e ) {
                throw new ParameterValueException( param,
                    "Unknown table output format " + format, e );
            }
            outParam.setValueFromConsumer( new ServletTableConsumer( writer ) );
        }

        /* Configure graphics output to cause graphics to be written to 
         * servlet response. */
        else if ( isDefault && param instanceof PaintModeParameter ) {
            PaintModeParameter pmParam = (PaintModeParameter) param;
            ChoiceParameter formatParam = pmParam.getFormatParameter();
            GraphicExporter exporter =
                (GraphicExporter) formatParam.objectValue( this );
            pmParam.setValueFromPainter( new ServletPainter( exporter ) );
        }

        /* Other parameters will be acquired from the servlet parameters
         * supplied by form or the query part of the URL. */
        else {
            final String stringVal;
            String[] valueArray = (String[]) paramMap_.get( param.getName() );
            if ( isDefault ) {
                stringVal = param.getDefault();
            }
            else if ( param instanceof MultiParameter ) {
                char sep = ((MultiParameter) param).getValueSeparator();
                StringBuffer sbuf = new StringBuffer();
                for ( int iv = 0; iv < valueArray.length; iv++ ) {
                    if ( iv > 0 ) {
                        sbuf.append( sep );
                    }
                    sbuf.append( valueArray[ iv ] );
                }
                stringVal = sbuf.toString();
            }
            else {
                if ( valueArray.length > 1 ) {
                    throw new ParameterValueException(
                        param,
                        "Multiple values supplied for " +
                        "single-valued  parameter" );
                }
                stringVal = valueArray[ 0 ];
            }
            if ( stringVal == null || stringVal.length() == 0 ) {
                if ( param.isNullPermitted() ) {
                    param.setValueFromString( this, null );
                }
                else {
                    throw new ParameterValueException(
                                  param, "null value not allowed" );
                }
            }
            else {
                param.setValueFromString( this, stringVal );
            }
        }
    }

    public void clearValue( Parameter param ) {
        synchronized ( paramMap_ ) {
            paramMap_.remove( param.getName() );
        }
    }

    public String[] getNames() {
        return (String[]) paramMap_.keySet().toArray( new String[ 0 ] );
    }

    public PrintStream getOutputStream() {
        return outStream_;
    }

    public PrintStream getErrorStream() {
        return outStream_;
    }

    public StarTableFactory getTableFactory() {
        return tableFactory_;
    }

    public StarTableOutput getTableOutput() {
        return tableOutput_;
    }

    public JDBCAuthenticator getJdbcAuthenticator() {
        return jdbcAuth_;
    }

    public boolean isDebug() {
        return debug_;
    }

    public void setDebug( boolean debug ) {
        debug_ = debug;
    }

    public boolean isStrictVotable() {
        return strictVot_;
    }

    public void setStrictVotable( boolean strictVot ) {
        strictVot_ = strictVot;
    }

    /**
     * TableConsumer implementation which writes a table to the servlet 
     * response object associated witht this environment.
     */
    private class ServletTableConsumer implements TableConsumer {
        private final StarTableWriter tableWriter_;

        /**
         * Constructor.
         *
         * @param  tableWriter   writer which can serialize a table to an
         *                       output stream
         */
        ServletTableConsumer( StarTableWriter tableWriter ) {
            tableWriter_ = tableWriter;
        }

        public void consume( StarTable table ) throws IOException {
            response_.setContentType( tableWriter_.getMimeType() );
            if ( response_ instanceof HttpServletResponse ) {
                HttpServletResponse hr = (HttpServletResponse) response_;
                hr.setStatus( hr.SC_OK );
            }
            OutputStream out =
                new BufferedOutputStream( response_.getOutputStream() );
            tableWriter_.writeStarTable( table, out );
            out.flush();
            out.close();
        }
    }

    /**
     * Painter implementation which draws graphics to the servlet response
     * object associated with this environment.
     */
    private class ServletPainter implements Painter {
        private final GraphicExporter exporter_;

        /**
         * Constructor.
         *
         * @param  exporter   writer which can serialize a graphics component
         *                    to an output stream
         */
        ServletPainter( GraphicExporter exporter ) {
            exporter_ = exporter;
        }

        public void paintPlot( JComponent plot ) throws IOException {
            response_.setContentType( exporter_.getMimeType() );
            if ( response_ instanceof HttpServletResponse ) {
                HttpServletResponse hr = (HttpServletResponse) response_;
                hr.setStatus( hr.SC_OK );
            }
            OutputStream out =
                new BufferedOutputStream( response_.getOutputStream() );
            exporter_.exportGraphic( plot, out );
            out.flush();
            out.close();
        }
    }
}
