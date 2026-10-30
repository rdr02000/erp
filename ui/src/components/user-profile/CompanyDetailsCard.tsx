"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { useState } from "react";
import type { FormEvent } from "react";
import { useModal } from "@/hooks/useModal";
import { PencilIcon } from "@/icons";
import Input from "../form/input/InputField";
import Label from "../form/Label";
import Button from "../ui/button/Button";
import { Modal } from "../ui/modal";
import {
  toCompanyDetailsInput,
  type CompanyDetails,
  type CompanyDetailsInput,
} from "./userProfileApi";

interface CompanyDetailsCardProps {
  company: CompanyDetails | null;
  onSave: (company: CompanyDetailsInput) => Promise<boolean>;
  isSaving: boolean;
}

export default function CompanyDetailsCard({
  company,
  onSave,
  isSaving,
}: CompanyDetailsCardProps) {
  const t = useTranslations("userProfile");
  const { isOpen, openModal, closeModal } = useModal();
  const [failedLogoUrl, setFailedLogoUrl] = useState<string | null>(null);
  const [form, setForm] = useState<CompanyDetailsInput>(() =>
    toCompanyDetailsInput(company),
  );

  const updateField = (field: keyof CompanyDetailsInput, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (await onSave(form)) closeModal();
  };

  return (
    <>
      <section className="rounded-lg border border-gray-200 p-5 lg:p-6 dark:border-gray-800">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex-1">
            <h4 className="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
              {t("companyDetails")}
            </h4>
            <div className="mb-5 flex items-center gap-4">
              {company?.logoUrl && company.logoUrl !== failedLogoUrl ? (
                <Image
                  src={company.logoUrl}
                  alt={t("companyLogoAlt", { name: company.name })}
                  width={64}
                  height={64}
                  unoptimized
                  onError={() => setFailedLogoUrl(company.logoUrl)}
                  className="size-16 rounded-lg border border-gray-200 bg-white object-contain p-1 dark:border-gray-700 dark:bg-gray-900"
                />
              ) : (
                <div
                  aria-label={t("companyLogoAlt", {
                    name: company?.name ?? t("companyDetails"),
                  })}
                  className="flex size-16 shrink-0 items-center justify-center rounded-lg border border-gray-200 bg-gray-50 text-lg font-semibold text-gray-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300"
                >
                  {company?.name?.slice(0, 2).toUpperCase() ?? "--"}
                </div>
              )}
              <p className="text-sm font-medium text-gray-800 dark:text-white/90">
                {company?.name || t("notProvided")}
              </p>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
              <CompanyValue label={t("companyName")} value={company?.name} />
              <CompanyValue label={t("companyIndustry")} value={company?.industry} />
              <CompanyValue label={t("companyWebsite")} value={company?.website} />
              <CompanyValue label={t("companyEmail")} value={company?.email} />
              <CompanyValue label={t("companyPhone")} value={company?.phone} />
              <CompanyValue label={t("companyAddress")} value={company?.address} />
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              setForm(toCompanyDetailsInput(company));
              openModal();
            }}
            className="flex h-10 w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-medium text-gray-700 shadow-theme-xs hover:bg-gray-50 hover:text-gray-800 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-white/3 dark:hover:text-gray-200"
          >
            <PencilIcon className="size-5" />
            {t("edit")}
          </button>
        </div>
      </section>

      <Modal isOpen={isOpen} onClose={closeModal} className="m-4 max-w-[700px]">
        <div className="relative max-h-[90vh] w-full overflow-y-auto rounded-3xl bg-white p-4 lg:p-11 dark:bg-gray-900">
          <div className="px-2 pe-14">
            <h4 className="mb-2 text-2xl font-semibold text-gray-800 dark:text-white/90">
              {t("editCompanyDetails")}
            </h4>
            <p className="mb-6 text-sm text-gray-500 dark:text-gray-400">
              {t("companyDescription")}
            </p>
          </div>
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-x-6 gap-y-5 px-2 sm:grid-cols-2">
              <CompanyInput
                label={t("companyName")}
                value={form.name}
                onChange={(value) => updateField("name", value)}
                maxLength={255}
              />
              <CompanyInput
                label={t("companyIndustry")}
                value={form.industry}
                onChange={(value) => updateField("industry", value)}
                maxLength={100}
              />
              <CompanyInput
                label={t("companyWebsite")}
                type="url"
                value={form.website}
                onChange={(value) => updateField("website", value)}
                maxLength={2048}
              />
              <CompanyInput
                label={t("companyEmail")}
                type="email"
                value={form.email}
                onChange={(value) => updateField("email", value)}
                maxLength={320}
              />
              <CompanyInput
                label={t("companyPhone")}
                type="tel"
                value={form.phone}
                onChange={(value) => updateField("phone", value)}
                maxLength={32}
              />
              <CompanyInput
                label={t("companyLogoUrl")}
                type="url"
                value={form.logoUrl}
                onChange={(value) => updateField("logoUrl", value)}
                maxLength={2048}
              />
              <div className="sm:col-span-2">
                <Label>{t("companyAddress")}</Label>
                <textarea
                  value={form.address}
                  onChange={(event) => updateField("address", event.target.value)}
                  rows={3}
                  maxLength={10000}
                  className="w-full rounded-lg border border-gray-300 bg-transparent px-4 py-3 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
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

function CompanyInput({
  label,
  type = "text",
  value,
  onChange,
  maxLength,
}: {
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  maxLength: number;
}) {
  return (
    <div>
      <Label>{label}</Label>
      <Input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        maxLength={maxLength}
      />
    </div>
  );
}

function CompanyValue({
  label,
  value,
}: {
  label: string;
  value?: string | null;
}) {
  const t = useTranslations("userProfile");
  return (
    <div>
      <p className="mb-2 text-xs leading-normal text-gray-500 dark:text-gray-400">
        {label}
      </p>
      <p className="break-words whitespace-pre-wrap text-sm font-medium text-gray-800 dark:text-white/90">
        {value || t("notProvided")}
      </p>
    </div>
  );
}