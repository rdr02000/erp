package com.sass.ramel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeCrudAccessView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeCrudAccessRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.SubscriptionView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeRolesRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.CreateCompanyRoleRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.RoleOption;
import com.sass.ramel.dto.CompanyRoleManagementModels.UserProductAccessView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UserResourcePermissionView;
import com.sass.ramel.repository.CompanyRoleManagementRepository;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Transactional
    public RoleOption createCompanyRole(
            long userId,
            long subscriptionId,
            CreateCompanyRoleRequest request) {
        if (!repository.hasAdministratorAccess(userId, subscriptionId)) {
            throw new AccessDeniedException(
                    "Company administrator access is required for this subscription.");
        }
        if (request == null
                || request.roleKey() == null
                || !request.roleKey().matches("[a-z][a-z0-9_]{0,99}")
                || request.name() == null
                || request.name().isBlank()
                || request.name().strip().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A role name of at most 150 characters and a valid role key are required.");
        }

        String roleName = request.name().strip();
        if (repository.insertCompanyRole(subscriptionId, request.roleKey(), roleName) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A role with this key already exists for the subscription.");
        }
        return repository.findRoleBySubscriptionAndKey(subscriptionId, request.roleKey())
                .map(role -> new RoleOption(role.getRoleId(), role.getRoleKey(), role.getName()))
                .orElseThrow(() -> new IllegalStateException(
                        "The created company role could not be loaded."));
    }

    public List<UserProductAccessView> getUserProductAccess(long userId) {
        Map<Long, UserProductAccessAccumulator> accessBySubscription =
                new LinkedHashMap<>();
        for (CompanyRoleManagementRepository.UserProductRoleProjection row
                : repository.findUserProductRoles(userId)) {
            UserProductAccessAccumulator access = accessBySubscription.computeIfAbsent(
                    row.getSubscriptionId(),
                    ignored -> new UserProductAccessAccumulator(
                            row.getSubscriptionId(),
                            row.getCompanyName(),
                            row.getProductCode(),
                            row.getProductName()));
            if (row.getRoleName() != null) {
                access.roles.add(row.getRoleName());
            }
        }

        for (CompanyRoleManagementRepository.UserProductPermissionProjection row
                : repository.findUserProductPermissions(userId)) {
            UserProductAccessAccumulator access =
                    accessBySubscription.get(row.getSubscriptionId());
            if (access != null) {
                access.permissions.add(new UserResourcePermissionView(
                        row.getResourceKey(),
                        row.isCanCreate(),
                        row.isCanRead(),
                        row.isCanUpdate(),
                        row.isCanDelete(),
                        row.isHasOverride()));
            }
        }

        return accessBySubscription.values().stream()
                .map(UserProductAccessAccumulator::toView)
                .toList();
    }

    private static final class UserProductAccessAccumulator {
        private final long subscriptionId;
        private final String companyName;
        private final String productCode;
        private final String productName;
        private final List<String> roles = new ArrayList<>();
        private final List<UserResourcePermissionView> permissions = new ArrayList<>();

        private UserProductAccessAccumulator(
                long subscriptionId,
                String companyName,
                String productCode,
                String productName) {
            this.subscriptionId = subscriptionId;
            this.companyName = companyName;
            this.productCode = productCode;
            this.productName = productName;
        }

        private UserProductAccessView toView() {
            return new UserProductAccessView(
                    subscriptionId,
                    companyName,
                    productCode,
                    productName,
                    List.copyOf(roles),
                    List.copyOf(permissions));
        }
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
