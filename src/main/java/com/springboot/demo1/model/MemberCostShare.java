package com.springboot.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberCostShare implements Serializable {
    public int deductible;
    public String _org;
    public int copay;
    public String objectId;
    public String objectType;
}
