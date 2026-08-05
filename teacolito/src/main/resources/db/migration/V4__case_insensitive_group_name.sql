ALTER TABLE expense_groups DROP CONSTRAINT uq_expense_groups_name_created_by;

CREATE UNIQUE INDEX uq_expense_groups_name_created_by_ci ON expense_groups (LOWER(name), created_by);
