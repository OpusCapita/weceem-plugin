package com.jcatalog.wiki.block;

import net.java.textilej.parser.Attributes;
import net.java.textilej.parser.DocumentBuilder.BlockType;
import net.java.textilej.parser.markup.Block;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;

/**
 * Class is the same as net.java.textilej.parser.markup.confluence.block.ParagraphBlock,
 * except 'confluenceBlockStart' constant value, because processing for
 * {code}, {info}, {tip}, {warning}, {panel}, {note} and {toc} blocks don't needed.
 */
public class ParagraphBlock extends Block {

    private int blockLineCount = 0;

    /**
     * @see net.java.textilej.parser.markup.Block#processLineContent(java.lang.String, int)
     */
    @Override
    public int processLineContent(String line, int offset) {
        if (blockLineCount == 0) {
            Attributes attributes = new Attributes();

            builder.beginBlock(BlockType.PARAGRAPH, attributes);
        }

        if (dialect.isEmptyLine(line)) {
            setClosed(true);
            return 0;
        }

        ++blockLineCount;

        ConfluenceDialect myDialect = (ConfluenceDialect) getDialect();

        // NOTE: in Textile paragraphs can have nested lists and other things, however
        //       the resulting XHTML is invalid -- so here we allow for similar constructs
        //       however we cause them to end the paragraph rather than being nested.
        for (Block block : myDialect.getParagraphBreakingBlocks()) {
            if (block.canStart(line, offset)) {
                setClosed(true);
                return 0;
            }
        }

        if (blockLineCount > 1) {
            builder.lineBreak();
        }
        myDialect.emitMarkupLine(getParser(), state, line, offset);

        return -1;
    }

    /**
     * @see net.java.textilej.parser.markup.Block#canStart(java.lang.String, int)
     */
    @Override
    public boolean canStart(String line, int lineOffset) {
        blockLineCount = 0;
        return true;
    }

    /**
     * @see net.java.textilej.parser.markup.Block#setClosed(boolean)
     */
    @Override
    public void setClosed(boolean closed) {
        if (closed && !isClosed()) {
            builder.endBlock();
        }
        super.setClosed(closed);
    }
}
