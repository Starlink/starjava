//=== File Prolog =============================================================
//	This code was adapted by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Development History -----------------------------------------------------
//
//	06/15/00	S. Grosvenor / 588 Booz-Allen
//		Original implementation
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//=== End File Prolog====================================================================

package jsky.util.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import jsky.science.Wavelength;
import jsky.science.Wavelength1DModel;
import jsky.util.ReplaceablePropertyChangeListener;
import jsky.util.ReplacementEvent;

/**
 * Adapter class the implements the TableModel interface around a Wavelength1DModel
 *
 */
public class Wavelength1DTableModel implements TableModel, ReplaceablePropertyChangeListener {

    private Wavelength1DModel fModel;
    private double fWavelengths[];
    private double fValues[];

    public Wavelength1DTableModel(Wavelength1DModel model) {
        setModel(model);
    }

    public void setModel(Wavelength1DModel model) {
        if (fModel != null) fModel.removePropertyChangeListener(this);
        fModel = model;
        if (fModel != null) {
            fModel.addPropertyChangeListener(this);
            fWavelengths = model.toArrayWavelengths(null, null, 0);
            fValues = model.toArrayData(null, null, 0);
        }
        else {
            fWavelengths = null;
            fValues = null;
        }
        fireTableDataChanged();
    }

    public Wavelength1DModel getModel() {
        return fModel;
    }

    /*
     * Returns the lowest common denominator Class in the column.
    */
    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return Wavelength.class;
        else
            return Double.class;
    }

    /*
     * Returns the number of columns managed by the data source object.
    */
    public int getColumnCount() {
        return 2;
    }

    /*
     * Returns the name of the column at columnIndex.
    */
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Wavelength";
        else
            return "Value";
    }

    /*
     * Returns the number of records managed by the data source object.
    */
    public int getRowCount() {
        return fWavelengths.length;
    }

    /*
     * Returns an attribute value for the cell at columnIndex and rowIndex.
    */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0)
            return new Wavelength(fWavelengths[rowIndex]);
        else
            return new Double(fValues[rowIndex]);
    }

    /*
     * Returns true if the cell at rowIndex and columnIndex is editable.
    */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // not yet anyway
    }

    /*
     * Sets an attribute value for the record in the cell at columnIndex and rowIndex.
    */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // do nothing
    }

    // ===
    // much of the following code is taken direct from javax.swing.table.AbstractTableModel
    // ===

    private transient TableModelListener[] fTableListeners = null;

    /*
     * Add a listener to the list that's notified each time a change to the data model occurs.
    */
    public void addTableModelListener(TableModelListener listener) {
        synchronized (fModel) {
            TableModelListener[] els = null;
            if (fTableListeners != null) {
                for (int i = 0; i < fTableListeners.length; i++) {
                    if (fTableListeners[i] == listener) return; // already have it, don't add it
                }
                int length = fTableListeners.length;
                els = new TableModelListener[length + 1];
                System.arraycopy(fTableListeners, 0, els, 0, length);
            }
            else {
                els = new TableModelListener[1];
            }
            els[els.length - 1] = listener;
            fTableListeners = els;
        }
    }

    /*
     * Remove a listener from the list that's notified each time a change to the data model occurs.
    */
    public void removeTableModelListener(TableModelListener listener) {
        if (fTableListeners == null) return;
        synchronized (fModel) {
            int length = fTableListeners.length;
            for (int i = 0; i < fTableListeners.length; i++) if (fTableListeners[i] == listener) length--;
            if (length == 0) {
                fTableListeners = null;
                return;
            }
            TableModelListener[] els = new TableModelListener[length];
            int nexti = 0;
            for (int i = 0; i < fTableListeners.length; i++) {
                if (fTableListeners[i] != listener) {
                    els[nexti++] = fTableListeners[i];
                }
            }
            fTableListeners = els;
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        fireTableDataChanged();
    }

    public void replaceObject(ReplacementEvent event) {
        if (event.getOldValue() == fModel) {
            setModel((Wavelength1DModel) event.getNewValue());
        }
    }

    /**
     * Forward the given notification event to all TableModelListeners that registered
     * themselves as listeners for this table model.
     * @see #addTableModelListener
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableChanged(TableModelEvent evt) {
        if (fTableListeners == null) return;

        TableModelListener[] targets = fTableListeners;
        for (int i = 0; i < targets.length; i++) {
            targets[i].tableChanged(evt);
        }
    }

    /**
     * Notify all listeners that all cell values in the table's rows may have changed.
     * The number of rows may also have changed and the JTable should redraw the
     * table from scratch. The structure of the table, ie. the order of the
     * columns is assumed to be the same.
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this));
    }

    /**
     * Notify all listeners that the value of the cell at (row, column)
     * has been updated.
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableCellUpdated(int row, int column) {
        fireTableChanged(new TableModelEvent(this, row, row, column));
    }


}
