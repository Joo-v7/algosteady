ALTER TABLE execution_job ADD COLUMN kind VARCHAR(255) NOT NULL DEFAULT 'RUN';
ALTER TABLE execution_job ADD CONSTRAINT execution_job_kind CHECK (kind IN ('RUN', 'GRADING'));
ALTER TABLE execution_job ALTER COLUMN example_id DROP NOT NULL;
ALTER TABLE execution_job ADD CONSTRAINT execution_job_example CHECK (
    (kind = 'RUN' AND example_id IS NOT NULL) OR (kind = 'GRADING' AND example_id IS NULL)
);

CREATE TABLE submission (
    id UUID PRIMARY KEY
  , exercise_id VARCHAR(255) NOT NULL
  , content_version VARCHAR(255) NOT NULL
  , language VARCHAR(255) NOT NULL
  , code TEXT NOT NULL
  , reference_viewed BOOLEAN NOT NULL
  , job_id UUID NOT NULL UNIQUE REFERENCES execution_job (id)
  , submitted_at TIMESTAMP WITH TIME ZONE NOT NULL
  , FOREIGN KEY (exercise_id, content_version) REFERENCES exercise_content (exercise_id, content_version)
);
