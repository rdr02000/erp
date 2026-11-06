package com.sass.ramel.dto;

public final class MenuApiModels {

    private MenuApiModels() {
    }

    public record MenuItemView(
            long menuItemId,
            String menuKey,
            String label,
            String route,
            String iconKey,
            Long parentMenuItemId,
            int sortOrder) {
    }
}
