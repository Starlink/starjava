package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.jdbc.JDBCUtils;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.ResultSetJELRowReader;

/**
 * JDBC table column writer task.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public class SqlUpdate implements Task {

    private final ConnectionParameter connParam_;
    private final Parameter selectParam_;
    private final AssignParameter assignParam_;
    private final BooleanParameter progressParam_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );
    private static final char[] SPINNER = new char[] { '|', '/', '-', '\\', };
    private static final int INTERVAL = 500;

    /**
     * Constructor.
     */
    public SqlUpdate() {
        connParam_ = new ConnectionParameter( "db" );

        selectParam_ = new Parameter( "select" );
        selectParam_.setUsage( "<select-stmt>" );
        selectParam_.setPrompt( "SELECT statement" );
        selectParam_.setDescription( new String[] {
            "<p>Gives the full text (including \"<code>SELECT</code>\")",
            "of the SELECT statement to identify which rows undergo",
            "updates.",
            "</p>",
        } );

        assignParam_ = new AssignParameter( "assign" );
        assignParam_.setPrompt( "col=expr assignment(s)" );
        assignParam_.setUsage( "<col>=<expr>" );
        assignParam_.setDescription( new String[] {
            "<p>Assigns new values for a given column.",
            "The assignment is made in the form",
            "<code>&lt;colname&gt;=&lt;expr&gt;</code>",
            "where <code>&lt;colname&gt;</code> is the name of a column",
            "in the SQL table and",
            "<code>&lt;expr&gt;</code> is the text of an expression using",
            "STILTS's expression language, as described in <ref id='jel'/>.",
            "SQL table column names or " + JELRowReader.COLUMN_ID_CHAR + "ID",
            "identifiers may be used as variables in the usual way.",
            "</p>",
            "<p>This parameter may be supplied more than once to effect",
            "multiple assignments, or multiple assignments may be made",
            "by separating them with semicolons in the value of this",
            "parameter.",
            "</p>",
        } );

        progressParam_ = new BooleanParameter( "progress" );
        progressParam_.setDefault( "true" );
        progressParam_.setPrompt( "Display progress on console?" );
        progressParam_.setDescription( new String[] {
            "<p>If true, a spinner will be drawn on standard error",
            "which shows how many rows have been updated so far.",
            "</p>",
        } );
    }

    public String getPurpose() {
        return "Updates values in an SQL table";
    }

    public Parameter[] getParameters() {
        List paramList = new ArrayList();
        paramList.add( connParam_ );
        paramList.addAll( Arrays.asList( connParam_
                                        .getAssociatedParameters() ) );
        paramList.add( selectParam_ );
        paramList.add( assignParam_ );
        paramList.add( progressParam_ );
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final Connection connection = connParam_.connectionValue( env );
        final String select = selectParam_.stringValue( env );
        final Assignment[] assigns = assignParam_.assignmentsValue( env );
        boolean progress = progressParam_.booleanValue( env );
        final PrintStream progStrm = progress ? env.getErrorStream()
                                              : null;
        return new Executable() {
            public void execute() throws IOException, TaskException {
                try {
                    sqlUpdates( connection, select, assigns, progStrm );
                }
                catch ( SQLException e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
            }
        };
    }

    /**
     * Performs the actual updates on an SQL result set.
     *
     * @param  connection   active connection to a database
     * @param  select  SELECT statement to generate the updatable ResultSet
     * @param  assigns  array of specifiers for updating column values
     * @param  progStrm  destination stream for progresss output, or null
     *                   for no updates
     */
    private static void sqlUpdates( Connection connection, String select,
                                    Assignment[] assigns, PrintStream progStrm )
            throws SQLException, TaskException {

        /* Prepare for JDBC access. */
        Statement stmt = JDBCUtils.createStreamingStatement( connection, true );
        logWarnings( connection.getWarnings() );
        connection.clearWarnings();
        if ( stmt.getResultSetConcurrency() != ResultSet.CONCUR_UPDATABLE ) {
            logger_.warning( "JDBC driver apparently does not provide "
                           + "updatable statements" );
        }
        ResultSet rset = stmt.executeQuery( select );
        logWarnings( stmt.getWarnings() );
        stmt.clearWarnings();
        if ( rset.getConcurrency() != ResultSet.CONCUR_UPDATABLE ) {
            logger_.warning( "JDBC driver apparently does not provide "
                           + "updatable ResultSet" );
        }

        /* Compile assignment statements for execution. */
        JELRowReader jelly = new ResultSetJELRowReader( rset );
        Library lib = JELUtils.getLibrary( jelly );
        int nassign = assigns.length;
        int[] iacols = new int[ nassign ];
        CompiledExpression[] acompexs = new CompiledExpression[ nassign ];
        for ( int ia = 0; ia < nassign; ia++ ) {
            iacols[ ia ] = rset.findColumn( assigns[ ia ].getName() );
            String expr = assigns[ ia ].getExpression();
            try {
                acompexs[ ia ] = Evaluator.compile( expr, lib );
            }
            catch ( CompilationException e ) {
                throw (TaskException)
                      new TaskException( "Error parsing \"" + expr + "\": " +
                                         e.getMessage() )
                     .initCause( e );
            }
        }

        /* Cycle through ResultSet rows doing the updates. */
        long alarm = System.currentTimeMillis() + INTERVAL;
        long irow = 0;
        int progCount = 0;
        while ( rset.next() ) {
            for ( int ia = 0; ia < nassign; ia++ ) {
                CompiledExpression compex = acompexs[ ia ];
                Object val;
                try {
                    val = jelly.evaluate( compex );
                }
                catch( Throwable e ) {
                    throw (TaskException)
                          new TaskException( "Error evaluating \"" +
                                             assigns[ ia ].getExpression() +
                                             "\": " + e.getMessage() )
                         .initCause( e );
                }
                rset.updateObject( iacols[ ia ], val );
            }
            rset.updateRow();
            if ( progStrm != null ) {
                long now = System.currentTimeMillis();
                if ( now > alarm ) {
                    alarm = now + INTERVAL;
                    StringBuffer sbuf = new StringBuffer()
                        .append( '\r' )
                        .append( SPINNER[ progCount % SPINNER.length ] )
                        .append( ' ' )
                        .append( irow + 1 )
                        .append( '\r' );
                    progCount++;
                    progStrm.print( sbuf.toString() );
                }
            }
        }

        /* Tidy up. */
        rset.close();
        stmt.close();
        connection.close();
    }

    /**
     * Outputs any accumulated SQL warnings through the logging system.
     *
     * @param  warnings  SQL warning object - may be null
     */
    private static void logWarnings( SQLWarning warnings ) {
        for ( ; warnings != null; warnings = warnings.getNextWarning() ) {
            logger_.warning( warnings.toString() );
        }
    }

    /**
     * Parameter for holding one or more assignment statements.
     */
    private static class AssignParameter extends Parameter
                                         implements MultiParameter {
        private Assignment[] assigns_;

        /**
         * Constructor.
         *
         * @param  name  parameter name
         */
        public AssignParameter( String name ) {
            super( name );
            setUsage( "<name>=<value>" );
            setNullPermitted( false );
        }

        public char getValueSeparator() {
            return ';';
        }

        public void setValueFromString( Environment env, String sval )
                throws TaskException {
            assigns_ = sval == null ? new Assignment[ 0 ]
                                    : parseAssignments( sval );
            super.setValueFromString( env, sval );
        }

        /**
         * Returns the value of this parameter as an array of Assignment
         * objects.
         *
         * @param  env  execution environment
         * @return   array of assignments
         */
        public Assignment[] assignmentsValue( Environment env )
                throws TaskException {
            checkGotValue( env );
            return assigns_;
        }

        /**
         * Parses a {@link #getValueSeparator}-separated string of
         * name=value strings into an array of Assignment objects.
         *
         * @param  sval  string value representing zero or more assignments
         * @return  array of Assignments
         */
        private Assignment[] parseAssignments( String sval )
                throws TaskException {
            String[] lines = 
                sval.split( new String( new char[] { getValueSeparator() } ) );
            Assignment[] assigns = new Assignment[ lines.length ];
            for ( int i = 0; i < lines.length; i++ ) {
                String line = lines[ i ];
                int ieq = line.indexOf( '=' );
                if ( ieq > 0 ) {
                    assigns[ i ] =
                        new Assignment( line.substring( 0, ieq ).trim(),
                                        line.substring( ieq + 1 ).trim() );
                }
                else {
                    String msg = "No \"=\" character in assignment \""
                               + line + "\"";
                    throw new ParameterValueException( this, msg );
                }
            }
            return assigns;
        }
    }

    /**
     * Helper class representing a name=expr assignment.
     */
    private static class Assignment {
        private final String name_;
        private final String expr_;

        /**
         * Constructor.
         *
         * @param   name   name
         * @param   expr   expression
         */
        public Assignment( String name, String expr ) {
            name_ = name;
            expr_ = expr;
        }

        /**
         * Returns the name.
         *
         * @return   variable name
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns the expression.
         *
         * @return  expression
         */
        public String getExpression() {
            return expr_;
        }
    }
}
