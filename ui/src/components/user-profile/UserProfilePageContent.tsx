"use client";

import {
  AuthApiError,
  fetchCurrentUser,
  type AuthenticatedUser,
} from "@/components/auth/authApi";
import { useRouter } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import UserAddressCard from "./UserAddressCard";
import CompanyDetailsCard from "./CompanyDetailsCard";
import Security from "./Security";
import UserMetaCard from "./UserMetaCard";
import {
  fetchUserProfile,
  fetchUserProductAccess,
  saveUserProfile,
  toUserProfileInput,
  type CompanyDetailsInput,
  type UserProfile,
  type UserProfileInput,
  type UserProductAccess,
} from "./userProfileApi";

export default function UserProfilePageContent() {
  const t = useTranslations("userProfile");
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState<AuthenticatedUser | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [productAccess, setProductAccess] = useState<UserProductAccess[]>([]);
  const [productAccessLoading, setProductAccessLoading] = useState(true);
  const [productAccessError, setProductAccessError] = useState<string | null>(
    null,
  );

  useEffect(() => {
    let active = true;

    fetchCurrentUser()
      .then(async (user) => {
        if (!active) return;
        setCurrentUser(user);
        const [profileResult, accessResult] = await Promise.allSettled([
          fetchUserProfile(user.id),
          fetchUserProductAccess(),
        ]);
        if (!active) return;
        if (profileResult.status === "fulfilled") {
          setProfile(profileResult.value);
        } else {
          if (
            profileResult.reason instanceof AuthApiError &&
            profileResult.reason.status === 401
          ) {
            router.replace("/signin");
            return;
          }
          setError(
            profileResult.reason instanceof Error
              ? profileResult.reason.message
              : t("profileLoadFailed"),
          );
        }
        if (accessResult.status === "fulfilled") {
          setProductAccess(accessResult.value);
        } else {
          if (
            accessResult.reason instanceof AuthApiError &&
            accessResult.reason.status === 401
          ) {
            router.replace("/signin");
            return;
          }
          setProductAccessError(
            accessResult.reason instanceof Error
              ? accessResult.reason.message
              : t("accessLoadFailed"),
          );
        }
      })
      .catch((cause: unknown) => {
        if (!active) return;
        if (cause instanceof AuthApiError && cause.status === 401) {
          router.replace("/signin");
          return;
        }
        setError(cause instanceof Error ? cause.message : t("profileLoadFailed"));
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
          setProductAccessLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [router, t]);

  const saveProfile = async (input: UserProfileInput) => {
    if (currentUser === null) return false;
    setIsSaving(true);
    setError(null);
    setSaved(false);
    try {
      const savedProfile = await saveUserProfile(
        currentUser.id,
        input,
        profile !== null,
      );
      setProfile(savedProfile);
      setSaved(true);
      return true;
    } catch (cause) {
      if (cause instanceof AuthApiError && cause.status === 401) {
        router.replace("/signin");
      }
      setError(cause instanceof Error ? cause.message : t("saveFailed"));
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  const saveCompany = (company: CompanyDetailsInput) =>
    saveProfile({ ...toUserProfileInput(profile), company });

  return (
    <div className="space-y-6">
      {error && (
        <p
          role="alert"
          className="rounded-lg border border-error-500/30 bg-error-50 px-4 py-3 text-sm text-error-600 dark:bg-error-500/10 dark:text-error-400"
        >
          {error}
        </p>
      )}
      {saved && (
        <p
          role="status"
          className="rounded-lg border border-success-500/30 bg-success-50 px-4 py-3 text-sm text-success-600 dark:bg-success-500/10 dark:text-success-400"
        >
          {t("saveSucceeded")}
        </p>
      )}

      {isLoading ? (
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {t("loadingProfile")}
        </p>
      ) : currentUser !== null ? (
        <>
          <UserMetaCard
            username={currentUser.username}
            profile={profile}
            onSave={saveProfile}
            isSaving={isSaving}
          />
          <UserAddressCard
            address={profile?.address ?? ""}
            onSave={(address) =>
              saveProfile({ ...toUserProfileInput(profile), address })
            }
            isSaving={isSaving}
          />
          <section className="rounded-2xl border border-gray-200 p-5 lg:p-6 dark:border-gray-800">
            <h2 className="text-lg font-semibold text-gray-800 dark:text-white/90">
              {t("rolesAndPermissions")}
            </h2>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {t("rolesAndPermissionsDescription")}
            </p>
            {productAccessLoading ? (
              <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">
                {t("loadingRolesAndPermissions")}
              </p>
            ) : productAccessError ? (
              <p className="mt-4 text-sm text-error-500" role="alert">
                {productAccessError}
              </p>
            ) : productAccess.length === 0 ? (
              <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">
                {t("noProductAccess")}
              </p>
            ) : (
              <div className="mt-5 space-y-5">
                {productAccess.map((access) => (
                  <article
                    key={access.subscriptionId}
                    className="rounded-xl border border-gray-200 p-4 dark:border-gray-800"
                  >
                    <h3 className="font-medium text-gray-800 dark:text-white/90">
                      {access.companyName} - {access.productName}
                    </h3>
                    <p className="mt-1 text-xs text-gray-500">
                      {access.productCode}
                    </p>
                    <div className="mt-4">
                      <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {t("assignedRoles")}
                      </h4>
                      <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                        {access.roles.length > 0
                          ? access.roles.join(", ")
                          : t("noAssignedRoles")}
                      </p>
                    </div>
                    <div className="mt-4">
                      <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {t("permissions")}
                      </h4>
                      {access.permissions.length === 0 ? (
                        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                          {t("noPermissions")}
                        </p>
                      ) : (
                        <div className="mt-2 max-w-full overflow-x-auto">
                          <table className="w-full min-w-[560px] text-sm">
                            <thead>
                              <tr className="border-b border-gray-200 text-start dark:border-gray-800">
                                {(
                                  [
                                    "resource",
                                    "create",
                                    "read",
                                    "update",
                                    "delete",
                                  ] as const
                                ).map((column) => (
                                  <th
                                    key={column}
                                    scope="col"
                                    className="px-3 py-2 text-start font-medium text-gray-500 dark:text-gray-400"
                                  >
                                    {t(column)}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
                              {access.permissions.map((permission) => (
                                <tr key={permission.resourceKey}>
                                  <th
                                    scope="row"
                                    className="px-3 py-2 text-start font-medium text-gray-800 dark:text-white/90"
                                  >
                                    {permission.resourceKey}
                                  </th>
                                  {(
                                    [
                                      permission.canCreate,
                                      permission.canRead,
                                      permission.canUpdate,
                                      permission.canDelete,
                                    ] as const
                                  ).map((allowed, index) => (
                                    <td
                                      key={`${permission.resourceKey}:${index}`}
                                      className="px-3 py-2 text-gray-600 dark:text-gray-400"
                                    >
                                      {allowed ? t("allowed") : t("denied")}
                                    </td>
                                  ))}
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
          <CompanyDetailsCard
            company={profile?.company ?? null}
            onSave={saveCompany}
            isSaving={isSaving}
          />
          <Security />
        </>
      ) : null}
    </div>
  );
}
