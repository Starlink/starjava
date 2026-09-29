package uk.ac.starlink.splat.data;

import java.util.Arrays;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

public class SDSSLineIDTableSpecDataImpl extends LineIDTableSpecDataImpl {
	
	 
	 
	 
	   public SDSSLineIDTableSpecDataImpl( StarTable starTable )
		        throws SplatException
		    {
		        super( starTable, true );// sdss
		        setName( starTable );
		        openTable( starTable );
		     
		    }
	   /**
	     * Open a table.
	     */
	    protected void openTable( StarTable starTable )
	        throws SplatException
	    {
	        //  Table needs random access so we can size it for making local
	        //  copies of the data in the columns. This is a nullop if the table
	        //  is already random.
	        try {
	            this.starTable = Tables.randomTable( starTable );
	            readSDSSTable();
	        }
	        catch (Exception e) {
	            throw new SplatException( "Failed to open table: " +
	                                      starTable.getName(), e );
	        }
	    } 

     void readSDSSTable() throws SplatException {
	    {
	        //  Access table columns and look for which to assign to the various
	        //  data. The default, if the matching fails, is to use the
	        //  first and second (whatever that means) columns and use those.
	        columnInfos = Tables.getColumnInfos( starTable );
	        columnNames = new String[columnInfos.length];
	        for ( int i = 0; i < columnNames.length; i++ ) {
	            columnNames[i] = columnInfos[i].getName().toLowerCase();
	        }

	        coordColumn=-1;
	        if (coordColName == null) {
	        	coordColumn =
	        			TableColumnChooser.getInstance().getCoordMatch( columnInfos,
	                                                            columnNames );
	        	if ( coordColumn == -1 ) {
	            	// No match for coordinates, look for "first" numeric column.
	            	for ( int i = 0; i < columnInfos.length; i++ ) {
	                	if ( Number.class.isAssignableFrom
	                		( columnInfos[i].getContentClass() ) ) {
	                		coordColumn = i;
	                    	break;
	                	}
	            	}
	        	}
	        } else {
	        	
	        	for ( int i = 0; i < columnInfos.length; i++ ) {
	        	
	            	if ( coordColName.equals(columnNames[i])) {
	            		coordColumn = i;
	                	break;
	            	}
	        	}
	        }
	        
	        if ( coordColumn == -1 ) {
	            throw new SplatException( "Line identifier tables must "+
	                                      "contain at least one numeric column" );
	        }
	        
	       labelColumn =-1;
	    	//  Look for the labels.
	        if (labelColName == null) {
	        	labelColumn =
	        			TableColumnChooser.getInstance().getLabelMatch( columnInfos,
	                                                            columnNames );
	        } else {
	        	
	        	for ( int i = 0; i < columnInfos.length; i++ ) {
	            	if ( labelColName.equals(columnNames[i])) {
	            		labelColumn = i;
	                	break;
	            	}
	        	}
	        }
	        //  Find the size of the table. Limited to 2G cells.
	        dims[0] = (int) starTable.getRowCount();
	        
	        if ( dims[0]==1 ) {
	        	 int[] shape = columnInfos[coordColumn].getShape();
	        	 dims[0]=shape[0];
	        	 labels = (String[]) readColumn( labelColumn );
	        	 
	        	 // coords are stored as float array, need to convert
	        	 
	        	 float [] rowData;
	        	 rowData = (float []) readColumn( coordColumn);
	        	 coords =  new double[rowData.length];
	        	 
	        	 for (int i = 0; i < rowData.length; i++) {
      	            coords[i] = rowData[i];  // Convert each element  
	        	 }
	        } else {

	        	labels = new String[dims[0]];
	        	if (labelColumn > -1 && labels != null)
	        		readColumn( labels, labelColumn );

	        	//  Access column data, one value per cell.
	        	coords = new double[dims[0]];
	        	readColumn( coords, coordColumn );

	        }
	        data = new double[dims[0]];
	        Arrays.fill(data, SpecData.BAD);
	        

	        //  Create the AST frameset that describes the data-coordinate
	        //  relationship.
	        createAst();
	    }

	}
     /**
      * Read a column of strings from the table into the given array.
      */
     protected Object readColumn( int index )
         throws SplatException
     {
         Object row = null;
         try {
        	 starTable.getRowSequence();
             RowSequence rseq = starTable.getRowSequence();
            
             while( rseq.next() ) {
                row =  rseq.getCell( index );
             }
             rseq.close();
             return row;
            
         }
         catch (Exception e) {
             throw new SplatException( "Failed reading table column" , e );
         }
     }
	   
}
