package uk.ac.starlink.splat.iface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import uk.ac.starlink.splat.util.Utilities;

/**
 * This class interacts with a permanent set of configuration states
 * stored in an XML-format disk file (PlotConfigs.xml). Each state is
 * identified by a description and date stamp, which are added when a
 * state is stored. The content of the state isn't actually understood
 * at this level that is left to more specific classes.
 * <p>
 * An instance of this class presents itself as two services. One is
 * as a JTable model, this provides a model of two columns, the first
 * being the state description the second the date stamp. The second
 * set of services are provided to get at states and to store new
 * ones. These latter options are provided by passing back references
 * to suitable Elements of a DOM, which then act as roots for the
 * state.
 * <p>
 * The format of the XML file is just determined by the practices
 * adopted in this file, rather than by a DTD or Schema. The root
 * element is <splat-plot-configs> each child of this element is
 * called <config> with the attributes "description" and "date-stamp",
 * what goes after this is determined by the writers of the
 * configurations, but the general idea is for each object in the
 * configuration to write its state to a new Element.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see #Element.
 */
public class PlotConfigStore extends AbstractTableModel
{
    // TODO: should there be just one instance of this class? There
    // may be contention with the backing store if not.

    /**
     * The Document.
     */
    protected Document document = null;

    /**
     * Document root Element.
     */
    protected Element rootElement = null;

    /**
     * Create an instance. This synchronises the current total state
     * with that of the backing store.
     */
    public PlotConfigStore()
    {
        initFromBackingStore();
    }

    /**
     * Initialise the local DOM from the backing store file. If this
     * doesn't exist then just create an empty Document with a basic
     * root Element.
     */
    public void initFromBackingStore()
    {
        //  Locate the backing store file.
        File backingStore = Utilities.getConfigFile("PlotConfigs.xml");
        if ( backingStore.canRead() ) {

            //  And parse it into a Document.
            SAXBuilder builder = new SAXBuilder( false );
            try {
                document  = builder.build( backingStore );
            }
            catch (Exception e) {
                document = null;
                e.printStackTrace();
            }
        }

        //  If the document is still null create a default one.
        if ( document == null ) {
            rootElement = new Element( "splat-plot-configs" );
            document = new Document( rootElement );
        }
        else {
            //  Locate the root Element.
            rootElement = document.getRootElement();
        }
        fireTableDataChanged();
    }

    /**
     * Get the number of states that are stored.
     */
    public int getCount()
    {
        //  This is the number of children of the root Element.
        return rootElement.getChildren().size();
    }

    /**
     * Get a state from the store. These are indexed simply by the
     * order in the current document.
     */
    public Element getState( int index )
    {
        List children = rootElement.getChildren();
        return (Element) children.get( index );
    }

    /**
     * Get the description of a state by index.
     */
    public String getDescription( int index )
    {
        return getState( index ).getAttribute( "description" ).
            getValue();
    }

    /**
     * Set the description of a state by index.
     */
    public void setDescription( int index, String value )
    {
        getState( index ).getAttribute( "description" ).
            setValue(value);
        fireTableRowsUpdated( index, index );
    }

    /**
     * Get the date stamp of a state by index.
     */
    public String getDateStamp( int index )
    {
        return getState( index ).getAttribute( "date-stamp" ).getValue();
    }

    /**
     * Set the date stamp of a state by index. Updates to
     * representation of the current time.
     */
    public void setDateStamp( int index )
    {
        getState( index ).getAttribute( "date-stamp" ).
            setValue( new Date().toString() );
        fireTableRowsUpdated( index, index );
    }

    /**
     * Add a new state root in a given Element. The Element should be
     * created by the newState method.
     */
    public void stateCompleted( Element newState )
    {
        rootElement.addContent( newState );
        fireTableDataChanged();
    }

    /**
     * Create a new Element ready for attaching a configuration state
     * to (i.e. get this Element then write the configuration data
     * attached to it). When the new configuration is completed invoke
     * the stateCompleted method, the configuration will not be part
     * of the structure until then.
     */
    public Element newState( String description )
    {
        Element newRoot = new Element( "config" );
        newRoot.setAttribute( "description", description );
        newRoot.setAttribute( "date-stamp", new Date().toString() );
        return newRoot;
    }

    /**
     * Permanently remove a state from store.
     */
    public void removeState( int index )
    {
        Element child = getState( index );
        String name = child.getName();
        rootElement.removeChild( name );
        fireTableRowsDeleted( index, index );
    }

    /**
     * Save the Document to backing store.
     */
    public void writeToBackingStore()
    {
        File backingStore = Utilities.getConfigFile("PlotConfigs.xml");
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( backingStore );
            r = new BufferedWriter( new OutputStreamWriter( f ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        XMLOutputter out = new XMLOutputter( "   ", true );
        out.setTextNormalize( true );
        out.setEncoding( "ISO-8859-1" ); // ?? User can type in funny
                                         // characters from
                                         // fonts. Default is UTF-8
        try {
            out.output( document, r );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            f.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

//
// AbstractTableModel interface.
//
    /**
     * Return the numbers of states that we're currently storing.
     */
    public int getRowCount()
    {
        return getCount();
    }

    /**
     * Get the number of columns. Always 2.
     */
    public int getColumnCount()
    {
        return 2;
    }

    /**
     * Return either the description or date stamp of a state.
     */
    public Object getValueAt( int row, int column )
    {
        if ( column == 0 ) {
            return getDescription( row );
        } else {
            return getDateStamp( row );
        }
    }

    /**
     * Get the name of a column.
     */
    public String getColumnName( int column )
    {
        if ( column == 0 ) {
            return "Description";
        }
        else {
            return "Date";
        }
    }

    /**
     * Change a description in response to a user edit.
     */
    public void setValueAt( Object value, int row, int column )
    {
        setDescription( row, (String) value );
    }

    /**
     * Let the JTable know that the descriptions are edittable.
     */
    public boolean isCellEditable( int row, int column )
    {
        if ( column == 0 ) {
            return true;
        }
        return false;
    }
}
