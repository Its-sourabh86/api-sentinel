package com.apisentinel.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class JsonSchemaChecker {

    private final JsonSchemaFactory factory =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    /** Schema khud valid hai ya nahi, save karte waqt check karne ke liye. Galat ho toh exception. */
    public void assertSchemaIsValid(JsonNode schemaNode) {
        factory.getSchema(schemaNode);
    }

    /** Document ko schema ke against check karo. Khaali list = sab theek. */
    public List<String> check(JsonNode document, String schemaText) {
        JsonSchema schema = factory.getSchema(schemaText);
        Set<ValidationMessage> messages = schema.validate(document);
        return messages.stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .toList();
    }
}
