/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-SEP-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.SplatException;

/**
 * Load a list of spectra into the {@link SplatBrowser}, or save a spectrum
 * using a thread to avoid blocking of the UI. A single instance of this class
 * exists so it is only possible to load or save one set of files at a time.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpectrumIO
{
    //  XXX deficiencies: cannot stop during load of a spectrum. This would
    //  require a lot of work catching the problems with interrupting
    //  incomplete objects. The ProgressMonitor is hacked to display after a
    //  certain time, not just an amount of loading, this could be done better.

    /**
     * Private constructor so that only one instance can exist.
     */
    private SpectrumIO()
    {
        //  Do nothing.
    }

    /**
     * Instance of this class.
     */
    private static SpectrumIO instance = null;

    /**
     * Return the reference to the single instance of SpectrumIO.
     */
    public static SpectrumIO getInstance()
    {
        if ( instance == null ) {
            instance = new SpectrumIO();
        }
        return instance;
    }

    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * The list of spectra to load. These are characterised as Props
     * instances.
     */
    private Vector queue = new Vector();

    /**
     * The Thread that the loading or saving is actually performed in.
     */
    private Thread loadThread = null;

    /**
     * The ProgressMonitor.
     */
    private int progressValue;
    private int progressMaximum;
    private ProgressMonitor progressMonitor = null;

    /**
     * The SplatBrowser to notify about any spectra that are loaded.
     */
    private SplatBrowser browser = null;

    /**
     * Whether the spectra should also be displayed.
     */
    private boolean display = true;

    /**
     * Last spectrum.
     */
    private Props lastSpectrum = null;

    /**
     * Step to push ProgressMonitor into displaying more often (there's a need
     * to shift by 10% before it even thinks about displaying, which means
     * single spectra never get a ProgressMonitor).
     */
    private static int USTEP = 200;

    /**
     * Load an array of spectra whose specifications are contained in the
     * elements of an array of Strings. If the loading process takes a "long"
     * time then a ProgressMonitor dialog with be displayed, which can also be
     * cancelled (which causes the next spectrum not to be loaded, the
     * currently loaded spectrum will complete). This version uses a single
     * defined type for all spectra (set to {@link SpecDataFactory.DEFAULT}
     * for the usual file extensions rules).
     */
    public void load( SplatBrowser browser, String[] spectra,
                      boolean display, int usertype )
    {
        setSpectra( spectra, usertype );
        this.browser = browser;
        this.display = display;
        loadSpectra();
    }

    /**
     * Load an array of spectra whose specifications are contained in the
     * elements of an array of Strings. If the loading process takes a "long"
     * time then a ProgressMonitor dialog with be displayed, which can also be
     * cancelled (which causes the next spectrum not to be loaded, the
     * currently loaded spectrum will complete). This method uses a different
     * type for each input spectrum, so the usertypes array is required to be
     * the same size as the spectra array.
     */
    public void load( SplatBrowser browser, String[] spectra,
                      boolean display, int[] usertypes, String[] shortNames )
    {
        setSpectra( spectra, usertypes, shortNames );
        this.browser = browser;
        this.display = display;
        loadSpectra();
    }

    /**
     * Load an array of spectra whose specifications are contained in the
     * elements of the props array. If the loading process takes a "long"
     * time then a ProgressMonitor dialog with be displayed, which can also be
     * cancelled (which causes the next spectrum not to be loaded, the
     * currently loaded spectrum will complete).
     */
    public void load( SplatBrowser browser, boolean display, Props[] props )
    {
        queue.clear();
        for ( int i = 0; i < props.length; i++ ) {
            queue.add( props[i] );
        }
        this.browser = browser;
        this.display = display;
        loadSpectra();
    }

    /**
     * Set the spectra to load, all have the same data type.
     */
    protected synchronized void setSpectra( String[] spectra, int type )
    {
        queue.clear();
        if ( spectra != null ) {
            for ( int i = 0; i < spectra.length; i++ ) {
                queue.add( new Props( spectra[i], type, null ) );
            }
        }
    }

    /**
     * Set the spectra to load and, if given (can be null) the individual
     * types.
     */
    protected synchronized void setSpectra( String[] spectra, int[] types )
    {
        queue.clear();
        if ( spectra != null ) {
            for ( int i = 0; i < spectra.length; i++ ) {
                queue.add( new Props( spectra[i], types[i], null ) );
            }
        }
    }

    /**
     * Set the spectra to load and, if given (can be null) the individual
     * types and some short names.
     */
    protected synchronized void setSpectra( String[] spectra, int[] types,
                                            String[] shortNames )
    {
        queue.clear();
        if ( spectra != null ) {
            for ( int i = 0; i < spectra.length; i++ ) {
                queue.add( new Props( spectra[i], types[i], shortNames[i] ) );
            }
        }
    }


    /**
     * Get the next spectrum to load. Returns null when none left.
     */
    protected synchronized Props getSpectrum()
    {
        try {
            lastSpectrum = (Props) queue.remove( 0 );
        }
        catch (ArrayIndexOutOfBoundsException e) {
            lastSpectrum = null;
        }
        return lastSpectrum;
    }

    /**
     * Load all the spectra that are waiting to be processed.
     * Use a new Thread so that we do not block the GUI or event threads.
     */
    protected void loadSpectra()
    {
        if ( ! queue.isEmpty() ) {
            initProgressMonitor( queue.size(), "Loading spectra..." );
            waitTimer = new Timer ( 2000, new ActionListener()
                {
                    private int soFar = 0;
                    public void actionPerformed( ActionEvent e )
                    {
                        progressMonitor.setProgress( filesDone*USTEP + soFar );
                        soFar++;
                        if ( soFar >= USTEP ) soFar = 0;
                        progressMonitor.setNote( lastSpectrum.getSpectrum() );

                        //  Stop loading spectra if asked.
                        if (progressMonitor.isCanceled() ) {
                            waitTimer.stop();

                            //  If interrupted do not display already loaded
                            //  spectra and clear the queue.
                            if ( progressMonitor.isCanceled() ) {
                                display = false;
                                queue.clear();
                            }
                            //spectraLoadThread.interrupt();
                        }
                    }
                });

            //  Block the browser window and start the timer that updates the
            //  ProgressMonitor.
            browser.setWaitCursor();
            waitTimer.start();

            //  Now create the thread that reads the spectra.
            loadThread = new Thread( "Spectra loader" ) {
                    public void run() {
                        try {
                            addSpectra();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        finally {
                            //  Always tidy up and rewaken interface when
                            //  complete (including if an error is thrown).
                            browser.resetWaitCursor();
                            waitTimer.stop();
                            closeProgressMonitor();
                        }
                    }
                };

            //  Start loading spectra.
            loadThread.start();
        }
    }

    /**
     * Timer for used for event queue actions.
     */
    private Timer waitTimer;

    /**
     * Number of files which we have loaded.
     */
    private int filesDone = 0;

    /**
     * Load all the currently queued spectra into the global list of spectra
     * and inform the associated SplatBrowser to display.
     * given attempt to open the files using the type provided by the
     * user (in the open file dialog).
     */
    protected void addSpectra()
    {
        if ( queue.isEmpty() ) return;

        int validFiles = 0;
        int failedFiles = 0;
        int initialsize = queue.size();
        filesDone = 0;
        StringBuffer failures = null;
        SplatException lastException = null;

        // Add all spectra to the browser until the queue is empty.
        while( ! queue.isEmpty() ) {
            try {
                browser.tryAddSpectrum( getSpectrum() );
                validFiles++;
            }
            catch (SplatException e) {
                if ( failures == null ) {
                    failures = new StringBuffer();
                }
                failures.append( e.getMessage() + "\n" );
                failedFiles++;
                lastException = e;
            }
            filesDone++;
        }

        //  Report any failures. If there is just one make usual report.
        if ( failures != null ) {
            String message = null;
            if ( failedFiles == 1 ) {
                message = lastException.getMessage();
            }
            else {
                message = "Failed to open " + failedFiles + " spectra";
                lastException = new SplatException( failures.toString() );
            }
            new ExceptionDialog( browser, message, lastException );
        }

        //  And now display them if we can.
        if ( display && validFiles > 0 ) {
            int count = globalList.specCount();
            browser.displayRange( count - initialsize, count -1 );
        }
    }

    /**
     * Save a given spectrum as a file. Use a thread so that we do not
     * block the GUI or event threads.
     *
     * @param globalIndex global list index of the spectrum to save.
     * @param target the file to write the spectrum into.
     */
    public void save( SplatBrowser browser, int globalIndex, String target )
    {
        final int localGlobalIndex = globalIndex;
        final String localTarget = target;
        final SplatBrowser localBrowser = browser;

        //  Monitor progress by checking the filesDone variable.
        initProgressMonitor( 1, "Saving spectrum..." );
        progressMonitor.setNote( "as " + localTarget );
        waitTimer = new Timer ( 2000, new ActionListener()
            {
                int soFar = 0;
                public void actionPerformed( ActionEvent e )
                {
                    progressMonitor.setProgress( soFar );
                    if ( soFar > USTEP ) soFar = 0;
                }
            });
        browser.setWaitCursor();
        waitTimer.start();

        //  Now create the thread that saves the spectrum.
        Thread saveThread = new Thread( "Spectrum saver" ) {
                public void run() {
                    try {
                        localBrowser.saveSpectrum( localGlobalIndex,
                                                   localTarget );
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        //  Always tidy up and rewaken interface when
                        //  complete (including if an error is thrown).
                        localBrowser.resetWaitCursor();
                        waitTimer.stop();
                        closeProgressMonitor();
                    }
                }
            };
        //  Start saving spectrum.
        saveThread.start();
    }

    /**
     * Initialise the startup progress monitor.
     *
     * @param intervals the number of intervals (i.e. calls to
     *                   updateProgressMonitor) expected before action
     *                   is complete.
     * @param title the title for the monitor window.
     * @see #updateProgressMonitor
     */
    protected void initProgressMonitor( int intervals, String title )
    {
        closeProgressMonitor();
        progressValue = 0;
        progressMaximum = intervals * USTEP;
        progressMonitor = new ProgressMonitor( browser, title, "", 0,
                                               progressMaximum );
        progressMonitor.setMillisToDecideToPopup( 2000 );
        progressMonitor.setMillisToPopup( 2000 );
    }

    /**
     *  Update the progress monitor.
     *
     *  @param note note to show in the progress monitor dialog.
     *
     *  @see #initProgressMonitor
     */
    protected void updateProgressMonitor( String note )
    {
        progressMonitor.setProgress( ++progressValue );
        progressMonitor.setNote( note );
    }

    /**
     *  Close the progress monitor.
     *
     *  @see #initProgressMonitor
     */
    protected void closeProgressMonitor()
    {
        if ( progressMonitor != null ) {
            progressMonitor.close();
            progressMonitor = null;
        }
    }

    /**
     * Container class for describing the properties of a spectrum to be
     * loaded. The only required value is the spectrum specification (name,
     * URL etc.). The others cover a pre-defined type, a shortname, the data
     * and spectral coordinate units (ideally as strings understood by AST)
     * and the columns to be used for data and coordinates, if the spectrum 
     * is a table.
     */
    public static class Props
    {
        protected String spectrum;
        protected int type;
        protected String shortName;
        protected String dataUnits;
        protected String coordUnits;
        protected String dataColumn;
        protected String coordColumn;

        public Props( String spectrum )
        {
            this( spectrum, SpecDataFactory.DEFAULT, null, null, null, 
                  null, null ); 
        }

        public Props( String spectrum, int type, String shortName )
        {
            this( spectrum, type, shortName, null, null, null, null );
        }

        public Props( String spectrum, int type, String shortName,
                          String dataUnits, String coordUnits, 
                          String dataColumn, String coordColumn )
        {
            this.spectrum = spectrum;
            this.type = type;
            this.shortName = shortName;
            this.dataUnits = dataUnits;
            this.coordUnits = coordUnits;
            this.dataColumn = dataColumn;
            this.coordColumn = coordColumn;
        }

        public String getSpectrum()
        {
            return spectrum;
        }

        public void setSpectrum( String spectrum )
        {
            this.spectrum = spectrum;
        }

        public int getType()
        {
            return type;
        }

        public void setType( int type )
        {
            this.type = type;
        }


        public String getShortName()
        {
            return shortName;
        }

        public void setShortName( String shortName )
        {
            this.shortName = shortName;
        }

        public String getDataUnits()
        {
            return dataUnits;
        }

        public void setDataUnits( String dataUnits )
        {
            this.dataUnits = dataUnits;
        }

        public String getCoordUnits()
        {
            return coordUnits;
        }

        public void setCoordUnits( String coordUnits )
        {
            this.coordUnits = coordUnits ;
        }

        public String getDataColumn()
        {
            return dataColumn;
        }

        public void setDataColumn( String dataColumn )
        {
            this.dataColumn = dataColumn;
        }

        public String getCoordColumn()
        {
            return coordColumn;
        }

        public void setCoordColumn( String coordColumn )
        {
            this.coordColumn = coordColumn;
        }

        /**
         * Apply the tranmutable properties of this object to a SpecData
         * instance. This does not include the spectrum specification, or data
         * type, these must be used to open the spectrum.
         */
        public void apply( SpecData spectrum )
        {
            if ( shortName != null && shortName.length() != 0 ) {
                spectrum.setShortName( shortName );
            }
            if ( dataUnits != null && dataUnits.length() != 0 ) {
                spectrum.setDataUnits( dataUnits );
            }
            if ( coordUnits != null && coordUnits.length() != 0 ) {
                //  Bit tricky, set the units of the FrameSet used for drawing
                //  and the underlying frame produced from the data. Still
                //  might be lost if the spectrum is reloaded from disk file.
                FrameSet frameSet = spectrum.getFrameSet();
                frameSet.setUnit( 1, coordUnits );
                frameSet = spectrum.getAst().getRef();
                frameSet.setUnit( 1, coordUnits );
            }
            if ( dataColumn != null && dataColumn.length() != 0 ) {
                try {
                    spectrum.setYDataColumnName( dataColumn );
                }
                catch (SplatException e) {
                    e.printStackTrace();
                }
            }
            if ( coordColumn != null && coordColumn.length() != 0 ) {
                try {
                    spectrum.setXDataColumnName( coordColumn );
                }
                catch (SplatException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
