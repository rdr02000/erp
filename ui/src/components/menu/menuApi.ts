import { getResponseError, sessionFetch } from "@/components/auth/authApi";

export interface MenuItem {
  menuItemId: number;
  menuKey: string;
  label: string;
  route: string | null;
  iconKey: string | null;
  parentMenuItemId: number | null;
  sortOrder: number;
}

export async function fetchMenuItems(): Promise<MenuItem[]> {
  const response = await sessionFetch("/api/menu-items");
  if (!response.ok) {
    throw await getResponseError(response);
  }
  return response.json() as Promise<MenuItem[]>;
}
