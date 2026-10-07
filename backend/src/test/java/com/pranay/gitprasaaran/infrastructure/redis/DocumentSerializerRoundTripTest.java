package com.pranay.gitprasaaran.infrastructure.redis;

import com.pranay.gitprasaaran.domain.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentSerializerRoundTripTest {

    @Test
    void shouldSerializeAndDeserializeDocumentUsingRedisSerializer() throws Exception {
        Document original = new Document(
                "architecture",
                "Architecture",
                "Overview of the architecture",
                "# Architecture\n\nThis is a sample.",
                "<h1>Architecture</h1><p>This is a sample.</p>",
                "docs/architecture.md"
        );

        JacksonJsonRedisSerializer<Document> serializer = new JacksonJsonRedisSerializer<>(Document.class);

        byte[] serialized = serializer.serialize(original);
        Document deserialized = serializer.deserialize(serialized);

        assertEquals(original, deserialized);
    }
}
