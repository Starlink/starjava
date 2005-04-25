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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.ObjectInputStream;

import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.splat.data.LineIDSpecData;
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
     * Constructor. Single argument is the menu to populate
     *
     * @param targetMenu the menu to populate.
     */
    public LocalLineIDManager( JMenu targetMenu, SplatBrowser browser )
    {
        this.targetMenu = targetMenu;
        this.browser = browser;
        readDescriptionFile();
        addLineIDs();
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
        if ( propsList.size() > 0 ) {
            JMenu lineIDMenu = new JMenu( "Line identifiers" );
            targetMenu.add( lineIDMenu );
            buildMenus( lineIDMenu );
        }
    }

    /**
     * Create a series of menus for the a directory. If any
     * directories are encountered these are added as submenus (unless
     * they are empty).
     */
    protected void buildMenus( JMenu menu )
    {
        int nprops = propsList.size();
        File file;
        JMenu newMenu;
        JMenuItem item;
        LineProps props;
        String name;
        String parent;
        HashMap subMenus = new HashMap();
        int index;

        for ( int i = 0; i < nprops; i++ ) {
            props = (LineProps) propsList.get( i );
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

    //
    //  Action for containing the properties of each line identifier spectrum.
    //
    protected class LineProps
        extends AbstractAction
    {
        private String name;
        private String fullName;
        private double[] range = new double[2];
        private String units;
        private String system;
        private LineIDSpecData specData = null;

        public LineProps( String description )
            throws SplatException
        {
            process( description );
        }
        private void process( String description )
            throws SplatException
        {
            String[] words = description.split( "\t" );
            if ( words.length == 5 ) {
                setFullName( words[0] );
                setName( words[0] );
                setRange( words[1], words[2] );
                setUnits( words[3] );
                setSystem( words[4] );
            }
            else {
                throw new SplatException( "Description file is corrupt" );
            }
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

        public void setUnits( String units )
        {
            this.units = units;
        }
        public String getUnits()
        {
            return this.units;
        }

        public void setSystem( String system )
        {
            this.system = system;
        }
        public String getSystem()
        {
            return this.system;
        }

        public String toString()
        {
            return name;
        }

        //
        //  Implement the ActionListener interface. This deserializes the
        //  stored LineIDSpecData object and makes it available on the global
        //  list.
        //
        public void actionPerformed( ActionEvent e )
        {
            InputStream specStr =
                this.getClass().getResourceAsStream( "/ids" + fullName );
            if ( specStr != null ) {
                try {
                    ObjectInputStream ois = new ObjectInputStream( specStr );
                    specData = (LineIDSpecData) ois.readObject();
                    ois.close();
                    globalList.add( specData );
                }
                catch (Exception ie) {
                    logger.log( Level.INFO, ie.getMessage(), ie );
                }
            }
        }
    }
}
