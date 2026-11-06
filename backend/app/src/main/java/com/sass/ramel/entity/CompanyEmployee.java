package com.sass.ramel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company_employees")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CompanyEmployee {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;
}
