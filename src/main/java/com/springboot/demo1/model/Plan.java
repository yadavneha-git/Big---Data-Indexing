package com.springboot.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Plan implements Serializable {

      private MemberCostShare planCostShares;
      private List<PlanService> linkedPlanServices;
      private String _org;
      private String objectId;
      private String objectType;
      private String planType;
      private String creationDate;
}
