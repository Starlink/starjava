<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:import href="toHTML1.xslt"/>

  <xsl:output method="xml"/>

  <xsl:key name="file" use="'true'" 
           match="*/docinfo|*/abstract|*/sect|*/subsect|*/subsubsect|*/subsubsubsect|*/subsubsubsubsect|*/subsubsubsubsubsect"/>

  <xsl:template match="sun">
    <multisection>
      <xsl:element name="filesection">
        <xsl:attribute name="file">
          <xsl:call-template name="getFile"/>
        </xsl:attribute>
        <xsl:attribute name="method">
          <xsl:text>html</xsl:text>
        </xsl:attribute>
        <html>
          <xsl:text>&#x0a;</xsl:text>
          <head>
            <xsl:call-template name="cssStylesheet"/>
            <title>
              <xsl:apply-templates select="docinfo/title"/>
            </title>
          </head>
          <xsl:text>&#x0a;</xsl:text>
          <body>
            <xsl:apply-templates select="docinfo"/>
            <hr/>
            <h2>Contents</h2>
            <xsl:apply-templates select="docbody" mode="toc"/>
            <xsl:call-template name="pageFooter"/>
          </body>
        </html>
      </xsl:element>
      <xsl:apply-templates select="docbody"/>
    </multisection>
  </xsl:template>

  <xsl:template match="abstract|sect|subsect|subsubsect|subsubsubsect|subsubsubsubsect|subsubsubsubsubsect">
    <xsl:text>&#x0a;</xsl:text>
    <hr/>
    <xsl:text>&#x0a;</xsl:text>
    <xsl:element name="filesection">
      <xsl:attribute name="file">
        <xsl:call-template name="getFile"/>
      </xsl:attribute>
      <html>
        <xsl:text>&#x0a;</xsl:text>
        <head>
          <xsl:call-template name="cssStylesheet"/>
          <title>
            <xsl:apply-templates mode="nameref" select="."/>
          </title>
        </head>
        <xsl:text>&#x0a;</xsl:text>
        <body>
          <hr/>
          <xsl:text>&#x0a;</xsl:text>
          <xsl:call-template name="navBar"/>
          <hr/>
          <xsl:if test="name(.)='abstract'">
            <h2>Abstract</h2> 
          </xsl:if>
          <xsl:apply-templates select="subhead|p|px|figure|subdiv"/>
          <xsl:if test="sect|subsect|subsubsect|subsubsubsect|subsubsubsubsect|subsubsubsubsubsect">
            <ul>
              <xsl:apply-templates mode="toc" 
                               select="sect|subsect|subsubsect|subsubsubsect|subsubsubsubsect|subsubsubsubsubsect"/>
            </ul>
          </xsl:if>
          <hr/>
          <xsl:call-template name="navBar"/>
          <xsl:call-template name="pageFooter"/>
        </body>
      </html>
    </xsl:element>
    <xsl:apply-templates select="sect|subsect|subsubsect|subsubsubsect|subsubsubsubsect|subsubsubsubsubsect"/>
  </xsl:template>

  <xsl:template name="getRef">
    <xsl:param name="node" select="."/>
    <xsl:param name="sect"
               select="($node/ancestor-or-self::sect
                       |$node/ancestor-or-self::subsect
                       |$node/ancestor-or-self::subsubsect
                       |$node/ancestor-or-self::subsubsubsect
                       |$node/ancestor-or-self::subsubsubsubsect
                       |$node/ancestor-or-self::subsubsubsubsubsect
                       |$node/ancestor-or-self::docbody
                       |$node/ancestor-or-self::docinfo
                       |$node/ancestor-or-self::abstract
                       |$node/ancestor-or-self::sun)[last()]"/>
    <xsl:param name="sectId">
      <xsl:call-template name="getId">
        <xsl:with-param name="node" select="$sect"/>
      </xsl:call-template>
    </xsl:param>
    <xsl:param name="nodeId">
      <xsl:call-template name="getId">
        <xsl:with-param name="node" select="$node"/>
      </xsl:call-template>
    </xsl:param>
    <xsl:param name="file">
      <xsl:call-template name="getFile">
        <xsl:with-param name="node" select="$sect"/>
      </xsl:call-template>
    </xsl:param>
    <xsl:value-of select="$file"/>
    <xsl:if test="$sectId != $nodeId">
      <xsl:text>#</xsl:text>
      <xsl:value-of select="$nodeId"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="getFile">
    <xsl:param name="node" select="."/>
    <xsl:choose>
      <xsl:when test="name($node)='docinfo' or 
                      name($node)='docbody' or
                      name($node)='sun'">
        <xsl:text>index.html</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="getId">
          <xsl:with-param name="node" select="$node"/>
        </xsl:call-template>
        <xsl:text>.html</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="seqNo">
    <xsl:variable name="seqNode" select="."/>
    <xsl:for-each select="key('file','true')">
      <xsl:if test=".=$seqNode">
        <xsl:value-of select="position()"/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="navBar">
    <xsl:variable name="seqno">
      <xsl:call-template name="seqNo"/>
    </xsl:variable>
    <xsl:variable name="next"
                  select="key('file','true')[position()=$seqno + 1]"/>
    <xsl:variable name="prev"
                  select="key('file','true')[position()=$seqno - 1]"/>
    <xsl:variable name="up"
                  select="ancestor::*[local-name()!='appendices'][1]"/>
    <xsl:variable name="contents"
                  select="/sun/docinfo"/>

    <xsl:call-template name="navButton">
      <xsl:with-param name="text" select="'Next'"/>
      <xsl:with-param name="destination" select="$next"/>
    </xsl:call-template>
    <xsl:call-template name="navButton">
      <xsl:with-param name="text" select="'Previous'"/>
      <xsl:with-param name="destination" select="$prev"/>
    </xsl:call-template>
    <xsl:call-template name="navButton">
      <xsl:with-param name="text" select="'Up'"/>
      <xsl:with-param name="destination" select="$up"/>
    </xsl:call-template>
    <xsl:call-template name="navButton">
      <xsl:with-param name="text" select="'Contents'"/>
      <xsl:with-param name="destination" select="$contents"/>
    </xsl:call-template>
    <br/>
    <xsl:call-template name="navLine">
      <xsl:with-param name="text" select="'Next'"/>
      <xsl:with-param name="destination" select="$next"/>
    </xsl:call-template>
    <xsl:call-template name="navLine">
      <xsl:with-param name="text" select="'Up'"/>
      <xsl:with-param name="destination" select="$up"/>
    </xsl:call-template>
    <xsl:call-template name="navLine">
      <xsl:with-param name="text" select="'Previous'"/>
      <xsl:with-param name="destination" select="$prev"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="navButton">
    <xsl:param name="text"/>
    <xsl:param name="destination"/>
    <xsl:choose>
      <xsl:when test="$destination">
        <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:call-template name="getRef">
              <xsl:with-param name="node" select="$destination"/>
            </xsl:call-template>
          </xsl:attribute>
          <xsl:value-of select="$text"/>
        </xsl:element>
       </xsl:when>
       <xsl:otherwise>
         <xsl:value-of select="$text"/>
       </xsl:otherwise>
    </xsl:choose>
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template name="navLine">
    <xsl:param name="text"/>
    <xsl:param name="destination"/>
    <xsl:text> </xsl:text>
    <xsl:choose>
      <xsl:when test="$destination">
        <b>
          <xsl:value-of select="$text"/>
          <xsl:text>: </xsl:text>
        </b>
        <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:call-template name="getRef">
              <xsl:with-param name="node" select="$destination"/>
            </xsl:call-template>
          </xsl:attribute>
          <xsl:apply-templates mode="nameref" select="$destination"/> 
        </xsl:element>
        <br/>
        <xsl:text>&#x0a;</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
