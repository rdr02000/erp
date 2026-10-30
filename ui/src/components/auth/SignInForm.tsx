"use client";

import Checkbox from "@/components/form/input/Checkbox";
import Input from "@/components/form/input/InputField";
import Label from "@/components/form/Label";
import Button from "@/components/ui/button/Button";
import { useRouter } from "@/i18n/navigation";
import { EyeCloseIcon, EyeIcon } from "@/icons";
import { useTranslations } from "next-intl";
import { type FormEvent, useState } from "react";
import { signIn } from "./authApi";

export default function SignInForm() {
  const t = useTranslations("auth");
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [includeLocation, setIncludeLocation] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      const location = includeLocation ? await getBrowserLocation() : null;
      await signIn(email, password, location);
      router.replace("/profile");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : t("signInFailed"));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex w-full flex-1 flex-col lg:w-1/2">
      <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center">
        <div className="mb-5 sm:mb-8">
          <h1 className="mb-2 text-title-sm font-semibold text-gray-800 sm:text-title-md dark:text-white/90">
            {t("signIn")}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {t("signInDescription")}
          </p>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="space-y-6">
            <div>
              <Label htmlFor="signin-email">
                {t("email")} <span className="text-error-500">*</span>
              </Label>
              <Input
                id="signin-email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder={t("emailPlaceholder")}
              />
            </div>
            <div>
              <Label htmlFor="signin-password">
                {t("password")} <span className="text-error-500">*</span>
              </Label>
              <div className="relative">
                <Input
                  id="signin-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder={t("passwordPlaceholder")}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((visible) => !visible)}
                  aria-label={showPassword ? t("hidePassword") : t("showPassword")}
                  className="inset-e-4 absolute top-1/2 z-30 -translate-y-1/2"
                >
                  {showPassword ? (
                    <EyeIcon className="fill-gray-500 dark:fill-gray-400" />
                  ) : (
                    <EyeCloseIcon className="fill-gray-500 dark:fill-gray-400" />
                  )}
                </button>
              </div>
            </div>
            <div className="space-y-2">
              <Checkbox
                id="signin-include-location"
                checked={includeLocation}
                onChange={setIncludeLocation}
                label={t("includeLoginLocation")}
              />
            </div>
            {error && (
              <p
                role="alert"
                className="rounded-lg border border-error-500/30 bg-error-50 px-4 py-3 text-sm text-error-600 dark:bg-error-500/10 dark:text-error-400"
              >
                {error}
              </p>
            )}
            <Button
              className="w-full"
              size="sm"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? t("signingIn") : t("signIn")}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function getBrowserLocation(): Promise<{
  latitude: number;
  longitude: number;
  accuracyMeters: number;
} | null> {
  if (!navigator.geolocation) return Promise.resolve(null);

  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      ({ coords }) =>
        resolve({
          latitude: coords.latitude,
          longitude: coords.longitude,
          accuracyMeters: coords.accuracy,
        }),
      () => resolve(null),
        { enableHighAccuracy: false, timeout: 5000, maximumAge: 60000 },
    );
  });
}
