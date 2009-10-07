//
// Copyright 1999-2002 - Universite Louis Pasteur / Centre National de la
// Recherche Scientifique
//
// ------
//
// CDS XML Parser
//
// Author:  Pierre Fernique
// Address: Centre de Donnees astronomiques de Strasbourg
//          11 rue de l'Universite
//          67000 STRASBOURG
//          FRANCE
// Email:   fernique@astro.u-strasbg.fr, question@simbad.u-strasbg.fr
//
// -------
//
// In accordance with the international conventions about intellectual
// property rights this software and associated documentation files
// (the "Software") is protected. The rightholder authorizes :
// the reproduction and representation as a private copy or for educational
// and research purposes outside any lucrative use,
// subject to the following conditions:
//
// The above copyright notice shall be included.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON INFRINGEMENT,
// LOSS OF DATA, LOSS OF PROFIT, LOSS OF BARGAIN OR IMPOSSIBILITY
// TO USE SUCH SOFWARE. IN NO EVENT SHALL THE RIGHTHOLDER BE LIABLE
// FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
// THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// For any other exploitation contact the rightholder.
//
//                        -----------
//
// Conformement aux conventions internationales relatives aux droits de
// propriete intellectuelle ce logiciel et sa documentation sont proteges.
// Le titulaire des droits autorise :
// la reproduction et la representation a titre de copie privee ou des fins
// d'enseignement et de recherche et en dehors de toute utilisation lucrative.
// Cette autorisation est faite sous les conditions suivantes :
//
// La mention du copyright portee ci-dessus devra etre clairement indiquee.
//
// LE LOGICIEL EST LIVRE "EN L'ETAT", SANS GARANTIE D'AUCUNE SORTE.
// LE TITULAIRE DES DROITS NE SAURAIT, EN AUCUN CAS ETRE TENU CONTRACTUELLEMENT
// OU DELICTUELLEMENT POUR RESPONSABLE DES DOMMAGES DIRECTS OU INDIRECTS
// (Y COMPRIS ET A TITRE PUREMENT ILLUSTRATIF ET NON LIMITATIF,
// LA PRIVATION DE JOUISSANCE DU LOGICIEL, LA PERTE DE DONNEES,
// LE MANQUE A GAGNER OU AUGMENTATION DE COUTS ET DEPENSES, LES PERTES
// D'EXPLOITATION,LES PERTES DE MARCHES OU TOUTES ACTIONS EN CONTREFACON)
// POUVANT RESULTER DE L'UTILISATION, DE LA MAUVAISE UTILISATION
// OU DE L'IMPOSSIBILITE D'UTILISER LE LOGICIEL, ALORS MEME
// QU'IL AURAIT ETE AVISE DE LA POSSIBILITE DE SURVENANCE DE TELS DOMMAGES.
//
// Pour toute autre utilisation contactez le titulaire des droits.
//

package cds.xml;


import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.*;

/** XML parser.
 * Simple XML parser for well-formed XML documents in ASCII, without validation
 * Resolves automatically &amp; &quot; &apos; &lt; &gt;.
 * Compacts blanks in one space (doesn't take into account SPACE XMl declaration.
 * Assumes that the XML document begins with <?xml...> or <?XML...>.
 *
 * It uses 3 methodes to interact with a XML event consumer (similar to the
 * XML SAX interface) -> startElement(), endElement() and characters() but the
 * startElement() send an Hashtable for the tag parameters instead of the
 * AttrList SAX object ==> See the XmlConsumer java interface to have an
 * example of usage
 *
 * The in(nameList) method allows one to know at any time if the current
 * parser state correspond to a particular XML levels
 *
 * @version 1.7 juin 07 gère les CDATA dans les tags
 * @version 1.6 oct 05 gère (ou plutôt ignore) les namespace VOTable + encodage correcte
 * @version 1.5 7 juillet 05 parse(dis,endTag) pour un parsing partiel
 * @version 1.4 19 jan 01 mode 5 in getString fixed
 * @version 1.3 27 apr 00 Doesn't transmit <?xml...> prefix
 * @version 1.2 12 apr 00 Take into account large escape sections
 * @version 1.1 13 mar 00 Take into account more XML errors.
 * @version 1.0 03 sep 99 Creation
 * @author P.Fernique [CDS]
 * @Copyright ULP/CNRS 2000
 */
public final class XMLParser {
   private XMLConsumer ac;		// XML event consumer
   private InputStream  dis;	// input stream
   private Stack stack;			// XML tag stack
   public int nstack;
   private String name;			// Current tag name
   private Hashtable param;		// Params of the current tag
   private char [] ch;			// work buffer
   private int start,length,end;// index on the work buffer ch[]
   private byte [] tmp;			// reader buffer
   private int offset,max;		// index on the reader buffer tmp[]
   private String error;		// error report
   private boolean beforeXML;	// true if the parser waits <?xml...> tag
   private String endTag;	    // Contient le tag de fin de parsing (si parsing partiel)
   private int line;            // Ligne courante (en cas d'erreur)

   static final int BUFSIZE = 64*1024;	// Reader buffer size
   static final int MAXBUF  = BUFSIZE;	// number of chars before a flush

//   private boolean formatQuest = false;

   // Default macros and their equivalence
   static final String [] mKey   = { "&amp;","&gt;","&lt;","&apos;","&quot;" };
   static final String [] mValue = { "&",    ">",   "<",   "'",     "\""      };

  /** Create a new XMLParser object
   * @param ac an object implementing the XMLConsumer interface
   * @param withStack true si on gère une pile des tags (méthode in() et getStack() possible)
   */
   public XMLParser(XMLConsumer ac) { this(ac,false); }
   public XMLParser(XMLConsumer ac,boolean withStack) {
      this.ac = ac;
      stack = withStack ? new Stack() : null;
      nstack=0;
      param = new Hashtable();
      tmp = new byte[BUFSIZE];
      offset=max=0;
      beforeXML=true;
      error=null;
   }

  /** Launch the XML parsing.
   * In case of errors, the report is accessible by getError()
   * @param dis the input stream
   * @return true or false according to the parsing result
   * @throws Exception
   */
   public boolean parse(InputStream dis) throws Exception {
//      formatQuest = false;
      this.dis = dis;
      endTag=null;
      line=0;
//      setTestBeforeXML(dis);
      boolean rep;
      try { rep=xmlBeforeTag(); }
      catch( Exception e ) {
         throw e;
      }
      try { if( dis!=null ) dis.close(); } catch( Exception e) {}
      return rep;
   }

   /** Lancement du parsing XML pour une sous-section. Attention, on ne ferme
    * pas le flux
    * En cas d'erreur, utiliser getError()
    * @param dis le stream à parser
    * @param endTag le TAG XML qui déterminera la fin du parsing partiel
    * @return true ou false suivant le parsing
    * @throws Exception
    */
    public boolean parse(InputStream dis,String endTag) throws Exception {
//       formatQuest = false;
       this.dis = dis;
       this.endTag=endTag;
       line=0;
//       setTestBeforeXML(dis);
       boolean rep;
       try { rep=xmlBeforeTag(); }
       catch( Exception e ) {
          throw e;
       }
       return rep;
    }
    
//    private void setTestBeforeXML(MyInputStream dis) throws Exception {
//       beforeXML = (dis.getType() & MyInputStream.VOTABLE) ==0; 
//    }

  /** Return the stack of the tag name.
   *@return The stack of the XML tag name
   */
   public Stack getStack() throws Exception {
      if( stack==null ) throw new Exception("No XML stack");
      return stack;
   }   
   
   public int getDepth() { return nstack; }

  /** Return true if the tag name list in parameter is found
   * in the XML stack (with the same order) and ends with the last
   * tag name in the list.
   * @param nameList List of tag names, separated by spaces
   * @return true if all names are found in the XML stack respecting
   *              the same order
   */
   public boolean in(String nameList) throws Exception {
      if( stack==null ) throw new Exception("No XML stack");
      Enumeration e = stack.elements();
      StringTokenizer st = new StringTokenizer(nameList,", /.\t\n\r\f");

      // Name list loop
      while( st.hasMoreTokens() ) {
         String s=st.nextToken();
         boolean found=false;

         // Stack loop
         while( e.hasMoreElements()
            && !(found=s.equals((String)e.nextElement())) );
         if( !found ) return false;
      }
      return !e.hasMoreElements();
   }

  /** Return the error report.
   *@return The string containing the error report
   *        or null if there isn't
   */
   public String getError() { return error; }

  /** Set error message
   * @param s
   */
   private void setError(String s) { error=s+"\n"; }

  /** Encode XML macros
   * @param s the string to encode
   * @return the string encoded
   */
   static public String XMLEncode(String s) {
      char [] a = s.toCharArray();
      StringBuffer b = new StringBuffer();
      int j;

      for( int i=0; i<a.length; i++ ) {
         for( j=0; j<mValue.length && mValue[j].charAt(0)!=a[i]; j++);
         if( j<mValue.length ) b.append(mKey[j]);
         else b.append(a[i]);
      }

      return b.toString();
   }

  /** Decode XML macros
   * @param s the string to decode
   * @return the string decoded, null if macro error
   */
   static public String XMLDecode(String s) {
      char [] a = s.toCharArray();
      StringBuffer b = new StringBuffer();	// for the result
      StringBuffer c=null;			// for the current macro
      int mode=0;				// automate mode
      int i;
      

      for( i=0; i<a.length; i++ ) {
         char ch = a[i];
         switch(mode) {
            case 0:	// wait & + copy
               if( ch=='&' ) { c=new StringBuffer(); c.append(ch); mode=1; }
               else b.append(ch);
               break;
            case 1:	// wait ; + memorize the macro
               c.append(ch);
               if( ch==';' ) {
                  int j;
                  String tmp = c.toString();
                  for( j=0; j<mKey.length && !tmp.equals(mKey[j]); j++);
                  if( j<mKey.length ) b.append(mValue[j]);	// found
                  else return null;
                  mode=0;
               }
               break;
         }
      }

      // no end macro
      if( mode==1 ) return null;

      return b.toString();
   }
   
   /** Retourne les caractères non lus du buffer courant, ou null si lecture terminée */
   public byte [] getUnreadBuffer() { 
      if( max== -1 ) return null;
      byte [] buf = new byte[max-offset];
      System.arraycopy(tmp,offset,buf,0,max-offset);
      return buf;
   }

  /** Get the next character from the stream
   *
   * @return char
   */
   private char xmlGetc() {
      if( offset>=max ) {
         try { max=dis.read(tmp); }
         catch( IOException e ) { setError("Stream error: "+e); return 0; }
         if( max==-1 ) return 0;   // end of stream
         offset=0;
      }
      return (char)tmp[offset++];
   }

  /** Return true if the charater is a space (' ', '\t', '\n' or '\r')
   * @param ch
   * @return boolean
   */
//   private static final boolean isSpace(char ch) {
//      return ch==' ' || ch=='\t' || ch=='\n' || ch=='\r';
//   }
   
   static private final char CDATA[] = { '!','[','C','D','A','T','A','[' }; 
   
   
  /** Recherche la chaine courante en fonction du mode du parsing
   * La mémorisation se fait dans le tableau ch[], et les variables start et length.
   * Par défaut les blancs sont concaténés en 1 unique espace et les macros XML sont résolues
   * @param mode 0 jusqu'à '>' ou espace
   *             1 jusqu'à '>'
   *             2 jusqu'à '<' (pas de mémorisation)
   *             3 jusqu'à "]]" (pas de concaténation des blancs ni de résolution des macros)
   *             4 jusqu'à "-->" (pas de concaténation des blancs ni de résolution des macros)
   *             5 jusqu'à "<?xml....>" ou <?XML...>
   *              (pas de concaténation des blancs ni de résolution des macros)
   * @return le dernier caractère ou 0 si fin du stream ou error (=> error!=null)
   * @throws Exception
   */
   private char xmlGetString(int mode) throws Exception {
      StringBuffer a = new StringBuffer(); // Pour mémoriser la chaine courante
      int l=0;				               // taille dans a[]
      int ol=-1;				           // en mode 5, <?xml...> l'offset du suffixe
      char c=0;                            // caractère courant
      char c1=0;				           // caractère courant éventuellement substitué (macro)
      boolean minus,ominus,endminus;	   // pour tester la séquence "--"
      boolean bracket,obracket,endbracket; // pour tester la séquence "]]"
      boolean xml0,xml1,xml2,xml3,xml4;	   // pour tester la séquence <?xml...>"
      boolean space=false;		           // le caractère courant est un espace
      boolean ospace=false;		           // le caractère précédent est un espace
      boolean encore=true;		           // test de fin de boucle
      boolean flagNL=false; 		       // true si le caractère précédent était un \n
      StringBuffer macro=null;             // pour la mémorisation de la macro en cours
      byte cdataPos=0;                     // position dans CDATA String (mode 2 uniquement)
      int omode=-1;                         // mode précédant dans le cas d'un changement temporaire de mode

      minus=ominus=endminus=false;
      bracket=obracket=endbracket=false;
      xml0=xml1=xml2=xml3=xml4=false;

//System.out.print("mode="+mode+": [");
      while( encore && (c1=c=xmlGetc())!=0 ) {
//System.out.print(c);
         
         // Traitement des macros (si besoin est)
//         if( mode<3 && mode>5 && (c=='&' || macro!=null) ) {   // C'est idiot, mais pourquoi ? PF 08/2006
         if( mode<3 && (c=='&' || macro!=null) ) {
            if( macro==null ) macro=new StringBuffer(10);
            macro.append(c);
            if( c!=';' ) continue;
            String m=macro.toString();
            int i;
            for( i=0; i<mKey.length && !m.equals(mKey[i]); i++ );
            if( i<mKey.length ) {
               c1=mValue[i].charAt(0);
            } else { error="unknown macro ["+m+"]"; return 0; }
            macro=null;
         }
         
         space=Character.isSpace(c);
         
         switch(mode) {
            case 0: encore=(!space && c!='>'); break;
            case 1: encore=(c!='>'); break;
            case 2: encore=(c!='<');
            
                    // Sequence CDATA ?
                    if( cdataPos<CDATA.length && c==CDATA[cdataPos] ) cdataPos++;
                    else cdataPos=0;
                    
                    // Flush temporaire pour eviter les out of mem
                    if( l>=MAXBUF && flagNL ) {
                       ch=a.toString().toCharArray();
                       ac.characters(ch,0,ch.length);
                       a = new StringBuffer();
                       ol=l=0;
                    }
                    break;
            case 3: encore = !(endbracket && c=='>');
                    bracket=(c==']');
                    endbracket = (obracket && bracket);
                    obracket=bracket;

                    // Flush temporaire pour eviter les out of mem
                    if( l>=MAXBUF && flagNL ) {
                       ch=a.toString().toCharArray();
                       ac.characters(ch,0,ch.length);
                       a = new StringBuffer();
                       ol=l=0;
                    }
                    break;
            case 4: encore = !(endminus && c=='>');
                    minus=(c=='-');
                    endminus = (ominus && minus);
                    ominus=minus;
                    break;
            case 5: encore = !(xml4 && c=='>');
                    xml4=(xml3 && (c=='l' || c=='L') || xml4);
                    xml3=(xml2 && (c=='m' || c=='M'));
                    xml2=(xml1 && (c=='x' || c=='X'));
                    xml1=(xml0 && c=='?');
                    xml0=(c=='<');
                    if( xml0 ) ol=l;	// Pour ne pas memoriser le <?xml..>
                    break;
         }
         
         if( mode==3 || mode==5 || (!space || space && !ospace) && mode<3 ) {
            if( mode!=3 && mode!=5 && space ) c1=' '; // substitution des blancs
            a.append(c1);
            l++;
         }
         
         // Séquence d'échappement ![CDATA[  ?
         if( mode==2 && cdataPos==CDATA.length ) {
            l-=CDATA.length;
            a.delete(l,l+CDATA.length);  // On supprime la séquence ![CDATA déjà copié
            omode=2; mode=3;             // et on passe en mode sans macro
         }
         if( endbracket && omode==2 ) {
            l-=2;
            a.delete(l,l+2);   // On supprime la séquence ]] déjà chargé
            omode=-1; mode=2;  // et on repasse en mode 2
         }


         ospace=space;
         flagNL=(c=='\n' || c=='\r');
         if( c=='\n' ) line++;
      }

      // Memorization
      if( mode!=4 ) ch=a.toString().toCharArray();
      start=0;
      length=(mode==3)?ch.length-3:(mode==5 && xml4 && ol>=0 )?ol:ch.length-1;
      if( length<0 ) length=0;
//System.out.println("]");
      return c1;
   }

  /** Get from ch[] work buffer the current param name for an XML tag
   * <XXX name=value ...>.
   * At the beginning, the start index points after the XXX tag name.
   * At the end, the start index points to the fist character after the
   * tag name.
   * @return the param name
   */
   private String getNameParam() {

      // skip blanks
      while( start<end && Character.isSpace(ch[start]) ) start++;

      // go until blank or '='
      int a = start;
      while( start<end && !Character.isSpace(ch[start]) && ch[start]!='=' ) start++;

//System.out.println("name=["+(new String(ch,a,start-a))+"]");
      return new String(ch,a,start-a);
   }

  /** Get from ch[] work buffer the current param value for an XML tag
   * <XXX name=value ...>.
   * The param value can be quoted by " or '.
   * At the beginning, the start index points on the blanks or = character
   * before the value.
   * At the end, the start index has been reached the end of the ch buffer
   * @return the param value, null if error
   */
   private String getValueParam() {

      // skip the blanks and/or =
      while( start<end && (Character.isSpace(ch[start]) || ch[start]=='=') ) start++;

      // take into account the end delimiter (' or " or space)
      char stop=' ';	// end character (' ' for any blanks)
      if( start<end && (ch[start]=='"' || ch[start]=='\'') ) {
         stop=ch[start];
         start++;
      }

      // Go until the end of the value
      int a = start;
      while( start<end && ( (stop==' ' && !Character.isSpace(ch[start]))
                         || (stop!=' ' && ch[start]!=stop) ) ) start++;

      // DEJA FAIT DANS xmlGetString()
//      String s = XMLDecode( new String(ch,a,start-a));
//      if( s==null ) { setError("Macro error"); return null; }
      
      int debut=start;

      // Go to the next param
      if( stop!=' ' ) start++;	// skip the quote
      while( start<end && Character.isSpace(ch[start]) ) start++;	// skip the blanks
//System.out.println("value=["+s+"]");
//      return s;
      return new String(ch,a,debut-a);
   }

  /** Récupère le contenu du tag courant
   * @param mode -1 Sans parsing des paramètres et jusqu'à  "--"
   *              0 Sans parsing des paramètres
   *              1 Avec parsing des paramètres => mémorisation dans la Hashtable param
   * @return -1 : error
   *          0 : Ok
   *          3 : C'est un end tag (ex: <name param/>)
   * @throws Exception
   */
   private int xmlGetParamTag(int mode) throws Exception {
      char c;
      int code=0;
      String n,v;	// Nom et valeur de l'attribut courant

      // Récupère la chaine courante en fonction du mode de parsing
      c=xmlGetString(mode==-1?4:1);

       // traitement particulier du tag <XXX/>
      if( length>0 && ch[start+length-1]=='/' ) {
         code=3;
         length--;
      }

      // Y a-t-il une erreur ?
      if( c!='>' ) {
         setError("No end tag");
         return -1;
      }

      // Place le paramètre dans la hashtable
      if( mode==1 ) {
         param.clear();
         end=start+length;
         while( start<end ) {
            n=getNameParam();
            if( (v=getValueParam())==null) return -1;
            param.put(n,v);
         }
      }

      return code;
   }

  /** Recupère le nom du tag (en supprimant un éventuel namespace)
   * @return -1 : erreur
   *          0 : il y a des paramètres après le nom du tag
   *          1 : tag de debut simple <XXX>
   *          2 : tag de fin </XXX>
   *          3 : tag de fin avec attribut <XXX ????/>
   * @throws Exception
   */
   private int xmlGetNameTag() throws Exception {
      char c; 		// le caractère courant
      int code=1;	// le code de retour

      // parse la chaine courante jusqu'à '>' ou espace
      if( (c=xmlGetString(0))==0 ) {
         setError("stream truncated");
         return -1;
      }

      // Traitement du tag </XXX>
      if( length>0 && ch[start]=='/' ) {
         code=2;
         start++;
         length--;

      // Triatement du tag <XXX/>
      } else if( length>1 && ch[start+length-1]=='/' ) {
         code=3;
         length--;
      }

      // Il y a-t-il des paramètres après le nom ?
      if( code==1 && c!='>' ) code=0;
      
      // Supprime un éventuel namespace (cochonnerie de XML)
      int debut=start;
      int longueur=length;
      for( int i=start; i<start+length; i++) if( ch[i]==':' ) { debut=i+1; longueur=length-(i-start)-1; break; }
      
      // Mémorisation du nom du tag
      name = new String(ch,debut,longueur);

      return code;
   }
   
   /**
    * Retourne true si on a terminé un parsing partiel. Teste simplement si le
    * tag courant est égale au endTag indiqué au démarrage du parsing
    * @param tag le tag courant
    * @return true si le parsing partiel est terminé, sinon false
    */
   private boolean partialParsing(String tag) {
      return endTag!=null && endTag.equals(tag);
   }

  /** Parsing du tag courant
    * @return 1 ok, 0 erreur, -1 fin de parsing partiel
    * @throws Exception
    */
   private int xmlInTag() throws Exception {
      int code;		// Code de retour de xmlGetNameTag() et xmlGetParamTag()

      // Récupère le nom du tag
      if( (code=xmlGetNameTag())<0 ) return 0;

      // Analyse d'une séquence d'échappement CDATA
      if( length>=7 && (new String(ch,start,8)).equals("![CDATA[") ) {
         char c=xmlGetString(3);
         
         // normalement le test devrait être "length>0" mais il peut y avoir
         // éventuellement juste un \n dans le cas de lecture par blocs, ce qui fait
         // planter la suite. Comme de toutes façons je ne vois pas une table n'ayant qu'une
         // colonne d'une ligne colonne d'une seul caractère
         if( length>1 ) ac.characters(ch,start,length);
         return c!=0?1:0;
      }

      // Commentaire XML <!-- ... -->  => ignoré
      char c = ch[start];
      if( length>=3 && c=='!'
          && ch[start+1]=='-' && ch[start+2]=='-') return xmlGetParamTag(-1)>=0?1:0;

      // Tags spéciaux <!..., <?... => ignoré
      if( c=='?' || c=='!' ) return xmlGetParamTag(0)>=0?1:0;

      // récupération des paramètres si nécessaire
      if( code==0 ) code=xmlGetParamTag(1);
      else if( code==1 ) param.clear();

//System.out.println("code="+code);

      // Traitement des erreurs
      if( code<0 ) return 0;

      // Fin de tag directement dans le tag => on dépile
      if( code==3 ) {

          // On envoit au consommateur
          // La pile doit être mise à jour au préalable, puis dépilé immédiatement
          if( stack!=null ) stack.push(name);
          nstack++;
          ac.startElement(name,param);
          if( stack!=null ) stack.pop();
          nstack--;
          ac.endElement(name);
          return partialParsing(name)?-1:1;

      // Fin de tag en deux morceaux => On dépile et on compare avec le haut de la pile
      } else if( code==2 ) {

          if( nstack==0 /* stack.empty() */ ) {
             setError("Unexpected end tag (</"+name+">)");
             return 0;
          }
          nstack--;
          if( stack!=null ) {
             String s = (String)stack.pop();
//           System.out.println("pop "+s+" compare to "+name);
             if( !s.equals(name) ) {
                setError("Tags unbalanced (<"+s+">...</"+name+">)");
                return 0;
             }
          }

          // envoi au consommateur
          ac.endElement(name);
          return partialParsing(name)?-1:1;
       }

       // on empile
       if( stack!=null ) stack.push(name);
       nstack++;
//System.out.println("push "+name);

       // envoi au consommateur
       ac.startElement(name,param);
       // if format testing and element found then the parsing must stop
//       if (formatQuest == true) {
//        if (name.compareTo("VOTABLE") == 0 || name.compareTo("ASTRO") == 0)
//          return 0;
//       }
       return 1;
   }

  /** recherche du prochain tag <XXX ...>.
   * Rq: Si le stream ne commence pas par du XML, les caractères seront
   * envoyés au consommateur tels quels
   * @return true ou false en conftion du résultat du parsing XML
   * @throws Exception
   */
   private boolean xmlBeforeTag() throws Exception {
      char c;	// caractère courant

      while(true ) {
         c = xmlGetString(beforeXML?5:2);       	// va au prochain '<'
         if( length>0 && !(length==1 && ch[start]==' '))
               ac.characters(ch,start,length);	   // Envoi au consommateur
         if( beforeXML ) { beforeXML=false; continue; }
         if( c==0 ) return (error==null);	       // Fin du stream
         switch( xmlInTag() ) { 
            case 0: return false;	// Fin sur erreur
            case -1:
               xmlGetString(1); 
//             System.out.println("J'ai fini le parsing pour "+endTag);
               return true;	// Fin d'un parsing partiel
         }
      }
   }
}
