package org.dice_research.tfc.pre;

import java.io.File;

import org.dice_research.topicmodeling.io.CorpusReader;
import org.dice_research.topicmodeling.io.CorpusWriter;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusReaderDecorator;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusWriterDecorator;
import org.dice_research.topicmodeling.io.java.CorpusObjectReader;
import org.dice_research.topicmodeling.io.java.CorpusObjectWriter;
import org.dice_research.topicmodeling.preprocessing.corpus.DocumentFrequencyBasedCorpusPreprocessor;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes the BOW corpus of the {@link WikipediaBOWCreation} class and
 * reduces the vocabulary based on the DF of the single words.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class WikipediaCorpusReduction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaCorpusReduction.class);

    private static final int minDocFrequence = 10;
    private static final double maxDocFrequenceRate = 0.6;

    public static void main(String[] args) {
        if ((args.length != 2) && (args.length != 4)) {
            LOGGER.error("ERROR. usage: <input-file> <output-file>");
            System.exit(1);
        }
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        reduceVocab(inputFile, outputFile);
    }

    public static void reduceVocab(File inputFile, File outputFile) {
        try {
            LOGGER.info("Reading corpus...");
            CorpusReader reader = new GZipCorpusReaderDecorator(new CorpusObjectReader());
            reader.readCorpus(inputFile);
            Corpus corpus = reader.getCorpus();
            LOGGER.info("Preprocessing corpus...");
            int maxDocFrequence = (int) (maxDocFrequenceRate * corpus.getNumberOfDocuments());
            DocumentFrequencyBasedCorpusPreprocessor preproc = new DocumentFrequencyBasedCorpusPreprocessor(
                    minDocFrequence, maxDocFrequence);
            corpus = preproc.preprocess(corpus);
            LOGGER.info("Writing corpus...");
            CorpusWriter writer = new GZipCorpusWriterDecorator(new CorpusObjectWriter());
            writer.writeCorpus(corpus, outputFile);
        } catch (Exception e) {
            LOGGER.error("Error while preprocessing.", e);
        }
    }
}
