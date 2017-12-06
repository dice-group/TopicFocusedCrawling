package org.dice_research.tfc.pre;

import java.io.File;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.commons.compress.utils.IOUtils;
import org.dice_research.topicmodeling.io.json.JsonBasedCorpusPartWriter;
import org.dice_research.topicmodeling.io.json.JsonPartsBasedDocumentSupplier;
import org.dice_research.topicmodeling.lang.postagging.StanfordPipelineWrapper;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplierAsIterator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PosTaggingSupplierDecorator;
import org.dice_research.topicmodeling.wikipedia.WikipediaMarkupDeletingDocumentSupplierDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes the parts of the {@link WikipediaPartitioning} class and
 * tags them using a POS tagger.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class WikipediaPosTagging {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPosTagging.class);

    // public static final String POS_TAG_DIRECTORY =
    // "/home/micha/data/wikipedia/tagged";

    private static final boolean PARALLEL_EXECUTION = false;

    public static void main(String[] args) {
        if ((args.length != 2) && (args.length != 4)) {
            LOGGER.error("ERROR. usage: <input-dir> <output-dir> [start-part-id] [end-part-id-(excluding)]");
            System.exit(1);
        }
        File inputDir = new File(args[0]);
        File intermediateDir = new File(args[1]);
        int startId = 0;
        int endId = Integer.MAX_VALUE;
        if (args.length > 2) {
            startId = Integer.parseInt(args[2]);
            endId = Integer.parseInt(args[3]);
        }
        wikipediaPosTagging(inputDir, intermediateDir, startId, endId);
    }

    public static void wikipediaPosTagging(File inputFolder, File outputDir, int startId, int endId) {
        InputStream input = null;
        JsonBasedCorpusPartWriter writer = null;
        try {
            writer = new JsonBasedCorpusPartWriter(outputDir, WikipediaPartitioning.DOCUMENTS_PER_PART, startId);

            StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(
                            new DocumentSupplierAsIterator(
                                    new JsonPartsBasedDocumentSupplier(inputFolder, startId, endId)),
                            Spliterator.DISTINCT & Spliterator.NONNULL), PARALLEL_EXECUTION)
                    // Remove wikimarkup from documents
                    .map(new WikipediaMarkupDeletingDocumentSupplierDecorator(null))
                    // Add POS tags
                    .map(new PosTaggingSupplierDecorator(null,
                            StanfordPipelineWrapper.createDefaultStanfordPipelineWrapper()))
                    .forEach(writer);
        } catch (Exception e) {
            LOGGER.error("Error while pos tagging.", e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(writer);
        }
    }
}
