# Data Architecture & Persistence Layer

The PostgreSQL schema defines user and company identity data, individual and company product subscriptions, subscription-scoped employee permissions, and product-driven menu navigation. The diagram shows all 14 persisted tables; the three SQL views are listed below as derived data.

## Entity Model

<!-- mermaid-checked: every attribute is `<type> <name> [<key>] ["<description>"]` with at most one of PK/FK/UK, no \n in descriptions, no {} in descriptions, every relationship label is double-quoted -->
```mermaid
erDiagram
    app_users ||--o| user_profiles : "has profile"
    companies ||--o{ user_profiles : "associated with"
    app_users ||--o{ login_activities : "has login history"
    app_users ||--o{ user_product_subscriptions : "subscribes individually"
    products ||--o{ user_product_subscriptions : "is subscribed to"
    companies ||--o{ company_product_subscriptions : "subscribes to"
    products ||--o{ company_product_subscriptions : "is subscribed to"
    companies ||--o{ company_employees : "employs"
    app_users ||--o{ company_employees : "belongs to company"
    company_employees ||--o{ company_employee_product_access : "receives product access"
    company_product_subscriptions ||--o{ company_employee_product_access : "grants employee access"
    company_product_subscriptions ||--o{ company_subscription_roles : "defines roles"
    company_subscription_roles ||--o{ company_role_permissions : "has permissions"
    company_employee_product_access ||--o{ company_employee_roles : "receives role"
    company_subscription_roles ||--o{ company_employee_roles : "assigned as"
    app_users o|--o{ company_employee_roles : "assigns role"
    products ||--o{ product_menu_items : "offers menu items"
    menu_items ||--o{ product_menu_items : "available to products"
    menu_items o|--o{ menu_items : "has child items"

    app_users {
        bigint id PK
        string username UK
        string password_hash
        timestamptz created_at
    }
    user_profiles {
        bigint user_id PK "also FK to app_users"
        bigint company_id FK
        string first_name
        string last_name
        string email UK
        string phone
    }
    companies {
        bigint company_id PK
        string name UK
        string industry
        string email
        string phone
        text address
    }
    login_activities {
        bigint id PK
        bigint user_id FK
        string device_type
        string browser
        string operating_system
        double latitude
        double longitude
        timestamptz logged_at
    }
    products {
        bigint product_id PK
        string name UK
        string product_code UK
        numeric price
        boolean is_active
    }
    user_product_subscriptions {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        string status
        timestamptz subscribed_at
        timestamptz expires_at
    }
    company_product_subscriptions {
        bigint id PK
        bigint company_id FK
        bigint product_id FK
        string status
        timestamptz subscribed_at
        timestamptz expires_at
    }
    company_employees {
        bigint employee_id PK
        bigint company_id FK
        bigint user_id FK
        string job_title
        string status
        timestamptz joined_at
    }
    company_employee_product_access {
        bigint employee_id PK "also FK to company_employees"
        bigint company_product_subscription_id PK "also FK to company_product_subscriptions"
        bigint company_id FK
        timestamptz granted_at
    }
    company_subscription_roles {
        bigint role_id PK
        bigint company_product_subscription_id FK
        string role_key
        string name
        boolean is_system
    }
    company_role_permissions {
        bigint role_id PK "also FK to company_subscription_roles"
        string resource_key PK
        bigint company_product_subscription_id FK
        boolean can_create
        boolean can_read
        boolean can_update
        boolean can_delete
    }
    company_employee_roles {
        bigint employee_id PK "also FK to company_employee_product_access"
        bigint company_product_subscription_id PK "also FK to company_employee_product_access"
        bigint role_id PK "also FK to company_subscription_roles"
        bigint assigned_by_user_id FK
        timestamptz assigned_at
    }
    menu_items {
        bigint menu_item_id PK
        string menu_key UK
        string label
        string route
        bigint parent_menu_item_id FK
        integer sort_order
        boolean is_active
    }
    product_menu_items {
        bigint product_id PK "also FK to products"
        bigint menu_item_id PK "also FK to menu_items"
    }
```

### Derived Views

- `user_available_menu_items` resolves active menu items from an individual user's active product subscriptions.
- `company_employee_available_menu_items` resolves menu items granted through an employee's company product access.
- `company_employee_product_permissions` aggregates the CRUD permissions of an employee's assigned roles by resource.
