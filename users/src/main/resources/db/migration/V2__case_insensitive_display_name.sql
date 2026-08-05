ALTER TABLE user_profiles DROP CONSTRAINT uq_user_profiles_display_name;

CREATE UNIQUE INDEX uq_user_profiles_display_name_ci ON user_profiles (LOWER(display_name));
