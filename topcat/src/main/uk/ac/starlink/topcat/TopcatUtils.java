package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.MetaCopyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.task.InvokeUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.URLUtils;

/**
 * Class containing miscellaneous static methods and constants 
 * for use in TOPCAT.
 *
 * @author   Mark Taylor
 * @since    19 Aug 2004
 */
public class TopcatUtils {

    private static Boolean canJel_;
    private static String[] about_;
    private static String version_;
    private static String stilVersion_;
    private static DecimalFormat longFormat_;
    private static Map<Object,Object> statusMap_;
    private static Boolean canBrowse_;
    private static Consumer<URL> urlHandler_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    static final TopcatCodec DFLT_SESSION_ENCODER;
    static final TopcatCodec[] SESSION_DECODERS = new TopcatCodec[] {
        DFLT_SESSION_ENCODER = new TopcatCodec2(),
        new TopcatCodec1(),
    };

    public static String DEMO_LOCATION = "uk/ac/starlink/topcat/demo";
    public static String DEMO_TABLE = "6dfgs_mini.xml.bz2";
    public static String DEMO_NODES = "demo_list";
    public static final String VERSION_RESOURCE = "version-string";
    public static final String STATUS_URL =
        "http://www.starlink.ac.uk/topcat/topcat-status";

    /**
     * Column auxiliary metadata key identifying the uniqe column identifier
     * for use in algebraic expressions.
     */
    public static final ValueInfo COLID_INFO =
        new DefaultValueInfo( TopcatJELRowReader.COLUMN_ID_CHAR + "ID",
                              String.class, "Unique column ID" );

    /**
     * Column auxiliary metadata key identifying the text string which
     * gives an expression for a synthetic column.
     */
    public final static ValueInfo EXPR_INFO =
        new DefaultValueInfo( "Expression", String.class,
                              "Algebraic expression for column value" );

    /**
     * Column auxiliary metadata key identifying an object which can convert
     * from non-numeric cell values to numeric ones.
     */
    public final static ValueInfo NUMERIC_CONVERTER_INFO =
        new DefaultValueInfo( "Decoder", ValueConverter.class,
                              "Converts from string to numeric values" );

    /**
     * Data identifier for epoch-type data.
     */
    public final static ValueInfo TIME_INFO =
        new DefaultValueInfo( "time", Number.class, "Epoch" );
    static {
        ((DefaultValueInfo) TIME_INFO).setUCD( "TIME" );
    }

    /**
     * Returns the table represented by the current state of a given
     * TopcatModel in a form suitable for persisting into one of the
     * known serialization formats.
     *
     * <p>This basicaly uses {@link TopcatModel#getApparentStarTable},
     * but may apply a few extra tweaks for a table that is known to be
     * about to be saved.
     *
     * @param   tcModel   topcat model
     * @return   saveable table
     */
    public static StarTable getSaveTable( TopcatModel tcModel ) {

        /* Get the apparent table. */
        StarTable table =
            new MetaCopyStarTable( tcModel.getApparentStarTable() );
        int ncol = table.getColumnCount();

        /* Remove metadata that's only intended for internal topcat use. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            List<DescribedValue> auxData = cinfo.getAuxData();
            for ( Iterator<DescribedValue> it = auxData.iterator();
                  it.hasNext(); ) {
                DescribedValue dval = it.next();
                if ( TopcatUtils.COLID_INFO.equals( dval.getInfo() ) ) {
                    it.remove();
                }
            }
        }

        /* Identify synthetic columns, and adjust their column descriptions
         * to include the expression that defines them.
         * Before a table save (though not session save) operation is
         * an appropriate time to do this, since the expression calculation
         * behaviour will be lost following this serialization. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            String expr = cinfo.getAuxDatumValue( EXPR_INFO, String.class );
            if ( expr != null ) {
                String desc0 = cinfo.getDescription();
                String descrip = desc0 == null || desc0.trim().length() == 0
                               ? "Expression: " + expr
                               : desc0 + " (Expression: " + expr + ")";
                cinfo.setDescription( descrip );
            }
        }

        /* Return the result. */
        return table;
    }

    /**
     * Returns the expression text for a column.
     * This should only have a non-null value for synthetic columns.
     *
     * @param  info   column info
     * @return  synthetic expression string
     */
    public static String getExpression( ColumnInfo info ) {
        DescribedValue exprValue = info.getAuxDatum( EXPR_INFO );
        if ( exprValue == null ) {
            return null;
        }
        else {
            Object expr = exprValue.getValue();
            return expr == null ? null : expr.toString();
        }
    }

    /**
     * Returns the base name of a column; that is one without any 
     * suffix based on <code>baseSuffix</code>.
     * This method is used in conjunction with {@link #getDistinctName}.
     *
     * @param  origName  full name, possibly including bits of suffix
     * @param  baseSuffix   the base suffix string
     * @return  name without any suffix-like elements of the sort 
     *          specified by <code>baseSuffix</code>
     */
    public static String getBaseName( String origName, String baseSuffix ) {
        return suffixPattern( baseSuffix )
              .matcher( origName )
              .replaceFirst( "" );
    }

    /**
     * Returns a column name based on a given one which is guaranteed 
     * distinct from any others in the column list.
     * If the submitted <code>origName</code> is already unique, it may be
     * returned.  Otherwise a new name may be made which involves appending
     * the given <code>baseSuffix</code> to it.
     *
     * @param  colList  column list within which distinct naming is required
     * @param  origName  initial name
     * @param  baseSuffix  suffix used for deduplication
     * @return  a name resembling <code>origName</code> which is not the same
     *          as any existing column names in <code>colList</code>
     * @see  #getBaseName
     */
    public static String getDistinctName( ColumnList colList, String origName, 
                                          String baseSuffix ) {
        Pattern suffixPattern = suffixPattern( baseSuffix );

        /* Get the base name - that's one as it would have been prior to
         * having been mangled by this process before. */
        String baseName = getBaseName( origName, baseSuffix );
        int baseLeng = baseName.length();

        /* Go through all the known columns looking for ones that match this
         * suffix pattern. */
        int ncol = colList.size();
        int nextFreeIndex = 0;
        boolean unique = true;
        for ( int ic = 0; ic < ncol; ic++ ) {
            String colName = ((StarTableColumn) colList.getColumn( ic ))
                            .getColumnInfo().getName();
            if ( colName.startsWith( baseName ) ) {
                if ( colName.equals( baseName ) ) {
                    unique = false;
                }
                Matcher matcher = suffixPattern
                                 .matcher( colName.substring( baseLeng ) );
                if ( matcher.matches() ) {
                    unique = false;
                    String suffIndex = matcher.group( 1 );
                    int isuf = ( suffIndex == null || suffIndex.length() == 0 )
                             ? 0
                             : Integer.parseInt( suffIndex );
                    if ( isuf >= nextFreeIndex ) {
                        nextFreeIndex = isuf + 1;
                    }
                }
            }
        }

        /* Calculate and return the new column name. */
        if ( unique ) {
            return origName;
        }
        else {
            String newName = baseName + baseSuffix;
            if ( nextFreeIndex > 0 ) {
                newName += nextFreeIndex;
            }
            return newName;
        }
    }

    /**
     * Returns a Pattern object which will match a suffix of the sort
     * used by {@link #getDistinctName}.
     *
     * @param   baseSuffix  suffix base string
     * @return  end-of-string matching pattern
     * @see    #getDistinctName
     * @see    #getBaseName
     */
    private static Pattern suffixPattern( String baseSuffix ) {
        return Pattern.compile( "\\Q" + baseSuffix + "\\E" + "([0-9]*)$" );
    }

    /**
     * Returns the name of this application.
     *
     * @return  "TOPCAT"
     */
    public static String getApplicationName() {
        return "TOPCAT";
    }

    /**
     * Returns some lines of text describing this copy of the software
     * including its version and versions of some important components.
     *
     * @return   lines of About text
     */
    public static String[] getAbout() {
        if ( about_ == null ) {
            about_ = new String[] {
                "This is TOPCAT - Tool for OPerations on Catalogues And Tables",
                "",
                "TOPCAT Version " + getVersion(),
                "STIL Version " + getSTILVersion(),
                "Starjava revision: " + getRevision(),
                "JVM: " + InvokeUtils.getJavaVM(),
                "",
                "Author: Mark Taylor (Bristol University)",
                "WWW: http://www.starlink.ac.uk/topcat/",
            };
        }
        return about_;
    }

    /**
     * Returns an object that will take documentation-type URLs
     * and display them somehow, probably in an external browser window.
     *
     * @return  url handler, or null if browsing is not possible
     */
    public static Consumer<URL> getDocUrlHandler() {
        if ( urlHandler_ == null ) {
            final Desktop desktop = getBrowserDesktop();
            final Consumer<URL> handler;
            if ( desktop == null ) {
                logger_.warning( "No browser control"
                               + "; clicking URLs won't open them" );
                handler = url -> {
                    logger_.info( "Clicked URL: " + url );
                };
            }
            else {
                handler = url -> {
                    logger_.info( "Passing URL to browser: " + url );
                    try {
                        desktop.browse( url.toURI() );
                    }
                    catch ( Throwable e ) {
                        logger_.log( Level.WARNING,
                                     "Trouble sending URL " + url
                                   + " to browser", e );
                    }
                };
            }
            urlHandler_ = handler;
        }
        assert urlHandler_ != null;
        return urlHandler_;
    }

    /**
     * Returns the values from a row of a given table as a list of
     * DescribedValues, suitable for use as parameters (per-value metadata)
     * of a StarTable.
     *
     * @param  tcModel  table supplying values
     * @param  lrow    row index
     * @return   list of described values
     */
    public static List<DescribedValue> getRowAsParameters( TopcatModel tcModel,
                                                           long lrow ) {
        StarTable table = tcModel.getDataModel();
        TableColumnModel tcm = tcModel.getColumnModel();
        int ncol = tcm.getColumnCount();
        List<DescribedValue> list = new ArrayList<DescribedValue>( ncol );
        try {
            Object[] row = table.getRow( lrow );
            for ( int icol = 0; icol < ncol; icol++ ) {
                int jcol = tcm.getColumn( icol ).getModelIndex();
                ValueInfo info =
                    new DefaultValueInfo( table.getColumnInfo( jcol ) );
                list.add( new DescribedValue( info, row[ jcol ] ) );
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Trouble reading parameters: " + e, e );
        }
        return list;
    }

    /**
     * Sets the text content of the system clipboard(s).
     * This is somewhat OS-dependent.  X11 uses a PRIMARY selection
     * (middle mouse button) alongside the CLIPBOARD selection
     * (explicit cut'n'paste).  JTextComponents fill both, though
     * not under exactly the same circumstances.  This method sets the
     * text for both if both are available.
     * This may not replicate exactly the behaviour expected by X clients,
     * but I think it's what users would want to happen.  I may be wrong.
     *
     * @param  txt  text to set as clipboard contents
     */
    public static void setClipboardText( String txt ) {
        StringSelection selection = new StringSelection( txt );
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        /* Set the system clipboard to the given text. */
        toolkit.getSystemClipboard().setContents( selection, null );

        /* If there's a primary clipboard, set the text there too. */
        Clipboard primary = toolkit.getSystemSelection();
        if ( primary != null ) {
            primary.setContents( selection, selection );
        }
    }

    /**
     * Scrolls a JTable as necessary to ensure that a given row index
     * is scrolled to the visible part of the viewport.
     *
     * @param  jtab  JTable
     * @param  irow   table index that must be visible
     * @see  javax.swing.JList#ensureIndexIsVisible(int)
     */
    public static void ensureRowIndexIsVisible( JTable jtab, int irow ) {
        Container tParent = jtab.getParent();
        if ( tParent instanceof JViewport ) {
            Rectangle vRect = ((JViewport) tParent).getViewRect();
            Rectangle cRect = jtab.getCellRect( irow, 0, true );
            Rectangle targetRect =
                new Rectangle( vRect.x, cRect.y, vRect.width, cRect.height );
            jtab.scrollRectToVisible( targetRect );
        }
    }

    /**
     * Alerts the user that the system has run out of memory, and provides
     * the option of some useful tips.
     *
     * @param   e  exception, or null
     */
    public static void memoryError( OutOfMemoryError e ) {
        ControlWindow control = ControlWindow.getInstance();
        String nullOpt = "OK";
        String helpOpt = "Help!";
        String[] options = new String[] { nullOpt, helpOpt };
        String msg = e.getMessage();
        if ( msg == null || msg.trim().length() == 0 ) {
            msg = "Out of memory";
        }
        String title = "Out Of Memory";
        int iopt = JOptionPane.showOptionDialog( control, msg, title,
                                                 JOptionPane.DEFAULT_OPTION,
                                                 JOptionPane.ERROR_MESSAGE,
                                                 null, options, helpOpt );
        if ( iopt > 0 && iopt < options.length &&
             helpOpt.equals( options[ iopt ] ) ) {
            HelpWindow helpWin = HelpWindow.getInstance( control );
            helpWin.makeVisible();
            helpWin.setID( "largeTables" );
        }
    }

    /**
     * Queues a {@link #memoryError} call for later execution on the
     * event dispatch thread.
     *
     * @param   e  exception, or null
     */
    public static void memoryErrorLater( final OutOfMemoryError e ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                memoryError( e );
            }
        } );
    }

    /**
     * Indicates if it's possible to use JEL to compile algebraic expressions.
     *
     * <p>This used to query the SecurityManager, but the
     * <code>java.lang.SecurityManager</code> class is
     * deprecated for removal in later JREs, and I don't think it's
     * likely that expression compilation will be blocked.
     * So this method currently returns true unconditionally,
     * but retain it in case it turns out there are circumstances
     * we can test for that bear on this.
     *
     * @return   true iff JEL epxression compilation is expected to work
     */
    public static boolean canJel() {
        return true;
    }

    /**
     * Returns a browse-capable desktop instance, or null if none is available.
     *
     * @return  desktop
     */
    public synchronized static Desktop getBrowserDesktop() {
        if ( canBrowse_ == null ) {
            boolean canBrowse =
                   Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported( Desktop.Action.BROWSE );
            if ( ! canBrowse ) {
                logger_.warning( "Can't send URLs to browser"
                               + " (no Desktop.Action.BROWSE)" );
            }
            canBrowse_ = Boolean.valueOf( canBrowse );
        }
        return canBrowse_.booleanValue() ? Desktop.getDesktop()
                                         : null;
    }

    /**
     * Returns the version string for this copy of TOPCAT.
     *
     * @return  version number only
     */
    public static String getVersion() {
        return IOUtils.getResourceContents( AuxWindow.class, VERSION_RESOURCE,
                                            null );
    }

    /**
     * Returns the version control revision number
     * for TOPCAT and its dependencies.
     *
     * @return   revision identifier
     */
    public static String getRevision() {
        return IOUtils.getResourceContents( AuxWindow.class, "revision-string",
                                            Level.CONFIG );
    }

    /**
     * Returns the version string for the version of STIL being used here.
     *
     * @return  STIL version number
     */
    public static String getSTILVersion() {
        return IOUtils.getResourceContents( StarTable.class, "stil.version",
                                            null );
    }

    /**
     * Ascertains the most recent release using an external connection,
     * and reports through the logging system as appropriate.
     */
    public static void enquireLatestVersion() {
        if ( statusMap_ == null ) {
            try {
                Properties versionProps = new Properties();
                versionProps.load( openWellKnownUrlStream( STATUS_URL ) );
                statusMap_ = new HashMap<Object,Object>( versionProps );
            }
            catch ( Throwable e ) {
                statusMap_ = new HashMap<Object,Object>();
            }
            String currentVersion = getVersion();
            Object latestVers = statusMap_.get( "topcat.latest.version" );
            String latestVersion = latestVers instanceof String
                                 ? (String) latestVers
                                 : null;
            StringBuffer statusBuf = new StringBuffer()
                .append( "This is TOPCAT version " )
                .append( currentVersion );
            boolean isOld = false;
            if ( latestVersion != null ) {
                try {
                    isOld = new Version( latestVersion )
                           .compareTo( new Version( currentVersion ) ) > 0;
                    if ( isOld ) {
                        statusBuf.append( " (out of date - latest is " )
                                 .append( latestVersion )
                                 .append( ")" );
                    }
                    else {
                        statusBuf.append( " (up to date)" );
                    }
                }
                catch ( Throwable e ) {
                    logger_.info( "Version arithmetic error: " + e );
                }
            }
            logger_.log( isOld ? Level.INFO : Level.INFO,
                         statusBuf.toString() );
        }
    }

    /**
     * Determines whether two objects are equal in the sense of 
     * {@link java.lang.Object#equals}.  Unlike that method however,
     * it returns true if both objects are <code>null</code>, and
     * won't throw a NullPointerException.
     *
     * @param  o1  first object
     * @param  o2  second object
     * @return   true if <code>o1.equals(o2)</code> or they're both null
     */
    public static boolean equals( Object o1, Object o2 ) {
        return ( o1 == null && o2 == null )
            || ( o1 != null && o2 != null && o1.equals( o2 ) );
    }

    /**
     * Returns a string unique to an object's identity.
     * This is modelled on the value that Object.toString() usually returns,
     * though not guaranteed to be the same.
     *
     * @param   obj  object
     * @return  unique string for object
     */
    public static String identityString( Object obj ) {
        return new StringBuffer()
              .append( obj.getClass().getName() )
              .append( '@' )
              .append( Integer.toHexString( System.identityHashCode( obj ) ) )
              .toString();
    }

    /**
     * Formats a long value for presentation as text.
     * This typically puts separators between groups of three numbers for
     * improved visibility.  TOPCAT policy is usually to do this only for
     * numbers which are, or might be expected to be, quite large.
     *
     * @param   num   number to format
     * @return   formatted value
     */
    public static String formatLong( long num ) {
        if ( longFormat_ == null ) {
            longFormat_ = new DecimalFormat();
        }
        return longFormat_.format( num );
    }

    /**
     * Reshapes a set of components so that they all have the same
     * preferred size (that of the largest one).
     *
     * @param   comps  components to align
     */
    public static void alignComponents( JComponent[] comps ) {
        int maxw = 0;
        int maxh = 0;
        for ( int i = 0; i < comps.length; i++ ) {
            Dimension prefSize = comps[ i ].getPreferredSize();
            maxw = Math.max( maxw, prefSize.width );
            maxh = Math.max( maxh, prefSize.height );
        }
        Dimension prefSize = new Dimension( maxw, maxh );
        for ( int i = 0; i < comps.length; i++ ) {
            comps[ i ].setPreferredSize( prefSize );
        }
    }

    /**
     * Displays an informational About TOPCAT window.
     *
     * @param  parent  parent component
     */
    public static void showAbout( Component parent ) {
        Object[] msg = new Object[] {
            TopcatUtils.getAbout(),
            Box.createVerticalStrut( 10 ),
            AuxWindow.getSponsorLogos(),
        };
        JOptionPane.showMessageDialog( parent, msg, "About TOPCAT",
                                       JOptionPane.INFORMATION_MESSAGE,
                                       ResourceIcon.getTopcatLogo() );
    }

    /**
     * Invokes the java.awt.Desktop.setAboutHandler method.
     * Since that method was introduced at Java 9, and at time of writing
     * the build and minimal target deployment platform is Java 8,
     * this is done by reflection, and hence will only do useful work
     * in a runtime environment of &gt;=9.  
     * This method only does useful work anyway on certain platforms,
     * notably MacOS, for which an About button is part of the standard
     * application GUI.
     * If for whatever reason the assignment doesn't work, it fails silently.
     *
     * <p>If the target platform is upgraded one day to Java &gt;= 9,
     * this reflective code can be eliminated or replaced by something
     * very much simpler.
     *
     * @param  doAbout  callback to display an About Application window
     * @return  true iff the handler installation apparently succeeded
     */
    public static boolean setAboutHandler( Runnable doAbout ) {

        /* See if the AboutHandler interface exists.
         * If not, we're using a java version <9, so none of this reflection
         * will work; bail out. */
        final Class<?> aboutHandlerClazz;
        try {
            aboutHandlerClazz =
                Class.forName( "java.awt.desktop.AboutHandler" );
        }
        catch ( Throwable e ) {
            return false;
        }
        try {

            /* Create an AboutHandler instance whose only method (handleAbout)
             * runs the supplied callback code.  This has to be a proxy. */
            InvocationHandler handler = new InvocationHandler() {
                public Object invoke( Object proxy, Method method,
                                      Object[] args ) {
                    if ( "handleAbout".equals( method.getName() ) ) {
                        doAbout.run();
                    }
                    return null;
                }
            };
            Object aboutHandler =
                Proxy.newProxyInstance( TopcatUtils.class.getClassLoader(),
                                        new Class<?>[] { aboutHandlerClazz },
                                        handler );

            /* Locate and invoke the Desktop.setAboutHandler method
             * reflectively. */
            Method setAboutHandlerMethod =
                Arrays.stream( Desktop.class.getMethods() )
                      .filter( m -> "setAboutHandler".equals( m.getName() ) )
                      .findAny()
                      .get();
            setAboutHandlerMethod.invoke( Desktop.getDesktop(), aboutHandler );
            return true;
        }

        /* setAboutHandler fails with an UnsupportedOperationException on
         * non-Mac platforms. */
        catch ( UnsupportedOperationException e ) {
            return false;
        }

        /* Otherwise it looks like we're on Java 9, but something went wrong,
         * probably with the reflection.  Oh well. */
        catch ( Throwable e ) {
            logger_.log( Level.INFO,
                         "Slightly surprising reflection failure", e );
            return false;
        }
    }

    /**
     * Using input from the user, adds a new (or reused) Row Subset
     * to the given TopcatModel based on a given BitSet.
     *
     * @param  parent  parent component for dialogue
     * @param  tcModel  topcat model
     * @param  matchMask  mask for included rows
     * @param  dfltName  default name for subset
     * @param  msgLines  lines of text to appear in dialogue window
     * @param  title   dialogue window title
     */
    public static void addSubset( JComponent parent, TopcatModel tcModel,
                                  BitSet matchMask, String dfltName,
                                  String[] msgLines, String title ) {
        int nmatch = matchMask.cardinality();
        Box nameLine = Box.createHorizontalBox();
        JComboBox<String> nameSelector = tcModel.createNewSubsetNameSelector();
        nameSelector.setSelectedItem( dfltName );
        nameLine.add( new JLabel( "Subset name: " ) );
        nameLine.add( nameSelector );
        List<Object> msgList = new ArrayList<Object>();
        msgList.addAll( Arrays.asList( msgLines ) );
        msgList.add( nameLine );
        int opt = JOptionPane
                 .showOptionDialog( parent, msgList.toArray(), title,
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null, null, null );
        String name = getSubsetName( nameSelector );
        if ( opt == JOptionPane.OK_OPTION && name != null ) {
            tcModel.addSubset( new BitsRowSubset( name, matchMask ) );
        }
    }

    /**
     * Encodes a TopcatModel as a StarTable including per-table session
     * information, suitable for serialization.
     *
     * @param  tcModel   model
     * @return   table
     */
    public static StarTable encodeSession( TopcatModel tcModel ) {
        StarTable table = DFLT_SESSION_ENCODER.encode( tcModel );
        assert DFLT_SESSION_ENCODER.isEncoded( table );
        return table;
    }

    /**
     * Attempts to unpack a StarTable into a TopcatModel containing
     * per-table application session information.
     * For this to work it must have been written using one of
     * the TopcatCodec formats that this application is aware of.
     * If not, null is returned.
     *
     * @param  table  encoded table
     * @param  location  table location string
     * @param  controlWindow  control window, or null if necessary
     * @return   topcat model, or null
     */
    public static TopcatModel decodeSession( StarTable table, String location,
                                             ControlWindow controlWindow ) {
        for ( TopcatCodec codec : SESSION_DECODERS ) {
            if ( codec.isEncoded( table ) ) {
                return codec.decode( table, location, controlWindow );
            }
        }
        return null;
    }

    /**
     * Returns the subset name corresponding to the currently
     * selected value of a row subset selector box.
     *
     * @param rsetNameSelector  combo box returned by
     *        TopcatModel.createNewSubsetNameSelector
     * @return   subset name as string, or null
     */
    private static String getSubsetName( JComboBox<String> rsetNameSelector ) {
        Object item = rsetNameSelector.getSelectedItem();
        if ( item == null ) {
            return null;
        }
        else if ( item instanceof String ) {
            String name = (String) item;
            return name.trim().length() > 0 ? name : null;
        }
        else {
            assert false;
            return item.toString();
        }
    }

    /**
     * Converts an HTTP URL to an HTTPS one that is the same apart from
     * the protocol part.
     *
     * @param   httpUrl  URL assumed to start "http://"
     * @return   same as input but starting "https://";
     */
    static String toHttpsUrl( String httpUrl ) {
        return httpUrl.replaceFirst( "^http:", "https:" );
    }

    /**
     * Attempts to open a connection to a given URL.
     * Redirects, including http->https redirects, are followed.
     * If it fails, an attempt is made using an https version of the same URL.
     *
     * @param   url    HTTP-protocol URL to open
     * @return  input stream containing non-error resource content, not null
     * @throws  IOException  if it can't be contacted
     */
    private static InputStream openWellKnownUrlStream( String url )
            throws IOException {

        /* First try the normal URL. */
        IOException e1 = null;
        try {
            URLConnection conn =
                URLUtils.followRedirects( URLUtils.newURL( url )
                                                  .openConnection(),
                                          (int[]) null );
            if ( conn instanceof HttpURLConnection &&
                 ((HttpURLConnection) conn).getResponseCode() == 200 ) {
                return conn.getInputStream();
            }
        }
        catch ( IOException e ) {
            e1 = e;
        }

        /* If that failed, try an HTTPS version of the same thing. */
        String url2 = toHttpsUrl( url );
        if ( ! url.equals( url2 ) ) {
            try {
                URLConnection conn2 =
                    URLUtils
                   .followRedirects( URLUtils.newURL( url2 ).openConnection(),
                                     (int[]) null );
                if ( conn2 instanceof HttpURLConnection &&
                     ((HttpURLConnection) conn2).getResponseCode() == 200 ) {
                    return conn2.getInputStream();
                }
            }
            catch ( IOException e2 ) {
                // nope
            }
        }

        /* If no success but the first attempt resulted in an exception,
         * throw that. */
        if ( e1 != null ) {
            throw e1;
        }

        /* Otherwise, it just wasn't there. */
        throw new FileNotFoundException( url + ", " + url2 );
    }
}
