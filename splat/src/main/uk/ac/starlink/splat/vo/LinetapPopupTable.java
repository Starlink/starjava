/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.util.Iterator;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import uk.ac.starlink.splat.util.SplatException;

/**
 * @author Margarida Castro Neves
 *
 */
public class LinetapPopupTable extends ServerPopupTable {
	
	
	 private String[] headers = { "short name", "title", "description", "identifier",
               "publisher", "contact", "access URL", "reference URL", "waveband", "content type",
               "data source", "creation type", "stantardid", "version", "subjects", "table name"};//, "tags"};

	/**
	 * Linetap version of ServerPopupTable to show table name at the panel
	 * @param linetapServerList 
	 */
	
    public LinetapPopupTable() {
        super();
        try {
           serverList=new LinetapServerList();
       } catch (SplatException e) {
           //
       }
    }

    public LinetapPopupTable(AbstractServerList list) {
        super();
        serverList=list;
        populate();
        sortTableAlphabetically();

    }
    
    /*
     * Populate
     * fills the table with the values of serverList
     * Instead of using directly the StarTable model,
     * this way the columns are set in the desired order
     */
    public void populate() {


        DefaultTableModel model =  (DefaultTableModel) this.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(this.headers);

        Iterator<?>  i =  serverList.getIterator();


        while( i.hasNext()) {

            SSAPRegResource server= (SSAPRegResource) i.next();
            if (server != null) {
                SSAPRegCapability caps[] = server.getCapabilities();
                if (caps == null || caps.length == 0) continue;
             
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
               
                String tabname = server.getTableName();
                if (tabname == null || tabname == "")
                	tabname = name;
                tablerow[TABLENAME_INDEX] =tabname;
                
                model.addRow(tablerow);
            }
        }

        this.setModel(model);
        setRowSorter(new TableRowSorter<TableModel>(model));
        updateServerTable();

    }

 // server table formatting and sorting
 // leave only shortname and tablename (the first and the last)
    protected void updateServerTable() {
    	
    	for (int i=getColumnCount()-1; i>=0; i--)  {
            /// update server table to show only the two first columns
            // the information remains in the tablemodel
    		String name = getColumnName(i);
    		if (name!="table name" )//&& name != "access URL")
               removeColumn(getColumn(name));
      }

    }
	
  

}
