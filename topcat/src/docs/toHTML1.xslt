<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="JAVADOCS"
             select="'http://andromeda.star.bris.ac.uk/starjavadocs/'"/>
  <xsl:param name="VERSION" select="'???'"/>

  <!-- Top level element -->

  <xsl:template match="sun">
    <html>
      <head>
        <title>
          <xsl:apply-templates select="docinfo/title"/>
        </title>
      </head>
      <body>
        <xsl:apply-templates select="docinfo"/>
        <h2>
          <a href="http://www.starlink.ac.uk/">Starlink Project</a>
        </h2>
        <hr/>
        <h2>Contents</h2>
        <xsl:apply-templates select="docbody" mode="toc"/>
        <hr/>
        <xsl:apply-templates select="docbody"/>
        <xsl:call-template name="pageFooter"/>
      </body>
    </html>
  </xsl:template>


  <!-- normal processing -->

  <xsl:template match="docinfo">
    <h1 align="center">
      <xsl:apply-templates select="title"/>
      <br/>
      <xsl:text>Version </xsl:text>
      <xsl:call-template name="getVersion"/>
    </h1>
    <hr/>
    <p>
      <i>
        Starlink User Note
        <xsl:apply-templates select="docnumber"/>
        <br/>
        <xsl:apply-templates select="authorlist"/>
        <br/>
        <xsl:apply-templates select="docdate"/>
      </i>
    </p>
  </xsl:template>

  <xsl:template match="author">
    <xsl:apply-templates/>
    <xsl:if test="following-sibling::author">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="contact">
    <xsl:apply-templates/>
    <br/>
  </xsl:template>

  <xsl:template match="abstract">
    <h2><a name="abstract"/>Abstract</h2>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="p|px">
    <p><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="dd/p[position()=1]">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="ul|ol|li|dl|dd|blockquote|code|em|strong">
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

  <xsl:template match="img">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="verbatim">
    <pre><xsl:apply-templates/></pre>
  </xsl:template>

  <xsl:template match="blockcode">
    <pre><xsl:apply-templates select="text()"/></pre>
  </xsl:template>

  <xsl:template match="hidden"/>

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
        <xsl:when test="@hypertext">
          <xsl:value-of select="@hypertext"/>
        </xsl:when>
        <xsl:when test="string(.)">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>section </xsl:text>
          <xsl:apply-templates mode="ref" select="id(@id)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="webref">
    <a href="{@url}">
      <xsl:choose>
        <xsl:when test="@hypertext">
          <xsl:value-of select="@hypertext"/>
        </xsl:when>
        <xsl:when test="string(.)">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@url"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template match="javadoc">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:choose>
          <xsl:when test="@docset">
            <xsl:value-of select="@docset"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$JAVADOCS"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="translate(@class, '.', '/')"/>
        <xsl:choose>
          <xsl:when test="@class='.'">
            <xsl:value-of select="'index.html'"/>
          </xsl:when>
          <xsl:when test="substring(@class, string-length(@class))='.'">
            <xsl:value-of select="'package-summary.html'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'.html'"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="@member"> 
          <xsl:value-of select="'#'"/>
          <xsl:value-of select="normalize-space(@member)"/>
        </xsl:if>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="@hypertext">
          <xsl:value-of select="@hypertext"/>
        </xsl:when>
        <xsl:when test="string(.)">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <code>
            <xsl:choose>
              <xsl:when test="substring(@class, string-length(@class))='.'">
                <xsl:value-of 
                     select="substring(@class, 1, string-length(@class)-1)"/>
              </xsl:when>
              <xsl:when test="@member">
                <xsl:value-of select="substring-before(@member, '(')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="lastPart">
                  <xsl:with-param name="text" select="@class"/>
                </xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
          </code>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>


  <!-- titles -->

  <xsl:template match="sect/subhead/title">
    <hr/>
    <h2>
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

  <xsl:template match="subsect/subhead/title">
    <h3>
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
    </h3>
  </xsl:template>

  <xsl:template match="subsubsect/subhead/title">
    <h4>
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
    </h4>
  </xsl:template>

  <xsl:template match="subsubsubsect/subhead/title">
    <h5>
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
    </h5>
  </xsl:template>


  <!-- table of contents -->

  <xsl:template mode="toc" match="docbody">
    <ul>
      <xsl:apply-templates mode="toc" select="abstract|sect"/>
    </ul>
  </xsl:template>

  <xsl:template mode="toc" match="abstract">
    <li>
      <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:call-template name="getRef"/>
        </xsl:attribute>
        <xsl:text>Abstract</xsl:text>
      </xsl:element>
    </li>
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
    <xsl:if test="subsect">
      <ul>
        <xsl:apply-templates mode="toc" select="subsect"/>
      </ul>
    </xsl:if>
  </xsl:template>

  <xsl:template mode="toc" match="subsect">
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
    <xsl:if test="subsubsect">
      <ul>
        <xsl:apply-templates mode="toc" select="subsubsect"/>
      </ul>
    </xsl:if>
  </xsl:template>

  <xsl:template mode="toc" match="subsubsect">
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
    <xsl:if test="subsubsubsect">
      <ul>
        <xsl:apply-templates mode="toc" select="subsubsubsect"/>
      </ul>
    </xsl:if>
  </xsl:template>

  <xsl:template mode="toc" match="subsubsubsect">
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
  </xsl:template>


  <!-- section numbering -->

  <xsl:template mode="ref" match="sect">
    <xsl:number count="sect"/>
  </xsl:template>

  <xsl:template mode="ref" match="subsect">
    <xsl:apply-templates mode="ref" select=".."/>
    <xsl:text>.</xsl:text>
    <xsl:number count="subsect"/>
  </xsl:template>

  <xsl:template mode="ref" match="subsubsect">
    <xsl:apply-templates mode="ref" select=".."/>
    <xsl:text>.</xsl:text>
    <xsl:number count="subsubsect"/>
  </xsl:template>

  <xsl:template mode="ref" match="subsubsubsect">
    <xsl:apply-templates mode="ref" select=".."/>
    <xsl:text>.</xsl:text>
    <xsl:number count="subsubsubsect"/>
  </xsl:template>


  <!-- named section reference -->

  <xsl:template mode="nameref" match="sect|subsect|subsubsect|subsubsubsect">
    <xsl:apply-templates mode="nameref" select="subhead/title"/>
  </xsl:template>

  <xsl:template mode="nameref" match="title">
    <xsl:apply-templates mode="nameref"/>
  </xsl:template>

    <xsl:template mode="nameref" match="*">
      what??
      <xsl:value-of select="name(.)"/>
    </xsl:template>

  <xsl:template mode="nameref" match="abstract">
    <xsl:text>Abstract</xsl:text>
  </xsl:template>

  <xsl:template mode="nameref" match="docbody|docinfo">
    <xsl:text>Top</xsl:text>
  </xsl:template>


  <!-- subroutines -->

  <xsl:template name="lastPart">
    <xsl:param name="text"/>
    <xsl:choose>
      <xsl:when test="contains($text, '.')">
        <xsl:call-template name="lastPart">
          <xsl:with-param name="text" select="substring-after($text, '.')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="getId">
    <xsl:param name="node" select="."/>
    <xsl:choose>
      <xsl:when test="$node/@id">
        <xsl:value-of select="$node/@id"/>
      </xsl:when>
      <xsl:when test="name($node)='abstract'">
        <xsl:text>abstract</xsl:text>
      </xsl:when>
      <xsl:when test="name($node)='sun' or
                      name($node)='docinfo' or
                      name($node)='docbody'">
        <xsl:text>sun</xsl:text>
        <xsl:value-of select="/sun/docinfo/docnumber"/>
      </xsl:when>
      <xsl:when test="name($node)='sect' or
                      name($node)='subsect' or
                      name($node)='subsubsect' or
                      name($node)='subsubsubsect'">
        <xsl:text>sec</xsl:text>
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

  <xsl:template name="pageFooter">
    <hr/>
    <i>
      <xsl:apply-templates select="/sun/docinfo/title"/>
      <br/>
      Starlink User Note 
      <xsl:apply-templates select="/sun/docinfo/docnumber"/>
      <br/>
      <xsl:apply-templates select="/sun/docinfo/contactlist"/>
    </i>
  </xsl:template>

  <xsl:template name="getVersion">
    <xsl:choose>
      <xsl:when test="/sun/docinfo/softwareversion">
        <xsl:apply-templates select="/sun/docinfo/softwareversion"/>
      </xsl:when>
      <xsl:when test="$VERSION">
        <xsl:value-of select="$VERSION"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>???</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
