package com.springboot.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service implements Serializable {
    private String _org;
    private String objectId;
    private String objectType;
    private String name;
}
