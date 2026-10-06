ALTER TABLE component ADD COLUMN project_id CHAR(36) NULL;

ALTER TABLE component
    ADD CONSTRAINT fk_component_project
        FOREIGN KEY (project_id) REFERENCES software_project (id);
