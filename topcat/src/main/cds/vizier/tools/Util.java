//
//Copyright 1999-2005 - Universite Louis Pasteur / Centre National de la
//Recherche Scientifique
//
//------
//
//Address: Centre de Donnees astronomiques de Strasbourg
//       11 rue de l'Universite
//       67000 STRASBOURG
//       FRANCE
//Email:   question@simbad.u-strasbg.fr
//
//-------
//
//In accordance with the international conventions about intellectual
//property rights this software and associated documentation files
//(the "Software") is protected. The rightholder authorizes :
//the reproduction and representation as a private copy or for educational
//and research purposes outside any lucrative use,
//subject to the following conditions:
//
//The above copyright notice shall be included.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
//OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON INFRINGEMENT,
//LOSS OF DATA, LOSS OF PROFIT, LOSS OF BARGAIN OR IMPOSSIBILITY
//TO USE SUCH SOFWARE. IN NO EVENT SHALL THE RIGHTHOLDER BE LIABLE
//FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
//THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//For any other exploitation contact the rightholder.
//
//                     -----------
//
//Conformement aux conventions internationales relatives aux droits de
//propriete intellectuelle ce logiciel et sa documentation sont proteges.
//Le titulaire des droits autorise :
//la reproduction et la representation a titre de copie privee ou des fins
//d'enseignement et de recherche et en dehors de toute utilisation lucrative.
//Cette autorisation est faite sous les conditions suivantes :
//
//La mention du copyright portee ci-dessus devra etre clairement indiquee.
//
//LE LOGICIEL EST LIVRE "EN L'ETAT", SANS GARANTIE D'AUCUNE SORTE.
//LE TITULAIRE DES DROITS NE SAURAIT, EN AUCUN CAS ETRE TENU CONTRACTUELLEMENT
//OU DELICTUELLEMENT POUR RESPONSABLE DES DOMMAGES DIRECTS OU INDIRECTS
//(Y COMPRIS ET A TITRE PUREMENT ILLUSTRATIF ET NON LIMITATIF,
//LA PRIVATION DE JOUISSANCE DU LOGICIEL, LA PERTE DE DONNEES,
//LE MANQUE A GAGNER OU AUGMENTATION DE COUTS ET DEPENSES, LES PERTES
//D'EXPLOITATION,LES PERTES DE MARCHES OU TOUTES ACTIONS EN CONTREFACON)
//POUVANT RESULTER DE L'UTILISATION, DE LA MAUVAISE UTILISATION
//OU DE L'IMPOSSIBILITE D'UTILISER LE LOGICIEL, ALORS MEME
//QU'IL AURAIT ETE AVISE DE LA POSSIBILITE DE SURVENANCE DE TELS DOMMAGES.
//
//Pour toute autre utilisation contactez le titulaire des droits.
//
package cds.vizier.tools;


import java.awt.*;
import java.util.Vector;

/**
 * Diverses méthodes utilitaires
 */
public final class Util {

	
    public static String CR;
    public static String FS;
    
	static {
		CR = System.getProperty("line.separator");
        FS = System.getProperty("file.separator");
	}
   
   /** Adapted from a C-algorithm from P. Fernique
    * checks whether word matches mask
    * @param mask a string which may contain '?' and '*' wildcards
    * @param word the string to check
    * @return boolean true if word matches mask, false otherwise
    */
   static public boolean matchMask(String mask, String word) {
       if( word==null || mask==null ) return false;
       mask = mask+'\0';
       word = word+'\0';
       int indiceM,indiceA;
       indiceM=indiceA=0;
       String stringB=null;
       String stringC=null;

       while( mask.charAt(indiceM)!='\0' || word.charAt(indiceA)!='\0' ) {
        if( mask.charAt(indiceM)=='\\' ) {
            indiceM++;
            continue;
        }
        
           if( mask.charAt(indiceM)=='*' && (indiceM==0 || mask.charAt(indiceM-1)!='\\') ) {
               indiceM++;
               stringB = mask.substring(indiceM);
               continue;
           }
           if( stringB!=null && !stringB.equals(mask) && word.charAt(indiceA)==word.charAt(0) ) stringC = word.substring(indiceA);

           if( mask.charAt(indiceM)==word.charAt(indiceA) || mask.charAt(indiceM)=='?' ) {
               if( mask.charAt(indiceM)=='\0' ) {
                   if( stringB==null ) return false;
               }
               else indiceM++;
               if( word.charAt(indiceA)=='\0' ) return false;
               else indiceA++;
           }
           else {
               if( stringB!=null ) {
                   mask = stringB;
                   indiceM = 0;

                   if( stringC!=null ) {
                       word = stringC;
                       indiceA = 0;
                       stringC = null;
                   }
                   else {
                       if( stringB.charAt(0)!=word.charAt(indiceA) || word.charAt(indiceA)=='\\' ) {
                           if( word.charAt(indiceA)=='\0' ) return false;
                           else indiceA++;
                       }
                   }
               } else return false;
           }
       }
       return true;
   }

   /**
    * Arrondit en travaillant sur la representation String
    * @param x Le nombre a arrondir
    * @param p Le nombre de decimales souhaitees
    * @return
    */
   static public String myRound(String x) { return myRound(x,0); }
   static public String myRound(String x,int p) {
      
      // Problème en cas de notation scientifique
      
      char a[] = x.toCharArray();
      char b[] = new char[a.length];
      int j=0;
      int mode=0;     
      
      int len=x.indexOf('E');
      if( len<0 ) len=x.indexOf('e');
      
      int n = len<0 ? a.length : len;

      for( int i=0; i<n; i++ ) {
         switch(mode) {
            case 0: if( a[i]=='.' ) {
                       if (p == 0)  return new String(b,0,j);
                       mode = 1;
                  }
                  b[j++]=a[i];
                  break;
            case 1: p--;
                  if( p==0 ) mode=2;
                  if( i+1<a.length && Character.isDigit(a[i+1]) && a[i+1]>='5' ) {
                    b[j++]=a[i]++;
                  } else b[j++]=a[i];
                  break;
            case 2:
                  if( Character.isDigit(a[i])) break;
                  mode=3;
            case 3:
                  b[j++]=a[i];
                  break;
         }
      }

      String s = new String(b,0,j);
      if( len>=0 ) return s+x.substring(len);
      return s;
      
   }
   
   /**
    * Tokenizer spécialisé : renvoie le tableau des chaines séparés par sep ssi freq(c1) dans s == freq(c2) dans s
    * exemple : tokenize...("xmatch 2MASS( RA , DE ) GSC( RA2000 , DE2000 )", ' ', '(', ')' ) renvoie :
    * {"xmatch" , "2MASS( RA , DE )", "GSC( RA2000 , DE2000 )"} 
    * Le délimiteur n'est pas considéré comme un token
    * @param s
    * @param sep ensemble des délimiteurs
    * @param c1
    * @param c2
    * @return
    */
	static public String[] split(String s, String sep, char c1, char c2, boolean trim) {
		if( s==null ) return null;
		char[] c = s.toCharArray();

		Vector v = new Vector();
		StringBuffer sb = new StringBuffer();
		int nbC1 = 0;
		int nbC2 = 0;
		
		for( int i=0; i<c.length; i++ ) {
			if( c[i]==c1 ) nbC1++;
			if( c[i]==c2 ) nbC2++;
			
			if( sep.indexOf(c[i])>=0 && nbC1==nbC2 ) {
				if( sb.length()>0 ) v.addElement(trim?sb.toString().trim():sb.toString());
				sb = new StringBuffer();
				continue;
			}
			
			sb.append(c[i]);
			
		}
		
		// ajout du dernier élément
		if( sb.length()>0 ) v.addElement(trim?sb.toString().trim():sb.toString());
		
		String[] tokens = new String[v.size()];
		v.copyInto(tokens);
		v = null;
		return tokens;
	}
	
	static public String[] split(String s, String sep) {
		return split(s,sep,'@','@');
	}
	
	static public String[] split(String s, String sep, char c1, char c2) {
		return split(s,sep,c1,c2,false);
	}
	
	/** Utilitaire pour ajouter des blancs après un mot afin de lui donner une taille particulière
	 * @param key le mot à aligner
	 * @param n le nombre de caractères souhaités
	 * @return le mot aligné, ou si trop grand, avec juste un espace derrière
	 */
	static public String align(String key,int n) { return align(key,n,""); }
    static public String align(String key,int n,String suffixe) {
		int i=key.length();
		if( i>=n ) return key+ suffixe +" ";
		StringBuffer s = new StringBuffer();
		for( int j=0; j<n-i; j++ ) s.append(' ');
		return key+suffixe+s;
	}
	
	/** Utilitaire pour ajouter des zéros avant un nombre pour l'aligner sur 3 digits
	 * @param x la valeur à aligner
	 * @return le nombre aligné
	 */
	static public String align(int x) {
		if( x<10 ) return "00"+x;
		else if( x<100 ) return "0"+x;
		else return ""+x;
	}
	
	/** Arrondit et limite le nombre de décimales
	 * @param d nombre à arrondir
	 * @param nbDec nb de décimales à conserver
	 * @return le nombre arrondi en conservant nbDec décimales
	 */
	public static double round(double d, int nbDec) {
		double fact = Math.pow(10,nbDec);
		return Math.round(d*fact)/fact;
	}
	
	/**
	 * Utilitaire pour insérer des \n dans un texte afin de replier les lignes
	 * @param s Le texte à "folder"
	 * @param taille le nombre maximum de caractères par ligne (80 par défaut)
	 * @return le texte avec les retours à la ligne
	 */
	static public String fold(String s) { return fold(s,80); }
	static public String fold(String s,int taille) {
		char a[] = s.toCharArray();
		int i,j,n;
		for( i=j=n=0; i<a.length; i++,n++ ) {
			if( a[i]==' ') j=i;		// Je mémorise l'emplacement du dernier blanc
			if( n>taille && j!=0 ) { a[j]='\n'; n=j=0; } // J'insère un retour à la ligne
			if( a[i]=='\n' ) n=0;
        }
		return new String(a);
	}
	
	/**
	 * 
	 * @param c couleur dont on veut la couleur inverse
	 * @return
	 */
    static public Color getReverseColor(Color c) {
    	if( c==null ) return null;
    	return new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());
    }
    
    static final Color CEBOX = new Color(172,168,153);
    static final Color CIBOX = new Color(113,111,100);
    
    /**
     * Dessine les bords d'un rectangle avec un effet de volume
     * @param g Le contexte graphique concerné
     * @param w la largeur
     * @param h la hauteur
     */
    static public void drawEdge(Graphics g,int w,int h) {
       g.setColor(CEBOX);
       g.drawLine(0,0,w-1,0); g.drawLine(0,0,0,h-1);
       g.setColor(CIBOX);
       g.drawLine(1,1,w-1,1); g.drawLine(1,1,1,h-1);
       g.setColor(Color.white);
       g.drawLine(w-1,h-1,0,h-1); g.drawLine(w-1,h-1,w-1,0);
    }

    
    /**
     * Pause du thread courant
     * @param ms temps de pause en millisecondes
     */
    static public void pause(int ms) {
       try { Thread.currentThread().sleep(ms); }
       catch( Exception e) {}
    }
	
    /**
     * Decodeur HTTP
     * Temporairement necessaire car URLDecoder n'apparait que dans la JVM 1.2
     */
    public static String myDecode(String s) {
       char a[] = s.toCharArray();
       char d[] = new char[2];
       StringBuffer b = new StringBuffer(a.length);
       char c;
       int mode=0;

       for( int i=0; i<a.length; i++) {
          c=a[i];
          switch(mode ) {
             case 0: // Copie simple
                if( c!='%' ) { b.append(c=='+'?' ':c); break; }
                else mode=1;
                break;
             case 1:
                d[0]=c;
                mode=2;
                break;
             case 2:
                d[1]=c;
                c = (char)(Integer.parseInt(new String(d),16));
                b.append(c);
                mode=0;
          }
       }

       return b.toString();
    }
	/**
	 * Cherche un objet dans un tableau et retourne l'indice correspondant
	 * @param o objet à trouver
	 * @param array tableau dans lequel on recherche
	 * @return premier indice de o dans array, -1 si non trouvé
	 */
	static public int indexInArrayOf(Object o, Object[] array) {
		if( o==null || array==null ) return -1;
		
		for( int i=0; i<array.length; i++ ) {
			if( o.equals(array[i]) ) return i;
		}
		return -1;
	}
    
    /** Recherche la position d'une chaine dans un tableau de chaine
     * @param s la chaine à chercher
     * @param array le tableau de chaines
     * @param caseInsensitive true si on ignore la distinction maj/min
     * @return position ou -1 si non trouvé
     */
    static public int indexInArrayOf(String s,String[] array) { return indexInArrayOf(s,array,false); }
    static public int indexInArrayOf(String s,String[] array,boolean caseInsensitive) {
       if( s==null || array==null ) return -1;
    	
       for( int i=0; i<array.length; i++ ) {
          if( !caseInsensitive && s.equals(array[i])
            || caseInsensitive && s.equalsIgnoreCase(array[i]) ) return i;
       }
       return -1;
    }
    
    /** Recherche la position d'un mot dans une chaine en ignorant la case */
    static public int indexOfIgnoreCase(String s,String w) {
       s = toUpper(s);
       w = toUpper(w);
       return s.indexOf(w);
    }
	
	/**
	 * Remplit une chaine avec des blancs jusqu'à obtenir la longueur désirée
	 * @param s
	 * @param totLength
	 * @return String
	 */
	static public String fillWithBlank(String s, int totLength) {
		StringBuffer sb = new StringBuffer(s);
		for( int i=s.length(); i<totLength; i++ ) {
			sb.append(" ");
		}
		return sb.toString();
	}
	
    
    /** Conversion en majuscules d'une chaine */
	static public String toUpper(String s) {
	   char a[] = s.toCharArray();
	   for( int i=0; i<a.length; i++ ) a[i] = Character.toUpperCase(a[i]);
	   return new String(a);
	}
    
	static private String HEX = "0123456789ABCDEF";
    
    /** Affichage en hexadécimal d'un caractère */
    static public String hex(char c) { return hex((int)c); }
    
    /** Affichage en hexadécimal d'un octet */
    static public String hex(int b) {
	   return ""+HEX.charAt(b/16)+HEX.charAt(b%16);
	}


// PAS ENCORE TESTE    
//    /** Extrait le premier nombre entier qui se trouve dans la chaine à partir
//     * d'une certaine position
//     * Ne prend pas en compte un signe éventuel
//     * @param s la chaine à traiter
//     * @param pos la position de départ
//     * @return le nombre trouvé, ou 0 si aucun
//     */
//    public int getInteger(String s) { return getInteger(s,0); }
//    public int getInteger(String s,int pos) {
//       int i;
//       int n=s.length();
//       for( i=pos; i<n && !Character.isDigit(s.charAt(i)); i++);
//
//       int val;
//       for( val=0; i<n && Character.isDigit(s.charAt(i)); i++) {
//          val = val*10 + (int)(s.charAt(i)-'0');
//       }
//       
//       return val;
//    }
       

}
