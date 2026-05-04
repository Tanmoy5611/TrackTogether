-- Dummy data for analytics and reporting sprint.
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
    ('77777777-7777-7777-7777-777777777777', 'GOOGLE-777777777777777777777', 'mila.willems@student.kdg.be', 'Mila Willems', true);

INSERT INTO member (user_id, co2_saved)
VALUES
    ('11111111-1111-1111-1111-111111111111', 0),
    ('22222222-2222-2222-2222-222222222222', 0),
    ('33333333-3333-3333-3333-333333333333', 0),
    ('44444444-4444-4444-4444-444444444444', 0),
    ('55555555-5555-5555-5555-555555555555', 0),
    ('66666666-6666-6666-6666-666666666666', 0),
    ('67676767-6767-6767-6767-676767676767', 0),
    ('77777777-7777-7777-7777-777777777777', 0);

INSERT INTO admin (user_id)
VALUES
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222');

INSERT INTO super_admin (user_id)
VALUES
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222');

INSERT INTO moderator (user_id)
VALUES ('55555555-5555-5555-5555-555555555555');

INSERT INTO activity (id, name, description, location, latitude, longitude, date, time, distance_km, creator_id)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Football training', 'Evening football session', 'KDG Campus Zuid', 51.2030, 4.4210, '2026-04-01', '18:30:00', 7.5, '11111111-1111-1111-1111-111111111111'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'AI workshop', 'Data & AI practice workshop', 'KDG Groenplaats', 51.2194, 4.4025, '2026-04-08', '13:00:00', 5.0, '22222222-2222-2222-2222-222222222222'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'Museum visit', 'Group visit to MAS', 'MAS Antwerp', 51.2289, 4.4047, '2026-04-17', '10:00:00', 8.0, '33333333-3333-3333-3333-333333333333'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'Hackathon', 'Student hackathon evening', 'Startup Village', 51.2127, 4.4215, '2026-05-03', '19:00:00', 12.0, '11111111-1111-1111-1111-111111111111'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'Volunteering day', 'Community cleanup', 'Park Spoor Noord', 51.2319, 4.4268, '2026-05-11', '09:00:00', 6.0, '44444444-4444-4444-4444-444444444444'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', 'Quarterly meetup', 'Large student meetup', 'KDG Hoboken', 51.1769, 4.3489, '2026-07-02', '17:30:00', 14.0, '22222222-2222-2222-2222-222222222222');

INSERT INTO travel_group (group_id, transport_mode, location, max_members, activity_id, owner_id)
VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'CARPOOL', 'Antwerp Central Station', 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'BIKE', 'Groenplaats', 8, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'PUBLIC_TRANSPORT', 'Berchem Station', 10, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'WALK', 'Meir', 6, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '33333333-3333-3333-3333-333333333333'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5', 'CARPOOL', 'KDG Zuid parking', 5, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6', 'CAR', 'Campus parking', 3, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '55555555-5555-5555-5555-555555555555'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7', 'PUBLIC_TRANSPORT', 'Antwerp Central Station', 12, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '44444444-4444-4444-4444-444444444444'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8', 'CARPOOL', 'Hoboken P+R', 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9', 'BIKE', 'KDG Campus Zuid', 8, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '66666666-6666-6666-6666-666666666666');

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
    (22, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9', '77777777-7777-7777-7777-777777777777', NULL);

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
    ('cccccccc-cccc-cccc-cccc-ccccccccccc9', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9');

ALTER SEQUENCE travel_group_member_seq RESTART WITH 100;
