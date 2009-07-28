package com.jcatalog.wiki;

import com.jcatalog.wiki.block.ParagraphBlock;
import net.java.textilej.parser.markup.Block;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.confluence.block.CodeBlock;
import net.java.textilej.parser.markup.confluence.block.ExtendedPreformattedBlock;
import net.java.textilej.parser.markup.confluence.block.ExtendedQuoteBlock;
import net.java.textilej.parser.markup.confluence.block.HeadingBlock;
import net.java.textilej.parser.markup.confluence.block.ListBlock;
import net.java.textilej.parser.markup.confluence.block.QuoteBlock;
import net.java.textilej.parser.markup.confluence.block.TableBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * The same markup dialect as Confluence dialect except it doesn't process
 * macros such as {panel}, {note}, {warning}, {info}, {tip}, {toc}.
 */
public class WeceemDialect extends ConfluenceDialect {

    private List<Block> supportedBlocks;

    public WeceemDialect() {
        supportedBlocks = new ArrayList<Block>();
        supportedBlocks.add(new HeadingBlock());
        supportedBlocks.add(new ListBlock());
        supportedBlocks.add(new QuoteBlock());
        supportedBlocks.add(new TableBlock());
        supportedBlocks.add(new ExtendedQuoteBlock());
        supportedBlocks.add(new ExtendedPreformattedBlock());
        supportedBlocks.add(new CodeBlock());
        supportedBlocks.add(new ParagraphBlock());
    }

    public List<Block> getBlocks() {
        return supportedBlocks;
    }
}
