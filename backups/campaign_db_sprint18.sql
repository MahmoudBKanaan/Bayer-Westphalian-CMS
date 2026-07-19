--
-- PostgreSQL database dump
--

\restrict ubnjgrm9J46OqhYYlvnVAcMmazh0zLJeq4CthTqLUv9N1yDYcgyES58MDIGIq9r

-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: ai_recommendation_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.ai_recommendation_type AS ENUM (
    'PRODUCT',
    'SEGMENT',
    'COPY',
    'RISK',
    'DUPLICATE_WARNING'
);


ALTER TYPE public.ai_recommendation_type OWNER TO bwc_app;

--
-- Name: campaign_channel; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.campaign_channel AS ENUM (
    'EMAIL',
    'PHONE',
    'SMS',
    'MIXED'
);


ALTER TYPE public.campaign_channel OWNER TO bwc_app;

--
-- Name: campaign_recipient_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.campaign_recipient_status AS ENUM (
    'ELIGIBLE',
    'EXCLUDED',
    'SENT',
    'OPENED',
    'CLICKED',
    'REPLIED',
    'CONVERTED',
    'FAILED'
);


ALTER TYPE public.campaign_recipient_status OWNER TO bwc_app;

--
-- Name: campaign_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.campaign_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED',
    'ACTIVE',
    'PAUSED',
    'COMPLETED',
    'ARCHIVED'
);


ALTER TYPE public.campaign_status OWNER TO bwc_app;

--
-- Name: communication_channel; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.communication_channel AS ENUM (
    'EMAIL',
    'SMS',
    'PHONE',
    'IN_APP'
);


ALTER TYPE public.communication_channel OWNER TO bwc_app;

--
-- Name: consent_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.consent_status AS ENUM (
    'GIVEN',
    'WITHDRAWN',
    'REQUIRED',
    'EXPIRED',
    'REJECTED'
);


ALTER TYPE public.consent_status OWNER TO bwc_app;

--
-- Name: consent_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.consent_type AS ENUM (
    'MARKETING_EMAIL',
    'MARKETING_PHONE',
    'MARKETING_SMS',
    'GUARDIAN',
    'DATA_PROCESSING'
);


ALTER TYPE public.consent_type OWNER TO bwc_app;

--
-- Name: contact_event_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.contact_event_type AS ENUM (
    'SENT',
    'OPENED',
    'CLICKED',
    'REPLIED',
    'FAILED',
    'UNSUBSCRIBED',
    'CALLED',
    'NOTE'
);


ALTER TYPE public.contact_event_type OWNER TO bwc_app;

--
-- Name: contact_outcome; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.contact_outcome AS ENUM (
    'INTERESTED',
    'NOT_INTERESTED',
    'CONVERTED',
    'NO_RESPONSE',
    'FAILED'
);


ALTER TYPE public.contact_outcome OWNER TO bwc_app;

--
-- Name: customer_age_group; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.customer_age_group AS ENUM (
    'MINOR',
    '18_25',
    '26_40',
    '41_60',
    '60_PLUS'
);


ALTER TYPE public.customer_age_group OWNER TO bwc_app;

--
-- Name: customer_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.customer_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'INTERESTED',
    'UNINTERESTED',
    'CONVERTED'
);


ALTER TYPE public.customer_status OWNER TO bwc_app;

--
-- Name: customer_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.customer_type AS ENUM (
    'CUSTOMER',
    'PROSPECT',
    'BENEFICIARY'
);


ALTER TYPE public.customer_type OWNER TO bwc_app;

--
-- Name: follow_up_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.follow_up_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
);


ALTER TYPE public.follow_up_status OWNER TO bwc_app;

--
-- Name: ownership_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.ownership_status AS ENUM (
    'ACTIVE',
    'EXPIRED',
    'CANCELLED'
);


ALTER TYPE public.ownership_status OWNER TO bwc_app;

--
-- Name: payment_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.payment_status AS ENUM (
    'DUE',
    'PAID',
    'OVERDUE',
    'DEFAULT_RISK'
);


ALTER TYPE public.payment_status OWNER TO bwc_app;

--
-- Name: product_change_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.product_change_status AS ENUM (
    'OPEN',
    'APPROVED',
    'REJECTED',
    'IMPLEMENTED'
);


ALTER TYPE public.product_change_status OWNER TO bwc_app;

--
-- Name: product_change_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.product_change_type AS ENUM (
    'PRICE_CHANGE',
    'DURATION_CHANGE',
    'EXPIRATION_RULE_CHANGE',
    'STATUS_CHANGE'
);


ALTER TYPE public.product_change_type OWNER TO bwc_app;

--
-- Name: product_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.product_type AS ENUM (
    'HOMEOWNER_INSURANCE',
    'LIFE_INSURANCE',
    'INVESTMENT_FUND',
    'HEALTH_INSURANCE',
    'AUTO_INSURANCE',
    'OTHER'
);


ALTER TYPE public.product_type OWNER TO bwc_app;

--
-- Name: reminder_level; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.reminder_level AS ENUM (
    'GREEN',
    'YELLOW',
    'RED'
);


ALTER TYPE public.reminder_level OWNER TO bwc_app;

--
-- Name: reminder_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.reminder_status AS ENUM (
    'PENDING',
    'SENT',
    'FAILED',
    'CANCELLED'
);


ALTER TYPE public.reminder_status OWNER TO bwc_app;

--
-- Name: reminder_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.reminder_type AS ENUM (
    'PAYMENT_DUE',
    'PRODUCT_EXPIRATION'
);


ALTER TYPE public.reminder_type OWNER TO bwc_app;

--
-- Name: report_export_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.report_export_status AS ENUM (
    'REQUESTED',
    'COMPLETED',
    'FAILED'
);


ALTER TYPE public.report_export_status OWNER TO bwc_app;

--
-- Name: report_export_type; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.report_export_type AS ENUM (
    'CSV',
    'PDF'
);


ALTER TYPE public.report_export_type OWNER TO bwc_app;

--
-- Name: segment_join_operator; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.segment_join_operator AS ENUM (
    'AND',
    'OR'
);


ALTER TYPE public.segment_join_operator OWNER TO bwc_app;

--
-- Name: segment_operator; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.segment_operator AS ENUM (
    'EQUALS',
    'NOT_EQUALS',
    'CONTAINS',
    'IN',
    'BETWEEN',
    'BEFORE',
    'AFTER'
);


ALTER TYPE public.segment_operator OWNER TO bwc_app;

--
-- Name: segment_visibility; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.segment_visibility AS ENUM (
    'PRIVATE',
    'TEAM',
    'GLOBAL'
);


ALTER TYPE public.segment_visibility OWNER TO bwc_app;

--
-- Name: system_role_name; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.system_role_name AS ENUM (
    'ADMIN',
    'CAMPAIGN_MANAGER',
    'BI_ANALYST',
    'PRODUCT_MANAGER',
    'COMPLIANCE_OFFICER',
    'CUSTOMER_SERVICE_AGENT',
    'SALES_AGENT',
    'MARKETING_ANALYST',
    'EXECUTIVE_VIEWER',
    'SYSTEM_AUDITOR'
);


ALTER TYPE public.system_role_name OWNER TO bwc_app;

--
-- Name: user_status; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.user_status AS ENUM (
    'ACTIVE',
    'DISABLED',
    'LOCKED'
);


ALTER TYPE public.user_status OWNER TO bwc_app;

--
-- Name: work_priority; Type: TYPE; Schema: public; Owner: bwc_app
--

CREATE TYPE public.work_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH'
);


ALTER TYPE public.work_priority OWNER TO bwc_app;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_recommendations; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.ai_recommendations (
    id uuid NOT NULL,
    recommendation_type public.ai_recommendation_type NOT NULL,
    target_entity_type character varying(100) NOT NULL,
    target_entity_id uuid,
    input_summary text NOT NULL,
    recommendation text NOT NULL,
    explanation text NOT NULL,
    confidence_score numeric(5,2),
    approved_by_user_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    review_notes text,
    CONSTRAINT ai_recommendations_confidence_score_range CHECK (((confidence_score IS NULL) OR ((confidence_score >= (0)::numeric) AND (confidence_score <= (100)::numeric)))),
    CONSTRAINT ai_recommendations_explanation_not_blank CHECK ((length(TRIM(BOTH FROM explanation)) > 0)),
    CONSTRAINT ai_recommendations_input_summary_not_blank CHECK ((length(TRIM(BOTH FROM input_summary)) > 0)),
    CONSTRAINT ai_recommendations_recommendation_not_blank CHECK ((length(TRIM(BOTH FROM recommendation)) > 0)),
    CONSTRAINT ai_recommendations_target_entity_type_not_blank CHECK ((length(TRIM(BOTH FROM target_entity_type)) > 0))
);


ALTER TABLE public.ai_recommendations OWNER TO bwc_app;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.audit_logs (
    id uuid NOT NULL,
    actor_user_id uuid,
    action character varying(255) NOT NULL,
    entity_type character varying(100) NOT NULL,
    entity_id uuid,
    old_value jsonb,
    new_value jsonb,
    ip_address character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.audit_logs OWNER TO bwc_app;

--
-- Name: beneficiaries; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.beneficiaries (
    id uuid NOT NULL,
    policyholder_customer_id uuid NOT NULL,
    beneficiary_customer_id uuid NOT NULL,
    relationship character varying(100) NOT NULL,
    guardian_name character varying(255),
    guardian_email character varying(255),
    guardian_consent_required boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT beneficiaries_distinct_customers CHECK ((policyholder_customer_id <> beneficiary_customer_id))
);


ALTER TABLE public.beneficiaries OWNER TO bwc_app;

--
-- Name: bwc_schema_metadata; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.bwc_schema_metadata (
    id bigint NOT NULL,
    schema_version character varying(32) NOT NULL,
    description character varying(255) NOT NULL,
    installed_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.bwc_schema_metadata OWNER TO bwc_app;

--
-- Name: bwc_schema_metadata_id_seq; Type: SEQUENCE; Schema: public; Owner: bwc_app
--

CREATE SEQUENCE public.bwc_schema_metadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.bwc_schema_metadata_id_seq OWNER TO bwc_app;

--
-- Name: bwc_schema_metadata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: bwc_app
--

ALTER SEQUENCE public.bwc_schema_metadata_id_seq OWNED BY public.bwc_schema_metadata.id;


--
-- Name: campaign_metrics; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.campaign_metrics (
    id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    audience_size integer DEFAULT 0 NOT NULL,
    eligible_count integer DEFAULT 0 NOT NULL,
    excluded_count integer DEFAULT 0 NOT NULL,
    sent_count integer DEFAULT 0 NOT NULL,
    opened_count integer DEFAULT 0 NOT NULL,
    clicked_count integer DEFAULT 0 NOT NULL,
    replied_count integer DEFAULT 0 NOT NULL,
    converted_count integer DEFAULT 0 NOT NULL,
    estimated_cost numeric(12,2),
    estimated_revenue numeric(12,2),
    estimated_roi numeric(12,2),
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.campaign_metrics OWNER TO bwc_app;

--
-- Name: campaign_products; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.campaign_products (
    campaign_id uuid NOT NULL,
    product_id uuid NOT NULL
);


ALTER TABLE public.campaign_products OWNER TO bwc_app;

--
-- Name: campaign_recipients; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.campaign_recipients (
    id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    eligibility_status public.campaign_recipient_status NOT NULL,
    exclusion_reason text,
    eligibility_explanation text,
    sent_at timestamp with time zone,
    opened_at timestamp with time zone,
    clicked_at timestamp with time zone,
    converted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.campaign_recipients OWNER TO bwc_app;

--
-- Name: campaigns; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.campaigns (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    objective text NOT NULL,
    status public.campaign_status DEFAULT 'DRAFT'::public.campaign_status NOT NULL,
    owner_user_id uuid,
    segment_id uuid,
    channel public.campaign_channel NOT NULL,
    message_subject character varying(255),
    message_body text,
    start_date date,
    end_date date,
    approved_by uuid,
    approved_at timestamp with time zone,
    rejection_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    compliance_review_notes text
);


ALTER TABLE public.campaigns OWNER TO bwc_app;

--
-- Name: COLUMN campaigns.compliance_review_notes; Type: COMMENT; Schema: public; Owner: bwc_app
--

COMMENT ON COLUMN public.campaigns.compliance_review_notes IS 'Optional Compliance Officer review notes captured during approve/reject (item 231).';


--
-- Name: consent_records; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.consent_records (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    consent_type public.consent_type NOT NULL,
    status public.consent_status NOT NULL,
    purpose text NOT NULL,
    source character varying(100),
    granted_at timestamp with time zone,
    withdrawn_at timestamp with time zone,
    expires_at timestamp with time zone,
    evidence_file_url text,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consent_records_expiration_after_grant CHECK (((expires_at IS NULL) OR (granted_at IS NULL) OR (expires_at > granted_at))),
    CONSTRAINT consent_records_withdrawal_after_grant CHECK (((withdrawn_at IS NULL) OR (granted_at IS NULL) OR (withdrawn_at >= granted_at)))
);


ALTER TABLE public.consent_records OWNER TO bwc_app;

--
-- Name: contact_events; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.contact_events (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    campaign_id uuid,
    channel public.communication_channel NOT NULL,
    event_type public.contact_event_type NOT NULL,
    outcome public.contact_outcome,
    notes text,
    occurred_at timestamp with time zone NOT NULL,
    created_by uuid
);


ALTER TABLE public.contact_events OWNER TO bwc_app;

--
-- Name: customers; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.customers (
    id uuid NOT NULL,
    customer_type public.customer_type NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    email character varying(255),
    phone character varying(50),
    address_line character varying(255),
    city character varying(100),
    country character varying(100),
    date_of_birth date,
    age_group public.customer_age_group,
    status public.customer_status DEFAULT 'ACTIVE'::public.customer_status NOT NULL,
    do_not_contact boolean DEFAULT false NOT NULL,
    source character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    status_changed_at timestamp with time zone
);


ALTER TABLE public.customers OWNER TO bwc_app;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO bwc_app;

--
-- Name: follow_up_tasks; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.follow_up_tasks (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    campaign_id uuid,
    assigned_to uuid,
    title character varying(255) NOT NULL,
    description text,
    due_date date,
    status public.follow_up_status DEFAULT 'OPEN'::public.follow_up_status NOT NULL,
    priority public.work_priority DEFAULT 'MEDIUM'::public.work_priority NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    completed_at timestamp with time zone
);


ALTER TABLE public.follow_up_tasks OWNER TO bwc_app;

--
-- Name: payment_records; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.payment_records (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    product_ownership_id uuid NOT NULL,
    due_date date NOT NULL,
    paid_at timestamp with time zone,
    amount_due numeric(12,2) NOT NULL,
    amount_paid numeric(12,2),
    status public.payment_status DEFAULT 'DUE'::public.payment_status NOT NULL,
    reminder_count integer DEFAULT 0 NOT NULL,
    CONSTRAINT payment_records_amount_due_non_negative CHECK ((amount_due >= (0)::numeric)),
    CONSTRAINT payment_records_amount_paid_non_negative CHECK (((amount_paid IS NULL) OR (amount_paid >= (0)::numeric))),
    CONSTRAINT payment_records_reminder_count_non_negative CHECK ((reminder_count >= 0))
);


ALTER TABLE public.payment_records OWNER TO bwc_app;

--
-- Name: product_change_requests; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.product_change_requests (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    requested_by uuid,
    request_type public.product_change_type NOT NULL,
    description text NOT NULL,
    status public.product_change_status DEFAULT 'OPEN'::public.product_change_status NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT product_change_requests_description_not_blank CHECK ((length(TRIM(BOTH FROM description)) > 0))
);


ALTER TABLE public.product_change_requests OWNER TO bwc_app;

--
-- Name: product_ownerships; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.product_ownerships (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    product_id uuid NOT NULL,
    policy_number character varying(100),
    start_date date NOT NULL,
    expiration_date date,
    status public.ownership_status DEFAULT 'ACTIVE'::public.ownership_status NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT product_ownerships_expiration_after_start CHECK (((expiration_date IS NULL) OR (expiration_date >= start_date)))
);


ALTER TABLE public.product_ownerships OWNER TO bwc_app;

--
-- Name: products; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.products (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    product_type public.product_type NOT NULL,
    description text,
    price numeric(12,2),
    duration_months integer,
    expiration_policy character varying(100),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT products_duration_positive CHECK (((duration_months IS NULL) OR (duration_months > 0))),
    CONSTRAINT products_price_non_negative CHECK (((price IS NULL) OR (price >= (0)::numeric)))
);


ALTER TABLE public.products OWNER TO bwc_app;

--
-- Name: reminder_schedules; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.reminder_schedules (
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    product_id uuid NOT NULL,
    reminder_type public.reminder_type NOT NULL,
    reminder_level public.reminder_level NOT NULL,
    scheduled_date date NOT NULL,
    status public.reminder_status DEFAULT 'PENDING'::public.reminder_status NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    sent_at timestamp with time zone
);


ALTER TABLE public.reminder_schedules OWNER TO bwc_app;

--
-- Name: report_exports; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.report_exports (
    id uuid NOT NULL,
    requested_by uuid,
    report_name character varying(255) NOT NULL,
    export_type public.report_export_type NOT NULL,
    status public.report_export_status DEFAULT 'REQUESTED'::public.report_export_status NOT NULL,
    file_url text,
    requested_at timestamp with time zone DEFAULT now() NOT NULL,
    completed_at timestamp with time zone
);


ALTER TABLE public.report_exports OWNER TO bwc_app;

--
-- Name: roles; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.roles (
    id uuid NOT NULL,
    name public.system_role_name NOT NULL,
    description text NOT NULL,
    display_name character varying(100) NOT NULL,
    allowed_functions text NOT NULL,
    mvp_role boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.roles OWNER TO bwc_app;

--
-- Name: segment_criteria; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.segment_criteria (
    id uuid NOT NULL,
    segment_id uuid NOT NULL,
    field_name character varying(100) NOT NULL,
    operator public.segment_operator NOT NULL,
    value text NOT NULL,
    logical_group character varying(50),
    join_operator public.segment_join_operator DEFAULT 'AND'::public.segment_join_operator NOT NULL,
    CONSTRAINT segment_criteria_field_name_not_blank CHECK ((length(TRIM(BOTH FROM field_name)) > 0)),
    CONSTRAINT segment_criteria_logical_group_not_blank CHECK (((logical_group IS NULL) OR (length(TRIM(BOTH FROM logical_group)) > 0))),
    CONSTRAINT segment_criteria_value_not_blank CHECK ((length(TRIM(BOTH FROM value)) > 0))
);


ALTER TABLE public.segment_criteria OWNER TO bwc_app;

--
-- Name: segments; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.segments (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    owner_user_id uuid,
    visibility public.segment_visibility DEFAULT 'PRIVATE'::public.segment_visibility NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT segments_name_not_blank CHECK ((length(TRIM(BOTH FROM name)) > 0)),
    CONSTRAINT segments_updated_at_after_created_at CHECK ((updated_at >= created_at))
);


ALTER TABLE public.segments OWNER TO bwc_app;

--
-- Name: system_settings; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.system_settings (
    id uuid NOT NULL,
    monthly_contact_limit integer NOT NULL,
    send_retry_limit integer NOT NULL,
    uninterested_exclusion_days integer NOT NULL,
    updated_by_user_id uuid,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT system_settings_monthly_contact_limit_chk CHECK (((monthly_contact_limit >= 1) AND (monthly_contact_limit <= 100))),
    CONSTRAINT system_settings_send_retry_limit_chk CHECK (((send_retry_limit >= 1) AND (send_retry_limit <= 20))),
    CONSTRAINT system_settings_uninterested_exclusion_days_chk CHECK (((uninterested_exclusion_days >= 1) AND (uninterested_exclusion_days <= 3650)))
);


ALTER TABLE public.system_settings OWNER TO bwc_app;

--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp with time zone DEFAULT now() NOT NULL,
    assigned_by uuid
);


ALTER TABLE public.user_roles OWNER TO bwc_app;

--
-- Name: users; Type: TABLE; Schema: public; Owner: bwc_app
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    status public.user_status DEFAULT 'ACTIVE'::public.user_status NOT NULL,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.users OWNER TO bwc_app;

--
-- Name: bwc_schema_metadata id; Type: DEFAULT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.bwc_schema_metadata ALTER COLUMN id SET DEFAULT nextval('public.bwc_schema_metadata_id_seq'::regclass);


--
-- Data for Name: ai_recommendations; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.ai_recommendations (id, recommendation_type, target_entity_type, target_entity_id, input_summary, recommendation, explanation, confidence_score, approved_by_user_id, created_at, review_notes) FROM stdin;
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.audit_logs (id, actor_user_id, action, entity_type, entity_id, old_value, new_value, ip_address, created_at) FROM stdin;
\.


--
-- Data for Name: beneficiaries; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.beneficiaries (id, policyholder_customer_id, beneficiary_customer_id, relationship, guardian_name, guardian_email, guardian_consent_required, created_at) FROM stdin;
\.


--
-- Data for Name: bwc_schema_metadata; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.bwc_schema_metadata (id, schema_version, description, installed_at) FROM stdin;
1	v0.2	Initial KB schema initialized	2026-07-16 14:01:40.977449+00
\.


--
-- Data for Name: campaign_metrics; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.campaign_metrics (id, campaign_id, audience_size, eligible_count, excluded_count, sent_count, opened_count, clicked_count, replied_count, converted_count, estimated_cost, estimated_revenue, estimated_roi, updated_at) FROM stdin;
\.


--
-- Data for Name: campaign_products; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.campaign_products (campaign_id, product_id) FROM stdin;
\.


--
-- Data for Name: campaign_recipients; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.campaign_recipients (id, campaign_id, customer_id, eligibility_status, exclusion_reason, eligibility_explanation, sent_at, opened_at, clicked_at, converted_at, created_at) FROM stdin;
\.


--
-- Data for Name: campaigns; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.campaigns (id, name, objective, status, owner_user_id, segment_id, channel, message_subject, message_body, start_date, end_date, approved_by, approved_at, rejection_reason, created_at, updated_at, compliance_review_notes) FROM stdin;
\.


--
-- Data for Name: consent_records; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.consent_records (id, customer_id, consent_type, status, purpose, source, granted_at, withdrawn_at, expires_at, evidence_file_url, created_by, created_at) FROM stdin;
\.


--
-- Data for Name: contact_events; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.contact_events (id, customer_id, campaign_id, channel, event_type, outcome, notes, occurred_at, created_by) FROM stdin;
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.customers (id, customer_type, first_name, last_name, email, phone, address_line, city, country, date_of_birth, age_group, status, do_not_contact, source, created_at, updated_at, deleted_at, status_changed_at) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create initial schema	SQL	V1__create_initial_schema.sql	799123168	bwc_app	2026-07-16 14:01:40.868275	682	t
2	2	enhance roles table	SQL	V2__enhance_roles_table.sql	1530809207	bwc_app	2026-07-16 14:01:41.699128	31	t
3	3	enhance user roles table	SQL	V3__enhance_user_roles_table.sql	799387073	bwc_app	2026-07-16 14:01:41.75314	28	t
4	4	enhance customers table	SQL	V4__enhance_customers_table.sql	-76184064	bwc_app	2026-07-16 14:01:41.797439	36	t
5	5	enhance beneficiaries table	SQL	V5__enhance_beneficiaries_table.sql	-107952566	bwc_app	2026-07-16 14:01:41.847814	19	t
6	6	enhance consent records table	SQL	V6__enhance_consent_records_table.sql	-1782174342	bwc_app	2026-07-16 14:01:41.876492	32	t
7	7	enhance products table	SQL	V7__enhance_products_table.sql	-875059668	bwc_app	2026-07-16 14:01:41.920123	33	t
8	8	enhance product ownerships table	SQL	V8__enhance_product_ownerships_table.sql	-1437939854	bwc_app	2026-07-16 14:01:41.967823	30	t
9	9	enhance product change requests table	SQL	V9__enhance_product_change_requests_table.sql	1206935384	bwc_app	2026-07-16 14:01:42.006397	41	t
10	10	enhance payment records table	SQL	V10__enhance_payment_records_table.sql	-584696855	bwc_app	2026-07-16 14:01:42.056533	46	t
11	11	enhance segments table	SQL	V11__enhance_segments_table.sql	-842184937	bwc_app	2026-07-16 14:01:42.135244	106	t
12	12	enhance segment criteria table	SQL	V12__enhance_segment_criteria_table.sql	-1624267569	bwc_app	2026-07-16 14:01:42.301377	57	t
13	13	enhance ai recommendations table	SQL	V13__enhance_ai_recommendations_table.sql	-1073939575	bwc_app	2026-07-16 14:01:42.370379	28	t
14	14	add kb foreign key constraints	SQL	V14__add_kb_foreign_key_constraints.sql	343543730	bwc_app	2026-07-16 14:01:42.40767	18	t
15	15	add kb search filter indexes	SQL	V15__add_kb_search_filter_indexes.sql	-293555133	bwc_app	2026-07-16 14:01:42.452395	193	t
16	16	seed kb roles	SQL	V16__seed_kb_roles.sql	-1216516066	bwc_app	2026-07-16 14:01:42.658667	3	t
17	17	seed mvp role users	SQL	V17__seed_mvp_role_users.sql	-1404632064	bwc_app	2026-07-16 14:01:42.67447	8	t
18	18	reset admin password	SQL	V18__reset_admin_password.sql	1979824462	bwc_app	2026-07-16 14:01:42.691544	2	t
19	19	reset demo login passwords	SQL	V19__reset_demo_login_passwords.sql	245541912	bwc_app	2026-07-16 14:01:42.717948	4	t
20	20	reset all test account passwords	SQL	V20__reset_all_test_account_passwords.sql	-1586578724	bwc_app	2026-07-16 14:01:42.732339	2	t
21	21	seed all role test accounts and reset password	SQL	V21__seed_all_role_test_accounts_and_reset_password.sql	-2146513836	bwc_app	2026-07-16 14:01:42.743087	7	t
22	22	add campaign compliance review notes	SQL	V22__add_campaign_compliance_review_notes.sql	-1130293596	bwc_app	2026-07-16 14:01:42.769531	3	t
23	23	add ai recommendation review notes	SQL	V23__add_ai_recommendation_review_notes.sql	193686792	bwc_app	2026-07-16 14:01:42.780844	2	t
24	24	create system settings table	SQL	V24__create_system_settings_table.sql	-1807014799	bwc_app	2026-07-16 14:01:42.790255	14	t
25	25	add customer status changed at	SQL	V25__add_customer_status_changed_at.sql	-1810390753	bwc_app	2026-07-16 14:01:42.818361	13	t
\.


--
-- Data for Name: follow_up_tasks; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.follow_up_tasks (id, customer_id, campaign_id, assigned_to, title, description, due_date, status, priority, created_at, completed_at) FROM stdin;
\.


--
-- Data for Name: payment_records; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.payment_records (id, customer_id, product_ownership_id, due_date, paid_at, amount_due, amount_paid, status, reminder_count) FROM stdin;
\.


--
-- Data for Name: product_change_requests; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.product_change_requests (id, product_id, requested_by, request_type, description, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: product_ownerships; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.product_ownerships (id, customer_id, product_id, policy_number, start_date, expiration_date, status, created_at) FROM stdin;
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.products (id, name, product_type, description, price, duration_months, expiration_policy, active, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: reminder_schedules; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.reminder_schedules (id, customer_id, product_id, reminder_type, reminder_level, scheduled_date, status, created_at, sent_at) FROM stdin;
\.


--
-- Data for Name: report_exports; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.report_exports (id, requested_by, report_name, export_type, status, file_url, requested_at, completed_at) FROM stdin;
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.roles (id, name, description, display_name, allowed_functions, mvp_role, created_at, updated_at) FROM stdin;
00000000-0000-0000-0000-000000000001	ADMIN	Manages users, roles, settings, and full system configuration	Admin	Manage users, assign roles, manage settings, view all modules, configure limits, view audit logs	t	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000002	CAMPAIGN_MANAGER	Creates campaigns, segments, recipients, messages, schedules, and launches approved campaigns	Campaign Manager	Create/edit campaigns, define segments, preview recipients, submit campaigns, launch approved campaigns, manage follow-ups, view campaign analytics	t	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000003	BI_ANALYST	Views dashboards, reports, customer analytics, segmentation insights, and performance data	BI Analyst	View analytics, reports, segmentation insights, audience counts, campaign performance, product performance; may create analytical segment drafts if allowed	t	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000004	PRODUCT_MANAGER	Manages insurance/investment products and product-change requests	Product Manager	Create/edit/disable products, manage product details, create product-change requests, view product performance	t	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000005	COMPLIANCE_OFFICER	Reviews consent, opt-outs, eligibility, campaign approval, and audit logs	Compliance Officer	Review consent, opt-outs, guardian consent, eligibility, approve/reject campaigns, view audit logs and compliance reports	t	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000006	CUSTOMER_SERVICE_AGENT	Manages customer/prospect details, notes, contact outcomes, and consent updates	Customer Service Agent	Create/update customers, update contact details, record consent, mark opt-outs, add notes, update contact outcomes, manage follow-up tasks	t	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000007	SALES_AGENT	Follows up with assigned interested prospects and updates conversion status	Sales Agent	View assigned leads, update contact outcomes, mark interested/not interested/converted, complete follow-up tasks	f	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000008	MARKETING_ANALYST	Reviews campaign metrics, audience behavior, and campaign performance	Marketing Analyst	View campaign metrics, audience segment performance, reports, segmentation insights, and recommend targeting improvements	f	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000009	EXECUTIVE_VIEWER	Views high-level dashboards and management reports only	Executive Viewer	View read-only dashboards, ROI, campaign summaries, and product performance reports	f	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
00000000-0000-0000-0000-000000000010	SYSTEM_AUDITOR	Reviews audit logs, consent history, approval history, and sensitive actions	System Auditor	View audit logs, consent history, campaign approval history, user activity history, and export audit reports	f	2026-07-16 14:01:41.710065+00	2026-07-16 14:01:42.663163+00
\.


--
-- Data for Name: segment_criteria; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.segment_criteria (id, segment_id, field_name, operator, value, logical_group, join_operator) FROM stdin;
\.


--
-- Data for Name: segments; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.segments (id, name, description, owner_user_id, visibility, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: system_settings; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.system_settings (id, monthly_contact_limit, send_retry_limit, uninterested_exclusion_days, updated_by_user_id, created_at, updated_at) FROM stdin;
a1000000-0000-0000-0000-000000000001	3	3	90	\N	2026-07-16 14:01:42.797904+00	2026-07-16 14:01:42.797904+00
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.user_roles (user_id, role_id, assigned_at, assigned_by) FROM stdin;
10000000-0000-0000-0000-000000000001	00000000-0000-0000-0000-000000000001	2026-07-16 14:01:42.678425+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000002	00000000-0000-0000-0000-000000000002	2026-07-16 14:01:42.678425+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000003	00000000-0000-0000-0000-000000000003	2026-07-16 14:01:42.678425+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000004	00000000-0000-0000-0000-000000000004	2026-07-16 14:01:42.678425+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000005	00000000-0000-0000-0000-000000000005	2026-07-16 14:01:42.678425+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000006	00000000-0000-0000-0000-000000000006	2026-07-16 14:01:42.678425+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000007	00000000-0000-0000-0000-000000000007	2026-07-16 14:01:42.748058+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000008	00000000-0000-0000-0000-000000000008	2026-07-16 14:01:42.748058+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000009	00000000-0000-0000-0000-000000000009	2026-07-16 14:01:42.748058+00	10000000-0000-0000-0000-000000000001
10000000-0000-0000-0000-000000000010	00000000-0000-0000-0000-000000000010	2026-07-16 14:01:42.748058+00	10000000-0000-0000-0000-000000000001
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: bwc_app
--

COPY public.users (id, email, password_hash, full_name, status, last_login_at, created_at, updated_at) FROM stdin;
10000000-0000-0000-0000-000000000002	campaign.manager@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Campaign Manager	ACTIVE	\N	2026-07-16 14:01:42.678425+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000004	product.manager@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Product Manager	ACTIVE	\N	2026-07-16 14:01:42.678425+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000005	compliance.officer@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Compliance Officer	ACTIVE	\N	2026-07-16 14:01:42.678425+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000006	customer.service@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Customer Service Agent	ACTIVE	\N	2026-07-16 14:01:42.678425+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000007	sales.agent@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Sales Agent	ACTIVE	\N	2026-07-16 14:01:42.748058+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000008	marketing.analyst@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Marketing Analyst	ACTIVE	\N	2026-07-16 14:01:42.748058+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000009	executive.viewer@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Executive Viewer	ACTIVE	\N	2026-07-16 14:01:42.748058+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000010	system.auditor@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test System Auditor	ACTIVE	\N	2026-07-16 14:01:42.748058+00	2026-07-16 14:01:42.748058+00
10000000-0000-0000-0000-000000000003	bi.analyst@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test BI Analyst	ACTIVE	2026-07-16 14:11:06.145731+00	2026-07-16 14:01:42.678425+00	2026-07-16 14:11:06.154614+00
10000000-0000-0000-0000-000000000001	admin@bayer-westphalian.test	$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm	Test Admin	ACTIVE	2026-07-16 14:12:17.591538+00	2026-07-16 14:01:42.678425+00	2026-07-16 14:12:17.605559+00
\.


--
-- Name: bwc_schema_metadata_id_seq; Type: SEQUENCE SET; Schema: public; Owner: bwc_app
--

SELECT pg_catalog.setval('public.bwc_schema_metadata_id_seq', 1, true);


--
-- Name: ai_recommendations ai_recommendations_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.ai_recommendations
    ADD CONSTRAINT ai_recommendations_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: beneficiaries beneficiaries_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.beneficiaries
    ADD CONSTRAINT beneficiaries_pkey PRIMARY KEY (id);


--
-- Name: beneficiaries beneficiaries_unique_link; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.beneficiaries
    ADD CONSTRAINT beneficiaries_unique_link UNIQUE (policyholder_customer_id, beneficiary_customer_id);


--
-- Name: bwc_schema_metadata bwc_schema_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.bwc_schema_metadata
    ADD CONSTRAINT bwc_schema_metadata_pkey PRIMARY KEY (id);


--
-- Name: campaign_metrics campaign_metrics_campaign_id_key; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_metrics
    ADD CONSTRAINT campaign_metrics_campaign_id_key UNIQUE (campaign_id);


--
-- Name: campaign_metrics campaign_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_metrics
    ADD CONSTRAINT campaign_metrics_pkey PRIMARY KEY (id);


--
-- Name: campaign_products campaign_products_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_products
    ADD CONSTRAINT campaign_products_pkey PRIMARY KEY (campaign_id, product_id);


--
-- Name: campaign_recipients campaign_recipients_campaign_customer_unique; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_recipients
    ADD CONSTRAINT campaign_recipients_campaign_customer_unique UNIQUE (campaign_id, customer_id);


--
-- Name: campaign_recipients campaign_recipients_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_recipients
    ADD CONSTRAINT campaign_recipients_pkey PRIMARY KEY (id);


--
-- Name: campaigns campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_pkey PRIMARY KEY (id);


--
-- Name: consent_records consent_records_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.consent_records
    ADD CONSTRAINT consent_records_pkey PRIMARY KEY (id);


--
-- Name: contact_events contact_events_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.contact_events
    ADD CONSTRAINT contact_events_pkey PRIMARY KEY (id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: follow_up_tasks follow_up_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.follow_up_tasks
    ADD CONSTRAINT follow_up_tasks_pkey PRIMARY KEY (id);


--
-- Name: payment_records payment_records_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.payment_records
    ADD CONSTRAINT payment_records_pkey PRIMARY KEY (id);


--
-- Name: product_change_requests product_change_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_change_requests
    ADD CONSTRAINT product_change_requests_pkey PRIMARY KEY (id);


--
-- Name: product_ownerships product_ownerships_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_ownerships
    ADD CONSTRAINT product_ownerships_pkey PRIMARY KEY (id);


--
-- Name: product_ownerships product_ownerships_policy_number_unique; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_ownerships
    ADD CONSTRAINT product_ownerships_policy_number_unique UNIQUE (policy_number);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: reminder_schedules reminder_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.reminder_schedules
    ADD CONSTRAINT reminder_schedules_pkey PRIMARY KEY (id);


--
-- Name: report_exports report_exports_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.report_exports
    ADD CONSTRAINT report_exports_pkey PRIMARY KEY (id);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: segment_criteria segment_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.segment_criteria
    ADD CONSTRAINT segment_criteria_pkey PRIMARY KEY (id);


--
-- Name: segments segments_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.segments
    ADD CONSTRAINT segments_pkey PRIMARY KEY (id);


--
-- Name: system_settings system_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: audit_logs_actor_created_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX audit_logs_actor_created_idx ON public.audit_logs USING btree (actor_user_id, created_at);


--
-- Name: audit_logs_entity_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX audit_logs_entity_idx ON public.audit_logs USING btree (entity_type, entity_id);


--
-- Name: campaign_recipients_status_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX campaign_recipients_status_idx ON public.campaign_recipients USING btree (campaign_id, eligibility_status);


--
-- Name: campaigns_status_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX campaigns_status_idx ON public.campaigns USING btree (status);


--
-- Name: consent_records_customer_status_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX consent_records_customer_status_idx ON public.consent_records USING btree (customer_id, status);


--
-- Name: contact_events_customer_occurred_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX contact_events_customer_occurred_idx ON public.contact_events USING btree (customer_id, occurred_at);


--
-- Name: customers_email_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX customers_email_idx ON public.customers USING btree (email);


--
-- Name: customers_name_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX customers_name_idx ON public.customers USING btree (last_name, first_name);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: follow_up_tasks_assignee_status_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX follow_up_tasks_assignee_status_idx ON public.follow_up_tasks USING btree (assigned_to, status);


--
-- Name: idx_ai_recommendations_approved_by; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_ai_recommendations_approved_by ON public.ai_recommendations USING btree (approved_by_user_id);


--
-- Name: idx_ai_recommendations_created_at; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_ai_recommendations_created_at ON public.ai_recommendations USING btree (created_at);


--
-- Name: idx_ai_recommendations_target; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_ai_recommendations_target ON public.ai_recommendations USING btree (target_entity_type, target_entity_id);


--
-- Name: idx_ai_recommendations_type; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_ai_recommendations_type ON public.ai_recommendations USING btree (recommendation_type);


--
-- Name: idx_beneficiaries_beneficiary_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_beneficiaries_beneficiary_customer ON public.beneficiaries USING btree (beneficiary_customer_id);


--
-- Name: idx_beneficiaries_guardian_consent_required; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_beneficiaries_guardian_consent_required ON public.beneficiaries USING btree (guardian_consent_required);


--
-- Name: idx_beneficiaries_policyholder_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_beneficiaries_policyholder_customer ON public.beneficiaries USING btree (policyholder_customer_id);


--
-- Name: idx_campaign_products_product; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaign_products_product ON public.campaign_products USING btree (product_id);


--
-- Name: idx_campaign_recipients_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaign_recipients_customer ON public.campaign_recipients USING btree (customer_id);


--
-- Name: idx_campaign_recipients_customer_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaign_recipients_customer_status ON public.campaign_recipients USING btree (customer_id, eligibility_status);


--
-- Name: idx_campaigns_approved_by; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaigns_approved_by ON public.campaigns USING btree (approved_by);


--
-- Name: idx_campaigns_owner_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaigns_owner_status ON public.campaigns USING btree (owner_user_id, status);


--
-- Name: idx_campaigns_segment; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaigns_segment ON public.campaigns USING btree (segment_id);


--
-- Name: idx_campaigns_status_dates; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_campaigns_status_dates ON public.campaigns USING btree (status, start_date, end_date);


--
-- Name: idx_consent_records_created_by; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_consent_records_created_by ON public.consent_records USING btree (created_by);


--
-- Name: idx_consent_records_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_consent_records_customer ON public.consent_records USING btree (customer_id);


--
-- Name: idx_consent_records_customer_type_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_consent_records_customer_type_status ON public.consent_records USING btree (customer_id, consent_type, status);


--
-- Name: idx_consent_records_expires_at; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_consent_records_expires_at ON public.consent_records USING btree (expires_at);


--
-- Name: idx_consent_records_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_consent_records_status ON public.consent_records USING btree (status);


--
-- Name: idx_contact_events_campaign; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_contact_events_campaign ON public.contact_events USING btree (campaign_id);


--
-- Name: idx_contact_events_campaign_occurred; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_contact_events_campaign_occurred ON public.contact_events USING btree (campaign_id, occurred_at);


--
-- Name: idx_contact_events_created_by; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_contact_events_created_by ON public.contact_events USING btree (created_by);


--
-- Name: idx_customers_city; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_city ON public.customers USING btree (city);


--
-- Name: idx_customers_do_not_contact; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_do_not_contact ON public.customers USING btree (do_not_contact);


--
-- Name: idx_customers_email; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_email ON public.customers USING btree (email);


--
-- Name: idx_customers_phone; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_phone ON public.customers USING btree (phone);


--
-- Name: idx_customers_search_name; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_search_name ON public.customers USING btree (last_name, first_name, email);


--
-- Name: idx_customers_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_status ON public.customers USING btree (status);


--
-- Name: idx_customers_status_changed_at; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_status_changed_at ON public.customers USING btree (status_changed_at);


--
-- Name: idx_customers_status_deleted; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_status_deleted ON public.customers USING btree (status, deleted_at);


--
-- Name: idx_customers_type_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_customers_type_status ON public.customers USING btree (customer_type, status);


--
-- Name: idx_follow_up_tasks_campaign; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_follow_up_tasks_campaign ON public.follow_up_tasks USING btree (campaign_id);


--
-- Name: idx_follow_up_tasks_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_follow_up_tasks_customer ON public.follow_up_tasks USING btree (customer_id);


--
-- Name: idx_follow_up_tasks_status_due; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_follow_up_tasks_status_due ON public.follow_up_tasks USING btree (status, due_date);


--
-- Name: idx_payment_records_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_payment_records_customer ON public.payment_records USING btree (customer_id);


--
-- Name: idx_payment_records_customer_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_payment_records_customer_status ON public.payment_records USING btree (customer_id, status);


--
-- Name: idx_payment_records_ownership; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_payment_records_ownership ON public.payment_records USING btree (product_ownership_id);


--
-- Name: idx_payment_records_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_payment_records_status ON public.payment_records USING btree (status);


--
-- Name: idx_product_change_requests_product; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_change_requests_product ON public.product_change_requests USING btree (product_id);


--
-- Name: idx_product_change_requests_product_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_change_requests_product_status ON public.product_change_requests USING btree (product_id, status);


--
-- Name: idx_product_change_requests_requested_by; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_change_requests_requested_by ON public.product_change_requests USING btree (requested_by);


--
-- Name: idx_product_change_requests_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_change_requests_status ON public.product_change_requests USING btree (status);


--
-- Name: idx_product_ownership_expiration; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_ownership_expiration ON public.product_ownerships USING btree (expiration_date);


--
-- Name: idx_product_ownerships_customer_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_ownerships_customer_status ON public.product_ownerships USING btree (customer_id, status);


--
-- Name: idx_product_ownerships_product; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_ownerships_product ON public.product_ownerships USING btree (product_id);


--
-- Name: idx_product_ownerships_product_expiration; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_ownerships_product_expiration ON public.product_ownerships USING btree (product_id, expiration_date);


--
-- Name: idx_product_ownerships_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_ownerships_status ON public.product_ownerships USING btree (status);


--
-- Name: idx_product_ownerships_status_expiration; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_product_ownerships_status_expiration ON public.product_ownerships USING btree (status, expiration_date);


--
-- Name: idx_products_active; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_products_active ON public.products USING btree (active);


--
-- Name: idx_products_deleted_at; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_products_deleted_at ON public.products USING btree (deleted_at);


--
-- Name: idx_products_name; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_products_name ON public.products USING btree (name);


--
-- Name: idx_products_name_type_active; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_products_name_type_active ON public.products USING btree (name, product_type, active);


--
-- Name: idx_products_type; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_products_type ON public.products USING btree (product_type);


--
-- Name: idx_reminder_schedules_customer; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_reminder_schedules_customer ON public.reminder_schedules USING btree (customer_id);


--
-- Name: idx_reminder_schedules_customer_date; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_reminder_schedules_customer_date ON public.reminder_schedules USING btree (customer_id, scheduled_date);


--
-- Name: idx_reminder_schedules_product; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_reminder_schedules_product ON public.reminder_schedules USING btree (product_id);


--
-- Name: idx_reminder_schedules_product_date; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_reminder_schedules_product_date ON public.reminder_schedules USING btree (product_id, scheduled_date);


--
-- Name: idx_reminder_schedules_status_date; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_reminder_schedules_status_date ON public.reminder_schedules USING btree (status, scheduled_date);


--
-- Name: idx_report_exports_requested_by; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_report_exports_requested_by ON public.report_exports USING btree (requested_by);


--
-- Name: idx_report_exports_status_requested; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_report_exports_status_requested ON public.report_exports USING btree (status, requested_at);


--
-- Name: idx_segment_criteria_field_name; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segment_criteria_field_name ON public.segment_criteria USING btree (field_name);


--
-- Name: idx_segment_criteria_operator; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segment_criteria_operator ON public.segment_criteria USING btree (operator);


--
-- Name: idx_segment_criteria_segment; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segment_criteria_segment ON public.segment_criteria USING btree (segment_id);


--
-- Name: idx_segment_criteria_segment_field; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segment_criteria_segment_field ON public.segment_criteria USING btree (segment_id, field_name);


--
-- Name: idx_segments_name; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segments_name ON public.segments USING btree (name);


--
-- Name: idx_segments_owner_user; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segments_owner_user ON public.segments USING btree (owner_user_id);


--
-- Name: idx_segments_owner_visibility; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segments_owner_visibility ON public.segments USING btree (owner_user_id, visibility);


--
-- Name: idx_segments_visibility; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_segments_visibility ON public.segments USING btree (visibility);


--
-- Name: idx_user_roles_role; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_user_roles_role ON public.user_roles USING btree (role_id);


--
-- Name: idx_users_full_name; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_users_full_name ON public.users USING btree (full_name);


--
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- Name: payment_records_due_status_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX payment_records_due_status_idx ON public.payment_records USING btree (due_date, status);


--
-- Name: product_ownerships_customer_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX product_ownerships_customer_idx ON public.product_ownerships USING btree (customer_id);


--
-- Name: products_type_active_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX products_type_active_idx ON public.products USING btree (product_type, active);


--
-- Name: reminder_schedules_date_status_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX reminder_schedules_date_status_idx ON public.reminder_schedules USING btree (scheduled_date, status);


--
-- Name: user_roles_assigned_by_idx; Type: INDEX; Schema: public; Owner: bwc_app
--

CREATE INDEX user_roles_assigned_by_idx ON public.user_roles USING btree (assigned_by);


--
-- Name: ai_recommendations ai_recommendations_approved_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.ai_recommendations
    ADD CONSTRAINT ai_recommendations_approved_by_user_id_fkey FOREIGN KEY (approved_by_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: audit_logs audit_logs_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: beneficiaries beneficiaries_beneficiary_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.beneficiaries
    ADD CONSTRAINT beneficiaries_beneficiary_customer_id_fkey FOREIGN KEY (beneficiary_customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: beneficiaries beneficiaries_policyholder_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.beneficiaries
    ADD CONSTRAINT beneficiaries_policyholder_customer_id_fkey FOREIGN KEY (policyholder_customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: campaign_metrics campaign_metrics_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_metrics
    ADD CONSTRAINT campaign_metrics_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_products campaign_products_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_products
    ADD CONSTRAINT campaign_products_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_products campaign_products_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_products
    ADD CONSTRAINT campaign_products_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE RESTRICT;


--
-- Name: campaign_recipients campaign_recipients_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_recipients
    ADD CONSTRAINT campaign_recipients_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: campaign_recipients campaign_recipients_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaign_recipients
    ADD CONSTRAINT campaign_recipients_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: campaigns campaigns_approved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: campaigns campaigns_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: campaigns campaigns_segment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_segment_id_fkey FOREIGN KEY (segment_id) REFERENCES public.segments(id) ON DELETE SET NULL;


--
-- Name: consent_records consent_records_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.consent_records
    ADD CONSTRAINT consent_records_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: consent_records consent_records_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.consent_records
    ADD CONSTRAINT consent_records_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: contact_events contact_events_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.contact_events
    ADD CONSTRAINT contact_events_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE SET NULL;


--
-- Name: contact_events contact_events_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.contact_events
    ADD CONSTRAINT contact_events_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: contact_events contact_events_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.contact_events
    ADD CONSTRAINT contact_events_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: follow_up_tasks follow_up_tasks_assigned_to_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.follow_up_tasks
    ADD CONSTRAINT follow_up_tasks_assigned_to_fkey FOREIGN KEY (assigned_to) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: follow_up_tasks follow_up_tasks_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.follow_up_tasks
    ADD CONSTRAINT follow_up_tasks_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE SET NULL;


--
-- Name: follow_up_tasks follow_up_tasks_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.follow_up_tasks
    ADD CONSTRAINT follow_up_tasks_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: payment_records payment_records_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.payment_records
    ADD CONSTRAINT payment_records_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: payment_records payment_records_product_ownership_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.payment_records
    ADD CONSTRAINT payment_records_product_ownership_id_fkey FOREIGN KEY (product_ownership_id) REFERENCES public.product_ownerships(id) ON DELETE CASCADE;


--
-- Name: product_change_requests product_change_requests_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_change_requests
    ADD CONSTRAINT product_change_requests_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE;


--
-- Name: product_change_requests product_change_requests_requested_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_change_requests
    ADD CONSTRAINT product_change_requests_requested_by_fkey FOREIGN KEY (requested_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: product_ownerships product_ownerships_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_ownerships
    ADD CONSTRAINT product_ownerships_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: product_ownerships product_ownerships_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.product_ownerships
    ADD CONSTRAINT product_ownerships_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE RESTRICT;


--
-- Name: reminder_schedules reminder_schedules_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.reminder_schedules
    ADD CONSTRAINT reminder_schedules_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: reminder_schedules reminder_schedules_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.reminder_schedules
    ADD CONSTRAINT reminder_schedules_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE RESTRICT;


--
-- Name: report_exports report_exports_requested_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.report_exports
    ADD CONSTRAINT report_exports_requested_by_fkey FOREIGN KEY (requested_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: segment_criteria segment_criteria_segment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.segment_criteria
    ADD CONSTRAINT segment_criteria_segment_id_fkey FOREIGN KEY (segment_id) REFERENCES public.segments(id) ON DELETE CASCADE;


--
-- Name: segments segments_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.segments
    ADD CONSTRAINT segments_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: system_settings system_settings_updated_by_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_updated_by_user_fk FOREIGN KEY (updated_by_user_id) REFERENCES public.users(id);


--
-- Name: user_roles user_roles_assigned_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE RESTRICT;


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: bwc_app
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict ubnjgrm9J46OqhYYlvnVAcMmazh0zLJeq4CthTqLUv9N1yDYcgyES58MDIGIq9r

