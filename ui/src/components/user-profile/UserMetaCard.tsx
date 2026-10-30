"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { useModal } from "../../hooks/useModal";
import {
  FacebookIcon,
  InstagramIcon,
  LinkedInIcon,
  PencilIcon,
  XSocialIcon,
} from "../../icons";
import Input from "../form/input/InputField";
import Label from "../form/Label";
import Button from "../ui/button/Button";
import { Modal } from "../ui/modal";
import {
  toUserProfileInput,
  type UserProfile,
  type UserProfileInput,
} from "./userProfileApi";

interface UserMetaCardProps {
  username: string;
  profile: UserProfile | null;
  onSave: (profile: UserProfileInput) => Promise<boolean>;
  isSaving: boolean;
}

const fallbackProfilePicture = "/images/user/user-01.jpg";

export default function UserMetaCard({
  username,
  profile,
  onSave,
  isSaving,
}: UserMetaCardProps) {
  const t = useTranslations("userProfile");
  const { isOpen, openModal, closeModal } = useModal();
  const [form, setForm] = useState<UserProfileInput>(() =>
    toUserProfileInput(profile),
  );
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null);

  const displayName =
    [profile?.firstName, profile?.lastName].filter(Boolean).join(" ") ||
    username;
  const requestedImageUrl = profile?.profilePictureUrl || fallbackProfilePicture;
  const imageUrl =
    requestedImageUrl === failedImageUrl
      ? fallbackProfilePicture
      : requestedImageUrl;
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (await onSave(form)) closeModal();
  };

  const updateField = (field: keyof UserProfileInput, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  return (
    <>
      <div className="mb-6 rounded-2xl border border-gray-200 p-5 lg:p-6 dark:border-gray-800">
        <div className="flex flex-col gap-5 sm:flex-row xl:gap-10">
          <div className="flex-1">
            <div className="mb-6 flex items-center gap-5">
              <div className="overflow-hidden rounded-full border border-gray-200 dark:border-gray-800">
                <Image
                  src={imageUrl}
                  width={80}
                  height={80}
                  unoptimized
                  className="size-20 object-cover"
                  alt={t("profilePicture")}
                  onError={() => setFailedImageUrl(requestedImageUrl)}
                />
              </div>
              <div className="text-start">
                <h4 className="mb-2 text-lg font-semibold text-gray-800 dark:text-white/90">
                  {displayName}
                </h4>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {username}
                </p>
              </div>
            </div>

            <div className="grid max-w-4xl grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
              <ProfileValue label={t("firstName")} value={profile?.firstName} />
              <ProfileValue label={t("lastName")} value={profile?.lastName} />
              <ProfileValue label={t("email")} value={profile?.email} />
              <ProfileValue label={t("phone")} value={profile?.phone} />
            </div>
            <div className="mt-6">
              <h5 className="mb-3 text-sm font-medium text-gray-700 dark:text-gray-300">
                {t("socialAccounts")}
              </h5>
              <div className="flex flex-wrap items-center gap-x-6 gap-y-4">
                <SocialAccount
                  iconClassName="text-brand-600 dark:text-brand-400"
                  icon={<FacebookIcon />}
                  label={t("facebook")}
                  value={profile?.facebookUrl}
                />
                <SocialAccount
                  iconClassName="text-gray-900 dark:text-gray-100"
                  icon={<XSocialIcon />}
                  label={t("x")}
                  value={profile?.xUrl}
                />
                <SocialAccount
                  iconClassName="text-blue-light-600 dark:text-blue-light-400"
                  icon={<LinkedInIcon />}
                  label={t("linkedin")}
                  value={profile?.linkedinUrl}
                />
                <SocialAccount
                  iconClassName="text-theme-pink-500"
                  icon={<InstagramIcon />}
                  label={t("instagram")}
                  value={profile?.instagramUrl}
                />
              </div>
            </div>
          </div>
          <div>
            <button
              type="button"
              onClick={() => {
                setForm(toUserProfileInput(profile));
                openModal();
              }}
              className="flex h-10 w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-medium text-gray-700 shadow-theme-xs hover:bg-gray-50 hover:text-gray-800 disabled:cursor-not-allowed disabled:opacity-50 lg:inline-flex lg:w-auto dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-white/3 dark:hover:text-gray-200"
            >
              <PencilIcon className="size-5" />
              {t("edit")}
            </button>
          </div>
        </div>
      </div>

      <Modal
        isOpen={isOpen}
        onClose={closeModal}
        className="m-4 max-w-[700px]"
      >
        <div className="relative max-h-[90vh] w-full overflow-y-auto rounded-3xl bg-white p-4 lg:p-11 dark:bg-gray-900">
          <div className="px-2 pe-14">
            <h4 className="mb-2 text-2xl font-semibold text-gray-800 dark:text-white/90">
              {t("editProfile")}
            </h4>
            <p className="mb-6 text-sm text-gray-500 dark:text-gray-400">
              {t("profileDescription")}
            </p>
          </div>
          <form onSubmit={handleSubmit} className="flex flex-col">
            <div className="grid grid-cols-1 gap-x-6 gap-y-5 px-2 sm:grid-cols-2">
              <div>
                <Label>{t("firstName")}</Label>
                <Input
                  value={form.firstName ?? ""}
                  onChange={(event) =>
                    updateField("firstName", event.target.value)
                  }
                  maxLength={100}
                />
              </div>
              <div>
                <Label>{t("lastName")}</Label>
                <Input
                  value={form.lastName ?? ""}
                  onChange={(event) =>
                    updateField("lastName", event.target.value)
                  }
                  maxLength={100}
                />
              </div>
              <div>
                <Label>{t("email")}</Label>
                <Input
                  type="email"
                  value={form.email ?? ""}
                  onChange={(event) => updateField("email", event.target.value)}
                  maxLength={320}
                />
              </div>
              <div>
                <Label>{t("phone")}</Label>
                <Input
                  type="tel"
                  value={form.phone ?? ""}
                  onChange={(event) => updateField("phone", event.target.value)}
                  maxLength={32}
                />
              </div>
              <div className="sm:col-span-2">
                <Label>{t("profilePictureUrl")}</Label>
                <Input
                  type="url"
                  value={form.profilePictureUrl ?? ""}
                  onChange={(event) =>
                    updateField("profilePictureUrl", event.target.value)
                  }
                  maxLength={2048}
                />
              </div>
              <div>
                <Label>{t("facebook")}</Label>
                <Input
                  type="url"
                  value={form.facebookUrl ?? ""}
                  onChange={(event) =>
                    updateField("facebookUrl", event.target.value)
                  }
                  maxLength={2048}
                />
              </div>
              <div>
                <Label>{t("x")}</Label>
                <Input
                  type="url"
                  value={form.xUrl ?? ""}
                  onChange={(event) => updateField("xUrl", event.target.value)}
                  maxLength={2048}
                />
              </div>
              <div>
                <Label>{t("linkedin")}</Label>
                <Input
                  type="url"
                  value={form.linkedinUrl ?? ""}
                  onChange={(event) =>
                    updateField("linkedinUrl", event.target.value)
                  }
                  maxLength={2048}
                />
              </div>
              <div>
                <Label>{t("instagram")}</Label>
                <Input
                  type="url"
                  value={form.instagramUrl ?? ""}
                  onChange={(event) =>
                    updateField("instagramUrl", event.target.value)
                  }
                  maxLength={2048}
                />
              </div>
            </div>
            <div className="mt-6 flex items-center gap-3 px-2 sm:justify-end">
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={closeModal}
                disabled={isSaving}
              >
                {t("close")}
              </Button>
              <Button type="submit" size="sm" disabled={isSaving}>
                {isSaving ? t("saving") : t("saveChanges")}
              </Button>
            </div>
          </form>
        </div>
      </Modal>
    </>
  );
}

function ProfileValue({ label, value }: { label: string; value?: string | null }) {
  const t = useTranslations("userProfile");
  return (
    <div>
      <p className="mb-2 text-xs leading-normal text-gray-500 dark:text-gray-400">
        {label}
      </p>
      <p className="break-words text-sm font-medium text-gray-800 dark:text-white/90">
        {value || t("notProvided")}
      </p>
    </div>
  );
}

function SocialAccount({
  icon,
  iconClassName,
  label,
  value,
}: {
  icon: ReactNode;
  iconClassName: string;
  label: string;
  value?: string | null;
}) {
  const safeUrl = toHttpUrl(value);

  const iconClass = `inline-flex shrink-0 items-center justify-center transition-transform hover:scale-110 ${iconClassName}`;

  return safeUrl ? (
    <a
      href={safeUrl}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`${label} profile`}
      title={safeUrl}
      className={`${iconClass} focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500`}
    >
      {icon}
    </a>
  ) : (
    <span aria-label={label} title={label} className={iconClass}>
      {icon}
    </span>
  );
}

function toHttpUrl(value?: string | null): string | null {
  if (!value) return null;

  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}
