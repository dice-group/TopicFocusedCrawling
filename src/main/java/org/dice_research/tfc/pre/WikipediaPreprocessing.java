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
import org.dice_research.topicmodeling.io.xml.XmlBasedCorpusPartWriter;
import org.dice_research.topicmodeling.lang.Term;
import org.dice_research.topicmodeling.lang.postagging.StanfordPipelineWrapper;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplierAsIterator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentTextWithTermInfoCreatingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PosTaggingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.NumberArticlesDocumentFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.WikipediaRedirectDetectingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.WikipediaRedirectPropertyBasedFilter;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.wikipedia.WikipediaDumpReader;
import org.dice_research.topicmodeling.wikipedia.WikipediaMarkupDeletingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.wikipedia.doc.WikipediaArticleId;
import org.dice_research.topicmodeling.wikipedia.doc.WikipediaNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class WikipediaPreprocessing {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaDumpReader.class);

    private static final String WIKIPEDIA_DUMP_FILE = "/home/micha/data/wikipedia/page.bz2";
    // private static final String WIKIPEDIA_DUMP_FILE =
    // "/home/micha/data/wikipedia/enwiki-20171103-pages-meta-current.xml.bz2";
    private static final String INTERMEDIATE_DIRECTORY = "/home/micha/data/wikipedia/intermediate";

    private static final int MIN_WORD_LENGTH = 3;
    private static final int DOCUMENTS_PER_PART = 1000;

    public static void main(String[] args) {
        
        Document d = new Document(0);
        d.addProperty(new DocumentName("Test text"));
        d.addProperty(new DocumentText("This is a test text."));
        d.addProperty(new TermTokenizedText(new Term("This", "this", "wf"), new Term("is","be","v")));
        
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization();
        builder.registerTypeAdapter(Document.class, new TypeAdapter<Document>() {

            @Override
            public void write(JsonWriter out, Document d) throws IOException {
                // TODO Auto-generated method stub
            }

            @Override
            public Document read(JsonReader in) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }
        });
        Gson gson = builder.create();
        System.out.println(gson.toJson(d));
        Document d2 = gson.fromJson(gson.toJson(d), Document.class);
        System.out.println(d2.toString());
        
//        File inputDir = new File(WIKIPEDIA_DUMP_FILE);
//        File intermediateDir = new File(INTERMEDIATE_DIRECTORY);
//        wikipediaParsing(inputDir, intermediateDir);
    }

    public static void wikipediaParsing(File dumpFile, File outputDir) {
        InputStream input = null;
        XmlBasedCorpusPartWriter writer = null;
        try {
            input = new FileInputStream(dumpFile);
            input = new BZip2CompressorInputStream(input);
            // DocumentSupplier supplier = WikipediaDumpReader.createReader(input,
            // Charsets.UTF_8);
            // supplier = new DocumentFilteringSupplierDecorator(supplier, new
            // NumberArticlesDocumentFilter(false));
            // supplier = new DocumentFilteringSupplierDecorator(supplier, new
            // WikipediaRedirectPropertyBasedFilter());
            // supplier = new WikipediaMarkupDeletingDocumentSupplierDecorator(supplier,
            // true, false);
            // supplier = new PosTaggingSupplierDecorator(supplier, StanfordPipelineWrapper
            // .createDefaultStanfordPipelineWrapper(t -> t.getWordForm().length() <
            // MIN_WORD_LENGTH));
            // supplier = new XmlBasedCorpusPartWriter(outputDir, DOCUMENTS_PER_PART);

            writer = new XmlBasedCorpusPartWriter(outputDir, DOCUMENTS_PER_PART);
            XmlBasedCorpusPartWriter.registerParseableDocumentProperty(WikipediaArticleId.class);
            XmlBasedCorpusPartWriter.registerParseableDocumentProperty(WikipediaNamespace.class);
            
            StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(
                            new DocumentSupplierAsIterator(WikipediaDumpReader.createReader(input, Charsets.UTF_8)),
                            Spliterator.DISTINCT & Spliterator.NONNULL), false)
                    .map(new WikipediaRedirectDetectingSupplierDecorator(null))
                    .filter(new NumberArticlesDocumentFilter(false))
                    .filter(new WikipediaRedirectPropertyBasedFilter())
                    .map(new WikipediaMarkupDeletingDocumentSupplierDecorator(null))
                    .map(new PosTaggingSupplierDecorator(null, StanfordPipelineWrapper
                             .createDefaultStanfordPipelineWrapper(t -> t.getWordForm().length() < MIN_WORD_LENGTH)))
                    .map(new DocumentTextWithTermInfoCreatingSupplierDecorator(null))
                    .forEach(writer);
            // DocumentSupplier supplier = WikipediaDumpReader.createReader(input,
            // Charsets.UTF_8);
            // supplier = new DocumentFilteringSupplierDecorator(supplier, new
            // NumberArticlesDocumentFilter(false));
            // supplier = new DocumentFilteringSupplierDecorator(supplier, new
            // WikipediaRedirectPropertyBasedFilter());
            // supplier = new WikipediaMarkupDeletingDocumentSupplierDecorator(supplier,
            // true, false);
            // supplier = new PosTaggingSupplierDecorator(supplier, StanfordPipelineWrapper
            // .createDefaultStanfordPipelineWrapper(t -> t.getWordForm().length() <
            // MIN_WORD_LENGTH));
            // supplier = new XmlBasedCorpusPartWriter(outputDir, DOCUMENTS_PER_PART);
        } catch (IOException e) {
            LOGGER.error("Error while parsing Wikipedia dump file.");
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(writer);
        }
    }
}
