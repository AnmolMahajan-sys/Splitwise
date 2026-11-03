-- Checks that an expense's split total equals its amount.
CREATE OR REPLACE FUNCTION check_expense_split_total(
    p_expense_id BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
expense_amount NUMERIC(12,2);
    split_total NUMERIC(12,2);
BEGIN
SELECT amount
INTO expense_amount
FROM expenses
WHERE id = p_expense_id;

-- The expense may have been deleted.
IF NOT FOUND THEN
        RETURN;
END IF;

SELECT COALESCE(SUM(share_amount), 0)
INTO split_total
FROM expense_splits
WHERE expense_id = p_expense_id;

IF split_total <> expense_amount THEN
        RAISE EXCEPTION
            'Expense % has amount % but split total %',
            p_expense_id, expense_amount, split_total;
END IF;
END;
$$;


-- Trigger function for expense changes.
CREATE OR REPLACE FUNCTION validate_expense_total_from_expense()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM check_expense_split_total(NEW.id);
RETURN NEW;
END;
$$;


-- Trigger function for split changes.
CREATE OR REPLACE FUNCTION validate_expense_total_from_split()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM check_expense_split_total(OLD.expense_id);

    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM check_expense_split_total(OLD.expense_id);

        IF NEW.expense_id IS DISTINCT FROM OLD.expense_id THEN
            PERFORM check_expense_split_total(NEW.expense_id);
END IF;

ELSE
        PERFORM check_expense_split_total(NEW.expense_id);
END IF;

RETURN NULL;
END;
$$;


-- Check expense amount after insert or update.
CREATE CONSTRAINT TRIGGER trg_check_expense_split_total
AFTER INSERT OR UPDATE
                           ON expenses
                           DEFERRABLE INITIALLY DEFERRED
                           FOR EACH ROW
                           EXECUTE FUNCTION validate_expense_total_from_expense();


-- Check split changes at transaction commit.
CREATE CONSTRAINT TRIGGER trg_check_split_total
AFTER INSERT OR UPDATE OR DELETE
                ON expense_splits
                    DEFERRABLE INITIALLY DEFERRED
                    FOR EACH ROW
                    EXECUTE FUNCTION validate_expense_total_from_split();