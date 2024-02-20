package com.springboot.demo1.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.demo1.model.MemberCostShare;
import com.springboot.demo1.model.Plan;
import com.springboot.demo1.model.PlanService;
import com.springboot.demo1.model.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Util {


    static ObjectMapper objectMapper = new ObjectMapper();

    public static Plan convertJsonNodeToPlan(JsonNode planJsonNode) throws JsonProcessingException {
        return objectMapper.treeToValue(planJsonNode, Plan.class);
    }

    public static JsonNode convertPlanToJsonNode(Plan plan){
        return objectMapper.valueToTree(plan);
    }

    public static Plan updatePlan(Plan oldPlan, Plan newPlan) {
        // Update org
        if (newPlan.get_org() != null && !newPlan.get_org().equals(oldPlan.get_org())) {
            oldPlan.set_org(newPlan.get_org());
        }

        // Update objectType
        if (newPlan.getObjectType() != null && !newPlan.getObjectType().equals(oldPlan.getObjectType())) {
            oldPlan.setObjectType(newPlan.getObjectType());
        }

        // Update planType
        if (newPlan.getPlanType() != null && !newPlan.getPlanType().equals(oldPlan.getPlanType())) {
            oldPlan.setPlanType(newPlan.getPlanType());
        }

        // Update creationDate
        if (newPlan.getCreationDate() != null && !newPlan.getCreationDate().equals(oldPlan.getCreationDate())) {
            oldPlan.setCreationDate(newPlan.getCreationDate());
        }

        // Update planCostShares
        if (newPlan.getPlanCostShares() != null) {
            MemberCostShare nCostShare = newPlan.getPlanCostShares();
            MemberCostShare oCostShare = oldPlan.getPlanCostShares();
            // Update deductible
            if (nCostShare.getDeductible() != oCostShare.getDeductible()) {
                oCostShare.setDeductible(nCostShare.getDeductible());
            }
            // Update org
            if (nCostShare.get_org() != null && !nCostShare.get_org().equals(oCostShare.get_org())) {
                oCostShare.set_org(nCostShare.get_org());
            }
            // Update copay
            if (nCostShare.getCopay() != oCostShare.getCopay()) {
                oCostShare.setCopay(nCostShare.getCopay());
            }
            // Update objectType
            if (nCostShare.getObjectType() != null && !nCostShare.getObjectType().equals(oCostShare.getObjectType())) {
                oCostShare.setObjectType(nCostShare.getObjectType());
            }

            oldPlan.setPlanCostShares(oCostShare);
        }

        // Update linkedPlanServices
        if (newPlan.getLinkedPlanServices() != null) {
            List<PlanService> newService = newPlan.getLinkedPlanServices();
            List<PlanService> oldService = oldPlan.getLinkedPlanServices();
            HashMap<String, PlanService> oldServiceMap = new HashMap<>();
            for (PlanService planService : oldService) {
                oldServiceMap.put(planService.getObjectId(), planService);
            }
            for (PlanService nPlanService : newService) {
                if (!oldServiceMap.containsKey(nPlanService.getObjectId())) {
                    oldServiceMap.put(nPlanService.getObjectId(), nPlanService);
                } else {
                    PlanService oPlanService = oldServiceMap.get(nPlanService.getObjectId());
                    Service oLinkedService = oPlanService.getLinkedService();
                    Service nLinkedService = nPlanService.getLinkedService();
                    // Update linkedService
                    if (nLinkedService != null) {
                        // Update org
                        if (nLinkedService.get_org() != null && !nLinkedService.get_org().equals(oLinkedService.get_org())) {
                            oLinkedService.set_org(nLinkedService.get_org());
                        }
                        // Update objectType
                        if (nLinkedService.getObjectType() != null && !nLinkedService.getObjectType().equals(oLinkedService.getObjectType())) {
                            oLinkedService.setObjectType(nLinkedService.getObjectType());
                        }
                        //Update Name
                        if (nLinkedService.getName() != null && !nLinkedService.getName().equals(oLinkedService.getName())) {
                            oLinkedService.setName(nLinkedService.getName());
                        }
                        oPlanService.setLinkedService(oLinkedService);
                    }

                    MemberCostShare oCostShare = oPlanService.getPlanserviceCostShares();
                    MemberCostShare nCostShare = nPlanService.getPlanserviceCostShares();
                    // Update planserviceCostShares
                    if (nCostShare != null) {
                        // Update deductible
                        if (nCostShare.getDeductible() != oCostShare.getDeductible()) {
                            oCostShare.setDeductible(nCostShare.getDeductible());
                        }
                        // Update org
                        if (nCostShare.get_org() != null && !nCostShare.get_org().equals(oCostShare.get_org())) {
                            oCostShare.set_org(nCostShare.get_org());
                        }
                        // Update copay
                        if (nCostShare.getCopay() != oCostShare.getCopay()) {
                            oCostShare.setCopay(nCostShare.getCopay());
                        }
                        // Update objectType
                        if (nCostShare.getObjectType() != null && !nCostShare.getObjectType().equals(oCostShare.getObjectType())) {
                            oCostShare.setObjectType(nCostShare.getObjectType());
                        }
                        oPlanService.setPlanserviceCostShares(oCostShare);
                    }

                    // Update org
                    if (nPlanService.get_org() != null && !nPlanService.get_org().equals(oPlanService.get_org())) {
                        oPlanService.set_org(nPlanService.get_org());
                    }

                    // Update objectType
                    if (nPlanService.getObjectType() != null && !nPlanService.getObjectType().equals(oPlanService.getObjectType())) {
                        oPlanService.setObjectType(nPlanService.getObjectType());
                    }

                    oldServiceMap.put(oPlanService.getObjectId(), oPlanService);
                }
            }
            List<PlanService> updatedLinkedPlanServices = new ArrayList<>(oldServiceMap.values());
            oldPlan.setLinkedPlanServices(updatedLinkedPlanServices);
        }
        return oldPlan;
    }


}
