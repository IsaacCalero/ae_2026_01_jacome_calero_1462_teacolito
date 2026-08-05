ALTER TABLE group_members DROP CONSTRAINT group_members_group_id_fkey;
ALTER TABLE group_members
    ADD CONSTRAINT group_members_group_id_fkey FOREIGN KEY (group_id) REFERENCES expense_groups (id) ON DELETE CASCADE;

ALTER TABLE expenses DROP CONSTRAINT expenses_group_id_fkey;
ALTER TABLE expenses
    ADD CONSTRAINT expenses_group_id_fkey FOREIGN KEY (group_id) REFERENCES expense_groups (id) ON DELETE CASCADE;

ALTER TABLE settlements DROP CONSTRAINT settlements_group_id_fkey;
ALTER TABLE settlements
    ADD CONSTRAINT settlements_group_id_fkey FOREIGN KEY (group_id) REFERENCES expense_groups (id) ON DELETE CASCADE;

ALTER TABLE expense_shares DROP CONSTRAINT expense_shares_expense_id_fkey;
ALTER TABLE expense_shares
    ADD CONSTRAINT expense_shares_expense_id_fkey FOREIGN KEY (expense_id) REFERENCES expenses (id) ON DELETE CASCADE;
