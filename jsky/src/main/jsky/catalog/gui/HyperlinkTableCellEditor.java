/*
 * ESO Archive
 *
 * $Id: HyperlinkTableCellEditor.java,v 1.5 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/23  Created
 */

package jsky.catalog.gui;


import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import jsky.catalog.*;
import jsky.util.*;
import jsky.util.gui.DialogUtil;


/**
 * Used to display a button in a table cell.
 * Pressing the button should follow the field's URL and display the results in the
 * appropriate way.
 *
 * @see HyperlinkTableCellRenderer
 */
public class HyperlinkTableCellEditor extends JButton
        implements TableCellEditor, ActionListener {

    /** The table field corresponding to this cell */
    protected FieldDesc field;

    /** The table containg the field */
    protected TableQueryResult tableQueryResult;

    /** The value in the cell */
    protected Object value;

    /** The table row containing this button */
    protected int row;

    /** Used to display query results found by following the link */
    QueryResultDisplay queryResultDisplay;


    /**
     * Create a JTable cell renderer for columns containing a URL.
     * The URL string may contain column name variables of the form
     * $COLNAME or ${COLNAME}, which are substituted with the values in
     * the named columns.
     *
     * @param field object representing a field (a table column description) in the table query result.
     * @param tableQueryResult contains the table data
     */
    public HyperlinkTableCellEditor(FieldDesc field, TableQueryResult tableQueryResult,
                                    QueryResultDisplay queryResultDisplay) {
        this.field = field;
        this.tableQueryResult = tableQueryResult;
        this.queryResultDisplay = queryResultDisplay;
        addActionListener(this);
        setHorizontalAlignment(LEFT);
    }


    /**
     * Called when a button in the table is pressed
     */
    public void actionPerformed(ActionEvent ev) {
        // get the query result and display it
        try {
            QueryResult queryResult = field.getLinkValue(tableQueryResult, value, row);
            queryResultDisplay.setQueryResult(queryResult);
        }
        catch (Exception e) {
            if (getText().length() != 0)
                DialogUtil.error(e);
        }
    }


    /**
     *  Sets an initial <I>value</I> for the editor.  This will cause
     *  the editor to stopEditing and lose any partially edited value
     *  if the editor is editing when this method is called. <p>
     *
     *  Returns the component that should be added to the client's
     *  Component hierarchy.  Once installed in the client's hierarchy
     *  this component will then be able to draw and receive user input.
     *
     * @param	table		the JTable that is asking the editor to edit
     *				This parameter can be null.
     * @param	value		the value of the cell to be edited.  It is
     *				up to the specific editor to interpret
     *				and draw the value.  eg. if value is the
     *				String "true", it could be rendered as a
     *				string or it could be rendered as a check
     *				box that is checked.  null is a valid value.
     * @param	isSelected	true is the cell is to be renderer with
     *				selection highlighting
     * @param	row     	the row of the cell being edited
     * @param	column  	the column of the cell being edited
     * @return	the component for editing
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        this.row = row;
        this.value = value;

        try {
            field.getLinkValue(tableQueryResult, value, row); // just a test
            setText(field.getLinkText(tableQueryResult, value, row, column));
        }
        catch (Exception e) {
            setText("");
        }

        return this;
    }


    /** Returns the value contained in the editor**/
    public Object getCellEditorValue() {
        return value;
    }


    /**
     * Ask the editor if it can start editing using <I>anEvent</I>.
     * <I>anEvent</I> is in the invoking component coordinate system.
     * The editor can not assume the Component returned by
     * getCellEditorComponent() is installed.  This method is intended
     * for the use of client to avoid the cost of setting up and installing
     * the editor component if editing is not possible.
     * If editing can be started this method returns true.
     *
     * @param	anEvent		the event the editor should use to consider
     *				whether to begin editing or not.
     * @return	true if editing can be started.
     * @see #shouldSelectCell
     */
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }


    /**
     * Tell the editor to start editing using <I>anEvent</I>.  It is
     * up to the editor if it want to start editing in different states
     * depending on the exact type of <I>anEvent</I>.  For example, with
     * a text field editor, if the event is a mouse event the editor
     * might start editing with the cursor at the clicked point.  If
     * the event is a keyboard event, it might want replace the value
     * of the text field with that first key, etc.  <I>anEvent</I>
     * is in the invoking component's coordinate system.  A null value
     * is a valid parameter for <I>anEvent</I>, and it is up to the editor
     * to determine what is the default starting state.  For example,
     * a text field editor might want to select all the text and start
     * editing if <I>anEvent</I> is null.  The editor can assume
     * the Component returned by getCellEditorComponent() is properly
     * installed in the clients Component hierarchy before this method is
     * called. <p>
     *
     * The return value of shouldSelectCell() is a boolean indicating whether
     * the editing cell should be selected or not.  Typically, the return
     * value is true, because is most cases the editing cell should be
     * selected.  However, it is useful to return false to keep the selection
     * from changing for some types of edits.  eg. A table that contains
     * a column of check boxes, the user might want to be able to change
     * those checkboxes without altering the selection.  (See Netscape
     * Communicator for just such an example)  Of course, it is up to
     * the client of the editor to use the return value, but it doesn't
     * need to if it doesn't want to.
     *
     * @param	anEvent		the event the editor should use to start
     *				editing.
     * @return	true if the editor would like the editing cell to be selected
     * @see #isCellEditable
     */
    public boolean shouldSelectCell(EventObject anEvent) {
        return (anEvent instanceof MouseEvent);
    }


    /**
     * Tell the editor to stop editing and accept any partially edited
     * value as the value of the editor.  The editor returns false if
     * editing was not stopped, useful for editors which validates and
     * can not accept invalid entries.
     *
     * @return	true if editing was stopped
     */
    public boolean stopCellEditing() {
        return true;
    }

    /**
     * Tell the editor to cancel editing and not accept any partially
     * edited value.
     */
    public void cancelCellEditing() {
    }

    /**
     * Add a listener to the list that's notified when the editor starts,
     * stops, or cancels editing.
     *
     * @param	l		the CellEditorListener
     */
    public void addCellEditorListener(CellEditorListener l) {
    }

    /**
     * Remove a listener from the list that's notified
     *
     * @param	l		the CellEditorListener
     */
    public void removeCellEditorListener(CellEditorListener l) {
    }

    // The default table renderers define validate, revalidate, repaint,
    // and  firePropertyChange to be noops
    public void validate() {
    }

    public void revalidate() {
    }

    public void repaint(Rectangle r) {
    }

    public void repaint(long tm, int x, int y, int width, int height) {
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}

