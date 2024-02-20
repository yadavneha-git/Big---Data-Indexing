package com.springboot.demo1.services;

import com.springboot.demo1.model.Plan;

public interface PlanServices {

    boolean savePlan(Plan plan);

    Plan fetchPlanById(String id);

    boolean deletePlan(String id);

    boolean updatePlan(String id, Plan plan);

    boolean putPlan(String id, Plan plan);
}
