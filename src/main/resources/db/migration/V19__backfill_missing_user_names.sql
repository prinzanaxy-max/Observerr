-- Backfill display names for accounts created before signup collected first/last name.
-- Prefer email local-part (jane.doe → Jane / Doe); fall back to institutional id suffix.

UPDATE users
SET
    first_name = LEFT(
        INITCAP(
            COALESCE(
                NULLIF(
                    SPLIT_PART(
                        REGEXP_REPLACE(
                            REGEXP_REPLACE(SPLIT_PART(email, '@', 1), '[0-9]+', ' ', 'g'),
                            '[._+\-]+',
                            ' ',
                            'g'
                        ),
                        ' ',
                        1
                    ),
                    ''
                ),
                'Student'
            )
        ),
        50
    ),
    last_name = LEFT(
        INITCAP(
            COALESCE(
                NULLIF(
                    NULLIF(
                        TRIM(
                            SUBSTRING(
                                REGEXP_REPLACE(
                                    REGEXP_REPLACE(SPLIT_PART(email, '@', 1), '[0-9]+', ' ', 'g'),
                                    '[._+\-]+',
                                    ' ',
                                    'g'
                                )
                                FROM LENGTH(
                                    SPLIT_PART(
                                        REGEXP_REPLACE(
                                            REGEXP_REPLACE(SPLIT_PART(email, '@', 1), '[0-9]+', ' ', 'g'),
                                            '[._+\-]+',
                                            ' ',
                                            'g'
                                        ),
                                        ' ',
                                        1
                                    )
                                ) + 1
                            )
                        ),
                        ''
                    ),
                    ''
                ),
                NULLIF(REGEXP_REPLACE(institutional_id, '^(STU|LEC)-', '', 'i'), ''),
                'User'
            )
        ),
        50
    )
WHERE (first_name IS NULL OR BTRIM(first_name) = '')
   OR (last_name IS NULL OR BTRIM(last_name) = '');
