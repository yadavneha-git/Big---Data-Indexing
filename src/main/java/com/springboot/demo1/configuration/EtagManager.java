package com.springboot.demo1.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.springboot.demo1.model.Plan;
import com.springboot.demo1.utilities.Util;

import java.util.Base64;
import java.util.List;

public class EtagManager {

    public String getETag(JsonNode planJsonNode) {
        String hash = java.lang.String.valueOf(java.lang.String.valueOf(planJsonNode.hashCode()));
        String base63Encode = Base64.getEncoder().encodeToString(hash.getBytes());
        base63Encode = "\"" + base63Encode + "\"";
        return base63Encode;
    }

    public boolean verifyEtag(Plan plan, List<String> eTags){
        String eTag = getETag(Util.convertPlanToJsonNode(plan));
        return eTags.contains(eTag);
    }
}
