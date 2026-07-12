update users
set
    password_hash = '$2a$10$GFtULbvKYFbewg0wAdMEzOdxlIoeSLiCmAW1yINnGYWZWMacUIBwm',
    status = 'ACTIVE',
    updated_at = now()
where email in (
    'admin@bayer-westphalian.test',
    'campaign.manager@bayer-westphalian.test',
    'bi.analyst@bayer-westphalian.test',
    'product.manager@bayer-westphalian.test',
    'compliance.officer@bayer-westphalian.test',
    'customer.service@bayer-westphalian.test'
);