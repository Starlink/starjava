<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <xsl:output version="1.0"
            method="xml"
            indent="yes"/>

  <xsl:param name="VERSION" select="'???'"/>
  <xsl:param name="BASEDIR" select="'.'"/>


  <!-- Templates -->

  <xsl:template match="sun">
    <fo:root>
      <fo:layout-master-set>

        <fo:simple-page-master master-name="cover-page"
                               xsl:use-attribute-sets="page-dimensions">
          <fo:region-body/>
        </fo:simple-page-master>

        <fo:simple-page-master master-name="body-page"
                               xsl:use-attribute-sets="page-dimensions">
          <fo:region-body margin-top="8mm"/>
          <fo:region-before extent="8mm"/>
          <fo:region-after extent="0mm"/>
        </fo:simple-page-master>

        <fo:page-sequence-master master-name="doc-pages">
          <fo:single-page-master-reference master-reference="cover-page"/>
          <fo:repeatable-page-master-reference master-reference="body-page"/>
        </fo:page-sequence-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="doc-pages"
                        force-page-count="no-force">
        <fo:title>
          <xsl:apply-templates select="docinfo/title"/>
        </fo:title>

        <fo:flow flow-name="xsl-region-body">
          <fo:block font-family="serif">
            <xsl:apply-templates select="docinfo"/>
            <fo:block xsl:use-attribute-sets="sect-title">
              <fo:block>
                Contents
              </fo:block>
              <xsl:apply-templates select="docbody" mode="toc"/>
            </fo:block>
          </fo:block>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="body-page"
                        force-page-count="no-force">
        <fo:title>
          <xsl:apply-templates select="docinfo/title"/>
        </fo:title>

        <fo:static-content flow-name="xsl-region-before">
          <fo:block text-align="justify" text-align-last="justify">
            <fo:inline font-style="italic">
              <xsl:text>SUN/</xsl:text>
              <xsl:apply-templates select="/sun/docinfo/docnumber"/>
            </fo:inline>
            <fo:leader leader-pattern="space"/>
            <fo:inline>
              <fo:page-number/>
            </fo:inline>
          </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">
          <fo:block font-family="serif">
            <xsl:apply-templates select="docbody"/>
          </fo:block>
        </fo:flow>
      </fo:page-sequence>

    </fo:root>
  </xsl:template>


  <!-- Docinfo and children -->

  <xsl:template match="docinfo">
    <fo:block text-align="center" xsl:use-attribute-sets="doc-title">
      <xsl:apply-templates select="title"/>
      <fo:block>
        <fo:leader xsl:use-attribute-sets="rule"/>
      </fo:block>
      <fo:block>
        <xsl:text>Version </xsl:text>
        <xsl:call-template name="getVersion"/>
      </fo:block>
    </fo:block>
    <fo:block font-style="italic">
      <fo:block>
        Starlink User Note
        <xsl:apply-templates select="docnumber"/>
      </fo:block>
      <fo:block>
        <xsl:apply-templates select="authorlist"/>
      </fo:block>
      <fo:block>
        <xsl:apply-templates select="docdate"/>
      </fo:block>
    </fo:block>
    <xsl:apply-templates select="../docbody/abstract"/>
  </xsl:template>
  
  <xsl:template match="author">
    <xsl:apply-templates/>
    <xsl:if test="following-sibling::author">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="contact">
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>


  <!-- Docbody and children -->

  <xsl:template match="docbody">
    <xsl:apply-templates select="sect|appendices"/>
  </xsl:template>

  <xsl:template match="p|px">
    <fo:block xsl:use-attribute-sets="p">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="ul|ol">
    <fo:list-block provisional-distance-between-starts="2em"
                   provisional-label-separation="1em"
                   space-before="0.5em"
                   space-after="0.5em">
      <xsl:apply-templates/>
    </fo:list-block>
  </xsl:template>

  <xsl:template match="ul/li">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block text-align="end">
          <fo:inline font-family="Symbol">&#x2022;</fo:inline>
        </fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates/>
        </fo:block>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <xsl:template match="ol/li">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block text-align="end">
          <xsl:number format="1."/>
        </fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates/>
        </fo:block>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <xsl:template match="dl">
    <fo:list-block provisional-distance-between-starts="2em"
                   space-before="0.5em"
                   space-after="0.5em">
      <xsl:apply-templates select="dt"/>
    </fo:list-block>
  </xsl:template>

  <xsl:template match="dt">
    <fo:list-item>
      <fo:list-item-label>
        <fo:block/>
      </fo:list-item-label>
      <fo:list-item-body>
        <fo:block font-weight="bold" start-indent="body-start() - 1em"
                                     text-align="start">
          <xsl:apply-templates/>
        </fo:block>
        <fo:block start-indent="body-start()">
          <xsl:apply-templates select="following-sibling::dd[1]"/>
        </fo:block>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <xsl:template match="dd">
    <fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="dd/p">
    <fo:block text-align="justify" text-indent="0pt"
              space-before="0pt" space-after="0.5em">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="ref">
    <xsl:choose>
      <xsl:when test="@plaintext">
        <xsl:value-of select="@plaintext"/>
        <xsl:if test="@plaintextref='yes'">
          <xsl:text> (</xsl:text>
          <xsl:call-template name="secRefText"/>
          <xsl:text>)</xsl:text>
        </xsl:if>
      </xsl:when>
      <xsl:when test="string(.)">
        <xsl:apply-templates/>
        <xsl:if test="@plaintextref='yes'">
          <xsl:text> (</xsl:text>
          <xsl:call-template name="secRefText"/>
          <xsl:text>)</xsl:text>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="secRefText"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="webref">
    <xsl:choose>
      <xsl:when test="@plaintext">
        <xsl:value-of select="@plaintext"/>
        <xsl:call-template name="webRefNote"/>
      </xsl:when>
      <xsl:when test="string(.)">
        <xsl:apply-templates/>
        <xsl:call-template name="webRefNote"/>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline font-family="sans-serif">
          <xsl:value-of select="@url"/>
        </fo:inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="verbatim">
    <fo:block xsl:use-attribute-sets="verbatim">
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>

  <xsl:template match="blockcode">
    <fo:block xsl:use-attribute-sets="verbatim">
      <xsl:apply-templates select="text()"/>
    </fo:block>
  </xsl:template>

  <xsl:template match="blockquote">
    <fo:block xsl:use-attribute-sets="blockquote">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="em">
    <fo:inline xsl:use-attribute-sets="em">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="strong">
    <fo:inline xsl:use-attribute-sets="strong">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="code">
    <fo:inline xsl:use-attribute-sets="code">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="javadoc">
    <xsl:choose>
      <xsl:when test="@plaintext">
        <xsl:value-of select="@plaintext"/>
      </xsl:when>
      <xsl:when test="string(.)">
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline xsl:use-attribute-sets="code">
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
        </fo:inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="var">
    <fo:inline xsl:use-attribute-sets="var">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="label">
    <fo:inline xsl:use-attribute-sets="label">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="sub">
    <fo:inline xsl:use-attribute-sets="sub">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="sup">
    <fo:inline xsl:use-attribute-sets="sup">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="m">
    <fo:inline xsl:use-attribute-sets="m">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="img">
    <xsl:element name="fo:external-graphic">
 <!-- These alignments would probably be an improvement, but are not
  !   implemented in FOP.  If I get an FO processor which does implement
  !   baseline alignments I should do some experimentation
  !   <xsl:attribute name="alignment-baseline">text-after-edge</xsl:attribute>
  !   <xsl:attribute name="alignment-adjust">text-after-edge</xsl:attribute>
  !-->
      <xsl:attribute name="src">
        <xsl:value-of select="$BASEDIR"/>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="@src"/>
      </xsl:attribute>
    </xsl:element>
  </xsl:template>

  <xsl:template match="figure">
    <fo:block keep-together="always" text-align="center">
      <fo:block>
        <xsl:element name="fo:external-graphic">
          <xsl:attribute name="src">
            <xsl:value-of select="$BASEDIR"/>
            <xsl:text>/../figures/</xsl:text>
            <xsl:value-of select="figureimage/@src"/>
          </xsl:attribute>
        </xsl:element>
      </fo:block>
      <fo:block font-weight="bold" text-align="center">
        <xsl:apply-templates select="caption"/>
      </fo:block>
    </fo:block>
  </xsl:template>

  <xsl:template match="abstract">
    <fo:block keep-together="always" id="{generate-id(.)}">
      <fo:block xsl:use-attribute-sets="sect-title">
        <xsl:text>Abstract</xsl:text>
      </fo:block>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="sect|appendices/sect/subsect">
    <fo:block id="{generate-id(.)}" break-before="page">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="subsect|subsubsect|subsubsubsect">
    <fo:block id="{generate-id()}">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>
 
  <xsl:template match="docbody/sect/subhead/title
                      |appendices/sect/subhead/title
                      |appendices/sect/subsect/subhead/title">
    <fo:block xsl:use-attribute-sets="sect-title">
      <xsl:apply-templates mode="ref" select="../.."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="docbody/sect/subsect/subhead/title
                      |appendices/sect/subsect/subsubsect/subhead/title">
    <fo:block xsl:use-attribute-sets="subsect-title">
      <xsl:apply-templates mode="ref" select="../.."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template
      match="docbody/sect/subsect/subsubsect/subhead/title
            |appendices/sect/subsect/subsubsect/subsubsubsect/subhead/title">
    <fo:block xsl:use-attribute-sets="subsubsect-title">
      <xsl:apply-templates mode="ref" select="../.."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template
      match="docbody/sect/subsect/subsubsect/subsubsubsect/subhead/title">
    <fo:block xsl:use-attribute-sets="subsubsubsect-title">
      <xsl:apply-templates mode="ref" select="../.."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>


  <!-- Section numbering -->

  <xsl:template mode="ref" match="sect">
    <xsl:number count="sect"/>
  </xsl:template>

  <xsl:template mode="ref" match="appendices/sect">
    <xsl:number count="sect" format="A"/>
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

  <!-- Section type description -->

  <xsl:template mode="sectype" match="sect|subsect|subsubsect|subsubsubsect">
    <xsl:choose>
      <xsl:when test="ancestor::appendices">
        <xsl:text>Appendix</xsl:text>
      </xsl:when>
      <xsl:when test="ancestor-or-self::sect">
        <xsl:text>Section</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>item??</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Table of contents -->

  <xsl:template mode="toc" match="docbody">
    <fo:block>
      <xsl:apply-templates mode="toc"
                           select="abstract|sect|appendices/sect"/>
    </fo:block>
  </xsl:template>

  <xsl:template mode="toc" match="abstract">
    <fo:block xsl:use-attribute-sets="toc-sect">
      <xsl:text>Abstract</xsl:text>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id(.)}"/>
    </fo:block>
  </xsl:template>

  <xsl:template mode="toc" match="appendices/sect">
    <fo:block xsl:use-attribute-sets="toc-sect" space-before="0.3em">
      <xsl:text>Appendix </xsl:text>
      <xsl:apply-templates mode="ref" select="."/>
      <xsl:text>: </xsl:text>
      <xsl:apply-templates mode="toc" select="subhead/title"/>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id()}"/>
    </fo:block>
    <xsl:apply-templates mode="toc" select="subsect"/>
  </xsl:template>

  <xsl:template mode="toc" match="docbody/sect
                                 |appendices/sect/subsect">
    <fo:block xsl:use-attribute-sets="toc-sect">
      <xsl:apply-templates mode="ref" select="."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates mode="toc" select="subhead/title"/>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id()}"/>
    </fo:block>
    <xsl:apply-templates mode="toc" select="subsect|subsubsect"/>
  </xsl:template>

  <xsl:template mode="toc" match="docbody/sect/subsect
                                 |appendices/sect/subsect/subsubsect">
    <fo:block xsl:use-attribute-sets="toc-subsect">
      <xsl:apply-templates mode="ref" select="."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates mode="toc" select="subhead/title"/>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id()}"/>
    </fo:block>
    <xsl:apply-templates mode="toc" select="subsubsect|subsubsubsect"/>
  </xsl:template>

  <xsl:template mode="toc"
                match="docbody/sect/subsect/subsubsect
                      |appendices/sect/subsect/subsubsect/subsubsubsect">
    <fo:block xsl:use-attribute-sets="toc-subsubsect">
      <xsl:apply-templates mode="ref" select="."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates mode="toc" select="subhead/title"/>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id()}"/>
    </fo:block>
    <xsl:apply-templates mode="toc" select="subsubsubsect"/>
  </xsl:template>

  <xsl:template mode="toc"
                match="docbody/sect/subsect/subsubsect/subsubsubsect">
    <fo:block xsl:use-attribute-sets="toc-subsubsubsect">
      <xsl:apply-templates mode="ref" select="."/>
      <xsl:text> </xsl:text>
      <xsl:apply-templates mode="toc" select="subhead/title"/>
      <fo:leader leader-pattern="dots"/>
      <fo:page-number-citation ref-id="{generate-id()}"/>
    </fo:block>
  </xsl:template>



  <!-- Subroutines -->

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

  <xsl:template name="secRefText">
    <xsl:apply-templates mode="sectype" select="id(@id)"/>
    <xsl:text> </xsl:text>
    <xsl:apply-templates mode="ref" select="id(@id)"/>
  </xsl:template>

  <xsl:template name="webRefNote">
  <xsl:if test="false()"> <!-- Doesn't place the footnotes well -->
    <xsl:variable name="footnote-symbol">
      *
    </xsl:variable>
    <fo:footnote>
      <fo:inline font-size="0.8em" vertical-align="super">
        <xsl:value-of select="$footnote-symbol"/>
      </fo:inline>
      <fo:footnote-body>
        <fo:block font-size="smaller">
          <fo:inline vertical-align="super">
            <xsl:value-of select="$footnote-symbol"/>
          </fo:inline>
          <fo:inline>
            <xsl:value-of select="@url"/>
          </fo:inline>
        </fo:block>
      </fo:footnote-body>
    </fo:footnote>
  </xsl:if>
  </xsl:template>

  <!-- Attribute sets -->

  <xsl:attribute-set name="page-dimensions">
    <xsl:attribute name="page-width">210mm</xsl:attribute>
    <xsl:attribute name="page-height">297mm</xsl:attribute>
    <xsl:attribute name="margin-top">15mm</xsl:attribute>
    <xsl:attribute name="margin-bottom">15mm</xsl:attribute>
    <xsl:attribute name="margin-left">20mm</xsl:attribute>
    <xsl:attribute name="margin-right">20mm</xsl:attribute>
  </xsl:attribute-set>

  <!-- Headings -->
  <xsl:attribute-set name="doc-title">
    <xsl:attribute name="space-before">1em</xsl:attribute>
    <xsl:attribute name="space-after">0.5em</xsl:attribute>
    <xsl:attribute name="font-size">xx-large</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="sect-title">
    <xsl:attribute name="space-before">1em</xsl:attribute>
    <xsl:attribute name="space-after">0.5em</xsl:attribute>
    <xsl:attribute name="font-size">x-large</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="subsect-title">
    <xsl:attribute name="space-before">1em</xsl:attribute>
    <xsl:attribute name="space-after">0.5em</xsl:attribute>
    <xsl:attribute name="font-size">large</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="subsubsect-title">
    <xsl:attribute name="space-before">1em</xsl:attribute>
    <xsl:attribute name="space-after">0.5em</xsl:attribute>
    <xsl:attribute name="font-size">medium</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="subsubsubsect-title">
    <xsl:attribute name="space-before">1em</xsl:attribute>
    <xsl:attribute name="space-after">0.5em</xsl:attribute>
    <xsl:attribute name="font-size">small</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="color">black</xsl:attribute>
    <xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="toc-sect">
    <xsl:attribute name="font-weight">800</xsl:attribute>
    <xsl:attribute name="text-align-last">justify</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="toc-subsect">
    <xsl:attribute name="font-weight">600</xsl:attribute>
    <xsl:attribute name="text-align-last">justify</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="toc-subsubsect">
    <xsl:attribute name="font-weight">400</xsl:attribute>
    <xsl:attribute name="text-align-last">justify</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="toc-subsubsubsect">
    <xsl:attribute name="font-weight">200</xsl:attribute>
    <xsl:attribute name="text-align-last">justify</xsl:attribute>
  </xsl:attribute-set>

  <!-- Block level Attributes -->
  <!-- p-->
  <xsl:attribute-set name="p">
    <xsl:attribute name="text-align">justify</xsl:attribute>
    <xsl:attribute name="text-indent">0em</xsl:attribute>
    <xsl:attribute name="space-before">0.5em</xsl:attribute>
    <xsl:attribute name="space-after">0.5em</xsl:attribute>
  </xsl:attribute-set>

  <!-- verbatim -->
  <xsl:attribute-set name="verbatim">
    <xsl:attribute name="font-family">monospace</xsl:attribute>
    <xsl:attribute name="font-size">0.8em</xsl:attribute>
    <xsl:attribute name="white-space-treatment">ignore-if-before-linefeed</xsl:attribute>
    <xsl:attribute name="white-space-collapse">false</xsl:attribute>
    <xsl:attribute name="wrap-option">no-wrap</xsl:attribute>
    <xsl:attribute name="text-align">start</xsl:attribute>
    <xsl:attribute name="text-indent">1em</xsl:attribute>
    <xsl:attribute name="space-before">0.6em</xsl:attribute>
    <xsl:attribute name="space-after">0.6em</xsl:attribute>
  </xsl:attribute-set>

  <!-- blockquote -->
  <xsl:attribute-set name="blockquote">
    <xsl:attribute name="start-indent">inherit + 4em </xsl:attribute>
    <xsl:attribute name="end-indent">inherit + 4em</xsl:attribute>
    <xsl:attribute name="text-indent">1em</xsl:attribute>
    <xsl:attribute name="space-before">0.6em</xsl:attribute>
    <xsl:attribute name="space-after">0.6em</xsl:attribute>
    <xsl:attribute name="margin-top">1em</xsl:attribute>
    <xsl:attribute name="margin-bottom">1em</xsl:attribute>
  </xsl:attribute-set>

  <!-- Text -->
  <xsl:attribute-set name="em">
    <xsl:attribute name="font-style">italic</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="strong">
    <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="code">
    <xsl:attribute name="font-family">monospace</xsl:attribute>
    <xsl:attribute name="font-size">0.8em</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="label">
    <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="var">
      <xsl:attribute name="font-style">italic</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="sup">
    <xsl:attribute name="vertical-align">super</xsl:attribute>
    <xsl:attribute name="font-size">.8em</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="sub">
    <xsl:attribute name="vertical-align">sub</xsl:attribute>
    <xsl:attribute name="font-size">.8em</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="m">
    <xsl:attribute name="font-style">italic</xsl:attribute>
    <xsl:attribute name="font-family">serif</xsl:attribute>
  </xsl:attribute-set>

  <!-- Leaders -->
  <xsl:attribute-set name="rule">
    <xsl:attribute name="leader-length">100%</xsl:attribute>
  </xsl:attribute-set>





</xsl:stylesheet>
