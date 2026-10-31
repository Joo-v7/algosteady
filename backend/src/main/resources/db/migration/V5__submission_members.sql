ALTER TABLE submission ADD COLUMN member_id UUID REFERENCES member_account (id);
ALTER TABLE execution_job ADD COLUMN member_id UUID REFERENCES member_account (id);

CREATE INDEX submission_member_history
    ON submission (member_id, exercise_id, language, submitted_at DESC, id DESC);
