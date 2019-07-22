/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     22-MAR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;

/**
 * Class that makes any locally installed examples available and arranges for
 * them to be loaded into SPLAT. All examples are stored in SPLAT serialised
 * stacks that are contained in the "/examples" hierarchy. The examples to
 * be used are listed in the simple text file "list", which is located with
 * the stacks.
 * <p>
 * The format of the list file is one example per-line, with the following
 * details, name of the stack file, title for the example (additional graphics
 * and plot options could be added?). Each part is separated by tabs.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ExamplesManager
    implements ActionListener
{
    /**
     * The menu to populate with the available examples.
     */
    protected JMenu targetMenu = null;

    /**
     * A SplatBrowser, used to load and display the spectra.
     */
    protected SplatBrowser browser = null;

    /**
     * Constructor.
     *
     * @param targetMenu the menu to populate.
     * @param browser the SplatBrowser for displaying any spectra.
     */
    public ExamplesManager( JMenu targetMenu, SplatBrowser browser )
    {
        this.targetMenu = targetMenu;
        this.browser = browser;
        addExamples();
    }

    /**
     * Gather the available examples and create a menu system to represent
     * them.
     */
    protected void addExamples()
    {
        JMenu examplesMenu = new JMenu( "Example data" );
        if ( buildMenus( examplesMenu ) ) {
            targetMenu.add( examplesMenu );
        }
    }

    /**
     * Create a series of menus for each of the example datasets. Returns
     * false if menus are not created for some reason.
     */
    protected boolean buildMenus( JMenu menu )
    {
        //  Access the examples list.
        InputStream strm = 
            ExamplesManager.class.getResourceAsStream( "/examples/list" );
        if ( strm == null ) {
            return false;
        }
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( strm ) );

        List fileNames = new ArrayList();
        List shortNames = new ArrayList();
        String[] parts = null;
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                //  Parse line into filename and title.
                parts = line.split( "\t" );
                fileNames.add( "/examples/" + parts[0].trim() );
                shortNames.add( parts[1] );
            }
            rdr.close();
        }
        catch ( IOException e ) {
            return false;
        }

        JMenuItem item = null;
        for ( int i = 0; i < fileNames.size(); i++ ) {
            item = new JMenuItem( (String) shortNames.get(i) );
            menu.add( item );
            item.addActionListener( this );
            item.setActionCommand( (String) fileNames.get(i) );
        }
        if ( ! fileNames.isEmpty() ) {
            return true;
        }
      
        return false;
    }

//
//  Implement the ActionListener interface for loading an example dataset.
//
    public void actionPerformed( ActionEvent e )
    {
        JMenuItem item = (JMenuItem) e.getSource();
        String uri = item.getActionCommand();
        InputStream strm = 
            ExamplesManager.class.getResourceAsStream( uri );
        int plotIndex = browser.readStack( strm, true );

        //  XXX in the future we could store both of the configurations
        //  associated with a Plot and recover them (from the XML form).
        //  For now just switch on log-scaling.
        if ( plotIndex != -1 ) {
            GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();
            PlotControl plotControl = globalList.getPlot( plotIndex );
            DivaPlot plot = plotControl.getPlot();
            plot.getAstAxes().setXLog( true );
            plot.getAstAxes().setYLog( true );
            
            //  Make sure we match coordinates.
            plotControl.getSpecDataComp().setCoordinateMatching( true );
        }
    }
}
