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
