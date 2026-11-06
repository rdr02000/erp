import { getResponseError, sessionFetch } from "@/components/auth/authApi";

export interface CompanyRole {
  roleId: number;
  roleKey: string;
  name: string;
}

export interface EmployeeRoleAssignment {
  employeeId: number;
  userId: number;
  username: string;
  firstName: string | null;
  lastName: string | null;
  hasProductAccess: boolean;
  roleIds: number[];
}

export interface EmployeeCrudAccess {
  employeeId: number;
  companyId: number;
  subscriptionId: number;
  productId: number;
  productCode: string;
  productName: string;
  resourceKey: string;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  hasOverride: boolean;
}

export interface EmployeeCrudAccessUpdate {
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}

export interface CompanySubscriptionRoles {
  subscriptionId: number;
  companyName: string;
  productCode: string;
  productName: string;
  roles: CompanyRole[];
  employees: EmployeeRoleAssignment[];
}

export async function fetchCompanyRoleAccess(): Promise<boolean> {
  const response = await sessionFetch("/api/company/role-management/access");
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<boolean>;
}

export async function fetchCompanyRoleManagement(): Promise<
  CompanySubscriptionRoles[]
> {
  const response = await sessionFetch("/api/company/role-management");
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<CompanySubscriptionRoles[]>;
}

export async function fetchEmployeeCrudAccess(
  subscriptionId: number,
  employeeId: number,
): Promise<EmployeeCrudAccess[]> {
  const response = await sessionFetch(
    `/api/company/role-management/subscriptions/${subscriptionId}/employees/${employeeId}/permissions`,
  );
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<EmployeeCrudAccess[]>;
}

export async function saveEmployeeCrudAccess(
  subscriptionId: number,
  employeeId: number,
  resourceKey: string,
  permissions: EmployeeCrudAccessUpdate,
): Promise<void> {
  const response = await sessionFetch(
    `/api/company/role-management/subscriptions/${subscriptionId}/employees/${employeeId}/permissions/${encodeURIComponent(resourceKey)}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(permissions),
    },
  );
  if (!response.ok) {
    throw await getResponseError(response);
  }
}

export async function resetEmployeeCrudAccess(
  subscriptionId: number,
  employeeId: number,
  resourceKey: string,
): Promise<void> {
  const response = await sessionFetch(
    `/api/company/role-management/subscriptions/${subscriptionId}/employees/${employeeId}/permissions/${encodeURIComponent(resourceKey)}`,
    { method: "DELETE" },
  );
  if (!response.ok) {
    throw await getResponseError(response);
  }
}

export async function saveEmployeeRoles(
  subscriptionId: number,
  employeeId: number,
  hasProductAccess: boolean,
  roleIds: number[],
): Promise<void> {
  const response = await sessionFetch(
    `/api/company/role-management/subscriptions/${subscriptionId}/employees/${employeeId}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ hasProductAccess, roleIds }),
    },
  );
  if (!response.ok) {
    throw await getResponseError(response);
  }
}
