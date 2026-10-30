"use client";

import Button from "@/components/ui/button/Button";
import { useRouter } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { signOut } from "./authApi";

export default function SignOutButton() {
  const t = useTranslations("auth");
  const router = useRouter();
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSignOut = async () => {
    setIsSigningOut(true);
    setError(null);
    try {
      await signOut();
      router.replace("/signin");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : t("signOutFailed"));
    } finally {
      setIsSigningOut(false);
    }
  };

  return (
    <div className="flex items-center gap-2">
      {error && (
        <span role="alert" className="text-xs text-error-500">
          {error}
        </span>
      )}
      <Button
        type="button"
        size="sm"
        disabled={isSigningOut}
        onClick={handleSignOut}
      >
        {isSigningOut ? t("signingOut") : t("signOut")}
      </Button>
    </div>
  );
}
