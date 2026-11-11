package com.sass.ramel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sass.ramel.dto.CompanyRoleManagementModels.SubscriptionView;
import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeCrudAccessView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeCrudAccessRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeRolesRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.CreateCompanyRoleRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.RoleOption;
import com.sass.ramel.dto.CompanyRoleManagementModels.UserProductAccessView;
import com.sass.ramel.security.AuthenticatedUser;
import com.sass.ramel.service.CompanyRoleManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/company/role-management")
@RequiredArgsConstructor
public class CompanyRoleManagementController {

    private final CompanyRoleManagementService service;

    @GetMapping({"/access", "/acces"})
    public ResponseEntity<Boolean> getAccess(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(service.canManageRoles(currentUser.id()));
    }

    @GetMapping
    public List<SubscriptionView> getManagedSubscriptions(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getManagedSubscriptions(currentUser.id());
    }

    @PostMapping("/subscriptions/{subscriptionId}/roles")
    public ResponseEntity<RoleOption> createCompanyRole(
            @PathVariable long subscriptionId,
            @RequestBody CreateCompanyRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        RoleOption role = service.createCompanyRole(
                currentUser.id(), subscriptionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    @GetMapping("/my-access")
    public List<UserProductAccessView> getMyProductAccess(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getUserProductAccess(currentUser.id());
    }

    @GetMapping("/subscriptions/{subscriptionId}/employees/{employeeId}/permissions")
    public List<EmployeeCrudAccessView> getEmployeeCrudAccess(
            @PathVariable long subscriptionId,
            @PathVariable long employeeId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getEmployeeCrudAccess(
                currentUser.id(), subscriptionId, employeeId);
    }

            @PutMapping(
                "/subscriptions/{subscriptionId}/employees/{employeeId}/permissions/{resourceKey}")
            public ResponseEntity<Void> updateEmployeeCrudAccess(
                @PathVariable long subscriptionId,
                @PathVariable long employeeId,
                @PathVariable String resourceKey,
                @RequestBody UpdateEmployeeCrudAccessRequest request,
                @AuthenticationPrincipal AuthenticatedUser currentUser) {
            service.updateEmployeeCrudAccess(
                currentUser.id(), subscriptionId, employeeId, resourceKey, request);
            return ResponseEntity.noContent().build();
            }

            @DeleteMapping(
                "/subscriptions/{subscriptionId}/employees/{employeeId}/permissions/{resourceKey}")
            public ResponseEntity<Void> resetEmployeeCrudAccess(
                @PathVariable long subscriptionId,
                @PathVariable long employeeId,
                @PathVariable String resourceKey,
                @AuthenticationPrincipal AuthenticatedUser currentUser) {
            service.resetEmployeeCrudAccess(
                currentUser.id(), subscriptionId, employeeId, resourceKey);
            return ResponseEntity.noContent().build();
            }

    @PutMapping("/subscriptions/{subscriptionId}/employees/{employeeId}")
    public ResponseEntity<Void> updateEmployeeRoles(
            @PathVariable long subscriptionId,
            @PathVariable long employeeId,
            @RequestBody UpdateEmployeeRolesRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        service.updateEmployeeRoles(
                currentUser.id(), subscriptionId, employeeId, request);
        return ResponseEntity.noContent().build();
    }
}
