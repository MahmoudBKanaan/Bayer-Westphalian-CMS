update users
set
    password_hash = '$2a$10$7I/H4CZsDZEdTyzfcUZi7uZ.rDAQ8YpISVhtcqqB17Nq1/A3ZJeHC',
    status = 'ACTIVE',
    updated_at = now()
where email in (
    'admin@bayer-westphalian.test',
    'compliance.officer@bayer-westphalian.test'
);
