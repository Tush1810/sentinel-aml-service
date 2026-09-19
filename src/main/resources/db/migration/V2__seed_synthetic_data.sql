-- ============================================================================
-- V2__seed_synthetic_data.sql
-- Synthetic (non-PII) seed dataset for Sentinel AML prototype.
--
-- Covers customers, accounts and transactions for at least 5 laundering
-- typologies (problem statement requires >= 3):
--   1. STRUCTURING            (rules.yml: STRUCTURING)
--   2. RAPID_MOVEMENT         (rules.yml: RAPID_MOVEMENT)
--   3. HIGH_RISK_JURISDICTION (rules.yml: HIGH_RISK_JURISDICTION)
--   4. ROUND_NUMBER           (rules.yml: ROUND_NUMBER)
--   5. CTR_THRESHOLD          (rules.yml: CTR_THRESHOLD)
--
-- Business keys used here (CUST_10001+, ACC_100001+, TXN_100001+) are chosen
-- to avoid collision with pre-existing sample refs CUST_00001/00002 and
-- ACC_000001/000002 referenced elsewhere in the codebase.
--
-- All foreign keys are resolved via subqueries on the *_ref business keys,
-- never hardcoded numeric ids.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- CUSTOMERS (8): varied risk_rating, kyc_status, one PEP. Indian names/cities.
-- ----------------------------------------------------------------------------
INSERT INTO customer (
    customer_ref, first_name, last_name, gender, date_of_birth, email, phone_number,
    city, state, country, postal_code, occupation, annual_income, marital_status,
    education_level, employment_status, customer_since, customer_segment,
    kyc_status, risk_rating, politically_exposed, preferred_channel,
    email_verified, phone_verified, num_complaints_last_year
) VALUES
('CUST_10001', 'Aarav', 'Sharma', 'MALE', '1985-04-12', 'aarav.sharma.10001@example.com', '+91-9800010001',
    'Mumbai', 'Maharashtra', 'IN', '400001', 'Business Owner', 2800000.00, 'MARRIED',
    'GRADUATE', 'SELF_EMPLOYED', '2015-06-01', 'RETAIL',
    'VERIFIED', 'HIGH', FALSE, 'MOBILE', TRUE, TRUE, 0),

('CUST_10002', 'Priya', 'Nair', 'FEMALE', '1990-08-23', 'priya.nair.10002@example.com', '+91-9800010002',
    'Bengaluru', 'Karnataka', 'IN', '560001', 'Software Engineer', 1800000.00, 'SINGLE',
    'POST_GRADUATE', 'SALARIED', '2018-01-15', 'RETAIL',
    'VERIFIED', 'LOW', FALSE, 'MOBILE', TRUE, TRUE, 0),

('CUST_10003', 'Rohan', 'Verma', 'MALE', '1978-11-02', 'rohan.verma.10003@example.com', '+91-9800010003',
    'Delhi', 'Delhi', 'IN', '110001', 'Politician', 5200000.00, 'MARRIED',
    'GRADUATE', 'SELF_EMPLOYED', '2010-03-20', 'PRIVATE_BANKING',
    'VERIFIED', 'HIGH', TRUE, 'BRANCH', TRUE, TRUE, 1),

('CUST_10004', 'Ishita', 'Bose', 'FEMALE', '1995-02-14', 'ishita.bose.10004@example.com', '+91-9800010004',
    'Kolkata', 'West Bengal', 'IN', '700001', 'Marketing Manager', 1450000.00, 'SINGLE',
    'GRADUATE', 'SALARIED', '2019-07-10', 'RETAIL',
    'VERIFIED', 'MEDIUM', FALSE, 'MOBILE', TRUE, TRUE, 0),

('CUST_10005', 'Vikram', 'Reddy', 'MALE', '1982-06-30', 'vikram.reddy.10005@example.com', '+91-9800010005',
    'Hyderabad', 'Telangana', 'IN', '500001', 'Import Export Trader', 3600000.00, 'MARRIED',
    'GRADUATE', 'SELF_EMPLOYED', '2012-09-05', 'BUSINESS',
    'PENDING', 'HIGH', FALSE, 'BRANCH', FALSE, TRUE, 2),

('CUST_10006', 'Meera', 'Iyer', 'FEMALE', '1988-12-19', 'meera.iyer.10006@example.com', '+91-9800010006',
    'Chennai', 'Tamil Nadu', 'IN', '600001', 'Doctor', 2200000.00, 'MARRIED',
    'POST_GRADUATE', 'SALARIED', '2016-04-22', 'RETAIL',
    'VERIFIED', 'LOW', FALSE, 'MOBILE', TRUE, TRUE, 0),

('CUST_10007', 'Karan', 'Malhotra', 'MALE', '1993-09-08', 'karan.malhotra.10007@example.com', '+91-9800010007',
    'Pune', 'Maharashtra', 'IN', '411001', 'Freelance Consultant', 980000.00, 'SINGLE',
    'GRADUATE', 'SELF_EMPLOYED', '2020-11-11', 'RETAIL',
    'PENDING', 'MEDIUM', FALSE, 'MOBILE', TRUE, FALSE, 1),

('CUST_10008', 'Ananya', 'Joshi', 'FEMALE', '1975-03-27', 'ananya.joshi.10008@example.com', '+91-9800010008',
    'Ahmedabad', 'Gujarat', 'IN', '380001', 'Diamond Trader', 6100000.00, 'MARRIED',
    'GRADUATE', 'SELF_EMPLOYED', '2008-05-30', 'PRIVATE_BANKING',
    'REJECTED', 'HIGH', FALSE, 'BRANCH', FALSE, FALSE, 3);


-- ----------------------------------------------------------------------------
-- ACCOUNTS (12): SAVINGS / CURRENT / NRE, all INR.
-- ----------------------------------------------------------------------------
INSERT INTO account (
    account_ref, customer_id, account_type, account_status, currency, open_date,
    branch_code, branch_city, current_balance, avg_monthly_balance_6m, credit_limit,
    credit_utilization_pct, overdraft_enabled, card_type, joint_account,
    num_linked_devices, mobile_banking_enrolled, last_login_date, avg_monthly_txn_count,
    account_tier
) VALUES
('ACC_100001', (SELECT id FROM customer WHERE customer_ref = 'CUST_10001'), 'CURRENT', 'ACTIVE', 'INR', '2015-06-05',
    'MUM01', 'Mumbai', 425000.00, 380000.00, 500000.00, 40.00, TRUE, 'BUSINESS_DEBIT', FALSE,
    2, TRUE, '2026-09-17', 45, 'GOLD'),

('ACC_100002', (SELECT id FROM customer WHERE customer_ref = 'CUST_10001'), 'SAVINGS', 'ACTIVE', 'INR', '2015-06-05',
    'MUM01', 'Mumbai', 152000.00, 140000.00, NULL, NULL, FALSE, 'CLASSIC', FALSE,
    2, TRUE, '2026-09-16', 20, 'SILVER'),

('ACC_100003', (SELECT id FROM customer WHERE customer_ref = 'CUST_10002'), 'SAVINGS', 'ACTIVE', 'INR', '2018-01-20',
    'BLR02', 'Bengaluru', 310000.00, 295000.00, NULL, NULL, FALSE, 'CLASSIC', FALSE,
    1, TRUE, '2026-09-18', 30, 'SILVER'),

('ACC_100004', (SELECT id FROM customer WHERE customer_ref = 'CUST_10003'), 'CURRENT', 'ACTIVE', 'INR', '2010-03-25',
    'DEL01', 'Delhi', 980000.00, 850000.00, 1000000.00, 55.00, TRUE, 'PLATINUM', FALSE,
    3, TRUE, '2026-09-19', 25, 'PLATINUM'),

('ACC_100005', (SELECT id FROM customer WHERE customer_ref = 'CUST_10003'), 'NRE', 'ACTIVE', 'INR', '2011-02-10',
    'DEL01', 'Delhi', 2100000.00, 1950000.00, NULL, NULL, FALSE, 'PLATINUM', FALSE,
    1, TRUE, '2026-09-14', 8, 'PLATINUM'),

('ACC_100006', (SELECT id FROM customer WHERE customer_ref = 'CUST_10004'), 'SAVINGS', 'ACTIVE', 'INR', '2019-07-15',
    'KOL01', 'Kolkata', 88000.00, 92000.00, NULL, NULL, FALSE, 'CLASSIC', FALSE,
    2, TRUE, '2026-09-18', 18, 'SILVER'),

('ACC_100007', (SELECT id FROM customer WHERE customer_ref = 'CUST_10005'), 'CURRENT', 'ACTIVE', 'INR', '2012-09-10',
    'HYD01', 'Hyderabad', 610000.00, 540000.00, 750000.00, 62.00, TRUE, 'BUSINESS_DEBIT', FALSE,
    2, TRUE, '2026-09-19', 55, 'GOLD'),

('ACC_100008', (SELECT id FROM customer WHERE customer_ref = 'CUST_10005'), 'NRE', 'ACTIVE', 'INR', '2013-01-18',
    'HYD01', 'Hyderabad', 1500000.00, 1400000.00, NULL, NULL, FALSE, 'PLATINUM', FALSE,
    1, FALSE, '2026-08-30', 6, 'GOLD'),

('ACC_100009', (SELECT id FROM customer WHERE customer_ref = 'CUST_10006'), 'SAVINGS', 'ACTIVE', 'INR', '2016-04-25',
    'CHE01', 'Chennai', 245000.00, 230000.00, NULL, NULL, FALSE, 'CLASSIC', FALSE,
    2, TRUE, '2026-09-17', 22, 'SILVER'),

('ACC_100010', (SELECT id FROM customer WHERE customer_ref = 'CUST_10007'), 'SAVINGS', 'ACTIVE', 'INR', '2020-11-15',
    'PUN01', 'Pune', 62000.00, 58000.00, NULL, NULL, FALSE, 'CLASSIC', FALSE,
    1, TRUE, '2026-09-15', 15, 'SILVER'),

('ACC_100011', (SELECT id FROM customer WHERE customer_ref = 'CUST_10008'), 'CURRENT', 'DORMANT', 'INR', '2008-06-01',
    'AHM01', 'Ahmedabad', 3200000.00, 2900000.00, 2000000.00, 18.00, TRUE, 'BUSINESS_DEBIT', FALSE,
    1, FALSE, '2026-06-01', 5, 'PLATINUM'),

('ACC_100012', (SELECT id FROM customer WHERE customer_ref = 'CUST_10008'), 'NRE', 'ACTIVE', 'INR', '2009-02-14',
    'AHM01', 'Ahmedabad', 4500000.00, 4200000.00, NULL, NULL, FALSE, 'PLATINUM', FALSE,
    1, TRUE, '2026-09-10', 10, 'PLATINUM');


-- ============================================================================
-- TRANSACTIONS (~60+)
-- Base reference date for "recent" activity: 2026-09-19. History spans back
-- roughly 120 days (from ~2026-05-22) to give behavioural-baseline rules
-- enough data, with the suspicious clusters concentrated in the last ~10 days.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- TYPOLOGY 1: STRUCTURING
-- Rule STRUCTURING: >=3 txns with amountBase in [9000,9999] on ONE account
-- within a 24h window. Account ACC_100001 (Aarav Sharma, CURRENT, HIGH risk).
-- ----------------------------------------------------------------------------
INSERT INTO txn (txn_ref, account_id, direction, txn_type, amount, currency, amount_base,
                  exchange_rate, counterparty_name, counterparty_account, counterparty_bank,
                  counterparty_country, channel, description, txn_timestamp)
VALUES
('TXN_100001', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'CREDIT', 'CASH_DEPOSIT',
    9200.00, 'INR', 9200.00, 1.0, 'Cash Counter', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Cash deposit', '2026-09-15 09:15:00+05:30'),
('TXN_100002', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'CREDIT', 'CASH_DEPOSIT',
    9500.00, 'INR', 9500.00, 1.0, 'Cash Counter', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Cash deposit', '2026-09-15 13:40:00+05:30'),
('TXN_100003', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'CREDIT', 'CASH_DEPOSIT',
    9800.00, 'INR', 9800.00, 1.0, 'Cash Counter', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Cash deposit', '2026-09-15 19:05:00+05:30'),
('TXN_100004', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'CREDIT', 'CASH_DEPOSIT',
    9100.00, 'INR', 9100.00, 1.0, 'Cash Counter', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Cash deposit', '2026-09-16 08:50:00+05:30');


-- ----------------------------------------------------------------------------
-- TYPOLOGY 2: RAPID_MOVEMENT
-- Rule RAPID_MOVEMENT: large CREDIT then DEBITs totalling >=80% within 48h.
-- Account ACC_100007 (Vikram Reddy, CURRENT, HIGH risk, import/export trader).
-- ----------------------------------------------------------------------------
INSERT INTO txn (txn_ref, account_id, direction, txn_type, amount, currency, amount_base,
                  exchange_rate, counterparty_name, counterparty_account, counterparty_bank,
                  counterparty_country, channel, description, txn_timestamp)
VALUES
('TXN_100005', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'CREDIT', 'WIRE',
    500000.00, 'INR', 500000.00, 1.0, 'Global Traders Ltd', 'GT-88213', 'HSBC', 'AE', 'ONLINE',
    'Incoming wire - trade settlement', '2026-09-10 10:00:00+05:30'),
('TXN_100006', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'DEBIT', 'TRANSFER',
    150000.00, 'INR', 150000.00, 1.0, 'Sunrise Exports', 'SE-11209', 'ICICI Bank', 'IN', 'ONLINE',
    'Vendor payment', '2026-09-10 15:30:00+05:30'),
('TXN_100007', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'DEBIT', 'TRANSFER',
    150000.00, 'INR', 150000.00, 1.0, 'Orion Logistics', 'OL-44510', 'HDFC Bank', 'IN', 'ONLINE',
    'Vendor payment', '2026-09-11 09:20:00+05:30'),
('TXN_100008', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'DEBIT', 'WITHDRAWAL',
    100000.00, 'INR', 100000.00, 1.0, 'Cash Counter', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Cash withdrawal', '2026-09-11 17:45:00+05:30');
-- total debits within 48h = 400000 = 80% of 500000 credit


-- ----------------------------------------------------------------------------
-- TYPOLOGY 3: HIGH_RISK_JURISDICTION
-- Rule HIGH_RISK_JURISDICTION: counterparty_country in ('IR','KP','SY','MM','AF','YE','AE').
-- Spread across several accounts/customers.
-- ----------------------------------------------------------------------------
INSERT INTO txn (txn_ref, account_id, direction, txn_type, amount, currency, amount_base,
                  exchange_rate, counterparty_name, counterparty_account, counterparty_bank,
                  counterparty_country, channel, description, txn_timestamp)
VALUES
('TXN_100009', (SELECT id FROM account WHERE account_ref = 'ACC_100008'), 'DEBIT', 'WIRE',
    250000.00, 'INR', 250000.00, 1.0, 'Al Rashid Trading', 'ART-3092', 'Emirates NBD', 'AE', 'ONLINE',
    'Outgoing wire - goods payment', '2026-09-12 11:15:00+05:30'),
('TXN_100010', (SELECT id FROM account WHERE account_ref = 'ACC_100012'), 'DEBIT', 'WIRE',
    180000.00, 'INR', 180000.00, 1.0, 'Yerevan Metals Co', 'YMC-1187', 'Central Bank', 'MM', 'ONLINE',
    'Outgoing wire - commodity purchase', '2026-09-08 14:00:00+05:30'),
('TXN_100011', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'CREDIT', 'WIRE',
    95000.00, 'INR', 95000.00, 1.0, 'Kabul Traders Union', 'KTU-2201', 'Afghan United Bank', 'AF', 'ONLINE',
    'Incoming wire', '2026-09-05 16:30:00+05:30'),
('TXN_100012', (SELECT id FROM account WHERE account_ref = 'ACC_100005'), 'DEBIT', 'WIRE',
    310000.00, 'INR', 310000.00, 1.0, 'Damascus Holdings', 'DH-7743', 'Syria National Bank', 'SY', 'ONLINE',
    'Outgoing wire - investment', '2026-09-13 12:00:00+05:30'),
('TXN_100013', (SELECT id FROM account WHERE account_ref = 'ACC_100011'), 'DEBIT', 'WIRE',
    420000.00, 'INR', 420000.00, 1.0, 'Sanaa General Trading', 'SGT-5561', 'Yemen Commercial Bank', 'YE', 'ONLINE',
    'Outgoing wire', '2026-09-09 10:45:00+05:30');


-- ----------------------------------------------------------------------------
-- TYPOLOGY 4: ROUND_NUMBER
-- Rule ROUND_NUMBER: >=3 txns, exact multiples of 10000, >=10000, within 168h (7 days).
-- Account ACC_100004 (Rohan Verma, CURRENT, PEP, HIGH risk).
-- ----------------------------------------------------------------------------
INSERT INTO txn (txn_ref, account_id, direction, txn_type, amount, currency, amount_base,
                  exchange_rate, counterparty_name, counterparty_account, counterparty_bank,
                  counterparty_country, channel, description, txn_timestamp)
VALUES
('TXN_100014', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'DEBIT', 'TRANSFER',
    50000.00, 'INR', 50000.00, 1.0, 'Horizon Consultants', 'HC-9021', 'Axis Bank', 'IN', 'ONLINE',
    'Consulting fee', '2026-09-01 10:00:00+05:30'),
('TXN_100015', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'DEBIT', 'TRANSFER',
    60000.00, 'INR', 60000.00, 1.0, 'Horizon Consultants', 'HC-9021', 'Axis Bank', 'IN', 'ONLINE',
    'Consulting fee', '2026-09-03 11:00:00+05:30'),
('TXN_100016', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'DEBIT', 'TRANSFER',
    40000.00, 'INR', 40000.00, 1.0, 'Horizon Consultants', 'HC-9021', 'Axis Bank', 'IN', 'ONLINE',
    'Consulting fee', '2026-09-05 09:30:00+05:30'),
('TXN_100017', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'DEBIT', 'TRANSFER',
    70000.00, 'INR', 70000.00, 1.0, 'Horizon Consultants', 'HC-9021', 'Axis Bank', 'IN', 'ONLINE',
    'Consulting fee', '2026-09-07 14:15:00+05:30');


-- ----------------------------------------------------------------------------
-- TYPOLOGY 5: CTR_THRESHOLD
-- Rule CTR_THRESHOLD: single txn with amountBase >= 10000.
-- Several standalone large transactions across different accounts/customers.
-- ----------------------------------------------------------------------------
INSERT INTO txn (txn_ref, account_id, direction, txn_type, amount, currency, amount_base,
                  exchange_rate, counterparty_name, counterparty_account, counterparty_bank,
                  counterparty_country, channel, description, txn_timestamp)
VALUES
('TXN_100018', (SELECT id FROM account WHERE account_ref = 'ACC_100002'), 'CREDIT', 'DEPOSIT',
    15000.00, 'INR', 15000.00, 1.0, 'Employer Payroll', NULL, 'Sentinel Bank', 'IN', 'ONLINE',
    'Bonus payment', '2026-08-20 10:00:00+05:30'),
('TXN_100019', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'CREDIT', 'DEPOSIT',
    22000.00, 'INR', 22000.00, 1.0, 'Insurance Payout', NULL, 'LIC', 'IN', 'ONLINE',
    'Insurance maturity payout', '2026-08-25 11:30:00+05:30'),
('TXN_100020', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'DEBIT', 'PAYMENT',
    13500.00, 'INR', 13500.00, 1.0, 'City Furnishings', NULL, 'SBI', 'IN', 'CARD',
    'Furniture purchase', '2026-08-28 17:00:00+05:30'),
('TXN_100021', (SELECT id FROM account WHERE account_ref = 'ACC_100006'), 'CREDIT', 'TRANSFER',
    18000.00, 'INR', 18000.00, 1.0, 'Freelance Client', NULL, 'Kotak Bank', 'IN', 'ONLINE',
    'Project payment', '2026-09-02 13:20:00+05:30'),
('TXN_100022', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'WITHDRAWAL',
    11000.00, 'INR', 11000.00, 1.0, 'Cash Counter', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Cash withdrawal', '2026-09-06 12:10:00+05:30');


-- ----------------------------------------------------------------------------
-- ORDINARY / NON-SUSPICIOUS TRANSACTIONS
-- Everyday salary credits, bill payments, small transfers, groceries, EMIs.
-- Spread across ~120 days (2026-05-22 through 2026-09-19) to build behavioural
-- baselines and keep the dataset realistic (not every customer looks criminal).
-- ----------------------------------------------------------------------------
INSERT INTO txn (txn_ref, account_id, direction, txn_type, amount, currency, amount_base,
                  exchange_rate, counterparty_name, counterparty_account, counterparty_bank,
                  counterparty_country, channel, description, txn_timestamp)
VALUES
('TXN_100023', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'CREDIT', 'DEPOSIT',
    145000.00, 'INR', 145000.00, 1.0, 'Acme Software Pvt Ltd', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-05-30 09:00:00+05:30'),
('TXN_100024', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'PAYMENT',
    3200.00, 'INR', 3200.00, 1.0, 'BESCOM', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Electricity bill', '2026-06-02 18:30:00+05:30'),
('TXN_100025', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'PAYMENT',
    1850.00, 'INR', 1850.00, 1.0, 'Airtel', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Mobile & broadband bill', '2026-06-05 20:15:00+05:30'),
('TXN_100026', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'CREDIT', 'DEPOSIT',
    145000.00, 'INR', 145000.00, 1.0, 'Acme Software Pvt Ltd', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-06-30 09:00:00+05:30'),
('TXN_100027', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'PAYMENT',
    5600.00, 'INR', 5600.00, 1.0, 'BigBasket', NULL, 'HDFC Bank', 'IN', 'UPI',
    'Groceries', '2026-07-04 19:00:00+05:30'),
('TXN_100028', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'CREDIT', 'DEPOSIT',
    145000.00, 'INR', 145000.00, 1.0, 'Acme Software Pvt Ltd', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-07-31 09:00:00+05:30'),
('TXN_100029', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'PAYMENT',
    2100.00, 'INR', 2100.00, 1.0, 'Swiggy', NULL, 'HDFC Bank', 'IN', 'UPI',
    'Food delivery', '2026-08-10 21:00:00+05:30'),
('TXN_100030', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'CREDIT', 'DEPOSIT',
    145000.00, 'INR', 145000.00, 1.0, 'Acme Software Pvt Ltd', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-08-31 09:00:00+05:30'),
('TXN_100031', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'PAYMENT',
    4200.00, 'INR', 4200.00, 1.0, 'Amazon', NULL, 'HDFC Bank', 'IN', 'CARD',
    'Online shopping', '2026-09-14 15:30:00+05:30'),

('TXN_100032', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'CREDIT', 'DEPOSIT',
    95000.00, 'INR', 95000.00, 1.0, 'City Hospital', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-06-01 09:00:00+05:30'),
('TXN_100033', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'DEBIT', 'PAYMENT',
    12500.00, 'INR', 12500.00, 1.0, 'HDFC Life', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Insurance premium', '2026-06-05 10:00:00+05:30'),
('TXN_100034', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'CREDIT', 'DEPOSIT',
    95000.00, 'INR', 95000.00, 1.0, 'City Hospital', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-07-01 09:00:00+05:30'),
('TXN_100035', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'DEBIT', 'PAYMENT',
    3400.00, 'INR', 3400.00, 1.0, 'Reliance Digital', NULL, 'ICICI Bank', 'IN', 'CARD',
    'Electronics purchase', '2026-07-18 16:45:00+05:30'),
('TXN_100036', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'CREDIT', 'DEPOSIT',
    95000.00, 'INR', 95000.00, 1.0, 'City Hospital', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-08-01 09:00:00+05:30'),
('TXN_100037', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'DEBIT', 'PAYMENT',
    2200.00, 'INR', 2200.00, 1.0, 'Zomato', NULL, 'ICICI Bank', 'IN', 'UPI',
    'Food delivery', '2026-08-22 20:00:00+05:30'),
('TXN_100038', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'CREDIT', 'DEPOSIT',
    95000.00, 'INR', 95000.00, 1.0, 'City Hospital', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-09-01 09:00:00+05:30'),

('TXN_100039', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'CREDIT', 'DEPOSIT',
    68000.00, 'INR', 68000.00, 1.0, 'Freelance Client Payments', NULL, 'SBI', 'IN', 'ONLINE',
    'Consulting income', '2026-06-10 09:00:00+05:30'),
('TXN_100040', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'DEBIT', 'PAYMENT',
    9800.00, 'INR', 9800.00, 1.0, 'Landlord', NULL, 'SBI', 'IN', 'UPI',
    'House rent', '2026-06-12 09:00:00+05:30'),
('TXN_100041', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'CREDIT', 'DEPOSIT',
    72000.00, 'INR', 72000.00, 1.0, 'Freelance Client Payments', NULL, 'SBI', 'IN', 'ONLINE',
    'Consulting income', '2026-07-10 09:00:00+05:30'),
('TXN_100042', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'DEBIT', 'PAYMENT',
    9800.00, 'INR', 9800.00, 1.0, 'Landlord', NULL, 'SBI', 'IN', 'UPI',
    'House rent', '2026-07-12 09:00:00+05:30'),
('TXN_100043', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'CREDIT', 'DEPOSIT',
    65000.00, 'INR', 65000.00, 1.0, 'Freelance Client Payments', NULL, 'SBI', 'IN', 'ONLINE',
    'Consulting income', '2026-08-10 09:00:00+05:30'),
('TXN_100044', (SELECT id FROM account WHERE account_ref = 'ACC_100010'), 'DEBIT', 'PAYMENT',
    9800.00, 'INR', 9800.00, 1.0, 'Landlord', NULL, 'SBI', 'IN', 'UPI',
    'House rent', '2026-08-12 09:00:00+05:30'),

('TXN_100045', (SELECT id FROM account WHERE account_ref = 'ACC_100002'), 'CREDIT', 'TRANSFER',
    30000.00, 'INR', 30000.00, 1.0, 'Aarav Sharma - ACC_100001', NULL, 'Sentinel Bank', 'IN', 'ONLINE',
    'Self transfer from current account', '2026-06-15 10:00:00+05:30'),
('TXN_100046', (SELECT id FROM account WHERE account_ref = 'ACC_100002'), 'DEBIT', 'PAYMENT',
    4500.00, 'INR', 4500.00, 1.0, 'Tata Power', NULL, 'Sentinel Bank', 'IN', 'ONLINE',
    'Electricity bill', '2026-06-20 18:00:00+05:30'),
('TXN_100047', (SELECT id FROM account WHERE account_ref = 'ACC_100002'), 'CREDIT', 'TRANSFER',
    30000.00, 'INR', 30000.00, 1.0, 'Aarav Sharma - ACC_100001', NULL, 'Sentinel Bank', 'IN', 'ONLINE',
    'Self transfer from current account', '2026-07-15 10:00:00+05:30'),
('TXN_100048', (SELECT id FROM account WHERE account_ref = 'ACC_100002'), 'DEBIT', 'PAYMENT',
    3900.00, 'INR', 3900.00, 1.0, 'Jio Fiber', NULL, 'Sentinel Bank', 'IN', 'ONLINE',
    'Broadband bill', '2026-07-20 18:00:00+05:30'),

('TXN_100049', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'CREDIT', 'DEPOSIT',
    620000.00, 'INR', 620000.00, 1.0, 'Retail Customers', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Daily sales deposit', '2026-06-08 18:00:00+05:30'),
('TXN_100050', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'DEBIT', 'PAYMENT',
    210000.00, 'INR', 210000.00, 1.0, 'Wholesale Supplier Co', NULL, 'Axis Bank', 'IN', 'ONLINE',
    'Inventory purchase', '2026-06-10 11:00:00+05:30'),
('TXN_100051', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'CREDIT', 'DEPOSIT',
    580000.00, 'INR', 580000.00, 1.0, 'Retail Customers', NULL, 'Sentinel Bank', 'IN', 'BRANCH',
    'Daily sales deposit', '2026-07-08 18:00:00+05:30'),
('TXN_100052', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'DEBIT', 'PAYMENT',
    198000.00, 'INR', 198000.00, 1.0, 'Wholesale Supplier Co', NULL, 'Axis Bank', 'IN', 'ONLINE',
    'Inventory purchase', '2026-07-10 11:00:00+05:30'),
('TXN_100053', (SELECT id FROM account WHERE account_ref = 'ACC_100001'), 'DEBIT', 'PAYMENT',
    35000.00, 'INR', 35000.00, 1.0, 'Staff Payroll', NULL, 'Sentinel Bank', 'IN', 'ONLINE',
    'Staff salaries', '2026-08-01 09:00:00+05:30'),

('TXN_100054', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'CREDIT', 'DEPOSIT',
    380000.00, 'INR', 380000.00, 1.0, 'Government Honorarium', NULL, 'Axis Bank', 'IN', 'ONLINE',
    'Official honorarium', '2026-06-18 09:00:00+05:30'),
('TXN_100055', (SELECT id FROM account WHERE account_ref = 'ACC_100004'), 'DEBIT', 'PAYMENT',
    45000.00, 'INR', 45000.00, 1.0, 'Office Utilities', NULL, 'Axis Bank', 'IN', 'ONLINE',
    'Office maintenance', '2026-06-22 14:00:00+05:30'),
('TXN_100056', (SELECT id FROM account WHERE account_ref = 'ACC_100005'), 'CREDIT', 'DEPOSIT',
    150000.00, 'INR', 150000.00, 1.0, 'NRE Remittance', NULL, 'Axis Bank', 'AE', 'ONLINE',
    'Family remittance', '2026-07-25 10:00:00+05:30'),

('TXN_100057', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'CREDIT', 'DEPOSIT',
    320000.00, 'INR', 320000.00, 1.0, 'Domestic Buyer Pvt Ltd', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Trade receivable', '2026-06-25 10:00:00+05:30'),
('TXN_100058', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'DEBIT', 'PAYMENT',
    85000.00, 'INR', 85000.00, 1.0, 'Shipping Co', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Freight charges', '2026-06-27 15:00:00+05:30'),
('TXN_100059', (SELECT id FROM account WHERE account_ref = 'ACC_100007'), 'CREDIT', 'DEPOSIT',
    290000.00, 'INR', 290000.00, 1.0, 'Domestic Buyer Pvt Ltd', NULL, 'HDFC Bank', 'IN', 'ONLINE',
    'Trade receivable', '2026-08-05 10:00:00+05:30'),

('TXN_100060', (SELECT id FROM account WHERE account_ref = 'ACC_100006'), 'CREDIT', 'DEPOSIT',
    210000.00, 'INR', 210000.00, 1.0, 'Ad Agency Pvt Ltd', NULL, 'Kotak Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-06-30 09:00:00+05:30'),
('TXN_100061', (SELECT id FROM account WHERE account_ref = 'ACC_100006'), 'DEBIT', 'PAYMENT',
    38000.00, 'INR', 38000.00, 1.0, 'Home Loan EMI', NULL, 'Kotak Bank', 'IN', 'ONLINE',
    'EMI payment', '2026-07-03 06:00:00+05:30'),
('TXN_100062', (SELECT id FROM account WHERE account_ref = 'ACC_100006'), 'CREDIT', 'DEPOSIT',
    210000.00, 'INR', 210000.00, 1.0, 'Ad Agency Pvt Ltd', NULL, 'Kotak Bank', 'IN', 'ONLINE',
    'Monthly salary', '2026-08-31 09:00:00+05:30'),
('TXN_100063', (SELECT id FROM account WHERE account_ref = 'ACC_100006'), 'DEBIT', 'PAYMENT',
    38000.00, 'INR', 38000.00, 1.0, 'Home Loan EMI', NULL, 'Kotak Bank', 'IN', 'ONLINE',
    'EMI payment', '2026-09-03 06:00:00+05:30'),

('TXN_100064', (SELECT id FROM account WHERE account_ref = 'ACC_100011'), 'CREDIT', 'DEPOSIT',
    50000.00, 'INR', 50000.00, 1.0, 'Interest Credit', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Quarterly interest credit', '2026-06-30 00:00:00+05:30'),
('TXN_100065', (SELECT id FROM account WHERE account_ref = 'ACC_100012'), 'CREDIT', 'DEPOSIT',
    75000.00, 'INR', 75000.00, 1.0, 'Interest Credit', NULL, 'ICICI Bank', 'IN', 'ONLINE',
    'Quarterly interest credit', '2026-06-30 00:00:00+05:30'),

('TXN_100066', (SELECT id FROM account WHERE account_ref = 'ACC_100003'), 'DEBIT', 'PAYMENT',
    1200.00, 'INR', 1200.00, 1.0, 'Netflix', NULL, 'HDFC Bank', 'IN', 'CARD',
    'Subscription', '2026-09-16 08:00:00+05:30'),
('TXN_100067', (SELECT id FROM account WHERE account_ref = 'ACC_100009'), 'DEBIT', 'PAYMENT',
    2800.00, 'INR', 2800.00, 1.0, 'Apollo Pharmacy', NULL, 'ICICI Bank', 'IN', 'CARD',
    'Medicines', '2026-09-17 19:30:00+05:30');
