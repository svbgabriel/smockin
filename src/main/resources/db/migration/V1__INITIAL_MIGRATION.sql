CREATE SEQUENCE hibernate_sequence;

-- APP_CONFIG table
CREATE TABLE app_config (
                            id BIGINT NOT NULL,
                            app_current_version VARCHAR(20) NOT NULL,
                            app_install_version VARCHAR(20) NOT NULL,
                            db_install_date TIMESTAMP NOT NULL,
                            last_version_update TIMESTAMP,

                            CONSTRAINT pk_app_config PRIMARY KEY (id)
);

-- REST_PROJ table
CREATE TABLE rest_proj (
                           id BIGINT NOT NULL,
                           date_created TIMESTAMP NOT NULL,
                           ext_id VARCHAR(255) NOT NULL,
                           last_updated TIMESTAMP,
                           name VARCHAR(100) NOT NULL,

                           CONSTRAINT pk_rest_proj PRIMARY KEY (id),
                           CONSTRAINT uq_rest_proj_ext_id UNIQUE (ext_id),
                           CONSTRAINT uq_rest_proj_name UNIQUE (name)
);

-- SERVER_CONFIG table
CREATE TABLE server_config (
                               id BIGINT NOT NULL,
                               date_created TIMESTAMP NOT NULL,
                               ext_id VARCHAR(255) NOT NULL,
                               last_updated TIMESTAMP,
                               auto_start BOOLEAN NOT NULL,
                               max_threads INTEGER NOT NULL,
                               min_threads INTEGER NOT NULL,
                               port INTEGER NOT NULL,
                               proxy_mode BOOLEAN NOT NULL DEFAULT FALSE,
                               server_type VARCHAR(20) NOT NULL,
                               time_out_millis INTEGER NOT NULL,

                               CONSTRAINT pk_server_config PRIMARY KEY (id),
                               CONSTRAINT uq_server_config_ext_id UNIQUE (ext_id),
                               CONSTRAINT uq_server_config_port UNIQUE (port),
                               CONSTRAINT uq_server_config_server_type UNIQUE (server_type)
);

-- SERVER_CONFIG_NATIVE_PROPERTIES table
CREATE TABLE server_config_native_properties (
                                                 server_config_id BIGINT NOT NULL,
                                                 native_properties VARCHAR(255),
                                                 native_properties_key VARCHAR(255) NOT NULL,

                                                 CONSTRAINT pk_server_config_native_properties PRIMARY KEY (server_config_id, native_properties_key),
                                                 CONSTRAINT fk_server_config_native_properties_config_id
                                                     FOREIGN KEY (server_config_id) REFERENCES server_config(id)
);

-- SMKN_USER table
CREATE TABLE smkn_user (
                           id BIGINT NOT NULL,
                           date_created TIMESTAMP NOT NULL,
                           ext_id VARCHAR(255) NOT NULL,
                           last_updated TIMESTAMP,
                           ctx_path VARCHAR(50) NOT NULL,
                           full_name VARCHAR(100) NOT NULL,
                           password VARCHAR(100) NOT NULL,
                           pw_reset_token VARCHAR(50) NOT NULL,
                           pw_reset_token_expiry TIMESTAMP,
                           role VARCHAR(10) NOT NULL,
                           sess_token VARCHAR(350) NOT NULL,
                           rec_status VARCHAR(15) NOT NULL,
                           user_name VARCHAR(35) NOT NULL,

                           CONSTRAINT pk_smkn_user PRIMARY KEY (id),
                           CONSTRAINT uq_smkn_user_ext_id UNIQUE (ext_id),
                           CONSTRAINT uq_smkn_user_ctx_path UNIQUE (ctx_path),
                           CONSTRAINT uq_smkn_user_pw_reset_token UNIQUE (pw_reset_token),
                           CONSTRAINT uq_smkn_user_sess_token UNIQUE (sess_token),
                           CONSTRAINT uq_smkn_user_user_name UNIQUE (user_name)
);

-- MAIL_MOCK table
CREATE TABLE mail_mock (
                           id BIGINT NOT NULL,
                           date_created TIMESTAMP NOT NULL,
                           ext_id VARCHAR(255) NOT NULL,
                           last_updated TIMESTAMP,
                           address VARCHAR(120) NOT NULL,
                           save_rec_mail BOOLEAN NOT NULL,
                           rec_status VARCHAR(15) NOT NULL,
                           created_by BIGINT NOT NULL,

                           CONSTRAINT pk_mail_mock PRIMARY KEY (id),
                           CONSTRAINT uq_mail_mock_ext_id UNIQUE (ext_id),
                           CONSTRAINT uq_mail_mock_address UNIQUE (address),
                           CONSTRAINT fk_mail_mock_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id)
);

-- MAIL_MOCK_MSG table
CREATE TABLE mail_mock_msg (
                               id BIGINT NOT NULL,
                               date_created TIMESTAMP NOT NULL,
                               ext_id VARCHAR(255) NOT NULL,
                               last_updated TIMESTAMP,
                               mail_body TEXT NOT NULL,
                               date_received TIMESTAMP NOT NULL,
                               mail_sender VARCHAR(200) NOT NULL,
                               mail_subject VARCHAR(500) NOT NULL,
                               mail_mock_id BIGINT NOT NULL,

                               CONSTRAINT pk_mail_mock_msg PRIMARY KEY (id),
                               CONSTRAINT uq_mail_mock_msg_ext_id UNIQUE (ext_id),
                               CONSTRAINT fk_mail_mock_msg_mail_mock_id FOREIGN KEY (mail_mock_id) REFERENCES mail_mock(id)
);

-- MAIL_MOCK_MSG_ATCH table
CREATE TABLE mail_mock_msg_atch (
                                    id BIGINT NOT NULL,
                                    date_created TIMESTAMP NOT NULL,
                                    ext_id VARCHAR(255) NOT NULL,
                                    last_updated TIMESTAMP,
                                    mime_type VARCHAR(50) NOT NULL,
                                    file_name VARCHAR(200) NOT NULL,
                                    mail_mock_msg_id BIGINT NOT NULL,

                                    CONSTRAINT pk_mail_mock_msg_atch PRIMARY KEY (id),
                                    CONSTRAINT uq_mail_mock_msg_atch_ext_id UNIQUE (ext_id),
                                    CONSTRAINT fk_mail_mock_msg_atch_msg_id FOREIGN KEY (mail_mock_msg_id) REFERENCES mail_mock_msg(id)
);

-- MAIL_MOCK_MSG_ATCH_CONT table
CREATE TABLE mail_mock_msg_atch_cont (
                                         id BIGINT NOT NULL,
                                         date_created TIMESTAMP NOT NULL,
                                         ext_id VARCHAR(255) NOT NULL,
                                         last_updated TIMESTAMP,
                                         content TEXT NOT NULL,
                                         mail_mock_msg_atch_id BIGINT NOT NULL,

                                         CONSTRAINT pk_mail_mock_msg_atch_cont PRIMARY KEY (id),
                                         CONSTRAINT uq_mail_mock_msg_atch_cont_ext_id UNIQUE (ext_id),
                                         CONSTRAINT fk_mail_mock_msg_atch_cont_atch_id FOREIGN KEY (mail_mock_msg_atch_id) REFERENCES mail_mock_msg_atch(id)
);

-- PROXY_FORWARD_USER_CONFIG table
CREATE TABLE proxy_forward_user_config (
                                           id BIGINT NOT NULL,
                                           date_created TIMESTAMP NOT NULL,
                                           ext_id VARCHAR(255) NOT NULL,
                                           last_updated TIMESTAMP,
                                           no_forward_when_404_mock BOOLEAN NOT NULL DEFAULT FALSE,
                                           proxy_mode_type VARCHAR(8),
                                           created_by BIGINT,
                                           server_config_id BIGINT,

                                           CONSTRAINT pk_proxy_forward_user_config PRIMARY KEY (id),
                                           CONSTRAINT uq_proxy_forward_user_config_ext_id UNIQUE (ext_id),
                                           CONSTRAINT fk_proxy_forward_user_config_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id),
                                           CONSTRAINT fk_proxy_forward_user_config_server_config_id FOREIGN KEY (server_config_id) REFERENCES server_config(id)
);

-- PROXY_FORWARD_MAPPING table
CREATE TABLE proxy_forward_mapping (
                                       id BIGINT NOT NULL,
                                       date_created TIMESTAMP NOT NULL,
                                       ext_id VARCHAR(255) NOT NULL,
                                       last_updated TIMESTAMP,
                                       is_disabled BOOLEAN NOT NULL DEFAULT FALSE,
                                       path VARCHAR(1000) NOT NULL,
                                       proxy_forward_url VARCHAR(500) NOT NULL,
                                       proxy_forward_user_config_id BIGINT NOT NULL,
                                       created_by BIGINT,
                                       stateful_parent BIGINT,

                                       CONSTRAINT pk_proxy_forward_mapping PRIMARY KEY (id),
                                       CONSTRAINT uq_proxy_forward_mapping_ext_id UNIQUE (ext_id),
                                       CONSTRAINT uq_proxy_forward_mapping_path UNIQUE (path),
                                       CONSTRAINT fk_proxy_forward_mapping_config_id FOREIGN KEY (proxy_forward_user_config_id) REFERENCES proxy_forward_user_config(id),
                                       CONSTRAINT fk_proxy_forward_mapping_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id),
                                       CONSTRAINT fk_proxy_forward_mapping_stateful_parent FOREIGN KEY (stateful_parent) REFERENCES proxy_forward_mapping(id)
);

-- REST_MOCK table
CREATE TABLE rest_mock (
                           id BIGINT NOT NULL,
                           date_created TIMESTAMP NOT NULL,
                           ext_id VARCHAR(255) NOT NULL,
                           last_updated TIMESTAMP,
                           init_order INTEGER NOT NULL,
                           http_method VARCHAR(10) NOT NULL,
                           mock_type VARCHAR(10) NOT NULL,
                           path VARCHAR(1000) NOT NULL,
                           proxy_fw_no_rule_match BOOLEAN NOT NULL DEFAULT FALSE,
                           proxy_push_id_on_cnct BOOLEAN NOT NULL DEFAULT FALSE,
                           proxy_time_out_millis BIGINT NOT NULL,
                           random_def BOOLEAN NOT NULL,
                           random_lat BOOLEAN NOT NULL DEFAULT FALSE,
                           rdm_lat_range_max BIGINT NOT NULL DEFAULT 0,
                           rdm_lat_range_min BIGINT NOT NULL DEFAULT 0,
                           sse_heart_beat_millis BIGINT NOT NULL DEFAULT 0,
                           rec_status VARCHAR(15) NOT NULL,
                           ws_time_out_millis BIGINT NOT NULL DEFAULT 0,
                           created_by BIGINT,
                           proj_id BIGINT,
                           stateful_parent BIGINT,

                           CONSTRAINT pk_rest_mock PRIMARY KEY (id),
                           CONSTRAINT uq_rest_mock_ext_id UNIQUE (ext_id),
                           CONSTRAINT uq_rest_mock_path_method_user UNIQUE (path, http_method, created_by),
                           CONSTRAINT fk_rest_mock_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id),
                           CONSTRAINT fk_rest_mock_proj_id FOREIGN KEY (proj_id) REFERENCES rest_proj(id),
                           CONSTRAINT fk_rest_mock_stateful_parent FOREIGN KEY (stateful_parent) REFERENCES rest_mock(id)
);

-- REST_MOCK_DEF table
CREATE TABLE rest_mock_def (
                               id BIGINT NOT NULL,
                               date_created TIMESTAMP NOT NULL,
                               ext_id VARCHAR(255) NOT NULL,
                               last_updated TIMESTAMP,
                               freq_count INTEGER NOT NULL DEFAULT 0,
                               freq_percent INTEGER NOT NULL DEFAULT 0,
                               http_status_code INTEGER NOT NULL,
                               order_no INTEGER NOT NULL,
                               response_body TEXT,
                               response_content_type VARCHAR(100) NOT NULL,
                               sleep_in_millis BIGINT NOT NULL,
                               suspend BOOLEAN NOT NULL DEFAULT FALSE,
                               rest_mock_id BIGINT NOT NULL,

                               CONSTRAINT pk_rest_mock_def PRIMARY KEY (id),
                               CONSTRAINT uq_rest_mock_def_ext_id UNIQUE (ext_id),
                               CONSTRAINT fk_rest_mock_def_rest_mock_id FOREIGN KEY (rest_mock_id) REFERENCES rest_mock(id)
);

-- REST_MOCK_DEF_RES_HDR table
CREATE TABLE rest_mock_def_res_hdr (
                                       restful_mock_definition_order_id BIGINT NOT NULL,
                                       response_headers VARCHAR(255),
                                       response_headers_key VARCHAR(255) NOT NULL,

                                       CONSTRAINT pk_rest_mock_def_res_hdr PRIMARY KEY (restful_mock_definition_order_id, response_headers_key),
                                       CONSTRAINT fk_rest_mock_def_res_hdr_def_id FOREIGN KEY (restful_mock_definition_order_id) REFERENCES rest_mock_def(id)
);

-- REST_MOCK_JS_HANDLER table
CREATE TABLE rest_mock_js_handler (
                                      id BIGINT NOT NULL,
                                      date_created TIMESTAMP NOT NULL,
                                      ext_id VARCHAR(255) NOT NULL,
                                      last_updated TIMESTAMP,
                                      syntax TEXT,
                                      rest_mock_id BIGINT NOT NULL,
                                      created_by BIGINT,

                                      CONSTRAINT pk_rest_mock_js_handler PRIMARY KEY (id),
                                      CONSTRAINT uq_rest_mock_js_handler_ext_id UNIQUE (ext_id),
                                      CONSTRAINT fk_rest_mock_js_handler_rest_mock_id FOREIGN KEY (rest_mock_id) REFERENCES rest_mock(id),
                                      CONSTRAINT fk_rest_mock_js_handler_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id)
);

-- REST_MOCK_RULE table
CREATE TABLE rest_mock_rule (
                                id BIGINT NOT NULL,
                                date_created TIMESTAMP NOT NULL,
                                ext_id VARCHAR(255) NOT NULL,
                                last_updated TIMESTAMP,
                                http_status_code INTEGER NOT NULL,
                                order_no INTEGER NOT NULL,
                                response_body TEXT,
                                response_content_type VARCHAR(100) NOT NULL,
                                sleep_in_millis BIGINT NOT NULL,
                                suspend BOOLEAN NOT NULL DEFAULT FALSE,
                                rest_mock_id BIGINT NOT NULL,

                                CONSTRAINT pk_rest_mock_rule PRIMARY KEY (id),
                                CONSTRAINT uq_rest_mock_rule_ext_id UNIQUE (ext_id),
                                CONSTRAINT fk_rest_mock_rule_rest_mock_id FOREIGN KEY (rest_mock_id) REFERENCES rest_mock(id)
);

-- REST_MOCK_RULE_GRP table
CREATE TABLE rest_mock_rule_grp (
                                    id BIGINT NOT NULL,
                                    date_created TIMESTAMP NOT NULL,
                                    ext_id VARCHAR(255) NOT NULL,
                                    last_updated TIMESTAMP,
                                    order_no INTEGER NOT NULL,
                                    rest_mock_rule_id BIGINT NOT NULL,

                                    CONSTRAINT pk_rest_mock_rule_grp PRIMARY KEY (id),
                                    CONSTRAINT uq_rest_mock_rule_grp_ext_id UNIQUE (ext_id),
                                    CONSTRAINT fk_rest_mock_rule_grp_rule_id FOREIGN KEY (rest_mock_rule_id) REFERENCES rest_mock_rule(id)
);

-- REST_MOCK_RULE_GRP_COND table
CREATE TABLE rest_mock_rule_grp_cond (
                                         id BIGINT NOT NULL,
                                         date_created TIMESTAMP NOT NULL,
                                         ext_id VARCHAR(255) NOT NULL,
                                         last_updated TIMESTAMP,
                                         is_case_stiv BOOLEAN,
                                         comp VARCHAR(15) NOT NULL,
                                         data_type VARCHAR(15) NOT NULL,
                                         field VARCHAR(200),
                                         match_value VARCHAR(5000),
                                         match_on VARCHAR(22) NOT NULL,
                                         rest_mock_rule_grp_id BIGINT NOT NULL,

                                         CONSTRAINT pk_rest_mock_rule_grp_cond PRIMARY KEY (id),
                                         CONSTRAINT uq_rest_mock_rule_grp_cond_ext_id UNIQUE (ext_id),
                                         CONSTRAINT fk_rest_mock_rule_grp_cond_grp_id FOREIGN KEY (rest_mock_rule_grp_id) REFERENCES rest_mock_rule_grp(id)
);

-- REST_MOCK_RULE_RES_HDR table
CREATE TABLE rest_mock_rule_res_hdr (
                                        restful_mock_definition_rule_id BIGINT NOT NULL,
                                        response_headers VARCHAR(255),
                                        response_headers_key VARCHAR(255) NOT NULL,

                                        CONSTRAINT pk_rest_mock_rule_res_hdr PRIMARY KEY (restful_mock_definition_rule_id, response_headers_key),
                                        CONSTRAINT fk_rest_mock_rule_res_hdr_rule_id FOREIGN KEY (restful_mock_definition_rule_id) REFERENCES rest_mock_rule(id)
);

-- REST_MOCK_STATEFUL_META table
CREATE TABLE rest_mock_stateful_meta (
                                         id BIGINT NOT NULL,
                                         date_created TIMESTAMP NOT NULL,
                                         ext_id VARCHAR(255) NOT NULL,
                                         last_updated TIMESTAMP,
                                         id_field_location VARCHAR(200),
                                         id_field_name VARCHAR(35) NOT NULL,
                                         initial_response_body TEXT,
                                         rest_mock_id BIGINT NOT NULL,

                                         CONSTRAINT pk_rest_mock_stateful_meta PRIMARY KEY (id),
                                         CONSTRAINT uq_rest_mock_stateful_meta_ext_id UNIQUE (ext_id),
                                         CONSTRAINT fk_rest_mock_stateful_meta_rest_mock_id FOREIGN KEY (rest_mock_id) REFERENCES rest_mock(id)
);

-- S3_MOCK table
CREATE TABLE s3_mock (
                         id BIGINT NOT NULL,
                         date_created TIMESTAMP NOT NULL,
                         ext_id VARCHAR(255) NOT NULL,
                         last_updated TIMESTAMP,
                         bucket VARCHAR(80) NOT NULL,
                         rec_status VARCHAR(15) NOT NULL,
                         sync_mode VARCHAR(15) NOT NULL,
                         created_by BIGINT NOT NULL,

                         CONSTRAINT pk_s3_mock PRIMARY KEY (id),
                         CONSTRAINT uq_s3_mock_ext_id UNIQUE (ext_id),
                         CONSTRAINT uq_s3_mock_bucket UNIQUE (bucket),
                         CONSTRAINT fk_s3_mock_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id)
);

-- S3_MOCK_DIR table
CREATE TABLE s3_mock_dir (
                             id BIGINT NOT NULL,
                             date_created TIMESTAMP NOT NULL,
                             ext_id VARCHAR(255) NOT NULL,
                             last_updated TIMESTAMP,
                             name VARCHAR(80) NOT NULL,
                             s3_mock_dir_parent BIGINT,
                             s3_mock BIGINT,

                             CONSTRAINT pk_s3_mock_dir PRIMARY KEY (id),
                             CONSTRAINT uq_s3_mock_dir_ext_id UNIQUE (ext_id),
                             CONSTRAINT fk_s3_mock_dir_parent FOREIGN KEY (s3_mock_dir_parent) REFERENCES s3_mock_dir(id),
                             CONSTRAINT fk_s3_mock_dir_s3_mock FOREIGN KEY (s3_mock) REFERENCES s3_mock(id)
);

-- S3_MOCK_FILE table
CREATE TABLE s3_mock_file (
                              id BIGINT NOT NULL,
                              date_created TIMESTAMP NOT NULL,
                              ext_id VARCHAR(255) NOT NULL,
                              last_updated TIMESTAMP,
                              mime_type VARCHAR(50) NOT NULL,
                              name VARCHAR(200) NOT NULL,
                              s3_mock_id BIGINT,
                              s3_mock_dir_id BIGINT,

                              CONSTRAINT pk_s3_mock_file PRIMARY KEY (id),
                              CONSTRAINT uq_s3_mock_file_ext_id UNIQUE (ext_id),
                              CONSTRAINT fk_s3_mock_file_s3_mock_id FOREIGN KEY (s3_mock_id) REFERENCES s3_mock(id),
                              CONSTRAINT fk_s3_mock_file_s3_mock_dir_id FOREIGN KEY (s3_mock_dir_id) REFERENCES s3_mock_dir(id)
);

-- S3_MOCK_FILE_CONTENT table
CREATE TABLE s3_mock_file_content (
                                      id BIGINT NOT NULL,
                                      date_created TIMESTAMP NOT NULL,
                                      ext_id VARCHAR(255) NOT NULL,
                                      last_updated TIMESTAMP,
                                      content TEXT NOT NULL,
                                      s3_mock_file_id BIGINT NOT NULL,

                                      CONSTRAINT pk_s3_mock_file_content PRIMARY KEY (id),
                                      CONSTRAINT uq_s3_mock_file_content_ext_id UNIQUE (ext_id),
                                      CONSTRAINT fk_s3_mock_file_content_file_id FOREIGN KEY (s3_mock_file_id) REFERENCES s3_mock_file(id)
);

-- USER_KEY_VALUE_DATA table
CREATE TABLE user_key_value_data (
                                     id BIGINT NOT NULL,
                                     date_created TIMESTAMP NOT NULL,
                                     ext_id VARCHAR(255) NOT NULL,
                                     last_updated TIMESTAMP,
                                     user_key VARCHAR(50) NOT NULL,
                                     user_value TEXT NOT NULL,
                                     created_by BIGINT NOT NULL,

                                     CONSTRAINT pk_user_key_value_data PRIMARY KEY (id),
                                     CONSTRAINT uq_user_key_value_data_ext_id UNIQUE (ext_id),
                                     CONSTRAINT uq_user_key_value_data_key_user UNIQUE (user_key, created_by),
                                     CONSTRAINT fk_user_key_value_data_created_by FOREIGN KEY (created_by) REFERENCES smkn_user(id)
);

-- Create indexes for performance optimization
CREATE INDEX idx_mail_mock_msg_mail_mock_id ON mail_mock_msg(mail_mock_id);
CREATE INDEX idx_mail_mock_msg_atch_msg_id ON mail_mock_msg_atch(mail_mock_msg_id);
CREATE INDEX idx_rest_mock_created_by ON rest_mock(created_by);
CREATE INDEX idx_rest_mock_proj_id ON rest_mock(proj_id);
CREATE INDEX idx_rest_mock_def_rest_mock_id ON rest_mock_def(rest_mock_id);
CREATE INDEX idx_rest_mock_rule_rest_mock_id ON rest_mock_rule(rest_mock_id);
CREATE INDEX idx_s3_mock_created_by ON s3_mock(created_by);
CREATE INDEX idx_user_key_value_data_created_by ON user_key_value_data(created_by);
