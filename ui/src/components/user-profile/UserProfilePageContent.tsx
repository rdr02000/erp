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
  saveUserProfile,
  toUserProfileInput,
  type CompanyDetailsInput,
  type UserProfile,
  type UserProfileInput,
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

  useEffect(() => {
    let active = true;

    fetchCurrentUser()
      .then(async (user) => {
        if (!active) return;
        setCurrentUser(user);
        const loadedProfile = await fetchUserProfile(user.id);
        if (active) setProfile(loadedProfile);
      })
      .catch((cause: unknown) => {
        if (!active) return;
        if (cause instanceof AuthApiError && cause.status === 401) {
          router.replace("/signin");
          return;
        }
        setError(
          cause instanceof Error ? cause.message : t("profileLoadFailed"),
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
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
