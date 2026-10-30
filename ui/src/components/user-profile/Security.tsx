"use client";

import { changePassword } from "@/components/auth/authApi";
import { useModal } from "@/hooks/useModal";
import { PencilIcon } from "@/icons";
import { useTranslations } from "next-intl";
import { useState } from "react";
import type { FormEvent } from "react";
import Input from "../form/input/InputField";
import Label from "../form/Label";
import Button from "../ui/button/Button";
import { Modal } from "../ui/modal";

export default function Security() {
  const [switcherToggle, setSwitcherToggle] = useState(false);
  const t = useTranslations("userProfile");
  const { isOpen, openModal, closeModal } = useModal();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSaved(false);

    if (newPassword !== confirmPassword) {
      setError(t("passwordMismatch"));
      return;
    }

    setIsSaving(true);
    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSaved(true);
      closeModal();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : t("passwordChangeFailed"));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <>
    <section className="rounded-lg border border-gray-200 bg-white p-5 lg:p-6 dark:border-gray-800 dark:bg-white/3">
      <h4 className="mb-4 text-lg font-semibold text-gray-800 lg:mb-6 dark:text-white/90">
        {t("security")}
      </h4>
      {error && <p role="alert" className="mb-4 text-sm text-error-500">{error}</p>}
      {saved && <p role="status" className="mb-4 text-sm text-success-600">{t("passwordChanged")}</p>}
      <div>
        <div className="flex flex-col justify-between gap-4 border-b border-gray-200 py-4 first:pt-0 last:border-b-0 last:pb-0 sm:flex-row sm:items-end dark:border-gray-800">
          <div>
            <span className="mb-1 block text-base font-medium text-gray-800 dark:text-white/90">
              {t("changePassword")}
            </span>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {t("changePasswordDescription")}
            </p>
          </div>
          <div>
            <button
              type="button"
              onClick={() => {
                setError(null);
                setCurrentPassword("");
                setNewPassword("");
                setConfirmPassword("");
                openModal();
              }}
              className="flex h-10 items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-medium text-gray-700 shadow-theme-xs hover:bg-gray-50 hover:text-gray-800 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-white/3 dark:hover:text-gray-200"
            >
              <PencilIcon className="size-5" />
              {t("changePassword")}
            </button>
          </div>
        </div>
        <div className="flex flex-col justify-between gap-4 border-b border-gray-200 py-4 first:pt-0 last:border-b-0 last:pb-0 sm:flex-row sm:items-end dark:border-gray-800">
          <div>
            <span className="block text-base font-medium text-gray-800 dark:text-white/90">
              Two-factor authentication (2FA)
            </span>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Keep your account secure by enabling 2FA
            </p>
          </div>
          <div>
            <label
              htmlFor="toggle1"
              className="flex cursor-pointer items-center gap-3 text-sm font-medium text-gray-700 select-none dark:text-gray-400"
            >
              <div className="relative">
                <input
                  type="checkbox"
                  id="toggle1"
                  className="sr-only"
                  checked={switcherToggle}
                  onChange={(e) => setSwitcherToggle(e.target.checked)}
                />
                <div
                  className={`block h-5 w-9 rounded-full duration-200 ${
                    switcherToggle
                      ? "bg-brand-500 dark:bg-brand-500"
                      : "bg-gray-200 dark:bg-white/10"
                  }`}
                ></div>
                <div
                  className={`absolute top-0.5 start-0.5 h-4 w-4 rounded-full bg-white shadow-theme-sm duration-200 ease-linear ${
                    switcherToggle
                      ? "translate-x-full rtl:-translate-x-full"
                      : "translate-x-0"
                  }`}
                ></div>
              </div>
            </label>
          </div>
        </div>
      </div>
    </section>
    <Modal isOpen={isOpen} onClose={closeModal} className="m-4 max-w-[500px]">
      <div className="relative w-full rounded-3xl bg-white p-4 lg:p-8 dark:bg-gray-900">
        <h4 className="mb-2 text-xl font-semibold text-gray-800 dark:text-white/90">
          {t("changePassword")}
        </h4>
        <p className="mb-6 text-sm text-gray-500 dark:text-gray-400">
          {t("changePasswordDescription")}
        </p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="current-password">{t("currentPassword")}</Label>
            <Input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required />
          </div>
          <div>
            <Label htmlFor="new-password">{t("newPassword")}</Label>
            <Input id="new-password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} required />
          </div>
          <div>
            <Label htmlFor="confirm-password">{t("confirmPassword")}</Label>
            <Input id="confirm-password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} required />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" size="sm" variant="outline" onClick={closeModal} disabled={isSaving}>
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
