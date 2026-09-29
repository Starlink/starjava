package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.votable.VOTableBuilder;

public class SpeciesTable {
	

	private StarTable speciesTable=null;
	private int nameIndex = -1;
	private int formulaIndex = -1;
	private int inchikeyIndex= -1;
	private List<String> speciesdbUrl = new ArrayList<String>();
	private LineBrowser browser; 

	public SpeciesTable(LineBrowser browser) {
	
		this.browser=browser;
		queryServices(browser);
	}

	private void queryServices(LineBrowser browser) {
		SlapServerPopupTable serviceTable = browser.getLinesQueryPanel().getSlapTable();
		//serviceTable.populate();
		/// get slap services species urls
	      for ( int r : serviceTable.getSelectedRows() ) {		           
	           int row=serviceTable.convertRowIndexToModel(r);
	           speciesdbUrl.add(serviceTable.getSpeciesURL(row));
	      }		
	      if (speciesdbUrl.size() == 0)
	    	  return;
	      
	      // query and get species 
	      
	      speciesTable = updateSpeciesTable(speciesdbUrl);
	      
	      for (int i = 0; i < speciesTable.getColumnCount(); i++) {
			    String colName = speciesTable.getColumnInfo(i).getName();
			    if (colName.equalsIgnoreCase("species_name"))     nameIndex = i;
			    if (colName.equalsIgnoreCase("stoichiometric_formula"))  formulaIndex= i;
>>>>>>> splat4.1doc
			    if (colName.equalsIgnoreCase("inchikey")) inchikeyIndex = i;
			}
	      if (nameIndex < 0 || formulaIndex < 0 || inchikeyIndex < 0) {
	    	    throw new IllegalArgumentException("name/formula/inchikey column not found");
	    	}
	}

	public StarTable updateSpeciesTable( List<String> species_url ) {

		StarTable species = null;
		if (species_url.size() > 1) {
			for (String surl: species_url) {
				StarTable table = doSlapSpeciesQuery(surl);
				if (species == null) {
		            species = table;   // first table becomes the base
		        } else {
		        	StarTable tables[] = {species, table};
		        	try {
		        		species = new ConcatStarTable(species, tables);
		        	} catch (IOException e) {
					
		        		e.printStackTrace();
		        	}
		        }

			}
		} else {
			species = doSlapSpeciesQuery(species_url.get(0));
		}

		return species;
	}

	private StarTable doSlapSpeciesQuery(String spurl) {
		
     	URLConnection con = browser.checkAndConnect(  spurl, null  );
     	StarTable startable= null;
      	
      		try {
      			con.connect();
				startable = new StarTableFactory(true).makeStarTable( con.getInputStream(), new VOTableBuilder() );
			} catch (TableFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
      //  if (con != null)
          	return startable;      
    
	}
	
	public StarTable getTable() {
		return speciesTable;
	}
	
	public int getNameIndex( ) {
		return nameIndex;
	}
	public int getFormulaIndex( ) {
		return formulaIndex;
	}
	public int getInchiKeyIndex( ) {
		return inchikeyIndex;
	}
/*	
	public  Map<String,SpeciesItem>  getMatches(String pref) {
		
		Map <String, SpeciesItem> results = new HashMap<String,SpeciesItem>();
		
		//  !!!!!!!!!!! if user already edited the  line, separate name and formula and query again
		
		StarTable st = speciesTable; //querySpecies(pref.toLowerCase());
		
		pref = pref.toLowerCase();
		
		for (int i = 0; i < st.getRowCount(); i++) {  // Loop through the rows						
	        // name, formula, inchikey
			try {
				String name = (String) st.getCell(i,nameIndex);
				String formula = (String) st.getCell(i,formulaIndex) ;
				String inchikey = (String) st.getCell(i,inchikeyIndex);
				 
		        boolean nameMatch = name.startsWith(pref);
		        boolean formulaMatch = formula.equalsIgnoreCase(pref);

		        if (nameMatch || formulaMatch) {
		           
		        	SpeciesItem species = new SpeciesItem();
		        	species.setName( (String) st.getCell(i,nameIndex) );
		        	species.setFormula( (String) st.getCell(i,formulaIndex) );
		        	species.setInchikey( (String) st.getCell(i,inchikeyIndex) );
		
		        	results.put( species.getKey(), species);
		        }
			
			} catch (IOException e) {
				
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
		return  results;
		
	} // getMatches
	
*/

}
