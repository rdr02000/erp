package com.sass.ramel.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import com.sass.ramel.entity.MenuItem;

import java.util.List;

public interface MenuItemRepository extends ListCrudRepository<MenuItem, Long> {

    @Query(value = """
            SELECT DISTINCT menu_item.*
            FROM menu_items AS menu_item
            JOIN company_employee_available_menu_items AS available_menu
                ON available_menu.menu_item_id = menu_item.menu_item_id
            JOIN company_employees AS employee
                ON employee.employee_id = available_menu.employee_id
            WHERE employee.user_id = :userId
            ORDER BY menu_item.sort_order, menu_item.menu_key
            """, nativeQuery = true)
    List<MenuItem> findAvailableByUserId(@Param("userId") long userId);
}
