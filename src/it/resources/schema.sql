CREATE extension IF NOT EXISTS "uuid-ossp";

CREATE TYPE channel_enum AS ENUM ('EMAIL', 'DIIA');

CREATE TABLE settings
(
    id uuid NOT NULL default uuid_generate_v4(),
    keycloak_id uuid NOT NULL,
    CONSTRAINT settings__settings_id__pk PRIMARY KEY (id),
    CONSTRAINT settings__keycloak_id__uk UNIQUE (keycloak_id)
);

CREATE TABLE notification_channel
(
    id uuid NOT NULL default uuid_generate_v4(),
    settings_id uuid NOT NULL,
    channel channel_enum NOT NULL,
    address text,
    deactivation_reason text,
    is_activated boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT notification_channel__id__pk PRIMARY KEY (id),
    CONSTRAINT notification_channel__address__uk UNIQUE (address),
    CONSTRAINT notification_channel__settings_channel__uk UNIQUE (settings_id, channel),
    CONSTRAINT notification_channel__settings_fk FOREIGN KEY (settings_id)
        REFERENCES settings (id)
);

INSERT INTO settings (id, keycloak_id) VALUES
(
    '321e7654-e89b-12d3-a456-426655441111',
    '496fd2fd-3497-4391-9ead-41410522d06f'
),
(
    '7f18fd5f-d68e-4609-85a8-eb5745488ac2',
    '4cb2fb36-df5a-474d-9e82-0a9848231bd6'
),
(
    '321e7654-e89b-12d3-a456-426655441112',
    '46f36b22-a4bd-45b0-8e48-98dc38ba0e33'
);

INSERT INTO notification_channel (
	id, settings_id, channel, address, deactivation_reason, is_activated)
	VALUES
	('69d24728-6d59-4513-9919-69e5e1546762', '321e7654-e89b-12d3-a456-426655441111', 'EMAIL',
	 'settings@gmail.com', NULL, TRUE),
    ('9b85f2f0-4a1f-4539-bc8a-845e90e42442', '321e7654-e89b-12d3-a456-426655441112', 'DIIA',
     NULL, 'User deactivated', FALSE);
