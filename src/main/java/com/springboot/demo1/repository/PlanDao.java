package com.springboot.demo1.repository;


import com.springboot.demo1.model.Plan;

public interface PlanDao {
    boolean savePlan(Plan plan);

    Plan fetchPlanById(String id);

    boolean deletePlan(String id);



}
