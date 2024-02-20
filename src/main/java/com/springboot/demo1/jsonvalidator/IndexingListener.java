package com.springboot.demo1.jsonvalidator;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;


@Component
public class IndexingListener {

    private static final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost", 9200, "http")));
    public static final String IndexName = "indexplan";

    public void receiveMessage(Map<String, String> message) throws JSONException, IOException {
        System.out.println("Message received: " + message);

        String operation = message.get("operation");
        String body = message.get("body");
        JSONObject jsonBody = new JSONObject(body);


        switch (operation) {
            case "SAVE": {
                postDocument(jsonBody);
                break;
            }
            case "DELETE": {
                deleteDocument(jsonBody);
                break;
            }
        }
    }

    private void deleteDocument(JSONObject jsonObject) throws IOException, JSONException {
        ArrayList<String> listOfKeys = new ArrayList<>();
        convertToKeys(jsonObject, "", listOfKeys);
        for (String key : listOfKeys) {
            DeleteRequest request = new DeleteRequest(IndexName, "_doc", key);
            try {
                DeleteResponse deleteResponse = client.delete(request,RequestOptions.DEFAULT);
                if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                    System.out.println("Document " + key + " Not Found!!");
                } else {
                    System.out.println("Document " + key + " deleted successfully.");
                }
            } catch (ElasticsearchException e) {
                if (e.status() == RestStatus.NOT_FOUND) {
                    System.out.println("Document " + key + " Not Found!!");
                } else {
                    // Handle other exceptions if needed
                    e.printStackTrace();
                }
            }
        }
    }

    private void convertToKeys(JSONObject jsonObject, String prefix, ArrayList<String> listOfKeys) throws JSONException {
        Iterator<String> keysIterator = jsonObject.keys();
        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                // Recursively explore nested JSON objects
                convertToKeys((JSONObject) value, prefix + key + ".", listOfKeys);
            } else {
                listOfKeys.add(prefix + key);
            }
        }
    }

    private void postDocument(JSONObject jsonObject) {
        try {


            if (!indexExists()) {
                createElasticIndex();
            }

            Map<String, Map<String, Object>> MapOfDocuments = new HashMap<>();
            //---
            JSONObject plan = new JSONObject();

            //---
            convertMapToDocumentIndex(plan, "", "plan", MapOfDocuments);
            //        System.out.println("------------------MapOfDocuments-------------------------");
            //        System.out.println(MapOfDocuments.toString());
            //        System.out.println("------------------newMap-------------------------");
            //        System.out.println(newMap.toString());
            for (Map.Entry<String, Map<String, Object>> entry : MapOfDocuments.entrySet()) {
//                System.out.println("------------------entry-------------------------");
//                System.out.println(entry);

                String parentId = entry.getKey().split(":")[0];
                String objectId = entry.getKey().split(":")[1];
                IndexRequest request = new IndexRequest(IndexName);
                request.id(objectId);
                request.source(entry.getValue());
                request.routing(parentId);
                request.setRefreshPolicy("wait_for");
//                System.out.println("------------------request-------------------------");
//                System.out.println(request);
                IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
                System.out.println("response id: " + indexResponse.getId() + " parent id: " + parentId);

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }



    private static Map<String, Map<String, Object>> convertMapToDocumentIndex(JSONObject jsonObject,
                                                                              String parentId, String objectName, Map<String, Map<String, Object>> MapOfDocuments) throws JSONException {


        Map<String, Map<String, Object>> map = new HashMap<>();
        Map<String, Object> valueMap = new HashMap<>();
        Iterator<String> iterator = jsonObject.keys();

        while (iterator.hasNext()) {

            String key = iterator.next();
            String redisKey = jsonObject.get("objectType") + ":" + parentId;
            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {

                convertMapToDocumentIndex((JSONObject) value, jsonObject.get("objectId").toString(), key.toString(), MapOfDocuments);

            } else if (value instanceof JSONArray) {

                convertToList((JSONArray) value, jsonObject.get("objectId").toString(), key.toString(), MapOfDocuments);

            } else {
                valueMap.put(key, value);
                map.put(redisKey, valueMap);
            }
        }

        Map<String, Object> temp = new HashMap<>();
        if (objectName == "plan") {
            valueMap.put("plan_join", objectName);
        } else {
            temp.put("name", objectName);
            temp.put("parent", parentId);
            valueMap.put("plan_join", temp);
        }

        String id = parentId + ":" + jsonObject.get("objectId").toString();
//        System.out.println("11111================================================");
        MapOfDocuments.put(id, valueMap);
//        System.out.println(MapOfDocuments);
//        System.out.println("11111======map==========================================");
//        System.out.println(map);
        return MapOfDocuments;
    }

    private void createElasticIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(IndexName);
        request.settings(Settings.builder().put("index.number_of_shards", 1).put("index.number_of_replicas", 1));
        XContentBuilder mapping = getMapping();
        request.mapping("_doc", String.valueOf(mapping));
        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        boolean acknowledged = createIndexResponse.isAcknowledged();
        System.out.println("Index Creation: " + acknowledged);
    }



    private boolean indexExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest(IndexName);
        boolean exist = client.indices().exists(request, RequestOptions.DEFAULT);
        return exist;
    }



    private static List<Object> convertToList(JSONArray array, String parentId, String objectName, Map<String, Map<String, Object>> MapOfDocuments) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = null;
            try {
                value = array.get(i);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if (value instanceof JSONArray) {
                value = convertToList((JSONArray) value, parentId, objectName, MapOfDocuments);
            } else if (value instanceof JSONObject) {
                value = convertMapToDocumentIndex((JSONObject) value, parentId, objectName, MapOfDocuments);
            }
            list.add(value);
        }
        return list;
    }


    private XContentBuilder getMapping() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("properties");
            {
                builder.startObject("plan");
                {
                    builder.startObject("properties");
                    {
                        builder.startObject("_org");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("objectId");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                        builder.startObject("objectType");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("planType");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("creationDate");
                        {
                            builder.field("type", "date");
                            builder.field("format", "MM-dd-yyyy");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
                builder.startObject("planCostShares");
                {
                    builder.startObject("properties");
                    {
                        builder.startObject("copay");
                        {
                            builder.field("type", "long");
                        }
                        builder.endObject();
                        builder.startObject("deductible");
                        {
                            builder.field("type", "long");
                        }
                        builder.endObject();
                        builder.startObject("_org");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("objectId");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                        builder.startObject("objectType");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
                builder.startObject("linkedPlanServices");
                {
                    builder.startObject("properties");
                    {
                        builder.startObject("_org");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("objectId");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                        builder.startObject("objectType");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
                builder.startObject("linkedService");
                {
                    builder.startObject("properties");
                    {
                        builder.startObject("name");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("_org");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("objectId");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                        builder.startObject("objectType");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
                builder.startObject("planserviceCostShares");
                {
                    builder.startObject("properties");
                    {
                        builder.startObject("copay");
                        {
                            builder.field("type", "long");
                        }
                        builder.endObject();
                        builder.startObject("deductible");
                        {
                            builder.field("type", "long");
                        }
                        builder.endObject();
                        builder.startObject("_org");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                        builder.startObject("objectId");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                        builder.startObject("objectType");
                        {
                            builder.field("type", "text");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
                builder.startObject("plan_join");
                {
                    builder.field("type", "join");
                    builder.field("eager_global_ordinals", "true");
                    builder.startObject("relations");
                    {
                        builder.array("plan", "planCostShares", "linkedPlanServices");
                        builder.array("linkedPlanServices", "linkedService", "planserviceCostShares");
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();

        return builder;

    }


}
