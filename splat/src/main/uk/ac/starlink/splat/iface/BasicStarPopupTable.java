package uk.ac.starlink.splat.iface;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;

public class BasicStarPopupTable extends StarJTable {
	
	CopyListener copylistener = new CopyListener();
	final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);

	public BasicStarPopupTable() {
		super(true);
		initTable();
	}
	
	public BasicStarPopupTable(boolean rowHeader) {
		super(rowHeader);
		 initTable();
		
	}
	public BasicStarPopupTable( StarTable startable, boolean rowHeader ) {
        
        super(startable, rowHeader);
        initTable();
    }
   
	private void initTable() {
	       registerKeyboardAction(copylistener, "Copy", stroke, JComponent.WHEN_FOCUSED); // allow copying cell content to clipboard
	     
	}
	
	
	  public Point getPopupLocation(MouseEvent event) {
	       setPopupTriggerLocation(event);     
	       return super.getPopupLocation(event);
	   }
	   
	   protected void setPopupTriggerLocation(MouseEvent event) {
	       putClientProperty("popupTriggerLocation", 
	               event != null ? event.getPoint() : null);
	   }

	   public int getPopupRow () {
	       int row = rowAtPoint( (Point) getClientProperty("popupTriggerLocation") );
	       return convertRowIndexToModel(row);
	   }
	   
	   /**
	    *  copy cell content to clipboard
	    **/
	     
	     private void copyCell() {
	         int col = getSelectedColumn();
	         int row = getSelectedRow();
	         if (col != -1 && row != -1) {
	             Object value = getValueAt(row, col);
	             String cellContent="";
	             if (value != null) {
	                 cellContent = value.toString();
	             }

	             final StringSelection selection = new StringSelection(cellContent);     
	             final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	             clipboard.setContents(selection, selection);
	             
	         }
	     }
	     
	    //Action to copy cell content to clipboard   
	     class CopyListener implements ActionListener {
	         public void actionPerformed(ActionEvent event) {
	             copyCell();
	         }   
	     }
	
}
