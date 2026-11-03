-- Expense payer must belong to the expense's group
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_payer_group_member
        FOREIGN KEY (group_id, paid_by)
            REFERENCES group_members (group_id, user_id);

-- Settlement payer must belong to the settlement's group
ALTER TABLE settlements
    ADD CONSTRAINT fk_settlements_payer_group_member
        FOREIGN KEY (group_id, paid_by)
            REFERENCES group_members (group_id, user_id);

-- Settlement receiver must belong to the settlement's group
ALTER TABLE settlements
    ADD CONSTRAINT fk_settlements_receiver_group_member
        FOREIGN KEY (group_id, paid_to)
            REFERENCES group_members (group_id, user_id);