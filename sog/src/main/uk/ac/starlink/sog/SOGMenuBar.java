/*
 * ESO Archive
 * 
 * $Id$
 * 
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package uk.ac.starlink.sog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import jsky.util.I18N;
import jsky.util.gui.LookAndFeelMenu;
import jsky.app.jskycat.*;;


/** 
 * Implements the menubar for the SOG application class. 
 */
public class SOGMenuBar extends JMenuBar 
{
    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(JSkyCatMenuBar.class);

    /** Target class */
    protected SOG jSkyCat;

    /** Handle for the File menu */
    protected JMenu fileMenu;

    /** Handle for the View menu */
    protected JMenu viewMenu;


    
    /**
     * Create the menubar for the given SOG instance
     */
    public SOGMenuBar(SOG jSkyCat) {
	super();
	this.jSkyCat = jSkyCat;
	add(createFileMenu());
	//add(createViewMenu());
    }

    /**
     * Create the File menu. 
     */
    protected JMenu createFileMenu() {
	fileMenu = new JMenu(_I18N.getString("file"));
	fileMenu.add(createFileOpenMenuItem());
	fileMenu.addSeparator();
	fileMenu.add(createFileExitMenuItem());
	return fileMenu;
    }

    /**
     * Create the View menu. 
     */
    protected JMenu createViewMenu() {
	viewMenu = new JMenu(_I18N.getString("view"));
	//viewMenu.add(new LookAndFeelMenu());
	return viewMenu;
    }


    /**
     * Create the File => Open menu item
     */
    protected JMenuItem createFileOpenMenuItem() {
	JMenuItem menuItem = new JMenuItem(_I18N.getString("open"));
        menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		jSkyCat.open();
	    }
	});
	return menuItem;
    }

    /**
     * Create the File => Exit menu item
     */
    protected JMenuItem createFileExitMenuItem() {
	JMenuItem menuItem = new JMenuItem(_I18N.getString("exit"));
        menuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ae) {
		jSkyCat.exit();
	    }
	});
	return menuItem;
    }
}
