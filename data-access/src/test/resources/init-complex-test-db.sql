-- Test SQL dla złożonych struktur danych PostgreSQL
-- Tworzy przykładowe dane do testowania konwerterów

-- 1. Najpierw tworzymy typy enum
CREATE TYPE test_status AS ENUM ('active', 'inactive', 'pending', 'not_started');
CREATE TYPE test_priority AS ENUM ('low', 'medium', 'high', 'critical');
CREATE TYPE test_category AS ENUM ('bug_fix', 'feature', 'enhancement', 'documentation');

-- 2. Tworzymy typy kompozytowe
CREATE TYPE test_metadata AS (
    created_at timestamp,
    updated_at timestamp,
    version integer,
    tags text[]
);

CREATE TYPE test_person AS (
    name text,
    age integer,
    email text,
    active boolean,
    roles text[]
);

CREATE TYPE test_task AS (
    id integer,
    title text,
    description text,
    status test_status,
    priority test_priority,
    category test_category,
    assignee test_person,
    metadata test_metadata,
    subtasks text[],
    estimated_hours numeric
);

CREATE TYPE test_project AS (
    name text,
    description text,
    status test_status,
    team_members test_person[],
    tasks test_task[],
    metadata test_metadata,
    budget numeric
);

-- 3. Tworzymy tabelę testową
CREATE TABLE complex_test_data (
    id SERIAL PRIMARY KEY,
    
    -- Proste typy
    simple_text text,
    simple_number integer,
    simple_bool boolean,
    simple_json jsonb,
    simple_uuid uuid,
    simple_date date,
    simple_timestamp timestamp,
    
    -- Enumy
    single_status test_status,
    status_array test_status[],
    
    -- Tablice prostych typów
    text_array text[],
    number_array integer[],
    nested_text_array text[][],
    
    -- Kompozyty
    single_person test_person,
    person_array test_person[],
    
    -- Złożone struktury
    project_data test_project,
    project_array test_project[]
);

-- 4. Wstawiamy testowe dane z wszystkimi możliwymi kombinacjami
INSERT INTO complex_test_data (
    simple_text,
    simple_number,
    simple_bool,
    simple_json,
    simple_uuid,
    simple_date,
    simple_timestamp,
    
    single_status,
    status_array,
    
    text_array,
    number_array,
    nested_text_array,
    
    single_person,
    person_array,
    
    project_data,
    project_array
) VALUES (
    -- Proste typy
    'Test "quoted" text with special chars: ąćęłńóśźż',
    42,
    true,
    '{"name": "test", "value": 123, "nested": {"key": "value"}, "array": [1, 2, 3]}',
    gen_random_uuid(),
    '2024-01-15',
    '2024-01-15 14:30:00',

    -- Enumy
    'active',
    ARRAY['active', 'pending', 'not_started']::test_status[],

    -- Tablice prostych typów
    ARRAY['first', 'second', 'third with "quotes"', 'fourth with ąćę'],
    ARRAY[1, 2, 3, 4, 5],
    ARRAY[ARRAY['a', 'b'], ARRAY['c', 'd'], ARRAY['e with "quotes"', 'f']],

    -- Kompozyty
    ROW(
        'John "The Developer" Doe',
        30,
        'john@example.com',
        true,
        ARRAY['admin', 'developer', 'team-lead']
    )::test_person,

    ARRAY[
        ROW('Alice Smith', 25, 'alice@example.com', true, ARRAY['developer', 'frontend']),
        ROW('Bob "Database" Johnson', 35, 'bob@example.com', false, ARRAY['dba', 'backend']),
        ROW('Carol "The Tester" Williams', 28, 'carol@example.com', true, ARRAY['qa', 'automation'])
    ]::test_person[],

    -- Mega złożony projekt
    ROW(
        'Complex "Enterprise" Project',
        'A very complex project with all possible data types and "special characters"',
        'active',
        -- team_members (tablica kompozytów)
        ARRAY[
            ROW('Project Manager', 40, 'pm@example.com', true, ARRAY['manager', 'stakeholder']),
            ROW('Senior Dev "The Architect"', 35, 'senior@example.com', true, ARRAY['architect', 'senior-dev']),
            ROW('Junior Dev', 24, 'junior@example.com', true, ARRAY['junior-dev', 'learner']),
            ROW(NULL, 30, '', true, ARRAY['user'])::test_person
        ]::test_person[],
        -- tasks (tablica kompozytów z enumami i innymi kompozytami)
        ARRAY[
            ROW(
                1,
                'Setup "Development" Environment',
                'Configure all development tools and "databases"',
                'active',
                'high',
                'enhancement',
                ROW('DevOps Guy', 32, 'devops@example.com', true, ARRAY['devops', 'infrastructure']),
                ROW('2024-01-01 09:00:00', '2024-01-15 14:30:00', 1, ARRAY['setup', 'infrastructure', 'priority']),
                ARRAY['install docker', 'setup database', 'configure "CI/CD"'],
                16.5
            ),
            ROW(
                2,
                'Implement "Core" Features',
                'Build the main functionality with proper "error handling"',
                'pending',
                'critical',
                'feature',
                ROW('Lead Developer', 38, 'lead@example.com', true, ARRAY['lead', 'full-stack']),
                ROW('2024-01-10 10:00:00', '2024-01-20 16:00:00', 2, ARRAY['core', 'critical', 'feature']),
                ARRAY['design API', 'implement "business logic"', 'add tests', 'write "documentation"'],
                40.0
            )
        ]::test_task[],
        -- metadata
        ROW(
            '2024-01-01 08:00:00',
            '2024-01-15 14:30:00',
            3,
            ARRAY['enterprise', 'complex', 'multi-team', 'high-priority']
        ),
        150000.50
    )::test_project,
    
    -- Tablica projektów
    ARRAY[
        ROW(
            'Small "Maintenance" Project',
            'Quick fixes and "minor improvements"',
            'not_started',
            ARRAY[
                ROW('Maintainer', 29, 'maintainer@example.com', true, ARRAY['maintainer'])
            ]::test_person[],
            ARRAY[
                ROW(
                    10,
                    'Fix "Critical" Bug',
                    'Resolve the issue with "data corruption"',
                    'pending',
                    'critical',
                    'bug_fix',
                    ROW('Bug Hunter', 31, 'bughunter@example.com', true, ARRAY['debugger']),
                    ROW('2024-01-20 09:00:00', '2024-01-20 09:00:00', 1, ARRAY['bugfix', 'urgent']),
                    ARRAY['investigate issue', 'fix "root cause"', 'test solution'],
                    8.0
                )
            ]::test_task[],
            ROW('2024-01-20 09:00:00', '2024-01-20 09:00:00', 1, ARRAY['maintenance', 'bugfix']),
            5000.00
        ),
        ROW(
            'Research "Innovation" Project',
            'Experimental features and "new technologies"',
            'active',
            ARRAY[
                ROW('Researcher "The Innovator"', 33, 'research@example.com', true, ARRAY['researcher', 'innovator']),
                ROW('Data Scientist', 27, 'data@example.com', true, ARRAY['data-science', 'ml'])
            ]::test_person[],
            ARRAY[
                ROW(
                    20,
                    'Prototype "AI" Integration',
                    'Build proof of concept for "machine learning" features',
                    'active',
                    'medium',
                    'feature',
                    ROW('AI Specialist', 30, 'ai@example.com', true, ARRAY['ai', 'ml', 'python']),
                    ROW('2024-01-05 10:00:00', '2024-01-15 15:00:00', 5, ARRAY['ai', 'prototype', 'experimental']),
                    ARRAY['research "algorithms"', 'build model', 'integrate "with backend"', 'test "accuracy"'],
                    60.0
                )
            ]::test_task[],
            ROW('2024-01-05 10:00:00', '2024-01-15 15:00:00', 5, ARRAY['research', 'innovation', 'ai']),
            75000.25
        )
    ]::test_project[]
);

-- 5. Zapytania testowe do wyciągnięcia danych
-- Możesz użyć tych zapytań w testach:

-- Proste zapytanie z pojedynczym parametrem enum
-- SELECT * FROM complex_test_data WHERE single_status = 'active';

-- Zapytanie z tablicą enumów
-- SELECT * FROM complex_test_data WHERE 'active' = ANY(status_array);

-- Zapytanie z kompozytem jako parametr
-- SELECT * FROM complex_test_data WHERE single_person = ROW('John "The Developer" Doe', 30, 'john@example.com', true, ARRAY['admin', 'developer', 'team-lead'])::test_person;

-- Złożone zapytanie z tablicą kompozytów
-- SELECT * FROM complex_test_data WHERE ROW('Alice Smith', 25, 'alice@example.com', true, ARRAY['developer', 'frontend'])::test_person = ANY(person_array);

-- Mega złożone zapytanie z projektem
-- SELECT * FROM complex_test_data WHERE (project_data).name LIKE '%Complex%';

-- 6. Przykładowe dane do kopiowania w testach:

/*
PRZYKŁADY WARTOŚCI DO UŻYCIA W TESTACH:

1. Enum:
'active'::test_status

2. Tablica enumów:
ARRAY['active', 'pending', 'not_started']::test_status[]

3. Kompozyt:
ROW('John "The Developer" Doe', 30, 'john@example.com', true, ARRAY['admin', 'developer'])::test_person

4. Tablica kompozytów:
ARRAY[
    ROW('Alice Smith', 25, 'alice@example.com', true, ARRAY['developer']),
    ROW('Bob Johnson', 35, 'bob@example.com', false, ARRAY['dba'])
]::test_person[]

5. Zagnieżdżona tablica tekstowa:
ARRAY[ARRAY['a', 'b'], ARRAY['c with "quotes"', 'd']]

6. JSON:
'{"name": "test", "nested": {"key": "value"}, "array": [1, 2, 3]}'::jsonb

7. Mega złożony projekt (do kopiowania):
ROW(
    'Test Project',
    'Description with "quotes"',
    'active',
    ARRAY[ROW('Developer', 30, 'dev@test.com', true, ARRAY['dev'])]::test_person[],
    ARRAY[ROW(1, 'Task', 'Description', 'active', 'high', 'feature', 
              ROW('Assignee', 25, 'test@test.com', true, ARRAY['role']), 
              ROW('2024-01-01 00:00:00', '2024-01-01 00:00:00', 1, ARRAY['tag']), 
              ARRAY['subtask'], 8.0)]::test_task[],
    ROW('2024-01-01 00:00:00', '2024-01-01 00:00:00', 1, ARRAY['project']),
    10000.00
)::test_project
*/

-- 7. Sprawdzenie czy dane zostały poprawnie wstawione
SELECT 
    id,
    simple_text,
    single_status,
    array_length(status_array, 1) as status_array_length,
    (single_person).name as person_name,
    array_length(person_array, 1) as person_array_length,
    (project_data).name as project_name,
    array_length((project_data).team_members, 1) as team_size,
    array_length((project_data).tasks, 1) as task_count,
    array_length(project_array, 1) as project_array_length
FROM complex_test_data;