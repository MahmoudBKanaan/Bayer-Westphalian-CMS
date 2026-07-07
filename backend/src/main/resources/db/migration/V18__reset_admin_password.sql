update users
set
    password_hash = '$2a$10$mEKGFHmnmMTliTDop3GfruZ/BrA.kgP1qNTvxXuutzGK8lIcHQNHC',
    status = 'ACTIVE',
    updated_at = now()
where email = 'admin@bayer-westphalian.test';
