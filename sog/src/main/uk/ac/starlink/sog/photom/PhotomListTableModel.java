/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-NOV-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * Creates a TableModel view of a given PhotomList.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhotomListTableModel
    extends AbstractTableModel
    implements ChangeListener
{
    /** The PhotomList we're offering a TableModel for */
    private PhotomList list = null;

    /**
     * Create an instance.
     */
    public PhotomListTableModel( PhotomList list )
    {
        super();
        setPhotomList( list );
    }

    /**
     * Set the PhotomList. If null then any previous PhotomList is
     * removed.
     */
    public void setPhotomList( PhotomList list )
    {
        if ( this.list != null ) {
            this.list.removeChangeListener( this );
        }
        this.list = list;
        if ( list != null ) {
            list.addChangeListener( this );
        }
    }

    /**
     * Get the PhotomList.
     */
    public PhotomList getPhotomList()
    {
        return list;
    }
    
    // 
    // Implementation for parts of AbstractTableModel. Just pass on
    // to the PhotomList.
    //
    public int getRowCount()
    {
        return list.getRowCount();
    }

    public int getColumnCount()
    {
        return list.getColumnCount();
    }

    public Object getValueAt( int row, int column )
    {
        return list.getValueAt( row, column );
    }

    public String getColumnName( int index )
    {
        return list.getColumnName( index );
    }

    //
    // Implement the ChangeListener for events in the PhotomList.
    //
    public void stateChanged( ChangeEvent e )
    {
        //  Update table. Don't know what is changed so make it complete.
        fireTableStructureChanged();
        fireTableDataChanged();
    }
}

