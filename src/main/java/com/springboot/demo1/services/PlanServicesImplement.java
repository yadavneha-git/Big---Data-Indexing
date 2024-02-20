package com.springboot.demo1.services;

import com.springboot.demo1.model.Plan;
import com.springboot.demo1.repository.PlanDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanServicesImplement implements PlanServices {
    @Autowired
    private PlanDao planDao;

    @Override
    public boolean savePlan(Plan plan) {
        return planDao.savePlan(plan);
    }


    @Override
    public Plan fetchPlanById(String id) {
        return planDao.fetchPlanById(id);
    }

    @Override
    public boolean deletePlan(String id) {
        return planDao.deletePlan(id);
    }

    @Override
    public boolean updatePlan(String id, Plan plan) {
        return planDao.savePlan(plan) ;
    }

    @Override
    public boolean putPlan(String id, Plan plan) {
        return planDao.savePlan(plan) ;
    }

}
