package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.table.jdbc.SequentialResultSetStarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Tokenizer;

/**
 * SQL command-line client.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2008
 */
public class SqlClient implements Task {

    private final ConnectionParameter connParam_;
    private final StatementParameter stmtParam_;
    private final OutputFormatParameter ofmtParam_;

    /** 
     * Constructor.
     */
    public SqlClient() {
        connParam_ = new ConnectionParameter( "db" );
        stmtParam_ = new StatementParameter( "sql" );
        ofmtParam_ = new OutputFormatParameter( "ofmt" );
        ofmtParam_.setDefault( "text" );
    }

    public String getPurpose() {
        return "Executes SQL statements";
    }

    public Parameter[] getParameters() {
        List paramList = new ArrayList();
        paramList.add( connParam_ );
        paramList.addAll( Arrays.asList( connParam_
                                        .getAssociatedParameters() ) );
        paramList.add( stmtParam_ );
        paramList.add( ofmtParam_ );
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    public Executable createExecutable( Environment env )
            throws TaskException {
        StarTableWriter writer;
        try {
            writer = LineTableEnvironment.getTableOutput( env )
                    .getHandler( ofmtParam_.stringValue( env ) );
        }
        catch ( TableFormatException e ) {
            throw new ParameterValueException( ofmtParam_,
                                               "Unknown table format", e );
        }
        ResultSink sink =
            new StreamResultSink( writer, env.getOutputStream() );
        Connection connection = connParam_.connectionValue( env );
        String[] sqlLines = 
            Tokenizer.tokenizeLines( stmtParam_.stringValue( env ) );
        PrintStream err = env.getErrorStream();
        try {
            return new SqlExecutable( connection, sqlLines, sink, err );
        }
        catch ( SQLException e ) {
            throw new TaskException( e.getMessage(), e );
        }
    }

    /**
     * Indicates whether a given StarTableWriter can work with streamed input.
     *
     * <p><strong>Note</strong> really this ought to work by interrogating the
     * writer object itself, but currently no suitable method exists on the
     * {@link uk.ac.starlink.table.StarTableWriter} interface.
     *
     * @param  writer   table output handler
     * @return  true  if <code>writer</code> can stream output
     */
    private static boolean isStreamable( StarTableWriter writer ) {
        if ( writer.getFormatName().toLowerCase().indexOf( "fits" ) >= 0 ) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Executable implementation for the Sql task.
     */
    private static class SqlExecutable implements Executable {
        private final Connection connection_;
        private final Statement stmt_;
        private final String[] sqlLines_;
        private final ResultSink sink_;
        private final PrintStream err_;

        /**
         * Constructor.
         *
         * @param  connection  JDBC connection to database; will be closed
         *                     following execution
         * @param  sqlLines one or more SQL statements for execution
         * @param  sink   destination for output result sets
         * @param  err    destination stream for bookkeeping output
         */
        SqlExecutable( Connection connection, String[] sqlLines,
                       ResultSink sink, PrintStream err  )
                throws SQLException {
            connection_ = connection;
            stmt_ = connection.createStatement();
            sqlLines_ = sqlLines;
            sink_ = sink;
            err_ = err;
        }

        public void execute() throws TaskException, IOException {
            try {
                for ( int i = 0; i < sqlLines_.length; i++ ) {
                    executeLine( sqlLines_[ i ] );
                }
            }
            catch ( SQLException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
            finally {
                try {
                    connection_.close();
                }
                catch ( SQLException e ) {
                    // no action
                }
            }
        }

        /**
         * Executes a single SQL statement.
         *
         * @param  sqlLine  line of SQL 
         */
        private void executeLine( String sqlLine )
                throws SQLException, IOException {
            long start = System.currentTimeMillis();
            err_.println( "sql> " + sqlLine.trim() );
            for ( boolean hasRset = stmt_.execute( sqlLine );
                  hasRset || stmt_.getUpdateCount() >= 0;
                  hasRset = stmt_.getMoreResults() ) {
                if ( hasRset ) {
                    sink_.write( stmt_.getResultSet() );
                }
                else {
                    err_.println( "Updates: " + stmt_.getUpdateCount() );
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            err_.println( "Elapsed time: " + (float) ( elapsed / 1000. )
                        + " sec" );
            err_.println();
        }
    }

    /**
     * Abstract class defining what happens to output result sets.
     * Must be capable of outputting multiple result sets one after another.
     */
    private static abstract class ResultSink {

        private final boolean isStream_;

        /**
         * Constructor.
         *
         * @param   isStream   whether the 
         *                     {@link #write(uk.a.starlink.table.StarTable)
         *                     requires only a single run-through
         */
        protected ResultSink( boolean isStream ) {
            isStream_ = isStream;
        }

        /**
         * Sends a table to this sink's destination.
         *
         * @param  table  table
         */
        protected abstract void write( StarTable table ) throws IOException;

        /**
         * Sends a result set to this sink's destination.
         *
         * @param  rset  result set
         */
        public void write( ResultSet rset ) throws SQLException, IOException {
            StarTable table = new SequentialResultSetStarTable( rset );
            if ( ! isStream_ ) {
                table = Tables.randomTable( table );
            }
            write( table );
        }
    }

    /**
     * ResultSink implementation which uses a StarTableWriter writing to
     * a PrintStream.  All tables are just concatenated to the same stream.
     */
    private static class StreamResultSink extends ResultSink {

        private final StarTableWriter writer_;
        private final PrintStream out_;

        /**
         * Constructor.
         *
         * @param  writer  table writer
         * @param  out  destination stream
         */
        public StreamResultSink( StarTableWriter writer, PrintStream out ) {
            super( isStreamable( writer ) );
            writer_ = writer;
            out_ = out;
        }

        protected void write( StarTable table ) throws IOException {
            writer_.writeStarTable( table, out_ );
        }
    }

    /**
     * MultiParameter for specifying SQL statements.
     */
    private static class StatementParameter extends Parameter
                                            implements MultiParameter {

        /**
         * Constructor.
         *
         * @param  name  parameter name
         */
        StatementParameter( String name ) {
            super( name );
            setUsage( "<sql>" );
            setPrompt( "Line of SQL" );
            setDescription( new String[] {
                "<p>Text of an SQL statement for execution.",
                "This parameter may be repeated, or statements may be",
                "separated by semicolon (\"<code>;</code>\") characters.",
                "</p>",
            } );
        }

        public char getValueSeparator() {
            return ';';
        }
    }
}
