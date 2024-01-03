package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * Filter which sets a parameter on the table.
 *
 * @author   Mark Taylor
 * @since    2 Aug 2006
 */
public class SetParameterFilter extends BasicFilter {

    public SetParameterFilter() {
        super( "setparam",
               "[-type byte|short|int|long|float|double|boolean|string]\n" +
               "[-desc <descrip>] [-unit <units>] [-ucd <ucd>]\n" +
               "[-utype <utype>] [-xtype <xtype>]\n" +
               "<pname> <pexpr>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Sets a named parameter in the table to a given value.",
            "The parameter named <code>&lt;pname&gt;</code> is set",
            "to the value <code>&lt;pexpr&gt;</code>,",
            "which may be a literal value or an expression involving",
            "mathematical operations and other parameter names",
            "(using the <code>param$&lt;name&gt;</code> syntax).",
            "By default, the data type of the parameter is determined",
            "by the type of the supplied expression,",
            "but this can be overridden using the <code>-type</code> flag.",
            "The parameter description, units, UCD, Utype and Xtype attributes",
            "may optionally be set using the other flags.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String ptype = null;
        String pname = null;
        String pexpr = null;
        String pdesc = null;
        String pucd = null;
        String putype = null;
        String pxtype = null;
        String punits = null;
        while ( argIt.hasNext() ) {
            String arg = argIt.next();
            if ( "-type".equals( arg ) && ptype == null && argIt.hasNext() ) {
                argIt.remove();
                ptype = argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-desc" ) && pdesc == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                pdesc = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-ucd" ) && pucd == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                pucd = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-utype" ) && putype == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                putype = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-xtype" ) && pxtype == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                pxtype = argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-unit" ) && punits == null &&
                      argIt.hasNext() ) {
                argIt.remove();
                punits = argIt.next();
                argIt.remove();
            }
            else if ( pname == null ) {
                pname = arg;
                argIt.remove();
            }
            else if ( pexpr == null ) {
                pexpr = arg;
                argIt.remove();
            }
        }
        if ( pname == null ) {
            throw new ArgException( "No parameter name specified" );
        }
        else if ( pexpr == null ) {
            throw new ArgException( "No parameter value specified" );
        }
        final String name = pname;
        final String expr = pexpr;
        final String type = ptype;
        final String descrip = pdesc;
        final String ucd = pucd;
        final String utype = putype;
        final String xtype = pxtype;
        final String units = punits;
        final Class<?> clazz = type == null ? null : getClass( type );
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                Object value = evaluate( expr, base, clazz, type );
                base.setParameter( createDescribedValue( name, value, descrip,
                                                         ucd, utype, xtype,
                                                         units, clazz ) );
                return base;
            }
        };
    }

    /**
     * Turns a string such as "int" or "float" into a class.  
     * A primitive class is returned if one is available.
     *
     * @param  type  data type
     * @return  class matching <code>type</code>
     */
    private static Class<?> getClass( String type ) throws ArgException {
        type = type.toLowerCase().trim();
        if ( "byte".equals( type ) ) {
            return byte.class;
        }
        else if ( "short".equals( type ) ) {
            return short.class;
        }
        else if ( "int".equals( type ) ) {
            return int.class;
        }
        else if ( "long".equals( type ) ) {
            return long.class;
        }
        else if ( "float".equals( type ) ) {
            return float.class;
        }
        else if ( "double".equals( type ) ) {
            return double.class;
        }
        else if ( "string".equals( type ) ) {
            return String.class;
        }
        else {
            throw new ArgException( "Unknown parameter type \"" + type + "\"" );
        }
    }

    /**
     * Evaluates an expression in the context of a table.
     * If a class is specified, the result is guaranteed to be of the type
     * indicated by that class (or an error will be thrown).
     * If no JEL expression can be evaluated, the supplied expression itself
     * will be returned as a string, as long as that is not inconsistent
     * with the requested class.
     *
     * @param  expr  expression to evaluate
     * @param  table  evaluation context
     * @param  clazz  required class of result, or null;
     *                a primitive, rather than wrapper, class
     *                should be supplied if available
     * @param  type   user-supplied string corresponding to clazz, or null
     *                (used for error messages only)
     */
    public static Object evaluate( String expr, StarTable table,
                                   Class<?> clazz, String type )
            throws IOException {
        String qexpr = "\"" + expr + "\"";

        /* We need to do some manual casting in some cases,
         * otherwise JEL complains about requiring explicit narrowing casts. */
        String cexpr = clazz == null
                     ? expr
                     : "(" + clazz.getCanonicalName() + ") (" + expr + ")";
        
        /* Compile and evaluate the expression. */
        JELRowReader rdr = JELUtils.createDatalessRowReader( table );
        Library lib = JELUtils.getLibrary( rdr );
        CompiledExpression compex;
        try {
            compex = Evaluator.compile( cexpr, lib, clazz );
        }
        catch ( CompilationException e ) {
            if ( clazz == null || clazz.equals( String.class ) ) {
                return expr;
            }
            else {
                String msg = "Bad expression " + qexpr + " for type "
                           + ( type == null ? clazz.getName() : type )
                           + ": " + e.getMessage();
                throw (IOException) new IOException( msg ).initCause( e );
            }
        }
        final Object value;
        try {
            value = rdr.evaluate( compex );
        }
        catch ( Throwable e ) {
            if ( clazz == null || clazz.equals( String.class ) ) {
                return expr;
            }
            else {
                String msg = "Evaluation error for " + qexpr
                           + ": " + e.getMessage();
                throw (IOException) new IOException( msg ).initCause( e );
            }
        }

        /* If the value is a byte or a short and no explicit class was
         * required, promote it to an int.
         * The most likely reason for finding a byte or short here is that
         * the expression was a small literal, and in this case an integer
         * is likely to be less surprising. */
        if ( clazz == null &&
             ( value instanceof Short || value instanceof Byte ) ) {
            return new Integer( ((Number) value).intValue() );
        }
        else {
            return value;
        }
    }

    /**
     * Turns a value plus metadata into a DescribedValue.
     *
     * @param   name  parameter name
     * @param   sexpr   expression giving parameter value
     * @param   descrip  parameter description, or null
     * @param   ucd   parameter UCD, or null
     * @param   utype  parameter Utype, or null
     * @param   xtype  parameter Xtype, or null
     * @param   units  parameter units, or null
     * @param   type   user-supplied type string, or null
     * @param   clazz  class (possibly primitive) of parameter type,
     *                 or null for automatic
     *          determination
     * @param   table  table
     */
    private static DescribedValue createDescribedValue( String name,
                                                        Object value,
                                                        String descrip,
                                                        String ucd,
                                                        String utype,
                                                        String xtype,
                                                        String units,
                                                        Class<?> clazz )
            throws IOException {
        Class<?> pclazz = clazz == null ? value.getClass()
                                        : JELUtils.getWrapperType( clazz );
        DefaultValueInfo info = new DefaultValueInfo( name, pclazz, descrip );
        if ( ucd != null && ucd.trim().length() > 0 ) {
            info.setUCD( ucd );
        }
        if ( utype != null && utype.trim().length() > 0 ) {
            info.setUtype( utype );
        }
        if ( xtype != null && xtype.trim().length() > 0 ) {
            info.setXtype( xtype );
        }
        if ( units != null && units.trim().length() > 0 ) {
            info.setUnitString( units );
        }
        return new DescribedValue( info, value );
    }
}
