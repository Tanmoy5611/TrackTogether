INSERT INTO users (original_id,email,name, status)
values ('GOOGLE-1', 'yoran.delcroix@outlook.com', 'yoran', 1);


-- for testing the travel group
-- users
INSERT INTO users (user_id, email, name, original_id, status)
VALUES ('22222222-2222-2222-2222-222222222222','test@test.com','Alice','GOOGLE-1',true) ON CONFLICT (user_id) DO NOTHING;


-- member
INSERT INTO member (user_id, co2_saved)
VALUES ('22222222-2222-2222-2222-222222222222',0) ON CONFLICT (user_id) DO NOTHING;


-- activity
INSERT INTO activity (activity_id, title, description, location, start_time, is_verified, creator_id)
VALUES ('11111111-1111-1111-1111-111111111111','Football','Football match','Antwerp',NOW(),true, '22222222-2222-2222-2222-222222222222')
ON CONFLICT (activity_id) DO NOTHING;


-- travel_group
INSERT INTO travel_group (group_id, available_spots, location, transport_mode, activity_id, max_members)
VALUES ('33333333-3333-3333-3333-333333333333',5,'Antwerp','CAR', '11111111-1111-1111-1111-111111111111', 5 )
ON CONFLICT (group_id) DO NOTHING;