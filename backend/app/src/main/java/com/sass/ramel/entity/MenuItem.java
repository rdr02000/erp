package com.sass.ramel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu_items")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MenuItem {

    @Id
    @Column(name = "menu_item_id")
    private Long id;

    @Column(name = "menu_key", nullable = false, length = 100)
    private String menuKey;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 2048)
    private String route;

    @Column(name = "icon_key", length = 100)
    private String iconKey;

    @Column(name = "parent_menu_item_id")
    private Long parentMenuItemId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
