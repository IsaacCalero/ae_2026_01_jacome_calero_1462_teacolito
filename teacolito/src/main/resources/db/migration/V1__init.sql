CREATE TABLE expense_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    invite_code VARCHAR(255) NOT NULL UNIQUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    closed BOOLEAN NOT NULL
);

CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES expense_groups (id),
    member_username VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_group_members_group_id ON group_members (group_id);

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES expense_groups (id),
    payer_username VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    spent_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_expenses_group_id ON expenses (group_id);

CREATE TABLE expense_shares (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses (id),
    debtor_username VARCHAR(255) NOT NULL,
    share_amount NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_expense_shares_expense_id ON expense_shares (expense_id);

CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES expense_groups (id),
    from_username VARCHAR(255) NOT NULL,
    to_username VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    settled_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_settlements_group_id ON settlements (group_id);
