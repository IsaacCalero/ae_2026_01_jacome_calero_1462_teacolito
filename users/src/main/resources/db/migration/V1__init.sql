CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    sub VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_profiles_sub UNIQUE (sub),
    CONSTRAINT uq_user_profiles_display_name UNIQUE (display_name)
);
