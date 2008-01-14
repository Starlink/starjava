<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="CSS_HREF" select="''"/>
  <xsl:param name="SOFTWARE_VERSION" select="''"/>
  <xsl:param name="DOC_VERSION" select="''"/>

  <!-- Top level element -->

  <xsl:template match="faqdoc">
    <html>
      <head>
        <xsl:call-template name="cssStylesheet"/>
        <title>
          <xsl:apply-templates select="faqinfo/title"/>
          <xsl:text> FAQ</xsl:text>
        </title>
      </head>
      <body>
        <xsl:apply-templates select="faqinfo"/>
        <hr/>
        <h2 align="center">
          <xsl:text>Contents</xsl:text>
        </h2>
        <xsl:apply-templates select="faqbody/faqlist" mode="toc"/>
        <hr/>
        <xsl:apply-templates select="faqbody"/>
      </body>
    </html>
  </xsl:template>

  <!-- Normal processing -->

  <xsl:template match="faqinfo">
    <h1 align="center">
      <xsl:apply-templates select="title"/>
      <xsl:text> Frequently Asked Questions</xsl:text>
    </h1>
    <br/>
    <xsl:text>Software version: </xsl:text>
    <xsl:call-template name="getSoftwareVersion"/>
    <br/>
    <xsl:text>FAQ version: </xsl:text>
    <xsl:call-template name="getDocVersion"/>
    <br/>
    <xsl:apply-templates select="contactlist"/>
  </xsl:template>

 
  <!-- Text elements -->

  <xsl:template match="question">
    <p>
      <xsl:element name="a">
        <xsl:attribute name="name">
          <xsl:call-template name="getId">
            <xsl:with-param name="node" select="."/>
          </xsl:call-template>
        </xsl:attribute>
        <b>
          <xsl:apply-templates mode="ref" select="."/>
          <xsl:text> </xsl:text>
          <xsl:apply-templates select="q"/>
        </b>
      </xsl:element>
    </p>
    <p>
      <xsl:apply-templates select="a"/>
    </p>
    <hr/>
  </xsl:template>

  <xsl:template match="p">
    <p><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="contact">
    <xsl:apply-templates/>
    <br/>
  </xsl:template>

  <xsl:template match="dd/p[position()=1]">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="ul|ol|li|dl|dd|blockquote|code|em|strong|sub|sup">
    <xsl:copy>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="m">
    <i>
      <xsl:apply-templates/>
    </i>
  </xsl:template>

  <xsl:template match="label">
    <b><xsl:apply-templates/></b>
  </xsl:template>

  <xsl:template match="var">
    <i><xsl:apply-templates/></i>
  </xsl:template>

  <xsl:template match="verbatim">
    <pre><xsl:apply-templates/></pre>
  </xsl:template>

  <xsl:template match="blockcode">
    <pre><xsl:apply-templates/></pre>
  </xsl:template>
  <xsl:template match="dt">
    <xsl:copy>
      <strong>
        <xsl:apply-templates/>
      </strong>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="ref">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:call-template name="getRef">
          <xsl:with-param name="node" select="id(@id)"/>
        </xsl:call-template>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="string(.)">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates mode="sectype" select="id(@id)"/>
          <xsl:text> </xsl:text>
          <xsl:apply-templates mode="ref" select="id(@id)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="webref">
    <a href="{@url}">
      <xsl:choose>
        <xsl:when test="string(.)">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@url"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template match="docxref">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:call-template name="docRefUrl">
          <xsl:with-param name="doc" select="@doc"/>
          <xsl:with-param name="loc" select="@loc"/>
        </xsl:call-template>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="string(.)">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="docRefText">
            <xsl:with-param name="doc" select="@doc"/>
            <xsl:with-param name="loc" select="@loc"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <!-- titles -->

  <xsl:template match="sect/subhead/title">
    <hr/>
    <h2 align="center">
      <xsl:element name="a">
        <xsl:attribute name="name">
          <xsl:call-template name="getId">
            <xsl:with-param name="node" select="../.."/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:apply-templates mode="ref" select="../.."/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates/>
      </xsl:element>
    </h2>
  </xsl:template>

  <!-- table of contents -->

  <xsl:template mode="toc" match="faqbody/faqlist">
    <ul>
      <xsl:apply-templates mode="toc" select="sect"/>
    </ul>
  </xsl:template>

  <xsl:template mode="toc" match="title">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template mode="toc" match="sect">
    <li>
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:call-template name="getRef"/>
        </xsl:attribute>
        <xsl:apply-templates mode="ref" select="."/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates mode="toc" select="subhead/title"/>
      </xsl:element>
    </li>
    <ul>
      <xsl:apply-templates mode="toc" select="question"/>
    </ul>
  </xsl:template>

  <xsl:template mode="toc" match="question">
    <li>
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:call-template name="getRef"/>
        </xsl:attribute>
        <xsl:apply-templates mode="ref" select="."/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates select="q"/>
      </xsl:element>
    </li>
  </xsl:template>


  <!-- Section numbering -->

  <xsl:template mode="ref" match="sect">
    <xsl:number count="sect"/>
  </xsl:template>

  <xsl:template mode="ref" match="question">
    <xsl:text>Q</xsl:text>
    <xsl:apply-templates mode="ref" select=".."/>
    <xsl:text>.</xsl:text>
    <xsl:number count="question"/>
  </xsl:template>


  <!-- Section type description -->

  <xsl:template mode="sectype" match="sect">
    <xsl:text>Section</xsl:text>
  </xsl:template>

  <xsl:template mode="sectype" match="question">
    <xsl:text></xsl:text>
  </xsl:template>

  


  <!-- Subroutines -->

  <xsl:template name="cssStylesheet">
    <xsl:if test="$CSS_HREF">
      <xsl:element name="link">
        <xsl:attribute name="rel">stylesheet</xsl:attribute>
        <xsl:attribute name="type">text/css</xsl:attribute>
        <xsl:attribute name="href">
          <xsl:value-of select="$CSS_HREF"/>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>
  </xsl:template>

  <xsl:template name="getSoftwareVersion">
    <xsl:choose>
      <xsl:when test="/faqdoc/faqinfo/softwareversion">
        <xsl:apply-templates select="/faqdoc/faqinfo/softwareversion"/>
      </xsl:when>
      <xsl:when test="$SOFTWARE_VERSION">
        <xsl:value-of select="$SOFTWARE_VERSION"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>???</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="getDocVersion">
    <xsl:choose>
      <xsl:when test="/faqdoc/faqinfo/docversion">
        <xsl:apply-templates select="/faqdoc/faqinfo/docversion"/>
      </xsl:when>
      <xsl:when test="$DOC_VERSION">
        <xsl:value-of select="$DOC_VERSION"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>???</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="getId">
    <xsl:param name="node" select="."/>
    <xsl:choose>
      <xsl:when test="$node/@id">
        <xsl:value-of select="$node/@id"/>
      </xsl:when>
      <xsl:when test="name($node)='faq' or
                      name($node)='faqinfo' or
                      name($node)='faqbody'">
        <xsl:text>this FAQ</xsl:text>
      </xsl:when>
      <xsl:when test="name($node)='sect'">
        <xsl:text>sec</xsl:text>
        <xsl:apply-templates mode="ref" select="$node"/>
      </xsl:when>
      <xsl:when test="name($node)='question'">
        <xsl:apply-templates mode="ref" select="$node"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="generate-id($node)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="getRef">
    <xsl:param name="node" select="."/>
    <xsl:text>#</xsl:text>
    <xsl:call-template name="getId">
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="docRefUrl">
    <xsl:param name="doc"/>
    <xsl:param name="loc"/>
    <xsl:choose>
      <xsl:when test="$doc='sun252'">
        <xsl:text>http://www.starlink.ac.uk/stil/sun252/</xsl:text>
      </xsl:when>
      <xsl:when test="$doc='sun253'">
        <xsl:text>http://www.starlink.ac.uk/topcat/sun253/</xsl:text>
      </xsl:when>
      <xsl:when test="$doc='sun256'">
        <xsl:text>http://www.starlink.ac.uk/stilts/sun256/</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message terminate="yes">
          <xsl:text>Unknown document ID</xsl:text>
          <xsl:value-of select="$doc"/>
        </xsl:message>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="$loc">
      <xsl:value-of select="$loc"/>
      <xsl:text>.html</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template name="docRefText">
    <xsl:param name="doc"/>
    <xsl:param name="loc"/>
    <xsl:choose>
      <xsl:when test="$doc='sun252'">
        <xsl:text>SUN/252</xsl:text>
      </xsl:when>
      <xsl:when test="$doc='sun253'">
        <xsl:text>SUN/253</xsl:text>
      </xsl:when>
      <xsl:when test="$doc='sun256'">
        <xsl:text>SUN/256</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message terminate="yes">
          <xsl:text>Unknown document ID $doc</xsl:text>
        </xsl:message>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
