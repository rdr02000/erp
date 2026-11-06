"use client";

import { Link, usePathname } from "@/i18n/navigation";
import { cn } from "@/utils";
import { useTranslations } from "next-intl";
import Image from "next/image";
import { useCallback, useEffect, useRef, useState } from "react";
import { useSidebar } from "../context/SidebarContext";
import {
  ChevronDownIcon,
  GridIcon,
  HorizontaLDots,
  UserCircleIcon,
  CalenderIcon,
  PencilIcon,
  ListIcon,
  TableIcon,
  PageIcon,
  PieChartIcon,
  BoxCubeIcon,
  PlugInIcon,
} from "../icons/index";
import SidebarWidget from "./SidebarWidget";
import { fetchCurrentUser } from "@/components/auth/authApi";
import { fetchUserProfile } from "@/components/user-profile/userProfileApi";
import { fetchCompanyRoleAccess } from "@/components/company-roles/companyRoleApi";
import { fetchMenuItems, type MenuItem } from "@/components/menu/menuApi";

type NavItem = {
  key: string;
  label: string;
  icon: React.ReactNode;
  path?: string;
  subItems?: {
    key: string;
    label: string;
    path: string;
  }[];
};

const menuIcons: Record<string, React.ReactNode> = {
  dashboard: <GridIcon />,
  calendar: <CalenderIcon />,
  practice: <PencilIcon />,
  userprofile: <UserCircleIcon />,
  users: <UserCircleIcon />,
  forms: <ListIcon />,
  tables: <TableIcon />,
  pages: <PageIcon />,
  chart: <PieChartIcon />,
  reports: <PieChartIcon />,
  building: <BoxCubeIcon />,
  receipt: <ListIcon />,
  settings: <PlugInIcon />,
};

function buildNavigation(items: MenuItem[]): NavItem[] {
  const itemIds = new Set(items.map((item) => item.menuItemId));
  const childrenByParent = new Map<number, MenuItem[]>();

  items.forEach((item) => {
    if (item.parentMenuItemId === null || !itemIds.has(item.parentMenuItemId)) {
      return;
    }
    const children = childrenByParent.get(item.parentMenuItemId) ?? [];
    children.push(item);
    childrenByParent.set(item.parentMenuItemId, children);
  });

  return items
    .filter(
      (item) =>
        item.parentMenuItemId === null ||
        !itemIds.has(item.parentMenuItemId),
    )
    .sort(
      (first, second) =>
        first.sortOrder - second.sortOrder ||
        first.menuKey.localeCompare(second.menuKey),
    )
    .map((item) => {
      const childItems = (childrenByParent.get(item.menuItemId) ?? [])
        .sort(
          (first, second) =>
            first.sortOrder - second.sortOrder ||
            first.menuKey.localeCompare(second.menuKey),
        )
        .flatMap((child) =>
          child.route
            ? [
                {
                  key: child.menuKey,
                  label: child.label,
                  path: child.route,
                },
              ]
            : [],
        );
      const subItems = item.route
        ? [
            { key: item.menuKey, label: item.label, path: item.route },
            ...childItems,
          ]
        : childItems;

      return {
        key: item.menuKey,
        label: item.label,
        icon: menuIcons[item.iconKey?.toLowerCase() ?? ""] ?? <GridIcon />,
        ...(item.route ? { path: item.route } : {}),
        ...(subItems.length > 0 ? { subItems } : {}),
      };
    })
    .filter((item) => item.path || item.subItems);
}

const AppSidebar: React.FC = () => {
  const { isExpanded, isMobileOpen, isHovered, setIsHovered } = useSidebar();
  const pathname = usePathname();
  const t = useTranslations("sidebar");
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [navItems, setNavItems] = useState<NavItem[]>([]);
  const [menuLoading, setMenuLoading] = useState(true);
  const [menuError, setMenuError] = useState<string | null>(null);
  const pathnameRef = useRef(pathname);

  const renderMenuItems = (
    navItems: NavItem[],
    menuType: "main",
  ) => (
    <ul className="flex flex-col gap-1">
      {navItems.map((nav, index) => (
        <li key={nav.key}>
          {nav.subItems ? (
            <button
              onClick={() => handleSubmenuToggle(index, menuType)}
              className={cn(
                "group menu-item cursor-pointer",
                openSubmenu?.type === menuType && openSubmenu?.index === index
                  ? "menu-item-active"
                  : "menu-item-inactive",
                !isExpanded && !isHovered
                  ? "lg:justify-center"
                  : "lg:justify-start",
              )}
            >
              <span
                className={cn(
                  openSubmenu?.type === menuType && openSubmenu?.index === index
                    ? "menu-item-icon-active"
                    : "menu-item-icon-inactive",
                )}
              >
                {nav.icon}
              </span>
              {(isExpanded || isHovered || isMobileOpen) && (
                <span className="menu-item-text">{nav.label}</span>
              )}
              {(isExpanded || isHovered || isMobileOpen) && (
                <ChevronDownIcon
                  className={cn(
                    "ms-auto h-5 w-5 transition-transform duration-200",
                    openSubmenu?.type === menuType &&
                      openSubmenu?.index === index
                      ? "rotate-180 text-brand-500"
                      : "",
                  )}
                />
              )}
            </button>
          ) : (
            nav.path && (
              <Link
                href={nav.path}
                className={cn(
                  "group menu-item",
                  isActive(nav.path)
                    ? "menu-item-active"
                    : "menu-item-inactive",
                )}
              >
                <span
                  className={cn(
                    isActive(nav.path)
                      ? "menu-item-icon-active"
                      : "menu-item-icon-inactive",
                  )}
                >
                  {nav.icon}
                </span>
                {(isExpanded || isHovered || isMobileOpen) && (
                  <span className="menu-item-text">{nav.label}</span>
                )}
              </Link>
            )
          )}
          {nav.subItems && (isExpanded || isHovered || isMobileOpen) && (
            <div
              ref={(el) => {
                subMenuRefs.current[`${menuType}-${index}`] = el;
              }}
              className="overflow-hidden transition-all duration-300"
              style={{
                height:
                  openSubmenu?.type === menuType && openSubmenu?.index === index
                    ? `${subMenuHeight[`${menuType}-${index}`]}px`
                    : "0px",
              }}
            >
              <ul className="ms-9 mt-2 space-y-1">
                {nav.subItems.map((subItem) => (
                  <li key={subItem.key}>
                    <Link
                      href={subItem.path}
                      className={`menu-dropdown-item ${
                        isActive(subItem.path)
                          ? "menu-dropdown-item-active"
                          : "menu-dropdown-item-inactive"
                      }`}
                    >
                      {subItem.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </li>
      ))}
    </ul>
  );

  const [openSubmenu, setOpenSubmenu] = useState<{
    type: "main";
    index: number;
  } | null>(null);
  const [subMenuHeight, setSubMenuHeight] = useState<Record<string, number>>(
    {},
  );
  const subMenuRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const isActive = useCallback((path: string) => path === pathname, [pathname]);

  useEffect(() => {
    pathnameRef.current = pathname;
  }, [pathname]);

  useEffect(() => {
    // Set the height of the submenu items when the submenu is opened
    if (openSubmenu !== null) {
      const key = `${openSubmenu.type}-${openSubmenu.index}`;
      if (subMenuRefs.current[key]) {
        setSubMenuHeight((prevHeights) => ({
          ...prevHeights,
          [key]: subMenuRefs.current[key]?.scrollHeight || 0,
        }));
      }
    }
  }, [openSubmenu]);

  useEffect(() => {
    let active = true;

    Promise.all([fetchMenuItems(), fetchCompanyRoleAccess()])
      .then(([items, canManageCompanyRoles]) => {
        if (active) {
          const navigation = buildNavigation(items);
          if (canManageCompanyRoles) {
            navigation.push({
              key: "company-roles",
              label: t("items.companyRoles"),
              icon: <UserCircleIcon />,
              path: "/company/roles",
            });
          }
          setNavItems(navigation);
          const activeIndex = navigation.findIndex((nav) =>
            nav.subItems?.some((subItem) => subItem.path === pathnameRef.current),
          );
          if (activeIndex >= 0) {
            setOpenSubmenu({ type: "main", index: activeIndex });
          }
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setMenuError(
            error instanceof Error ? error.message : t("menuLoadFailed"),
          );
        }
      })
      .finally(() => {
        if (active) setMenuLoading(false);
      });

    return () => {
      active = false;
    };
  }, [t]);

  useEffect(() => {
    let active = true;

    fetchCurrentUser()
      .then((user) => fetchUserProfile(user.id))
      .then((profile) => {
        if (active) setAvatarUrl(profile?.company?.logoUrl || null);
      })
      .catch(() => {
        if (active) setAvatarUrl(null);
      });

    return () => {
      active = false;
    };
  }, []);

  const handleSubmenuToggle = (
    index: number,
    menuType: "main",
  ) => {
    setOpenSubmenu((prevOpenSubmenu) => {
      if (
        prevOpenSubmenu &&
        prevOpenSubmenu.type === menuType &&
        prevOpenSubmenu.index === index
      ) {
        return null;
      }
      return { type: menuType, index };
    });
  };

  return (
    <aside
      className={`fixed top-0 left-0 z-50 flex h-full flex-col border-r border-gray-200 bg-white px-5 text-gray-900 transition-all duration-300 ease-in-out xl:mt-0 rtl:right-0 rtl:left-auto rtl:border-r-0 rtl:border-l dark:border-gray-800 dark:bg-gray-900 ${
        isExpanded || isMobileOpen ? "w-72.5" : isHovered ? "w-72.5" : "w-22.5"
      } ${
        isMobileOpen
          ? "translate-x-0"
          : "-translate-x-full rtl:translate-x-full"
      } xl:translate-x-0 xl:rtl:translate-x-0`}
      onMouseEnter={() => !isExpanded && setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div
        className={`flex py-8 ${
          !isExpanded && !isHovered ? "xl:justify-center" : "justify-start"
        }`}
      >
        <Link href="/">
          {isExpanded || isHovered || isMobileOpen ? (
            <>
              <Image
                className="dark:hidden"
                src={avatarUrl || "/images/logo/logo.svg"}
                alt="Logo"
                width={150}
                height={40}
                priority
                style={{ width: "auto", height: "auto" }}
              />
              <Image
                className="hidden dark:block"
                src={avatarUrl || "/images/logo/logo-dark.svg"}
                alt="Logo"
                width={150}
                height={40}
                priority
                style={{ width: "auto", height: "auto" }}
              />
            </>
          ) : (
            <Image
              src="/images/logo/logo-icon.svg"
              alt="Logo"
              width={32}
              height={32}
              priority
              style={{ width: "auto", height: "auto" }}
            />
          )}
        </Link>
      </div>
      <div className="no-scrollbar flex flex-col overflow-y-auto duration-300 ease-linear">
        <nav className="mb-6">
          <div className="flex flex-col gap-4">
            <div>
              <h2
                className={`mb-4 flex text-xs leading-5 text-gray-400 uppercase ${
                  !isExpanded && !isHovered
                    ? "xl:justify-center"
                    : "justify-start"
                }`}
              >
                {isExpanded || isHovered || isMobileOpen ? (
                  t("groups.menu")
                ) : (
                  <HorizontaLDots />
                )}
              </h2>
              {menuLoading ? (
                <p className="px-3 text-sm text-gray-500">{t("menuLoading")}</p>
              ) : menuError ? (
                <p className="px-3 text-sm text-red-500" role="alert">
                  {menuError}
                </p>
              ) : navItems.length > 0 ? (
                renderMenuItems(navItems, "main")
              ) : (
                <p className="px-3 text-sm text-gray-500">
                  {t("noMenuItems")}
                </p>
              )}
            </div>
          </div>
        </nav>
        {isExpanded || isHovered || isMobileOpen ? <SidebarWidget /> : null}
      </div>
    </aside>
  );
};

export default AppSidebar;
