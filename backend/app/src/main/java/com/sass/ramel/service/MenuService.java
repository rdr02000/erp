package com.sass.ramel.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sass.ramel.dto.MenuApiModels.MenuItemView;
import com.sass.ramel.entity.MenuItem;
import com.sass.ramel.repository.MenuItemRepository;

import java.util.List;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;

    public MenuService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemView> getAvailableMenuItems(long userId) {
        return menuItemRepository.findAvailableByUserId(userId).stream()
                .map(MenuService::toView)
                .toList();
    }

    private static MenuItemView toView(MenuItem menuItem) {
        return new MenuItemView(
                menuItem.getId(),
                menuItem.getMenuKey(),
                menuItem.getLabel(),
                menuItem.getRoute(),
                menuItem.getIconKey(),
                menuItem.getParentMenuItemId(),
                menuItem.getSortOrder());
    }
}
