package org.dice_research.tfc.pre;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.Charsets;
import org.dice_research.topicmodeling.io.json.JsonBasedCorpusPartWriter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplierAsIterator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.NumberArticlesDocumentFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.StringContainingDocumentPropertyBasedFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.StringContainingDocumentPropertyBasedFilter.StringContainingDocumentPropertyBasedFilterType;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.WikipediaRedirectDetectingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.WikipediaRedirectPropertyBasedFilter;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.wikipedia.WikipediaDumpReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for separating the huge Wikipedia corpus into single parts.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class WikipediaPartitioning {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaDumpReader.class);

    public static final int DOCUMENTS_PER_PART = 10000;

    public static void main(String[] args) {
        if (args.length != 2) {
            LOGGER.error("ERROR. usage: <input-file> <output-dir>");
            System.exit(1);
        }
        File inputDir = new File(args[0]);
        File intermediateDir = new File(args[1]);
        wikipediaPartitioning(inputDir, intermediateDir);
    }

    public static void wikipediaPartitioning(File dumpFile, File outputDir) {
        InputStream input = null;
        JsonBasedCorpusPartWriter writer = null;
        try {
            input = new FileInputStream(dumpFile);
            input = new BZip2CompressorInputStream(input);

            writer = new JsonBasedCorpusPartWriter(outputDir, DOCUMENTS_PER_PART);

            StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(
                            new DocumentSupplierAsIterator(WikipediaDumpReader.createReader(input, Charsets.UTF_8)),
                            Spliterator.DISTINCT & Spliterator.NONNULL), false)
                    // Remove documents without a text
                    .filter(new StringContainingDocumentPropertyBasedFilter<DocumentText>(
                            StringContainingDocumentPropertyBasedFilterType.EQUALS_NOT, DocumentText.class, ""))
                    // Detect redirects
                    .map(new WikipediaRedirectDetectingSupplierDecorator(null))
                    // Remove number articles
                    .filter(new NumberArticlesDocumentFilter(false))
                    // Remove redirects
                    .filter(new WikipediaRedirectPropertyBasedFilter())
                    // Write results
                    .forEach(writer);

        } catch (IOException e) {
            LOGGER.error("Error while parsing Wikipedia dump file.");
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(writer);
        }
    }
}
