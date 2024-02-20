package com.springboot.demo1.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ValidationMessage;
import com.springboot.demo1.configuration.EtagManager;
import com.springboot.demo1.jsonvalidator.JsonValidator;
import com.springboot.demo1.model.Plan;
import com.springboot.demo1.services.PlanServices;
import com.springboot.demo1.utilities.Util;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class PlanController {

    @Autowired
    private PlanServices planServices;

    @Autowired
    private EtagManager etagManager;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping(value = "/plan", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ObjectNode> savePlan(@RequestBody(required = true) JsonNode plan,
                                               @RequestHeader HttpHeaders headers) {

        // Authorize the request
        if (!authorize(headers)) {
            // Return a forbidden response
            ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
            responseJsonNode.put("error", "Unauthorized access. Token not generated");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.UNAUTHORIZED);
        }


        JsonValidator validator = new JsonValidator();
        Set<ValidationMessage> errors = validator.isPlanValid(plan);
        ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
        if(errors.isEmpty()){
            try {
                boolean result = planServices.savePlan(Util.convertJsonNodeToPlan(plan));
                if (result){
                    responseJsonNode.put("objectId", plan.get("objectId"));
                    String eTag = etagManager.getETag(plan);
//                    pullElasticSearch();
                    HttpHeaders responseHeader=new HttpHeaders();
                    responseHeader.setETag(eTag);
                    return new ResponseEntity<>(responseJsonNode, responseHeader, HttpStatus.CREATED);
                } else {
                    responseJsonNode.put("error", "Error while saving the plan");
                    return new ResponseEntity<>(responseJsonNode, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (JsonProcessingException e) {
                responseJsonNode.put("error", "Error while saving the plan");
                return new ResponseEntity<>(responseJsonNode, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            List<String> errorMessage = errors.stream().map(ValidationMessage::getMessage).toList();
            ArrayNode arrayNode = mapper.valueToTree(errorMessage);
            responseJsonNode.putArray("error").addAll(arrayNode);
            return new ResponseEntity<>(responseJsonNode, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/plan/{id}")
    public ResponseEntity<Object> fetchPlanById(@PathVariable("id") String id,
                                              @RequestHeader HttpHeaders requestHeaders) {

        Plan plan;
        plan = planServices.fetchPlanById(id);

        // Authorize the request
        if (!authorize(requestHeaders)) {
            // Return a forbidden response
            ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
            responseJsonNode.put("error", "Unauthorized access. Token not found");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.UNAUTHORIZED);
        }

        if(plan != null) {
            String eTag = etagManager.getETag(Util.convertPlanToJsonNode(plan));
            HttpHeaders responseHeader = new HttpHeaders();
            responseHeader.setETag(eTag);

            if(etagManager.verifyEtag(plan, requestHeaders.getIfNoneMatch())) {
                return new ResponseEntity<>(responseHeader, HttpStatus.NOT_MODIFIED);
            } else {
                return new ResponseEntity<>(plan, responseHeader, HttpStatus.OK);
            }
        } else {
            ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
            responseJsonNode.put("error", "Object Id not found");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/plan/{id}")
    public ResponseEntity<String> deletePlan(@PathVariable("id") String id,
                                             @RequestHeader HttpHeaders requestHeaders) {

        // Authorize the request
        if (!authorize(requestHeaders)) {
            // Return a forbidden response
            ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
            responseJsonNode.put("error", "Unauthorized access. Token not found");
            return new ResponseEntity<>("Unauthorized access. Token not found", HttpStatus.UNAUTHORIZED);
        }

        boolean result = planServices.deletePlan(id);
        if (result) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Plan not found");
        }
    }

    @PatchMapping(value = "/plan/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ObjectNode> updatePlan(@PathVariable("id") String id,
                                                 @RequestBody(required = true) JsonNode plan,
                                                 @RequestHeader HttpHeaders requestHeaders) {

        ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();

        // Authorize the request
        if (!authorize(requestHeaders)) {
            // Return a forbidden response
            responseJsonNode.put("error", "Unauthorized access. Token not found");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.UNAUTHORIZED);
        }

        if (requestHeaders.getIfMatch().isEmpty()) {
            responseJsonNode.put("error", "Etag not provided in request.");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.BAD_REQUEST);
        }

        Plan oldPlan = planServices.fetchPlanById(id);

        if(oldPlan != null) {
            if (etagManager.verifyEtag(oldPlan, requestHeaders.getIfMatch())) {
                try {
                    Plan newPlan = Util.convertJsonNodeToPlan(plan);
                    Plan updatedPlan = Util.updatePlan(oldPlan, newPlan);
                    boolean result = planServices.savePlan(updatedPlan);
                    if (result) {
                        String eTag = etagManager.getETag(Util.convertPlanToJsonNode(updatedPlan));
                        HttpHeaders responseHeader=new HttpHeaders();
                        responseHeader.setETag(eTag);
                        responseJsonNode.put("message", "Resource updated successfully.");
                        return new ResponseEntity<>(responseJsonNode, responseHeader, HttpStatus.OK);
                    } else {
                        responseJsonNode.put("error", "Error while patching the plan");
                        return new ResponseEntity<>(responseJsonNode, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } catch (JsonProcessingException e) {
                    responseJsonNode.put("error", "Error while patching the plan");
                    return new ResponseEntity<>(responseJsonNode, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                responseJsonNode.put("error", "Plan has been updated by another user.");
                return new ResponseEntity<>(responseJsonNode, HttpStatus.PRECONDITION_FAILED);
            }
        } else {
            responseJsonNode.put("error", "Object Id not found");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/plan/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ObjectNode> putPlan(@PathVariable("id") String id,
                                                 @RequestBody(required = true) JsonNode plan,
                                                 @RequestHeader HttpHeaders requestHeaders) {

        // Authorize the request
        if (!authorize(requestHeaders)) {
            // Return a forbidden response
            ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
            responseJsonNode.put("error", "Unauthorized access. Token not found");
            return new ResponseEntity<>(responseJsonNode, HttpStatus.UNAUTHORIZED);
        }

        JsonValidator validator = new JsonValidator();
        // Validate the plan JSON against the schema
        Set<ValidationMessage> errors = validator.isPlanValid(plan);

        // Create a response JSON node
        ObjectNode responseJsonNode = JsonNodeFactory.instance.objectNode();
        if (errors.isEmpty()) {
            try {
                // Update the plan in the PlanServices
                boolean result = planServices.putPlan(id, Util.convertJsonNodeToPlan(plan));

                // If the update is successful, set the objectId in the response JSON
                if (result) {
                    responseJsonNode.put("objectId", id);

                    // Get the ETag for the updated plan
                    String eTag = etagManager.getETag(plan);

                    // Create response headers
                    HttpHeaders responseHeaders = new HttpHeaders();

                    // Set the ETag header
                    responseHeaders.setETag(eTag);

                    // Return a successful response with the response JSON and headers
                    return new ResponseEntity<>(responseJsonNode, responseHeaders, HttpStatus.OK);
                } else {
                    // If there was an error during the update, set an error message in the response JSON
                    responseJsonNode.put("error", "Error while updating the plan");
                    // Return an internal server error response
                    return new ResponseEntity<>(responseJsonNode, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (Exception e) {
                // If there was an error processing the update, set an error message in the response JSON
                responseJsonNode.put("error", "Error while updating the plan: " + e.getMessage());
                // Return an internal server error response
                return new ResponseEntity<>(responseJsonNode, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            // If there are validation errors, create an array of error messages
            List<String> errorMessages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());

            // Convert the error messages to an array node
            ArrayNode arrayNode = mapper.valueToTree(errorMessages);

            // Add the array of error messages to the response JSON
            responseJsonNode.putArray("error").addAll(arrayNode);

            // Return a bad request response with the response JSON
            return new ResponseEntity<>(responseJsonNode, HttpStatus.BAD_REQUEST);
        }
    }





    @GetMapping(value = "/token", produces = "application/json")
    public ResponseEntity<Map<String, Object>> createToken() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Create the token JSON
            JSONObject jsonToken = new JSONObject();
            jsonToken.put("Issuer", "nehayadav");

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df.setTimeZone(tz);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, 60);
            Date expiryDate = calendar.getTime();

            jsonToken.put("expiry", df.format(expiryDate));
            String token = jsonToken.toString();

            SecretKey spec = loadKey();
            Cipher c = Cipher.getInstance(algorithm);
            c.init(Cipher.ENCRYPT_MODE, spec);
            byte[] encryptedBytes = c.doFinal(token.getBytes());
            String encodedToken = Base64.getEncoder().encodeToString(encryptedBytes);

            response.put("token", encodedToken);
            response.put("expiry", df.format(expiryDate));

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Token creation failed.Try again.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    private String key = "ssdkF$HUy2A#D%kd";
    private String algorithm = "AES";
    public boolean authorize(HttpHeaders headers) {

        if (headers.getFirst("Authorization") == null)
            return false;

        String token = headers.getFirst("Authorization").substring(7);
        byte[] decrToken = Base64.getDecoder().decode(token);
        SecretKey spec = loadKey();
        try {
            Cipher c = Cipher.getInstance(algorithm);
            c.init(Cipher.DECRYPT_MODE, spec);
            String tokenString = new String(c.doFinal(decrToken));
            JSONObject jsonToken = new JSONObject(tokenString);

            String ttldateAsString = jsonToken.get("expiry").toString();
            Date currentDate = Calendar.getInstance().getTime();

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            formatter.setTimeZone(tz);

            Date ttlDate = formatter.parse(ttldateAsString);
            currentDate = formatter.parse(formatter.format(currentDate));

            if (currentDate.after(ttlDate)) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private SecretKey loadKey() {
        return new SecretKeySpec(key.getBytes(), algorithm);
    }

}


