-- @formatter:off because of terribly unreliable IDEA reformat for SQL
-- drop schema public CASCADE;
CREATE SCHEMA IF NOT EXISTS public;

DO $$
    BEGIN
        perform pg_get_functiondef('gen_random_uuid()'::regprocedure);
        raise notice 'gen_random_uuid already exists, skipping create EXTENSION pgcrypto';
    EXCEPTION WHEN undefined_function THEN
        create EXTENSION pgcrypto;
    END
$$;

-- JSONB

create table tjson (
    oid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    ext JSONB,

    CONSTRAINT tjson_oid_pk PRIMARY KEY (oid)
);

ALTER TABLE tjson ADD CONSTRAINT tjson_name_key UNIQUE (name);
CREATE INDEX tjson_ext_idx ON tjson USING gin (ext);

--- EAV

create table teav (
    oid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,

    CONSTRAINT teav_oid_pk PRIMARY KEY (oid)
);

ALTER TABLE teav ADD CONSTRAINT teav_name_key UNIQUE (name);

create table teav_ext_string (
    owner_oid UUID NOT NULL references teav(oid),
    key VARCHAR(32) NOT NULL,
    value VARCHAR(255) NOT NULL,

    CONSTRAINT teav_ext_string_pk PRIMARY KEY (owner_oid, key, value)
);

CREATE INDEX teav_ext_string_key_value_idx ON teav_ext_string (key, value);

-- INSERTS
DELETE FROM tjson;
DELETE FROM teav_ext_string;
DELETE FROM teav;

-- start with smaller batches 1..1000, 1001..100000, etc.
DO
$$
DECLARE
    hobbies VARCHAR[];
    v VARCHAR;
    id UUID;
BEGIN
    FOR r IN 11000001..15000000 LOOP

        IF r % 10 <= 1 THEN
            -- some entries have no extension
            INSERT INTO tjson (name) VALUES ('user-' || LPAD(r::text, 10, '0'));
            INSERT INTO teav (name) VALUES ('user-' || LPAD(r::text, 10, '0'));
        ELSEIF r % 10 <= 3 THEN
            -- email+eid (constant keys) + other-key-{r} (variable key)
            INSERT INTO tjson (name, ext) VALUES (
                'user-' || LPAD(r::text, 10, '0'),
                ('{"eid": ' || r || ', "email": "user' || r || '@mycompany.com", "other-key-' || r || '": "other-value-' || r || '"}')::jsonb
            );
            INSERT INTO teav (name) VALUES ('user-' || LPAD(r::text, 10, '0')) RETURNING oid INTO id;
            INSERT INTO teav_ext_string (owner_oid, key, value) VALUES (id, 'email', 'user' || r || '@mycompany.com');
            INSERT INTO teav_ext_string (owner_oid, key, value) VALUES (id, 'eid', r);
            INSERT INTO teav_ext_string (owner_oid, key, value) VALUES (id, 'other-key-' || r, 'other-value-' || r);
        ELSEIF r % 10 <= 4 THEN
            -- rarely used values of hobbies key
            hobbies := random_pick(ARRAY['recording', 'guitar', 'beer', 'rum', 'writing', 'coding', 'debugging', 'gaming', 'shopping', 'watching videos', 'sleeping', 'dreaming'], 0.1);
            -- JSONB
            INSERT INTO tjson (name, ext) VALUES (
                'user-' || LPAD(r::text, 10, '0'),
                ('{"eid": ' || r || ', "hobbies": '|| array_to_json(hobbies)::text || '}')::jsonb
            );

            -- EAV
            INSERT INTO teav (name) VALUES ('user-' || LPAD(r::text, 10, '0')) RETURNING oid INTO id;
            INSERT INTO teav_ext_string (owner_oid, key, value) VALUES (id, 'eid', r);
            FOREACH v IN ARRAY hobbies LOOP
                    INSERT INTO teav_ext_string (owner_oid, key, value)
                    VALUES (id, 'hobbies', v);
                END LOOP;
        ELSE
            -- these values are used by many entries
            hobbies := random_pick(ARRAY['eating', 'books', 'music', 'dancing', 'walking', 'jokes', 'video', 'photo'], 0.4);
            -- JSONB
            INSERT INTO tjson (name, ext) VALUES (
                'user-' || LPAD(r::text, 10, '0'),
                ('{"eid": ' || r || ', "hobbies": '|| array_to_json(hobbies)::text || '}')::jsonb
            );

            -- EAV
            INSERT INTO teav (name) VALUES ('user-' || LPAD(r::text, 10, '0')) RETURNING oid INTO id;
            INSERT INTO teav_ext_string (owner_oid, key, value) VALUES (id, 'eid', r);
            FOREACH v IN ARRAY hobbies LOOP
                INSERT INTO teav_ext_string (owner_oid, key, value)
                VALUES (id, 'hobbies', v);
            END LOOP;
        END IF;

        IF r % 1000 = 0 THEN
            COMMIT;
        END IF;
    END LOOP;
END $$;

-- SELECTS

select count(*) from tjson;
select count(*) from teav;
select count(*) from teav_ext_string;

select * from tjson;
select * from teav;
select * from teav_ext_string;

analyze teav;
analyze teav_ext_string;
analyze tjson;
-- cluster teav_ext_string using teav_ext_string_pk; -- 10-13min, not sure how useful this is

-- SHOW enable_seqscan;
-- SET enable_seqscan = on;


EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
select count(*)
-- select *
from tjson
where ext->>'email' LIKE 'user2%'
and ext ? 'email';

-- index experiments
analyze;
CREATE INDEX tjson_exttmp_idx ON tjson ((ext->>'email'));
DROP INDEX tjson_exttmp_idx;

-- BENCHMARK
-- counts
select count(*) from tjson;
-- low selectivity count
select count(*) from tjson where ext @> '{"hobbies":["video"]}';
-- high selectivity count (result is low)
select count(*) from tjson where ext @> '{"hobbies":["sleeping"]}';
select count(*) from tjson where ext->>'email' LIKE 'user2%';
select count(*) from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'email' and es.value LIKE 'user2%');

select count(*) from teav;
select count(*) from teav_ext_string;
select count(*) from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'video');
select count(*) from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'sleeping');

-- selects
select * from tjson limit 500;
select * from tjson where ext @> '{"hobbies":["video"]}' limit 500;
select * from tjson where ext @> '{"hobbies":["video"]}' order by oid limit 500;
select * from tjson where ext @> '{"hobbies":["video"]}' and oid>'fffe0000-0000-0000-0000-000000000000' order by oid limit 500;
select * from tjson where ext @> '{"hobbies":["sleeping"]}' limit 500;
select * from tjson where ext @> '{"hobbies":["sleeping"]}' order by oid limit 500;
select * from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'video') limit 500;
select * from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'video') order by t.oid limit 500;
select * from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'video') and t.oid>'fffe0000-0000-0000-0000-000000000000' order by t.oid limit 500;
select * from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'sleeping') limit 500;
-- EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
select * from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'hobbies' and es.value = 'sleeping') order by t.oid limit 500;

-- TODO
select * from tjson where ext->>'email' LIKE 'user2%' limit 500;
select * from teav t where exists (select from teav_ext_string es where es.owner_oid = t.oid and es.key = 'email' and es.value LIKE 'user2%')  limit 500;
