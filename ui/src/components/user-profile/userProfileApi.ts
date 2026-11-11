import { getResponseError, sessionFetch } from "@/components/auth/authApi";

export interface UserProfile {
  userId: number;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  profilePictureUrl: string | null;
  address: string | null;
  facebookUrl: string | null;
  xUrl: string | null;
  linkedinUrl: string | null;
  instagramUrl: string | null;
  company: CompanyDetails | null;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyDetails {
  id: number;
  name: string;
  industry: string | null;
  website: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  logoUrl: string | null;
}

export interface CompanyDetailsInput {
  name: string;
  industry: string;
  website: string;
  email: string;
  phone: string;
  address: string;
  logoUrl: string;
}

export interface UserProfileInput {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  profilePictureUrl: string | null;
  address: string | null;
  facebookUrl: string | null;
  xUrl: string | null;
  linkedinUrl: string | null;
  instagramUrl: string | null;
  company: CompanyDetailsInput | null;
}

export interface UserProductAccess {
  subscriptionId: number;
  companyName: string;
  productCode: string;
  productName: string;
  roles: string[];
  permissions: UserResourcePermission[];
}

export interface UserResourcePermission {
  resourceKey: string;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  hasOverride: boolean;
}

export function toCompanyDetailsInput(
  company: CompanyDetails | null | undefined,
): CompanyDetailsInput {
  return {
    name: company?.name ?? "",
    industry: company?.industry ?? "",
    website: company?.website ?? "",
    email: company?.email ?? "",
    phone: company?.phone ?? "",
    address: company?.address ?? "",
    logoUrl: company?.logoUrl ?? "",
  };
}

export function toUserProfileInput(
  profile: UserProfile | null,
): UserProfileInput {
  return {
    firstName: profile?.firstName ?? "",
    lastName: profile?.lastName ?? "",
    email: profile?.email ?? "",
    phone: profile?.phone ?? "",
    profilePictureUrl: profile?.profilePictureUrl ?? "",
    address: profile?.address ?? "",
    facebookUrl: profile?.facebookUrl ?? "",
    xUrl: profile?.xUrl ?? "",
    linkedinUrl: profile?.linkedinUrl ?? "",
    instagramUrl: profile?.instagramUrl ?? "",
    company: profile?.company ? toCompanyDetailsInput(profile.company) : null,
  };
}

export async function fetchUserProfile(
  userId: number,
): Promise<UserProfile | null> {
  const response = await sessionFetch(`/api/users/${userId}/profile`);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<UserProfile>;
}

export async function fetchUserProductAccess(): Promise<UserProductAccess[]> {
  const response = await sessionFetch("/api/company/role-management/my-access");
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<UserProductAccess[]>;
}

export async function saveUserProfile(
  userId: number,
  profile: UserProfileInput,
  exists: boolean,
): Promise<UserProfile> {
  const response = await sessionFetch(`/api/users/${userId}/profile`, {
    method: exists ? "PUT" : "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(profile),
  });
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<UserProfile>;
}
