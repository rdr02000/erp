package com.sass.ramel.dto;

import java.util.List;

public final class CompanyRoleManagementModels {

    private CompanyRoleManagementModels() {
    }

    public record SubscriptionView(
            long subscriptionId,
            String companyName,
            String productCode,
            String productName,
            List<RoleOption> roles,
            List<EmployeeRoleView> employees) {
    }

    public record RoleOption(long roleId, String roleKey, String name) {
    }

    public record EmployeeRoleView(
            long employeeId,
            long userId,
            String username,
            String firstName,
            String lastName,
            boolean hasProductAccess,
            List<Long> roleIds) {
    }

    public record EmployeeCrudAccessView(
            long employeeId,
            long companyId,
            long subscriptionId,
            long productId,
            String productCode,
            String productName,
            String resourceKey,
            boolean canCreate,
            boolean canRead,
            boolean canUpdate,
            boolean canDelete,
            boolean hasOverride) {
    }

    public record UpdateEmployeeCrudAccessRequest(
            Boolean canCreate,
            Boolean canRead,
            Boolean canUpdate,
            Boolean canDelete) {
    }

    public record UpdateEmployeeRolesRequest(boolean hasProductAccess, List<Long> roleIds) {
    }

    public record CreateCompanyRoleRequest(String roleKey, String name) {
    }

    public record UserProductAccessView(
            long subscriptionId,
            String companyName,
            String productCode,
            String productName,
            List<String> roles,
            List<UserResourcePermissionView> permissions) {
    }

    public record UserResourcePermissionView(
            String resourceKey,
            boolean canCreate,
            boolean canRead,
            boolean canUpdate,
            boolean canDelete,
            boolean hasOverride) {
    }
}
