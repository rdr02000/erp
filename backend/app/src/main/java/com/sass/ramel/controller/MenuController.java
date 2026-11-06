package com.sass.ramel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sass.ramel.dto.MenuApiModels.MenuItemView;
import com.sass.ramel.security.AuthenticatedUser;
import com.sass.ramel.service.MenuService;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public List<MenuItemView> getAvailableMenuItems(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return menuService.getAvailableMenuItems(currentUser.id());
    }
}
