/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     22-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import java.io.File;
import java.io.FileNotFoundException;

import org.w3c.dom.Element;

import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;

/**
 * Manage a list of photometry measurement objects. It provides a
 * concept of a "current" object and can be saved and restored from
 * {@link Element} (and thus to and from an XML representation).
 * <p>
 * A user of an object can be informed when the current object is
 * changed by registering for ChangeEvents.
 * <p>
 * Methods are provided that duplicate those that would be required
 * for the implementation of this class as an AbstractTableModel. This
 * should make it easy to proxy this class as a TableModel
 * ({@link PhotomListTableModel}).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhotomList
    extends PrimitiveXMLEncodeDecode
    implements ChangeListener
{
    /** Index of the "current" measurement */
    private int index = -1;                 // means none or rewound

    /** The list of references to the measurement objects */
    private ArrayList list = new ArrayList();

    /** The class of objects that are stored, these must all be the same */
    private Class theClass = null;

    /** If building a list of BasePhotom objects up defer firing
     *  events until complete 
     */
    private boolean building = false;

    /**
     * Create an instance.
     */
    public PhotomList()
    {
        //  Nothing to do.
    }

    /**
     * Add a BasePhotom object to the list. Returns the index.
     * Throws an ArrayStoreException if the object is of a type
     * different to those already added to this list. The new object
     * becomes the current one.
     */
    protected int add( BasePhotom basePhotom )
    {
        index++;
        list.add( index, basePhotom );
        if ( theClass == null ) {
            theClass = basePhotom.getClass();
        }
        else {
            if (! theClass.getName().equals(basePhotom.getClass().getName())) {
                throw new ArrayStoreException
                    ("Attempt to store photometry object of differing class");
            }
        }

        // We need to know when changes are made so that we can inform
        // our listeners.
        basePhotom.addChangeListener( this );
        setCurrent( index );
        fireChanged();
        return index;
    }

    /**
     * Add a list of BasePhotom objects to the list.
     */
    public void add( Collection list )
    {
        Iterator i = list.iterator();
        building = true;
        while ( i.hasNext() ) {
            add( (BasePhotom) i.next() );
        }
        enableChangeListeners();
        building = false;
        fireChanged();
    }

    /**
     * Make sure we're a ChangeListener for all the BasePhotom's.
     */
    protected void enableChangeListeners()
    {
        int size = list.size();
        if ( size > 0 ) {
            BasePhotom current;
            for ( int i = 0; i < size; i++ ) {
                current = (BasePhotom) list.get( i );

                // Make sure that we are not listening twice.
                current.removeChangeListener( this ); 
                current.addChangeListener( this );
            }
        }
    }

    /**
     * Stop being a ChangeListener for all the BasePhotom's.
     */
    protected void disableChangeListeners()
    {
        int size = list.size();
        if ( size > 0 ) {
            BasePhotom current;
            for ( int i = 0; i < size; i++ ) {
                current = (BasePhotom) list.get( i );
                current.removeChangeListener( this ); 
            }
        }
    }

    /**
     * Remove a BasePhotom object from the list.
     */
    public boolean remove( BasePhotom basePhotom )
    {
        int index = list.indexOf( basePhotom );
        if ( index == -1 ) {
            return false;
        }
        return remove( index );
    }

    /**
     * Remove a BasePhotom object from the list.
     */
    public boolean remove( int index )
    {
        try {
            list.remove( index );
        }
        catch (Exception e) {
            return false;
        }
        if ( this.index == index ) {
            this.index = -1;
        }
        fireChanged();
        return true;
    }

    /**
     * Get the number of BasePhotom objects available.
     */
    public int size()
    {
        return list.size();
    }

    /**
     * Set the current BasePhotom object. Truncates to the ends if an
     * attempt is made to step past them.
     */
    public void setCurrent( int index )
    {
        int oldindex = this.index;
        if ( index < 0 ) {
            this.index = 0;
        }
        else if ( index > list.size() ) {
            this.index = list.size() - 1;
        }
        else {
            this.index = index;
        }

        //  Only issue change if it has (classic pattern to short
        //  circuit any listener cycles).
        if ( index != oldindex ) {
            fireChanged();
        }
    }

    /**
     * Get the current BasePhotom object (or sub-class).
     */
    public BasePhotom getCurrent()
    {
        if ( index != -1 && index < list.size() ) {
            return (BasePhotom) list.get( index );
        }
        return null;
    }

    /**
     * Get a specific BasePhotom object (or sub-class). If the index
     * is out of the range an IndexOutOfBoundsException will be thrown.
     */
    public BasePhotom get( int index )
    {
        return (BasePhotom) list.get( index );
    }

    /**
     * Get the index of a object that is stored in the list. Returns
     * -1 if not there.
     */
    public int indexOf( Object object )
    {
        return list.indexOf( object );
    }

    //
    // Implement AbstractTableModel interface so that this class can
    // be easily proxied in some TableModel.
    //
    public int getRowCount()
    {
        return size();
    }

    public int getColumnCount()
    {
        // If no rows are entered yet just use a BasePhotom object.
        if ( size() == 0 ) {
            return BasePhotom.getNumberValues();
        }

        // Use the object type-specific count.
        return ( (BasePhotom) get( 0 ) ).getSpecificNumberValues();
    }

    public Object getValueAt( int row, int column )
    {
        BasePhotom basePhotom = (BasePhotom) get( row );
        return basePhotom.getValue( column );
    }

    /**
     *  Return the column name.
     */
    public String getColumnName( int index )
    {
        if ( size() == 0 ) {
            return BasePhotom.getDescription( index );
        }

        // Use the object type-specific count.
        return ( (BasePhotom) get( 0 ) ).getSpecificDescription( index );
    }

    //
    // Implement the ChangeListener interface
    //
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Registers a listener who wants to be informed about changes.
     *
     *  @param l the ChangeListener listener.
     */
    public void addChangeListener( ChangeListener l )
    {
        listeners.add( ChangeListener.class, l );
    }

    /**
     * De-registers a listener for changes.
     *
     *  @param l the ChangeListener listener.
     */
    public void removeChangeListener( ChangeListener l )
    {
        listeners.remove( ChangeListener.class, l );
    }

    /**
     * Send ChangeEvent event to all listeners.
     */
    protected void fireChanged()
    {
        // When building lists these are suppressed until completion.
        if ( ! building ) {
            Object[] la = listeners.getListenerList();
            ChangeEvent e = null;
            for ( int i = la.length - 2; i >= 0; i -= 2 ) {
                if ( la[i] == ChangeListener.class ) {
                    if ( e == null ) {
                        e = new ChangeEvent( this );
                    }
                    ((ChangeListener)la[i+1]).stateChanged( e );
                }
            }
        }
    }

    //
    // Input and output facilities.
    //
    public void write( File file )
    {
        try {
            PhotomEncoderAndDecoder.save( this, file );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Inverse of above...
    public void read( File file )
    {
        if ( file.exists() && file.canRead() ) {
            //  Reset current objects.
            ArrayList oldList = list;
            int oldIndex = index;
            list = new ArrayList();
            index = -1;
            building = true;

            //  Parse that file
            try {
                PhotomEncoderAndDecoder.read( file, this );
            }
            catch (Exception e) {
                e.printStackTrace();

                // Restore original list.
                list = oldList;
                index = oldIndex;
            }
            oldList = null;

            // When finished...
            building = false;
            fireChanged();
        }
    }

    //
    // Implement the XMLEncodeDecode interface so that this object
    // can be stored and restored in an Element (and consequently XML).
    //
    public void encode( Element rootElement )
    {
        int size = list.size();
        if ( size > 0 ) {
            //  Record the class of object.
            addChildElement( rootElement, "class", theClass.getName() );

            // Need to encode the ArrayList of BasePhotom objects.
            Element localRoot = addChildElement( rootElement, "list" );
            BasePhotom current;
            for ( int i = 0; i < size; i++ ) {
                current = (BasePhotom) list.get( i );
                current.encode( addChildElement( localRoot,
                                                 current.getTagName() ) );
            }
        }
    }

    public void decode( Element rootElement )
    {
        // Only two children are expected, class followed by
        // list. Look for class as we always need this before list.
        List children = getChildElements( rootElement );
        int size = children.size();
        Element element = null;
        Element classElement = null;
        Element listElement = null;
        String name = null;
        for ( int i = 0; i < size; i++ ) {
            element = (Element) children.get( i );
            name = getElementName( element );
            if ( name.equals( "class" ) ) {
                classElement = element;
            }
            else if ( name.equals( "list" ) ) {
                listElement = element;
            }
        }
        if ( classElement == null ) {
            throw new IllegalArgumentException
                ( "no class definition in PhotomList root element" );
        }
        if ( listElement == null ) {
            throw new IllegalArgumentException
                ( "no objects defined in PhotomList root element" );
        }
        try {
            theClass = this.getClass().forName( getElementValue( classElement ) );
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException
                ( "Cannot find class: " + getElementValue( classElement ) );
        }

        //  Now visit each element in the list and create a new object
        //  for it. Existing objects in the list are lost.
        children = getChildElements( listElement );
        size = children.size();
        if ( size > 0 ) {
            //  Object creation is by reflection since we don't
            //  know exactly what type they are...
            BasePhotom basePhotom = null;
            try {
                basePhotom = (BasePhotom) theClass.newInstance();
            }
            catch (Exception e) {
                throw new IllegalArgumentException
                    ( "Cannot create instances of class: " +
                      getElementValue( element ) );
            }

            list = new ArrayList();
            index = -1;
            BasePhotom copy = null;
            building = true;
            for ( int i = 0; i < size; i++ ) {
                element = (Element) children.get( i );
                copy = (BasePhotom) basePhotom.clone();
                copy.decode( element );
                add( copy );
            }
            building = false;
            fireChanged();
        }
    }

    // Not used for this implementation (only good for flat name,
    // value pair encoding).
    public void setFromString( String name, String value )
    {
        // Does nothing and shouldn't be called.
    }

    public String getTagName()
    {
        return "photomlist";
    }

    //
    // Implement ChangeListener interface. Each object on the list
    // invokes this. They should not do this if they are not really
    // changed (no speculative ChangeEvents as we want to avoid
    // listener loops).
    //
    public void stateChanged( ChangeEvent e )
    {
        fireChanged();
    }
}
