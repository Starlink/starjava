/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import uk.ac.starlink.util.SourceReader;

/**
 * This class interacts with a permanent set of configuration states
 * stored in an XML-format disk file. Each state is identified by a
 * description and date stamp, which are added when a state is
 * stored. The content of the state isn't actually understood at this
 * level that is left to more specific classes.
 * <p>
 * An instance of this class presents itself as two services. One is
 * as a JTable model, this provides a model of two columns, the first
 * being the state description the second the date stamp. The second
 * set of services are provided to get at states and to store new
 * ones. These latter options are provided by passing back references
 * to suitable Elements of a Document, which then act as roots for the
 * state.
 * <p>
 * The format of the XML file is just determined by the practices
 * adopted in this file and by the {@link StoreSource}
 * implementation, rather than by a DTD or Schema. The root element is
 * &lt;configs&gt; each child of this element is called whatever
 * {@link StoreSource#getTagName} returns, with the attributes "description"
 * and "date-stamp", what goes after this is determined by the writer
 * of the configurations, but the general idea is for each object in
 * the configuration to write its state to a new Element.
 *
 * @author Peter W. Draper
 * @see Element
 * @see StoreControlFrame
 * @see StoreSource
 */
public class StoreConfiguration
    extends AbstractTableModel
{
    /**
     * The Document.
     */
    protected Document document = null;

    /**
     * Document root Element.
     */
    protected Element rootElement = null;

    /**
     * Name of the application (used for name of config directory).
     */
    protected String applicationName = null;

    /**
     * Name of the file used for storage.
     */
    protected String storeName = null;

    /**
     * Create an instance. This synchronises the current total state
     * with that of the backing store.
     *
     * @param applicationName name of the application controlling this
     *                        store. Used to create a top-element,
     *                        also defines the configuration directory.
     *                        XXX use Properties for this?
     * @param storeName name of the file that contains the
     *                  configuration
     */
    @SuppressWarnings("this-escape")
    public StoreConfiguration( String applicationName, String storeName )
    {
        this.applicationName = applicationName;
        this.storeName = storeName;
        initFromBackingStore();
    }

    /**
     * Create an instance. This synchronises the current total state
     * with that read from a given InputStream (useful when want to
     * get a default configuration using a getResource()). If you use
     * this method it is not possible to save to backing store.
     *
     * @param inputStream InputStream that contains an XML description of a
     *               series of configurations (i.e. a wrapped backing
     *               store file).
     *
     */
    @SuppressWarnings("this-escape")
    public StoreConfiguration( InputStream inputStream )
    {
        initFromBackingStore( inputStream );
    }

    /**
     * Initialise the local DOM from an InputStream.
     */
    public void initFromBackingStore( InputStream inputStream )
    {
        //  And parse it into a Document.
        StreamSource saxSource = new StreamSource( inputStream );
        try {
            document = (Document) new SourceReader().getDOM( saxSource );
        }
        catch (Exception e) {
            document = null;
            e.printStackTrace();
        }

        //  If the document is still null create a default one.
        if ( document == null ) {
            createEmptyDoc();
        }
        else {
            //  Locate the root Element.
            rootElement = document.getDocumentElement();
        }
        fireTableDataChanged();
    }

    /**
     * Initialise the local DOM from the backing store file. If this
     * doesn't exist then just create an empty Document with a basic
     * root Element.
     */
    public void initFromBackingStore()
    {
        //  Locate the backing store file.
        File backingStore = StoreFileUtils.getConfigFile( applicationName,
                                                          storeName );
        if ( backingStore.canRead() ) {
            try {
                FileInputStream inputStream =
                    new FileInputStream( backingStore );
                initFromBackingStore( inputStream );
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            // Need dummy version.
            createEmptyDoc();
        }
    }

    /** Create an empty document */
    protected void createEmptyDoc()
    {
        try {
            DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.newDocument();
            rootElement = document.createElement( "configs" );
            document.appendChild( rootElement );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return all the Elements that are children of another Element.
     */
    public static List<Element> getChildElements( Element element )
    {
        NodeList nodeList = element.getChildNodes();
        List<Element> elementList = new ArrayList<Element>();
        for ( int i = 0; i < nodeList.getLength(); i++ ) {
            if ( nodeList.item( i ) instanceof Element ) {
                elementList.add( (Element) nodeList.item( i ) );
            }
        }
        return elementList;
    }

    /**
     * Get the number of states that are stored.
     */
    public int getCount()
    {
        //  This is the number of children of the root Element.
        return getChildElements( rootElement ).size();
    }

    /**
     * Get a state from the store. These are indexed simply by the
     * order in the current document.
     */
    public Element getState( int index )
    {
       return getChildElements( rootElement ).get( index );
    }

    /**
     * Re-get a state from the store. Re-getting implies that this
     * will be overwritten so all children are removed.
     */
    public Element reGetState( int index )
    {
        Element parent = getChildElements( rootElement ).get( index );
        List<Element> children = getChildElements( parent );
        int size = children.size();
        for ( int i = 0; i < size; i++ ) {
            parent.removeChild( children.get( i ) );
        }
        return parent;
    }

    /**
     * Get the description of a state by index.
     */
    public String getDescription( int index )
    {
        return getState( index ).getAttribute( "description" );
    }

    /**
     * Set the description of a state by index.
     */
    public void setDescription( int index, String value )
    {
        getState( index ).getAttributeNode( "description" ).setValue(value);
        fireTableRowsUpdated( index, index );
    }

    /**
     * Get the date stamp of a state by index.
     */
    public String getDateStamp( int index )
    {
        return getState( index ).getAttribute( "date-stamp" );
    }

    /**
     * Set the date stamp of a state by index. Updates to
     * representation of the current time.
     */
    public void setDateStamp( int index )
    {
        getState( index ).getAttributeNode( "date-stamp" ).
            setValue( new Date().toString() );
        fireTableRowsUpdated( index, index );
    }

    /**
     * Add a new state root in a given Element. The Element should be
     * created by the newState method.
     */
    public void stateCompleted( Element newState )
    {
        rootElement.appendChild( newState );
        fireTableDataChanged();
    }

    /**
     * Create a new Element ready for attaching a configuration state
     * to (i.e. get this Element then write the configuration data
     * attached to it). When the new configuration is completed invoke
     * the stateCompleted method, the configuration will not be part
     * of the structure until then.
     */
    public Element newState( String elementName, String description )
    {
        Element newRoot = document.createElement( elementName );
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
        rootElement.removeChild( child );
        fireTableRowsDeleted( index, index );
    }

    /**
     * Save the Document to backing store.
     */
    public void writeToBackingStore()
    {
        if ( applicationName == null || storeName == null ) {
            return;
        }
        File backingStore = StoreFileUtils.getConfigFile( applicationName,
                                                          storeName );
        FileOutputStream f = null;
        try {
            f = new FileOutputStream( backingStore );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }
        StreamResult out = new StreamResult( f );
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
        }
        catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return;
        }

        //?? User can type in funny characters??
        t.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
        t.setOutputProperty( OutputKeys.INDENT, "yes" );
        t.setOutputProperty( OutputKeys.STANDALONE, "yes" );
        try {
            t.transform( new DOMSource( document ), out );
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
