"use client";

import {
  fetchEmployeeCrudAccess,
  fetchCompanyRoleManagement,
  resetEmployeeCrudAccess,
  saveEmployeeCrudAccess,
  saveEmployeeRoles,
  type EmployeeCrudAccess,
  type CompanySubscriptionRoles,
} from "@/components/company-roles/companyRoleApi";
import { AuthApiError } from "@/components/auth/authApi";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Modal } from "@/components/ui/modal";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

function employeeName(
  firstName: string | null,
  lastName: string | null,
): string {
  return [firstName, lastName].filter(Boolean).join(" ");
}

interface EmployeeRoleDraft {
  hasProductAccess: boolean;
  roleIds: number[];
}

type CrudPermission = "canCreate" | "canRead" | "canUpdate" | "canDelete";

const CRUD_PERMISSION_LABELS: Record<CrudPermission, "create" | "read" | "update" | "delete"> = {
  canCreate: "create",
  canRead: "read",
  canUpdate: "update",
  canDelete: "delete",
};

const EMPLOYEES_PER_PAGE = 10;
const EMPLOYEE_PAGE_SIZES = [10, 25, 50, 100] as const;

function initialEmployeeRoleDraft(
  employee: CompanySubscriptionRoles["employees"][number],
): EmployeeRoleDraft {
  return {
    hasProductAccess: employee.hasProductAccess === true,
    roleIds: Array.isArray(employee.roleIds) ? employee.roleIds : [],
  };
}

export default function CompanyRoleManagementPage() {
  const t = useTranslations("companyRoles");
  const [subscriptions, setSubscriptions] = useState<
    CompanySubscriptionRoles[]
  >([]);
  const [selectedSubscriptionId, setSelectedSubscriptionId] = useState<
    number | null
  >(null);
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [employeePage, setEmployeePage] = useState(1);
  const [employeePageSize, setEmployeePageSize] =
    useState<number>(EMPLOYEES_PER_PAGE);
  const [editingRolesEmployeeId, setEditingRolesEmployeeId] = useState<
    number | null
  >(null);
  const [draftRoles, setDraftRoles] = useState<
    Record<string, EmployeeRoleDraft>
  >({});
  const [savingEmployeeId, setSavingEmployeeId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [savedRoleKey, setSavedRoleKey] = useState<string | null>(null);
  const [crudAccessTarget, setCrudAccessTarget] = useState<{
    subscription: CompanySubscriptionRoles;
    employee: CompanySubscriptionRoles["employees"][number];
  } | null>(null);
  const [crudAccessRows, setCrudAccessRows] = useState<EmployeeCrudAccess[]>([]);
  const [crudAccessOriginalRows, setCrudAccessOriginalRows] = useState<
    EmployeeCrudAccess[]
  >([]);
  const [crudAccessLoading, setCrudAccessLoading] = useState(false);
  const [crudAccessError, setCrudAccessError] = useState<string | null>(null);
  const [crudAccessSaveError, setCrudAccessSaveError] = useState<string | null>(null);
  const [savingCrudResourceKey, setSavingCrudResourceKey] = useState<string | null>(null);
  const [savedCrudResourceKey, setSavedCrudResourceKey] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    fetchCompanyRoleManagement()
      .then((result) => {
        if (!active) return;
        setSubscriptions(result);
        setSelectedSubscriptionId(result[0]?.subscriptionId ?? null);
      })
      .catch((cause: unknown) => {
        if (!active) return;
        if (cause instanceof AuthApiError && cause.status === 403) {
          setForbidden(true);
        } else {
          setLoadError(
            cause instanceof Error ? cause.message : t("loadFailed"),
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [t]);

  const selectedSubscription = subscriptions.find(
    (subscription) => subscription.subscriptionId === selectedSubscriptionId,
  );
  const searchTerm = employeeSearch.trim().toLocaleLowerCase();
  const filteredEmployees =
    selectedSubscription?.employees.filter((employee) => {
      const name = employeeName(employee.firstName, employee.lastName);
      return `${name} ${employee.username}`
        .toLocaleLowerCase()
        .includes(searchTerm);
    }) ?? [];
  const editingRolesEmployee = selectedSubscription?.employees.find(
    (employee) => employee.employeeId === editingRolesEmployeeId,
  );
  const totalPages = Math.ceil(filteredEmployees.length / employeePageSize);
  const currentPage = Math.min(employeePage, Math.max(totalPages, 1));
  const pageEmployees = filteredEmployees.slice(
    (currentPage - 1) * employeePageSize,
    currentPage * employeePageSize,
  );
  const firstEmployeeIndex =
    filteredEmployees.length === 0
      ? 0
      : (currentPage - 1) * employeePageSize + 1;
  const lastEmployeeIndex = Math.min(
    currentPage * employeePageSize,
    filteredEmployees.length,
  );

  function roleDraftKey(subscriptionId: number, employeeId: number): string {
    return `${subscriptionId}:${employeeId}`;
  }

  function getDraftRoles(
    subscription: CompanySubscriptionRoles,
    employeeId: number,
    employee: CompanySubscriptionRoles["employees"][number],
  ): EmployeeRoleDraft {
    const employeeDraft =
      draftRoles[roleDraftKey(subscription.subscriptionId, employeeId)];
    const initialDraft = initialEmployeeRoleDraft(employee);
    return {
      hasProductAccess:
        typeof employeeDraft?.hasProductAccess === "boolean"
          ? employeeDraft.hasProductAccess
          : initialDraft.hasProductAccess,
      roleIds: Array.isArray(employeeDraft?.roleIds)
        ? employeeDraft.roleIds
        : initialDraft.roleIds,
    };
  }

  function updateEmployeeDraft(
    subscriptionId: number,
    employeeId: number,
    employee: CompanySubscriptionRoles["employees"][number],
    update: (draft: EmployeeRoleDraft) => EmployeeRoleDraft,
  ) {
    const key = roleDraftKey(subscriptionId, employeeId);
    const currentDraft = draftRoles[key] ?? initialEmployeeRoleDraft(employee);
    setDraftRoles((current) => ({
      ...current,
      [key]: update(currentDraft),
    }));
    setSaveError(null);
    setSavedRoleKey(null);
  }

  async function handleSave(
    subscription: CompanySubscriptionRoles,
    employee: CompanySubscriptionRoles["employees"][number],
  ) {
    const employeeId = employee.employeeId;
    const key = roleDraftKey(subscription.subscriptionId, employeeId);
    const draft = getDraftRoles(subscription, employeeId, employee);
    const selectedRoleIds = draft.hasProductAccess ? draft.roleIds : [];
    setSavingEmployeeId(employeeId);
    setSaveError(null);
    setSavedRoleKey(null);
    try {
      await saveEmployeeRoles(
        subscription.subscriptionId,
        employeeId,
        draft.hasProductAccess,
        selectedRoleIds,
      );
      setSubscriptions((current) =>
        current.map((item) =>
          item.subscriptionId !== subscription.subscriptionId
            ? item
            : {
                ...item,
                employees: item.employees.map((employee) =>
                  employee.employeeId !== employeeId
                    ? employee
                    : {
                        ...employee,
                        hasProductAccess: draft.hasProductAccess,
                        roleIds: selectedRoleIds,
                      },
                ),
              },
        ),
      );
      setDraftRoles((current) => {
        const next = { ...current };
        delete next[key];
        return next;
      });
      setSavedRoleKey(key);
    } catch (cause: unknown) {
      setSaveError(cause instanceof Error ? cause.message : t("saveFailed"));
    } finally {
      setSavingEmployeeId(null);
    }
  }

  async function handleViewCrudAccess(
    subscription: CompanySubscriptionRoles,
    employee: CompanySubscriptionRoles["employees"][number],
  ) {
    setCrudAccessTarget({ subscription, employee });
    setCrudAccessRows([]);
    setCrudAccessOriginalRows([]);
    setCrudAccessError(null);
    setCrudAccessSaveError(null);
    setSavedCrudResourceKey(null);
    setCrudAccessLoading(true);
    try {
      const rows = await fetchEmployeeCrudAccess(
        subscription.subscriptionId,
        employee.employeeId,
      );
      setCrudAccessRows(rows);
      setCrudAccessOriginalRows(rows);
    } catch (cause: unknown) {
      setCrudAccessError(
        cause instanceof Error ? cause.message : t("permissionsLoadFailed"),
      );
    } finally {
      setCrudAccessLoading(false);
    }
  }

  function closeCrudAccess() {
    setCrudAccessTarget(null);
    setCrudAccessRows([]);
    setCrudAccessOriginalRows([]);
    setCrudAccessError(null);
    setCrudAccessSaveError(null);
    setSavedCrudResourceKey(null);
  }

  function updateCrudAccessDraft(
    resourceKey: string,
    permission: CrudPermission,
    allowed: boolean,
  ) {
    setCrudAccessRows((current) =>
      current.map((row) =>
        row.resourceKey === resourceKey
          ? { ...row, [permission]: allowed }
          : row,
      ),
    );
    setCrudAccessSaveError(null);
    setSavedCrudResourceKey(null);
  }

  function isCrudAccessChanged(access: EmployeeCrudAccess): boolean {
    const original = crudAccessOriginalRows.find(
      (row) => row.resourceKey === access.resourceKey,
    );
    return (
      original === undefined ||
      access.canCreate !== original.canCreate ||
      access.canRead !== original.canRead ||
      access.canUpdate !== original.canUpdate ||
      access.canDelete !== original.canDelete
    );
  }

  async function handleSaveCrudAccess(access: EmployeeCrudAccess) {
    if (!crudAccessTarget) return;
    const { subscription, employee } = crudAccessTarget;
    setSavingCrudResourceKey(access.resourceKey);
    setCrudAccessSaveError(null);
    setSavedCrudResourceKey(null);
    try {
      await saveEmployeeCrudAccess(
        subscription.subscriptionId,
        employee.employeeId,
        access.resourceKey,
        {
          canCreate: access.canCreate,
          canRead: access.canRead,
          canUpdate: access.canUpdate,
          canDelete: access.canDelete,
        },
      );
      const refreshed = await fetchEmployeeCrudAccess(
        subscription.subscriptionId,
        employee.employeeId,
      );
      setCrudAccessRows(refreshed);
      setCrudAccessOriginalRows(refreshed);
      setSavedCrudResourceKey(access.resourceKey);
    } catch (cause: unknown) {
      setCrudAccessSaveError(
        cause instanceof Error ? cause.message : t("permissionsSaveFailed"),
      );
    } finally {
      setSavingCrudResourceKey(null);
    }
  }

  async function handleResetCrudAccess(access: EmployeeCrudAccess) {
    if (!crudAccessTarget) return;
    const { subscription, employee } = crudAccessTarget;
    setSavingCrudResourceKey(access.resourceKey);
    setCrudAccessSaveError(null);
    setSavedCrudResourceKey(null);
    try {
      await resetEmployeeCrudAccess(
        subscription.subscriptionId,
        employee.employeeId,
        access.resourceKey,
      );
      const refreshed = await fetchEmployeeCrudAccess(
        subscription.subscriptionId,
        employee.employeeId,
      );
      setCrudAccessRows(refreshed);
      setCrudAccessOriginalRows(refreshed);
    } catch (cause: unknown) {
      setCrudAccessSaveError(
        cause instanceof Error ? cause.message : t("permissionsSaveFailed"),
      );
    } finally {
      setSavingCrudResourceKey(null);
    }
  }

  if (loading) {
    return <p className="text-sm text-gray-500">{t("loading")}</p>;
  }

  if (forbidden) {
    return (
      <section className="rounded-2xl border border-error-200 bg-error-50 p-6 dark:border-error-800 dark:bg-error-900/20">
        <h1 className="text-xl font-semibold text-gray-900 dark:text-white">
          {t("title")}
        </h1>
        <p
          className="mt-2 text-sm text-error-700 dark:text-error-300"
          role="alert"
        >
          {t("accessDenied")}
        </p>
      </section>
    );
  }

  if (loadError) {
    return (
      <section className="rounded-2xl border border-error-200 bg-error-50 p-6 dark:border-error-800 dark:bg-error-900/20">
        <h1 className="text-xl font-semibold text-gray-900 dark:text-white">
          {t("title")}
        </h1>
        <p
          className="mt-2 text-sm text-error-700 dark:text-error-300"
          role="alert"
        >
          {loadError}
        </p>
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">
          {t("title")}
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          {t("description")}
        </p>
      </header>

      {subscriptions.length === 0 ? (
        <p className="rounded-xl border border-gray-200 bg-white p-5 text-sm text-gray-500 dark:border-gray-800 dark:bg-white/[0.03]">
          {t("noSubscriptions")}
        </p>
      ) : (
        <>
          <label className="block max-w-lg">
            <span className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              {t("subscription")}
            </span>
            <select
              value={selectedSubscriptionId ?? ""}
              onChange={(event) => {
                setSelectedSubscriptionId(Number(event.target.value));
                setEmployeePage(1);
                setEditingRolesEmployeeId(null);
              }}
              className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
            >
              {subscriptions.map((subscription) => (
                <option
                  key={subscription.subscriptionId}
                  value={subscription.subscriptionId}
                >
                  {subscription.companyName} - {subscription.productName}
                </option>
              ))}
            </select>
          </label>

          {selectedSubscription && (
            <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
              <div className="border-b border-gray-200 px-5 py-4 dark:border-gray-800">
                <h2 className="font-semibold text-gray-900 dark:text-white">
                  {selectedSubscription.companyName} -{" "}
                  {selectedSubscription.productName}
                </h2>
                <p className="mt-1 text-xs text-gray-500">
                  {selectedSubscription.productCode}
                </p>
              </div>

              {selectedSubscription.employees.length === 0 ? (
                <p className="p-5 text-sm text-gray-500">{t("noEmployees")}</p>
              ) : (
                <>
                  <div className="border-b border-gray-200 px-5 py-4 dark:border-gray-800">
                    <label className="block max-w-md">
                      <span className="sr-only">{t("searchEmployees")}</span>
                      <input
                        type="search"
                        value={employeeSearch}
                        onChange={(event) => {
                          setEmployeeSearch(event.target.value);
                          setEmployeePage(1);
                        }}
                        placeholder={t("searchEmployees")}
                        aria-label={t("searchEmployees")}
                        className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-gray-500"
                      />
                    </label>
                  </div>
                  <div className="max-w-full overflow-x-auto">
                    <Table className="min-w-[900px]">
                      <TableHeader className="border-b border-gray-200 dark:border-gray-800">
                        <TableRow>
                          {(
                            [
                              "employee",
                              "productAccess",
                              "assignedRoles",
                              "actions",
                            ] as const
                          ).map((column) => (
                            <TableCell
                              key={column}
                              isHeader
                              className="px-5 py-3 text-start text-sm font-medium text-gray-500 dark:text-gray-400"
                            >
                              {t(column)}
                            </TableCell>
                          ))}
                        </TableRow>
                      </TableHeader>
                      <TableBody className="divide-y divide-gray-200 dark:divide-gray-800">
                        {filteredEmployees.length === 0 ? (
                          <TableRow>
                            <td
                              colSpan={4}
                              className="px-5 py-8 text-center text-sm text-gray-500"
                            >
                              {t("noMatchingEmployees")}
                            </td>
                          </TableRow>
                        ) : (
                          pageEmployees.map((employee) => {
                            const employeeDraft = getDraftRoles(
                              selectedSubscription,
                              employee.employeeId,
                              employee,
                            );
                            const selectedRoleIds = employeeDraft.roleIds;
                            const originalRoleIds = Array.isArray(
                              employee.roleIds,
                            )
                              ? employee.roleIds
                              : [];
                            const originalHasProductAccess =
                              employee.hasProductAccess === true;
                            const changed =
                              employeeDraft.hasProductAccess !==
                                originalHasProductAccess ||
                              (employeeDraft.hasProductAccess &&
                                (selectedRoleIds.length !==
                                  originalRoleIds.length ||
                                  selectedRoleIds.some(
                                    (roleId) =>
                                      !originalRoleIds.includes(roleId),
                                  )));
                            const name = employeeName(
                              employee.firstName,
                              employee.lastName,
                            );
                            const saved =
                              savedRoleKey ===
                              roleDraftKey(
                                selectedSubscription.subscriptionId,
                                employee.employeeId,
                              );

                            return (
                              <TableRow
                                key={employee.employeeId}
                                className="odd:bg-gray-50 even:bg-white dark:odd:bg-white/[0.03] dark:even:bg-transparent"
                              >
                                <TableCell className="px-5 py-4 text-start">
                                  <p className="font-medium text-gray-900 dark:text-white">
                                    {name || employee.username}
                                  </p>
                                  <p className="text-sm text-gray-500">
                                    {employee.username}
                                  </p>
                                </TableCell>
                                <TableCell className="px-5 py-4 text-start">
                                  <label className="inline-flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                                    <input
                                      type="checkbox"
                                      checked={employeeDraft.hasProductAccess}
                                      onChange={(event) =>
                                        updateEmployeeDraft(
                                          selectedSubscription.subscriptionId,
                                          employee.employeeId,
                                          employee,
                                          (draft) => ({
                                            hasProductAccess:
                                              event.target.checked,
                                            roleIds: event.target.checked
                                              ? draft.roleIds
                                              : [],
                                          }),
                                        )
                                      }
                                      className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500"
                                    />
                                    {t("productAccess")}
                                  </label>
                                </TableCell>
                                <TableCell className="px-5 py-4 text-start">
                                  {!employeeDraft.hasProductAccess ? (
                                    <span className="text-sm text-gray-500">
                                      {t("noProductAccess")}
                                    </span>
                                  ) : selectedSubscription.roles.length ===
                                    0 ? (
                                    <p className="text-sm text-gray-500">
                                      {t("noRoles")}
                                    </p>
                                  ) : (
                                    <div className="space-y-2">
                                      <p className="text-sm text-gray-700 dark:text-gray-300">
                                        {selectedSubscription.roles
                                          .filter((role) =>
                                            selectedRoleIds.includes(
                                              role.roleId,
                                            ),
                                          )
                                          .map((role) => role.name)
                                          .join(", ") || t("noRolesAssigned")}
                                      </p>
                                      <button
                                        type="button"
                                        onClick={() =>
                                          setEditingRolesEmployeeId(
                                            employee.employeeId,
                                          )
                                        }
                                        className="text-sm font-medium text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
                                      >
                                        {t("editRoles")}
                                      </button>
                                    </div>
                                  )}
                                </TableCell>
                                <TableCell className="px-5 py-4 text-start">
                                  <div className="flex flex-wrap items-center gap-3">
                                    <button
                                      type="button"
                                      onClick={() =>
                                        void handleViewCrudAccess(
                                          selectedSubscription,
                                          employee,
                                        )
                                      }
                                      className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/[0.03]"
                                    >
                                      {t("viewCrudAccess")}
                                    </button>
                                    <button
                                      type="button"
                                      disabled={
                                        !changed || savingEmployeeId !== null
                                      }
                                      onClick={() =>
                                        void handleSave(
                                          selectedSubscription,
                                          employee,
                                        )
                                      }
                                      className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                      {savingEmployeeId === employee.employeeId
                                        ? t("saving")
                                        : t("saveRoles")}
                                    </button>
                                    {saved && (
                                      <p
                                        className="text-sm text-success-600"
                                        role="status"
                                      >
                                        {t("saveSucceeded")}
                                      </p>
                                    )}
                                  </div>
                                </TableCell>
                              </TableRow>
                            );
                          })
                        )}
                      </TableBody>
                    </Table>
                  </div>
                  {filteredEmployees.length > 0 && (
                    <nav
                      aria-label={t("employeePagination")}
                      className="flex flex-wrap items-center justify-between gap-3 border-t border-gray-200 px-5 py-4 dark:border-gray-800"
                    >
                      <p
                        className="text-sm text-gray-500 dark:text-gray-400"
                        aria-live="polite"
                      >
                        {t("paginationSummary", {
                          start: firstEmployeeIndex,
                          end: lastEmployeeIndex,
                          total: filteredEmployees.length,
                        })}
                      </p>
                      <div className="flex gap-2">
                        <label className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                          <span>{t("pageSize")}</span>
                          <select
                            value={employeePageSize}
                            onChange={(event) => {
                              setEmployeePageSize(Number(event.target.value));
                              setEmployeePage(1);
                            }}
                            aria-label={t("pageSize")}
                            className="h-10 rounded-lg border border-gray-300 bg-white px-3 text-sm text-gray-800 focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
                          >
                            {EMPLOYEE_PAGE_SIZES.map((size) => (
                              <option key={size} value={size}>
                                {size}
                              </option>
                            ))}
                          </select>
                        </label>
                        <button
                          type="button"
                          disabled={currentPage === 1}
                          onClick={() =>
                            setEmployeePage((page) => Math.max(1, page - 1))
                          }
                          className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/[0.03]"
                        >
                          {t("previousPage")}
                        </button>
                        <button
                          type="button"
                          disabled={currentPage >= totalPages}
                          onClick={() =>
                            setEmployeePage((page) =>
                              Math.min(totalPages, page + 1),
                            )
                          }
                          className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/[0.03]"
                        >
                          {t("nextPage")}
                        </button>
                      </div>
                    </nav>
                  )}
                  {editingRolesEmployee && (
                    <Modal
                      isOpen
                      onClose={() => setEditingRolesEmployeeId(null)}
                      showCloseButton={false}
                      className="mx-4 max-w-xl p-6 sm:p-8"
                    >
                      <div
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="employee-roles-dialog-title"
                      >
                        <h2
                          id="employee-roles-dialog-title"
                          className="pr-10 text-lg font-semibold text-gray-900 dark:text-white"
                        >
                          {t("editRolesFor", {
                            employee:
                              employeeName(
                                editingRolesEmployee.firstName,
                                editingRolesEmployee.lastName,
                              ) || editingRolesEmployee.username,
                          })}
                        </h2>
                        <p className="mt-1 text-sm text-gray-500">
                          {editingRolesEmployee.username}
                        </p>
                        {selectedSubscription.roles.length === 0 ? (
                          <p className="mt-6 text-sm text-gray-500">
                            {t("noRoles")}
                          </p>
                        ) : (
                          <fieldset className="mt-6 space-y-3">
                            <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">
                              {t("assignedRoles")}
                            </legend>
                            {selectedSubscription.roles.map((role) => {
                              const employeeDraft = getDraftRoles(
                                selectedSubscription,
                                editingRolesEmployee.employeeId,
                                editingRolesEmployee,
                              );
                              return (
                                <label
                                  key={role.roleId}
                                  className="flex items-center gap-3 text-sm text-gray-700 dark:text-gray-300"
                                >
                                  <input
                                    type="checkbox"
                                    checked={employeeDraft.roleIds.includes(
                                      role.roleId,
                                    )}
                                    disabled={!employeeDraft.hasProductAccess}
                                    onChange={(event) =>
                                      updateEmployeeDraft(
                                        selectedSubscription.subscriptionId,
                                        editingRolesEmployee.employeeId,
                                        editingRolesEmployee,
                                        (draft) => ({
                                          ...draft,
                                          roleIds: event.target.checked
                                            ? [...draft.roleIds, role.roleId]
                                            : draft.roleIds.filter(
                                                (roleId) =>
                                                  roleId !== role.roleId,
                                              ),
                                        }),
                                      )
                                    }
                                    className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500"
                                  />
                                  {role.name}
                                </label>
                              );
                            })}
                          </fieldset>
                        )}
                        {!getDraftRoles(
                          selectedSubscription,
                          editingRolesEmployee.employeeId,
                          editingRolesEmployee,
                        ).hasProductAccess && (
                          <p className="mt-3 text-sm text-gray-500">
                            {t("enableProductAccessToAssignRoles")}
                          </p>
                        )}
                        <div className="mt-8 flex justify-end">
                          <button
                            type="button"
                            onClick={() => setEditingRolesEmployeeId(null)}
                            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/[0.03]"
                          >
                            {t("closeDialog")}
                          </button>
                        </div>
                      </div>
                    </Modal>
                  )}
                  {crudAccessTarget && (
                    <Modal
                      isOpen
                      onClose={closeCrudAccess}
                      className="mx-4 max-w-4xl p-6 sm:p-8"
                    >
                      <div
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="employee-crud-access-title"
                      >
                        <h2
                          id="employee-crud-access-title"
                          className="pe-10 text-lg font-semibold text-gray-900 dark:text-white"
                        >
                          {t("permissionsFor", {
                            employee:
                              employeeName(
                                crudAccessTarget.employee.firstName,
                                crudAccessTarget.employee.lastName,
                              ) || crudAccessTarget.employee.username,
                          })}
                        </h2>
                        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                          {crudAccessTarget.subscription.companyName} - {crudAccessTarget.subscription.productName}
                        </p>
                        {crudAccessLoading ? (
                          <p className="mt-6 text-sm text-gray-500" role="status">
                            {t("permissionsLoading")}
                          </p>
                        ) : crudAccessError ? (
                          <p className="mt-6 text-sm text-error-500" role="alert">
                            {crudAccessError}
                          </p>
                        ) : crudAccessRows.length === 0 ? (
                          <p className="mt-6 text-sm text-gray-500 dark:text-gray-400">
                            {t("noCrudPermissions")}
                          </p>
                        ) : (
                          <div className="mt-5 max-w-full overflow-x-auto">
                            <table className="w-full min-w-[560px] text-sm">
                              <thead>
                                <tr className="border-b border-gray-200 text-start dark:border-gray-800">
                                  {(["resource", "create", "read", "update", "delete", "actions"] as const).map((column) => (
                                    <th
                                      key={column}
                                      scope="col"
                                      className="px-3 py-3 text-start font-medium text-gray-500 dark:text-gray-400"
                                    >
                                      {t(column)}
                                    </th>
                                  ))}
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
                                {crudAccessRows.map((access) => (
                                  <tr key={`${access.productId}:${access.resourceKey}`}>
                                    <th scope="row" className="px-3 py-3 text-start font-medium text-gray-900 dark:text-white">
                                      {access.resourceKey}
                                    </th>
                                    {([
                                      ["canCreate", access.canCreate],
                                      ["canRead", access.canRead],
                                      ["canUpdate", access.canUpdate],
                                      ["canDelete", access.canDelete],
                                    ] as const).map(([permission, allowed]) => (
                                      <td key={permission} className="px-3 py-3">
                                        <label className="inline-flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                                          <input
                                            type="checkbox"
                                            checked={allowed}
                                            disabled={savingCrudResourceKey !== null}
                                            aria-label={`${access.resourceKey}: ${t(CRUD_PERMISSION_LABELS[permission])}`}
                                            onChange={(event) =>
                                              updateCrudAccessDraft(
                                                access.resourceKey,
                                                permission,
                                                event.target.checked,
                                              )
                                            }
                                            className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500"
                                          />
                                          <span className="sr-only">
                                            {allowed ? t("allowed") : t("denied")}
                                          </span>
                                        </label>
                                      </td>
                                    ))}
                                    <td className="px-3 py-3">
                                      <div className="flex items-center gap-3">
                                        <button
                                          type="button"
                                          disabled={
                                            savingCrudResourceKey !== null ||
                                            !isCrudAccessChanged(access)
                                          }
                                          onClick={() => void handleSaveCrudAccess(access)}
                                          className="rounded-lg bg-brand-500 px-3 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                          {savingCrudResourceKey === access.resourceKey
                                            ? t("saving")
                                            : t("savePermissions")}
                                        </button>
                                        {access.hasOverride && (
                                          <button
                                            type="button"
                                            disabled={savingCrudResourceKey !== null}
                                            onClick={() => void handleResetCrudAccess(access)}
                                            className="text-sm font-medium text-brand-600 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-50 dark:text-brand-400"
                                          >
                                            {t("resetPermissions")}
                                          </button>
                                        )}
                                      </div>
                                      {savedCrudResourceKey === access.resourceKey && (
                                        <p className="mt-1 text-xs text-success-600" role="status">
                                          {t("permissionsSaved")}
                                        </p>
                                      )}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        )}
                        {crudAccessSaveError && (
                          <p className="mt-4 text-sm text-error-500" role="alert">
                            {crudAccessSaveError}
                          </p>
                        )}
                        <div className="mt-8 flex justify-end">
                          <button
                            type="button"
                            onClick={closeCrudAccess}
                            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/[0.03]"
                          >
                            {t("closeDialog")}
                          </button>
                        </div>
                      </div>
                    </Modal>
                  )}
                </>
              )}
            </div>
          )}

          {saveError && (
            <p className="text-sm text-error-500" role="alert">
              {saveError}
            </p>
          )}
        </>
      )}
    </section>
  );
}
