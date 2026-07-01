-- ============================================================
-- V2: Seed permissions, roles, default admin user, approval routing
-- ============================================================

-- ---------- Permissions: module x action ----------
INSERT INTO permissions (module, action, code, description) VALUES
    ('ADMIN',   'CREATE',  'ADMIN_CREATE',   'Create admin entities (users, roles, routing)'),
    ('ADMIN',   'VIEW',    'ADMIN_VIEW',     'View admin entities'),
    ('ADMIN',   'UPDATE',  'ADMIN_UPDATE',   'Update admin entities'),
    ('ADMIN',   'DELETE',  'ADMIN_DELETE',   'Delete admin entities'),
    ('ADMIN',   'APPROVE', 'ADMIN_APPROVE',  'Approve admin-level actions'),
    ('MASTER',  'CREATE',  'MASTER_CREATE',  'Create master party records'),
    ('MASTER',  'VIEW',    'MASTER_VIEW',    'View master party records'),
    ('MASTER',  'UPDATE',  'MASTER_UPDATE',  'Update master party records'),
    ('MASTER',  'DELETE',  'MASTER_DELETE',  'Delete master party records'),
    ('MASTER',  'APPROVE', 'MASTER_APPROVE', 'Approve master-level actions'),
    ('BOOKING', 'CREATE',  'BOOKING_CREATE', 'Create courier bookings'),
    ('BOOKING', 'VIEW',    'BOOKING_VIEW',   'View courier bookings'),
    ('BOOKING', 'UPDATE',  'BOOKING_UPDATE', 'Update courier bookings'),
    ('BOOKING', 'DELETE',  'BOOKING_DELETE', 'Delete courier bookings'),
    ('BOOKING', 'APPROVE', 'BOOKING_APPROVE','Approve or reject courier bookings'),
    ('REPORTS', 'CREATE',  'REPORTS_CREATE', 'Create/schedule reports'),
    ('REPORTS', 'VIEW',    'REPORTS_VIEW',   'View and export reports'),
    ('REPORTS', 'UPDATE',  'REPORTS_UPDATE', 'Update report configuration'),
    ('REPORTS', 'DELETE',  'REPORTS_DELETE', 'Delete reports'),
    ('REPORTS', 'APPROVE', 'REPORTS_APPROVE','Approve report-level actions');

-- ---------- Roles ----------
INSERT INTO roles (name, description, system_role, created_by, created_at) VALUES
    ('ADMIN',         'Full system administrator', TRUE,  'system', NOW()),
    ('BOOKING_CLERK', 'Creates and manages courier bookings and parties', FALSE, 'system', NOW()),
    ('APPROVER',      'Approves or rejects courier bookings', FALSE, 'system', NOW()),
    ('VIEWER',        'Read-only access to bookings and reports', FALSE, 'system', NOW());

-- ADMIN role: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'ADMIN'), p.id FROM permissions p;

-- BOOKING_CLERK: master + booking create/view/update + reports view
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'BOOKING_CLERK'), p.id
FROM permissions p
WHERE p.code IN (
    'MASTER_CREATE','MASTER_VIEW','MASTER_UPDATE',
    'BOOKING_CREATE','BOOKING_VIEW','BOOKING_UPDATE',
    'REPORTS_VIEW'
);

-- APPROVER: view + approve bookings, view reports
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'APPROVER'), p.id
FROM permissions p
WHERE p.code IN ('BOOKING_VIEW','BOOKING_APPROVE','MASTER_VIEW','REPORTS_VIEW');

-- VIEWER: view-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'VIEWER'), p.id
FROM permissions p
WHERE p.code IN ('BOOKING_VIEW','MASTER_VIEW','REPORTS_VIEW','ADMIN_VIEW');

-- ---------- Default admin user (password: Admin@123, BCrypt) ----------
INSERT INTO users (username, password_hash, full_name, email, phone, active, created_by, created_at)
VALUES ('admin',
        '$2a$10$ReeZx/avmioSgCOAFexMie45yIVhXRED2dif5ojan.6YFbwUZmb2m',
        'System Administrator',
        'admin@courierapp.local',
        NULL,
        TRUE,
        'system',
        NOW());

INSERT INTO user_roles (user_id, role_id)
VALUES ((SELECT id FROM users WHERE username = 'admin'),
        (SELECT id FROM roles WHERE name = 'ADMIN'));

-- ---------- Approval routing: APPROVER role designated to approve bookings ----------
INSERT INTO approval_routing (role_id, user_id, active, created_by, created_at)
VALUES ((SELECT id FROM roles WHERE name = 'APPROVER'), NULL, TRUE, 'system', NOW());

-- Also allow ADMIN role to approve out of the box
INSERT INTO approval_routing (role_id, user_id, active, created_by, created_at)
VALUES ((SELECT id FROM roles WHERE name = 'ADMIN'), NULL, TRUE, 'system', NOW());
