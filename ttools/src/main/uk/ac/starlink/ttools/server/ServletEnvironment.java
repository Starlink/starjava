package uk.ac.starlink.ttools.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
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
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.plottask.Painter;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
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
        paramMap_ = new HashMap();
        for ( Iterator it = request.getParameterMap().entrySet().iterator();
              it.hasNext(); ) { 
            Map.Entry entry = (Map.Entry) it.next();
            paramMap_.put( LineTableEnvironment
                          .normaliseName( (String) entry.getKey() ),
                           entry.getValue() );
        }
        outStream_ = new PrintStream( response.getOutputStream() );
    }

    public void acquireValue( Parameter param ) throws TaskException {
        final String pname =
            LineTableEnvironment.normaliseName( param.getName() );

        /* Configure output tables to get written to servlet response. */
        boolean isDefault = ! paramMap_.containsKey( pname );
        if ( isDefault && param instanceof OutputTableParameter ) {
            OutputTableParameter outParam = (OutputTableParameter) param;
            OutputFormatParameter formatParam = outParam.getFormatParameter();
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
            formatParam.setDefault( "png" );
            GraphicExporter exporter =
                (GraphicExporter) formatParam.objectValue( this );
            pmParam.setValueFromPainter( new ServletPainter( exporter ) );
        }

        /* Other parameters will be acquired from the servlet parameters
         * supplied by form or the query part of the URL. */
        else {
            final String stringVal;
            String[] valueArray = (String[]) paramMap_.get( pname );

            /* No value supplied: use parameter default. */
            if ( isDefault ) {
                stringVal = param.getDefault();
            }

            /* Multiple-valued parameter: concatenate different values. */
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

            /* Single-valued parameter: be lenient here, and permit multiple
             * values if only one of them is non-null; this may facilitate
             * form processing in some cases (SELECT or TEXT form element). */
            else {
                if ( valueArray.length > 1 ) {
                    int nv = 0;
                    String val = null;
                    for ( int iv = 0; iv < valueArray.length; iv++ ) {
                        String value = valueArray[ iv ];
                        if ( value != null && value.length() > 0 ) {
                            nv++;
                            val = value;
                        }
                    }
                    if ( nv == 0 ) {
                        stringVal = null;
                    }
                    else if ( nv == 1 ) {
                        stringVal = val;
                    }
                    else {
                        throw new ParameterValueException(
                            param,
                            "Multiple values supplied for " +
                            "single-valued  parameter" );
                    }
                }
                else {
                    stringVal = valueArray[ 0 ];
                }
            }

            /* We have the string value, now process it. */
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
            paramMap_.remove( LineTableEnvironment
                             .normaliseName( param.getName() ) );
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
     * Returns true if the parameters passed into this environment reprsent
     * a request for help.
     *
     * @return   true for help request
     */
    public boolean isHelp() {
        if ( paramMap_.isEmpty() ) {
            return true;
        }
        for ( Iterator it = paramMap_.keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            if ( key.equalsIgnoreCase( "help" ) ||
                 key.equalsIgnoreCase( "-help" ) ) {
                return true;
            }
        }
        return false;
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

        public void paintPicture( Picture picture ) throws IOException {
            response_.setContentType( exporter_.getMimeType() );
            String encoding = exporter_.getContentEncoding();
            if ( encoding != null &&
                 response_ instanceof HttpServletResponse ) {
                ((HttpServletResponse) response_)
                     .setHeader( "Content-Encoding", encoding );
            }
            if ( response_ instanceof HttpServletResponse ) {
                HttpServletResponse hr = (HttpServletResponse) response_;
                hr.setStatus( hr.SC_OK );
            }
            OutputStream out =
                new BufferedOutputStream( response_.getOutputStream() );
            exporter_.exportGraphic( picture, out );
            out.flush();
            out.close();
        }
    }
}
