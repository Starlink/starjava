/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.starlink.topcat.contrib.basti;

import java.awt.Component;
import java.io.IOException;
import java.net.URL;

import javax.swing.ImageIcon;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.MultiTableLoadDialog;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.util.URLDataSource;
/**
 *
 * @author molinaro
 */
public class BaSTIMultiTableLoadDialog extends MultiTableLoadDialog {

    private static final String GET_VOT_ENDPOINT = "http://albione.oa-teramo.inaf.it/POSTQuery/getVOTable.php?";

    /* POST Message container and handler */
    static BaSTIPOSTMessage POSTQuery = new BaSTIPOSTMessage();

    public BaSTIMultiTableLoadDialog() {
        super("BaSTI Data Loader", "a Bag of Stellar Tracks and Isochrones (under development)");
        setIcon( ResourceIcon.LOAD );
        //setIcon( new ImageIcon("BaSTI_logo.jpg") );
    }

    @Override
    protected TablesSupplier getTablesSupplier() throws RuntimeException {
        return new TablesSupplier() {
            
            public StarTable[] getTables(StarTableFactory tfact, String format) throws IOException {
                String[] locations = new String[BaSTIPanel.ResultsTable.getSelectedRowCount()];
                String[] SubDirs = new String[BaSTIPanel.ResultsTable.getSelectedRowCount()];
                int[] rowSelection = BaSTIPanel.ResultsTable.getSelectedRows();

//                System.out.println(locations.length + " righe selezionate: ");
//                for (int j=0; j<locations.length; j++) { System.out.print(rowSelection[j] + " "); }
//                System.out.println();

                for ( int r=0; r<locations.length; r++ ) {
                    String[] rowPieces = BaSTIPOSTMessage.SQLresults[rowSelection[r]+2].split(":");
                    SubDirs[r] = rowPieces[1];
                    locations[r] = GET_VOT_ENDPOINT + rowPieces[1] +
                                BaSTIPanel.ResultsData.getValueAt(rowSelection[r], 0).toString();
//                    System.out.println("riga " + rowSelection[r] + ": " + locations[r]);
                }

                int NTables = locations.length;
                StarTable[] BaSTITables = new StarTable[NTables];

                for (int t=0; t<locations.length; t++) {
                    URL URLLocation = new URL(locations[t]);
                    BaSTITables[t] = tfact.makeStarTable(new URLDataSource(URLLocation), "votable");
                }
                return BaSTITables;
            }

            public String getTablesID() {
                return("BaSTI");
            }
        };
    }

    @Override
    protected Component createQueryPanel() {
        return BaSTIPanel.create();
    }

    public boolean isAvailable() {
        return true;
    }

}
