CREATE TABLE IF NOT EXISTS lecturer_analytics_overviews (
    id                              BIGSERIAL PRIMARY KEY,
    lecturer_id                     BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    period                          VARCHAR(10)    NOT NULL,
    total_exams_monitored           INT            NOT NULL,
    exams_change_percent            NUMERIC(5, 2),
    exams_change_direction          VARCHAR(10)    NOT NULL,
    exams_change_label              VARCHAR(50)    NOT NULL,
    total_flagged_events            INT            NOT NULL,
    flags_change_percent            NUMERIC(5, 2),
    flags_change_direction          VARCHAR(10)    NOT NULL,
    flags_change_label              VARCHAR(50)    NOT NULL,
    avg_integrity_score             NUMERIC(5, 2)  NOT NULL,
    integrity_change_percent        NUMERIC(5, 2),
    integrity_change_direction      VARCHAR(10)    NOT NULL,
    integrity_change_label          VARCHAR(50)    NOT NULL,
    most_common_flag_label          VARCHAR(100)   NOT NULL,
    most_common_flag_share_percent  INT            NOT NULL,
    most_common_flag_icon           VARCHAR(50)    NOT NULL DEFAULT 'visibility_off',
    trend_granularity               VARCHAR(10)    NOT NULL,
    trend_subtitle                  VARCHAR(255)   NOT NULL,
    created_at                      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_lecturer_analytics_period UNIQUE (lecturer_id, period)
);

CREATE INDEX IF NOT EXISTS idx_lecturer_analytics_overviews_lecturer
    ON lecturer_analytics_overviews (lecturer_id);

CREATE TABLE IF NOT EXISTS lecturer_analytics_trend_points (
    id                  BIGSERIAL PRIMARY KEY,
    overview_id         BIGINT      NOT NULL REFERENCES lecturer_analytics_overviews (id) ON DELETE CASCADE,
    label               VARCHAR(10) NOT NULL,
    sort_order          INT         NOT NULL,
    monitored_sessions  INT         NOT NULL,
    flagged_events      INT         NOT NULL,
    alert               BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_lecturer_analytics_trend_overview
    ON lecturer_analytics_trend_points (overview_id, sort_order);

CREATE TABLE IF NOT EXISTS lecturer_analytics_behaviors (
    id              BIGSERIAL PRIMARY KEY,
    overview_id     BIGINT       NOT NULL REFERENCES lecturer_analytics_overviews (id) ON DELETE CASCADE,
    behavior_code   VARCHAR(50)  NOT NULL,
    label           VARCHAR(100) NOT NULL,
    event_count     INT          NOT NULL,
    icon            VARCHAR(50)  NOT NULL,
    tone            VARCHAR(20)  NOT NULL,
    sort_order      INT          NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_lecturer_analytics_behaviors_overview
    ON lecturer_analytics_behaviors (overview_id, sort_order);

-- Seed analytics for every lecturer account (matches UI mockups)

INSERT INTO lecturer_analytics_overviews (
    lecturer_id, period,
    total_exams_monitored, exams_change_percent, exams_change_direction, exams_change_label,
    total_flagged_events, flags_change_percent, flags_change_direction, flags_change_label,
    avg_integrity_score, integrity_change_percent, integrity_change_direction, integrity_change_label,
    most_common_flag_label, most_common_flag_share_percent, most_common_flag_icon,
    trend_granularity, trend_subtitle
)
SELECT
    u.id,
    v.period,
    v.total_exams_monitored,
    v.exams_change_percent,
    v.exams_change_direction,
    v.exams_change_label,
    v.total_flagged_events,
    v.flags_change_percent,
    v.flags_change_direction,
    v.flags_change_label,
    v.avg_integrity_score,
    v.integrity_change_percent,
    v.integrity_change_direction,
    v.integrity_change_label,
    v.most_common_flag_label,
    v.most_common_flag_share_percent,
    v.most_common_flag_icon,
    v.trend_granularity,
    v.trend_subtitle
FROM users u
CROSS JOIN (VALUES
    ('7D', 312, 8.00, 'UP', 'from last week', 89, 3.00, 'UP', 'from last week', 95.10, 0.40, 'UP', 'vs last week',
     'Face Not Visible', 45, 'visibility_off', 'DAY', 'Daily flagged events vs monitored sessions'),
    ('30D', 1248, 12.00, 'UP', 'from last period', 342, 5.00, 'UP', 'from last period', 94.20, NULL, 'STABLE', 'Stable',
     'Face Not Visible', 45, 'visibility_off', 'DAY', 'Daily flagged events vs monitored sessions'),
    ('3M', 3842, 18.00, 'UP', 'from last quarter', 1024, 9.00, 'UP', 'from last quarter', 93.80, -0.20, 'DOWN', 'vs last quarter',
     'Face Not Visible', 45, 'visibility_off', 'WEEK', 'Daily flagged events vs monitored sessions')
) AS v(
    period,
    total_exams_monitored, exams_change_percent, exams_change_direction, exams_change_label,
    total_flagged_events, flags_change_percent, flags_change_direction, flags_change_label,
    avg_integrity_score, integrity_change_percent, integrity_change_direction, integrity_change_label,
    most_common_flag_label, most_common_flag_share_percent, most_common_flag_icon,
    trend_granularity, trend_subtitle
)
WHERE u.role = 'LECTURER'
ON CONFLICT (lecturer_id, period) DO NOTHING;

-- 7D trend (Mon–Sun, Thu alert)
INSERT INTO lecturer_analytics_trend_points (overview_id, label, sort_order, monitored_sessions, flagged_events, alert)
SELECT o.id, v.label, v.sort_order, v.monitored_sessions, v.flagged_events, v.alert
FROM lecturer_analytics_overviews o
JOIN (VALUES
    ('Mon', 1, 58, 14, FALSE),
    ('Tue', 2, 62, 16, FALSE),
    ('Wed', 3, 55, 13, FALSE),
    ('Thu', 4, 60, 28, TRUE),
    ('Fri', 5, 64, 15, FALSE),
    ('Sat', 6, 38, 7, FALSE),
    ('Sun', 7, 35, 6, FALSE)
) AS v(label, sort_order, monitored_sessions, flagged_events, alert) ON TRUE
WHERE o.period = '7D'
  AND NOT EXISTS (
      SELECT 1 FROM lecturer_analytics_trend_points tp WHERE tp.overview_id = o.id
  );

-- 30D trend (same week shape as mockup)
INSERT INTO lecturer_analytics_trend_points (overview_id, label, sort_order, monitored_sessions, flagged_events, alert)
SELECT o.id, v.label, v.sort_order, v.monitored_sessions, v.flagged_events, v.alert
FROM lecturer_analytics_overviews o
JOIN (VALUES
    ('Mon', 1, 210, 48, FALSE),
    ('Tue', 2, 225, 52, FALSE),
    ('Wed', 3, 198, 44, FALSE),
    ('Thu', 4, 215, 96, TRUE),
    ('Fri', 5, 230, 50, FALSE),
    ('Sat', 6, 145, 28, FALSE),
    ('Sun', 7, 132, 24, FALSE)
) AS v(label, sort_order, monitored_sessions, flagged_events, alert) ON TRUE
WHERE o.period = '30D'
  AND NOT EXISTS (
      SELECT 1 FROM lecturer_analytics_trend_points tp WHERE tp.overview_id = o.id
  );

-- 3M trend (W1–W12, W4 alert)
INSERT INTO lecturer_analytics_trend_points (overview_id, label, sort_order, monitored_sessions, flagged_events, alert)
SELECT o.id, v.label, v.sort_order, v.monitored_sessions, v.flagged_events, v.alert
FROM lecturer_analytics_overviews o
JOIN (VALUES
    ('W1', 1, 310, 72, FALSE),
    ('W2', 2, 325, 78, FALSE),
    ('W3', 3, 318, 74, FALSE),
    ('W4', 4, 332, 142, TRUE),
    ('W5', 5, 340, 80, FALSE),
    ('W6', 6, 355, 82, FALSE),
    ('W7', 7, 348, 79, FALSE),
    ('W8', 8, 360, 84, FALSE),
    ('W9', 9, 352, 81, FALSE),
    ('W10', 10, 365, 86, FALSE),
    ('W11', 11, 358, 83, FALSE),
    ('W12', 12, 369, 88, FALSE)
) AS v(label, sort_order, monitored_sessions, flagged_events, alert) ON TRUE
WHERE o.period = '3M'
  AND NOT EXISTS (
      SELECT 1 FROM lecturer_analytics_trend_points tp WHERE tp.overview_id = o.id
  );

-- Top flagged behaviors per period
INSERT INTO lecturer_analytics_behaviors (overview_id, behavior_code, label, event_count, icon, tone, sort_order)
SELECT o.id, v.behavior_code, v.label, v.event_count, v.icon, v.tone, v.sort_order
FROM lecturer_analytics_overviews o
JOIN (VALUES
    ('7D', 'FACE_NOT_VISIBLE', 'Face Not Visible', 43, 'visibility_off', 'error', 1),
    ('7D', 'MULTIPLE_FACES', 'Multiple Faces', 25, 'groups', 'neutral', 2),
    ('7D', 'AUDIO_ANOMALY', 'Audio Anomaly', 15, 'mic_off', 'warning', 3),
    ('7D', 'DEVICE_DETECTED', 'Device Detected', 8, 'smartphone', 'neutral', 4),
    ('7D', 'TAB_CHANGE', 'Tab Change', 6, 'tab', 'neutral', 5),
    ('30D', 'FACE_NOT_VISIBLE', 'Face Not Visible', 154, 'visibility_off', 'error', 1),
    ('30D', 'MULTIPLE_FACES', 'Multiple Faces', 89, 'groups', 'neutral', 2),
    ('30D', 'AUDIO_ANOMALY', 'Audio Anomaly', 52, 'mic_off', 'warning', 3),
    ('30D', 'DEVICE_DETECTED', 'Device Detected', 27, 'smartphone', 'neutral', 4),
    ('30D', 'TAB_CHANGE', 'Tab Change', 20, 'tab', 'neutral', 5),
    ('3M', 'FACE_NOT_VISIBLE', 'Face Not Visible', 477, 'visibility_off', 'error', 1),
    ('3M', 'MULTIPLE_FACES', 'Multiple Faces', 276, 'groups', 'neutral', 2),
    ('3M', 'AUDIO_ANOMALY', 'Audio Anomaly', 161, 'mic_off', 'warning', 3),
    ('3M', 'DEVICE_DETECTED', 'Device Detected', 84, 'smartphone', 'neutral', 4),
    ('3M', 'TAB_CHANGE', 'Tab Change', 62, 'tab', 'neutral', 5)
) AS v(period, behavior_code, label, event_count, icon, tone, sort_order)
    ON o.period = v.period
WHERE NOT EXISTS (
    SELECT 1 FROM lecturer_analytics_behaviors b WHERE b.overview_id = o.id
);
