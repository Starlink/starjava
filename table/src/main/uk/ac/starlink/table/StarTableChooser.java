package uk.ac.starlink.table;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import uk.ac.starlink.util.FileDataSource;

/**
 * File browser widget which permits selection of a {@link StarTable}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableChooser extends JFileChooser {

    private StarTableFactory tabfact = new StarTableFactory();

    /**
     * Constructs a <tt>StarTableChooser</tt> pointing to a give directory.
     *
     * @param  dir  the directory where this chooser
     */
    public StarTableChooser( File dir ) {
        super( dir );
        setFileSelectionMode( JFileChooser.FILES_ONLY );
    }

    /**
     * Constructs a <tt>StarTableChooser</tt> pointing to a user's default
     * directory.
     */
    public StarTableChooser() {
        this( null );
    }

    /**
     * Returns an existing <tt>StarTable</tt> object which has been 
     * selected by the user.  In the event that the user declines to
     * pick a file which can be made into a valid <tt>StarTable</tt>
     * then <tt>null</tt> is returned.
     *
     * @param  parent  the parent component, used for window positioning etc
     * @return  a new <tt>StarTable</tt> object as selected by the user,
     *          or <tt>null</tt>
     */
    public StarTable getTable( Component parent ) {
        while ( showOpenDialog( parent ) == APPROVE_OPTION ) {
            File file = getSelectedFile();
            String explanation;
            try {
                return tabfact.makeStarTable( new FileDataSource( file ) );
            }
            catch ( IOException e ) {
                JOptionPane
               .showMessageDialog( parent, e.getMessage(), 
                                   "No such table " + file.getName(),
                                   JOptionPane.ERROR_MESSAGE );
            }

            /* Exception other than IOException probably shouldn't happen,
             * but if not caught the main thead will die but not exit
             * since the event dispatcher will still be alive. */
            catch ( Throwable e ) {
                e.printStackTrace( System.err );
            }
        }
        return null;
    }

    /**
     * Returns the factory object which this chooser
     * uses to resolve files into <tt>StarTable</tt>s.
     *
     * @return  the factory
     */
    public StarTableFactory getStarTableFactory() {
        return tabfact;
    }

    /**
     * Sets the factory object which this chooser
     * uses to resove files into <tt>StarTable</tt>s.
     *
     * @param  the factory
     */
    public void setStarTableFactory( StarTableFactory tabfact ) {
        this.tabfact = tabfact;
    }
}
