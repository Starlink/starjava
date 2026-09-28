package uk.ac.starlink.splat.vo;

import java.util.Iterator;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class SlapServerPopupTable extends ServerPopupTable<SlapRegResource> {
	
	
	
   //  public static final int SPECIESURL_INDEX = 16;
	
	 private static final String[] headers = { "short name", "title", "description", "identifier",
             "publisher", "contact", "access URL", "reference URL", "waveband", "content type",
             "data source", "creation type", "stantardid", "version", "subjects", "species_url"};//, "tags"};


	public SlapServerPopupTable() {
		super();
		
	}

	public SlapServerPopupTable(SLAPServerList list) {
		super(list);
		
	}

    /*
     * Populate
     * fills the table with the values of serverList
     * Instead of using directly the StarTable model,
     * this way the columns are set in the desired order
     */
    @Override
    public void populate() {


        DefaultTableModel model =  (DefaultTableModel) this.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(headers);

        Iterator<?>  i =  serverList.getIterator();


        while( i.hasNext()) {

            SlapRegResource server= (SlapRegResource) i.next();
            if (server != null) {
                SLAPRegCapability caps[] = server.getCapabilities();
                if (caps == null || caps.length == 0) continue;
                String[] tablerow = new String[headers.length];

                String name = server.getShortName();
                if ( name == null )
                    name = server.getTitle();
                else {
                    name = name.trim();
                    if (name.isEmpty()) { // linetap case: name is the table name
                        name = server.getTableName();
                        name = name.trim();
                    }
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
                tablerow[SPECIESURL_INDEX] = cap.getSpeciesUrl();

                model.addRow(tablerow);
            }
        }

        this.setModel(model);
        setRowSorter(new TableRowSorter<TableModel>(model));
        updateServerTable();

    }
    public String getSpeciesURL(int row) {
        return (String) getModel().getValueAt(row, SPECIESURL_INDEX).toString();

    }


}
