-- Insert admin account
INSERT INTO admin (id, email, password, role)
VALUES (
    gen_random_uuid(),
    'admin@ezmovie.com',
    '$2a$10$rDkPvvAFV8cLz5h6Ux5QeO5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    'ADMIN'
);

-- Insert staff account
INSERT INTO admin (id, email, password, role)
VALUES (
    gen_random_uuid(),
    'staff@ezmovie.com',
    '$2a$10$rDkPvvAFV8cLz5h6Ux5QeO5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z',
    'STAFF'
); 