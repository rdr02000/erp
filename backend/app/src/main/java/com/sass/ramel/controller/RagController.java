package com.sass.ramel.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sass.ramel.service.WordDocumentIngestionService;

import java.io.IOException;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final ChatClient chatClient;
    private final VectorStore pgVectorStore;
    private final WordDocumentIngestionService wordDocumentIngestionService;

    public RagController(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("pgVectorStore") VectorStore pgVectorStore,
            WordDocumentIngestionService wordDocumentIngestionService) {
        this.chatClient = chatClientBuilder.build();
        this.pgVectorStore = pgVectorStore;
        this.wordDocumentIngestionService = wordDocumentIngestionService;
    }

    @PostMapping("/ingest/word-docs")
    public ResponseEntity<String> triggerIngestion() {
        try {
            wordDocumentIngestionService.ingestWordDocuments();
            return ResponseEntity.ok("Word documents successfully ingested and vectorized.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to process directory: " + e.getMessage());
        }
    }

    /**
     * Step 2: Retrieval & Augmented Generation (Online Pipeline)
     * Uses QuestionAnswerAdvisor to intercept the prompt, search the VectorStore, 
     * bundle context into the system instructions, and send it to Ollama.
     */
    @GetMapping("/ask")
    public String askQuestion(@RequestParam(value = "message") String message) {
        String systemPrompt = """
                You are a helpful assistant for this knowledge base.
                Use the retrieved context when it contains information relevant to the question.
                If the context does not contain the answer, answer using your general knowledge.
                Do not invent project-specific facts that are not in the context. If you cannot answer confidently,
                say so and explain what information is missing.
                """;

        return this.chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(QuestionAnswerAdvisor.builder(pgVectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(4)
                                .similarityThresholdAll()
                                .build())
                        .build())
                .call()
                .content();
    }
}