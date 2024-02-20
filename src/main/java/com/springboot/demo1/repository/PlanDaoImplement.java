package com.springboot.demo1.repository;

import com.springboot.demo1.model.Plan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlanDaoImplement implements PlanDao {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY = "Plan";

    @Override
    public boolean savePlan(Plan plan) {
        try {
            redisTemplate.opsForHash().put(KEY, plan.getObjectId(), plan);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Plan fetchPlanById(String id) {
        Plan plan;
        plan = (Plan) redisTemplate.opsForHash().get(KEY, id);
        return plan;
    }

    @Override
    public boolean deletePlan(String id) {
        try {
            Long x = redisTemplate.opsForHash().delete(KEY, id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
