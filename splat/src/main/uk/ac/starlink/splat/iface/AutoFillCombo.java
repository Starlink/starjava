package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jsky.util.Logger;
import uk.ac.starlink.splat.vo.TapQueryWithDatalink;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.ContentCoding;

@SuppressWarnings("serial")
public class AutoFillCombo  extends JPanel implements ActionListener, DocumentListener{

	JTextField textf;
	JComboBox<String> matchesBox;

	Boolean elementChosen=false; // true: a choice has been made in the combobox
	Boolean makeQuery=false;  // true :query database for species ; false: get list of elements. 
	Map <String,SpeciesItem> speciesInChiKey = null;
	String[] emptyBox= {"",""};

	public AutoFillCombo( String label, boolean querySpecies ) {
		
		 makeQuery= querySpecies;
		
		 GridBagConstraints gbc = new GridBagConstraints();
         gbc.anchor = GridBagConstraints.PAGE_START;
         gbc.fill = GridBagConstraints.HORIZONTAL; 
         this.setLayout(new GridBagLayout());
		
		JLabel lbl= new JLabel (label) ;
		
		textf = new JTextField( 25);
		textf.addActionListener(this);
		textf.getDocument().putProperty("owner", textf);
	    textf.getDocument().addDocumentListener( this );
	      
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(lbl, BorderLayout.WEST);
	    panel.add(textf,  BorderLayout.CENTER);
		matchesBox = new JComboBox<String>();
		matchesBox.setMaximumSize(this.getSize());
		matchesBox.addActionListener(this);
		//matchesBox.addItemListener(this);
		
		
		
		gbc.gridx=0;
		gbc.gridy=0;
		
		this.add (panel, gbc);
		gbc.gridy=1;
		this.add (matchesBox, gbc);
	
		
	}
	
	private void updateCombo(ArrayList<String> results ) {
		elementChosen=false;
		matchesBox.removeAllItems();
		if (results.size()==0) {			
			matchesBox.addItem("");
			matchesBox.setSelectedItem("");
		} else {
		    matchesBox.setModel ( new DefaultComboBoxModel(results.toArray()));
			matchesBox.addItem("");	    
		    matchesBox.showPopup();
	    }
		
	}
	
/*	private void updateComboSpecies(ArrayList<SpeciesItem> results ) {
		
		if (results.size()==0) {
			
			//matchesBox.removeAllItems();
			matchesBox.addItem(emptyBox);
			matchesBox.setSelectedItem("");
		} else {
		DefaultComboBoxModel<String[]> results2 = new DefaultComboBoxModel<>();

		   for (int i=0;i<results.size();i++) {
			   results2.addElement(results.get(i));
		   }
		    matchesBox.setModel(  results2 ) ;	
//		    matchesBox.setModel ( new DefaultComboBoxModel(String[2] results.toArray()));
			matchesBox.addItem(emptyBox);	    
		    matchesBox.showPopup();
	    }
		
	}
	*/
	
	public String getElement(  ) {

		return textf.getText();
	}
	
	public String getInChiKey() {
		if ( makeQuery ) {
			
			try {
				String spcs = textf.getText();
				if (! (spcs == null) && ! spcs.isEmpty())
					Logger.info (this, spcs);
					SpeciesItem chosen = (SpeciesItem) speciesInChiKey.get(spcs);
					String inchik = chosen.getInchikey();
					return (  speciesInChiKey.get( spcs ).getInchikey() );
			} catch ( Exception e) {
				Logger.info( this, "no inchiHey found");
				return "";
			}
		} else {
			
		}
		return "";
	}
	

	private void updateCombo(Map<String, SpeciesItem> results ) {
		
		speciesInChiKey = results;
		elementChosen=false;
		
	
		String [] resultList =  (results.keySet()).toArray(new String[0]);
				 
		if (results.size()==0) {
			
			//matchesBox.removeAllItems();
			matchesBox.addItem("");
			matchesBox.setSelectedItem("");
		} else {
			
		    matchesBox.setModel ( new DefaultComboBoxModel<String>( resultList));
			matchesBox.addItem("");	    
		    matchesBox.showPopup();
	    }
	
	}
   private void resetCombo()  {
		
       matchesBox.removeAllItems();
      //  elementChosen=false;
      //  matchesBox.addActionListener(this);
       // matchesBox.addItem("");
       // matchesBox.setSelectedItem("");
       textf.setText("");
       this.firePropertyChange("AutoFillCombo", true, false );

	}
	


	static class Elements {



		static final List<String> elementSymbol = Arrays.asList (new String[] {

				"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K",
				"Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb",
				"Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs",
				"Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta",
				"W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa",
				"U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt",
				"Ds", "Rg", "Cn", "Nh", "Fl", "Mc", "Lv", "Ts", "Og"

		});

		static final List<String> elementName = Arrays.asList (new String[] {
				"Hydrogen", "Helium", "Lithium", "Beryllium", "Boron", "Carbon", "Nitrogen", "Oxygen", "Fluorine", "Neon",
				"Sodium", "Magnesium", "Aluminum", "Silicon", "Phosphorus", "Sulfur", "Chlorine", "Argon", "Potassium",
				"Calcium", "Scandium", "Titanium", "Vanadium", "Chromium", "Manganese", "Iron", "Cobalt", "Nickel",
				"Copper", "Zinc", "Gallium", "Germanium", "Arsenic", "Selenium", "Bromine", "Krypton", "Rubidium", "Strontium",
				"Yttrium", "Zirconium", "Niobium", "Molybdenum", "Technetium", "Ruthenium", "Rhodium", "Palladium", "Silver",
				"Cadmium", "Indium", "Tin", "Antimony", "Tellurium", "Iodine", "Xenon", "Cesium", "Barium", "Lanthanum",
				"Cerium", "Praseodymium", "Neodymium", "Promethium", "Samarium", "Europium", "Gadolinium", "Terbium", "Dysprosium",
				"Holmium", "Erbium", "Thulium", "Ytterbium", "Lutetium", "Hafnium", "Tantalum", "Tungsten", "Rhenium", "Osmium",
				"Iridium", "Platinum", "Gold", "Mercury", "Thallium", "Lead", "Bismuth", "Polonium", "Astatine", "Radon",
				"Francium", "Radium", "Actinium", "Thorium", "Protactinium", "Uranium", "Neptunium", "Plutonium", "Americium",
				"Curium", "Berkelium", "Californium", "Einsteinium", "Fermium", "Mendelevium", "Nobelium", "Lawrencium", 
				"Rutherfordium", "Dubnium", "Seaborgium", "Bohrium", "Hassium", "Meitnerium", "Darmstadtium", "Roentgenium",
				"Copernicium", "Nihonium", "Flerovium", "Moscovium", "Livermorium", "Tennessine", "Oganesson"
		});
		
	
		
		private static ArrayList<String> getMatches(String pref) {
			
	
			ArrayList <String> results = new ArrayList<String>();
			String prefl = pref.toLowerCase().trim();
			
			
			for (int i=0;i< elementSymbol.size(); i++) {
				String name = elementName.get(i);
				String symbol = elementSymbol.get(i);
				String result = symbol + "-" + name;
				 if (name.toLowerCase().startsWith(prefl)) {
					 results.add(result);
				 }  
				 if (symbol.toLowerCase().startsWith(prefl) && !results.contains(result)) {
					 results.add(result);
				 }
				
			}
			return  results;
			
		} // getMatches


	} // Elements
	
	static class SpeciesQuery {
		
		private static String SPECIESDB_URL = "http://dc.zah.uni-heidelberg.de/tap";
		private static String SPECIES_TABLE = "species.main";
	
		
		private static StarTable querySpecies( String pref ) {
			
		 
			
		   	TapQueryWithDatalink tq;
	    	StarTable startable;
	    	
	    	String query = "SELECT DISTINCT name, formula, inchikey FROM "+SPECIES_TABLE+" WHERE name ILIKE '"+pref+"%' OR FORMULA ILIKE '"+pref+"'" ;
	       
	    	try {
				tq =  new TapQueryWithDatalink( new URL(SPECIESDB_URL), query,  null );
			} catch (MalformedURLException e) {
			
				e.printStackTrace();
				return null;
			}
	    	StarTableFactory tfact = new StarTableFactory();
	    	try {
				startable = tq.executeSync( tfact.getStoragePolicy(), ContentCoding.NONE );
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
	    	return startable;

		
		}
		
		private static Map<String,SpeciesItem>  getMatches(String pref) {
			
		//	ArrayList <String> results = new ArrayList<String>();
			Map <String, SpeciesItem> results = new HashMap<String,SpeciesItem>();
			
			//  !!!!!!!!!!! if user already edited the  line, separate name and formula and query again
			
	
			StarTable st = querySpecies(pref.toLowerCase());
			
			
			
			for (int i = 0; i < st.getRowCount(); i++) {  // Loop through the rows						
		        // name, formula, inchikey
				try {
					
					SpeciesItem species = new SpeciesItem();
					species.setName( (String) st.getCell(i,0) );
					species.setFormula( (String) st.getCell(i,1) );
					species.setInchikey( (String) st.getCell(i,2) );
			
					results.put( species.getKey(), species);
				
				} catch (IOException e) {
					
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
			
			return  results;
			
		} // getMatches
		
		
	} // SpeciesQuery
	
	static class SpeciesItem {
		String name;
		String formula;
		String inchikey;
		
		public SpeciesItem () {
			name = "";
			formula= "";
			inchikey= "";
		}
		
		public String getName() 
		{ return name;
		}
		public void setName ( String speciesname ) {
			name = speciesname;
		}
		public String getFormula() 
		{ return formula;
		}
		public void setFormula ( String speciesformula ) {
			formula = speciesformula;
		}
		public String getInchikey() { 
			return inchikey;
		}
		public void setInchikey ( String speciesinchikey ) {
			inchikey = speciesinchikey;
		}
		public String getKey() {
			return(String.format("%s  [ %s ]", name, formula));
		}
		
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		
		updateAction(e);
		
		
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		//if (! elementChosen)
			if (textf.getText().isEmpty() )
				resetCombo();
	//	else 
			updateAction(e);
		
	}

	@Override
	public void changedUpdate(DocumentEvent e) {		
		updateAction(e);

	}
  
	protected void updateAction(DocumentEvent e) {
		
		
		  ArrayList<String> result = new ArrayList<String>();
		  Map<String, SpeciesItem> resultMap;
		  Object owner = e.getDocument().getProperty("owner");
		  if(owner != null && owner.getClass() == JTextField.class) {
			  String text = textf.getText();
			  if ( text != null && ! text.isEmpty() ) {
				  if ( makeQuery ) {					  
					  if (! elementChosen ) { // did not select new combobox element
						  try {

							  resultMap = SpeciesQuery.getMatches(text);
						  }
						  catch (Exception e1) {

							  return;
						  }	 
						  updateCombo(resultMap);
					  } else {
						  elementChosen=false;
					  }

				  } else {
					  try {
						  result = Elements.getMatches(text);
					  }
					  catch (Exception e1) {

						  return;
					  }					  
					  updateCombo(result);
					 
				  }
			
			  }
			  			 
			//  else
			//	  matchesBox.removeAllItems();
	      
	      }
		  
	}
	


	@Override
	public void actionPerformed(ActionEvent e) {
		
		Logger.info( this,"changeselection");

		if (e.getSource() == matchesBox) {
			try {
				
				String chosen = (String) matchesBox.getSelectedItem();
				if (chosen == null) 
					chosen="";
				if (!chosen.isEmpty())
				   elementChosen=true;
				else 
				   elementChosen = false;
				textf.setText( chosen);
				
				
			} catch (Exception ex) {}
		}
		this.firePropertyChange("AutoFillCombo", true, false );


	}

/*	@Override
	public void itemStateChanged(ItemEvent e) {
		int state = e.getStateChange();
		System.out.println((state == ItemEvent.SELECTED) ? "Selected" : "Deselected");
		System.out.println("Item: " + e.getItem());
		ItemSelectable is = e.getItemSelectable();
		Object [] selected =   is.getSelectedObjects();
		if (selected.length > 0) {
			String sel = selected[0].toString();
			System.out.println(", Selected: " + sel);
			textf.setText(sel);
		}	        
	}
*/

}

