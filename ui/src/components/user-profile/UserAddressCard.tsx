"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import type { FormEvent } from "react";
import { useModal } from "@/hooks/useModal";
import { PencilIcon } from "@/icons";
import Label from "../form/Label";
import Button from "../ui/button/Button";
import { Modal } from "../ui/modal";

interface UserAddressCardProps {
  address: string;
  onSave: (address: string) => Promise<boolean>;
  isSaving: boolean;
}

export default function UserAddressCard({
  address,
  onSave,
  isSaving,
}: UserAddressCardProps) {
  const t = useTranslations("userProfile");
  const { isOpen, openModal, closeModal } = useModal();
  const [value, setValue] = useState(address);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (await onSave(value)) closeModal();
  };

  return (
    <>
      <div className="rounded-2xl border border-gray-200 p-5 lg:p-6 dark:border-gray-800">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex-1">
            <h4 className="mb-4 text-lg font-semibold text-gray-800 lg:mb-6 dark:text-white/90">
              {t("address")}
            </h4>
            <p className="whitespace-pre-wrap break-words text-sm font-medium text-gray-800 dark:text-white/90">
              {address || t("notProvided")}
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              setValue(address);
              openModal();
            }}
            className="flex h-10 w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-3 text-sm font-medium text-gray-700 shadow-theme-xs hover:bg-gray-50 hover:text-gray-800 sm:w-auto dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-white/3 dark:hover:text-gray-200"
          >
            <PencilIcon className="size-5" />
            {t("edit")}
          </button>
        </div>
      </div>

      <Modal isOpen={isOpen} onClose={closeModal} className="m-4 max-w-[700px]">
        <div className="relative w-full rounded-3xl bg-white p-4 lg:p-11 dark:bg-gray-900">
          <div className="px-2 pe-14">
            <h4 className="mb-2 text-2xl font-semibold text-gray-800 dark:text-white/90">
              {t("editAddress")}
            </h4>
            <p className="mb-6 text-sm text-gray-500 dark:text-gray-400">
              {t("addressDescription")}
            </p>
          </div>
          <form onSubmit={handleSubmit}>
            <div className="px-2">
              <Label htmlFor="user-address">{t("fullAddress")}</Label>
              <textarea
                id="user-address"
                value={value}
                onChange={(event) => setValue(event.target.value)}
                rows={4}
                className="w-full rounded-lg border border-gray-300 bg-transparent px-4 py-3 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
              />
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
