package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.InvokeUtils;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.StepFactory;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.ttools.task.AbstractInputTableParameter;
import uk.ac.starlink.ttools.task.Calc;
import uk.ac.starlink.ttools.task.ChoiceMode;
import uk.ac.starlink.ttools.task.ConsumerTask;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputFormatParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.InputTablesParameter;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.MultiOutputFormatParameter;
import uk.ac.starlink.ttools.task.OutputFormatParameter;
import uk.ac.starlink.ttools.task.OutputTableParameter;
import uk.ac.starlink.ttools.task.OutputModeParameter;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Writes a Jython module which facilitates use of STILTS functionality
 * from Jython.
 * The <code>main</code> method will write the jython source code
 * to standard output.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2010
 */
@SuppressWarnings("static")
public class JyStilts {

    private final Stilts stilts_;
    private final Formatter formatter_;
    private final Map<Class<?>,String> clazzMap_;
    private final String[] imports_;
    private final Map<String,String> paramAliasMap_;
    private static final String paramAliasDictName_ = "_param_alias_dict";

    /** Java classes which are used by python source code. */
    private static final Class<?>[] IMPORT_CLASSES = new Class<?>[] {
        java.io.ByteArrayInputStream.class,
        java.io.OutputStream.class,
        java.lang.Class.class,
        java.lang.System.class,
        java.lang.reflect.Array.class,
        java.util.ArrayList.class,
        uk.ac.starlink.table.ColumnInfo.class,
        uk.ac.starlink.table.MultiStarTableWriter.class,
        uk.ac.starlink.table.StarTable.class,
        uk.ac.starlink.table.StarTableFactory.class,
        uk.ac.starlink.table.StarTableOutput.class,
        uk.ac.starlink.table.TableSequence.class,
        uk.ac.starlink.table.Tables.class,
        uk.ac.starlink.table.WrapperStarTable.class,
        uk.ac.starlink.table.WrapperRowSequence.class,
        uk.ac.starlink.task.InvokeUtils.class,
        uk.ac.starlink.ttools.Stilts.class,
        uk.ac.starlink.ttools.filter.StepFactory.class,
        uk.ac.starlink.ttools.task.MapEnvironment.class,
        uk.ac.starlink.util.DataSource.class,
    };

    /**
     * Constructor.
     *
     * @param  stilts  stilts instance defining available tasks etc
     */
    public JyStilts( Stilts stilts ) {
        stilts_ = stilts;
        formatter_ = new Formatter();

        /* Prepare python imports. */
        clazzMap_ = new HashMap<Class<?>,String>();
        Class<?>[] clazzes = IMPORT_CLASSES;
        List<String> importList = new ArrayList<String>();
        importList.add( "import jarray.array" );
        importList.add( "from org.python.core.util import StringUtil" );
        for ( int ic = 0; ic < clazzes.length; ic++ ) {
            Class<?> clazz = clazzes[ ic ];
            String clazzName = clazz.getName();
            String importName = clazzName;
            importName = importName.replaceFirst( ".*\\.", "_" );
            if ( clazzMap_.containsValue( importName ) ) {
                throw new RuntimeException( "import name clash: "
                                          + importName );
            }
            clazzMap_.put( clazz, importName );
            String line = "import " + clazzName;
            if ( ! importName.equals( clazzName ) ) {
                line += " as " + importName;
            }
            importList.add( line );
        }

        /* Imports providing calculation static functions for use by users. */
        Class<?>[] calcClazzes =
            JELUtils.getStaticClasses().toArray( new Class<?>[ 0 ] );
        for ( int ic = 0; ic < calcClazzes.length; ic++ ) {
            Class<?> clazz = calcClazzes[ ic ];
            String clazzName = clazz.getName();
            String importName = clazzName;
            importName = importName.replaceFirst( ".*\\.", "" );
            if ( clazzMap_.containsValue( importName ) ) {
                throw new RuntimeException( "import name clash: "
                                          + importName );
            }
            String line = "import " + clazzName + " as " + importName;
            importList.add( line );
        }

        imports_ = importList.toArray( new String[ 0 ] );

        /* Some parameter names need to be aliased because they are python
         * reserved words. */
        paramAliasMap_ = new HashMap<String,String>();
        paramAliasMap_.put( "in", "in_" );
    }

    /**
     * Generates python source giving module header lines.
     *
     * @return  python source code lines
     */
    private String[] header() {
        return new String[] {
            "# This module auto-generated by java class "
                  + getClass().getName() + ".",
            "",
            "'''Provides access to STILTS commands.",
            "",
            "See the manual, http://www.starlink.ac.uk/stilts/sun256/",
            "for tutorial and full usage information.",
            "'''",
            "",
            "from __future__ import generators",
            "__author__ = 'Mark Taylor'",
            "",
        };
    }

    /**
     * Generates python source giving statements which
     * import java classes required for the rest of the python source
     * generated by this class.
     *
     * @return  python source code lines
     */
    private String[] imports() {
        return imports_;
    }

    /**
     * Returns the python name under which a given Java class has been
     * imported into python for use by this class.
     *
     * @param  clazz  java class
     * @return  python name for <code>clazz</code>
     */
    private String getImportName( Class<?> clazz ) {
        String cname = clazzMap_.get( clazz );
        if ( cname == null ) {
            throw new IllegalArgumentException( "Class " + clazz.getName()
                                              + " not imported" );
        }
        return cname;
    }

    /**
     * Generates python source defining utility functions.
     *
     * @return  python source code lines
     */
    private String[] defUtils() {
        List<String> lineList =
                new ArrayList<String>( Arrays.asList( new String[] {

            /* WrapperStarTable implementation which is a python container. */
            "class RandomJyStarTable(JyStarTable):",
            "    '''Extends the JyStarTable wrapper class for random access.",
            "",
            "    Instances of this class can be subscripted.",
            "    '''",
            "    def __init__(self, base_table):",
            "        JyStarTable.__init__(self, base_table)",
            "    def __len__(self):",
            "        return int(self.getRowCount())",
            "    def __getitem__(self, key):",
            "        if type(key) is type(slice(0)):",
            "            return [self._create_row(self.getRow(irow))",
            "                    for irow in _slice_range(key, len(self))]",
            "        elif key < 0:",
            "            irow = self.getRowCount() + key",
            "            return self._create_row(self.getRow(irow))",
            "        else:",
            "            return self._create_row(self.getRow(key))",
            "    def __str__(self):",
            "        return str(self.getName())"
                          + " + '(' + str(self.getRowCount()) + 'x'"
                          + " + str(self.getColumnCount()) + ')'",
            "    def coldata(self, key):",
            "        '''Returns a sequence of all the values"
                          + " in a given column.'''",
            "        icol = self._column_index(key)",
            "        return _Coldata(self, icol)",
            "",

            "class _Coldata(object):",
            "    def __init__(self, table, icol):",
            "        self.table = table",
            "        self.icol = icol",
            "        self.nrow = len(table)",
            "    def __iter__(self):",
            "        rowseq = self.table.getRowSequence()",
            "        while rowseq.next():",
            "            yield rowseq.getCell(self.icol)",
            "    def __len__(self):",
            "        return self.nrow",
            "    def __getitem__(self, key):",
            "        if type(key) is type(slice(0)):",
            "            return [self.table.getCell(irow, self.icol)",
            "                    for irow in _slice_range(key, self.nrow)]",
            "        elif key < 0:",
            "            irow = self.nrow + key",
            "            return self.table.getCell(irow, self.icol)",
            "        else:",
            "            return self.table.getCell(key, self.icol)",
            "",

            /* Wrapper ColumnInfo implementation with some pythonic knobs on. */
            "class _JyColumnInfo(" + getImportName( ColumnInfo.class ) + "):",
            "    def __init__(self, base):",
            "        " + getImportName( ColumnInfo.class )
                       + ".__init__(self, base)",
            "    def __str__(self):",
            "        return self.getName()",
            "",

            /* Wrapper row class. */
            "class _JyRow(tuple):",
            "    def __init__(self, array):",
            "        tuple.__init__(self, array)",
            "    def __getitem__(self, key):",
            "        icol = self.table._column_index(key)",
            "        return tuple.__getitem__(self, icol)",
            "",

            /* Execution environment implementation. */
            "class _JyEnvironment(" + getImportName( MapEnvironment.class )
                                    + "):",
            "    def __init__(self, grab_output=False):",
            "        " + getImportName( MapEnvironment.class )
                       + ".__init__(self)",
            "        if grab_output:",
            "            self._out = " + getImportName( MapEnvironment.class )
                                       + ".getOutputStream(self)",
            "        else:",
            "            self._out = " + getImportName( System.class ) + ".out",
            "        self._err = " + getImportName( System.class ) + ".err",
            "        self._used = {}",
            "    def getOutputStream(self):",
            "        return self._out",
            "    def getErrorStream(self):",
            "        return self._err",
            "    def acquireValue(self, param):",
            "        " + getImportName( MapEnvironment.class )
                       + ".acquireValue(self, param)",
            "        self._used[param.getName()] = True",
            "    def getUnusedArgs(self):",
            "        return filter(lambda n: n not in self._used,"
                               + " self.getNames())",
            "",

            /* Utility to raise an error if some args in the environment
             * were supplied but unused. */
            "def _check_unused_args(env):",
            "    names = env.getUnusedArgs()",
            "    if (names):",
            "        raise SyntaxError('Unused STILTS parameters %s' % "
                               + "str(tuple([str(n) for n in names])))",
            "",

            /* Utility to raise an error if a handler can't write multiple
             * tables. */
            "def _check_multi_handler(handler):",
            "    if not " + getImportName( Class.class )
                          + ".forName('" + MultiStarTableWriter.class.getName()
                                         + "')"
                          + ".isInstance(handler):",
            "        raise TypeError('Handler %s cannot write multiple tables' "
                                    + "% handler.getFormatName())",
            "",

            /* Converts a slice into a range. */
            "def _slice_range(slice, leng):",
            "    start = slice.start",
            "    stop = slice.stop",
            "    step = slice.step",
            "    if start is None:",
            "        start = 0",
            "    elif start < 0:",
            "        start += leng",
            "    if stop is None:",
            "        stop = leng",
            "    elif stop < 0:",
            "        stop += leng",
            "    if step is None:",
            "        return xrange(start, stop)",
            "    else:",
            "        return xrange(start, stop, step)",
            "",

            /* OutputStream based on a python file. */
            "class _JyOutputStream(" + getImportName( OutputStream.class )
                                     + "):",
            "    def __init__(self, file):",
            "        self._file = file",
            "    def write(self, *args):",
            "        narg = len(args)",
            "        if narg is 1:",
            "            arg0 = args[0]",
            "            if type(arg0) is type(1):",
            "                pyarg = chr(arg0)",
            "            else:",
            "                pyarg = arg0",
            "        elif narg is 3:",
            "            buf, off, leng = args",
            "            pyarg = buf[off:off + leng].tostring()",
            "        else:",
            "            raise SyntaxError('%d args?' % narg)",
            "        self._file.write(pyarg)",
            "    def close(self):",
            "        self._file.close()",
            "    def flush(self):",
            "        self._file.flush()",
            "",

            /* TableSequence based on a python iterable. */
            "class _JyTableSequence("
                   + getImportName( TableSequence.class ) + "):",
            "    def __init__(self, seq):",
            "        self._iter = iter(seq)",
            "    def nextTable(self):",
            "        try:",
            "            return self._iter.next()",
            "        except StopIteration:",
            "            return None",
            "",

            /* DataSource based on a python file. 
             * This is not very efficient (it slurps up the whole file into
             * memory at construction time), but it's difficult to do it
             * correctly, at least without a lot of mucking about with
             * threads. */
            "class _JyDataSource(" + getImportName( DataSource.class )
                                    + "):",
            "    def __init__(self, file):",
            "        buf = file.read(-1)",
            "        self._buffer = StringUtil.toBytes(buf)",
            "        if hasattr(file, 'name'):",
            "            self.setName(file.name)",
            "        else:",
            "            self.setName('unnamed')",
            "    def getRawInputStream(self):",
            "        return " + getImportName( ByteArrayInputStream.class )
                              + "(self._buffer)",

            /* Returns a StarTable with suitable python decoration. */
            "def import_star_table(table):",
            "    '''Imports a StarTable instance for use with JyStilts.",
            "",
            "    This factory function takes an instance of the Java class",
            "    " + StarTable.class.getName(),
            "    and returns an instance of a wrapper subclass which has some",
            "    decorations useful in a python environment.",
            "    This includes stilts cmd_* and mode_* methods, as well as",
            "    python-friendly standard methods to make it behave as an",
            "    iterable, and where possible a container, of data rows,",
            "    and overloaded addition and multiplication operators",
            "    with the semantics of concatenation.",
            "    '''",
            "    if table.isRandom():",
            "        return RandomJyStarTable(table)",
            "    else:",
            "        return JyStarTable(table)",
            "",

            /* Takes a python value and returns a value suitable for passing
             * to a java Stilts execution environment. */
            "def _map_env_value(pval):",
            "    if pval is None:",
            "        return None",
            "    elif pval is True:",
            "        return 'true'",
            "    elif pval is False:",
            "        return 'false'",
            "    elif isinstance(pval, " + getImportName( StarTable.class )
                                       + "):",
            "        return pval",
            "    elif _is_container(pval, " + getImportName( StarTable.class )
                                              + "):",
            "        return jarray.array(pval, "
                                      +  getImportName( StarTable.class ) + ")",
            "    else:",
            "        return str(pval)",
            "",

            /* Utility method to determine if a python object can be treated
             * as a container. */
            "def _is_container(value, type):",
            "    try:",
            "        if len(value) > 0:",
            "            for item in value:",
            "                if not isinstance(item, type):",
            "                    return False",
            "            return True",
            "        else:",
            "            return False",
            "    except TypeError:",
            "        return False",
            "",
 
            /* Stilts class instance. */
            "_stilts = " + getImportName( Stilts.class ) + "()",
            "",

            /* Set up verbosity. */
            getImportName( InvokeUtils.class ) + ".configureLogging(0, False)",
            "",
        } ) );

        /* Creates and populates a dictionary mapping parameter names to
         * their aliases where appropriate. */
        lineList.add( paramAliasDictName_ + " = {}" );
        for ( Map.Entry<String,String> entry : paramAliasMap_.entrySet() ) {
            lineList.add( paramAliasDictName_
                        + "['" + entry.getKey() + "']='"
                               + entry.getValue() + "'" );
        }
        lineList.add( "" );

        /* Return source line list array. */
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source which checks version mismatches between the
     * module generated by this class and the runtime java library.
     *
     * @return   python source code lines
     */
    private String[] defVersionCheck() {
        return new String[] {
            "_stilts_lib_version = _stilts.getVersion()",
            "_stilts_script_version = '" + stilts_.getVersion() + "'",
            "if _stilts_lib_version != _stilts_script_version:",
            "    print('WARNING: STILTS script/class library version mismatch"
                             + " (' + _stilts_script_version + '/'"
                                + " + _stilts_lib_version + ').')",
            "    print('         This may or may not cause trouble.')",
        };
    }

    /**
     * Generates python source defining a wrapper class for use with StarTables.
     * This can be applied to every StarTable generated by JyStilts
     * to provide additional functionality.
     * It supplies the various filters and modes as methods.
     *
     * @param  cname  class name
     * @return   python source code lines
     */
    private String[] defTableClass( String cname )
            throws LoadException, SAXException {
        List<String> lineList = new ArrayList<String>();
        lineList.add( "class " + cname + "("
                               + getImportName( WrapperStarTable.class )
                               + "):" );

        /* Doc string. */
        lineList.add( "    '''StarTable wrapper class for use within Jython." );
        lineList.add( "" );
        lineList.add( "Decorates a " + StarTable.class.getName() );
        lineList.add( "java object with methods for use within jython." );
        lineList.add( "These include special bound functions to make it an" );
        lineList.add( "iterable object (with items which are table rows)," );
        lineList.add( "arithmetic + and * overloads for concatenation," );
        lineList.add( "a write method for table viewing or output," );
        lineList.add( "and methods representing STILTS filter functionality," );
        lineList.add( "namely cmd_* methods for filters and mode_* methods" );
        lineList.add( "for output modes." );
        lineList.add( "" );
        lineList.add( "As a general rule, any StarTable object which is" );
        lineList.add( "intented for use by JyStilts program code should be" );
        lineList.add( "wrapped in an instance of this class." );
        lineList.add( "    '''" );

        /* Constructor. */
        lineList.add( "    def __init__(self, base_table):" );
        lineList.add( "        " + getImportName( WrapperStarTable.class )
                                 + ".__init__(self, base_table)" );

        /* Iterability. */
        lineList.add( "    def __iter__(self):" );
        lineList.add( "        rowseq = self.getRowSequence()" );
        lineList.add( "        while rowseq.next():" );
        lineList.add( "            yield self._create_row(rowseq.getRow())" );

        /* String conversion. */
        lineList.add( "    def __str__(self):" );
        lineList.add( "        return '%s (?x%d)' % "
                               + "(self.getName(), self.getColumnCount())" );

        /* Overload add/multiply arithmetic operators with concatenation
         * semantics. */
        lineList.add( "    def __add__(self, other):" );
        lineList.add( "        return tcat([self, other])" );
        lineList.add( "    def __mul__(self, count):" );
        lineList.add( "        return tcat([self] * count)" );
        lineList.add( "    def __rmul__(self, count):" );
        lineList.add( "        return tcat([self] * count)" );

        /* Add column tuple access method. */
        lineList.add( "    def columns(self):" );
        lineList.add( "        '''Returns a tuple of ColumnInfo objects"
                               + " describing the columns of this table.'''" );
        lineList.add( "        if hasattr(self, '_columns'):" );
        lineList.add( "            return self._columns" );
        lineList.add( "        else:" );
        lineList.add( "            col_list = []" );
        lineList.add( "            for i in xrange(self.getColumnCount()):" );
        lineList.add( "                col_list.append(_JyColumnInfo("
                                                 + "self.getColumnInfo(i)))" );
        lineList.add( "            self._columns = tuple(col_list)" );
        lineList.add( "            return self.columns()" );

        /* Add parameter dictionary access method. */
        lineList.add( "    def parameters(self):" );
        lineList.add( "        '''" );
        lineList.add( "Returns a mapping of table parameter names to values." );
        lineList.add( "" );
        lineList.add( "This does not provide all the information "
                    + "about the parameters," );
        lineList.add( "for instance units and UCDs are not included." );
        lineList.add( "For more detail, use the relevant StarTable methods." );
        lineList.add( "Currently, this is not a live list, "
                    + "in the sense that changing" );
        lineList.add( "the returned dictionary will not affect "
                    + "the table parameter values." );
        lineList.add( "        '''" );
        lineList.add( "        if hasattr(self, '_parameters'):" );
        lineList.add( "            return self._parameters" );
        lineList.add( "        else:" );
        lineList.add( "            params = {}" );
        lineList.add( "            for p in self.getParameters():" );
        lineList.add( "                params[p.getInfo().getName()]"
                                             + " = p.getValue()" );
        lineList.add( "            self._parameters = params" );
        lineList.add( "            return self.parameters()" );

        /* Add column data extraction method. */
        lineList.add( "    def coldata(self, key):" );
        lineList.add( "        '''Returns a sequence of all the values "
                               + "in a given column.'''" );
        lineList.add( "        icol = self._column_index(key)" );
        lineList.add( "        rowseq = self.getRowSequence()" );
        lineList.add( "        while rowseq.next():" );
        lineList.add( "            yield rowseq.getCell(icol)" );

        /* Add row count method. */
        lineList.add( "    def count_rows(self):" );
        lineList.add( "        '''Returns the number of rows in this table." );
        lineList.add( "        For random access tables it calls getRowCount" );
        lineList.add( "        which returns the value directly." );
        lineList.add( "        For non-random tables it may have to iterate "
                            + "over the rows." );
        lineList.add( "        That could be slow, though it should be "
                            + "much faster than iterating" );
        lineList.add( "        over this table as an iterable itself, "
                            + "since the cell data" );
        lineList.add( "        does not need to be made available.'''" );
        lineList.add( "        nrow = self.getRowCount();" );
        lineList.add( "        if nrow >= 0:" );
        lineList.add( "            return nrow" );
        lineList.add( "        else:" );
        lineList.add( "            nr = 0" );
        lineList.add( "            rseq = self.getRowSequence()" );
        lineList.add( "            while rseq.next():" );
        lineList.add( "                nr += 1" );
        lineList.add( "            return nr" );

        /* Row wrapper. */
        lineList.add( "    def _create_row(self, array):" );
        lineList.add( "        row = _JyRow(array)" );
        lineList.add( "        row.table = self" );
        lineList.add( "        return row" );

        /* Column subscripting. */
        lineList.add( "    def _column_index(self, key):" );
        lineList.add( "        if type(key) is type(1):" );
        lineList.add( "            if key >= 0:" );
        lineList.add( "                return key" );
        lineList.add( "            else:" );
        lineList.add( "                return key + self.getColumnCount()" );
        lineList.add( "        if hasattr(self, '_colmap'):" );
        lineList.add( "            return self._colmap[key]" );
        lineList.add( "        else:" );
        lineList.add( "            colmap = {}" );
        lineList.add( "            for ic, col in enumerate(self.columns()):" );
        lineList.add( "                if not col in colmap:" );
        lineList.add( "                    colmap[col] = ic" );
        lineList.add( "                colname = col.getName()" );
        lineList.add( "                if not colname in colmap:" );
        lineList.add( "                    colmap[colname] = ic" );
        lineList.add( "            self._colmap = colmap" );
        lineList.add( "            return self._column_index(key)" );

        /* Add special write method. */
        String[] writeLines = defWrite( "write", true );
        lineList.addAll( Arrays.asList( prefixLines( "    ", writeLines ) ) );

        /* Add filters as methods. */
        ObjectFactory<ProcessingFilter> filterFactory =
            StepFactory.getInstance().getFilterFactory();
        String[] filterNames = filterFactory.getNickNames();
        for ( int i = 0; i < filterNames.length; i++ ) {
            String name = filterNames[ i ];
            String[] filterLines = defCmd( "cmd_" + name, name, true );
            lineList.addAll( Arrays.asList( prefixLines( "    ",
                                                         filterLines ) ) );
        }

        /* Add modes as methods. */
        ObjectFactory<ProcessingMode> modeFactory = stilts_.getModeFactory();
        String[] modeNames = modeFactory.getNickNames();
        for ( int i = 0; i < modeNames.length; i++ ) {
            String name = modeNames[ i ];
            String[] modeLines = defMode( "mode_" + name, name, true );
            lineList.addAll( Arrays.asList( prefixLines( "    ",
                                                         modeLines ) ) );
        }

        /* Return the source code lines. */
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining the table read function.
     *
     * @param  fname  name of function
     * @return  python source code lines
     */
    private String[] defRead( String fname ) {
        List<String> lineList = new ArrayList<String>();
        lineList.add( "def " + fname
                             + "(location, fmt='(auto)', random=False):" );
        lineList.add( "    '''Reads a table from a filename, URL or "
                        + "python file object." );
        lineList.add( "" );
        lineList.add( "    The random argument determines whether random "
                        + "access is required" );
        lineList.add( "    for the table." );
        lineList.add( "    Setting it true may improve efficiency, " 
                        + "but perhaps at the cost" );
        lineList.add( "    of memory usage and load time for large tables." );
        lineList.add( "" );
        lineList.add( "    The fmt argument must be supplied if "
                        + "the table format cannot" );
        lineList.add( "    be auto-detected." );
        lineList.add( "" );
        lineList.add( "    In general supplying a filename is preferred; "
                        + "the current implementation" );
        lineList.add( "    may be much more expensive on memory "
                        + "if a python file object is used." ); 
        lineList.add( "" );
        String fmtInfo = new InputFormatParameter( "location" )
                        .getExtraUsage( new MapEnvironment() );
        lineList.addAll( Arrays.asList( prefixLines( " ", fmtInfo ) ) );
        lineList.add( "" );
        lineList.add( "    The result of the function is a "
                        + "JyStilts table object." );
        lineList.add( "    '''" );
        lineList.add( "    fact = " + getImportName( StarTableFactory.class )
                                    + "(random)" );
        lineList.add( "    " + getImportName( Stilts.class )
                             + ".addStandardSchemes(fact)" );
        lineList.add( "    if hasattr(location, 'read'):" );
        lineList.add( "        datsrc = _JyDataSource(location)" );
        lineList.add( "        table = fact.makeStarTable(datsrc, fmt)" );
        lineList.add( "    else:" );
        lineList.add( "        table = fact.makeStarTable(location, fmt)" );
        lineList.add( "    return import_star_table(table)" );
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining the multi-table read function.
     *
     * @param  fname  name of function
     * @return  python source code lines
     */
    private String[] defReads( String fname ) {
        List<String> lineList = new ArrayList<String>();
        lineList.add( "def " + fname
                             + "(location, fmt='(auto)', random=False):" );
        lineList.add( "    '''Reads multiple tables from a filename, URL or "
                        + "python file object." );
        lineList.add( "" );
        lineList.add( "    It only makes sense to use this function rather "
                        + "than tread() if the" );
        lineList.add( "    format is, or may be, one which can contain "
                        + "multiple tables." );
        lineList.add( "    Generally this means VOTable or FITS or one of "
                        + "their variants." );
        lineList.add( "" );
        lineList.add( "    The random argument determines whether random "
                        + "access is required" );
        lineList.add( "    for the table." );
        lineList.add( "    Setting it true may improve efficiency, " 
                        + "but perhaps at the cost" );
        lineList.add( "    of memory usage and load time for large tables." );
        lineList.add( "" );
        lineList.add( "    The fmt argument must be supplied if "
                        + "the table format cannot" );
        lineList.add( "    be auto-detected." );
        lineList.add( "" );
        lineList.add( "    In general supplying a filename is preferred; "
                        + "the current implementation" );
        lineList.add( "    may be much more expensive on memory "
                        + "if a python file object is used." ); 
        lineList.add( "" );
        lineList.add( "    The result of the function is a list of JyStilts "
                        + "table objects." );
        lineList.add( "    '''" );
        lineList.add( "    fact = " + getImportName( StarTableFactory.class )
                                    + "(random)" );
        lineList.add( "    " + getImportName( Stilts.class )
                             + ".addStandardSchemes(fact)" );
        lineList.add( "    if hasattr(location, 'read'):" );
        lineList.add( "        datsrc = _JyDataSource(location)" );
        lineList.add( "    else:" );
        lineList.add( "        datsrc = " + getImportName( DataSource.class )
                                          + ".makeDataSource(location)" );
        lineList.add( "    tseq = fact.makeStarTables(datsrc, fmt)" );
        lineList.add( "    tables = " + getImportName( Tables.class )
                                      + ".tableArray(tseq)" );
        lineList.add( "    return map(import_star_table, tables)" );
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining the table write function.
     *
     * @param  fname  name of function
     * @param  isBound  true for a bound method, false for a standalone function
     * @return  python source code lines
     */
    private String[] defWrite( String fname, boolean isBound ) {
        String tArgName = isBound ? "self" : "table";
        List<String> lineList = new ArrayList<String>();
        lineList.add( "def " + fname + "(" + tArgName
                                     + ", location=None, fmt='(auto)'):" );
        lineList.add( "    '''Writes table to a file." );
        lineList.add( "" );
        lineList.add( "    The location parameter may give a filename or a" );
        lineList.add( "    python file object open for writing." );
        lineList.add( "    if it is not supplied, standard output is used." );
        lineList.add( "" );
        lineList.add( "    The fmt parameter specifies output format." );
        String fmtInfo = new OutputFormatParameter( "out" )
                        .getExtraUsage( new MapEnvironment() );
        lineList.addAll( Arrays.asList( prefixLines( " ", fmtInfo ) ) );
        lineList.add( "    '''" );
        lineList.add( "    sto = " + getImportName( StarTableOutput.class )
                                   + "()" );
        lineList.add( "    if hasattr(location, 'write') and "
                           + "hasattr(location, 'flush'):" );
        lineList.add( "        ostrm = _JyOutputStream(location)" );
        lineList.add( "        name = getattr(location, 'name', None)" );
        lineList.add( "        handler = sto.getHandler(fmt, name)" );
        lineList.add( "        sto.writeStarTable(" + tArgName
                                                    + ", ostrm, handler)" );
        lineList.add( "    else:" );
        lineList.add( "        if location is None:" );
        lineList.add( "            location = '-'" );
        lineList.add( "        sto.writeStarTable(" + tArgName
                                                    + ", location, fmt)" );
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining the multi-table write function.
     *
     * @param  fname  name of function
     * @return  python source code lines
     */
    private String[] defWrites( String fname ) {
        List<String> lineList = new ArrayList<String>();
        lineList.add( "def " + fname
                             + "(tables, location=None, fmt='(auto)'):" );
        lineList.add( "    '''Writes a sequence of tables "
                        + "to a single container file." );
        lineList.add( "" );
        lineList.add( "    The tables parameter gives an iterable over "
                        + "JyStilts table objects" );
        lineList.add( "    The location parameter may give a filename "
                        + "or a python file object" );
        lineList.add( "    open for writing.  If it is not supplied, "
                        + " standard output is used." );
        lineList.add( "" );
        lineList.add( "    The fmt parameter specifies output format." );
        lineList.add( "    Note that not all formats can write multiple "
                        + "tables;" );
        lineList.add( "    an error will result if an attempt is made "
                        + "to write" );
        lineList.add( "    multiple tables to a single-table only format." );
        String fmtInfo = new MultiOutputFormatParameter( "out" )
                        .getExtraUsage( new MapEnvironment() );
        lineList.addAll( Arrays.asList( prefixLines( " ", fmtInfo ) ) );
        lineList.add( "    '''" );
        lineList.add( "    sto = " + getImportName( StarTableOutput.class )
                                   + "()" );
        lineList.add( "    tseq = _JyTableSequence(tables)" );
        lineList.add( "    if hasattr(location, 'write') and "
                           + "hasattr(location, 'flush'):" );
        lineList.add( "        ostrm = _JyOutputStream(location)" );
        lineList.add( "        name = getattr(location, 'name', None)" );
        lineList.add( "        handler = sto.getHandler(fmt, name)" );
        lineList.add( "        _check_multi_handler(handler)" );
        lineList.add( "        handler.writeStarTables(tseq, ostrm)" );
        lineList.add( "    else:" );
        lineList.add( "        if location is None:" );
        lineList.add( "            location = '-'" );
        lineList.add( "        handler = sto.getHandler(fmt, location)" );
        lineList.add( "        _check_multi_handler(handler)" );
        lineList.add( "        handler.writeStarTables(tseq, location, sto)" );
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining the general table filter function.
     *
     * @param  fname  name of function
     * @return  python source code lines
     */
    private String[] defFilter( String fname ) {
        return new String[] {
            "def " + fname + "(table, cmd):",
            "    '''Applies a filter operation to a table "
                 + "and returns the result.",
            "    In most cases, it's better to use one of the cmd_* functions.",
            "    '''",
            "    step = " + getImportName( StepFactory.class )
                          + ".getInstance().createStep(cmd)",
            "    return import_star_table(step.wrap(table))",
        };
    }

    /**
     * Generates python source defining a specific table filter function.
     *
     * @param  fname  name of function
     * @param  filterNickName  name under which the filter is known in the
     *         filter ObjectFactory
     * @param  isBound  true for a bound method, false for a standalone function
     * @return  python source code lines
     */
    private String[] defCmd( String fname, String filterNickName,
                             boolean isBound )
            throws LoadException, SAXException {
        ProcessingFilter filter =
            StepFactory.getInstance().getFilterFactory()
                                     .createObject( filterNickName );
        String usage = filter.getUsage();
        boolean hasUsage = usage != null && usage.trim().length() > 0;
        String tArgName = isBound ? "self" : "table";
        List<String> lineList = new ArrayList<String>();
        if ( hasUsage ) {
            lineList.add( "def " + fname + "(" + tArgName + ", *args):" );
        }
        else {
            lineList.add( "def " + fname + "(" + tArgName + "):" );
        }
        lineList.add( "    '''\\" );
        lineList.addAll( Arrays.asList( formatXml( filter
                                                  .getDescription() ) ) );
        lineList.add( "" );
        lineList.add( "The filtered table is returned." );
        if ( hasUsage ) {
            lineList.add( "" );
            lineList.add( "args is a list with words as elements:" );
            lineList.addAll( Arrays
                            .asList( prefixLines( "    ",
                                                  filter.getUsage() ) ) );
        }
        lineList.add( "'''" );
        lineList.add( "    pfilt = " + getImportName( StepFactory.class )
                           + ".getInstance()"
                           + ".getFilterFactory()"
                           + ".createObject(\"" + filterNickName + "\")" );
        if ( hasUsage ) {
            lineList.add( "    sargs = [str(a) for a in args]" );
        }
        else {
            lineList.add( "    sargs = []" );
        }
        lineList.add( "    argList = " + getImportName( ArrayList.class )
                                       + "(sargs)" );
        lineList.add( "    step = pfilt.createStep(argList.iterator())" );
        lineList.add( "    return import_star_table(step.wrap(" + tArgName
                                                                + "))" );
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining an output mode function.
     *
     * @param  fname  name of function
     * @param  modeNickName  name under which the mode is known in the
     *         mode ObjectFactory
     * @param  isBound  true for a bound method, false for a standalone function
     * @return  python source code lines
     */
    private String[] defMode( String fname, String modeNickName,
                              boolean isBound )
            throws LoadException, SAXException {
        ProcessingMode mode =
            stilts_.getModeFactory().createObject( modeNickName );

        /* Assemble mandatory and optional parameters. */
        Parameter<?>[] params = mode.getAssociatedParameters();
        List<String> lineList = new ArrayList<String>();
        List<Arg> mandArgList = new ArrayList<Arg>();
        List<Arg> optArgList = new ArrayList<Arg>();
        for ( int ip = 0; ip < params.length; ip++ ) {
            Parameter<?> param = params[ ip ];
            String name = param.getName();
            if ( paramAliasMap_.containsKey( name ) ) {
                param.setName( paramAliasMap_.get( name ) );
            }
            String pname = param.getName();
            String sdflt = getDefaultString( param );
            if ( sdflt == null ) {
                mandArgList.add( new Arg( param, pname ) );
            }
            else {
                optArgList.add( new Arg( param, pname + "=" + sdflt ) );
            }
        }

        /* Begin function definition. */
        String tArgName = isBound ? "self" : "table";
        List<Arg> argList = new ArrayList<Arg>();
        argList.addAll( mandArgList );
        argList.addAll( optArgList );
        StringBuffer sbuf = new StringBuffer()
            .append( "def " )
            .append( fname )
            .append( "(" )
            .append( tArgName );
        for ( Arg arg : argList ) {
            sbuf.append( ", " );
            sbuf.append( arg.formalArg_ );
        }
        sbuf.append( "):" );

        /* Add doc string. */
        lineList.add( sbuf.toString() );
        lineList.add( "    '''\\" );
        lineList.addAll( Arrays.asList( formatXml( mode.getDescription() ) ) );
        lineList.addAll( Arrays.asList( getParamDocs( params ) ) );
        lineList.add( "'''" );

        /* Create and populate execution environment. */
        lineList.add( "    env = _JyEnvironment()" );
        for ( Arg arg : argList ) {
            Parameter<?> param = arg.param_;
            String name = param.getName();
            lineList.add( "    env.setValue('" + name + "', "
                                          + "_map_env_value(" + name + "))" );
        }

        /* Create and invoke a suitable TableConsumer. */
        lineList.add( "    mode = _stilts"
                              + ".getModeFactory()"
                              + ".createObject('" + modeNickName + "')" );
        lineList.add( "    consumer = mode.createConsumer(env)" );
        lineList.add( "    _check_unused_args(env)" );
        lineList.add( "    consumer.consume(" + tArgName + ")" );

        /* Return the source code lines. */
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Generates python source defining a function for invoking a STILTS task.
     *
     * @param  fname  name of function
     * @param  taskNickName  name under which the task is known in the task
     *         ObjectFactory
     * @return  python source code lines
     */
    private String[] defTask( String fname, String taskNickName )
            throws LoadException, SAXException {
        Task task = stilts_.getTaskFactory().createObject( taskNickName );

        /* Identify tasks whose primary output is the table presented to
         * the processing mode. */
        boolean isProducer =
               task instanceof ConsumerTask
            && ((ConsumerTask) task).getOutputMode() instanceof ChoiceMode;
                 
        boolean returnOutput = task instanceof Calc;
        List<String> lineList = new ArrayList<String>();

        /* Get a list of the task parameters omitting those which we
         * don't want for jython purposes. */
        List<Parameter<?>> paramList =
            new ArrayList<Parameter<?>>( Arrays.asList( task.getParameters() ));
        List<Parameter<?>> shunnedList = new ArrayList<Parameter<?>>();
        for ( Iterator<Parameter<?>> it = paramList.iterator();
              it.hasNext(); ) {
            Parameter<?> param = it.next();
            if ( param instanceof AbstractInputTableParameter ) {
                shunnedList.add( ((AbstractInputTableParameter) param)
                                .getStreamParameter() );
            }
            if ( param instanceof InputTableParameter ) {
                param.setDescription( "<p>Input table.</p>" );
            }
            else if ( param instanceof InputTablesParameter ) {
                param.setDescription( "<p>Array of input tables.</p>" );
            }
            if ( param instanceof FilterParameter ||
                 param instanceof InputFormatParameter ||
                 param instanceof OutputFormatParameter ||
                 param instanceof OutputTableParameter ||
                 param instanceof OutputModeParameter ) {
                it.remove();
            }
        }
        paramList.removeAll( shunnedList );
        Parameter<?>[] params = paramList.toArray( new Parameter<?>[ 0 ] );

        /* Get a list of mandatory and optional parameters which we will
         * declare as part of the python function definition. 
         * Currently, we refrain from declaring most of the optional
         * arguments, preferring to use just a catch-all **kwargs.
         * This is because a few of the parameters declared by a Stilts
         * task are dummies of one kind or another, so declaring them
         * is problematic and/or confusing. 
         * The ones we do declare are the positional arguments, which
         * tends to be just a few non-problematic an mandatory ones,
         * as well as any which have had their names aliased, so that
         * this is clear from the documentation. */
        List<Arg> mandArgList = new ArrayList<Arg>();
        List<Arg> optArgList = new ArrayList<Arg>();
        int iPos = 0;
        for ( Parameter<?> param : params ) {
            String name = param.getName();
            if ( paramAliasMap_.containsKey( name ) ) {
                param.setName( paramAliasMap_.get( name ) );
            }
            String pname = param.getName();
            boolean byPos = false;
            int pos = param.getPosition();
            if ( pos > 0 ) {
                iPos++;
                assert pos == iPos;
                byPos = true;
            }
            if ( byPos || paramAliasMap_.containsKey( name ) ) {
                String sdflt = getDefaultString( param );
                if ( sdflt == null ) {
                    mandArgList.add( new Arg( param, pname ) );
                }
                else {
                    optArgList.add( new Arg( param, pname + "=" + sdflt ) );
                }
            }
        }

        /* Begin the function definition. */
        List<Arg> argList = new ArrayList<Arg>();
        argList.addAll( mandArgList );
        argList.addAll( optArgList );
        StringBuffer sbuf = new StringBuffer()
            .append( "def " )
            .append( fname )
            .append( "(" );
        for ( Arg arg : mandArgList ) {
            sbuf.append( arg.formalArg_ )
                .append( ", " );
        }
        sbuf.append( "**kwargs" )
            .append( "):" );
        lineList.add( sbuf.toString() );

        /* Write the doc string. */
        lineList.add( "    '''\\" );
        lineList.add( task.getPurpose() + "." );
        if ( isProducer ) {
            lineList.add( "" );
            lineList.add( "The return value is the resulting table." );
        }
        else if ( returnOutput ) {
            lineList.add( "" );
            lineList.add( "The return value is the output string." );
        }
        lineList.addAll( Arrays.asList( getParamDocs( params ) ) );
        lineList.add( "'''" );

        /* Create the task object. */
        lineList.add( "    task = _stilts"
                                + ".getTaskFactory()"
                                + ".createObject('" + taskNickName + "')" );

        /* Rename parameters as required. */
        lineList.add( "    for param in task.getParameters():" );
        lineList.add( "        pname = param.getName()" );
        lineList.add( "        if pname in " + paramAliasDictName_ + ":" );
        lineList.add( "            param.setName(" + paramAliasDictName_
                                                   + "[pname])" );

        /* Create the stilts execution environment. */
        if ( returnOutput ) {
            lineList.add( "    env = _JyEnvironment(grab_output=True)" );
        }
        else {
            lineList.add( "    env = _JyEnvironment()" );
        }

        /* Populate the environment from the mandatory and optional arguments
         * of the python function.  */
        for ( Arg arg : mandArgList ) {
            Parameter<?> param = arg.param_;
            String name = param.getName();
            lineList.add( "    env.setValue('" + name + "', "
                                          + "_map_env_value(" + name + "))" );
        }
        lineList.add( "    for kw in kwargs.iteritems():" );
        lineList.add( "        key = kw[0]" );
        lineList.add( "        value = kw[1]" );
        lineList.add( "        env.setValue(key, _map_env_value(value))" );

        /* For a consumer task, create a result table and return it. */
        if ( isProducer ) {
            lineList.add( "    table = task.createProducer(env).getTable()" );
            lineList.add( "    _check_unused_args(env)" );
            lineList.add( "    return import_star_table(table)" );
        }

        /* Otherwise execute the task in the usual way. */
        else {
            lineList.add( "    exe = task.createExecutable(env)" );
            lineList.add( "    _check_unused_args(env)" );
            lineList.add( "    exe.execute()" );

            /* If we're returning the output text, retrieve it from the
             * environment, tidy it up, and return it.  Otherwise, the
             * return is None. */
            if ( returnOutput ) {
                lineList.add( "    txt = env.getOutputText()" );
                lineList.add( "    return str(txt.strip())" );
            }
        }

        /* Return the source code lines. */
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Returns the python-optional-argument-type default value string for
     * a parameter.  May be null in the case that the argument must be
     * supplied.
     *
     * @param  param  STILTS parameter
     * @param  default value, suitable for insertion into python source
     */
    private String getDefaultString( Parameter<?> param ) {
        String dflt = param.getStringDefault();
        boolean isDfltNull = dflt == null || dflt.trim().length() == 0;
        boolean nullable = param.isNullPermitted();
        if ( nullable || ! isDfltNull ) {
            if ( isDfltNull ) {
                return "None";
            }
            else if ( param instanceof IntegerParameter ||
                      param instanceof DoubleParameter ) {
                return dflt;
            }
            else {
                return "'" + dflt + "'";
            }
        }
        else {
            return null;
        }
    }

    /**
     * Formats XML text for output to python source, to be inserted
     * within string literal quotes.
     *
     * @param  xml  xml text
     * @return  python source code lines for string literal content
     */
    private String[] formatXml( String xml ) throws SAXException {

        /* Shorten the lines by csub characters, so they don't overrun when
         * formatted with indentation by python help. */
        int csub = 8;
        String text = formatter_.formatXML( xml, csub );
        List<String> lineList = new ArrayList<String>();
        for ( String line : lineIterable( text ) ) {
            if ( line.trim().length() == 0 ) {
                lineList.add( "" );
            }
            else {
                assert "        ".equals( line.substring( 0, csub ) );
                lineList.add( line.substring( csub ) );
            }
        }
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Returns documentation for an array of parameters suitable for
     * insertion into a python literal doc string.
     *
     * @param  params  parameters to document
     * @return  lines of doc text
     */
    private String[] getParamDocs( Parameter<?>[] params ) throws SAXException {
        if ( params.length == 0 ) {
            return new String[ 0 ];
        }
        List<String> lineList = new ArrayList<String>();
        lineList.add( "" );
        lineList.add( "Parameters:" );
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<dl>" );
        for ( int i = 0; i < params.length; i++ ) {
            sbuf.append( UsageWriter.xmlItem( params[ i ], true ) );
        }
        sbuf.append( "</dl>" );
        lineList.addAll( Arrays.asList( formatXml( sbuf.toString() ) ) );
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Add a fixed prefix to each line of an input string.
     *
     * @param  prefix  per-line prefix
     * @param  text  text block, with lines terminated by newline characters
     * @return  python source code lines for string literal content
     */
    private String[] prefixLines( String prefix, String text ) {
        List<String> lineList = new ArrayList<String>();
        for ( String line : lineIterable( text ) ) {
            lineList.add( prefix + line );
        }
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Adds a fixed prefix to each element of a string array.
     *
     * @param  prefix  per-line prefix
     * @param  lines  input line array
     * @return  output line array
     */
    private String[] prefixLines( String prefix, String[] lines ) {
        List<String> lineList = new ArrayList<String>();
        for ( int i = 0; i < lines.length; i++ ) {
            lineList.add( prefix + lines[ i ] );
        }
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Outputs an array of lines through a writer.
     *
     * @param  lines  line array
     * @param  writer  destination stream
     */
    private void writeLines( String[] lines, Writer writer )
            throws IOException {
        BufferedWriter bw = new BufferedWriter( writer );
        for ( int i = 0; i < lines.length; i++ ) {
            bw.write( lines[ i ] );
            bw.newLine();
        }
        bw.newLine();
        bw.flush();
    }

    /**
     * Outputs the python source code for the stilts module.
     *
     * @param  writer  destination stream
     */
    public void writeModule( Writer writer )
            throws IOException, LoadException, SAXException {
        writeLines( header(), writer );
        writeLines( imports(), writer );
        writeLines( defTableClass( "JyStarTable" ), writer );
        writeLines( defUtils(), writer );
        writeLines( defVersionCheck(), writer );
        writeLines( defRead( "tread" ), writer );
        writeLines( defReads( "treads" ), writer );
        writeLines( defWrite( "twrite", false ), writer );
        writeLines( defWrites( "twrites" ), writer );
        writeLines( defFilter( "tfilter" ), writer );

        /* Write task wrappers. */
        ObjectFactory<Task> taskFactory = stilts_.getTaskFactory();
        String[] taskNames = taskFactory.getNickNames();
        for ( int i = 0; i < taskNames.length; i++ ) {
            String name = taskNames[ i ];
            String[] taskLines = defTask( name, name );
            writeLines( taskLines, writer );
        }

        /* Write filter wrappers. */
        ObjectFactory<ProcessingFilter> filterFactory =
            StepFactory.getInstance().getFilterFactory();
        String[] filterNames = filterFactory.getNickNames();
        for ( int i = 0; i < filterNames.length; i++ ) {
            String name = filterNames[ i ];
            String[] filterLines = defCmd( "cmd_" + name, name, false );
            writeLines( filterLines, writer );
        }

        /* Write mode wrappers. */
        ObjectFactory<ProcessingMode> modeFactory = stilts_.getModeFactory();
        String[] modeNames = modeFactory.getNickNames();
        for ( int i = 0; i < modeNames.length; i++ ) {
            String name = modeNames[ i ];
            String[] modeLines = defMode( "mode_" + name, name, false );
            writeLines( modeLines, writer );
        }
    }

    /**
     * Writes jython source code for a <code>stilts.py</code> module
     * to standard output.
     * No arguments.
     */
    public static void main( String[] args )
            throws IOException, LoadException, SAXException {
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" )
              .setLevel( Level.WARNING );
        new JyStilts( new Stilts() )
           .writeModule( new OutputStreamWriter(
                             new BufferedOutputStream( System.out ) ) );
    }

    /**
     * Convenience class which aggregates a parameter and its string 
     * representation in a python function definition formal parameter
     * list.
     */
    private static class Arg {
        final Parameter<?> param_;
        final String formalArg_;
        Arg( Parameter<?> param, String formalArg ) {
            param_ = param;
            formalArg_ = formalArg;
        }
    }

    /**
     * Returns an iterator over newline-separated lines in a string.
     * Unlike using StringTokenizer, empty lines will be included in
     * the output.
     *
     * @param  text  input text
     * @return   iterator over lines
     */
    private static Iterable<String> lineIterable( final String text ) {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private int pos_;
                    public boolean hasNext() {
                        return pos_ < text.length();
                    }
                    public String next() {
                        int nextPos = text.indexOf( '\n', pos_ );
                        if ( nextPos < 0 ) {
                            nextPos = text.length();
                        }
                        String line = text.substring( pos_, nextPos );
                        pos_ = nextPos + 1;
                        return line;
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Adapter to turn an OutputStream into a Writer.
     * Any attempt to write a non-ASCII character generates an IOException.
     */
    private static class OutputStreamWriter extends Writer {
        private final OutputStream out_;

        /**
         * Constructor.
         *
         * @param  out  destination stream
         */
        OutputStreamWriter( OutputStream out ) {
            out_ = out;
        }

        public void write( char[] cbuf, int off, int len ) throws IOException {
            byte[] buf = new byte[ len ];
            for ( int i = 0; i < len; i++ ) {
                buf[ i ] = toByte( cbuf[ off + i ] );
            }
            out_.write( buf, 0, len );
        }

        public void write( int c ) throws IOException {
            out_.write( toByte( (char) c ) );
        }

        public void flush() throws IOException {
            out_.flush();
        }

        public void close() throws IOException {
            out_.close();
        }

        /**
         * Turns a char into a byte, throwing an exception in case of
         * narrowing issues.
         *
         * @param  c  character
         * @return  equivalent ASCII byte
         */
        private byte toByte( char c ) throws IOException {
            if ( c >= 0 && c <= 127 ) {
                return (byte) c;
            }
            else if ( Character.isSpaceChar( c ) ) {
                return (byte) ' ';
            }
            else {
                throw new IOException(
                    "Non-ASCII character 0x" + Integer.toHexString( (int) c ) );
            }
        }
    }
}
