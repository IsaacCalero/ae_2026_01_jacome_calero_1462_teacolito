ALTER TABLE expense_groups
    ADD CONSTRAINT uq_expense_groups_name_created_by UNIQUE (name, created_by);
