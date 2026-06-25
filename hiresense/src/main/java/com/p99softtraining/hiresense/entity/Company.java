package com.p99softtraining.hiresense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="companies")
public class Company extends BaseEntity{

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Email
    @Column(nullable = false)
    private String email;


}
