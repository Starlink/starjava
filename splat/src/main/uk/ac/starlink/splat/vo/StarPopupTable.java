package uk.ac.starlink.splat.vo;

/**
 *  Extension of StarJTable that supports row popup menus
 *  and copying of a table cell to the clipboard
 *
 * @author Margarida Castro Neves
 */
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableModel;

public class StarPopupTable extends  StarJTable {
    
    CopyListener copylistener = new CopyListener();
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
    //final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    
    public StarPopupTable() {
        super(true);
        
        registerKeyboardAction(copylistener, "Copy", stroke, JComponent.WHEN_FOCUSED);
    }
    
    public StarPopupTable( boolean rowHeader) {
        
        super(rowHeader);
        registerKeyboardAction(copylistener, "Copy", stroke, JComponent.WHEN_FOCUSED);

    }
    
   public StarPopupTable( StarTable startable, boolean rowHeader ) {
        
        super(startable, rowHeader);
        registerKeyboardAction(copylistener, "Copy", stroke, JComponent.WHEN_FOCUSED);

    }
   
   protected void showInfo(int r) {
       
       String info = "<HTML><TABLE border=0 valign=\"top\">";
       StarTableModel model = (StarTableModel) this.getModel();
      
       for ( int c= 0; c< model.getColumnCount(); c++) {
           String val = "";
           try {
            val=model.getValueAt(r, c).toString();
            if (val.contains("<")  || val.contains("<")) {
              val = val.replace("<", "&lt;");
              val = val.replace(">", "&gt;");
            }
           } catch (Exception e) {
              // do nothing, the value will be an empty string
           }
           info += "<TR><TD col width= \"25%\"><B>"+model.getColumnName(c)+":</B></TD><TD col width= \"70%\">"+val+"</TD></TR>";            
       }

       info += "</TABLE></HTML>";
       
       JTextPane infoPane =  new JTextPane();
       infoPane.setContentType("text/html");
       infoPane.setText(info);       
       infoPane.setPreferredSize(new Dimension(500,300));      
       
       JOptionPane.showMessageDialog(this,
               new JScrollPane(infoPane)," Spectrum Information", JOptionPane.PLAIN_MESSAGE);
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
   class CopyListener implements ActionListener {
       public void actionPerformed(ActionEvent event) {
           copyCell();
       }   
   }

}
