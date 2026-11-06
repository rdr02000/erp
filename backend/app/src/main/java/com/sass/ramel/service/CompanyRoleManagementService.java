package com.sass.ramel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeRoleView;
import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeCrudAccessView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeCrudAccessRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.RoleOption;
import com.sass.ramel.dto.CompanyRoleManagementModels.SubscriptionView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeRolesRequest;
import com.sass.ramel.repository.CompanyRoleManagementRepository;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanyRoleManagementService {

    private final CompanyRoleManagementRepository repository;

    public boolean canManageRoles(long userId) {
        return repository.hasAdministratorAccess(userId);
    }

    public List<SubscriptionView> getManagedSubscriptions(long userId) {
        List<SubscriptionView> subscriptions =
                repository.findAdministratorSubscriptions(userId);
        if (subscriptions.isEmpty()) {
            throw new AccessDeniedException("Company administrator access is required.");
        }
        return subscriptions;
    }

    public List<EmployeeCrudAccessView> getEmployeeCrudAccess(
            long userId,
            long subscriptionId,
            long employeeId) {
        if (!repository.hasAdministratorAccess(userId, subscriptionId)) {
            throw new AccessDeniedException(
                    "Company administrator access is required for this subscription.");
        }
        if (!repository.employeeBelongsToSubscriptionCompany(employeeId, subscriptionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Employee does not belong to this subscription's company.");
        }
        return repository.findEmployeeCrudAccess(employeeId, subscriptionId).stream()
                .map(access -> new EmployeeCrudAccessView(
                        access.getEmployeeId(),
                        access.getCompanyId(),
                        access.getSubscriptionId(),
                        access.getProductId(),
                        access.getProductCode(),
                        access.getProductName(),
                        access.getResourceKey(),
                        access.isCanCreate(),
                        access.isCanRead(),
                        access.isCanUpdate(),
                        access.isCanDelete(),
                        access.isHasOverride()))
                .toList();
    }

                @Transactional
                public void updateEmployeeCrudAccess(
                    long userId,
                    long subscriptionId,
                    long employeeId,
                    String resourceKey,
                    UpdateEmployeeCrudAccessRequest request) {
                ensureEmployeePermissionScope(userId, subscriptionId, employeeId, resourceKey);
                if (request == null
                    || request.canCreate() == null
                    || request.canRead() == null
                    || request.canUpdate() == null
                    || request.canDelete() == null) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "All CRUD permission flags are required.");
                }
                repository.saveEmployeeCrudOverride(
                    employeeId,
                    subscriptionId,
                    resourceKey,
                    request.canCreate(),
                    request.canRead(),
                    request.canUpdate(),
                    request.canDelete());
                }

                @Transactional
                public void resetEmployeeCrudAccess(
                    long userId,
                    long subscriptionId,
                    long employeeId,
                    String resourceKey) {
                ensureEmployeePermissionScope(userId, subscriptionId, employeeId, resourceKey);
                repository.deleteEmployeeCrudOverride(employeeId, subscriptionId, resourceKey);
                }

                private void ensureEmployeePermissionScope(
                    long userId,
                    long subscriptionId,
                    long employeeId,
                    String resourceKey) {
                if (!repository.hasAdministratorAccess(userId, subscriptionId)) {
                    throw new AccessDeniedException(
                        "Company administrator access is required for this subscription.");
                }
                if (!repository.employeeBelongsToSubscriptionCompany(employeeId, subscriptionId)) {
                    throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee does not belong to this subscription's company.");
                }
                if (resourceKey == null || resourceKey.isBlank() || resourceKey.length() > 150
                    || !repository.employeeHasResourcePermission(
                        employeeId, subscriptionId, resourceKey)) {
                    throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Resource is not assigned to this employee.");
                }
                }

    @Transactional
    public void updateEmployeeRoles(
            long userId,
            long subscriptionId,
            long employeeId,
            UpdateEmployeeRolesRequest request) {
        if (!repository.hasAdministratorAccess(userId, subscriptionId)) {
            throw new AccessDeniedException(
                    "Company administrator access is required for this subscription.");
        }
        if (request == null || request.roleIds() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A role ID list is required.");
        }
        List<Long> roleIds = request.roleIds();
        if (roleIds.size() > 100
                || roleIds.stream().anyMatch(roleId -> roleId == null || roleId <= 0)
                || new HashSet<>(roleIds).size() != roleIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "At most 100 unique positive role IDs are allowed.");
        }
        if (!repository.employeeBelongsToSubscriptionCompany(employeeId, subscriptionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Employee does not belong to this subscription's company.");
        }
        if (!request.hasProductAccess()) {
            if (!roleIds.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Roles cannot be assigned without product access.");
            }
            repository.revokeProductAccess(employeeId, subscriptionId);
            return;
        }
        if (!repository.rolesBelongToSubscription(roleIds, subscriptionId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Every role must belong to this subscription.");
        }
        repository.grantProductAccess(employeeId, subscriptionId);
        repository.replaceEmployeeRoles(employeeId, subscriptionId, userId, roleIds);
    }
}
