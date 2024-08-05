package uk.ac.starlink.ttools.calc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.MetadataStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.URLUtils;

/**
 * Column calculator which interrogates the IPAC Dust Extinction map service.
 *
 * @see <a href="http://irsa.ipac.caltech.edu/applications/DUST/"
 *         >IPAC Galactic Dust Reddening and Extinction Service</a>
 * @see <a href="http://adsabs.harvard.edu/cgi-bin/basic_connect?qsearch=1998ApJ...500..525S"
           >1998ApJ...500..525S</a>
 */
public class SchlegelCalculator
        extends MultiServiceColumnCalculator<SchlegelCalculator.Spec> {

    /** Base URL for IPAC Dust Extinction map service. */
    public static final String SERVICE_URL =
        "http://irsa.ipac.caltech.edu/cgi-bin/DUST/nph-dust?";

    /** Statistic used by default for output. */
    public static final Statistic DEFAULT_STAT = Statistic.MEAN;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.calc" );

    public ValueInfo[] getTupleInfos() {
        DefaultValueInfo raInfo = new DefaultValueInfo( Tables.RA_INFO );
        DefaultValueInfo decInfo = new DefaultValueInfo( Tables.DEC_INFO );
        raInfo.setUnitString( "deg" );
        decInfo.setUnitString( "deg" );
        return new ValueInfo[] { raInfo, decInfo, };
    }

    /**
     * Returns the URL from which the results document can be read.
     *
     * @param   spec  calc specification object
     * @param  tuple  input tuple
     * @return   URL, or null if the query is bound to fail
     */
    private static String getQueryUrl( Spec spec, Object[] tuple ) {
        double ra = getRangedValue( tuple[ 0 ], -180, 360 );
        double dec = getRangedValue( tuple[ 1 ], -90, 90 );
        if ( Double.isNaN( ra ) || Double.isNaN( dec ) ) {
            return null;
        }
        else {
            return new StringBuffer()
                .append( SERVICE_URL )
                .append( "locstr=" )
                .append( CgiQuery.formatDouble( ra, 6, 16 ) )
                .append( "+" )
                .append( CgiQuery.formatDouble( dec, 6, 16 ) )
                .append( "+equ+j2000" )
                .toString();
        }
    }

    public ServiceOperation createServiceOperation( final Spec spec ) {
        return new ServiceOperation() {
            public StarTable getResultMetadata() {
                ValueInfo[] resultInfos = spec.getResultInfos();
                int ncol = resultInfos.length;
                ColumnInfo[] colInfos = new ColumnInfo[ ncol ];
                for ( int ic = 0; ic < ncol; ic++ ) {
                    colInfos[ ic ] = new ColumnInfo( resultInfos[ ic ] );
                }
                StarTable meta = new MetadataStarTable( colInfos );
                meta.getParameters()
                    .addAll( Arrays.asList( createServiceParams() ) );
                return meta;
            }
            public Object[] calculateRow( Object[] tuple ) throws IOException {
                return spec.calculateRow( tuple );
            }
        };
    }

    /**
     * Return fixed information associated with results from the IPAC service.
     */
    private static DescribedValue[] createServiceParams() {
        return new DescribedValue[] {
            new DescribedValue(
                new DefaultValueInfo( "Schlegel_Service", String.class, null ),
                "Schlegel parameters from " + SERVICE_URL ),
            new DescribedValue(
                new DefaultValueInfo( "Schlegel_Paper", String.class, null ),
                "D.J. Schlegel, D.P. Finkbeiner, & M. Davis "
              + "(1998, ApJ, 500, 525)" ),
            new DescribedValue(
                new DefaultValueInfo( "IPAC_Acknowledgement", String.class,
                                      null ),
                "This research has made use of "
              + "the NASA/IPAC Infrared Science Archive, which is operated by "
              + "the Jet Propulsion Laboratory, "
              + "California Institute of Technology, under contract with "
              + "the National Aeronautics and Space Administration." ),
        };
    }

    /**
     * Specifies the dust query to be made.
     * It specifies what results are to be retrieved, just aggregating a
     * list of ResultTypes and Statistics.
     * It does not specify the area; althoug the IPAC service allows you
     * to submit that (regSize parameter), it does not appear to have
     * any effect on the results, apart from the cutout image URLs, which
     * are not used here.  So there doesn't seem to be much point.
     */
    public static class Spec {
        private final ResultType[] rtypes_;
        private final Statistic[] stats_;
        private final DocumentBuilderFactory dbf_;

        /**
         * Constructor.
         *
         * @param  rtypes   result types to be retrieved
         * @param  stats    statistic values to be retrieved
         */
        public Spec( ResultType[] rtypes, Statistic[] stats ) {
            rtypes_ = rtypes;
            stats_ = stats;
            dbf_ = DocumentBuilderFactory.newInstance();
        }

        public ValueInfo[] getResultInfos() {
            List<ValueInfo> infoList = new ArrayList<ValueInfo>();
            for ( int ir = 0; ir < rtypes_.length; ir++ ) {
                ResultType rtype = rtypes_[ ir ];
                for ( int is = 0; is < stats_.length; is++ ) {
                    Statistic stat = stats_[ is ];
                    infoList.add( rtype.createInfo( stat ) );
                }
            }
            return infoList.toArray( new ValueInfo[ 0 ] );
        }

        /**
         * Retrieves the result for a given input tuple
         * and turns it into an output tuple.
         *
         * @param  tuple  input values
         * @return   output values
         */
        Object[] calculateRow( Object[] tuple ) throws IOException {
            Float[] row = new Float[ rtypes_.length * stats_.length ];
            String url = getQueryUrl( this, tuple );
            if ( url == null ) {
                return row;
            }
            Element resultsEl = getOkResultsElement( URLUtils.newURL( url ) );
            int ic = 0;
            for ( int ir = 0; ir < rtypes_.length; ir++ ) {
                ResultType rtype = rtypes_[ ir ];
                Map<String,Float> statsMap = rtype.getStatsMap( resultsEl );
                for ( int is = 0; is < stats_.length; is++ ) {
                    Statistic stat = stats_[ is ];
                    row[ ic++ ] = statsMap.get( stat.elName_ );
                }
            }
            assert ic == row.length;
            return row;
        }

        /**
         * Gets a &lt;results&gt; element from a dust service URL.
         * If the query failed, either the result document could not
         * be retrieved/parsed, or the results status="error", 
         * an exception is thrown.
         *
         * @param  url   query URL
         * @return  result-bearing document
         */
        private Element getOkResultsElement( URL url ) throws IOException {
            logger_.info( url.toString() );
            Document doc;
            try {
                doc = dbf_.newDocumentBuilder().parse( url.openStream() );
            }
            catch ( ParserConfigurationException e ) {
                throw (IOException) new IOException( "XML parsing trouble" )
                                   .initCause( e );
            }
            catch ( SAXException e ) {
                throw (IOException)
                      new IOException( "XML Parse failure for " + url )
                     .initCause( e );
            }
            Element el = doc.getDocumentElement();
            if ( ! "results".equals( el.getTagName() ) ) {
                throw new IOException( "Result from Schlegel service is not "
                                     + "<results>" );
            }
            String status = el.getAttribute( "status" );
            if ( "error".equals( status ) ) {
                NodeList msgEls = el.getElementsByTagName( "message" );
                final String message;
                if ( msgEls.getLength() == 1 &&
                     msgEls.item( 0 ) instanceof Text ) {
                    message = ((Text) msgEls.item( 0 )).getWholeText().trim();
                }
                else {
                    message = "<no message available>";
                }
                throw new IOException( "Schlegel query failed: " + message
                                     + "(" + url + ")" );
            }
            else if ( "ok".equals( status ) ) {
                return el;
            }
            else {
                logger_.warning( "Unknown status \"" + status
                               + "\" for results element" );
                return el;
            }
        }
    }

    /**
     * Returns a numeric value within a given range, or NaN if it's not.
     *
     * @param  obj  value, assumed numeric
     * @param  min  minimum permissible value
     * @param  max  maximum permissible value
     * @return  numeric value between min and max, or NaN
     */
    private static double getRangedValue( Object obj, double min, double max ) {
        if ( obj instanceof Number ) {
            double dval = ((Number) obj).doubleValue();
            return dval >= min && dval <= max ? dval : Double.NaN;
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Enumerates statistics available from the IPAC dust service.
     */
    public static enum Statistic {

        /** Mean value. */
        MEAN( "meanValue", "mean", "mean value" ),

        /** Value at reference pixel. */
        REF_PIXEL( "refPixelValue", "refpix", "value at reference pixel" ),

        /** Standard deviation. */
        STD( "std", "std", "standard deviation" ),

        /** Maximum value. */
        MAX( "maxValue", "max", "maximum value" ),

        /** Minimum value. */
        MIN( "minValue", "min", "minimum value" );
        
        private final String elName_;
        private final String nickName_;
        private final String desc_;

        /**
         * Constructor.
         *
         * @param   elName  name of the XML element child of statistics
         * @param  nickName  short name
         * @param  desc  plain text description
         */
        Statistic( String elName, String nickName, String desc ) {
            elName_ = elName;
            nickName_ = nickName;
            desc_ = desc;
        }
    }

    /**
     * Enumerates physical values available from the IPAC dust service.
     */
    public static enum ResultType {

        /** E(B-V) reddening. */
        REDDENING( "Reddening", "E(B-V) Reddening", "mag" ),

        /** 100 micron emission. */
        EMISSION( "Emission", "100 Micron Emission", "MJy/sr" ),

        /** Dust temperature. */
        TEMPERATURE( "Temperature", "Dust Temperature", "K" );

        private final String title_;
        private final String fullDesc_;
        private final String unit_;
        private final Pattern valueRegex_;

        /**
         * Constructor.
         *
         * @param  title   column heading
         * @param  fullDesc   plain text description
         * @param  unit   units, as given in statistic text in XML
         */
        ResultType( String title, String fullDesc, String unit ) {
            title_ = title;
            fullDesc_ = fullDesc;
            unit_ = unit;
            valueRegex_ = Pattern.compile( "\\s*([0-9eE+.\\-]*)\\s*\\("
                                         + unit.trim() + "\\)\\s*" );
        }

        /**
         * Returns a metadata item describing a given statistic for this
         * result type.
         *
         * @param   stat  statistic
         */
        ValueInfo createInfo( Statistic stat ) {
            String name = title_;
            if ( stat != DEFAULT_STAT ) {
                name += "_" + stat.nickName_;
            }
            String desc = "Schlegel " + fullDesc_ + " " + stat.desc_;
            DefaultValueInfo info =
                new DefaultValueInfo( name, Float.class, desc );
            info.setUnitString( unit_ );
            return info;
        }

        /**
         * Interrogates a &lt;results;gt; element to find named children
         * which are annotated with the units known by this result type.
         * The result is a mapping from statistic name to value.
         * 
         * @param  resultsEl  any element containing result elements
         * @return  stat->value map
         */
        Map<String,Float> getStatsMap( Element resultsEl ) {
            Map<String,Float> statsMap = new LinkedHashMap<String,Float>();
            Element statsEl = getStatsElement( resultsEl );
            if ( statsEl != null ) {
                for ( Node node = statsEl.getFirstChild();
                      node != null; node = node.getNextSibling() ) {
                    String txt = getTextContent( node );
                    if ( txt != null ) {
                        Float value = getTypedValue( txt );
                        if ( value != null ) {
                            statsMap.put( node.getNodeName(), value );
                        }
                    }
                }
            }
            return statsMap;
        }

        /**
         * Returns the &lt;statistics&gt; element corresponding to this
         * result type under the given results element.
         *
         * @param  resultsEl  any element containing result elements
         * @return  statistics element, or null
         */
        private Element getStatsElement( Element resultsEl ) {
            NodeList rlist = resultsEl.getElementsByTagName( "result" );
            for ( int ir = 0; ir < rlist.getLength(); ir++ ) {
                Element rEl = (Element) rlist.item( ir );
                NodeList dlist = rEl.getElementsByTagName( "desc" );
                for ( int id = 0; id < dlist.getLength(); id++ ) {
                    Element dEl = (Element) dlist.item( id );
                    if ( fullDesc_.trim()
                        .equalsIgnoreCase( getTextContent( dEl ).trim() ) ) {
                        NodeList slist =
                            rEl.getElementsByTagName( "statistics" );
                        if ( slist.getLength() == 1 ) {
                            return (Element) slist.item( 0 );
                        }
                        else {
                            return null;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Turns a text string (content of a statistic element) into a
         * numeric value suitable for this result type.  It's only recognised
         * if it is annotated with the right unit.
         * Otherwise, null is returned.
         *
         * @param  txt  element content
         * @return   numeric value in this result type's units, or null
         */
        private Float getTypedValue( String txt ) {
            Matcher matcher = valueRegex_.matcher( txt );
            if ( matcher.matches() ) {
                try {
                    return Float.valueOf( matcher.group( 1 ) );
                }
                catch ( NumberFormatException e ) {
                    assert false;
                    return null;
                }
            }
            else {
                return null;
            }
        }
    }

    /**
     * Returns the text content of an element, or null.
     *
     * @param  node  node 
     * @return  text content if node is an element, else null
     */
    private static String getTextContent( Node node ) {
        return node instanceof Element
             ? DOMUtils.getTextContent( (Element) node )
             : null;
    }

    /**
     * Diagnostic.
     */
    public static void main( String[] args ) throws IOException {
        if ( args.length == 0 ) {
            args = new String[] { "137", "-23.4" };
        }
        Double[] tuple = new Double[] {
            Double.valueOf( args[ 0 ] ),
            Double.valueOf( args[ 1 ] )
        };
        ResultType[] rtypes = new ResultType[] {
            ResultType.REDDENING,
            ResultType.EMISSION,
        };
        Statistic[] stats = new Statistic[] {
            Statistic.MEAN,
            Statistic.STD,
        };
        Spec spec = new Spec( rtypes, stats );
        ValueInfo[] infos = spec.getResultInfos();
        Object[] results = spec.calculateRow( tuple );
        for ( int ic = 0; ic < infos.length; ic++ ) {
            System.out.println( infos[ ic ] + " = " + results[ ic ] + " ("
                              + infos[ ic ].getDescription() + ")" );
        }
    }
}
