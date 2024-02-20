package com.springboot.demo1.jsonvalidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

public class JsonValidator {

    public Set<ValidationMessage> isPlanValid(JsonNode plan) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = factory.getSchema(
                JsonValidator.class.getResourceAsStream("/newSchema.json")
        );

        return jsonSchema.validate(plan);
    }
}
