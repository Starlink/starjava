package uk.ac.starlink.splat.data.ssldm;

/*****************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class Species {
    
    /* 
     * This class is a placeholder for a future model, providing a full description of the
     * physical and chemical property of the chemical element of compound where the
     * transition originating the line occurs.
     * While a more detailed data model for this class is built, this simplified version
     * contains simply the name of the Species involved in the transition.
     * 
     * @author Margarida Castro Neves
     */
    
    
    String elementName;  // name of element e.g. H, He, Fe,... 
    String name;         // title (name + ionization stage) e.g. Fe II, Ti IV, ...
    String ucd=null ;
    String utype=null;
    int ionizationStage=0;
    
    String roman1[]= {"I","II","III","IV","V","VI","VII","VIII","IX", "X"};
    String roman2[]= {"","X", "XX","XXX","XL","L","LX","LXX","LXXX","XC", "C"};
    String roman3[]= {"","C", "CC","CCC","CD","D","DC","DCC","DCCC","DM", "C"};

    public Species(String name) {
        this.name = name;
    }

    public Species() {
       
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setName(String elname, int stage) {
        this.ionizationStage=stage;
        this.name = elname+" "+ getIonizationStageRoman();
        setElementName(elname);
    }
    
    public String getElementName() {
        return elementName;
    }

    public void setElementName(String name) {
        this.elementName = name;
    }

    public String getUcd() {
        return ucd;
    }

    public void setUcd(String string) {
        this.ucd = string;
    }

    public String getUtype() {
        return utype;
    }

    public void setUtype(String utype) {
        this.utype = utype;
    }
    
    public int getIonizationStage() {
        return ionizationStage;
    }
    
    public String getIonizationStageRoman() {
    	if (ionizationStage <0)
    		return "";
        if (ionizationStage>500) 
            return ""; 
        // TODO later make this better now just a hack
        
        if (ionizationStage < 10)
            return roman1[ionizationStage];
        
        
        int unit = ionizationStage %10;  
        int tens = (ionizationStage-unit)/10;
        int hundreds = (ionizationStage-tens-unit)/10;
        return roman3[hundreds]+roman2[tens]+roman1[unit];
    }


    public void setIonizationStage(int stage) {
        this.ionizationStage = stage;
    }


}
