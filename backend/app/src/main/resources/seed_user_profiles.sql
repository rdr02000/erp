-- Re-running this development-only seed resets demo passwords to "password".
INSERT INTO app_users (username, password_hash)
SELECT
    'demo_user_' || lpad(user_number::text, 2, '0'),
    '$2a$10$htAB3o9AwIZWyUBpmpXl9ulCbcA.xLJ2M5QP9TX4.jLXTFb8NzwYe'
FROM generate_series(1, 5) AS samples(user_number)
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash;

WITH company_samples AS (
    SELECT *
    FROM (VALUES
        (1, 'Northstar Robotics', 'Industrial Automation', 'https://northstar-robotics.example',
            'hello@northstar-robotics.example', '+1-555-0101', '101 Orbit Avenue, Seattle, WA'),
        (2, 'Blueoak Analytics', 'Data Services', 'https://blueoak-analytics.example',
            'contact@blueoak-analytics.example', '+1-555-0102', '202 Insight Road, Austin, TX'),
        (3, 'Cedarline Logistics', 'Transportation', 'https://cedarline-logistics.example',
            'dispatch@cedarline-logistics.example', '+1-555-0103', '303 Harbor Street, Portland, OR'),
        (4, 'Solstice Healthworks', 'Healthcare', 'https://solstice-healthworks.example',
            'info@solstice-healthworks.example', '+1-555-0104', '404 Wellness Drive, Denver, CO'),
        (5, 'Copperfield Energy', 'Renewable Energy', 'https://copperfield-energy.example',
            'team@copperfield-energy.example', '+1-555-0105', '505 Solar Lane, Phoenix, AZ')
    ) AS companies(company_number, name, industry, website, email, phone, address)
), seeded_companies AS (
    INSERT INTO companies (name, industry, website, email, phone, address)
    SELECT name, industry, website, email, phone, address
    FROM company_samples
    ON CONFLICT (name) DO UPDATE SET
        industry = COALESCE(companies.industry, EXCLUDED.industry),
        website = COALESCE(companies.website, EXCLUDED.website),
        email = COALESCE(companies.email, EXCLUDED.email),
        phone = COALESCE(companies.phone, EXCLUDED.phone),
        address = COALESCE(companies.address, EXCLUDED.address)
    RETURNING company_id, name
), profile_samples AS (
    SELECT
        u.id AS user_id,
        seeded.company_id,
        (ARRAY['Alex', 'Jordan', 'Taylor', 'Morgan', 'Casey'])
            [floor(random() * 5 + 1)::int] AS first_name,
        (ARRAY['Rivera', 'Chen', 'Patel', 'Kim', 'Garcia'])
            [floor(random() * 5 + 1)::int] AS last_name,
        CASE
            WHEN u.username ~ '^demo_user_[0-9]{2}$'
                THEN u.username || '@example.test'
            ELSE 'demo.user.' || u.id || '@example.test'
        END AS email,
        '+1-555-' || lpad((u.id % 10000)::text, 4, '0') AS phone,
        (floor(random() * 8999 + 1000)::int)::text || ' Sample Street, Sample City'
            AS address,
        '/images/user/owner.jpg' AS profile_picture_url,
        'https://facebook.com/' ||
            lower((ARRAY['alex', 'jordan', 'taylor', 'morgan', 'casey'])
                [floor(random() * 5 + 1)::int]) ||
            (floor(random() * 9000 + 1000)::int)::text AS facebook_url,
        'https://x.com/' ||
            lower((ARRAY['rivera', 'chen', 'patel', 'kim', 'garcia'])
                [floor(random() * 5 + 1)::int]) ||
            (floor(random() * 9000 + 1000)::int)::text AS x_url,
        'https://linkedin.com/in/' ||
            lower((ARRAY['alex', 'jordan', 'taylor', 'morgan', 'casey'])
                [floor(random() * 5 + 1)::int]) || '-profile-' ||
            (floor(random() * 9000 + 1000)::int)::text AS linkedin_url,
        'https://instagram.com/' ||
            lower((ARRAY['rivera', 'chen', 'patel', 'kim', 'garcia'])
                [floor(random() * 5 + 1)::int]) || '_' ||
            (floor(random() * 9000 + 1000)::int)::text AS instagram_url
    FROM app_users u
    LEFT JOIN company_samples sample
        ON u.username ~ '^demo_user_[0-9]{2}$'
        AND substring(u.username from '[0-9]+$')::int = sample.company_number
    LEFT JOIN seeded_companies seeded ON seeded.name = sample.name
)
INSERT INTO user_profiles (
    user_id,
    company_id,
    first_name,
    last_name,
    email,
    phone,
    address,
    profile_picture_url,
    facebook_url,
    x_url,
    linkedin_url,
    instagram_url
)
SELECT
    user_id,
    company_id,
    first_name,
    last_name,
    email,
    phone,
    address,
    profile_picture_url,
    facebook_url,
    x_url,
    linkedin_url,
    instagram_url
FROM profile_samples
ON CONFLICT (user_id) DO UPDATE SET
    company_id = COALESCE(user_profiles.company_id, EXCLUDED.company_id),
    first_name = COALESCE(user_profiles.first_name, EXCLUDED.first_name),
    last_name = COALESCE(user_profiles.last_name, EXCLUDED.last_name),
    email = CASE
        WHEN EXCLUDED.email ~ '^demo_user_[0-9]{2}@example[.]test$'
            THEN EXCLUDED.email
        ELSE COALESCE(user_profiles.email, EXCLUDED.email)
    END,
    phone = COALESCE(user_profiles.phone, EXCLUDED.phone),
    address = COALESCE(user_profiles.address, EXCLUDED.address),
    profile_picture_url = COALESCE(user_profiles.profile_picture_url, EXCLUDED.profile_picture_url),
    facebook_url = COALESCE(user_profiles.facebook_url, EXCLUDED.facebook_url),
    x_url = COALESCE(user_profiles.x_url, EXCLUDED.x_url),
    linkedin_url = COALESCE(user_profiles.linkedin_url, EXCLUDED.linkedin_url),
    instagram_url = COALESCE(user_profiles.instagram_url, EXCLUDED.instagram_url);

-- Add 100 repeatable employee accounts distributed across the five demo companies.
WITH employee_samples AS (
    SELECT
        sample_number,
        'demo_employee_' || lpad(sample_number::text, 3, '0') AS username,
        CASE ((sample_number - 1) % 5) + 1
            WHEN 1 THEN 'Northstar Robotics'
            WHEN 2 THEN 'Blueoak Analytics'
            WHEN 3 THEN 'Cedarline Logistics'
            WHEN 4 THEN 'Solstice Healthworks'
            ELSE 'Copperfield Energy'
        END AS company_name
    FROM generate_series(1, 100) AS samples(sample_number)
)
INSERT INTO app_users (username, password_hash)
SELECT
    username,
    '$2a$10$htAB3o9AwIZWyUBpmpXl9ulCbcA.xLJ2M5QP9TX4.jLXTFb8NzwYe'
FROM employee_samples
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash;

WITH employee_samples AS (
    SELECT
        sample_number,
        'demo_employee_' || lpad(sample_number::text, 3, '0') AS username,
        CASE ((sample_number - 1) % 5) + 1
            WHEN 1 THEN 'Northstar Robotics'
            WHEN 2 THEN 'Blueoak Analytics'
            WHEN 3 THEN 'Cedarline Logistics'
            WHEN 4 THEN 'Solstice Healthworks'
            ELSE 'Copperfield Energy'
        END AS company_name
    FROM generate_series(1, 100) AS samples(sample_number)
)
INSERT INTO user_profiles (
    user_id,
    company_id,
    first_name,
    last_name,
    email
)
SELECT
    app_user.id,
    company.company_id,
    'Employee ' || lpad(employee.sample_number::text, 3, '0'),
    'Sample',
    employee.username || '@example.test'
FROM employee_samples AS employee
JOIN app_users AS app_user ON app_user.username = employee.username
JOIN companies AS company ON company.name = employee.company_name
ON CONFLICT (user_id) DO UPDATE SET
    company_id = EXCLUDED.company_id,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    email = EXCLUDED.email;

INSERT INTO products (name, product_code, description, price, is_active)
VALUES (
    'ERP Core',
    'ERP_CORE',
    'Core ERP features for company subscriptions',
    49.00,
    TRUE
)
ON CONFLICT (product_code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO products (name, product_code, description, price, is_active)
VALUES (
    'Inventory Management',
    'INVENTORY',
    'Inventory management product used to demonstrate company employee product access boundaries',
    29.00,
    TRUE
)
ON CONFLICT (product_code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_items (menu_key, label, route, icon_key, sort_order, is_active)
VALUES
    ('dashboard', 'Dashboard', '/', 'dashboard', 10, TRUE),
    ('employees', 'Employees', '/employees', 'users', 20, TRUE),
    ('customers', 'Customers', '/customers', 'building', 30, TRUE),
    ('invoices', 'Invoices', '/invoices', 'receipt', 40, TRUE),
    ('reports', 'Reports', '/reports', 'chart', 50, TRUE),
    ('company-settings', 'Company Settings', '/company/settings', 'settings', 60, TRUE)
ON CONFLICT (menu_key) DO UPDATE SET
    label = EXCLUDED.label,
    route = EXCLUDED.route,
    icon_key = EXCLUDED.icon_key,
    sort_order = EXCLUDED.sort_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO product_menu_items (product_id, menu_item_id)
SELECT product.product_id, menu_item.menu_item_id
FROM products AS product
CROSS JOIN menu_items AS menu_item
WHERE product.product_code = 'ERP_CORE'
  AND menu_item.menu_key IN (
      'dashboard',
      'employees',
      'customers',
      'invoices',
      'reports',
      'company-settings'
  )
ON CONFLICT (product_id, menu_item_id) DO NOTHING;

INSERT INTO company_product_subscriptions (
    company_id,
    product_id,
    status,
    subscribed_at,
    expires_at
)
SELECT DISTINCT
    profile.company_id,
    product.product_id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    NULL::TIMESTAMPTZ
FROM app_users AS app_user
JOIN user_profiles AS profile ON profile.user_id = app_user.id
CROSS JOIN products AS product
WHERE app_user.username ~ '^demo_user_[0-9]{2}$'
  AND profile.company_id IS NOT NULL
  AND product.product_code = 'ERP_CORE'
ON CONFLICT (company_id, product_id) DO UPDATE SET
    status = EXCLUDED.status,
    expires_at = EXCLUDED.expires_at,
    updated_at = CURRENT_TIMESTAMP;

-- Northstar Robotics subscribes to two products, but demo_user_01 is granted
-- employee access only to ERP_CORE by the access seed below.
INSERT INTO company_product_subscriptions (
    company_id,
    product_id,
    status,
    subscribed_at,
    expires_at
)
SELECT
    company.company_id,
    product.product_id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    NULL::TIMESTAMPTZ
FROM companies AS company
CROSS JOIN products AS product
WHERE company.name = 'Northstar Robotics'
  AND product.product_code = 'INVENTORY'
ON CONFLICT (company_id, product_id) DO UPDATE SET
    status = EXCLUDED.status,
    expires_at = EXCLUDED.expires_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO company_employees (company_id, user_id, job_title, status)
SELECT DISTINCT
    profile.company_id,
    app_user.id,
    'Company Administrator',
    'ACTIVE'
FROM app_users AS app_user
JOIN user_profiles AS profile ON profile.user_id = app_user.id
WHERE app_user.username ~ '^demo_user_[0-9]{2}$'
  AND profile.company_id IS NOT NULL
ON CONFLICT (company_id, user_id) DO UPDATE SET
    job_title = EXCLUDED.job_title,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO company_employees (company_id, user_id, job_title, status)
SELECT
    profile.company_id,
    app_user.id,
    'Employee',
    'ACTIVE'
FROM app_users AS app_user
JOIN user_profiles AS profile ON profile.user_id = app_user.id
WHERE app_user.username ~ '^demo_employee_[0-9]{3}$'
  AND profile.company_id IS NOT NULL
ON CONFLICT (company_id, user_id) DO UPDATE SET
    job_title = EXCLUDED.job_title,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO company_employee_product_access (
    employee_id,
    company_id,
    company_product_subscription_id
)
SELECT
    employee.employee_id,
    subscription.company_id,
    subscription.id
FROM company_employees AS employee
JOIN company_product_subscriptions AS subscription
    ON subscription.company_id = employee.company_id
JOIN products AS product
    ON product.product_id = subscription.product_id
JOIN app_users AS app_user
    ON app_user.id = employee.user_id
WHERE app_user.username ~ '^demo_user_[0-9]{2}$'
  AND product.product_code = 'ERP_CORE'
ON CONFLICT (employee_id, company_product_subscription_id) DO NOTHING;

INSERT INTO company_employee_product_access (
    employee_id,
    company_id,
    company_product_subscription_id
)
SELECT
    employee.employee_id,
    employee.company_id,
    subscription.id
FROM company_employees AS employee
JOIN app_users AS app_user ON app_user.id = employee.user_id
JOIN company_product_subscriptions AS subscription
    ON subscription.company_id = employee.company_id
JOIN products AS product ON product.product_id = subscription.product_id
WHERE app_user.username ~ '^demo_employee_[0-9]{3}$'
  AND product.product_code = 'ERP_CORE'
ON CONFLICT (employee_id, company_product_subscription_id) DO NOTHING;

DELETE FROM company_employee_product_access AS access
USING company_employees AS employee,
      app_users AS app_user,
      companies AS company,
      company_product_subscriptions AS subscription,
      products AS product
WHERE access.employee_id = employee.employee_id
  AND employee.user_id = app_user.id
  AND employee.company_id = company.company_id
  AND access.company_product_subscription_id = subscription.id
  AND subscription.product_id = product.product_id
  AND app_user.username = 'demo_user_01'
  AND company.name = 'Northstar Robotics'
  AND product.product_code <> 'ERP_CORE';

INSERT INTO company_subscription_roles (
    company_product_subscription_id,
    role_key,
    name,
    description,
    is_system
)
SELECT
    subscription.id,
    role.role_key,
    role.name,
    role.description,
    TRUE
FROM company_product_subscriptions AS subscription
JOIN products AS product ON product.product_id = subscription.product_id
CROSS JOIN (VALUES
    ('company_admin', 'Company Administrator', 'Full CRUD access to this product subscription.'),
    ('employee', 'Employee', 'Standard employee access to this product subscription.')
) AS role(role_key, name, description)
WHERE product.product_code = 'ERP_CORE'
  AND EXISTS (
      SELECT 1
      FROM app_users AS app_user
      JOIN user_profiles AS profile ON profile.user_id = app_user.id
      WHERE profile.company_id = subscription.company_id
        AND app_user.username ~ '^demo_user_[0-9]{2}$'
  )
ON CONFLICT (company_product_subscription_id, role_key) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_system = EXCLUDED.is_system,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO company_role_permissions (
    role_id,
    company_product_subscription_id,
    resource_key,
    can_create,
    can_read,
    can_update,
    can_delete
)
SELECT
    role.role_id,
    role.company_product_subscription_id,
    permission.resource_key,
    permission.can_create,
    permission.can_read,
    permission.can_update,
    permission.can_delete
FROM company_subscription_roles AS role
JOIN company_product_subscriptions AS subscription
    ON subscription.id = role.company_product_subscription_id
JOIN products AS product ON product.product_id = subscription.product_id
JOIN (VALUES
    ('company_admin', 'employees', TRUE, TRUE, TRUE, TRUE),
    ('company_admin', 'customers', TRUE, TRUE, TRUE, TRUE),
    ('company_admin', 'invoices', TRUE, TRUE, TRUE, TRUE),
    ('company_admin', 'reports', TRUE, TRUE, TRUE, TRUE),
    ('company_admin', 'company-settings', TRUE, TRUE, TRUE, TRUE),
    ('employee', 'employees', FALSE, TRUE, FALSE, FALSE),
    ('employee', 'customers', TRUE, TRUE, TRUE, FALSE),
    ('employee', 'invoices', TRUE, TRUE, TRUE, FALSE),
    ('employee', 'reports', FALSE, TRUE, FALSE, FALSE),
    ('employee', 'company-settings', FALSE, TRUE, FALSE, FALSE)
) AS permission(role_key, resource_key, can_create, can_read, can_update, can_delete)
    ON permission.role_key = role.role_key
WHERE product.product_code = 'ERP_CORE'
  AND EXISTS (
      SELECT 1
      FROM app_users AS app_user
      JOIN user_profiles AS profile ON profile.user_id = app_user.id
      WHERE profile.company_id = subscription.company_id
        AND app_user.username ~ '^demo_user_[0-9]{2}$'
  )
ON CONFLICT (role_id, resource_key) DO UPDATE SET
    can_create = EXCLUDED.can_create,
    can_read = EXCLUDED.can_read,
    can_update = EXCLUDED.can_update,
    can_delete = EXCLUDED.can_delete;

-- Assign demo_user_02 the Employee role at Blueoak Analytics instead of the admin role.
DELETE FROM company_employee_roles AS employee_role
USING company_employees AS employee,
      app_users AS app_user,
      companies AS company,
      company_product_subscriptions AS subscription,
      products AS product,
      company_subscription_roles AS role
WHERE employee_role.employee_id = employee.employee_id
  AND employee_role.company_product_subscription_id = subscription.id
  AND employee_role.role_id = role.role_id
  AND employee.user_id = app_user.id
  AND employee.company_id = company.company_id
  AND subscription.company_id = company.company_id
  AND subscription.product_id = product.product_id
  AND role.company_product_subscription_id = subscription.id
  AND app_user.username = 'demo_user_02'
  AND company.name = 'Blueoak Analytics'
  AND product.product_code = 'ERP_CORE'
  AND role.role_key = 'company_admin';

INSERT INTO company_employee_roles (
    employee_id,
    company_product_subscription_id,
    role_id,
    assigned_by_user_id
)
SELECT
    employee.employee_id,
    access.company_product_subscription_id,
    role.role_id,
    employee.user_id
FROM company_employees AS employee
JOIN app_users AS app_user ON app_user.id = employee.user_id
JOIN company_employee_product_access AS access
    ON access.employee_id = employee.employee_id
JOIN company_subscription_roles AS role
    ON role.company_product_subscription_id = access.company_product_subscription_id
   AND role.role_key = CASE
       WHEN app_user.username = 'demo_user_02'
        AND employee.company_id = (
            SELECT company_id
            FROM companies
            WHERE name = 'Blueoak Analytics'
        )
           THEN 'employee'
       ELSE 'company_admin'
   END
JOIN company_product_subscriptions AS subscription
    ON subscription.id = access.company_product_subscription_id
JOIN products AS product ON product.product_id = subscription.product_id
WHERE app_user.username ~ '^demo_user_[0-9]{2}$'
  AND product.product_code = 'ERP_CORE'
ON CONFLICT (employee_id, company_product_subscription_id, role_id) DO NOTHING;

DELETE FROM company_employee_roles AS employee_role
USING company_employees AS employee,
      app_users AS app_user,
      company_product_subscriptions AS subscription,
      products AS product
WHERE employee_role.employee_id = employee.employee_id
  AND employee_role.company_product_subscription_id = subscription.id
  AND employee.user_id = app_user.id
  AND subscription.company_id = employee.company_id
  AND subscription.product_id = product.product_id
  AND app_user.username ~ '^demo_employee_[0-9]{3}$'
  AND product.product_code = 'ERP_CORE';

INSERT INTO company_employee_roles (
    employee_id,
    company_product_subscription_id,
    role_id,
    assigned_by_user_id
)
SELECT
    employee.employee_id,
    access.company_product_subscription_id,
    role.role_id,
    NULL
FROM company_employees AS employee
JOIN app_users AS app_user ON app_user.id = employee.user_id
JOIN company_employee_product_access AS access
    ON access.employee_id = employee.employee_id
JOIN company_product_subscriptions AS subscription
    ON subscription.id = access.company_product_subscription_id
JOIN products AS product ON product.product_id = subscription.product_id
JOIN company_subscription_roles AS role
    ON role.company_product_subscription_id = subscription.id
   AND role.role_key = 'employee'
WHERE app_user.username ~ '^demo_employee_[0-9]{3}$'
  AND product.product_code = 'ERP_CORE'
ON CONFLICT (employee_id, company_product_subscription_id, role_id) DO NOTHING;

-- Select employees with exactly one assigned role across their company's subscriptions.
SELECT
    app_user.id AS user_id,
    app_user.username,
    company.company_id,
    company.name AS company_name,
    role.name AS role_name,
    product.name AS product_name
FROM company_employees AS employee
JOIN app_users AS app_user
    ON app_user.id = employee.user_id
JOIN companies AS company
    ON company.company_id = employee.company_id
JOIN company_employee_roles AS employee_role
    ON employee_role.employee_id = employee.employee_id
JOIN company_subscription_roles AS role
    ON role.role_id = employee_role.role_id
   AND role.company_product_subscription_id = employee_role.company_product_subscription_id
JOIN company_product_subscriptions AS subscription
    ON subscription.id = employee_role.company_product_subscription_id
JOIN products AS product
    ON product.product_id = subscription.product_id
JOIN (
    SELECT
        employee.employee_id,
        employee.company_id
    FROM company_employees AS employee
    JOIN company_employee_roles AS employee_role
        ON employee_role.employee_id = employee.employee_id
    GROUP BY employee.employee_id, employee.company_id
    HAVING COUNT(*) = 1
) AS employees_with_one_role
    ON employees_with_one_role.employee_id = employee.employee_id
   AND employees_with_one_role.company_id = employee.company_id;

-- List each employee's assigned role within their company and product subscription.
SELECT
    app_user.id AS user_id,
    app_user.username,
    company.company_id,
    company.name AS company_name,
    role.role_key,
    role.name AS role_name,
    product.product_code,
    product.name AS product_name
FROM company_employees AS employee
JOIN app_users AS app_user
    ON app_user.id = employee.user_id
JOIN companies AS company
    ON company.company_id = employee.company_id
JOIN company_employee_roles AS employee_role
    ON employee_role.employee_id = employee.employee_id
JOIN company_subscription_roles AS role
    ON role.role_id = employee_role.role_id
   AND role.company_product_subscription_id = employee_role.company_product_subscription_id
JOIN company_product_subscriptions AS subscription
    ON subscription.id = employee_role.company_product_subscription_id
   AND subscription.company_id = employee.company_id
JOIN products AS product
    ON product.product_id = subscription.product_id
ORDER BY company.name, app_user.username, product.name, role.name;
