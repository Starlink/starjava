package uk.ac.starlink.splat.data;

/**
 * This class provides access to spectral data stored in tables, where the
 * original data is in SDFITS format.
 * <p>
 * The tables supported are any that the {@link uk.ac.starlink.table}
 * package supports.
 * <p>
 * As tables can contain many columns it is necessary to provide for
 * the selection of coordinate, value and error columns. These are
 * provided through the standard {@link SpecDataImpl}.
 *
 * @author Margarida Castro Neves
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 */

import java.io.IOException;

import nom.tam.fits.Header;
import uk.ac.starlink.splat.util.SEDSplatException;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

public class SDFitsTableSpecDataImpl extends TableSpecDataImpl {
    
    private String objectName="";
    
    public SDFitsTableSpecDataImpl(StarTable starTable, 
            String fullName, Header fitsHeaders, long row ) throws SplatException {
        
  
        
        super( starTable, row );
        //openTable( starTable );
        this.fullName = fullName;
        this.originalFitsHeaders = fitsHeaders;
        
    }
    
    /**
     * Open a table. Throws a SEDSplatException, if the table may be an SED
     */
    protected void openTable( StarTable starTable, long row )
        throws SplatException
    {
        //  Table needs random access so we can size it for making local
        //  copies of the data in the columns. This is a nullop if the table
        //  is already random.
        try {
              this.starTable = starTable ;//Tables.randomTable( starTable );
          //  if (starTable.getName().equals("SINGLE DISH")) { // read SDFITS format
         //       readSDTable(-1);
       //     } else {          
                readSDTable( row );
       //     }
        }
        catch (SEDSplatException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open table: " +
                                      starTable.getName(), e );
        }
    }   
    
    /**
     * Read in the data from the current SDFITS table. 
     */
    protected void readSDTable( long row )
        throws SplatException
    {
        
        
    //    row = -1; 
        //  Access table columns and look for which to assign to the various
        //  data types. 
        
        columnInfos = Tables.getColumnInfos( starTable ); 
        columnNames = new String[columnInfos.length];
        dataColumn = -1; // column describing data
        coordColumn = -1; // column describing coords
        int dataUnitColumn = -1;
        int dataArrayColumn = -1; // collumn where the data vector is found
        int coordsTypeColumn= -1;
        int coordsDeltaColumn= -1;
        int coordsRefValueColumn= -1;
        int coordsRPixColumn=-1;
        int shortNameColumn=-1;
        for ( int i = 0; i < columnNames.length; i++ ) {
            columnNames[i] = columnInfos[i].getName().replaceAll( "\\s", "_" );
            if (columnNames[i].equals("DATA") )
                dataArrayColumn = i;
           // if (columnNames[i].equals("TDIM"+dataArrayColumn) ) /!!!! USE TDIM!!!!
           //     dataColumn = i;
            if (columnNames[i].equals("TUNIT"+(dataArrayColumn+1)))
                dataUnitColumn=i;
            if (columnNames[i].equals("CTYPE1")) // at the moment get only the first axis descriptions
                coordsTypeColumn=i;
            if (columnNames[i].equals("CDELT1"))
                coordsDeltaColumn=i;
            if (columnNames[i].equals("CRVAL1"))
                coordsRefValueColumn=i;
            if (columnNames[i].equals("CRPIX1"))
                coordsRPixColumn=i;
            if (columnNames[i].equals("OBJECT"))
                shortNameColumn=i;
        }

       
        if (  dataArrayColumn == -1 ) {
            throw new SplatException    
                ( "No DATA column found" );
        }

        //  Find the size of the table. Limited to 2G cells.
     //   int rowCount = (int) starTable.getRowCount();
   
    //    for (row=0; row<rowCount; row++) {
            data = (double[]) readCell( row, dataArrayColumn );
            dims[0] = data.length;
            // create coords vector
            double coordsRefValue=0; // default values
            double coordsDeltaValue=1;
            double coordsRPixValue =dims[0]/2;

            if (coordsDeltaColumn != -1 && coordsRefValueColumn != -1 && coordsRPixColumn != -1) {   // SDFITS file contains these columns             
                    try {
                        coordsRefValue = (Double)  starTable.getCell(row,  coordsRefValueColumn);
                        coordsDeltaValue = (Double) starTable.getCell( row, coordsDeltaColumn);
                        coordsRPixValue = (Double) starTable.getCell( row, coordsRPixColumn);
                    } catch (IOException e) {
                       throw new SplatException(e);
                    }  
            }
            coords = new double[dims[0]];
            //coords[0]= coordsRefValue;
            for (int i=1; i<=coords.length; i++)
                coords[i-1] = (i-coordsRPixValue)*coordsDeltaValue + coordsRefValue; // The frequency at a given pixel (channel): 
                                                                                     // f(i) = (i-CRPIX1)*CDELT1 + CRVAL1        where i starts with 1                        
            //  try to write something useful in the labels
            if (dataUnitColumn >= 0 && coordsTypeColumn >=0) {              
                    String unit;
                    String descr;
                    try {
                        unit = starTable.getCell( row, dataUnitColumn).toString();
                        descr = starTable.getCell( row, coordsTypeColumn).toString();
                    } catch (IOException e) {
                        throw new SplatException(e);
                    }
                    columnInfos[dataUnitColumn].setUnitString(unit);
                    columnInfos[coordsTypeColumn].setDescription(descr);
                    columnInfos[coordsTypeColumn].setUnitString("Hz");  // Frequency in SDFITS allways in Hz?
            }          
            dataColumn = dataUnitColumn; // hack to try writing something useful in the labels
            coordColumn = coordsTypeColumn; // hack to try writing something useful in the labels
            
            if (shortNameColumn >= 0)
                try {
                    this.shortName = starTable.getCell( row, shortNameColumn).toString();
                } catch (IOException e) {                   
                    // do nothing throw new SplatException(e);
                }

                
                
                
        //  Create the AST frameset that describes the data-coordinate
            //  relationship.
            createAst();
            
 //       }                  
      
    }



}
