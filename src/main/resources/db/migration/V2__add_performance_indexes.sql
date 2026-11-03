CREATE INDEX idx_group_members_user_id
    ON group_members(user_id);

CREATE INDEX idx_expenses_paid_by
    ON expenses(paid_by);

CREATE INDEX idx_settlements_paid_by
    ON settlements(paid_by);

CREATE INDEX idx_settlements_paid_to
    ON settlements(paid_to);