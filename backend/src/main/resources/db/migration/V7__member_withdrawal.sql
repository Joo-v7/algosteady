ALTER TABLE submission DROP CONSTRAINT submission_member_id_fkey;
ALTER TABLE submission ADD CONSTRAINT submission_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES member_account (id) ON DELETE CASCADE;

ALTER TABLE execution_job DROP CONSTRAINT execution_job_member_id_fkey;
ALTER TABLE execution_job ADD CONSTRAINT execution_job_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES member_account (id) ON DELETE CASCADE;

ALTER TABLE execution_test_result DROP CONSTRAINT execution_test_result_job_id_fkey;
ALTER TABLE execution_test_result ADD CONSTRAINT execution_test_result_job_id_fkey
    FOREIGN KEY (job_id) REFERENCES execution_job (id) ON DELETE CASCADE;

ALTER TABLE submission DROP CONSTRAINT submission_job_id_fkey;
ALTER TABLE submission ADD CONSTRAINT submission_job_id_fkey
    FOREIGN KEY (job_id) REFERENCES execution_job (id) ON DELETE CASCADE;

ALTER TABLE daily_learning_completion DROP CONSTRAINT daily_learning_completion_member_id_fkey;
ALTER TABLE daily_learning_completion ADD CONSTRAINT daily_learning_completion_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES member_account (id) ON DELETE CASCADE;

ALTER TABLE daily_learning_completion DROP CONSTRAINT daily_learning_completion_job_id_fkey;
ALTER TABLE daily_learning_completion ADD CONSTRAINT daily_learning_completion_job_id_fkey
    FOREIGN KEY (job_id) REFERENCES execution_job (id) ON DELETE CASCADE;
