-- Validate that every expense split participant
-- belongs to the expense's group.

CREATE OR REPLACE FUNCTION validate_expense_split_membership()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
expense_group_id BIGINT;
BEGIN
SELECT group_id
INTO expense_group_id
FROM expenses
WHERE id = NEW.expense_id;

IF expense_group_id IS NULL THEN
        RAISE EXCEPTION 'Expense does not exist';
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM group_members
        WHERE group_id = expense_group_id
          AND user_id = NEW.user_id
    ) THEN
        RAISE EXCEPTION
            'Expense split participant must belong to the expense group';
END IF;

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_expense_split_membership
    BEFORE INSERT OR UPDATE OF expense_id, user_id
                     ON expense_splits
                         FOR EACH ROW
                         EXECUTE FUNCTION validate_expense_split_membership();


-- Prevent removing a group member who participates
-- in an existing expense split.

CREATE OR REPLACE FUNCTION prevent_group_member_removal_with_splits()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM expense_splits es
        JOIN expenses e ON e.id = es.expense_id
        WHERE e.group_id = OLD.group_id
          AND es.user_id = OLD.user_id
    ) THEN
        RAISE EXCEPTION
            'Cannot remove a group member who participates in existing expense splits';
END IF;

RETURN OLD;
END;
$$;

CREATE TRIGGER trg_prevent_group_member_removal_with_splits
    BEFORE DELETE OR UPDATE OF group_id, user_id
                     ON group_members
                         FOR EACH ROW
                         EXECUTE FUNCTION prevent_group_member_removal_with_splits();