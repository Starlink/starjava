/*
 * Copyright (C) 2003-2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-MAY-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.DSBSpecFrame;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.LineIDSpecDataImpl;
import uk.ac.starlink.splat.data.LineIDTXTSpecDataImpl;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;

/**
 * Class that makes any locally installed line identification files
 * available as a series of items in a sub-menu.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LocalLineIDManager
{
    // The instance of this class. There is only one.
    private static LocalLineIDManager instance = null;

    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.iface.LocalLineIDManager" );

    // Global list of spectra and plots.
    private static GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getInstance();

    /**
     * Name of the line identifier description file.
     */
    protected static final String descFile = "/ids/description.txt";

    /**
     * The properties of the line identifiers.
     */
    protected ArrayList propsList = new ArrayList();

    /**
     * The menu to populate with the available line identifiers.
     */
    protected JMenu targetMenu = null;

    /**
     * A SplatBrowser, used to create the spectrum and add to global lists.
     */
    protected SplatBrowser browser = null;

    /**
     * Constructor. Hidden from use.
     */
    private LocalLineIDManager()
    {
        readDescriptionFile();
    }

    /**
     * Return reference to the single instance of this class.
     */
    public static synchronized LocalLineIDManager getInstance()
    {
        if ( instance == null ) {
            instance = new LocalLineIDManager();
        }
        return instance;
    }

    /**
     * Populate and remain attached to a menu that displays all the known line
     * identifiers. This can only be used once per-application.
     *
     * @param targetMenu the menu to populate.
     * @param browser where spectra will be realized.
     */
    public void populate( JMenu targetMenu, SplatBrowser browser )
    {
        this.targetMenu = targetMenu;
        this.browser = browser;
        addLineIDs();
    }

    /**
     * Add a line identifier spectrum. Used when spectra not in the main
     * repository are added.
     *
     * @param specData the new line identifier spectrum.
     */
    public void addSpectrum( LineIDSpecData specData )
        throws SplatException
    {
        LineProps props = new LineProps( specData );
        propsList.add( props );
    }

    /**
     * The local identifiers are stored in a special jar file which should be
     * on the CLASSPATH. We need these in a jar file so that they can be
     * downloaded for webstart, but once in a jar file it's difficult to get
     * all the file names, so these are stored in a special file found with
     * the identifiers. This file also contains the details about the spectral
     * coordinate ranges and units that the identifiers
     */
    protected void readDescriptionFile()
    {
        //  Look for the description file.
        InputStream descStr= this.getClass().getResourceAsStream( descFile );
        if ( descStr == null ) {
            logger.warning(  "No line identifiers are available" );
            return;
        }

        //  And read it.
        BufferedReader bf =
            new BufferedReader( new InputStreamReader( descStr ) );
        String line;
        LineProps props;
        try {
            while ( ( line = bf.readLine() ) != null ) {
                try {
                    props = new LineProps( line );
                    propsList.add( props );
                }
                catch (SplatException e) {
                    logger.log( Level.INFO, e.getMessage(), e );
                }
            }
        }
        catch (IOException ie) {
            logger.log( Level.INFO, ie.getMessage(), ie );
        }
        try {
            descStr.close();
        }
        catch (IOException e) {
            //  Ignore.
        }
    }

    /**
     * Gather the available IDs and create a menu system to represent
     * them.
     */
    protected void addLineIDs()
    {
        if ( ! propsList.isEmpty() ) {
            JMenu lineIDMenu = new JMenu( "Line identifiers" );
            targetMenu.add( lineIDMenu );
            buildMenus( lineIDMenu );
            addChooser( lineIDMenu );
        }
    }

    /**
     * Create a series of menus for the a directory. If any
     * directories are encountered these are added as submenus (unless
     * they are empty).
     */
    protected void buildMenus( JMenu menu )
    {
        File file;
        JMenu newMenu;
        JMenuItem item;
        LineProps props;
        String name;
        String parent;
        HashMap subMenus = new HashMap();

        Iterator i = propsList.iterator();
        while( i.hasNext() ) {
            props = (LineProps) i.next();
            name = props.getName().substring( 1 ); // Strip leading "/"
            file = new File( name );
            parent = file.getParent();
            if ( parent != null ) {
                //  Is directory, have we got this already?
                if ( subMenus.containsKey( parent ) ) {
                    newMenu = (JMenu) subMenus.get( parent );
                }
                else {
                    newMenu = new JMenu( parent );
                    menu.add( newMenu );
                    subMenus.put( parent, newMenu );
                }

                //  Use name of file in directory, not full name.
                name = file.getName();
                item = new JMenuItem( name );
                newMenu.add( item );
            }
            else {
                item = new JMenuItem( name );
                menu.add( item );
            }

            //  Make sure LineProps has name suitable for showing in menu.
            props.setName( name );
            item.setAction( props );
        }
    }

    /**
     * Add an option to the menus for choosing from a list of names, rather
     * than specifying one-by-one.
     */
    protected void addChooser( JMenu lineIDMenu )
    {
        lineIDMenu.add( new JMenuItem( new LineIDChooser() ) );
    }

    /**
     * Optionally load and display all the line identifiers that match a given
     * spectral coordinate range. The line identifiers loaded can either be
     * all from those known, or just those that are already available in the
     * global list.  The range is specified as a lower and upper limit
     * together with a SpecFrame that describes the system that the coordinate
     * are defined in.
     */
    public int matchDisplayLoad( SpecFrame specFrame, double[] range,
                                 boolean load, boolean checkSideBand, 
                                 PlotControl control )
    {
        int count = matchDisplayLoader( specFrame, range, load, control );

        //  Repeat this all again for DSB spectra. We need to look 
        //  for matches in the other sideband.
        if ( checkSideBand && ( specFrame instanceof DSBSpecFrame ) ) {
            String sideband = specFrame.getC( "SideBand" );
            if ( ! "LO".equals( sideband ) ) {

                //  Need to convert the range into the coordinate system
                //  of the other side band, so keep a copy.
                DSBSpecFrame testSpecFrame = (DSBSpecFrame) specFrame.copy();

                if ( "USB".equals( sideband ) ) {
                    testSpecFrame.setC( "SideBand", "LSB" );
                }
                else {
                    testSpecFrame.setC( "SideBand", "USB" );
                }

                // Now transform the coordinates.
                testSpecFrame.setB( "AlignSideBand", true );
                specFrame.setB( "AlignSideBand", true );
                Mapping match = testSpecFrame.convert( specFrame, "" );
                if ( match != null ) {

                    range = match.tran1( 2, range, true );
                    double[] newrange = new double[2];
                    newrange[0] = Math.min( range[0], range[1] );
                    newrange[1] = Math.max( range[0], range[1] );
                    count += matchDisplayLoader( testSpecFrame, newrange,
                                                 load, control );
                }
            }
        }
        return count;
    }


    /**
     * See matchDisplayLoad. Does that job for a spectrum, repeated for
     * sideband switching.
     */
    private int matchDisplayLoader( SpecFrame specFrame, double[] range,
                                    boolean load, PlotControl control )
    {
        int count = 0;
        Iterator i = propsList.iterator();
        LineProps props;
       
        while( i.hasNext() ) {
            props = (LineProps) i.next();
            if ( props.intersects( specFrame, range ) ) {
                count++;
                props.maybeDisplaySpectrum( load, control );
            }
        }
        return count;
    }

    /**
     * Reload the underlying implementation of the given line identifier
     * spectrum. This can be used to refresh the actual spectrum by replacing
     * the existing implementation.
     */
    public LineIDSpecDataImpl reLoadSpecDataImpl( LineIDSpecData specData )
        throws SplatException
    {
        //  Check if we know about this one.
        Iterator i = propsList.iterator();
        LineProps props;
        while( i.hasNext() ) {
            props = (LineProps) i.next();
            if ( props.isOurLineIDSpecData( specData ) ) {
                return props.reLoadSpecDataImpl();
            }
        }
        throw new SplatException( "Internal error, unknown line identifier: "
                                  + specData.getShortName() );
    }

    //
    //  Action class for dealing with the properties of each line
    //  identifier spectrum.
    //
    protected class LineProps
        extends AbstractAction
    {
        private String name;
        private String fullName;

        // Coordinate range that the spectrum encompasses.
        private double[] range = new double[2];

        // Spectral coordinates system and units of the range.
        private String units;
        private String system;

        // The spectrum when deserialized.
        private LineIDSpecData specData = null;

        // SpecFrame describing the system and units. Only applies to the
        // initial coordinate range and may not be those of the specData
        // instance after it has been created (could be edited elsewhere).
        // Normally this is created lazily.
        private SpecFrame specFrame = null;

        // Type of backing for the spectrum. There will be two kinds, ones
        // loaded from the central repository and ones loaded from text
        // files.
        private boolean external = false;

        public LineProps( String description )
            throws SplatException
        {
            process( description );
        }

        public LineProps( LineIDSpecData specData )
            throws SplatException
        {
            process( specData );
        }

        private void process( String description )
            throws SplatException
        {
            String[] words = description.split( "\t" );
            if ( words.length == 5 ) {
                setFullName( words[0] );
                setName( words[0] );
                setRange( words[1], words[2] );
                setSystemAndUnits( words[4], words[3] );
            }
            else {
                throw new SplatException( "Description file is corrupt" );
            }
        }

        private void process( LineIDSpecData specData )
        {
            external = true;
            this.specData = specData;
            setFullName( specData.getFullName() );
            setName( specData.getShortName() );
            setRange( specData.getRange() );
            FrameSet frameSet = specData.getFrameSet();
            setSystemAndUnits( frameSet.getC( "System(1)" ),
                               frameSet.getC( "unit(1)" ) );
        }


        public void setName( String name )
        {
            this.name = name;
            putValue( NAME, name );
        }
        public String getName()
        {
            return this.name;
        }

        public void setFullName( String name )
        {
            this.fullName = name;
        }
        public String getFullName()
        {
            return this.fullName;
        }

        public void setRange( String xmin, String xmax )
        {
            try {
                range[0] = Double.parseDouble( xmin );
            }
            catch( NumberFormatException e ) {
                //  Shouldn't happen.
                logger.log( Level.INFO, e.getMessage(), e );
                range[0] = 0.0;
            }
            try {
                range[1] = Double.parseDouble( xmax );
            }
            catch( NumberFormatException e ) {
                //  Shouldn't happen.
                logger.log( Level.INFO, e.getMessage(), e );
                range[1] = 0.0;
            }
        }

        public void setRange( double[] range )
        {
            this.range[0] = range[0];
            this.range[1] = range[1];
        }

        public void setSystemAndUnits( String system, String units )
        {
            this.system = system;
            this.units = units;
            if ( specFrame != null ) {
                specFrame.setSystem( system );
                specFrame.setUnit( 1, units );

                //  Line identifier specifics for submilli... Don't think
                //  this will effect other bands.
                specFrame.setC( "StdOfRest", "Source" );
                specFrame.setC( "SourceVRF", "Topocentric" );
                specFrame.setD( "SourceVel", 0.0 );
            }
        }
        public String getSystem()
        {
            return this.system;
        }

        public String getUnits()
        {
            return this.units;
        }

        public String toString()
        {
            return name;
        }

        /**
         * Check if a given range of coordinates intersects the range of this
         * instance.
         *
         * @param targetFrame a SpecFrame that describes the coordinates of
         *                    the range.
         * @param targetRange lower and upper limits in the coordinate system
         *                    described by targetFrame.
         */
        public boolean intersects(SpecFrame targetFrame, double[] targetRange)
        {
            createSpecFrame();
            Mapping match = targetFrame.convert( specFrame, "" );
            if ( match != null ) {

                double[] matchedRange = match.tran1( 2, targetRange, true );
                double mintr = Math.min( matchedRange[0], matchedRange[1] );
                double maxtr = Math.max( matchedRange[0], matchedRange[1] );

                double minsr = Math.min( range[0], range[1] );
                double maxsr = Math.max( range[0], range[1] );

                if ( minsr <= maxtr && maxsr >= mintr  ) {
                    //!!!!!!!!!!!!!!!!!
                    return true;
                }
            }
            return false;
        }

        protected void createSpecFrame()
        {
            if ( specFrame == null ) {
                specFrame = new SpecFrame();
                setSystemAndUnits( system, units );
            }
        }

        public void maybeDisplaySpectrum( boolean load, PlotControl control )
        {
            int index = globalList.getSpectrumIndex( specData );
            if ( ! load && index == -1 ) {
                return;
            }
            loadSpectrum();
            if ( control != null ) {
                try {
                    globalList.addSpectrum( control, specData );
                }
                catch (SplatException e) {
                    logger.log( Level.INFO, e.getMessage(), e );
                }
            }
        }

        public void loadSpectrum()
        {
            // If this spectrum is already loaded then just make sure it is
            // still on the global list.
            if ( specData != null ) {
                int index = globalList.getSpectrumIndex( specData );
                if ( index == -1 ) {
                    globalList.add( specData );
                }
            }
            else {
                // Read the spectrum.
                readSpectrum();
                globalList.add( specData );
            }
        }

        /**
         * Reload the underlying implementation of the spectrum. This can be
         * used to refresh the actual spectrum by replacing the existing
         * implementation.
         */
        public LineIDSpecDataImpl reLoadSpecDataImpl()
            throws SplatException
        {
            LineIDSpecDataImpl impl = null;
            if ( external ) {
                impl = new LineIDTXTSpecDataImpl( fullName );
            }
            else {
                //  One from the repository. Need to re-read the SpecData
                //  instance and use the implementation from that.
                InputStream specStr =
                    this.getClass().getResourceAsStream( "/ids" + fullName );
                if ( specStr != null ) {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(specStr);
                        LineIDSpecData specData =
                            (LineIDSpecData) ois.readObject();
                        ois.close();
                        impl = (LineIDSpecDataImpl) specData.getSpecDataImpl();
                    }
                    catch (Exception ie) {
                        throw new SplatException( ie );
                    }
                }
                else {
                    //  Failed to find the identifier in the database.
                    throw new SplatException
                        ( "Failed to locate line identifier in database" );
                }
            }
            return impl;
        }

        protected void readSpectrum()
        {
            if ( external ) {
                //  In this case fullName is the file name.
                try {
                    LineIDTXTSpecDataImpl impl =
                        new LineIDTXTSpecDataImpl( fullName );
                    specData = new LineIDSpecData( impl );
                    FrameSet frameSet = specData.getFrameSet();
                    setSystemAndUnits( frameSet.getC( "system" ),
                                       frameSet.getC( "unit(1)" ) );
                    createSpecFrame();
                }
                catch (SplatException ie) {
                    logger.log( Level.INFO, ie.getMessage(), ie );
                }
            }
            else {
                //  One from the repository.
                InputStream specStr =
                    this.getClass().getResourceAsStream( "/ids" + fullName );
                if ( specStr != null ) {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(specStr);
                        specData = (LineIDSpecData) ois.readObject();
                        ois.close();
                        createSpecFrame();
                    }
                    catch (Exception ie) {
                        logger.log( Level.INFO, ie.getMessage(), ie );
                    }
                }
                else {
                    logger.warning
                        ( "Failed to locate line identifier in database" );
                }
            }
        }

        /**
         * See if a given LineIDSpecData is ours.
         */
        public boolean isOurLineIDSpecData( LineIDSpecData specData )
        {
            return ( this.specData == specData );
        }

        //
        //  Implement the ActionListener interface. This deserializes the
        //  stored LineIDSpecData object and makes it available on the global
        //  list.
        //
        public void actionPerformed( ActionEvent e )
        {
            loadSpectrum();
        }
    }

    //
    // Class for controlling a dialog showing the full list allowing multiple
    // selections.
    //
    protected class LineIDChooser
        extends AbstractAction
    {
        public LineIDChooser()
        {
            super( "Multiple selection..." );
        }

        /**
         * Show a dialog of all the known line identifiers. The user can
         * select a range of these to load.
         */
        public void showDialog()
        {
            Object[] selection =
                SelectListDialog.showDialog( browser, "Line identifiers",
                                             "Choose line identifiers",
                                             propsList.toArray() );
            if ( selection != null ) {
                for ( int i = 0; i < selection.length; i++ ) {
                    ((LineProps)selection[i]).loadSpectrum();
                }
            }
        }

        //
        //  Implement the ActionListener interface. This pops up a dialog for
        //  choosing a set of spectra,
        //
        public void actionPerformed( ActionEvent e )
        {
            showDialog();
        }
    }
}
