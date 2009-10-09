<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="text" encoding="UTF-8" media-type="text/plain"/>

  <xsl:strip-space elements="*"/>

  <xsl:template match="body">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- Heading -->
  <xsl:template match="h1|h2|h3|h4|h5|h6">
    <xsl:value-of select="name()"/>
    <xsl:text>.&#160;</xsl:text>
    <xsl:apply-templates/>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Strong -->
  <xsl:template match="strong">
    <xsl:text>*</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>*</xsl:text>
  </xsl:template>

  <!-- Emphasis -->
  <xsl:template match="em">
    <xsl:text>_</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>_</xsl:text>
  </xsl:template>

  <!-- Citation -->
  <xsl:template match="cite">
    <xsl:text>??</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>??</xsl:text>
  </xsl:template>

  <!-- Strikethrough -->
  <xsl:template match="del">
    <xsl:text>-</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>-</xsl:text>
  </xsl:template>

  <!-- Underlined -->
  <xsl:template match="u">
    <xsl:text>+</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>+</xsl:text>
  </xsl:template>

  <!-- Superscript -->
  <xsl:template match="sup">
    <xsl:text>^</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>^</xsl:text>
  </xsl:template>

  <!-- Subscript -->
  <xsl:template match="sub">
    <xsl:text>~</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>~</xsl:text>
  </xsl:template>

  <!-- Monospaced -->
  <xsl:template match="tt">
    <xsl:text>{{</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>}}</xsl:text>
  </xsl:template>

  <!-- Quoteable -->
  <xsl:template match="blockquote">
    <xsl:text>{quote}</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>{quote}</xsl:text>
  </xsl:template>

  <!-- Preformatted -->
  <xsl:template match="pre[not(code)]">
    <xsl:text>{noformat}</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>{noformat}</xsl:text>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Paragraph -->
  <xsl:template match="p[name(..) != 'blockquote' and name(..) != 'div']">
    <xsl:apply-templates/>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Line break -->
  <xsl:template match="br">
    <xsl:text>\\</xsl:text>
  </xsl:template>

  <!-- Horizontal ruler -->
  <xsl:template match="hr">
    <xsl:text>&#160;----&#160;</xsl:text>
  </xsl:template>

  <!-- Link -->
  <xsl:template match="a">
    <xsl:text>[</xsl:text>
    <xsl:if test="text() != @href">
      <xsl:value-of select="text()"/>
      <xsl:text>|</xsl:text>
    </xsl:if>
    <xsl:value-of select="@href"/>
    <xsl:text>]</xsl:text>
  </xsl:template>

  <!-- Image -->
  <xsl:template match="img">
    <xsl:text>!</xsl:text>
    <xsl:value-of select="@src"/>
    <xsl:text>!</xsl:text>
  </xsl:template>

  <!-- Lists -->
  <xsl:template match="ol|ul">
    <xsl:if test="ancestor::ol|ancestor::ul">
      <xsl:text>&#x0A;</xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- List item -->
  <xsl:template match="li">
    <xsl:for-each select="ancestor::ol|ancestor::ul">
      <xsl:choose>
        <xsl:when test="name(.) = 'ol'">
          <xsl:text>#</xsl:text>
        </xsl:when>
        <xsl:when test="(name(.) = 'ul') and (@style = 'list-style: square')">
          <xsl:text>-</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>*</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    <xsl:text>&#160;</xsl:text>
    <xsl:apply-templates/>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Table -->
  <xsl:template match="table">
    <xsl:apply-templates/>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Table row -->
  <xsl:template match="tr">
    <xsl:apply-templates/>
    <xsl:if test="(following-sibling::*)">
      <xsl:text>&#x0A;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Table header cell -->
  <xsl:template match="th">
    <xsl:text>||</xsl:text>
    <xsl:apply-templates/>
    <xsl:if test="not(following-sibling::*)">
      <xsl:text>||</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Table cell -->
  <xsl:template match="td">
    <xsl:text>|</xsl:text>
    <xsl:apply-templates/>
    <xsl:if test="not(following-sibling::*)">
      <xsl:text>|</xsl:text>
    </xsl:if>
  </xsl:template>

  <!--
    TODO:
    - &#8211; (—)
    - &#8212; (–)
  -->

</xsl:stylesheet>
