package com.sass.ramel.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeRoleView;
import com.sass.ramel.dto.CompanyRoleManagementModels.RoleOption;
import com.sass.ramel.dto.CompanyRoleManagementModels.SubscriptionView;
import com.sass.ramel.entity.CompanyEmployee;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CompanyRoleManagementRepository
        extends ListCrudRepository<CompanyEmployee, Long> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM company_employee_product_access AS access
                JOIN company_employees AS employee
                    ON employee.employee_id = access.employee_id
                JOIN company_product_subscriptions AS subscription
                    ON subscription.id = access.company_product_subscription_id
                JOIN company_employee_roles AS employee_role
                    ON employee_role.employee_id = access.employee_id
                   AND employee_role.company_product_subscription_id =
                       access.company_product_subscription_id
                JOIN company_subscription_roles AS role
                    ON role.role_id = employee_role.role_id
                   AND role.company_product_subscription_id =
                       employee_role.company_product_subscription_id
                JOIN products AS product
                    ON product.product_id = subscription.product_id
                WHERE employee.user_id = :userId
                  AND access.company_product_subscription_id = :subscriptionId
                  AND employee.status = 'ACTIVE'
                  AND subscription.status = 'ACTIVE'
                  AND subscription.subscribed_at <= CURRENT_TIMESTAMP
                  AND (subscription.expires_at IS NULL
                       OR subscription.expires_at > CURRENT_TIMESTAMP)
                  AND product.is_active
                  AND role.role_key = 'company_admin'
            )
            """, nativeQuery = true)
    boolean hasAdministratorAccess(
            @Param("userId") long userId,
            @Param("subscriptionId") long subscriptionId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM company_employee_product_access AS access
                JOIN company_employees AS employee
                    ON employee.employee_id = access.employee_id
                JOIN company_product_subscriptions AS subscription
                    ON subscription.id = access.company_product_subscription_id
                JOIN company_employee_roles AS employee_role
                    ON employee_role.employee_id = access.employee_id
                   AND employee_role.company_product_subscription_id =
                       access.company_product_subscription_id
                JOIN company_subscription_roles AS role
                    ON role.role_id = employee_role.role_id
                   AND role.company_product_subscription_id =
                       employee_role.company_product_subscription_id
                JOIN products AS product
                    ON product.product_id = subscription.product_id
                WHERE employee.user_id = :userId
                  AND employee.status = 'ACTIVE'
                  AND subscription.status = 'ACTIVE'
                  AND subscription.subscribed_at <= CURRENT_TIMESTAMP
                  AND (subscription.expires_at IS NULL
                       OR subscription.expires_at > CURRENT_TIMESTAMP)
                  AND product.is_active
                  AND role.role_key = 'company_admin'
            )
            """, nativeQuery = true)
    boolean hasAdministratorAccess(@Param("userId") long userId);

    @Query(value = """
            SELECT DISTINCT
                subscription.id AS "subscriptionId",
                company.name AS "companyName",
                product.product_code AS "productCode",
                product.name AS "productName"
            FROM company_employee_product_access AS access
            JOIN company_employees AS employee
                ON employee.employee_id = access.employee_id
            JOIN company_product_subscriptions AS subscription
                ON subscription.id = access.company_product_subscription_id
            JOIN company_employee_roles AS employee_role
                ON employee_role.employee_id = access.employee_id
               AND employee_role.company_product_subscription_id =
                   access.company_product_subscription_id
            JOIN company_subscription_roles AS role
                ON role.role_id = employee_role.role_id
               AND role.company_product_subscription_id =
                   employee_role.company_product_subscription_id
            JOIN companies AS company
                ON company.company_id = subscription.company_id
            JOIN products AS product
                ON product.product_id = subscription.product_id
            WHERE employee.user_id = :userId
              AND employee.status = 'ACTIVE'
              AND subscription.status = 'ACTIVE'
              AND subscription.subscribed_at <= CURRENT_TIMESTAMP
              AND (subscription.expires_at IS NULL
                   OR subscription.expires_at > CURRENT_TIMESTAMP)
              AND product.is_active
              AND role.role_key = 'company_admin'
            ORDER BY company.name, product.name
            """, nativeQuery = true)
    List<SubscriptionProjection> findAdministratorSubscriptionProjections(
            @Param("userId") long userId);

    @Query(value = """
            SELECT
                role.company_product_subscription_id AS "subscriptionId",
                role.role_id AS "roleId",
                role.role_key AS "roleKey",
                role.name AS "name"
            FROM company_subscription_roles AS role
            WHERE role.company_product_subscription_id IN (:subscriptionIds)
            ORDER BY role.company_product_subscription_id, role.name, role.role_key
            """, nativeQuery = true)
    List<RoleProjection> findRoleProjections(
            @Param("subscriptionIds") List<Long> subscriptionIds);

    @Query(value = """
            SELECT
                subscription.id AS "subscriptionId",
                company.name AS "companyName",
                product.product_code AS "productCode",
                product.name AS "productName",
                role.name AS "roleName"
            FROM company_employees AS employee
            JOIN company_employee_product_access AS access
                ON access.employee_id = employee.employee_id
            JOIN company_product_subscriptions AS subscription
                ON subscription.id = access.company_product_subscription_id
            JOIN companies AS company
                ON company.company_id = subscription.company_id
            JOIN products AS product
                ON product.product_id = subscription.product_id
            LEFT JOIN company_employee_roles AS employee_role
                ON employee_role.employee_id = access.employee_id
               AND employee_role.company_product_subscription_id =
                   access.company_product_subscription_id
            LEFT JOIN company_subscription_roles AS role
                ON role.role_id = employee_role.role_id
               AND role.company_product_subscription_id =
                   employee_role.company_product_subscription_id
            WHERE employee.user_id = :userId
              AND employee.status = 'ACTIVE'
              AND subscription.status = 'ACTIVE'
              AND subscription.subscribed_at <= CURRENT_TIMESTAMP
              AND (subscription.expires_at IS NULL
                   OR subscription.expires_at > CURRENT_TIMESTAMP)
              AND product.is_active
            ORDER BY company.name, product.name, role.name
            """, nativeQuery = true)
    List<UserProductRoleProjection> findUserProductRoles(@Param("userId") long userId);

    @Query(value = """
            SELECT
                subscription.id AS "subscriptionId",
                permissions.resource_key AS "resourceKey",
                permissions.can_create AS "canCreate",
                permissions.can_read AS "canRead",
                permissions.can_update AS "canUpdate",
                permissions.can_delete AS "canDelete",
                permissions.has_override AS "hasOverride"
            FROM company_employees AS employee
            JOIN company_employee_product_access AS access
                ON access.employee_id = employee.employee_id
            JOIN company_product_subscriptions AS subscription
                ON subscription.id = access.company_product_subscription_id
            JOIN products AS product
                ON product.product_id = subscription.product_id
            JOIN company_employee_product_permissions AS permissions
                ON permissions.employee_id = access.employee_id
               AND permissions.company_product_subscription_id =
                   access.company_product_subscription_id
            WHERE employee.user_id = :userId
              AND employee.status = 'ACTIVE'
              AND subscription.status = 'ACTIVE'
              AND subscription.subscribed_at <= CURRENT_TIMESTAMP
              AND (subscription.expires_at IS NULL
                   OR subscription.expires_at > CURRENT_TIMESTAMP)
              AND product.is_active
            ORDER BY subscription.id, permissions.resource_key
            """, nativeQuery = true)
    List<UserProductPermissionProjection> findUserProductPermissions(
            @Param("userId") long userId);

    @Modifying
    @Query(value = """
            INSERT INTO company_subscription_roles (
                company_product_subscription_id,
                role_key,
                name
            )
            VALUES (
                :subscriptionId,
                :roleKey,
                :name
            )
            ON CONFLICT (company_product_subscription_id, role_key) DO NOTHING
            """, nativeQuery = true)
    int insertCompanyRole(
            @Param("subscriptionId") long subscriptionId,
            @Param("roleKey") String roleKey,
            @Param("name") String name);

    @Query(value = """
            SELECT
                role.company_product_subscription_id AS "subscriptionId",
                role.role_id AS "roleId",
                role.role_key AS "roleKey",
                role.name AS "name"
            FROM company_subscription_roles AS role
            WHERE role.company_product_subscription_id = :subscriptionId
              AND role.role_key = :roleKey
            """, nativeQuery = true)
    Optional<RoleProjection> findRoleBySubscriptionAndKey(
            @Param("subscriptionId") long subscriptionId,
            @Param("roleKey") String roleKey);

    @Query(value = """
            SELECT
                subscription.id AS "subscriptionId",
                employee.employee_id AS "employeeId",
                app_user.id AS "userId",
                app_user.username AS "username",
                profile.first_name AS "firstName",
                profile.last_name AS "lastName",
                (access.employee_id IS NOT NULL) AS "hasProductAccess",
                employee_role.role_id AS "roleId"
            FROM company_product_subscriptions AS subscription
            JOIN company_employees AS employee
                ON employee.company_id = subscription.company_id
            JOIN app_users AS app_user
                ON app_user.id = employee.user_id
            LEFT JOIN user_profiles AS profile
                ON profile.user_id = app_user.id
            LEFT JOIN company_employee_product_access AS access
                ON access.employee_id = employee.employee_id
               AND access.company_product_subscription_id = subscription.id
            LEFT JOIN company_employee_roles AS employee_role
                ON employee_role.employee_id = access.employee_id
               AND employee_role.company_product_subscription_id =
                   access.company_product_subscription_id
            WHERE subscription.id IN (:subscriptionIds)
              AND employee.status = 'ACTIVE'
            ORDER BY subscription.id,
                     app_user.username,
                     employee.employee_id
            """, nativeQuery = true)
    List<EmployeeRoleProjection> findEmployeeRoleProjections(
            @Param("subscriptionIds") List<Long> subscriptionIds);

    @Query(value = """
            SELECT
                permissions.employee_id AS "employeeId",
                permissions.company_id AS "companyId",
                permissions.company_product_subscription_id AS "subscriptionId",
                permissions.product_id AS "productId",
                product.product_code AS "productCode",
                product.name AS "productName",
                permissions.resource_key AS "resourceKey",
                permissions.can_create AS "canCreate",
                permissions.can_read AS "canRead",
                permissions.can_update AS "canUpdate",
                permissions.can_delete AS "canDelete",
                permissions.has_override AS "hasOverride"
            FROM company_employee_product_permissions AS permissions
            JOIN products AS product
                ON product.product_id = permissions.product_id
            WHERE permissions.employee_id = :employeeId
              AND permissions.company_product_subscription_id = :subscriptionId
            ORDER BY product.name, permissions.resource_key
            """, nativeQuery = true)
    List<EmployeeCrudAccessProjection> findEmployeeCrudAccess(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM company_employee_product_access AS access
                JOIN company_employee_roles AS employee_role
                    ON employee_role.employee_id = access.employee_id
                   AND employee_role.company_product_subscription_id =
                       access.company_product_subscription_id
                JOIN company_role_permissions AS permission
                    ON permission.role_id = employee_role.role_id
                   AND permission.company_product_subscription_id =
                       employee_role.company_product_subscription_id
                WHERE access.employee_id = :employeeId
                  AND access.company_product_subscription_id = :subscriptionId
                  AND permission.resource_key = :resourceKey
            )
            """, nativeQuery = true)
    boolean employeeHasResourcePermission(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId,
            @Param("resourceKey") String resourceKey);

    @Modifying
    @Query(value = """
            INSERT INTO company_employee_permission_overrides (
                employee_id,
                company_product_subscription_id,
                resource_key,
                can_create,
                can_read,
                can_update,
                can_delete,
                updated_at
            )
            VALUES (
                :employeeId,
                :subscriptionId,
                :resourceKey,
                :canCreate,
                :canRead,
                :canUpdate,
                :canDelete,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (employee_id, company_product_subscription_id, resource_key)
            DO UPDATE SET
                can_create = EXCLUDED.can_create,
                can_read = EXCLUDED.can_read,
                can_update = EXCLUDED.can_update,
                can_delete = EXCLUDED.can_delete,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void saveEmployeeCrudOverride(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId,
            @Param("resourceKey") String resourceKey,
            @Param("canCreate") boolean canCreate,
            @Param("canRead") boolean canRead,
            @Param("canUpdate") boolean canUpdate,
            @Param("canDelete") boolean canDelete);

    @Modifying
    @Query(value = """
            DELETE FROM company_employee_permission_overrides
            WHERE employee_id = :employeeId
              AND company_product_subscription_id = :subscriptionId
              AND resource_key = :resourceKey
            """, nativeQuery = true)
    void deleteEmployeeCrudOverride(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId,
            @Param("resourceKey") String resourceKey);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM company_employees AS employee
                JOIN company_product_subscriptions AS subscription
                    ON subscription.company_id = employee.company_id
                WHERE employee.employee_id = :employeeId
                  AND subscription.id = :subscriptionId
            )
            """, nativeQuery = true)
    boolean employeeBelongsToSubscriptionCompany(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId);

    @Modifying
    @Query(value = """
            INSERT INTO company_employee_product_access (
                employee_id,
                company_id,
                company_product_subscription_id
            )
            SELECT
                employee.employee_id,
                employee.company_id,
                subscription.id
            FROM company_employees AS employee
            JOIN company_product_subscriptions AS subscription
                ON subscription.company_id = employee.company_id
            WHERE employee.employee_id = :employeeId
              AND subscription.id = :subscriptionId
            ON CONFLICT (employee_id, company_product_subscription_id) DO NOTHING
            """, nativeQuery = true)
    void grantProductAccess(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId);

    @Modifying
    @Query(value = """
            DELETE FROM company_employee_product_access
            WHERE employee_id = :employeeId
              AND company_product_subscription_id = :subscriptionId
            """, nativeQuery = true)
    void revokeProductAccess(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId);

    @Query(value = """
            SELECT COUNT(DISTINCT role_id)
            FROM company_subscription_roles
            WHERE company_product_subscription_id = :subscriptionId
              AND role_id IN (:roleIds)
            """, nativeQuery = true)
    long countRolesInSubscription(
            @Param("roleIds") List<Long> roleIds,
            @Param("subscriptionId") long subscriptionId);

    @Modifying
    @Query(value = """
            DELETE FROM company_employee_roles
            WHERE employee_id = :employeeId
              AND company_product_subscription_id = :subscriptionId
            """, nativeQuery = true)
    void deleteEmployeeRoles(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId);

    @Modifying
    @Query(value = """
            INSERT INTO company_employee_roles (
                employee_id,
                company_product_subscription_id,
                role_id,
                assigned_by_user_id
            )
            VALUES (
                :employeeId,
                :subscriptionId,
                :roleId,
                :assignedByUserId
            )
            """, nativeQuery = true)
    void insertEmployeeRole(
            @Param("employeeId") long employeeId,
            @Param("subscriptionId") long subscriptionId,
            @Param("roleId") long roleId,
            @Param("assignedByUserId") long assignedByUserId);

    default List<SubscriptionView> findAdministratorSubscriptions(long userId) {
        List<SubscriptionProjection> subscriptions =
                findAdministratorSubscriptionProjections(userId);
        if (subscriptions.isEmpty()) {
            return List.of();
        }

        List<Long> subscriptionIds = subscriptions.stream()
                .map(SubscriptionProjection::getSubscriptionId)
                .toList();
        Map<Long, List<RoleOption>> rolesBySubscription = new LinkedHashMap<>();
        for (Long subscriptionId : subscriptionIds) {
            rolesBySubscription.put(subscriptionId, new ArrayList<>());
        }
        for (RoleProjection role : findRoleProjections(subscriptionIds)) {
            rolesBySubscription.get(role.getSubscriptionId()).add(
                    new RoleOption(role.getRoleId(), role.getRoleKey(), role.getName()));
        }

        class EmployeeAccumulator {
            private final long employeeId;
            private final long userId;
            private final String username;
            private final String firstName;
            private final String lastName;
            private final boolean hasProductAccess;
            private final List<Long> roleIds = new ArrayList<>();

            private EmployeeAccumulator(EmployeeRoleProjection row) {
                employeeId = row.getEmployeeId();
                userId = row.getUserId();
                username = row.getUsername();
                firstName = row.getFirstName();
                lastName = row.getLastName();
                hasProductAccess = row.isHasProductAccess();
            }
        }

        Map<Long, Map<Long, EmployeeAccumulator>> employeesBySubscription =
                new LinkedHashMap<>();
        for (Long subscriptionId : subscriptionIds) {
            employeesBySubscription.put(subscriptionId, new LinkedHashMap<>());
        }
        for (EmployeeRoleProjection row : findEmployeeRoleProjections(subscriptionIds)) {
            Map<Long, EmployeeAccumulator> employees =
                    employeesBySubscription.get(row.getSubscriptionId());
            EmployeeAccumulator employee = employees.computeIfAbsent(
                    row.getEmployeeId(), ignored -> new EmployeeAccumulator(row));
            if (row.getRoleId() != null) {
                employee.roleIds.add(row.getRoleId());
            }
        }

        return subscriptions.stream()
                .map(subscription -> new SubscriptionView(
                        subscription.getSubscriptionId(),
                        subscription.getCompanyName(),
                        subscription.getProductCode(),
                        subscription.getProductName(),
                        List.copyOf(rolesBySubscription.get(subscription.getSubscriptionId())),
                        employeesBySubscription.get(subscription.getSubscriptionId())
                                .values().stream()
                                .map(employee -> new EmployeeRoleView(
                                        employee.employeeId,
                                        employee.userId,
                                        employee.username,
                                        employee.firstName,
                                        employee.lastName,
                                        employee.hasProductAccess,
                                        List.copyOf(employee.roleIds)))
                                .toList()))
                .toList();
    }

    default boolean rolesBelongToSubscription(List<Long> roleIds, long subscriptionId) {
        return roleIds.isEmpty()
                || countRolesInSubscription(roleIds, subscriptionId) == roleIds.size();
    }

    @Transactional
    default void replaceEmployeeRoles(
            long employeeId,
            long subscriptionId,
            long assignedByUserId,
            List<Long> roleIds) {
        deleteEmployeeRoles(employeeId, subscriptionId);
        roleIds.forEach(roleId ->
                insertEmployeeRole(employeeId, subscriptionId, roleId, assignedByUserId));
    }

    interface SubscriptionProjection {
        long getSubscriptionId();

        String getCompanyName();

        String getProductCode();

        String getProductName();
    }

    interface RoleProjection {
        long getSubscriptionId();

        long getRoleId();

        String getRoleKey();

        String getName();
    }

    interface UserProductRoleProjection {
        long getSubscriptionId();

        String getCompanyName();

        String getProductCode();

        String getProductName();

        String getRoleName();
    }

    interface UserProductPermissionProjection {
        long getSubscriptionId();

        String getResourceKey();

        boolean isCanCreate();

        boolean isCanRead();

        boolean isCanUpdate();

        boolean isCanDelete();

        boolean isHasOverride();
    }

    interface EmployeeRoleProjection {
        long getSubscriptionId();

        long getEmployeeId();

        long getUserId();

        String getUsername();

        String getFirstName();

        String getLastName();

        boolean isHasProductAccess();

        Long getRoleId();
    }

    interface EmployeeCrudAccessProjection {
        long getEmployeeId();

        long getCompanyId();

        long getSubscriptionId();

        long getProductId();

        String getProductCode();

        String getProductName();

        String getResourceKey();

        boolean isCanCreate();

        boolean isCanRead();

        boolean isCanUpdate();

        boolean isCanDelete();

        boolean isHasOverride();
    }

}
