package com.ai.rag.app;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AppApplicationTests {

    @Test
    void splitsSpringAiDocuments() {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(300)
                .build();
        assertFalse(splitter.split(new Document("sample document")).isEmpty());
    }
}
