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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.accessibility.AccessibleComponent;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.plaf.ToolTipUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import jsky.util.Logger;
import uk.ac.starlink.splat.iface.BasicStarPopupTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableModel;
import uk.ac.starlink.util.TemporaryFileDataSource;
import uk.ac.starlink.util.gui.ArrayTableSorter;

public class StarPopupTable extends  BasicStarPopupTable  {
 
  
    //final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    
    // tells if there are soda or datalink parameters associated to this results table
	private boolean hasSodaService = false;
	private boolean hasDatalinkService = false;
	private int previewcol=-1;
	
    
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
      // registerKeyboardAction(copylistener, "Copy", stroke, JComponent.WHEN_FOCUSED); // allow copying cell content to clipboard
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

         //  if (model.getColumnName(c).equalsIgnoreCase("preview")) {
           //    try {
           //        previewcol=c;
               //    URL url =new URL(val);
               //    BufferedImage preview = ImageIO.read(url);
                //   previewIcon=new ImageIcon(preview);
                  
             // } catch (MalformedURLException e) {
                   // TODO Auto-generated catch block
                   //e.printStackTrace();
            //  } catch (IOException e) {
             //      // TODO Auto-generated catch block
                   //e.printStackTrace();
           //    }
        //   }
           
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

   
 
   
   /** 
    * If the table has different rows with same pubdid (probably different formats of same spectrum) 
    * put them to only one row with an option for different formats
    **/
   
   public void makeUniquePubdid() {

	   StarTable startable = this.getStarTable();
//	   ColumnModel cmodel = this.getColumnModel();
	   int cols = startable.getColumnCount();
	   int pubdidcol=-1;
	   int formatcol=-1;
	   
	   
	   // find the right column
	   //
	   for (int i=0;i<cols;i++) {
		   // SSAP
		   ColumnInfo ci =  startable.getColumnInfo(i);
		   if (ci != null) {
			   try { 
				   if (ci.getUtype().toLowerCase().endsWith("curation.publisherdid") ) {
					   pubdidcol=i;
					   Logger.info(this, "pubdidcol1 "+i);
				   }
				   if (ci.getUtype().toLowerCase().endsWith("access.format") ) {
					   formatcol=i;
					   Logger.info(this, "formatcol1 "+i);
				   }
			   } catch( Exception e) {}
			   // Obscore
			   try { 
				   
				   if (ci.getName().endsWith("obs_publisher_did") ) {
					   pubdidcol=i;
					   Logger.info(this, "pubdidcol2 "+i);
				   }
				   if (ci.getName().endsWith("access_format") ) {
					   formatcol=i;
					   Logger.info(this, "formatcol2 "+i);
				   }
			   } catch( Exception e){}
		   }
	   }
	   if (pubdidcol == -1 || formatcol == -1)
		   return; // nothing can be done
	   
	
	   // sort by pubdid
	   //
	   
	   TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(this.getModel());
	   this.setRowSorter(sorter);
	   List<RowSorter.SortKey> sortKeys = new ArrayList<SortKey>();
	   sortKeys.add(new RowSorter.SortKey(pubdidcol, SortOrder.ASCENDING));	    
	   sorter.setSortKeys(sortKeys);
	   sorter.sort();
	   int rowCount = this.getRowCount();
	   // search for same pubdids
	   String prevPubdid="", prevFormat= "";
	try {
		prevPubdid = getStringValue(startable.getCell(0, pubdidcol));
		prevFormat= getStringValue(startable.getCell(0, formatcol));
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	   String prevPubdid2= getStringValue(this.getValueAt(0, pubdidcol));
	   String prevFormat2= getStringValue(this.getValueAt(0, formatcol));
	   
	   for(int i=1; i<rowCount;i++){
		   String pubdid = "";
		    String format = "";
		   try {
				pubdid = getStringValue(startable.getCell(0, pubdidcol));
				format= getStringValue(startable.getCell(0, formatcol));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		 
		    if (pubdid != null && pubdid.equals(prevPubdid)) {
		    	if (isPreferedFormat(format,prevFormat)) {
		    		this.removeRowSelectionInterval(i-1, i-1);
		    		prevFormat=format;
		    	} else {
		    		this.removeRowSelectionInterval(i, i);
		    	}
		    } else {
		    	prevPubdid=pubdid;
		    	prevFormat=format;
		    }
		}	   
   }
   
  

  
     

   private String getStringValue(Object value) {
	if (value != null)
		return value.toString();
	return null;
}

/** 
    * Using a predefined preference order, returns
    * 	true if format1 is prefered than format2
    *   false otherwise
    **/
private boolean isPreferedFormat(String format1, String format2){
	// list of preferred formats in descending order
	 ArrayList<String> pref = new ArrayList<String>(Arrays.asList(
             "votable", 
             "fits"
             ));
	 int rank1=pref.size(), rank2=pref.size();
	 for (int i=1;i<pref.size();i++) {
		 if (format1.toLowerCase().contains(pref.get(i))) {
			 rank1=i;
		 }	
		 if (format2.toLowerCase().contains(pref.get(i))) {
			 rank2=i;
		 }	
	 }
	 return (rank1<rank2);
		 
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
   

   public void setSoda() { // this table has soda services
	   hasSodaService=true;
	
   }
   
   public void setDataLink() { // this table has Datalink services
	   hasDatalinkService=true;
	
   }

   
   
   public  String getDataLinkID(int row, String field) { // get the datalink ID
	   if (! hasDatalinkService) 
		   return null;
	   
	   StarTable table = this.getStarTable();

	   for (int i=0;i<table.getColumnCount();i++) {
		   ColumnInfo ci =  table.getColumnInfo(i);
		   DescribedValue idValue = ci.getAuxDatumByName("ID");
		   if (idValue == null)
			   idValue = ci.getAuxDatumByName("VOTable ID");
		   String val="";
		   if (idValue != null) 
			   val = idValue.getValueAsString(field.length()+1);
		   if (val.equals(field)) {
			   try {
				   return  (String) table.getCell(row, i);
			   } catch (IOException e) {}							
		   }  
	   }

	   return null; 

   }
   
   
   
   public  String getAccessFormat(int row) { // get the access format value
	
	   
	   StarTable table = this.getStarTable();

	   for (int i=0;i<table.getColumnCount();i++) {
		   ColumnInfo ci =  table.getColumnInfo(i);
		   String name = ci.getName();
		   String utype = ci.getUtype();
		   name = name.toLowerCase();
		   
		   if ((name != null && name.toLowerCase().endsWith("access_format")) || (utype != null && utype.toLowerCase().endsWith("access.format"))) {
			   try {
				   return  (String) table.getCell(row, i);
			   } catch (IOException e) {}											     	    		   
		   }    	    	  		   
	   }
	   return ""; 
   }
   

   public  String getAccessURL(int row) { // get the access format value
	  
	   StarTable table = this.getStarTable();

	   for (int i=0;i<table.getColumnCount();i++) {
		   ColumnInfo ci =  table.getColumnInfo(i);
		   String name = ci.getName();
		   String utype = ci.getUtype();
		   if ((name != null && name.toLowerCase().endsWith("access_url")) || (utype != null && utype.toLowerCase().endsWith("access.reference"))) {
			   try {
				   return  (String) table.getCell(row, i);
			   } catch (IOException e) {}											     	    		   
		   }    	    	  
	   }

	   return null; 
   }
   
   
     
   public boolean hasSodaService() { // this table has soda services
	   return hasSodaService;
	
   }
   
   public boolean hasDataLinkService() { // this table has DataLink services
	   return hasDatalinkService;
	
   }
   
   public boolean rowsHaveDataLinkFormat() { // this table's access_urls are datalink	
	   String format = getAccessFormat(0);
	   if (format != null)
		   return format.contains("datalink");
	   return false;
	
   }
   
   
   public int getpreviewcol(TableModel model) {
	     previewcol=-1;
	     for ( int c=0; c< model.getColumnCount(); c++ ) {
	    	 if (model.getColumnName(c).equalsIgnoreCase("preview")) {
	    		 previewcol=c;
	    	 }
	     }	   
		 return previewcol;	    
   }
   
   public String getToolTipText(MouseEvent e) {
       String tip = null;
              
       java.awt.Point p = e.getPoint();
       int rowIndex = convertRowIndexToModel(rowAtPoint(p));
     
 
       TableModel model = this.getModel();
 	   previewcol = getpreviewcol(model);
	   if (previewcol > 0) {	
			String prvUrl = (String) model.getValueAt(rowIndex,previewcol );
			if (prvUrl != null && !prvUrl.isEmpty())
		      tip =  "<html><body><img src='" +prvUrl + "' width=200px height=auto ></body></html>";
 
	   }
       return tip;
   }

}




