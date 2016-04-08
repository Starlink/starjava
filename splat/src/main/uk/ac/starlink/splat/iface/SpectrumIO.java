/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2008-2009 Science and Technology Facilities Council
 *
 *  History:
 *     13-SEP-2004 (Peter W. Draper):
 *       Original version.
 *     2012 (Margarida Castro Neves)
 * 	added getData support
 *     2013
 *      added DataLink support 
 *     2015
 *	removed getData	support
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Vector;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.ObjectTypeEnum;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Load a list of spectra into the {@link SplatBrowser}, or save a spectrum
 * using a thread to avoid blocking of the UI. A single instance of this class
 * exists so it is only possible to load or save one set of files at a time.
 * <p>
 * Since the NDF library also caches AST references (to the WCS FrameSet) a
 * further constraint is that loading and saving must happen in the same
 * thread (other operations that touch the cached WCS should also happen in
 * this thread), so all spectra are dealt with using a single thread.
 * <p>
 * To avoid this problem it would be necessary to free the NDF cache
 * AST reference directly. Exported AST references are unlocked so using
 * those in other threads should be OK.
 * <p>
 * When loading spectra you can add further spectra which will join the queue
 * of spectra to load. When a list of spectra takes a long time to load a
 * progress dialog window will be shown displaying the number of spectra
 * remaining to load and the currently loading spectrum. Cancelling this
 * window clears the queue of spectra and further loading, after the current
 * spectrum completes, will not be done.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpectrumIO
{
    //  XXX deficiencies: cannot stop during load of a spectrum. This would
    //  require a lot of work catching the problems with interrupting
    //  incomplete objects.

    /**
     * Source types, from which the spectra may come from
     */
    // TODO: implement the source identification to remaining spectra's inputs 
    public static enum SourceType {
        UNDEFINED,
        SAMP,
        SSAP,
        LOCAL
    };
    
    /**
     * Private constructor so that only one instance can exist.
     */
    private SpectrumIO()
    {
        loadThread = new Loader();
        loadThread.start();
    }

    /**
     * Instance of this class.
     */
    private static SpectrumIO instance = null;

    /**
     * Return the reference to the single instance of SpectrumIO.
     */
    public static synchronized SpectrumIO getInstance()
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
    private Loader loadThread = null;

    /**
     * The ProgressFrame. This appears to denote that something is happening.
     */
    private ProgressFrame progressFrame =
        new ProgressFrame( "Loading spectra..." );

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
     * Index of saved file.
     */
    private final int globalIndex = 0;

    /**
     * Object to receive callbacks whenever the queue of files to load
     * is cleared.
     */
    private Watch watcher = null;

    /**
     * Load an array of spectra whose specifications are contained in the
     * elements of an array of Strings. This version uses a single
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
     * elements of an array of Strings. This method uses a different
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
     * elements of the props array.
     */
    public void load( SplatBrowser browser, boolean display, Props[] props )
    {
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
        if ( spectra != null ) {
            for ( int i = 0; i < spectra.length; i++ ) {
                queue.add( new Props( spectra[i], types[i], shortNames[i] ) );
            }
        }
    }


    /**
     * Get properties of the next spectrum to load. Returns null when none
     * left.
     */
    protected synchronized Props getSpectrumProps()
    {
        try {
            return (Props) queue.remove( 0 );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Load all the spectra that are waiting to be processed.
     */
    protected void loadSpectra()
    {
        if ( ! queue.isEmpty() ) {
            progressFrame.setTitle( "Loading "+ queue.size() +" spectra..." );
            progressFrame.start();

            //  Block the browser window during the load.
            browser.setWaitCursor();

            //  Set up thread to start reading the spectra.
            loadThread.setAdd( true );
        }
    }

    /**
     * Number of files which we have loaded.
     */
    private int filesDone = 0;

    /**
     * Load all the currently queued spectra into the global list of spectra
     * and inform the associated SplatBrowser to display. Note synchronized
     * as the thread is permanently running.
     */
    protected synchronized void addSpectra()
    {
        if ( queue.isEmpty() ) return;

        int validFiles = 0;
        int failedFiles = 0;
        int initialsize = globalList.specCount();
        filesDone = 0;
        StringBuffer failures = null;
        Exception lastException = null;

        // Add all spectra to the browser until the queue is empty or
        // the load is interrupted.
        while( ! queue.isEmpty() && ! progressFrame.isInterrupted() ) {
            progressFrame.setTitle( "Loading "+ queue.size() +" spectra..." );
            Props props = getSpectrumProps();
            progressFrame.setMessage( props.getSpectrum() );
            boolean success = false;
            try {
                browser.tryAddSpectrum( props );
                validFiles++;
                success = true;
            }
            catch (Exception e) {
                //  Catch all exceptions, we need to make sure that the
                //  watcher notification proceeds regardless.
                e.printStackTrace();
                lastException = e;
                if ( failures == null ) {
                    failures = new StringBuffer();
                }
                failures.append( e.getMessage() + "\n" );
                failedFiles++;
            }
            finally {
                if ( watcher != null ) {
                    if ( success ) {
                        watcher.loadSucceeded( props );
                    }
                    else {
                        watcher.loadFailed( props, lastException );
                    }
                }
            }
            filesDone++;
        }

        //  Report any failures. If there is just one make usual report.
        //  Report to the registered instance of Watch if set.
        if ( failures != null ) {
            String message = null;
            if ( failedFiles == 1 ) {
                message = lastException.getMessage();
            }
            else {
                message = "Failed to open " + failedFiles + " spectra";
                lastException = new SplatException( failures.toString() );
            }
            ErrorDialog.showError( browser, message, lastException );
        }

        //  And now display them if we can.
        if ( display && validFiles > 0 ) {
            int count = globalList.specCount();
            browser.displayRange( initialsize, count - 1 );
        }
    }

    /**
     * Save a given spectrum as a file.
     *
     * @param globalIndex global list index of the spectrum to save.
     * @param target the file to write the spectrum into.
     */
    public void save( SplatBrowser browser, int globalIndex, String target )
    {
        //  Monitor progress by checking the filesDone variable.
        progressFrame.setTitle( "Saving spectrum..." );
        progressFrame.setMessage( "as " + target );
        progressFrame.start();
        this.browser = browser;
        browser.setWaitCursor();

        //  Set up thread to save the spectrum.
        loadThread.setAdd( false );
        loadThread.setSave( globalIndex, target );
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
        protected String errorColumn;
        protected SourceType sourceType;
        protected String idValue;
        protected String idSource;
        protected String dataLinkRequest;
        protected String dataLinkFormat;
        protected ObjectTypeEnum objectType;

        protected String serverURL;

        public Props( String spectrum )
        {
            this( spectrum, SpecDataFactory.DEFAULT, null, null, null,
                  null, null, null );
        }

        public Props( String spectrum, int type, String shortName )
        {
            this( spectrum, type, shortName, null, null, null, null, null );
        }

        public Props( String spectrum, int type, String shortName,
                      String dataUnits, String coordUnits,
                      String dataColumn, String coordColumn )
        {
            this( spectrum, type, shortName, dataUnits, coordUnits,
                  dataColumn, coordColumn, null );
        }

        public Props( String spectrum, int type, String shortName,
                String dataUnits, String coordUnits,
                String dataColumn, String coordColumn,
                String errorColumn )
        {
           
            this( spectrum, type, shortName, dataUnits, coordUnits,
                  dataColumn, coordColumn, null, SourceType.UNDEFINED, null, null );
        }
        
        public Props( String spectrum, int type, String shortName,
                String dataUnits, String coordUnits,
                String dataColumn, String coordColumn,
                String errorColumn, SourceType sourceType, String idsrc, String idValue ) {
        	
        	this(spectrum, type, shortName,
                    dataUnits, coordUnits,
                    dataColumn, coordColumn,
                    errorColumn, sourceType, idsrc, idValue, 
                    null );
        }
        
        public Props( String spectrum, int type, String shortName,
                      String dataUnits, String coordUnits,
                      String dataColumn, String coordColumn,
                      String errorColumn, SourceType sourceType, String idsrc, String idValue,
                      ObjectTypeEnum objectType)
        {
            this.spectrum = spectrum;
            this.type = type;
            this.shortName = shortName;
            this.dataUnits = dataUnits;
            this.coordUnits = coordUnits;
            this.dataColumn = dataColumn;
            this.coordColumn = coordColumn;
            this.errorColumn = errorColumn;
            this.sourceType = sourceType;
            this.dataLinkRequest=null;
            this.dataLinkFormat=null; 
            this.idValue=idValue;
            this.idSource = idsrc;
            this.serverURL=null;
            this.objectType = objectType;
        }

        public String getSpectrum()
        {
            if (serverURL == null )
                return spectrum;
            
            if (idValue != null && dataLinkRequest != null && ! dataLinkRequest.isEmpty()) 
                if ( serverURL.endsWith("?"))
                    try {
                        return (serverURL+"ID="+URLEncoder.encode(idValue, "UTF-8")+dataLinkRequest);
                       // return (serverURL+"ID="+idValue+);
                    } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                else
                    try {
                     
                        return (serverURL+ "?"+"ID="+URLEncoder.encode(idValue, "UTF-8")+dataLinkRequest);
            
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            
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

        public String getErrorColumn()
        {
            return errorColumn;
        }

        public void setErrorColumn( String errorColumn )
        {
            this.errorColumn = errorColumn;
        }
        
        public SourceType getSourceType() {
            return sourceType;
        }
        
        public void setSourceType(SourceType sourceType) {
            this.sourceType = sourceType;
        }

        public String getidValue()
        {
            return idValue;
        }

        public void setIdValue( String idValue )
        {
            this.idValue = idValue; //!!!
        }

        public void setDataLinkRequest( String dataLinkRequest )
        {
            this.dataLinkRequest = dataLinkRequest;
        }
        public String getDataLinkRequest()
        {
            return dataLinkRequest;
        }

        public String getServerURL()
        {
            return serverURL;
        }

        public void setServerURL( String serverURL )
        {
            this.serverURL = serverURL;
        }

        public ObjectTypeEnum getObjectType() {
			return objectType;
		}
        
        public void setObjectType(ObjectTypeEnum objectType) {
			this.objectType = objectType;
		}
        
        //  Create a copy of this object.
        public Props copy()
        {
            return new Props( spectrum, type, shortName, dataUnits, coordUnits,
                              dataColumn, coordColumn, errorColumn );
        }

        /**
         * Apply the tranmutable properties of this object to a SpecData
         * instance. This does not include the spectrum specification, or data
         * type, these must be used to open the spectrum.
         */
        public void apply( SpecData spectrum )
        {
            boolean initialiseNeeded = false;
            if ( shortName != null && shortName.length() != 0 ) {
                spectrum.setShortName( shortName );
            }

            if ( dataUnits != null && dataUnits.length() != 0 ) {
                spectrum.setDataUnits( dataUnits );
                initialiseNeeded = true;
            }

            if ( coordUnits != null && coordUnits.length() != 0 ) {
                //  Bit tricky, set the units of the underlying frame produced
                //  from the data and reinitialise AST. Still might be lost if
                //  the spectrum is reloaded from disk file.
                String units = UnitUtilities.fixUpUnits( coordUnits );
                FrameSet frameSet = spectrum.getFrameSet();
                frameSet.setUnit( 1, units );
                initialiseNeeded = true;
            }

            if ( coordColumn != null && coordColumn.length() != 0 ) {
                try {
                    spectrum.setXDataColumnName( coordColumn, false );
                    initialiseNeeded = true;
                }
                catch (SplatException e) {
                    e.printStackTrace();
                }
            }

            if ( dataColumn != null && dataColumn.length() != 0 ) {
                try {
                    if ( spectrum.setYDataColumnName( dataColumn ) ) {
                        //  Coordinate systems update already done.
                        initialiseNeeded = false;
                    }
                }
                catch (SplatException e) {
                    e.printStackTrace();
                }
            }

            if ( errorColumn != null && errorColumn.length() != 0 ) {
                try {
                    spectrum.setYDataErrorColumnName( errorColumn );
                }
                catch (SplatException e) {
                    e.printStackTrace();
                }
            }
            /*
            if ( pubdidColumn != null && pubdidColumn.length() != 0 ) {
                try {
                    spectrum.setsetGetDataColumnName( pubdidColumn );
                }
                catch (SplatException e) {
                    e.printStackTrace();
                }
            }
             */
            if ( initialiseNeeded ) {
                //  Probably changed something important to the coordinate
                //  systems, so make sure AST descriptions are up to date.
                try {
                    spectrum.initialiseAst();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            if (objectType != null) {
            	spectrum.setObjectType(objectType);
            }
        }

        public void setDataLinkFormat(String format) {
            
            dataLinkFormat = format;
            
        }
        public String getDataLinkFormat() {
            
            return dataLinkFormat;
            
        }

        public void setIdSource(String idsrc) {
            
            idSource = idsrc;
            
        }

    }

    // Inner class for loading and saving Spectra.
    // Extend with other operations if needed.
    private class Loader
        extends Thread
    {
        private boolean addAction = true;
        private int globalIndex = 0;
        private String target = null;

        public Loader()
        {
            super( "Spectra loader" );
        }

        //  Set run action, either add to browser or save to disk.
        public void setAdd( boolean action )
        {
            addAction = action;
        }

        //  If saving set the necessary parameters.
        public void setSave( int globalIndex, String target )
        {
            this.globalIndex = globalIndex;
            this.target = target;
        }

        //  Thread run() method. This never exits as that would cause the
        //  thread to die and we'd lose the context. When there are no spectra
        //  to load or save this waits for a while and then checks again.
        //  In principle this could be hurried by using notify(), but that
        //  isn't necessary.
        public void run()
        {
            while ( !interrupted() ) {
                if ( browser != null ) {
                    if ( addAction ) {
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
                            progressFrame.stop();
                            if ( progressFrame.isInterrupted() ) {
                                queue.clear();
                            }
                        }
                    }
                    else {
                        try {
                            if ( target != null ) {
                                synchronized( target ) {
                                    //  Next loop will re-save if target is
                                    //  set, so clear that now, also do this
                                    //  before any exceptions could be thrown.
                                    String tcopy = target;
                                    target = null;
                                    browser.saveSpectrum( globalIndex,
                                                          tcopy );
                                }
                            }
                        }
                        catch (Exception e) {
                            ErrorDialog.showError( browser,
                                                   "Failed to save spectrum: "
                                                   + e.getMessage(), e );
                        }
                        finally {
                            //  Always tidy up and rewaken interface when
                            //  complete (including if an error is thrown).
                            browser.resetWaitCursor();
                            progressFrame.stop();
                            if ( progressFrame.isInterrupted() ) {
                                queue.clear();
                            }
                        }
                    }
                }
                waitDelay();
                if ( progressFrame.isInterrupted() ) {
                    queue.clear();
                }
            }
        }

        //  Wait for a while, then service the run method again for new
        //  actions.
        synchronized void waitDelay() {
            try {
                wait( 500 );
            }
            catch (InterruptedException e) {
            }
        }
    }

    /**
     * Set a watcher to be informed when the load queue has been emptied.
     *
     * @param watcher an instance of the Watch interface. Set to null
     *                to clear. Remember to do that otherwise you'll
     *                get reports about all spectra being loaded.
     */
    public void setWatcher( Watch watcher )
    {
        this.watcher = watcher;
    }

    /**
     * Interface for objects that wish to be informed about the result of
     * spectrum load attempts performed by addSpectra().  One or other
     * of the methods will be called for each load attempt.
     */
    public interface Watch
    {
        /**
         * Reports that a spectrum with given props was successfully loaded.
         *
         * @param   props  props object giving spectrum characteristics
         */
        public void loadSucceeded( Props props );

        /**
         * Reports that a load attempt for the given props failed.
         *
         * @param   props  props object giving spectrum characteristics
         * @param   error  exception resulting from load attempt
         */
        public void loadFailed( Props props, Throwable error );
    }
}
