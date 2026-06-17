-- noinspection SqlNoDataSourceInspectionForFile,SqlResolveForFile
-- Dummy data for analytics, reporting, and friend-matching demos.
-- CO2 model used by AnalyticsService:
-- Baseline = every participant travels alone by car.
-- Car = 0.120 kg CO2/km/person.
-- Carpool = 0.120 kg CO2/km for the car, divided across passengers.
-- Public transport = 0.040 kg CO2/km/person.
-- Bike/walk = 0 kg CO2/km.

INSERT INTO users (user_id, original_id, email, name, status)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'GOOGLE-108607539392394218230', 'yoran.delcroix@student.kdg.be', 'Yoran Delcroix', true),
    ('22222222-2222-2222-2222-222222222222', 'GOOGLE-101941250466901984913', 'tanmoy.das@student.kdg.be', 'Tanmoy Das', true),
    ('33333333-3333-3333-3333-333333333333', 'GOOGLE-333333333333333333333', 'emma.peeters@student.kdg.be', 'Emma Peeters', true),
    ('44444444-4444-4444-4444-444444444444', 'GOOGLE-444444444444444444444', 'noah.janssens@student.kdg.be', 'Noah Janssens', true),
    ('55555555-5555-5555-5555-555555555555', 'GOOGLE-116542068906624086959', 'jorge.simoes@student.kdg.be', 'Jorge Simoes', true),
    ('66666666-6666-6666-6666-666666666666', 'GOOGLE-666666666666666666666', 'lina.vermeulen@student.kdg.be', 'Lina Vermeulen', true),
    ('67676767-6767-6767-6767-676767676767', 'GOOGLE-676767676767676767676', 'bart.dezweter@kdg.be', 'Bart De Zweter', true),
    ('77777777-7777-7777-7777-777777777777', 'GOOGLE-777777777777777777777', 'mila.willems@student.kdg.be', 'Mila Willems', true)
ON CONFLICT DO NOTHING;

INSERT INTO member (user_id, co2_saved, preferred_transport_mode, default_departure_location, default_latitude, default_longitude)
VALUES
    ('11111111-1111-1111-1111-111111111111', 0, 'PUBLIC_TRANSPORT', 'Antwerp Central Station', 51.2172, 4.4211),
    ('22222222-2222-2222-2222-222222222222', 0, 'BIKE', 'Groenplaats', 51.2194, 4.4025),
    ('33333333-3333-3333-3333-333333333333', 0, 'CARPOOL', 'Antwerp Central Station', 51.2172, 4.4211),
    ('44444444-4444-4444-4444-444444444444', 0, 'PUBLIC_TRANSPORT', 'Berchem Station', 51.1992, 4.4320),
    ('55555555-5555-5555-5555-555555555555', 0, 'CAR', 'KDG Zuid parking', 51.2030, 4.4210),
    ('66666666-6666-6666-6666-666666666666', 0, 'BIKE', 'KDG Campus Zuid', 51.2030, 4.4210),
    ('67676767-6767-6767-6767-676767676767', 0, 'WALK', 'Meir', 51.2187, 4.4112),
    ('77777777-7777-7777-7777-777777777777', 0, 'CARPOOL', 'Hoboken P+R', 51.1805, 4.3480)
ON CONFLICT DO NOTHING;

INSERT INTO admin (user_id)
VALUES
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222')
ON CONFLICT DO NOTHING;

INSERT INTO super_admin (user_id)
VALUES
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222')
ON CONFLICT DO NOTHING;

INSERT INTO moderator (user_id)
VALUES ('55555555-5555-5555-5555-555555555555')
ON CONFLICT DO NOTHING;

INSERT INTO activity (id, name, description, location, latitude, longitude, date, time, distance_km, creator_id, verification_status)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Football training', 'Evening football session', 'KDG Campus Zuid', 51.2030, 4.4210, '2026-06-01', '18:30:00', 7.5, '11111111-1111-1111-1111-111111111111', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'AI workshop', 'Data & AI practice workshop', 'KDG Groenplaats', 51.2194, 4.4025, '2026-06-08', '13:00:00', 5.0, '22222222-2222-2222-2222-222222222222', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'Museum visit', 'Group visit to MAS', 'MAS Antwerp', 51.2289, 4.4047, '2026-06-17', '10:00:00', 8.0, '33333333-3333-3333-3333-333333333333', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'Hackathon', 'Student hackathon evening', 'Startup Village', 51.2127, 4.4215, '2026-06-23', '19:00:00', 12.0, '11111111-1111-1111-1111-111111111111', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'Volunteering day', 'Community cleanup', 'Park Spoor Noord', 51.2319, 4.4268, '2026-06-29', '09:00:00', 6.0, '44444444-4444-4444-4444-444444444444', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', 'Quarterly meetup', 'Large student meetup', 'KDG Hoboken', 51.1769, 4.3489, '2026-07-02', '17:30:00', 14.0, '22222222-2222-2222-2222-222222222222', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', 'De Lijn route lab', 'Hands-on route planning session for public transport matching', 'KDG Groenplaats', 51.2194, 4.4025, '2026-07-09', '14:00:00', 6.5, '44444444-4444-4444-4444-444444444444', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8', 'Exam prep meetup', 'Quiet study session before exams', 'KDG Campus Zuid', 51.2030, 4.4210, '2026-05-28', '16:00:00', 4.2, '66666666-6666-6666-6666-666666666666', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9', 'Summer volunteering', 'Helping at a student community garden', 'Park Spoor Noord', 51.2319, 4.4268, '2026-08-18', '08:45:00', 9.0, '33333333-3333-3333-3333-333333333333', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10', 'Welcome week fair', 'Start-of-year student fair', 'KDG Groenplaats', 51.2194, 4.4025, '2026-09-07', '11:00:00', 10.5, '11111111-1111-1111-1111-111111111111', 'APPROVED'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11', 'Board game night', 'Casual board game evening', 'KDG Campus Zuid', 51.2030, 4.4210, '2026-07-15', '19:00:00', 3.0, '33333333-3333-3333-3333-333333333333', 'PENDING'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12', 'Photography walk', 'Urban photography session around Antwerp', 'Meir', 51.2187, 4.4112, '2026-08-05', '10:30:00', 5.5, '44444444-4444-4444-4444-444444444444', 'PENDING')
ON CONFLICT (id) DO UPDATE
SET verification_status = EXCLUDED.verification_status;

INSERT INTO travel_group (
    group_id, transport_mode, location,
    departure_location, departure_latitude, departure_longitude,
    arrival_latitude, arrival_longitude,
    departure_time, estimated_arrival_time,
    max_members, activity_id, owner_id
)
VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'CARPOOL', 'Antwerp Central Station', 'Antwerp Central Station', 51.2172, 4.4211, 51.2030, 4.4210, '2026-06-01 17:30:00', '2026-06-01 18:10:00', 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'BIKE', 'Groenplaats', 'Groenplaats', 51.2194, 4.4025, 51.2030, 4.4210, '2026-06-01 17:45:00', '2026-06-01 18:15:00', 8, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'PUBLIC_TRANSPORT', 'Berchem Station', 'Berchem Station', 51.1992, 4.4320, 51.2194, 4.4025, '2026-06-08 12:10:00', '2026-06-08 12:45:00', 10, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'WALK', 'Meir', 'Meir', 51.2187, 4.4112, 51.2289, 4.4047, '2026-06-17 09:25:00', '2026-06-17 09:50:00', 6, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '33333333-3333-3333-3333-333333333333'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5', 'CARPOOL', 'KDG Zuid parking', 'KDG Zuid parking', 51.2030, 4.4210, 51.2127, 4.4215, '2026-06-23 18:15:00', '2026-06-23 18:40:00', 5, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6', 'CAR', 'Campus parking', 'Campus parking', 51.2127, 4.4215, 51.2127, 4.4215, '2026-06-23 18:30:00', '2026-06-23 18:45:00', 2, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '55555555-5555-5555-5555-555555555555'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7', 'PUBLIC_TRANSPORT', 'Antwerp Central Station', 'Antwerp Central Station', 51.2172, 4.4211, 51.2319, 4.4268, '2026-06-29 08:15:00', '2026-06-29 08:45:00', 12, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '44444444-4444-4444-4444-444444444444'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8', 'CARPOOL', 'Hoboken P+R', 'Hoboken P+R', 51.1805, 4.3480, 51.1769, 4.3489, '2026-07-02 16:45:00', '2026-07-02 17:10:00', 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9', 'BIKE', 'KDG Campus Zuid', 'KDG Campus Zuid', 51.2030, 4.4210, 51.1769, 4.3489, '2026-07-02 16:15:00', '2026-07-02 17:05:00', 8, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '66666666-6666-6666-6666-666666666666'),
    -- Open public-transport group near Yoran's default departure, useful for De Lijn matching demos.
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb10', 'PUBLIC_TRANSPORT', 'Antwerp Central Station', 'Antwerp Central Station', 51.2172, 4.4211, 51.2194, 4.4025, '2026-07-09 13:10:00', '2026-07-09 13:45:00', 5, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '44444444-4444-4444-4444-444444444444'),
    -- Full public-transport group for checking that full groups are not suggested.
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb11', 'PUBLIC_TRANSPORT', 'Berchem Station', 'Berchem Station', 51.1992, 4.4320, 51.2194, 4.4025, '2026-07-09 13:20:00', '2026-07-09 13:50:00', 2, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '66666666-6666-6666-6666-666666666666'),
    -- Different transport mode from the same event, useful for comparing match scores.
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb12', 'CARPOOL', 'Mechelen Station', 'Mechelen Station', 51.0176, 4.4821, 51.2194, 4.4025, '2026-07-09 12:45:00', '2026-07-09 13:35:00', 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '33333333-3333-3333-3333-333333333333'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb13', 'PUBLIC_TRANSPORT', 'Antwerp Central Station', 'Antwerp Central Station', 51.2172, 4.4211, 51.2030, 4.4210, '2026-05-28 15:15:00', '2026-05-28 15:50:00', 6, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8', '66666666-6666-6666-6666-666666666666'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb14', 'BIKE', 'Groenplaats', 'Groenplaats', 51.2194, 4.4025, 51.2030, 4.4210, '2026-05-28 15:25:00', '2026-05-28 15:55:00', 8, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15', 'CARPOOL', 'Berchem Station', 'Berchem Station', 51.1992, 4.4320, 51.2319, 4.4268, '2026-08-18 07:50:00', '2026-08-18 08:30:00', 5, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9', '33333333-3333-3333-3333-333333333333'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb16', 'PUBLIC_TRANSPORT', 'Antwerp Central Station', 'Antwerp Central Station', 51.2172, 4.4211, 51.2319, 4.4268, '2026-08-18 07:55:00', '2026-08-18 08:35:00', 10, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb17', 'WALK', 'Meir', 'Meir', 51.2187, 4.4112, 51.2194, 4.4025, '2026-09-07 10:25:00', '2026-09-07 10:50:00', 6, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10', '67676767-6767-6767-6767-676767676767'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18', 'CARPOOL', 'Hoboken P+R', 'Hoboken P+R', 51.1805, 4.3480, 51.2194, 4.4025, '2026-09-07 10:00:00', '2026-09-07 10:45:00', 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10', '77777777-7777-7777-7777-777777777777')
ON CONFLICT DO NOTHING;

INSERT INTO travel_group_member (id, group_id, member_id, location_id)
VALUES
    (1, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '11111111-1111-1111-1111-111111111111', NULL),
    (2, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '22222222-2222-2222-2222-222222222222', NULL),
    (3, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '33333333-3333-3333-3333-333333333333', NULL),
    (4, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '44444444-4444-4444-4444-444444444444', NULL),
    (5, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '55555555-5555-5555-5555-555555555555', NULL),
    (6, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', '11111111-1111-1111-1111-111111111111', NULL),
    (7, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', '66666666-6666-6666-6666-666666666666', NULL),
    (8, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', '77777777-7777-7777-7777-777777777777', NULL),
    (9, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', '22222222-2222-2222-2222-222222222222', NULL),
    (10, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', '33333333-3333-3333-3333-333333333333', NULL),
    (11, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5', '11111111-1111-1111-1111-111111111111', NULL),
    (12, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5', '44444444-4444-4444-4444-444444444444', NULL),
    (13, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5', '55555555-5555-5555-5555-555555555555', NULL),
    (14, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5', '66666666-6666-6666-6666-666666666666', NULL),
    (15, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6', '77777777-7777-7777-7777-777777777777', NULL),
    (16, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7', '11111111-1111-1111-1111-111111111111', NULL),
    (17, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7', '22222222-2222-2222-2222-222222222222', NULL),
    (18, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8', '33333333-3333-3333-3333-333333333333', NULL),
    (19, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8', '44444444-4444-4444-4444-444444444444', NULL),
    (20, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9', '55555555-5555-5555-5555-555555555555', NULL),
    (21, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9', '66666666-6666-6666-6666-666666666666', NULL),
    (22, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9', '77777777-7777-7777-7777-777777777777', NULL),
    (23, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '22222222-2222-2222-2222-222222222222', NULL),
    (24, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', '22222222-2222-2222-2222-222222222222', NULL),
    (25, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6', '55555555-5555-5555-5555-555555555555', NULL),
    (26, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7', '44444444-4444-4444-4444-444444444444', NULL),
    (27, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8', '22222222-2222-2222-2222-222222222222', NULL),
    (28, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb10', '44444444-4444-4444-4444-444444444444', NULL),
    (29, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb10', '77777777-7777-7777-7777-777777777777', NULL),
    (30, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb11', '66666666-6666-6666-6666-666666666666', NULL),
    (31, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb11', '33333333-3333-3333-3333-333333333333', NULL),
    (32, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb12', '33333333-3333-3333-3333-333333333333', NULL),
    (33, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb12', '55555555-5555-5555-5555-555555555555', NULL),
    (34, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb13', '66666666-6666-6666-6666-666666666666', NULL),
    (35, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb13', '11111111-1111-1111-1111-111111111111', NULL),
    (36, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb13', '22222222-2222-2222-2222-222222222222', NULL),
    (37, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb14', '22222222-2222-2222-2222-222222222222', NULL),
    (38, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb14', '77777777-7777-7777-7777-777777777777', NULL),
    (39, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15', '33333333-3333-3333-3333-333333333333', NULL),
    (40, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15', '44444444-4444-4444-4444-444444444444', NULL),
    (41, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15', '55555555-5555-5555-5555-555555555555', NULL),
    (42, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15', '77777777-7777-7777-7777-777777777777', NULL),
    (43, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb16', '11111111-1111-1111-1111-111111111111', NULL),
    (44, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb16', '66666666-6666-6666-6666-666666666666', NULL),
    (45, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb17', '67676767-6767-6767-6767-676767676767', NULL),
    (46, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb17', '33333333-3333-3333-3333-333333333333', NULL),
    (47, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18', '77777777-7777-7777-7777-777777777777', NULL),
    (48, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18', '11111111-1111-1111-1111-111111111111', NULL),
    (49, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18', '22222222-2222-2222-2222-222222222222', NULL),
    (50, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18', '55555555-5555-5555-5555-555555555555', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO conversation (conversation_id, created_at, travel_group_id)
VALUES
    ('cccccccc-cccc-cccc-cccc-ccccccccccc1', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc2', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc3', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc4', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc5', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc6', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc7', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc8', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8'),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc9', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc10', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb10'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc11', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb11'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc12', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb12'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc13', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb13'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc14', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb14'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc15', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc16', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb16'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc17', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb17'),
    ('cccccccc-cccc-cccc-cccc-cccccccccc18', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18')
ON CONFLICT DO NOTHING;

ALTER SEQUENCE travel_group_member_seq RESTART WITH 200;

-- ── Seed messages so reporting can be tested on localhost ─────────────────────
-- Jorge (55555...) is a member of groups/conversations ccc2, ccc5, ccc6, ccc9.
-- Messages are from OTHER members so Jorge can click the flag and submit a report.

INSERT INTO message (message_id, message, time_stamp, conversation_id, sender_id)
VALUES
    -- conversation ccc2: Bike group (Tanmoy owns it, Noah + Jorge are members)
    ('dddddddd-dddd-dddd-dddd-dddddddddd01', 'Hey everyone, what time are we meeting at Groenplaats?',       NOW() - INTERVAL '2 hours', 'cccccccc-cccc-cccc-cccc-ccccccccccc2', '22222222-2222-2222-2222-222222222222'),
    ('dddddddd-dddd-dddd-dddd-dddddddddd02', 'I was thinking around 17:30, gives us 15 min buffer.',         NOW() - INTERVAL '110 minutes', 'cccccccc-cccc-cccc-cccc-ccccccccccc2', '44444444-4444-4444-4444-444444444444'),
    ('dddddddd-dddd-dddd-dddd-dddddddddd03', 'Works for me. Bring your own lock!',                           NOW() - INTERVAL '100 minutes', 'cccccccc-cccc-cccc-cccc-ccccccccccc2', '22222222-2222-2222-2222-222222222222'),
    ('dddddddd-dddd-dddd-dddd-dddddddddd04', 'This group is a waste of time, nobody ever shows up on time.', NOW() - INTERVAL '90 minutes',  'cccccccc-cccc-cccc-cccc-ccccccccccc2', '44444444-4444-4444-4444-444444444444'),

    -- conversation ccc5: Carpool group (Yoran owns, Yoran + Noah + Jorge + Lina)
    ('dddddddd-dddd-dddd-dddd-dddddddddd05', 'Hackathon carpool leaving KDG Zuid at 18:15 sharp.',           NOW() - INTERVAL '3 hours',     'cccccccc-cccc-cccc-cccc-ccccccccccc5', '11111111-1111-1111-1111-111111111111'),
    ('dddddddd-dddd-dddd-dddd-dddddddddd06', 'Got it, I will be there on time.',                             NOW() - INTERVAL '170 minutes', 'cccccccc-cccc-cccc-cccc-ccccccccccc5', '66666666-6666-6666-6666-666666666666'),
    ('dddddddd-dddd-dddd-dddd-dddddddddd07', 'Can someone else drive? I really do not feel like it.',        NOW() - INTERVAL '160 minutes', 'cccccccc-cccc-cccc-cccc-ccccccccccc5', '44444444-4444-4444-4444-444444444444'),
    ('dddddddd-dddd-dddd-dddd-dddddddddd08', 'Stop being so lazy Noah, you always do this.',                 NOW() - INTERVAL '150 minutes', 'cccccccc-cccc-cccc-cccc-ccccccccccc5', '11111111-1111-1111-1111-111111111111')
ON CONFLICT DO NOTHING;