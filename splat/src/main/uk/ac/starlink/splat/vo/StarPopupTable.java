package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
/**
 *  Extension of StarJTable that supports row popup menus,
 *  copying contents of a table cell to the clipboard
 *  and table column sorting
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
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jsky.util.Logger;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableModel;
import uk.ac.starlink.util.TemporaryFileDataSource;

public class StarPopupTable extends  StarJTable {
 
    CopyListener copylistener = new CopyListener();
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
    //final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    
    public StarPopupTable() {
        super(true);
        initTable();
    }
    
    public StarPopupTable( boolean rowHeader) {
        
        super(rowHeader);
        initTable();
    }
    
   public StarPopupTable( StarTable startable, boolean rowHeader ) {
        
        super(startable, rowHeader);
        initTable();
    }
   
   private void initTable() {
       registerKeyboardAction(copylistener, "Copy", stroke, JComponent.WHEN_FOCUSED); // allow copying cell content to clipboard
       setAutoCreateRowSorter(true); // allow sorting by column
   }
   
   /*
    * Opens a window showing all the information contained in row r
    */
   protected void showInfo(int r) {
       
       String info = "<HTML><TABLE border=0 valign=\"top\">";
       StarTableModel model = (StarTableModel) this.getModel();
       ImageIcon previewIcon = null;
      
       for ( int c=0; c< model.getColumnCount(); c++) {
           String val = "";
           try {
               Object obj=model.getValueAt(r, c);
               Class<? extends Object> objclass = obj.getClass();
               if (objclass.isArray() && objclass.getComponentType().isPrimitive()) {
                   int len = Array.getLength(obj);
                   List<Object> l = new ArrayList<Object>();
                   for (int i=0;i<len;i++)
                       l.add(Array.get(obj, i));

                   val = l.toString();
               }
               else 
                   val=obj.toString();
               if (val.contains("<")  || val.contains("<")) {
                   val = val.replace("<", "&lt;");
                   val = val.replace(">", "&gt;");
               }
           } catch (Exception e) {
               // do nothing, the value will be an empty string
               Logger.info("debug", e.getMessage());
           }

           if (model.getColumnName(c).equalsIgnoreCase("preview")) {
               try {
                 
                   URL url =new URL(val);
                   BufferedImage preview = ImageIO.read(url);
                   previewIcon=new ImageIcon(preview);
                  
               } catch (MalformedURLException e) {
                   // TODO Auto-generated catch block
                   //e.printStackTrace();
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   //e.printStackTrace();
               }
           }
           
           if ( c==0 && model.getColumnName(c).isEmpty())
               info += "<TR><TD col width= \"25%\"><B>Index:</B></TD><TD col width= \"70%\">"+val+"</TD></TR>";   
           else     
               info += "<TR><TD col width= \"25%\"><B>"+model.getColumnName(c)+":</B></TD><TD col width= \"70%\">"+val+"</TD></TR>";   
           
       }

       info += "</TABLE></HTML>";
       JTextPane infoPane =  new JTextPane();     
       infoPane.setContentType("text/html");      
       infoPane.setText(info);       
       infoPane.setPreferredSize(new Dimension(500,300));      
       JOptionPane.showMessageDialog(this, new JScrollPane(infoPane)," Spectrum Information", JOptionPane.PLAIN_MESSAGE, previewIcon);    
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
    * Display the columns in a pre-chosen order (by utype) 
    * (assuming that the utypes are properly defined)
    **/
   
   public void rearrangeSSAP() {
       // UCDs order :
       ArrayList<String> order = new ArrayList<String>(Arrays.asList(
               "char.spectralaxis.coverage.bounds.start", // spec start
               "char.spectralaxis.coverage.bounds.stop",  // spec stop 
               "char.spectralaxis.coverage.location.value", // spec start (option 2)
               "char.spectralaxis.coverage.bounds.extent",  // spec stop 
               "dataid.title", 
               "target.name", 
               "char.timeaxis.coverage.location.value", // time start
               "char.timeaxis.coverage.bounds.extent", //  time stop 
               "derived.snr", 
               "dataset.length", 
               "access.reference", 
               "access.format", 
               "access.size"
               ));
            /*              "query.score", //score
                          "target.name", // target name
                          "char.spatialaxis.coverage.location.value", //location                        
                          "char.spectralaxis.coverage.bounds.start", //spec_min
                          "char.spectralaxis.coverage.bounds.stop",  //spec_max
                          "char.spatialaxis.coverage.bounds.extent", // Aperture
                          "char.timeaxis.coverage.bounds.start", // dateObs
                          "char.timeaxis.coverage.bounds.stop", // dateObs
                          "access.format" //mime type
                          ));*/

     // find indices of this columns from DefaultColumnModel   
 
     int [] newOrder = {-1,-1,-1,-1,-1,-1,-1,-1, -1, -1, -1, -1, -1}; 
     
       StarTable startable = this.getStarTable();
       int cols = startable.getColumnCount();
      
       for (int i=0;i<cols;i++) {
           ColumnInfo ci =  startable.getColumnInfo(i);
          
           if (ci != null) {
               String utype = ci.getUtype();
               if (utype != null) {
                   utype=utype.toLowerCase();
                   for (int j=0;j<order.size();j++) {
                       if (utype.endsWith(order.get(j)))
                          newOrder[j]=i;
                   }
               }
           }
       }
 
       
       if (newOrder[0]!=-1 && newOrder[1]!=-1 && newOrder[2]!=-1 && newOrder[3]!=-1) {
           newOrder[2]=-1; // skip redundant information about wavelength range
           newOrder[3]=-1;
       }
       
       rearrange(newOrder);
   }
 
   
   public void rearrangeObsCore() {
       // UCDs order :
       ArrayList<String> order = new ArrayList<String>(Arrays.asList(
               "dataproduct_type", 
               "em_min", //spec start
               "em_max",  // spec stop 
               "obs_title", 
               "target_name", 
               "t_min", // time start
               "t_max", //  time stop 
               "t_exptime", 
               "access_format", 
               "access_estsize"
               ));
                         

     // find indices of this columns from DefaultColumnModel   
 
     int [] newOrder = {-1,-1,-1,-1,-1,-1,-1,-1, -1, -1}; 
     StarTable startable = this.getStarTable();
     int cols = startable.getColumnCount();
    
     for (int i=0;i<cols;i++) {
         ColumnInfo ci =  startable.getColumnInfo(i);
        
         if (ci != null) {
             String name = ci.getName();
             if (name != null) {
                 name=name.toLowerCase();
                 for (int j=0;j<order.size();j++) {
                     if (name.endsWith(order.get(j)))
                        newOrder[j]=i;
                 }
             }
         }
     }
  
     rearrange(newOrder);
   }       
   
   private void rearrange( int[] newOrder ) {
         
  
             
       TableColumnModel model = this.getColumnModel();
       int moved=1;
       for (int i=0; i<newOrder.length; i++) {
           int oldPos=newOrder[i];
           if (oldPos!=-1) {
               model.moveColumn(oldPos+1, moved++);
               for (int j=i+1;j<newOrder.length;j++)
                   if (newOrder[j] !=-1 && newOrder[j]<oldPos) // shift the indexes after move, if necessary
                       newOrder[j]++;                       
           }        
       }       
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
