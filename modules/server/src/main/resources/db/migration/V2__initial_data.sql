-- USERS

INSERT INTO users(email, hashed_password) VALUES('vince@misterjvm.com', '1000:522A8F8FEF27AC8CDE10F23F4BDC76634594E3B24F17F743:119E61377766DFD35F63D48AFF3E4EEB3CDB1B503BFB137F');

-- TRAINERS

INSERT INTO trainers(name, description, url, image) VALUES('Paul J. Fabritz', 'Popular NBA trainer', 'https://pjfperformance.net', NULL);
INSERT INTO trainers(name, description, url, image) VALUES('Tyler Relph', 'Austin, TX NBA trainer', 'https://www.tylerrelphtraining.com/', NULL);

-- PROGRAMS

INSERT INTO programs(slug, name, url, trainer_id, trainer_name, payment_type, image, tags) VALUES('pjf-performance-unranked-academy', 'Unranked Academy', 'https://www.pjfperformance.com/unranked-basketball-academy/', 1, 'Paul J. Fabritz', 'Subscription', NULL, '{Strength,Shooting,Ball Handling,Agility}');
INSERT INTO programs(slug, name, url, trainer_id, trainer_name, payment_type, image, tags) VALUES('pjf-performance-the-vert-code', 'The Vert Code', 'https://www.pjfperformance.com/the-vert-code/', 1, 'Paul J. Fabritz', 'LifetimeAccess', NULL, '{Vertical}');
INSERT INTO programs(slug, name, url, trainer_id, trainer_name, payment_type, image, tags) VALUES('tyler-relph-basketball-academy', 'Tyler Relph Basketball Academy', 'https://www.trbasketballacademy.com/', 2, 'Tyler Relph', 'Subscription', NULL, '{Shooting, Ball Handling}');

-- REVIEWS

INSERT INTO reviews(program_id, program_slug, user_id, value, quality, content, user_experience, accessibility, support, would_recommend, review) VALUES(1, 'pjf-performance-unranked-academy', 1, 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Amazing', 'Absolutely the best program out there for all hoopers!');

-- INVITES

INSERT INTO invites(username, program_id, n_invites, active) VALUES ('vinh@misterjvm.com', 1, 10, true);