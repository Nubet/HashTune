ALTER TABLE recognitions ADD COLUMN recording_object_key VARCHAR(1024);
ALTER TABLE recognitions ADD COLUMN recording_content_type VARCHAR(255);
ALTER TABLE recognitions ADD COLUMN recording_file_name VARCHAR(255);
