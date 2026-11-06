CREATE TABLE IF NOT EXISTS company_employee_permission_overrides (
    employee_id BIGINT NOT NULL,
    company_product_subscription_id BIGINT NOT NULL,
    resource_key VARCHAR(150) NOT NULL,
    can_create BOOLEAN NOT NULL,
    can_read BOOLEAN NOT NULL,
    can_update BOOLEAN NOT NULL,
    can_delete BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (employee_id, company_product_subscription_id, resource_key),
    CONSTRAINT fk_employee_permission_override_access
        FOREIGN KEY (employee_id, company_product_subscription_id)
        REFERENCES company_employee_product_access (employee_id, company_product_subscription_id)
        ON DELETE CASCADE
);

CREATE OR REPLACE VIEW company_employee_product_permissions AS
SELECT
    employee.employee_id,
    access.company_product_subscription_id,
    subscription.company_id,
    subscription.product_id,
    permission.resource_key,
    COALESCE(permission_override.can_create, BOOL_OR(permission.can_create)) AS can_create,
    COALESCE(permission_override.can_read, BOOL_OR(permission.can_read)) AS can_read,
    COALESCE(permission_override.can_update, BOOL_OR(permission.can_update)) AS can_update,
    COALESCE(permission_override.can_delete, BOOL_OR(permission.can_delete)) AS can_delete,
    (permission_override.employee_id IS NOT NULL) AS has_override
FROM company_employee_product_access AS access
JOIN company_employees AS employee
    ON employee.employee_id = access.employee_id
JOIN company_product_subscriptions AS subscription
    ON subscription.id = access.company_product_subscription_id
JOIN products AS product
    ON product.product_id = subscription.product_id
JOIN company_employee_roles AS employee_role
    ON employee_role.employee_id = access.employee_id
   AND employee_role.company_product_subscription_id = access.company_product_subscription_id
JOIN company_role_permissions AS permission
    ON permission.role_id = employee_role.role_id
   AND permission.company_product_subscription_id = employee_role.company_product_subscription_id
LEFT JOIN company_employee_permission_overrides AS permission_override
    ON permission_override.employee_id = employee.employee_id
   AND permission_override.company_product_subscription_id = access.company_product_subscription_id
   AND permission_override.resource_key = permission.resource_key
WHERE employee.status = 'ACTIVE'
  AND subscription.status = 'ACTIVE'
  AND subscription.subscribed_at <= CURRENT_TIMESTAMP
  AND (subscription.expires_at IS NULL OR subscription.expires_at > CURRENT_TIMESTAMP)
  AND product.is_active
GROUP BY
    employee.employee_id,
    access.company_product_subscription_id,
    subscription.company_id,
    subscription.product_id,
    permission.resource_key,
    permission_override.employee_id,
    permission_override.can_create,
    permission_override.can_read,
    permission_override.can_update,
    permission_override.can_delete;