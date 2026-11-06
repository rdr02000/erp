package com.sass.ramel.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class WordDocumentIngestionService {
  private final VectorStore pgVectorStore;
    
    @Value("${app.ingestion.directory:./documents}")
    private String ingestionDirectory;

    public WordDocumentIngestionService(@Qualifier("pgVectorStore") VectorStore pgVectorStore) {
        this.pgVectorStore = pgVectorStore;
    }

    public void ingestWordDocuments() throws IOException {
        Path dirPath = Paths.get(ingestionDirectory);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Target directory does not exist: " + ingestionDirectory);
        }

        List<Document> splitDocuments = new ArrayList<>();
        TokenTextSplitter splitter = TokenTextSplitter.builder().
                withChunkSize(300)        // Total maximum tokens allowed per chunk
                .withMinChunkSizeChars(50)      // The minimum token overlap window size (rolling overlay)
                .withMinChunkLengthToEmbed(0)
                .build();

        // 1. EXTRACT: Filter for Word files (.docx and .doc)
        try (var stream = Files.newDirectoryStream(dirPath, "*.{docx,doc}")) {
            for (Path path : stream) {
                // Pass the file system resource to Tika reader
                FileSystemResource resource = new FileSystemResource(path.toFile());
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                
                List<Document> docs = reader.get();
                
                for (Document doc : docs) {
                    String filename = path.getFileName().toString();
                    doc.getMetadata().put("source_filename", filename);
                    doc.getMetadata().put("file_path", path.toAbsolutePath().toString());
                    for (Document chunk : splitter.apply(List.of(doc))) {
                        splitDocuments.add(new Document(
                                "Source document: " + filename + "\n\n" + chunk.getText(),
                                chunk.getMetadata()));
                    }
                }
            }
        }

        if (splitDocuments.isEmpty()) {
            return; // No documents found to process
        }

        // 3. LOAD: Write text chunks into the Vector Database
        // This automatically calls the underlying EmbeddingModel to vectorize the text
        pgVectorStore.accept(splitDocuments);
    }
}
