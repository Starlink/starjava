/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.starlink.topcat.contrib.basti;

import java.awt.Component;
import java.io.IOException;
import java.net.URL;

import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.QueueTableSequence;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.util.URLDataSource;
/**
 *
 * @author molinaro
 * @author taylor
 */
public class BaSTITableLoadDialog extends AbstractTableLoadDialog {

    private static final String GET_VOT_ENDPOINT = "http://albione.oa-teramo.inaf.it/POSTQuery/getVOTable.php?";

    /* POST Message container and handler */
    static BaSTIPOSTMessage POSTQuery = new BaSTIPOSTMessage();

    @SuppressWarnings("this-escape")
    public BaSTITableLoadDialog() {
        super("BaSTI Data Loader",
              "a Bag of Stellar Tracks and Isochrones");
        setIcon(ResourceIcon.BASTI);
    }

    public TableLoader createTableLoader() {
        return new TableLoader() {

            public String getLabel() {
                return "BaSTI";
            }

            public TableSequence loadTables( final StarTableFactory tfact )
                    throws IOException {
                final String[] locations = new String[BaSTIPanel.ResultsTable.getSelectedRowCount()];
                String[] SubDirs = new String[BaSTIPanel.ResultsTable.getSelectedRowCount()];
                int[] rowSelection = BaSTIPanel.ResultsTable.getSelectedRows();
                for ( int r=0; r<locations.length; r++ ) {
                    String[] rowPieces = BaSTIPOSTMessage.SQLresults[rowSelection[r]+2].split(":");
                    SubDirs[r] = rowPieces[1];
                    locations[r] = GET_VOT_ENDPOINT + rowPieces[1] + BaSTIPanel.ResultsData.getValueAt(rowSelection[r], 0).toString();
                }

                final QueueTableSequence tseq = new QueueTableSequence();
                Thread loader = new Thread("BaSTI loader") {
                    public void run() {
                        try {
                            for (int t=0; t<locations.length; t++) {
                                URL URLLocation = new URL(locations[t]);
                                StarTable table = tfact.makeStarTable(new URLDataSource(URLLocation), "votable");
                                table.setParameter(new DescribedValue(TableLoader.SOURCE_INFO, table.getName()));
                                tseq.addTable(table);
                            }
                        }
                        catch (Throwable e) {
                            tseq.addError(e);
                        }
                        finally {
                            tseq.endSequence();
                        }
                    }
                };
                loader.setDaemon(false);
                loader.start();
                return tseq;
            }
        };
    }

    protected Component createQueryComponent() {
        return BaSTIPanel.create();
    }
}
