"use client";

import { AuthApiError, fetchCurrentUser } from "@/components/auth/authApi";
import { useSidebar } from "@/context/SidebarContext";
import { useRouter } from "@/i18n/navigation";
import AppHeader from "@/layout/AppHeader";
import AppSidebar from "@/layout/AppSidebar";
import Backdrop from "@/layout/Backdrop";
import { useTranslations } from "next-intl";
import React, { useEffect, useState } from "react";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const t = useTranslations("auth");
  const { isExpanded, isHovered, isMobileOpen } = useSidebar();
  const [sessionState, setSessionState] = useState<"checking" | "ready" | "error">(
    "checking",
  );

  // Dynamic class for main content margin based on sidebar state
  const mainContentMargin = isMobileOpen
    ? "ml-0"
    : isExpanded || isHovered
    ? "lg:ml-[290px]"
    : "lg:ml-[90px]";

  useEffect(() => {
    let active = true;
    fetchCurrentUser()
      .then(() => {
        if (active) setSessionState("ready");
      })
      .catch((cause: unknown) => {
        if (!active) return;
        if (cause instanceof AuthApiError && cause.status === 401) {
          router.replace("/signin");
          return;
        }
        setSessionState("error");
      });

    return () => {
      active = false;
    };
  }, [router]);

  if (sessionState !== "ready") {
    return (
      <main className="flex min-h-screen items-center justify-center p-6">
        <div className="space-y-3 text-center">
          {sessionState === "checking" ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {t("checkingSession")}
            </p>
          ) : (
            <>
              <p role="alert" className="text-sm text-error-500">
                {t("sessionUnavailable")}
              </p>
              <button
                type="button"
                onClick={() => window.location.reload()}
                className="text-sm font-medium text-brand-500 hover:text-brand-600"
              >
                {t("tryAgain")}
              </button>
            </>
          )}
        </div>
      </main>
    );
  }

  return (
    <div className="min-h-screen xl:flex">
      {/* Sidebar and Backdrop */}
      <AppSidebar />
      <Backdrop />
      {/* Main Content Area */}
      <div
        className={`flex-1 transition-all  duration-300 ease-in-out ${mainContentMargin}`}
      >
        {/* Header */}
        <AppHeader />
        {/* Page Content */}
        <div className="p-4 mx-auto max-w-(--breakpoint-2xl) md:p-6">{children}</div>
      </div>
    </div>
  );
}
