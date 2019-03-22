package uk.ac.starlink.ttools.build;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.PdfGraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.PictureImageIcon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;
import uk.ac.starlink.util.XmlWriter;

/**
 * Instances of this class represent a given figure plotted using
 * the plot2 plotting classes and some externally supplied data.
 * A number of instances are defined, representing examples of
 * various different aspects of the plotting classes.
 * The main method provides options for external invocation to
 * plot the figures to the screen or to external graphics files,
 * list the required data files, etc.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2017
 */
public class Plot2Example {

    private final String label_;
    private final Context context_;
    private final PlotTask task_;
    private final String[] params_;

    /**
     * Constructor.
     *
     * @param  label   name of example
     * @param  context   example execution context
     * @param  task   the STILTS task used for the plot
     * @param  params  array of [*!]name=value pairs giving parameters;
     *                 a prepended "*" indicates emphasis;
     *                 a prepended "!" hides it from user view;
     *                 <code>value</code> part may be enclosed in single quotes;
     *                 a null in the list signifies line break for formatting
     */
    public Plot2Example( String label, Context context, PlotTask task,
                         String[] params ) {
        label_ = label;
        context_ = context;
        task_ = task;
        params_ = params;
    }

    /**
     * Returns this example's name.
     *
     * @return  identification string
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the lines representing the stilts command you would type
     * to execute this example.
     *
     * @return  one or more lines of text; intended to be presented in a
     *          fixed-width font
     */
    public String[] getLines() {
        String intro = "stilts " + task_.getName();
        String pad = intro.replaceAll( ".", " " );
        List<String> lines = new ArrayList<String>();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( intro );
        for ( String param : params_ ) {
            if ( param == null ) {
                if ( sbuf.length() > 0 ) {
                    lines.add( sbuf.toString() );
                }
                sbuf = new StringBuffer();
            }
            else {
                if ( sbuf.length() == 0 ) {
                    sbuf.append( pad );
                }
                sbuf.append( ' ' )
                    .append( toPair( param ).txt_ );
            }
        }
        if ( sbuf.length() > 0 ) {
            lines.add( sbuf.toString() );
        }
        return lines.toArray( new String[ 0 ] );
    }

    /**
     * Returns XML text representing the stilts command you would type
     * to execute this example.
     *
     * @return  XML text wrapped in a &lt;verbati&gt; element
     */
    public String getXml() {
        String intro = "   stilts " + task_.getName();
        String pad = intro.replaceAll( ".", " " );
        StringBuffer sbuf = new StringBuffer()
            .append( "<verbatim>" )
            .append( intro );
        for ( String param : params_ ) {
            if ( param == null ) {
                if ( sbuf.length() > 0 ) {
                    sbuf.append( "\n" )
                        .append( pad );
                }
            }
            else {
                String xtxt = toPair( param ).getXml();
                if ( xtxt.length() > 0 ) {
                    sbuf.append( ' ' )
                        .append( xtxt );
                }
            }
        }
        sbuf.append( "</verbatim>" );
        return sbuf.toString();
    }

    /**
     * Constructs an object giving this example's plot ready for output.
     *
     * @param  extraParams  additional name=value strings to apply
     *                      when doing the plot
     * @return  picture
     */
    public Picture createPicture( String[] extraParams ) throws
            IOException, TaskException, InterruptedException, LoadException {
        MapEnvironment env =
            createEnvironment( context_,
                               PlotUtil.arrayConcat( params_, extraParams ) );
        AbstractPlot2Task task = task_.createTask();
        Picture picture = PlotUtil.toPicture( task.createPlotIcon( env ) );
        checkUsedEnvironment( env );
        return picture;
    }

    /**
     * Tests that this example can execute, but does not run the
     * actual plotting code.  Required external data files must be in place.
     */
    public void testParams() throws Exception {
        MapEnvironment env = createEnvironment( context_, params_ );
        task_.createTask().testEnv( env );
        checkUsedEnvironment( env );
    }

    /**
     * Returns the names of table files that must be in place for this
     * example to run.
     *
     * @return   array of required table names
     */
    public String[] getRequiredTableNames() {
        Set<String> tnames = new TreeSet<String>();
        for ( String param : params_ ) {
            if ( param != null ) {
                String value = toPair( param ).value_;
                if ( context_.tableNames_.contains( value ) ) {
                    tnames.add( value );
                }
            }
        }
        return tnames.toArray( new String[ 0 ] );
    }

    /**
     * Returns a map from the names of the known examples to
     * their XML invocation text.
     *
     * @return  label-&gt;verbatim command elements
     */
    public static Map<String,String> getExamplesXml() {
        File dir = new File( "." );
        Plot2Example[] examples =
            createExamples( new Context( dir, TName.NAMES, dir, (URL) null ) );
        Map<String,String> map = new LinkedHashMap<String,String>();
        for ( Plot2Example ex : examples ) {
            map.put( ex.getLabel(), ex.getXml() );
        }
        return map;
    }

    /**
     * Checks examples.  If no data files are present, the test can't
     * be done and a short message is printed to stdout.
     * If at least one data file is present, all examples are checked,
     * and an exception gets thrown in case of a problem.
     *
     * <p>This method is intended for external invocation as a unit test.
     *
     * @param  dataDir  directory that may contain data tables
     */
    static void checkExamples( File dataDir ) throws Exception {
        Context context =
            new Context( dataDir, TName.NAMES, (File) null, (URL) null );
        Plot2Example[] examples = createExamples( context );
        Collection<String> tnames = new TreeSet<String>();
        for ( Plot2Example ex : examples ) {
            tnames.addAll( Arrays.asList( ex.getRequiredTableNames() ) );
        }
        int nHas = 0;
        for ( String tname : tnames ) {
            if ( new File( dataDir, tname ).exists() ) {
                nHas++;
            }
        }
        if ( nHas == 0 ) {
            System.out.println( "No required data tables in " + dataDir
                              + ", skip plot2 example tests" );
        }
        else {
            context.checkHasTables( tnames.toArray( new String[ 0 ] ) );
            for ( Plot2Example ex : examples ) {
                ex.testParams();
            }
        }
    }

    /**
     * Turns a param string into a Pair object.
     * Some checking is performed, and simple surrounding quotes are stripped.
     *
     * <p>The input string is basically of the form name=value.
     * If a "*" is prepended, then the pair is marked for emphasis,
     * if a "!" is prepended, then the pair is hidden from user view.
     * The <code>value</code> part may optionally be contained in single
     * quotes.
     *
     * @param  param  [*!]name=value string
     * @return  pair
     */
    private static Pair toPair( String param ) {
        final Flag flag;
        final String txt;
        switch ( param.charAt( 0 ) ) {
            case '*':
                flag = Flag.BOLD;
                txt = param.substring( 1 );
                break;
            case '!':
                flag = Flag.HIDDEN;
                txt = param.substring( 1 );
                break;
            default:
                flag = Flag.NORMAL;
                txt = param;
        }
        int ieq = txt.indexOf( '=' );
        if ( ieq > 0 ) {
            String key = txt.substring( 0, ieq );
            String rvalue = txt.substring( ieq + 1 );
            int vleng = rvalue.length();
            final String value;
            if ( rvalue.indexOf( '\'' ) == 0 &&
                 rvalue.indexOf( '\'', 1 ) == vleng - 1 ) {
                value = rvalue.substring( 1, vleng - 1 );
            }
            else {
                value = rvalue;
            }
            return new Pair( txt, flag, key, value );
        }
        else {
            throw new IllegalArgumentException( "param \"" + param
                                              + "\" not [*!]key=value" );
        }
    }

    /**
     * Constructs an execution environment for executing this example.
     *
     * @param  context  execution context
     * @param  params  name=value strings (nulls permitted)
     * @return  new environment populated with parameter values
     */
    private static MapEnvironment createEnvironment( Context context,
                                                     String[] params )
            throws IOException {
        Map<String,Object> map = new LinkedHashMap<String,Object>();
        for ( String param : params ) {
            if ( param != null ) {
                Pair pair = toPair( param );
                String key = pair.key_;
                String value = pair.value_;
                StarTable table = context.getTable( value );
                final Object item;
                if ( table != null ) {
                    item = table;
                }
                else if ( map.containsKey( key ) ) {
                    if ( key.startsWith( "cmd" ) ||
                         key.startsWith( "icmd" ) ) {
                        item = ((String) map.get( key ))
                             + new FilterParameter( "dummy" )
                                  .getValueSeparator()
                             + value;
                    }
                    else {
                        throw new IllegalArgumentException( "Multiple values "
                                                          + "for key " + key );
                    }
                }
                else {
                    item = value;
                }
                map.put( key, item );
            }
        }
        for ( Map.Entry<String,Object> entry :
              context.envDefaults_.entrySet() ) {
            String key = entry.getKey();
            if ( ! map.containsKey( key ) ) {
                map.put( key, entry.getValue() );
            }
        }
        return new MapEnvironment( map );
    }

    /**
     * Performs checking on a MapEnvironment after it has been used to
     * execute a command.  In particular it will throw an exception if
     * any of the supplied parameters were not used.
     *
     * @param  env  environment
     * @throws   TaskException if checks fail
     */
    private static void checkUsedEnvironment( MapEnvironment env )
            throws TaskException {
        String[] unused = env.getUnused();
        if ( unused.length > 0 ) {
            throw new TaskException( "Unused args: "
                                   + Arrays.toString( unused ) );
        }
    }

    /**
     * Turns a string into a URL useable as context.
     * That effectively means that the returned URL will have a "/"
     * appended if it didn't already have one.
     *
     * @param  txt  url string
     * @return  context url object
     */
    private static URL toContextUrl( String txt ) throws MalformedURLException {
        if ( txt == null ) {
            return null;
        }
        else if ( txt.endsWith( "/" ) ) {
            return new URL( txt );
        }
        else {
            return new URL( txt + "/" );
        }
    }

    /**
     * Defines common state within which an example is executed.
     */
    private static class Context {

        private final File dataDir_;
        private final Collection<String> tableNames_;
        private final File outDir_; 
        private final URL dataUrl_;
        private final StarTableFactory tableFactory_;
        private final Map<String,StarTable> tableMap_;
        private final Map<String,Object> envDefaults_;

        /**
         * Constructor.
         *
         * @param  dataDir  directory in which data (table) files can be found
         * @param  tableNames   names of tables in dataDir that are relevant
         *                      for this context
         * @param  outDir   directory to which output files will be written
         * @param  dataUrl   URL from which data files can be downloaded
         */
        public Context( File dataDir, String[] tableNames, File outDir,
                        URL dataUrl ) {
            dataDir_ = dataDir;
            tableNames_ = new TreeSet<String>( Arrays.asList( tableNames ) );
            outDir_ = outDir;
            dataUrl_ = dataUrl;
            tableFactory_ = new StarTableFactory();
            tableMap_ = new HashMap<String,StarTable>();
            envDefaults_ = new LinkedHashMap<String,Object>();
            envDefaults_.put( "ypix", "350" );
        }

        /**
         * Returns a table object for the table with a given name known by
         * this context.  Tables are read lazily but retained for future use.
         *
         * @param   table name
         * @return  table, or null if not known
         */
        public synchronized StarTable getTable( String name )
                throws IOException {
            if ( tableNames_.contains( name ) ) {
                if ( ! tableMap_.containsKey( name ) ) {
                    File f = new File( dataDir_, name );
                    tableMap_.put( name,
                                   tableFactory_
                                  .makeStarTable( new FileDataSource( f ) ) );
                }
                return tableMap_.get( name );
            }
            else {
                return null;
            }
        }

        /**
         * Checks that the named tables are available in this context.
         *
         * @throws  FileNotFoundException  if some files not available
         */
        public void checkHasTables( String[] tnames )
                throws FileNotFoundException {
            List<String> missing = new ArrayList<String>();
            for ( String tname : tnames ) {
                if ( ! new File( dataDir_, tname ).exists() ) {
                    missing.add( tname );
                }
            }
            if ( missing.size() > 0 ) {
                throw new FileNotFoundException( "Missing example data files"
                                               + " in " + dataDir_
                                               + ": " + missing );
            }
        }
    }

    /**
     * Defines one of the known stilts plotting tasks.
     */
    private static class PlotTask {
        private final String name_;
        private static ObjectFactory<Task> taskFactory_ =
            Stilts.getTaskFactory();

        /** Plane plot. */
        public static final PlotTask PLANE = new PlotTask( "plot2plane" );

        /** Sky plot. */
        public static final PlotTask SKY = new PlotTask( "plot2sky" );

        /** Cube plot. */
        public static final PlotTask CUBE = new PlotTask( "plot2cube" );

        /** Sphere plot. */
        public static final PlotTask SPHERE = new PlotTask( "plot2sphere" );

        /** Time plot. */
        public static final PlotTask TIME = new PlotTask( "plot2time" );

        /**
         * Constructor.
         *
         * @param  name  name as known to stilts
         * @param  clazz   class 
         */
        PlotTask( String name ) {
            name_ = name;
            createTask();
        }

        /**
         * Returns the stilts task name.
         */
        public String getName() {
            return name_;
        }

        /**
         * Constructs a new AbstractPlot2Task.
         *
         * @return  new stilts task
         */
        public AbstractPlot2Task createTask() {
            try {
                return (AbstractPlot2Task) taskFactory_.createObject( name_ );
            }
            catch ( Throwable e ) {
                throw new RuntimeException( "Trouble creating task " + name_,
                                            e );
            }
        }
    }

    /**
     * Defines execution modes for this class's main method.
     */
    private static enum Mode {

        /** Displays plots on the screen. */
        swing( true ) {
            public void execute( Context context, Plot2Example[] examples )
                    throws Exception {
                for ( Plot2Example ex : examples ) {
                    System.out.println( ex.getLabel() + ":" );
                    for ( String line : ex.getLines() ) {
                        System.out.println( "   " + line );
                    }
                    Picture pic = ex.createPicture( FORCEBITMAP );
                    JComponent picComp =
                        new JLabel( new PictureImageIcon( pic, true ) );
                    final JDialog dialog =
                        new JDialog( (Frame) null, ex.getLabel(), true );
                    dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
                    JComponent holder = (JComponent) dialog.getContentPane();
                    holder.add( picComp );
                    Object quitKey = "quit";
                    holder.getInputMap()
                          .put( KeyStroke.getKeyStroke( 'q' ), quitKey );
                    holder.getActionMap().put( quitKey, new AbstractAction() {
                        public void actionPerformed( ActionEvent evt ) {
                            dialog.dispose(); 
                        }
                    } );
                    dialog.pack();
                    dialog.setVisible( true );
                }
            }
        },

        /** Writes PNG plot files and auxiliary text files to outDir. */
        png( true ) {
            public void execute( Context context, Plot2Example[] examples )
                    throws Exception {
                writeExamples( context, examples, GraphicExporter.PNG, ".png" );
            }
        },

        /** Writes PDF plot files and auxiliary text files to outDir. */
        pdf( true ) {
            public void execute( Context context, Plot2Example[] examples )
                    throws Exception {
                writeExamples( context, examples,
                               PdfGraphicExporter.BASIC, ".pdf" );
            }
        },

        /** Copies missing data files from dataUrl to dataDir. */
        copydata( false ) {
            public void execute( Context context, Plot2Example[] examples )
                    throws IOException {
                File dataDir = context.dataDir_;
                URL dataUrl = context.dataUrl_;
                String intro =
                    "Copying files from " + dataUrl + " to " + dataDir + ":";
                int ic = 0;
                for ( String tname : context.tableNames_ ) {
                    File f = new File( dataDir, tname );
                    if ( ! f.exists() ) {
                        if ( ic++ == 0 ) {
                            System.out.println( intro );
                        }
                        System.out.println( "   " + tname );
                        URL turl = new URL( dataUrl, tname );
                        InputStream in = turl.openStream();
                        OutputStream out = new FileOutputStream( f );
                        IOUtils.copy( in, out );
                        in.close();
                        out.close();
                    }
                }
            }
        };
 
        private final boolean preTest_;

        /**
         * This is used to fix it so that shapes are drawn without
         * distortions.
         */ 
        private static final String[] FORCEBITMAP = { "forcebitmap=true" };

        /**
         * Constructor.
         *
         * @param  preTest  true iff it's appropriate to test example params
         *                  prior to execution
         */
        private Mode( boolean preTest ) {
            preTest_ = preTest;
        }

        /**
         * Does the work associated with this execution mode.
         *
         * @param  context  common context
         * @param  examples   example instances on which to work
         */
        public abstract void execute( Context context, Plot2Example[] examples )
                throws Exception;

        /**
         * Indicates whether it is appropriate to test example params
         * prior to execution.
         */
        public boolean requiresPreTest() {
            return preTest_;
        }

        /**
         * Writes example graphics using a supplied GraphicExporter.
         *
         * @param  context  plot execution context
         * @param  examples  examples to write
         * @param  exporter   graphic format exporter
         */
        private static void writeExamples( Context context,
                                           Plot2Example[] examples,
                                           GraphicExporter exporter,
                                           String suffix )
                throws Exception {
            String[] extraParams = exporter.isVector()
                                 ? new String[ 0 ]
                                 : FORCEBITMAP;
            File outDir = context.outDir_;
            int nex = examples.length;

            /* Write graphics files. */
            System.out.println( "Writing " + nex + " plot2 examples in "
                              + outDir + ":" );
            for ( int iex = 0; iex < examples.length; iex++ ) {
                Plot2Example ex = examples[ iex ];
                String label = ex.getLabel();
                String fname = "plot2-" + label + suffix;
                System.out.println( "   " + fname );
                OutputStream out =
                    new FileOutputStream( new File( outDir, fname ) );
                exporter.exportGraphic( ex.createPicture( extraParams ), out );
                out.close();
            }

            /* Write XML and plain text manifests. */
            String defsname = "plot2-figdefs.xml";
            String listname = "plot2-figs.lis";
            Charset utf8 = Charset.forName( "UTF-8" );
            System.out.println( "Writing " + defsname + ", " + listname );
            Writer defOut =
                new OutputStreamWriter(
                    new FileOutputStream( new File( outDir, defsname ) ),
                                          utf8 );
            Writer listOut =
                new OutputStreamWriter(
                    new FileOutputStream( new File( outDir, listname ) ),
                                          utf8 );
            for ( Plot2Example ex : examples ) {
                String label = ex.getLabel();
                String fname = "plot2-" + label + suffix;
                defOut.write( "  <!ENTITY FIG.plot2-" + label
                            + " '" + fname + "'>\n" );
                listOut.write( fname + "\n" );
            }
            defOut.close();
            listOut.close();
        }
    }

    /**
     * Defines known named tables.
     */
    private static class TName {

        /** Table rrlyrae from Gaia DR1. */
        public static final String RR;

        /** Table tgas_source table from Gaia DR1. */
        public static final String TGAS;

        /** List of Messier objects. */
        public static final String MESSIER;

        /** SDSS DR5 QSO list. */
        public static final String QSO;

        /** Query G2 from Millennium run database. */
        public static final String GAVO2;

        /**
         * VizieR table J/MNRAS/440/1571, from
         * http://vizier.u-strasbg.fr/viz-bin/votable?-source=J/MNRAS/440/1571
         */
        public static final String FORNAX;

        /** Originally obtained from Silvia Dalla. */
        public static final String ACE;

        /** starjava/source/topcat/src/etc/demo/votable/6dfgs_mini.fits. */
        public static final String MINI6;

        /**
         * Gaia DR1 query from Alcione Mora:
         *
         * select g_min_ks_index / 10 as g_min_ks,
         *        g_mag_abs_index / 10 as g_mag_abs,
         *        count(*) as n
         * from (select gaia.source_id,
         *              floor((gaia.phot_g_mean_mag+5*log10(gaia.parallax)-10)
         *                    * 10)
         *                  as g_mag_abs_index,
         *              floor((gaia.phot_g_mean_mag-tmass.ks_m) * 10)
         *                  as g_min_ks_index
         *       from gaiadr1.tgas_source as gaia
         *       inner join gaiadr1.tmass_best_neighbour as xmatch
         *               on gaia.source_id = xmatch.source_id
         *       inner join gaiadr1.tmass_original_valid as tmass
         *               on tmass.tmass_oid = xmatch.tmass_oid
         *       where gaia.parallax/gaia.parallax_error >= 5
         *         and ph_qual = 'AAA'
         *         and sqrt(power(2.5/log(10)*
         *                        gaia.phot_g_mean_flux_error
         *                        /gaia.phot_g_mean_flux,2))
         *                        <= 0.05
         *         and sqrt(power(2.5/log(10)*
         *                        gaia.phot_g_mean_flux_error
         *                        /gaia.phot_g_mean_flux,2)
         *                        + power(tmass.ks_msigcom,2)) <= 0.05
         *       ) as subquery
         * group by g_min_ks_index, g_mag_abs_index
         */
        public static final String HESS;

        /** EOP C04 from www.iers.org (I think). */
        public static final String IERS;

        /** Millennium Galaxy Catalog. */
        public static final String MGC;

        /**
         * Aggregate query from Simbad TAP service:
         *     SELECT hpx/16 AS hpx8, SUM(nbref) AS nbref, COUNT(*) AS nobj
         *     FROM public.basic
         *     WHERE hpx IS NOT NULL
         *     GROUP BY hpx8
         */
        public static final String SIMBAD_HPX;

        /** Some kind of solar data. */
        public static final String LRS;

        /**
         * Vizier table J/ApJS/166/549/table2
         * http://vizier.u-strasbg.fr/viz-bin/asu-binfits?
         *    -source=J/ApJS/166/549/table2&-out.add=_RAJ,_DEJ&-out.max=100000
         */
        public static final String NGC346;

        /** Crossmatch of NGC346 with Gaia DR1. */
        public static final String NGC346_GAIA;

        /** Planetary data obtained from Batiste Rousseau. */
        public static final String VIRVIS;

        /** All tables used for these examples. */
        public static final String[] NAMES = {
            RR = "rrlyrae.fits",
            TGAS = "tgas_source.fits",
            MESSIER = "messier.xml",
            QSO = "dr5qso.fits",
            GAVO2 = "gavo_g2.fits",
            FORNAX = "J_MNRAS_440_1571.vot",
            ACE = "ACE_data.vot",
            MINI6 = "6dfgs_mini.xml",
            HESS = "gk_hess.fits",
            IERS = "iers.fits",
            MGC = "mgc_ok.fits",
            SIMBAD_HPX = "simbad-hpx8.fits",
            LRS = "LRS_NPW_V010_20071101.cdf",
            NGC346 = "ngc346.fits",
            NGC346_GAIA = "ngc346xGaiadr1.fits",
            VIRVIS = "big_tab_VIR_VIS_CSA_public.fits",
        };
    }

    /**
     * Key-value pair representing a parameter setting.
     */
    private static class Pair {
        private final String txt_;
        private Flag flag_;
        private final String key_;
        private final String value_;

        /**
         * Constructor.
         *
         * @param  name=value part of text
         * @param  flag   param visiblity status
         * @param  key  key
         * @param  value  value
         */
        Pair( String txt, Flag flag, String key, String value ) {
            txt_ = txt;
            flag_ = flag;
            key_ = key;
            value_ = value;
        }

        /**
         * Returns an XML representation of the name=value string.
         * If flagged, it will be enclosed in a <code>strong</code> tag.
         *
         * @return XML representation
         */
        String getXml() {
            return flag_.wrapXml( XmlWriter.formatText( txt_ ) );
        }
    }

    /**
     * Indicates param visibility status.
     */
    private enum Flag {
        NORMAL() {
            String wrapXml( String etxt ) {
                return etxt;
            }
        },
        BOLD() {
            String wrapXml( String etxt ) {
                return "<strong>" + etxt + "</strong>";
            }
        },
        HIDDEN() {
            String wrapXml( String etxt ) {
                return "";
            }
        };

        /**
         * Turns text into some user-visible XML.
         *
         * @param   escapedText  text to wrap, no further XML escaping required
         * @return   XML output
         */
        abstract String wrapXml( String escapedTxt );
    }

    /**
     * Returns a list of named examples.
     * This list is used when generating user documentation.
     * The labels refer to names referenced in the user documents,
     * generally based on stilts/plot2 UI elements (plot layer types etc).
     *
     * @return  examples list for use in user documentation
     */
    private static Plot2Example[] createExamples( Context c ) {
        return new Plot2Example[] {
            new Plot2Example( "layer-mark", c, PlotTask.PLANE, new String[] {
                "*layer1=mark", "*in1=" + TName.RR,
                "*x1=p1", "*y1=peak_to_peak_g",
            } ),
            new Plot2Example( "layer-size", c, PlotTask.SKY, new String[] {
                "projection=aitoff", "xpix=500", "ypix=250", null,
                "*layer1=size", "*in1=" + TName.MESSIER,
                "*shading1=transparent",
                "*lon1=RA", "*lat1=DEC", "*size1=Radius",
            } ),
            new Plot2Example( "layer-sizexy", c, PlotTask.PLANE, new String[] {
                "*layer1=sizexy", "*in1=" + TName.QSO,
                "*shape1=filled_rectangle", null,
                "*x1=psfmag_u-psfmag_g", "*y1=psfmag_r-psfmag_z",
                "*xsize1=exp(psfmag_g)", "*ysize1=exp(psfmag_r)",
                null,
                "xmin=-3", "xmax=1", "ymin=1", "ymax=3.2",
            } ),
            new Plot2Example( "layer-xyvector", c, PlotTask.PLANE,
                              new String[] {
                "*layer1=xyvector", "*in1=" + TName.GAVO2, null,
                "*x1=x", "*y1=y", "*xdelta1=velX", "*ydelta1=velY",
                "*autoscale1=true", null,
                "xmin=9", "xmax=11", "ymin=12", "ymax=13.5",
            } ),
            new Plot2Example( "layer-xyerror", c, PlotTask.PLANE, new String[] {
                "in=" + TName.FORNAX, "x=S500", "y=S160", null,
                "layer1=mark", null,
                "*layer2=xyerror", "*xerrhi2=e_S500", "*yerrhi2=e_S160",
                "*errorbar2=capped_lines", null,
                "xlog=true", "ylog=true",
                "xmin=0.012", "xmax=1", "ymin=0.01", "ymax=10",
            } ),
            new Plot2Example( "layer-xyellipse", c, PlotTask.PLANE,
                              new String[] {
                "*in=" + TName.MGC,
                "*x=mgc_alpha_j2000", "*y=mgc_delta_j2000",
                null,
                "*ra=bulge_re/3600.", "*rb=bulge_re*bulge_e/3600.",
                "*posang=bulge_pa", null,
                "*autoscale=false", "*scale=10", "*color=blue", null,
                "*layer1=xyellipse", "*ellipse1=filled_ellipse",
                "*shading1=transparent", "*opaque1=4", null,
                "*layer2=xyellipse", "*ellipse2=crosshair_ellipse", null,
                "aspect=1", "xmin=181.3", "xmax=181.9",
            } ),
            new Plot2Example( "layer-skyellipse", c, PlotTask.SKY,
                              new String[] {
                "*in=" + TName.MGC, null,
                "*lon=mgc_alpha_j2000", "*lat=mgc_delta_j2000", null,
                "*ra=bulge_re", "*rb=bulge_re*bulge_e", "*unit=arcsec",
                "*posang=bulge_pa", null,
                "*scale=10", "*color=#cc00ff", null,
                "*layer1=skyellipse", "*ellipse1=filled_ellipse",
                "*shading1=transparent", "*opaque1=4", null,
                "*layer2=skyellipse", "*ellipse2=crosshair_ellipse", null,
                "clon=180.1", "clat=0", "radius=0.25",
            } ),
            new Plot2Example( "layer-xycorr", c, PlotTask.PLANE, new String[] {
                "*in=" + TName.TGAS,
                "*icmd='select skyDistanceDegrees(ra,dec,56.9,23.9)<0.4'",
                null,
                "*x=pmra", "*y=pmdec", null,
                "layer1=mark", null,
                "xerrhi2=pmra_error", "yerrhi2=pmdec_error", null,
                "color2=cyan", "shading2=transparent", null,
                "layer2a=xyerror",
                "errorbar2a=filled_rectangle", "opaque2a=10", null,
                "layer2b=xyerror",
                "errorbar2b=crosshair_rectangle", "opaque2b=4", null,
                "*layer3=xycorr", "*autoscale3=false", null,
                "*xerr3=pmra_error", "*yerr3=pmdec_error",
                "*xycorr3=pmra_pmdec_corr", null,
                "*ellipse3=crosshair_ellipse", null,
                "aspect=1", null,
                "xmin=17", "xmax=24", "ymin=-48", "ymax=-42",
            } ),
            new Plot2Example( "layer-skycorr", c, PlotTask.SKY, new String[] {
                "*in=" + TName.TGAS, null,
                "*lon=ra", "*lat=dec", null,
                "icmd='select ra>245.1&&ra<245.9&&dec>-17.8&&dec<-17.2'", null,
                "color=blue", null,
                "layer1=mark", null,
                "*unit=mas", "*scale=2e5", null,
                "ra2=ra_error", "rb2=dec_error", "posang2=90", null,
                "color2=orange", "shading2=transparent", null,
                "layer2a=skyellipse", "ellipse2a=filled_rectangle",
                "opaque2a=6", null,
                "layer2b=skyellipse", "ellipse2b=crosshair_rectangle",
                "opaque2b=2", null,
                "*layer3=skycorr", null,
                "*lonerr3=ra_error", "*laterr3=dec_error",
                "*corr3=ra_dec_corr", null,
                "*ellipse3=crosshair_ellipse",
            } ),
            new Plot2Example( "layer-line", c, PlotTask.TIME, new String[] {
                "*in=" + TName.ACE, "*t=epoch", null,
                "*layer1=line", "*y1=Br", "zone1=A", null,
                "*layer2=line", "*y2=Bt", "zone2=B", null,
                "*layer3=line", "*y3=Bn", "zone3=C",
            } ),
            new Plot2Example( "layer-line3d", c, PlotTask.CUBE, new String[] {
                "*in=" + TName.IERS, "*x=x", "*y=y", "*z=LOD", null,
                "*layer1=line3d",
                "*icmd1='select decYear>1963&&decYear<1964.5'",
                "*thick1=3", "*aux1=LOD", null,
                "layer2=mark", "shading2=translucent", "color2=cccc00",
                "translevel2=0.35", null,
                "auxmap=cyan-magenta", "auxvisible=false", "legend=false", null,
                "phi=-150", "theta=25", "psi=180",
                "xpix=400", "ypix=400",
            } ),
            new Plot2Example( "layer-linearfit", c, PlotTask.PLANE,
                              new String[] {
                "*in=" + TName.MINI6, "*x=RMAG", "*y=BMAG",
                "layer1=mark", "*layer2=linearfit",
            } ),
            new Plot2Example( "layer-label", c, PlotTask.SKY, new String[] {
                "*in=" + TName.MESSIER, "*lon=RA", "*lat=DEC",
                "layer1=mark", "*layer2=label", "*label2=NAME",
            } ),
            new Plot2Example( "layer-contour", c, PlotTask.PLANE, new String[] {
                "*in=" + TName.TGAS,
                "*x=phot_g_mean_mag", "*y=phot_g_mean_flux_error", null,
                "ylog=true", "xmax=14", "ymin=10", null,
                "layer1=mark", "shading1=density", "densemap1=greyscale", null,
                "*layer2=contour", "*scaling2=log", "*nlevel=6",
            } ),
            new Plot2Example( "layer-grid", c, PlotTask.PLANE, new String[] {
                "*layer1=grid", "*in1=" + TName.HESS,
                "*x1=g_min_ks", "*y1=g_mag_abs", null,
                "*weight1=n", "*combine1=sum",
                "*xbinsize1=0.2", "*ybinsize1=0.2",
                "*xphase1=0.5", "*yphase1=0.5", null,
                "yflip=true", "auxfunc=log", "auxmap=viridis",
            } ),
            new Plot2Example( "layer-fill", c, PlotTask.TIME, new String[] {
                "*layer1=fill", "*in1=" + TName.IERS,
                "*t1=decYear", "*y1=lodErr", "ylog=true",
            } ),
            new Plot2Example( "layer-quantile", c, PlotTask.PLANE,
                              new String[] {
                "*in=" + TName.TGAS,
                "*x=phot_g_mean_mag", "*y=phot_g_mean_flux_error", null,
                "ylog=true", "xmax=15", "ymin=10", null,
                "layer.d=mark", "color.d=99ff99", null,
                "*layer.q4=quantile", "*quantiles.q4=0.25,0.75",
                "*color.q4=magenta", "*transparency.q4=0.35", null,
                "*layer.q2=quantile", "*quantiles.q2=0.5", "*color.q2=SkyBlue",
                "*thick.q2=4", null,
                "*smooth.q=0.05", null,
                "leglabel.q4=Quartiles", "leglabel.q2=Median", "legseq=.q4,.q2",
                "legpos=0.95,0.95",
            } ),
            new Plot2Example( "layer-skyvector", c, PlotTask.SKY, new String[] {
                "*in=" + TName.TGAS, "*lon=ra", "*lat=dec", null,
                "layer1=mark", null,
                "*layer2=skyvector", null,
                "*dlon2=pmra", "*dlat2=pmdec", "*unit2=scaled",
                "*arrow2=medium_arrow", null,
                "clon=56.75", "clat=24.10", "radius=1.5",
            } ),
            new Plot2Example( "layer-histogram", c, PlotTask.PLANE,
                              new String[] {
                "*layer1=histogram", "*in1=" + TName.RR, "*x1=p1",
            } ),
            new Plot2Example( "layer-kde", c, PlotTask.PLANE, new String[] {
                "ymin=0", "*layer1=kde", "*in1=" + TName.RR, "*x1=p1",
            } ),
            new Plot2Example( "layer-knn", c, PlotTask.PLANE, new String[] {
                "*layer1=knn", "*in1=" + TName.RR, "*x1=p1",
            } ),
            new Plot2Example( "layer-densogram", c, PlotTask.PLANE,
                              new String[] {
                "*in=" + TName.TGAS, "*x=hypot(pmra_error,pmdec_error)", null,
                "xlog=true", "*normalise=maximum", null,
                "color=grey", "layer1=histogram", "layer2=kde", null,
                "*layer3=densogram", "*densemap3=skyblue-yellow-hotpink",
                "*densefunc3=log", null,
                "*size3=50", "*pos3=0.5",
            } ),
            new Plot2Example( "layer-gaussian", c, PlotTask.PLANE,
                              new String[] {
                "*in=" + TName.MGC, "*x=mgc_dc_sb", null,
                "layer1=histogram", "color1=green", null,
                "*layer2=gaussian", "*color2=grey", "*thick2=3", null,
                "ymax=1200",
            } ),
            new Plot2Example( "layer-skydensity", c, PlotTask.SKY,
                              new String[] { 
                "*in=" + TName.TGAS, "*lon=l", "*lat=b", null,
                "*layer1=skydensity", "*weight1=parallax",
                "*combine1=mean", "*level1=4", null,
                "projection=aitoff", "auxmap=PuRd", "auxfunc=histogram", null,
                "xpix=540", "ypix=250",
            } ),
            new Plot2Example( "layer-healpix", c, PlotTask.SKY, new String[] {
                "*layer1=healpix", "*in1=" + TName.SIMBAD_HPX,
                "*healpix1=HPX8", "*value1=NBREF", null,
                "*datalevel1=8", "*degrade1=2",
                "*combine=sum-per-unit", "*perunit=arcmin2", null,
                "projection=aitoff", "*datasys1=equatorial", "viewsys=galactic",
                "labelpos=none",
                null,
                "auxfunc=log", "auxmap=cold", "auxflip=true",
                "auxclip=0,1", null,
                "xpix=600", "ypix=280",
            } ),
            new Plot2Example( "layer-xyzvector", c, PlotTask.CUBE,
                              new String[] {
                "*in=" + TName.GAVO2, null,
                "*x=x", "*y=y", "*z=z",
                "*xdelta=velX", "*ydelta=velY", "*zdelta=velZ",
                "*autoscale=true", null,
                "*color=BlueViolet", "*scale=1.5", null,
                "*layer1=xyzvector", "*shading1=transparent", "*opaque1=5",
                "*arrow1=medium_filled_dart", null,
                "*layer2=xyzvector", "*shading2=flat",
                "*arrow2=medium_open_dart", null,
                "xmin=6", "xmax=7.5", "ymin=12.5", "ymax=13.5",
                "zmin=19", "zmax=21.5",
            } ),
            new Plot2Example( "layer-xyzerror", c, PlotTask.CUBE, new String[] {
                "*in=" + TName.QSO, "icmd='select morphology==1'", null,
                "*x=psfmag_g", "*xerrhi=psfmagerr_g", null,
                "*y=psfmag_r", "*yerrhi=psfmagerr_r", null,
                "*z=psfmag_u", "*zerrhi=psfmagerr_u", null,
                "layer1=mark", null,
                "*layer2=xyzerror", "*errorbar2=cuboid", null,
                "shading=transparent", "opaque=3", null,
                "xmin=17.5", "xmax=18",
                "ymin=17.3", "ymax=17.7",
                "zmin=17.4", "zmax=18.2",
            } ),
            new Plot2Example( "layer-spectrogram", c, PlotTask.TIME,
                              new String[] {
                "*layer1=spectrogram", "*in1=" + TName.LRS,
                "*t1=epoch", "*spectrum1=RX2", null,
                "auxfunc=linear", "auxmap=plasma", "auxclip=0,1", null,
                "xpix=600", "ypix=300", null,
                "tmin=2007-11-01T00", "tmax=2007-11-01T12", "ymax=500",
            } ),
            new Plot2Example( "layer-yerror", c, PlotTask.TIME, new String[] {
                "*in=" + TName.ACE, "*t=epoch", "*y=Bmag", null,
                "*layer1=yerror", "*yerrhi1=sigma_B", "*errorbar1=capped_lines",
                null,
                "layer2=mark", "shape2=open_circle", "size2=3", null,
                "layer3=line", "color3=a0a0a0", null,
                "tmin=2001-08-17T07", "tmax=2001-08-17T10", "ypix=250",
            } ),
            new Plot2Example( "layer-mark2", c, PlotTask.SKY, new String[] {
                "clon=14.78", "clat=-72.1525", "radius=0.0015",
                "sex=false", null,
                "layer_h=mark", "in_h=" + TName.NGC346,
                "lon_h=_RAJ2000", "lat_h=_DEJ2000", "color_h=red", null,
                "layer_g=mark", "in_g=" + TName.NGC346_GAIA,
                "lon_g=ra", "lat_g=dec",
                "color_g=blue", "shading_g=flat", "size_g=3", null,
                "*in_x=" + TName.NGC346_GAIA,
                "*lon1_x=_RAJ2000", "*lat1_x=_DEJ2000",
                "*lon2_x=ra", "*lat2_x=dec",
                "*shading_x=flat", null,
                "layer_xl=link2", "color_xl=greenyellow", null,
                "*layer_xm=mark2", "*color_xm=forestgreen",
                "*size_xm=4", "*shape_xm=open_circle", null,
                "seq=_xm,_xl,_h,_g",
                "leglabel_h=HST", "leglabel_g='Gaia DR1'",
                "legseq=_h,_g", "legpos=0.95,0.95",
            } ),
            new Plot2Example( "layer-link2", c, PlotTask.SKY, new String[] {
                "clon=14.78", "clat=-72.1525", "radius=0.0015",
                "sex=false", null,
                "layer_h=mark", "in_h=" + TName.NGC346,
                "lon_h=_RAJ2000", "lat_h=_DEJ2000", "color_h=red", null,
                "layer_g=mark", "in_g=" + TName.NGC346_GAIA,
                "lon_g=ra", "lat_g=dec",
                "color_g=blue", "shading_g=flat", "size_g=3", null,
                "*in_x=" + TName.NGC346_GAIA,
                "*lon1_x=_RAJ2000", "*lat1_x=_DEJ2000",
                "*lon2_x=ra", "*lat2_x=dec",
                "*shading_x=flat", null,
                "*layer_xl=link2", "*color_xl=forestgreen", null,
                "layer_xm=mark2", "color_xm=greenyellow",
                "size_xm=4", "shape_xm=open_circle", null,
                "seq=_xm,_xl,_h,_g",
                "leglabel_h=HST", "leglabel_g='Gaia DR1'",
                "legseq=_h,_g", "legpos=0.95,0.95",
            } ),
            new Plot2Example( "layer-poly4", c, PlotTask.SKY, new String[] {
                "in=" + TName.VIRVIS, "icmd='every 32'",
                null,
                "*lon1=LON_CORNER_1", "*lat1=LAT_CORNER_1", null,
                "*lon2=LON_CORNER_2", "*lat2=LAT_CORNER_2", null,
                "*lon3=LON_CORNER_3", "*lat3=LAT_CORNER_3", null,
                "*lon4=LON_CORNER_4", "*lat4=LAT_CORNER_4", null,
                "*aux=RADIUS", null,
                "*layer_o=poly4", "*polymode_o=outline", "*shading_o=aux",
                null,
                "*layer_f=poly4", "*polymode_f=fill", "*shading_f=aux",
                "*opaque_f=4", null,
                "auxmap=rainbow", "auxvisible=false",
                "xpix=300", "ypix=300", "labelpos=none",
            } ),
            new Plot2Example( "layer-mark4", c, PlotTask.PLANE, new String[] {
                "in=" + TName.VIRVIS, null,
                "icmd='select IOF_055<0.005'", null,
                "icmd='select lon_center>250&&lon_center<300&&"
                           + "lat_center>-65&&lat_center<-16'", null,
                "*x1=LON_CORNER_1", "*y1=LAT_CORNER_1", null,
                "*x2=LON_CORNER_2", "*y2=LAT_CORNER_2", null,
                "*x3=LON_CORNER_3", "*y3=LAT_CORNER_3", null,
                "*x4=LON_CORNER_4", "*y4=LAT_CORNER_4", null,
                "layer_q=poly4", "polymode_q=fill",
                "shading_q=transparent", "opaque_q=4", null,
                "*layer_m=mark4", "*color_m=404040", "*shape_m=open_circle",
                "*size_m=3",
            } ),
            new Plot2Example( "layer-polygon", c, PlotTask.SKY, new String[] {
                "in=" + TName.VIRVIS, null,
                "icmd='select ALTITUDE>4e4&&ALTITUDE<4.3e4'", null,
                "*layer=polygon", "*polymode=fill", null,
                "*lon=LON_CENTER", "*lat=LAT_CENTER", null,
                "*otherpoints=array(lon_corner_1,lat_corner_1,"
                                 + "lon_corner_2,lat_corner_2)", null,
                "shading=weighted", "weight=IR_TEMPERATURE",
                "auxmap=plasma", null,
                "clon=83", "clat=34", "radius=11",
            } ),
            new Plot2Example( "layer-function", c, PlotTask.PLANE,
                              new String[] {
                "*layer1=function", "*fexpr1=sin(x)/x", "*thick1=3", null,
                "xmin=0", "xmax=30", "ymin=-0.25", "ymax=0.25",
            } ),
            new Plot2Example( "layer-skygrid", c, PlotTask.SKY,
                              new String[] {
                "xpix=500", "ypix=250", "projection=aitoff", null,
                "*viewsys=ecliptic", null,
                "*layer1=skygrid", "*gridsys1=galactic",
                "*gridcolor1=HotPink", "*labelpos1=none",
            } ),
            new Plot2Example( "layer-spheregrid", c, PlotTask.SPHERE,
                              new String[] {
                "legend=false", "xpix=350", "ypix=350", null,
                "layer1=mark", "in1=" + TName.TGAS,
                "lon1=ra", "lat1=dec", "r1=1", null,
                "shading1=transparent", "opaque1=850", "color1=orange", null,
                "*layer2=spheregrid", "*gridcolor2=green", "*thick2=2",
            } ),
            createShadingExample( "flat", c, new String[ 0 ] ),
            createShadingExample( "auto", c, new String[ 0 ] ),
            createShadingExample( "transparent", c, new String[ 0 ] ),
            createShadingExample( "translucent", c, new String[ 0 ] ),
            createShadingExample( "density", c, new String[] {
                "*densemap1=viridis",
            } ),
            createShadingExample( "aux", c, new String[] {
                "*aux1=z", "*auxmap=plasma",
            } ),
            createShadingExample( "weighted", c, new String[] {
                "*weight1=z", "*auxmap=plasma",
            } ),
        };
    }

    /**
     * Creates an example plot to display shading mode.
     *
     * @param  shading  name of existing shading mode
     * @param  context  plot execution context
     * @param  extraParams   parameters specific to this shading mode
     * @return   new example
     */
    private static Plot2Example createShadingExample( String shading,
                                                      Context context,
                                                      String[] extraParams ) {
        List<String> paramList = new ArrayList<String>();
        paramList.addAll( Arrays.asList( new String[] {
            "layer1=mark", "in1=" + TName.QSO, null,
            "*shading1=" + shading,
        } ) );
        paramList.addAll( Arrays.asList( extraParams ) );
        paramList.addAll( Arrays.asList( new String[] {
            null,
            "x1=psfmag_g-psfmag_r", "y1=psfmag_u-psfmag_g", "size1=2", null,
            "xmin=-0.5", "xmax=2.5", "ymin=-1", "ymax=6",
        } ) );
        return new Plot2Example( "shading-" + shading, context, PlotTask.PLANE,
                                 paramList.toArray( new String[ 0 ] ) );
    }

    /**
     * Main method.  This is intended for invocation from the stilts/topcat
     * build systems.
     *
     * <p>Use the <code>-help</code> flag for usage information.
     */
    public static void main( String[] args ) throws Exception {

        /* Assemble usage string. */
        StringBuffer mbuf = new StringBuffer();
        for ( Mode mode : Mode.values() ) {
            mbuf.append( mbuf.length() > 0 ? "|" : "" )
                .append( mode );
        }
        String usage = new StringBuffer()
            .append( Plot2Example.class.getSimpleName() )
            .append( " [-mode " + mbuf + "]" )
            .append( " [-dataDir <dir>]" )
            .append( " [-outDir <dir>]" )
            .append( " [-dataUrl <url>]" )
            .append( " [-help]" )
            .append( " [label ...]" )
            .toString();

        /* Parse argument list. */
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        String dataDir = ".";
        String outDir = ".";
        String dataUrl = null;
        Mode mode = Mode.swing;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( "-mode".equalsIgnoreCase( arg ) ) {
                it.remove();
                String modeName = it.next();
                it.remove();
                mode = Mode.valueOf( modeName );
            }
            else if ( "-dataDir".equalsIgnoreCase( arg ) ) {
                it.remove();
                dataDir = it.next();
                it.remove();
            }
            else if ( "-outDir".equalsIgnoreCase( arg ) ) {
                it.remove();
                outDir = it.next();
                it.remove();
            }
            else if ( "-dataUrl".equalsIgnoreCase( arg ) ) {
                it.remove();
                dataUrl = it.next();
                it.remove();
            }
            else if ( arg.toLowerCase().startsWith( "-h" ) ) {
                System.err.println( usage );
                return;
            }
        }

        /* Set up list of known examples and execution context. */
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.SEVERE );
        Context context =
            new Context( new File( dataDir ), TName.NAMES,
                         new File( outDir ), toContextUrl( dataUrl ) );
        Map<String,Plot2Example> exampleMap =
            new LinkedHashMap<String,Plot2Example>();
        for ( Plot2Example ex : createExamples( context ) ) {
            if ( exampleMap.put( ex.label_, ex ) != null ) {
                throw new RuntimeException( "duplicate example label \"" 
                                          + ex.label_ + "\"" );
            }
        }

        /* Determine (possibly from command line) which example labels are
         * going to be used for this invocation. */
        String[] labels = argList.size() > 0
                        ? argList.toArray( new String[ 0 ] )
                        : exampleMap.keySet().toArray( new String[ 0 ] );

        /* Create a list of example object and required data tables. */
        List<Plot2Example> exampleList = new ArrayList<Plot2Example>();
        Set<String> tableNames = new TreeSet<String>();
        for ( String label : labels ) {
            Plot2Example ex = exampleMap.get( label );
            if ( ex != null ) {
                tableNames.addAll( Arrays
                                  .asList( ex.getRequiredTableNames() ) );
                exampleList.add( ex );
            }
            else {
                System.err.println( "no such example: " + label );
            }
        }
        Plot2Example[] examples = exampleList.toArray( new Plot2Example[ 0 ] );

        /* Perform pre-execution tests if appropriate; this can show up
         * problems earlier rather than later. */
        if ( mode.requiresPreTest() ) {
            context.checkHasTables( tableNames.toArray( new String[ 0 ] ) );
            for ( Plot2Example ex : examples ) {
                ex.testParams();
            }
        }

        /* Perform the mode-specific execution. */
        mode.execute( context, examples );
    }
}
