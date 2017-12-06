package org.dice_research.tfc.pre;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.dice_research.topicmodeling.io.CorpusWriter;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusWriterDecorator;
import org.dice_research.topicmodeling.io.java.CorpusObjectWriter;
import org.dice_research.topicmodeling.io.json.JsonPartsBasedDocumentSupplier;
import org.dice_research.topicmodeling.lang.Language;
import org.dice_research.topicmodeling.lang.postagging.PosTaggingTermFilter;
import org.dice_research.topicmodeling.lang.postagging.StopwordlistBasedTermFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplierAsIterator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentWordCountingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PropertyRemovingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.TermFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.WordIndexingSupplierDecorator;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.corpus.DocumentListCorpus;
import org.dice_research.topicmodeling.utils.corpus.properties.CorpusVocabulary;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentProperty;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.DocumentTextWordIds;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.utils.vocabulary.SimpleVocabulary;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.dice_research.topicmodeling.wikipedia.doc.WikipediaArticleId;
import org.dice_research.topicmodeling.wikipedia.doc.WikipediaNamespace;
import org.dice_research.topicmodeling.wikipedia.doc.WikipediaRedirect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;

/**
 * This class uses the tagged corpus parts of the {@link WikipediaPosTagging}
 * class and generates a bag of words corpus including a {@link Vocabulary}
 * instance.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class WikipediaBOWCreation {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaBOWCreation.class);

    private static final boolean PARALLEL_EXECUTION = false;

    private static final int MIN_WORD_LENGTH = 3;

    public static void main(String[] args) {
        if ((args.length != 2) && (args.length != 4)) {
            LOGGER.error("ERROR. usage: <input-dir> <output-file> [start-part-id] [end-part-id-(excluding)]");
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
        createBOWCorpus(inputDir, intermediateDir, startId, endId);
    }

    public static void createBOWCorpus(File inputFolder, File outputFile, int startId, int endId) {
        CorpusWriter writer = null;
        try {
            PosTaggingTermFilter stopWordListFilter = new StopwordlistBasedTermFilter(Language.ENG);
            RunAutomaton numberChecker = new RunAutomaton(new RegExp(".*[1-9].*").toAutomaton());
            Vocabulary vocabulary = new SimpleVocabulary();
            Corpus corpus = new DocumentListCorpus<List<Document>>(StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(
                            new DocumentSupplierAsIterator(
                                    new JsonPartsBasedDocumentSupplier(inputFolder, startId, endId)),
                            Spliterator.DISTINCT & Spliterator.NONNULL), PARALLEL_EXECUTION)
                    // Filter terms
                    .map(new TermFilteringSupplierDecorator(null,
                            t -> (stopWordListFilter.isTermGood(t) && (t.getLemma().length() < MIN_WORD_LENGTH)
                                    && !numberChecker.run(t.getLemma()))))
                    // Filter empty documents
                    .filter(d -> d.getProperty(TermTokenizedText.class).getTermTokenizedText().size() > 0)
                    // Index words
                    .map(new WordIndexingSupplierDecorator(null, vocabulary))
                    // Count words
                    .map(new DocumentWordCountingSupplierDecorator(null))
                    // Remove properties that are not necessary anymore
                    .map(new PropertyRemovingSupplierDecorator((DocumentSupplier) null,
                            (List<Class<? extends DocumentProperty>>) Arrays.asList(
                                    (Class<? extends DocumentProperty>) DocumentText.class,
                                    (Class<? extends DocumentProperty>) TermTokenizedText.class,
                                    (Class<? extends DocumentProperty>) DocumentName.class,
                                    (Class<? extends DocumentProperty>) WikipediaArticleId.class,
                                    (Class<? extends DocumentProperty>) WikipediaRedirect.class,
                                    (Class<? extends DocumentProperty>) WikipediaNamespace.class,
                                    (Class<? extends DocumentProperty>) DocumentTextWordIds.class)))
                    // Create final list
                    .collect(Collectors.toList()));

            corpus.addProperty(new CorpusVocabulary(vocabulary));
            writer = new GZipCorpusWriterDecorator(new CorpusObjectWriter());
            writer.writeCorpus(corpus, outputFile);
        } catch (Exception e) {
            LOGGER.error("Error while preprocessing.", e);
        }
    }
}
