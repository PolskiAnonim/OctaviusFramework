-- Table: stores color per process name
CREATE TABLE activity_tracker.process_colors (
    process_name text PRIMARY KEY,
    color varchar(7) NOT NULL
);

-- Function: picks a color from a 20-color palette, cycling by current row count
CREATE OR REPLACE FUNCTION activity_tracker.assign_process_color()
RETURNS TRIGGER AS $$
DECLARE
    palette text[] := ARRAY[
        '#EF4444', '#F97316', '#F59E0B', '#84CC16', '#22C55E',
        '#14B8A6', '#06B6D4', '#0EA5E9', '#3B82F6', '#6366F1',
        '#8B5CF6', '#A855F7', '#D946EF', '#EC4899', '#F43F5E',
        '#0891B2', '#7C3AED', '#DC2626', '#059669', '#4F46E5'
    ];
    current_count int;
BEGIN
    SELECT COUNT(*) INTO current_count FROM activity_tracker.process_colors;
    INSERT INTO activity_tracker.process_colors (process_name, color)
    VALUES (NEW.process_name, palette[(current_count % 20) + 1])
    ON CONFLICT (process_name) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: fires on every INSERT to activity_log
CREATE TRIGGER trg_assign_process_color
    BEFORE INSERT ON activity_tracker.activity_log
    FOR EACH ROW
    EXECUTE FUNCTION activity_tracker.assign_process_color();
