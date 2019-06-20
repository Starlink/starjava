package uk.ac.starlink.ttools.calc;

import junit.framework.TestCase;

public class WebMapperTest extends TestCase {

    public void testMappers() {
        exerciseMapper( WebMapper.BIBCODE,
            new String[] {
                "1974AJ.....79..819H",
                "1924MNRAS..84..308E",
                "1970ApJ...161L..77K",
                "2004PhRvL..93o0801M",
                "2015ApJS..217...27A",
                "2015ApJ...800...80L",
                "2001ApJ...560..566K",
                "2013AJ....146..151O",
                "2013MNRAS.432.3141C",
                "2015MNRAS.446.3749Y",
                "2015ApJS..217...27A",
                "2013MNRAS.434..336H",
                "2015ApJS..217...27A",
                "2014A&A...569A.124V",
                "1985AJ.....90.1681B",
            },
            new String[] {
                "201ApJS..217...27A",
                "201ApJS..217...2703A",
                "xxxxxxxxxxxxxxxxxxx",
                "3015ApJS..xxx...27A",
                "no",
            }
        );
        exerciseMapper( WebMapper.DOI,
            new String[] {
                "10.1000/182",
                "10.1093/mnras/212.3.601",
                "10.1038/nature15751",
                "10.1000.23/xxx",
                "doi:10.1088/0004-637X/742/2/83",
                "doi:10.1046/j.1365-8711.1999.02688.x",
                "doi:10.1086/112635",
                "doi:10.5479/ADS/bib/1906LicOB.4.98W",
            },
            new String[] {
                "10.1011",
                "10.100/99",
                "dob:10.1000/182",
                "doi:/10.1000/182",
                "11.1000/182",
                "10.1000x/99",
            }
        );
        exerciseMapper( WebMapper.ARXIV,
            new String[] {
                "arXiv:1501.00001",
                "arXiv:0706.0001",
                "arXiv:1501.00001v1",
                "arXiv:0706.0001v2",
                "math.GT/0309136",
                "arXiv:math.GT/0309136",
                "arXiv:quant-ph/0112172",
                "arXiv:astro-ph/0601006",
            },
            new String[] {
                "archiv:1501.00001",
                "arXiv.1201.00002",
                "arXiv:5508.00001",
                "arXiv:1213.00001",
                "moth.GT/0309136",
                "moth/0309136",
            }
        );
    }

    private void exerciseMapper( WebMapper mapper,
                                 String[] yesTxts, String[] noTxts ) {
        assertNull( mapper.toUrl( "" ) );
        assertNull( mapper.toUrl( "         " ) );
        for ( String txt : yesTxts ) {
            assertNotNull( mapper + " false negative: " + txt,
                           mapper.toUrl( txt ) );
        }
        for ( String txt : noTxts ) {
            assertNull( mapper + " false positive:" + txt,
                        mapper.toUrl( txt ) );
        }
    }
}
