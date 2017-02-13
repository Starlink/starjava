/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;

/**
 * ServerPopupTable
 * A RowPopupTable that represents a table of ssa service resources
 * 
 * @author mm
 *
 */
 public class ServerPopupTable extends RowPopupTable {

    
     private AbstractServerList serverList;
   //  private TableRowSorter<DefaultTableModel> sorter;

     static final int NRCOLS = 15;                    // the number of columns in the table

     // the table indexes
  
      public static final int SHORTNAME_INDEX = 0;
      public static final int TITLE_INDEX = 1;
      public static final int DESCRIPTION_INDEX = 2;
      public static final int IDENTIFIER_INDEX = 3;    
      public static final int PUBLISHER_INDEX = 4;
      public static final int CONTACT_INDEX = 5;
      public static final int ACCESSURL_INDEX = 6;
      public static final int REFURL_INDEX = 7;
      public static final int WAVEBAND_INDEX = 8;
      public static final int CONTTYPE_INDEX = 9;
      public static final int DATASOURCE_INDEX = 10;
      public static final int CREATIONTYPE_INDEX = 11;
      public static final int STDID_INDEX = 12;
      public static final int VERSION_INDEX = 13;
      public static final int SUBJECTS_INDEX = 14;
      public static final int TAGS_INDEX = 15;
     
     // the table headers
     private String[] headers = { "short name", "title", "description", "identifier",
                                 "publisher", "contact", "access URL", "reference URL", "waveband", "content type", 
                                 "data source", "creation type", "stantardid", "version", "subjects"};//, "tags"};


     public ServerPopupTable(AbstractServerList list) {
         super();       
         serverList=list;
         populate();
      //   setRowSorter(new TableRowSorter<TableModel>(this.getModel()));
         sortTableAlphabetically();
     }

    /* 
     * Populate 
     * fills the table with the values of serverList
     * Instead of using directly the StarTable model,
     * this way the columns are set in the desired order
     */
    public void populate() {
        
        Iterator<?>  i =  serverList.getIterator();
      
        DefaultTableModel model =  (DefaultTableModel) this.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(headers);
        
        while( i.hasNext()) {
            SSAPRegResource server= (SSAPRegResource) i.next(); 
          
            SSAPRegCapability caps[] = server.getCapabilities();
            String[] tablerow = new String[headers.length];
            
            String name = server.getShortName(); 
            if ( name == null )
                name = server.getTitle();
            else {
                name = name.trim();
                if (name.isEmpty()) { // actually shortname should not be empty, but there are empty shortnames...
                    name = server.getTitle();
                    name = name.trim();
                }
            }
                      
            tablerow[SHORTNAME_INDEX] = name;
            tablerow[TITLE_INDEX] = server.getTitle();
            tablerow[IDENTIFIER_INDEX] = server.getIdentifier();
            tablerow[PUBLISHER_INDEX] = server.getPublisher();
            tablerow[CONTACT_INDEX] =server.getContact();//.replace('<', ' ').replace('>',' ');
            tablerow[REFURL_INDEX] = server.getReferenceUrl();
            
            tablerow[WAVEBAND_INDEX] = stringJoin(server.getWaveband());
            tablerow[CONTTYPE_INDEX] = server.getContentType();
            tablerow[SUBJECTS_INDEX] =  stringJoin(server.getSubjects());
            SSAPRegCapability cap = caps[0]; 
            tablerow[ACCESSURL_INDEX] = cap.getAccessUrl();
            tablerow[DESCRIPTION_INDEX] = cap.getDescription(); 
            tablerow[DATASOURCE_INDEX] = cap.getDataSource(); 
            tablerow[CREATIONTYPE_INDEX] = cap.getCreationType();
            tablerow[STDID_INDEX] = cap.getStandardId(); 
            tablerow[VERSION_INDEX] = cap.getVersion();
          
            model.addRow(tablerow);          
        }
               
        this.setModel(model);
        setRowSorter(new TableRowSorter<TableModel>(model));
        updateServerTable();
      
    }
    
    // initially sort the services in alphabetical order
    public void sortTableAlphabetically() {
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) getRowSorter();
         List<RowSorter.SortKey> sortKeys = new ArrayList<SortKey>();
         sortKeys.add(new RowSorter.SortKey(SHORTNAME_INDEX, SortOrder.ASCENDING));
         sorter.setSortKeys(sortKeys);
         setRowSorter(sorter);        
     }
    
    
 // server table formatting and sorting
    private void updateServerTable() {
       
      for (int i=getColumnCount()-1; i>1; i--)  {
            /// update server table to show only the two first columns
            // the information remains in the tablemodel
            removeColumn(getColumn(getColumnName(i)));
      }
      updateUI();
   
    }
    
    // Transform a StringArray into a comma separated String
    private String stringJoin(String[] str) {
       
        if (str == null  || str.length==0 )
            return "";
        
        StringBuilder csbuilder=new StringBuilder();
        for (int s=0;s<str.length;s++)
            csbuilder.append(str[s]+", ");
        return csbuilder.substring(0, csbuilder.lastIndexOf(","));
    }

    /*
     * updateServers(StarTable table)
     * fills the table with the values from a StarTable
     */

    public void updateServers(StarTable table ) {
        
        try {
            if (serverList == null)
                serverList = new SSAServerList(table);
            else 
                serverList.addNewServers(table );
            
        } catch (SplatException e) {                
            e.printStackTrace();
        }

      //  this.setStarTable(table, false);
        populate();        
    }
    
    public void updateServers(StarTable table, ArrayList<String> manuallyAddedServices ) {
        
        try {
            if (serverList == null)
                serverList = new SSAServerList(table);
            else 
                serverList.addNewServers(table, manuallyAddedServices );
            
        } catch (SplatException e) {                
            e.printStackTrace();
        }

      //  this.setStarTable(table, false);
        populate();       
       
    }
    
   
   

    /**
     *  Opens a Dialog Window with Information on the right-clicked service
     * @param strings 
     * @param tags 
     */
    public void showInfo(int r, String type, String[] tags) {
       
       
        String info = "<HTML><TABLE border=0 valign=\"top\">";
        DefaultTableModel model = (DefaultTableModel) this.getModel();
            
       
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
        
        
        info += "<TR><TD col width= \"25%\"><B>Tags:</B></TD><TD col width= \"70%\">"+stringJoin(tags)+"</TD></TR>";  
        
        if (type.equals("SSAP")) {
            ArrayList<MetadataInputParameter> params = (ArrayList<MetadataInputParameter>) serverList.getResource((String) model.getValueAt(r, ServerPopupTable.SHORTNAME_INDEX)).getMetadata();
            if (params!=null) {
                String paramlist = "";
                for (int i=0;i<params.size();i++) {
                    String p = params.get(i).getName().replace("INPUT:", "");
                    paramlist+=p+"; ";
                }
                info += "<TR><TD col width= \"25%\"><B>Metadata:</B></TD><TD col width= \"70%\">"+paramlist+"</TD></TR>"; 
            } 
        }   

        info += "</TABLE></HTML>";
        
        JTextPane infoPane =  new JTextPane();
        infoPane.setContentType("text/html");
        infoPane.setText(info);       
        infoPane.setPreferredSize(new Dimension(500,300));      
        
        JOptionPane.showMessageDialog(this,
                new JScrollPane(infoPane),
                type+" Service Information",
                JOptionPane.PLAIN_MESSAGE);
     }

     public String getShortName(int row) {
         return (String) getModel().getValueAt(row, SHORTNAME_INDEX);
         
     }
     public String getAccessURL(int row) {
         return (String) getModel().getValueAt(row, ACCESSURL_INDEX).toString();
         
     }
     public String getTitle(int row) {
         return (String) getModel().getValueAt(row, TITLE_INDEX);
         
     }

    public void removeServer(int row) { 
       serverList.removeServer((String)getModel().getValueAt(row, SHORTNAME_INDEX).toString());
       //this.removeRowSelectionInterval(0, this.getRowCount()-1);
       ((DefaultTableModel) this.getModel()).removeRow(row);
       //populate();
    }

    public void addNewServer(SSAPRegResource server) {
       serverList.addServer(server, true);        
       populate();
 //      sortTable();
       //sortTable();
    }
    
    public  AbstractServerList getServerList() {
        return serverList;
     }

    public void saveServers() throws SplatException {
        serverList.saveServers();
        
    }

    public void saveServers(File file) throws SplatException {
        serverList.saveServers(file);
        
    }

    public void restoreServers(File file) throws SplatException {
        serverList.restoreServers(file);
        
    }

    public void setServerList(AbstractServerList list) {
        serverList=list;
        populate();
    }
    
    public void setServerListValue(AbstractServerList list) {
        serverList=list;
    }

    public void sortTable() {
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) getRowSorter();
        sorter.sort();        
    }

  /*  public void setServerTags(int row, String[] tags) {
       
        this.getModel().setValueAt(stringJoin(tags), row, TAGS_INDEX);
        
    }*/
}
