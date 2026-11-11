package com.sass.ramel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeRoleView;
import com.sass.ramel.dto.CompanyRoleManagementModels.EmployeeCrudAccessView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeCrudAccessRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.RoleOption;
import com.sass.ramel.dto.CompanyRoleManagementModels.SubscriptionView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UpdateEmployeeRolesRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.CreateCompanyRoleRequest;
import com.sass.ramel.dto.CompanyRoleManagementModels.UserProductAccessView;
import com.sass.ramel.dto.CompanyRoleManagementModels.UserResourcePermissionView;
import com.sass.ramel.repository.CompanyRoleManagementRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyRoleManagementServiceTest {

    @Mock
    private CompanyRoleManagementRepository repository;

    @InjectMocks
    private CompanyRoleManagementService service;

    @Test
    void deniesRoleManagementWhenUserIsNotAnAdministrator() {
        when(repository.findAdministratorSubscriptions(15)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getManagedSubscriptions(15))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository).findAdministratorSubscriptions(15);
    }

    @Test
    void returnsAdministratorSubscriptionsWithTheirRolesAndEmployees() {
        RoleOption role = new RoleOption(31, "company_admin", "Company Administrator");
        EmployeeRoleView employee =
                new EmployeeRoleView(41, 15, "manager", "Morgan", "Lee", true, List.of(31L));
        SubscriptionView subscription = new SubscriptionView(
                21,
                "Northstar Robotics",
                "ERP_CORE",
                "ERP Core",
                List.of(role),
                List.of(employee));
        when(repository.findAdministratorSubscriptions(15)).thenReturn(List.of(subscription));

        List<SubscriptionView> result = service.getManagedSubscriptions(15);

        assertThat(result).containsExactly(subscription);
    }

    @Test
    void createsRoleForAnAuthorizedSubscription() {
        var projection = mock(CompanyRoleManagementRepository.RoleProjection.class);
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.insertCompanyRole(21, "payroll_manager", "Payroll Manager"))
                .thenReturn(1);
        when(repository.findRoleBySubscriptionAndKey(21, "payroll_manager"))
                .thenReturn(Optional.of(projection));
        when(projection.getRoleId()).thenReturn(33L);
        when(projection.getRoleKey()).thenReturn("payroll_manager");
        when(projection.getName()).thenReturn("Payroll Manager");

        RoleOption result = service.createCompanyRole(
                15,
                21,
                new CreateCompanyRoleRequest("payroll_manager", " Payroll Manager "));

        assertThat(result).isEqualTo(
                new RoleOption(33, "payroll_manager", "Payroll Manager"));
    }

    @Test
    void rejectsRoleCreationByANonAdministrator() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(false);

        assertThatThrownBy(() -> service.createCompanyRole(
                15, 21, new CreateCompanyRoleRequest("payroll_manager", "Payroll Manager")))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository).hasAdministratorAccess(15, 21);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejectsDuplicateRoleKeysForASubscription() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.insertCompanyRole(21, "payroll_manager", "Payroll Manager"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.createCompanyRole(
                15, 21, new CreateCompanyRoleRequest("payroll_manager", "Payroll Manager")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void returnsCurrentUserRolesAndEffectivePermissionsGroupedBySubscription() {
        var role = mock(CompanyRoleManagementRepository.UserProductRoleProjection.class);
        when(role.getSubscriptionId()).thenReturn(21L);
        when(role.getCompanyName()).thenReturn("Northstar Robotics");
        when(role.getProductCode()).thenReturn("ERP_CORE");
        when(role.getProductName()).thenReturn("ERP Core");
        when(role.getRoleName()).thenReturn("Payroll Manager");
        when(repository.findUserProductRoles(15)).thenReturn(List.of(role));

        var permission =
                mock(CompanyRoleManagementRepository.UserProductPermissionProjection.class);
        when(permission.getSubscriptionId()).thenReturn(21L);
        when(permission.getResourceKey()).thenReturn("payroll");
        when(permission.isCanCreate()).thenReturn(true);
        when(permission.isCanRead()).thenReturn(true);
        when(permission.isCanUpdate()).thenReturn(false);
        when(permission.isCanDelete()).thenReturn(false);
        when(permission.isHasOverride()).thenReturn(true);
        when(repository.findUserProductPermissions(15)).thenReturn(List.of(permission));

        List<UserProductAccessView> result = service.getUserProductAccess(15);

        assertThat(result).containsExactly(new UserProductAccessView(
                21,
                "Northstar Robotics",
                "ERP_CORE",
                "ERP Core",
                List.of("Payroll Manager"),
                List.of(new UserResourcePermissionView(
                        "payroll", true, true, false, false, true))));
    }

        @Test
        void returnsEmployeeCrudPermissionsForAnAuthorizedCompanyAdministrator() {
                var projection = mock(CompanyRoleManagementRepository.EmployeeCrudAccessProjection.class);
                when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
                when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);
                when(projection.getEmployeeId()).thenReturn(41L);
                when(projection.getCompanyId()).thenReturn(7L);
                when(projection.getSubscriptionId()).thenReturn(21L);
                when(projection.getProductId()).thenReturn(11L);
                when(projection.getProductCode()).thenReturn("ERP_CORE");
                when(projection.getProductName()).thenReturn("ERP Core");
                when(projection.getResourceKey()).thenReturn("invoices");
                when(projection.isCanCreate()).thenReturn(true);
                when(projection.isCanRead()).thenReturn(true);
                when(projection.isCanUpdate()).thenReturn(false);
                when(projection.isCanDelete()).thenReturn(false);
                when(repository.findEmployeeCrudAccess(41, 21)).thenReturn(List.of(projection));

                List<EmployeeCrudAccessView> result = service.getEmployeeCrudAccess(15, 21, 41);

                assertThat(result).containsExactly(new EmployeeCrudAccessView(
                        41, 7, 21, 11, "ERP_CORE", "ERP Core", "invoices", true, true, false, false, false));
        }

        @Test
        void deniesCrudPermissionReadsWhenUserIsNotAdministratorForSubscription() {
                when(repository.hasAdministratorAccess(15, 21)).thenReturn(false);

                assertThatThrownBy(() -> service.getEmployeeCrudAccess(15, 21, 41))
                                .isInstanceOf(AccessDeniedException.class);
                verify(repository).hasAdministratorAccess(15, 21);
                verifyNoMoreInteractions(repository);
        }

    @Test
    void deniesUpdatesWhenUserIsNotAdministratorForSubscription() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(false);

        assertThatThrownBy(() -> service.updateEmployeeRoles(
                15, 21, 41, new UpdateEmployeeRolesRequest(true, List.of(32L))))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository).hasAdministratorAccess(15, 21);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void updatesCrudPermissionsForAnAssignedEmployeeResource() {
        UpdateEmployeeCrudAccessRequest request =
                new UpdateEmployeeCrudAccessRequest(true, true, false, false);
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);
        when(repository.employeeHasResourcePermission(41, 21, "invoices")).thenReturn(true);

        service.updateEmployeeCrudAccess(15, 21, 41, "invoices", request);

        verify(repository).saveEmployeeCrudOverride(
                41, 21, "invoices", true, true, false, false);
    }

    @Test
    void rejectsCrudUpdatesForResourcesNotAssignedToTheEmployee() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);
        when(repository.employeeHasResourcePermission(41, 21, "payroll")).thenReturn(false);

        assertThatThrownBy(() -> service.updateEmployeeCrudAccess(
                15,
                21,
                41,
                "payroll",
                new UpdateEmployeeCrudAccessRequest(true, true, true, true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsCrudUpdatesWhenPermissionFlagsAreMissing() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);
        when(repository.employeeHasResourcePermission(41, 21, "invoices")).thenReturn(true);

        assertThatThrownBy(() -> service.updateEmployeeCrudAccess(
                15, 21, 41, "invoices", new UpdateEmployeeCrudAccessRequest(null, true, true, true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsEmployeesFromAnotherCompany() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(false);

        assertThatThrownBy(() -> service.updateEmployeeRoles(
                15, 21, 41, new UpdateEmployeeRolesRequest(true, List.of(32L))))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(repository).employeeBelongsToSubscriptionCompany(41, 21);
    }

    @Test
    void rejectsRolesFromAnotherSubscription() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);
        when(repository.rolesBelongToSubscription(List.of(32L), 21)).thenReturn(false);

        assertThatThrownBy(() -> service.updateEmployeeRoles(
                15, 21, 41, new UpdateEmployeeRolesRequest(true, List.of(32L))))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void replacesEmployeeRolesAfterAllScopeChecksPass() {
        List<Long> roleIds = List.of(32L, 33L);
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);
        when(repository.rolesBelongToSubscription(roleIds, 21)).thenReturn(true);

        service.updateEmployeeRoles(
                15, 21, 41, new UpdateEmployeeRolesRequest(true, roleIds));

        verify(repository).grantProductAccess(41, 21);
        verify(repository).replaceEmployeeRoles(41, 21, 15, roleIds);
    }

    @Test
    void revokesProductAccessAndItsRolesWhenAccessIsDisabled() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);

        service.updateEmployeeRoles(
                15, 21, 41, new UpdateEmployeeRolesRequest(false, List.of()));

        verify(repository).revokeProductAccess(41, 21);
    }

    @Test
    void rejectsRoleAssignmentsWhenProductAccessIsDisabled() {
        when(repository.hasAdministratorAccess(15, 21)).thenReturn(true);
        when(repository.employeeBelongsToSubscriptionCompany(41, 21)).thenReturn(true);

        assertThatThrownBy(() -> service.updateEmployeeRoles(
                15, 21, 41, new UpdateEmployeeRolesRequest(false, List.of(32L))))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

}
