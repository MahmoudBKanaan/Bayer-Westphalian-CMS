Bayer-Westphalian Campaign Management Platform — Complete Knowledge Base
1. Project Identity
Item
Definition
Project name
Bayer-Westphalian Campaign Management Platform
System type
Internal enterprise CRM, campaign management, and marketing automation system
Business domain
Insurance marketing and business intelligence
Primary users
Internal Bayer-Westphalian employees
Main business case
Market insurance and investment products to grandchildren/beneficiaries of life-insurance payout customers while also supporting automated campaigns
Product status
Operation-ready business system; mock providers allowed only for development/testing
The system is an internal business application for Bayer-Westphalian Insurance. It allows authorized employees to manage customers, prospects, beneficiaries, consent, products, campaigns, segmentation, reminders, contact history, follow-ups, analytics, reports, and AI-assisted recommendations.
The system is not a public customer portal. It does not include public signup. Employee accounts are created and managed by an Admin.
2.1 Scrum Adaptation
Because this is a solo developer project, Scrum is adapted as follows:
Scrum role
Solo-project interpretation
Product Owner
The developer defines scope, prioritizes backlog, manages product value, and represents stakeholder needs from the task and KB
Scrum Master
The developer maintains the Scrum process, sprint cadence, board, impediment log, review notes, and retrospective notes
Developer
The developer designs, implements, tests, documents, and deploys the system
QA/Test role
The developer writes and executes unit, integration, security, frontend, and E2E tests
Stakeholders
Represented by the role personas in the KB: Admin, Campaign Manager, BI Analyst, Product Manager, Compliance Officer, Customer Service Agent, Sales Agent, Executive Viewer, System Auditor
The project must not be presented as a fake multi-person Scrum team. It should be documented honestly as a solo-adapted Scrum process.
2.2 Scrum Board Columns
Use GitHub Projects, Jira, Trello, or a spreadsheet with these columns:
Column
Meaning
Product Backlog
All known work items
Sprint Backlog
Items selected for current sprint
In Progress
Currently being implemented
Blocked
Work blocked by unresolved issue
Self Review
Code/design reviewed by the solo developer
Testing
Unit/integration/frontend/E2E testing
Done
Meets Definition of Done
2.3 Scrum Ceremonies
Ceremony
Solo execution
Sprint Planning
Select sprint goal, backlog items, acceptance criteria, and expected increment
Daily Scrum
5-minute self-check: what was done, what is next, what is blocked
Backlog Refinement
Update priorities, split large stories, clarify acceptance criteria
Sprint Review
Demonstrate the increment using screenshots, running app, tests, or diagrams
Sprint Retrospective
Record what worked, what failed, what will change next sprint
Release Review
Verify whether the release is production-ready or still requires hardening
2.4 Definition of Ready
A backlog item is ready for sprint implementation only when:
Requirement
The user story is clearly written
The related role/persona is known
The business value is clear
Acceptance criteria are written
Required API endpoint is known
Required database entity or migration is known
Required frontend screen/component is known
Required permissions are known
Validation rules are known
Audit requirements are known if the feature is sensitive
Test approach is known
The item is small enough for one sprint
2.5 Definition of Done
A feature is done only when:
Requirement
Backend API is implemented
Database migration exists if needed
Frontend UI is implemented if needed
Frontend validation exists
Backend validation exists
Role authorization is enforced on the backend
Frontend route protection exists
Error handling works
Sensitive actions are audited
Unit or integration tests exist
Feature is documented
Feature is demo-ready
Feature does not break existing tests
The feature is merged into the main branch only after self-review
3. Release Strategy
3.1 Release Versions
Release
Goal
Main content
v0.1
Project foundation
Repository, architecture, Docker, base apps
v0.2
Secure access
Auth, users, roles, protected routes
v0.3
CRM and compliance base
Customers, beneficiaries, consent, opt-outs
v0.4
Products and segmentation
Products, ownership, payments, segment builder
v0.5
Campaign lifecycle
Campaign builder, approval, recipient preview
v0.6
Communication and reminders
Contact history, provider adapter, reminders
v0.7
Analytics and AI
Dashboards, reports, AI-assisted features
v0.8
Audit and hardening
Audit logs, security hardening, access tests
v0.9
Production candidate
Full tests, CI/CD, deployment, docs
v1.0
Production-ready MVP
Deployed, documented, tested, demo-ready
4. Product Backlog Structure
4.1 Epics
Epic ID
Epic
E01
Agile planning and product documentation
E02
Repository, environment, and architecture setup
E03
Database schema and migrations
E04
Backend foundation
E05
Frontend foundation
E06
Authentication, users, and roles
E07
Customer and prospect management
E08
Beneficiary management
E09
Consent, opt-out, and eligibility
E10
Product management
E11
Product ownership and payment records
E12
Segmentation
E13
Campaign management
E14
Compliance review
E15
Recipient preview and campaign launch
E16
Communication tracking
E17
Follow-up management
E18
Reminder scheduling
E19
Analytics
E20
Reports
E21
AI-assisted features
E22
Audit logging
E23
Security hardening
E24
Testing and quality assurance
E25
CI/CD
E26
Production deployment
E27
Documentation and university report
2. Scope
2.1 Fully Implemented Core Modules
Module
Description
Authentication and roles
Secure employee login, JWT/session handling, role-based access
User management
Admin creates, disables, edits users and assigns roles
Customer/prospect management
CRUD, search, filters, soft delete, profile view
Beneficiary management
Link beneficiaries/grandchildren to life-insurance customers
Consent management
Record consent, opt-outs, guardian consent, do-not-contact status
Product management
CRUD for insurance and investment products
Product ownership
Track products owned by customers and expiration dates
Product change requests
Request product data changes from policy management
Segmentation
Create target groups by demographics, location, behavior, products, payment history, consent, expiration
Campaign management
Create, edit, submit, approve, launch, pause, complete, archive campaigns
Recipient preview
Show eligible and excluded recipients before campaign launch
Communication tracking
Record emails, calls, SMS, outcomes, replies, conversions
Follow-up management
Create and complete follow-up tasks for interested prospects
Reminder scheduling
Payment reminders and product-expiration campaign reminders
Analytics
Campaign KPIs, engagement, conversion, ROI, product performance
Reports
CSV/PDF exports and dashboard reports
Audit logging
Track sensitive actions, consent changes, approvals, role changes
AI-assisted features
Fuzzy search, segment suggestions, product recommendations, risk scoring, copy suggestions with human approval
2.2 Development/Test Mocking Only
Area
Development/test behavior
Production behavior
Email sending
Mock sender logs sent events
Real SMTP/provider integration
SMS sending
Mock SMS event
Real SMS provider integration
Payment data
Seed data or CSV import
Real billing/policy system integration or controlled import
Open/click events
Manual/demo events
Provider webhook or tracking integration
Conversion tracking
Manual conversion status
Real sales/policy integration
AI campaign text
Template/rule-based output
Real approved AI service or rule engine
Consent evidence file
Local placeholder
Secure file storage
Production must not simulate consent, opt-outs, eligibility, campaign approval, audit logs, permissions, or customer communication history.
3. Product Vision
Element
Content
Vision
A secure CRM-based campaign automation platform that enables insurance teams to manage customers, consent, segmentation, campaigns, reminders, follow-ups, analytics, and reports in one integrated system.
Main goal
Increase campaign relevance and conversion while preventing non-compliant or excessive marketing.
Primary value
Controlled marketing automation with compliance-aware targeting.
4. Business Goals
Goal
Measurement
Improve campaign targeting
Higher conversion rate
Reduce duplicate marketing
Fewer repeated contacts
Improve compliance
All contacts checked against consent and opt-out rules
Improve productivity
Faster campaign creation and approval
Improve reporting
Management can view ROI, engagement, and conversion
Improve customer experience
Customers receive relevant and controlled communication
5. Stakeholders
Stakeholder
Interest
Power
Priority
Product Owner / BI Unit
Useful campaign system
High
High
Campaign Managers
Create campaigns efficiently
High
High
BI Analysts
Analyze segments and performance
Medium
High
Compliance Officers
Ensure consent and legal control
High
High
Customer Service Agents
Maintain accurate customer data
Medium
Medium
Product Managers
Promote and maintain product data
Medium
Medium
Sales Agents
Follow up with interested prospects
Medium
Medium
Executive Management
View ROI and business impact
High
Medium
IT Operations
Maintain secure and stable system
Medium
Medium
Customers/prospects
Receive relevant and respectful communication
Medium
High
Beneficiaries/grandchildren
May receive offers if eligible
Medium
High
Guardians
Give consent for minors where required
Medium
High
6. User Roles
Role
Description
Admin
Manages users, roles, settings, and full system configuration
Campaign Manager
Creates campaigns, segments, recipients, messages, schedules, and launches approved campaigns
BI Analyst
Views dashboards, reports, customer analytics, segmentation insights, and performance data
Product Manager
Manages insurance/investment products and product-change requests
Compliance Officer
Reviews consent, opt-outs, eligibility, campaign approval, and audit logs
Customer Service Agent
Manages customer/prospect details, notes, contact outcomes, and consent updates
Sales Agent
Follows up with assigned interested prospects and updates conversion status
Marketing Analyst
Reviews campaign metrics, audience behavior, and campaign performance
Executive Viewer
Views high-level dashboards and management reports only
System Auditor
Reviews audit logs, consent history, approval history, and sensitive actions
MVP roles: Admin, Campaign Manager, BI Analyst, Product Manager, Compliance Officer, Customer Service Agent.
Extended enterprise roles: Sales Agent, Marketing Analyst, Executive Viewer, System Auditor.
7. Role Function Access Summary
Role
Allowed functions
Admin
Manage users, assign roles, manage settings, view all modules, configure limits, view audit logs
Campaign Manager
Create/edit campaigns, define segments, preview recipients, submit campaigns, launch approved campaigns, manage follow-ups, view campaign analytics
BI Analyst
View analytics, reports, segmentation insights, audience counts, campaign performance, product performance; may create analytical segment drafts if allowed
Product Manager
Create/edit/disable products, manage product details, create product-change requests, view product performance
Compliance Officer
Review consent, opt-outs, guardian consent, eligibility, approve/reject campaigns, view audit logs and compliance reports
Customer Service Agent
Create/update customers, update contact details, record consent, mark opt-outs, add notes, update contact outcomes, manage follow-up tasks
Sales Agent
View assigned leads, update contact outcomes, mark interested/not interested/converted, complete follow-up tasks
Marketing Analyst
View campaign metrics, audience segment performance, reports, segmentation insights, and recommend targeting improvements
Executive Viewer
View read-only dashboards, ROI, campaign summaries, and product performance reports
System Auditor
View audit logs, consent history, campaign approval history, user activity history, and export audit reports
8. Role 
→
 Screen 
→
 Function Access
Role
Screen
Functions
Admin
Login
Log in, log out
Admin
Dashboard
View all KPIs, campaigns, reminders, alerts
Admin
User Management
Create users, edit users, disable users, assign roles, reset passwords
Admin
Role Settings
Configure role permissions and access rules
Admin
Customers
View, search, filter, create, edit, soft-delete
Admin
Products
View, create, edit, disable/delete
Admin
Campaigns
View all, edit if required, pause/cancel if required
Admin
Segmentation
Create, edit, delete, preview, save segments
Admin
Compliance Review
View approval status; override only if logged
Admin
Analytics/Reports
View and export all reports
Admin
Audit Log
View all sensitive actions
Admin
System Settings
Configure contact limits, reminder rules, campaign settings
Campaign Manager
Login
Log in, log out
Campaign Manager
Dashboard
View campaign KPIs, active campaigns, reminders, audience counts
Campaign Manager
Customers
View, search, filter
Campaign Manager
Customer Details
View profile, consent status, product ownership, contact history
Campaign Manager
Campaigns
Create, view, edit drafts, pause, archive
Campaign Manager
Campaign Builder
Define name, objective, product, segment, message, schedule
Campaign Manager
Segmentation
Create, edit, preview, save target segments
Campaign Manager
Recipient Preview
View eligible/excluded recipients and exclusion reasons
Campaign Manager
Compliance Submission
Submit campaign for approval
Campaign Manager
Contact History
View communication timeline
Campaign Manager
Follow-Up Tasks
Create, assign, update follow-ups
Campaign Manager
Reminders
Create payment and product-expiration reminder campaigns
Campaign Manager
Analytics/Reports
View campaign performance
BI Analyst
Login
Log in, log out
BI Analyst
Dashboard
View BI KPIs and trends
BI Analyst
Analytics/Reports
View engagement, conversion, ROI, product performance
BI Analyst
Segmentation Insights
View segment size, patterns, exclusions, historical performance
BI Analyst
Customer Analytics
View aggregated statistics
BI Analyst
Campaign Analytics
View campaign effectiveness
BI Analyst
Segmentation
Read-only by default; optional analytical draft creation
BI Analyst
Recipient Preview
View aggregated eligible/excluded counts
Product Manager
Login
Log in, log out
Product Manager
Dashboard
View product KPIs
Product Manager
Products
Create, edit, search, disable/delete products
Product Manager
Product Details
Edit price, duration, expiration rules, status
Product Manager
Product Change Requests
Create, update, track requests
Product Manager
Campaigns
View campaigns linked to products
Product Manager
Analytics/Reports
View product performance
Compliance Officer
Login
Log in, log out
Compliance Officer
Dashboard
View consent alerts and pending approvals
Compliance Officer
Consent Management
Review consent, opt-outs, guardian consent, do-not-contact
Compliance Officer
Customer Details
View profile, consent history, eligibility
Compliance Officer
Compliance Review
Approve, reject, request changes, add notes
Compliance Officer
Recipient Preview
Review eligible/excluded recipients and exclusion reasons
Compliance Officer
Campaigns
View submitted, approved, rejected campaigns
Compliance Officer
Audit Log
View consent changes, approvals, opt-outs
Compliance Officer
Reports
View compliance reports
Customer Service Agent
Login
Log in, log out
Customer Service Agent
Dashboard
View assigned tasks and reminders
Customer Service Agent
Customers
View, search, create, update customer/prospect records
Customer Service Agent
Customer Details
Edit contact details, update status, add notes
Customer Service Agent
Consent Tab
Record consent, opt-out, do-not-contact, evidence
Customer Service Agent
Contact History
Add call/email notes and outcomes
Customer Service Agent
Follow-Up Tasks
Create, update, complete tasks
Customer Service Agent
Reminders
View reminder schedules
Sales Agent
Login
Log in, log out
Sales Agent
Dashboard
View assigned leads
Sales Agent
Assigned Leads
View assigned customers/prospects
Sales Agent
Customer Details
View lead profile, interest, contact history
Sales Agent
Contact History
Add contact outcome, notes
Sales Agent
Follow-Up Tasks
Update follow-up progress
Sales Agent
Conversion Status
Mark converted, not interested, future follow-up
Marketing Analyst
Login
Log in, log out
Marketing Analyst
Dashboard
View marketing KPIs
Marketing Analyst
Campaign Analytics
View open, click, conversion, ROI
Marketing Analyst
Audience Segments
View segment performance
Marketing Analyst
Reports
Generate/export campaign reports
Marketing Analyst
Segmentation Insights
Recommend target groups
Executive Viewer
Login
Log in, log out
Executive Viewer
Executive Dashboard
View high-level KPIs
Executive Viewer
Reports
View read-only management reports
Executive Viewer
Campaign Overview
View status and performance summary
Executive Viewer
Product Performance
View product-level outcomes
System Auditor
Login
Log in, log out
System Auditor
Audit Log
View system actions and sensitive changes
System Auditor
Consent History
Review consent and opt-out changes
System Auditor
Campaign Approval History
Review approvals/rejections
System Auditor
User Activity History
View sensitive user activity
System Auditor
Reports
Export audit reports
9. Access Levels
Access level
Meaning
NONE
No access
READ
View records only
CREATE
Create new records
UPDATE
Edit existing records
DELETE
Soft-delete or disable records
APPROVE
Approve/reject controlled workflows
EXPORT
Export reports/files
ADMIN
Full administrative configuration
AUDIT
View sensitive logs and history
Delete operations are soft-delete by default. Permanent deletion is not part of the MVP and should be restricted to system-level maintenance.
10. Functional Requirements
ID
Requirement
FR-001
Users can log in with email and password
FR-002
Users have assigned roles
FR-003
Pages and APIs are restricted by role
FR-004
Users can log out securely
FR-005
Admin can create, update, disable users
FR-010
Users can view paginated customers/prospects
FR-011
Authorized users can create customers/prospects
FR-012
Authorized users can edit customer details
FR-013
Authorized users can soft-delete customers/prospects
FR-014
Users can search customers
FR-015
System supports innovative/fuzzy customer search
FR-016
Users can view customer profiles
FR-017
Users can view contact history
FR-018
Users can view consent/opt-out status
FR-019
Users can mark customer status
FR-020
Users can import customers/prospects from CSV
FR-030
Users can add beneficiaries linked to customers
FR-031
Users can store beneficiary contact details
FR-032
System tracks guardian consent requirement
FR-033
Users can record consent status
FR-034
System blocks marketing without valid consent
FR-040
Users can view products
FR-041
Product Manager/Admin can create products
FR-042
Product Manager/Admin can edit products
FR-043
Product Manager/Admin can disable/delete products
FR-044
Users can search products
FR-045
Product Manager can create product-change requests
FR-046
Products can be assigned to campaigns
FR-050
Campaign Manager can create campaigns
FR-051
Campaign Manager can define campaign objective
FR-052
Campaign Manager can select promoted products
FR-053
Campaign Manager can define target audience
FR-054
System previews eligible recipients
FR-055
System excludes opt-outs and invalid consent
FR-056
System prevents duplicate/excessive marketing
FR-057
Campaign Manager can save campaign as draft
FR-058
Campaign Manager can submit campaign for review
FR-059
Compliance Officer can approve/reject campaign
FR-060
Campaign Manager can launch approved campaign
FR-061
Campaign Manager can pause/cancel campaign
FR-062
Campaign Manager can archive completed campaign
FR-070
Users can segment by age group
FR-071
Users can segment by location
FR-072
Users can segment by customer/prospect type
FR-073
Users can segment by product ownership
FR-074
Users can segment by payment history
FR-075
Users can segment by behavior/interests
FR-076
Users can segment by product expiration
FR-077
Users can save reusable segments
FR-078
Users can combine criteria with AND/OR logic
FR-079
System previews audience size
FR-080
System schedules payment reminders
FR-081
System sends Green first reminder
FR-082
System sends Yellow second reminder
FR-083
System sends Red third reminder
FR-084
System identifies likely payment default
FR-085
Product-expiration campaigns can start 3 months before expiration
FR-086
Product-expiration campaigns can start 6 months before expiration
FR-087
Product-expiration campaigns can start 12 months before expiration
FR-088
System creates follow-up tasks after reminders
FR-089
System logs all reminder attempts
FR-090
System records contact attempts
FR-091
System shows contact timeline
FR-092
System prevents excessive contact frequency
FR-093
Users can create follow-up tasks
FR-094
Users can record contact outcomes
FR-095
Users can add communication notes
FR-096
Users can remove uninterested parties from mailing lists
FR-097
System respects do-not-contact status
FR-100
Dashboard shows campaign totals
FR-101
Dashboard shows active campaigns
FR-102
Dashboard shows audience size
FR-103
Dashboard shows messages sent
FR-104
Dashboard shows open rate
FR-105
Dashboard shows click rate
FR-106
Dashboard shows conversion rate
FR-107
Dashboard shows estimated ROI
FR-108
Users can view performance charts
FR-109
Users can export CSV reports
FR-110
Users can generate PDF reports
11. AI-Assisted Functional Requirements
ID
Feature
Implementation
AI-001
Innovative customer search
Fuzzy/weighted search using name, email, city, product, notes
AI-002
Segment suggestions
Rule-based audience suggestions from product ownership, payment history, location, expiration
AI-003
Product recommendations
Rule-based product recommendation by profile and owned products
AI-004
Default-risk score
Score from missed payments, overdue days, reminder count, payment history
AI-005
Campaign copy suggestion
Human-approved generated subject/body/call-to-action
AI-006
Duplicate-contact risk warning
Warn if contact frequency or repeated campaign rule is violated
AI must support human decision-making only. It must not automatically make final legal, financial, or marketing decisions without human approval.
12. Non-Functional Requirements
ID
Requirement
Target
NFR-001
Security
Role-based access, password hashing, JWT/session security
NFR-002
Privacy
GDPR-aware consent, opt-out, data minimization
NFR-003
Performance
Normal searches under 1 second for project dataset
NFR-004
Availability
99% target for project-level deployment
NFR-005
Usability
Clear dashboards, forms, filters, validation
NFR-006
Maintainability
Layered backend, reusable frontend components
NFR-007
Scalability
Pagination, indexes, async jobs
NFR-008
Auditability
Sensitive actions logged
NFR-009
Reliability
Failed sends can be retried
NFR-010
Testability
Unit, integration, API, UI tests
NFR-011
Accessibility
Labels, keyboard support, contrast
NFR-012
Data integrity
Foreign keys, constraints, transactions
NFR-013
Backup/recovery
Database backup strategy
NFR-014
Observability
Logs, health endpoints, error tracking
13. Business Rules
ID
Rule
BR-001
A person with do_not_contact = true must never be included in a campaign
BR-002
A person who opted out of marketing must be excluded from marketing
BR-003
A beneficiary requiring guardian consent cannot be contacted until guardian consent is valid
BR-004
Consent must include type, purpose, source, date, and status
BR-005
Campaigns cannot launch before Compliance Officer approval
BR-006
Campaigns must show recipient eligibility reasons
BR-007
Campaigns must record excluded contacts and exclusion reasons
BR-010
Same customer cannot receive the same campaign twice
BR-011
Same customer cannot receive more than the configured number of marketing messages per month
BR-012
Failed sends can be retried maximum 3 times
BR-013
Uninterested customers are excluded from similar campaigns for a configurable period
BR-014
Converted customers should not receive the same campaign again
BR-020
Green reminder is the first reminder
BR-021
Yellow reminder is the second reminder
BR-022
Red reminder is the third reminder and indicates likely default risk
BR-023
Product-expiration campaign can start 3, 6, or 12 months before expiration
BR-024
Payment reminder must not be sent if payment is completed
BR-030
Campaign must have name, objective, target segment, product, message, schedule, owner
BR-031
Draft campaign can be edited
BR-032
Submitted campaign cannot be launched before approval
BR-033
Approved campaign can be launched, paused, completed, or archived
BR-034
Campaign metrics update after contact events
14. Technical Stack
Layer
Technology
Frontend
React, TypeScript, Vite
Routing
React Router
Data fetching
TanStack Query
Forms
React Hook Form
Validation
Zod frontend, Jakarta Bean Validation backend
UI
Tailwind CSS or MUI
Charts
Recharts
Backend
Java 21, Spring Boot
API
REST JSON
Security
Spring Security
Auth
JWT access token + refresh token or secure session
Database
PostgreSQL
ORM
Spring Data JPA / Hibernate
Migrations
Flyway
Mapping
MapStruct or manual mappers
Testing backend
JUnit, Mockito, Spring Boot Test, Testcontainers
Testing frontend
Vitest, React Testing Library, Playwright
DevOps
Docker, Docker Compose, GitHub Actions
Deployment
Docker containers with Nginx or Caddy
Architecture style: modular monolith.
15. System Architecture
Layer
Responsibility
React frontend
UI, forms, dashboards, routing, client-side validation
Spring Boot backend
APIs, business logic, security, validation, scheduled jobs
PostgreSQL database
Persistent relational data
Optional provider adapters
Email, SMS, file storage, external policy/payment systems
Flow:
React Frontend 
→
 HTTPS REST API 
→
 Spring Boot Backend 
→
 JPA/Hibernate 
→
 PostgreSQL
Backend package structure:
com.bayerwestphalian.campaign
  auth
  user
  customer
  beneficiary
  consent
  product
  campaign
  segment
  schedule
  communication
  analytics
  audit
  ai
  report
  common
Frontend structure:
src
  app
  api
  components
  features
    auth
    dashboard
    customers
    products
    campaigns
    segments
    schedules
    analytics
    compliance
    users
  pages
  types
  utils
16. Complete Data Model
16.1 Data Entities
Entity/Table
Purpose
Sensitivity
Primary access owner
users
Internal employee accounts
High
Admin
roles
System roles
Medium
Admin
user_roles
User-role mapping
Medium
Admin
customers
Customers, prospects, beneficiaries
High
Campaign Manager / Customer Service
beneficiaries
Grandchild/beneficiary links
High
Customer Service / Compliance
consent_records
Consent and opt-out evidence
Critical
Compliance
products
Insurance/investment products
Medium
Product Manager
product_ownerships
Products owned by customers
High
Product Manager / Campaign Manager
product_change_requests
Product change workflow
Medium
Product Manager
payment_records
Payment history and due dates
High
Customer Service / Campaign Manager
segments
Saved audience definitions
Medium
Campaign Manager
segment_criteria
Segment filter rules
Medium
Campaign Manager
campaigns
Campaign definition and lifecycle
Medium
Campaign Manager
campaign_products
Campaign-product relationship
Medium
Campaign Manager
campaign_recipients
Final recipient eligibility/outcome
High
Campaign Manager / Compliance
contact_events
Communication history
High
Customer Service / Campaign Manager
follow_up_tasks
Follow-up work items
Medium
Customer Service / Sales
reminder_schedules
Payment/product-expiration reminders
Medium
Campaign Manager
campaign_metrics
Aggregated campaign performance
Medium
BI Analyst
audit_logs
Sensitive action history
Critical
Compliance / Auditor
report_exports
Report export history
Medium
BI Analyst / Admin
ai_recommendations
AI suggestions and explanations
Medium
BI Analyst / Campaign Manager
16.2 Entity Fields
users
Field
Type
Notes
id
UUID
Primary key
email
VARCHAR(255)
Unique, required
password_hash
VARCHAR(255)
Required, BCrypt
full_name
VARCHAR(255)
Required
status
ENUM
ACTIVE, DISABLED, LOCKED
last_login_at
TIMESTAMP
Optional
created_at
TIMESTAMP
Required
updated_at
TIMESTAMP
Required
roles
Field
Type
Notes
id
UUID
Primary key
name
ENUM
ADMIN, CAMPAIGN_MANAGER, BI_ANALYST, PRODUCT_MANAGER, COMPLIANCE_OFFICER, CUSTOMER_SERVICE_AGENT, SALES_AGENT, MARKETING_ANALYST, EXECUTIVE_VIEWER, SYSTEM_AUDITOR
description
TEXT
Optional
customers
Field
Type
Notes
id
UUID
Primary key
customer_type
ENUM
CUSTOMER, PROSPECT, BENEFICIARY
first_name
VARCHAR(100)
Required
last_name
VARCHAR(100)
Required
email
VARCHAR(255)
Optional
phone
VARCHAR(50)
Optional
address_line
VARCHAR(255)
Optional
city
VARCHAR(100)
Optional
country
VARCHAR(100)
Optional
date_of_birth
DATE
Optional
age_group
ENUM
MINOR, 18_25, 26_40, 41_60, 60_PLUS
status
ENUM
ACTIVE, INACTIVE, INTERESTED, UNINTERESTED, CONVERTED
do_not_contact
BOOLEAN
Default false
source
VARCHAR(100)
Example: LIFE_INSURANCE_BENEFICIARY
created_at
TIMESTAMP
Required
updated_at
TIMESTAMP
Required
deleted_at
TIMESTAMP
Soft delete
beneficiaries
Field
Type
Notes
id
UUID
Primary key
policyholder_customer_id
UUID
FK customers.id
beneficiary_customer_id
UUID
FK customers.id
relationship
VARCHAR(100)
Grandchild, guardian, etc.
guardian_name
VARCHAR(255)
Optional
guardian_email
VARCHAR(255)
Optional
guardian_consent_required
BOOLEAN
Required
created_at
TIMESTAMP
Required
consent_records
Field
Type
Notes
id
UUID
Primary key
customer_id
UUID
FK customers.id
consent_type
ENUM
MARKETING_EMAIL, MARKETING_PHONE, MARKETING_SMS, GUARDIAN, DATA_PROCESSING
status
ENUM
GIVEN, WITHDRAWN, REQUIRED, EXPIRED, REJECTED
purpose
TEXT
Required
source
VARCHAR(100)
Letter, phone, web form, import
granted_at
TIMESTAMP
Optional
withdrawn_at
TIMESTAMP
Optional
expires_at
TIMESTAMP
Optional
evidence_file_url
TEXT
Optional
created_by
UUID
FK users.id
created_at
TIMESTAMP
Required
products
Field
Type
Notes
id
UUID
Primary key
name
VARCHAR(255)
Required
product_type
ENUM
HOMEOWNER_INSURANCE, LIFE_INSURANCE, INVESTMENT_FUND, HEALTH_INSURANCE, AUTO_INSURANCE, OTHER
description
TEXT
Optional
price
DECIMAL(12,2)
Optional
duration_months
INTEGER
Optional
expiration_policy
VARCHAR(100)
Optional
active
BOOLEAN
Default true
created_at
TIMESTAMP
Required
updated_at
TIMESTAMP
Required
deleted_at
TIMESTAMP
Soft delete
product_ownerships
Field
Type
Notes
id
UUID
Primary key
customer_id
UUID
FK customers.id
product_id
UUID
FK products.id
policy_number
VARCHAR(100)
Optional
start_date
DATE
Required
expiration_date
DATE
Optional
status
ENUM
ACTIVE, EXPIRED, CANCELLED
created_at
TIMESTAMP
Required
product_change_requests
Field
Type
Notes
id
UUID
Primary key
product_id
UUID
FK products.id
requested_by
UUID
FK users.id
request_type
ENUM
PRICE_CHANGE, DURATION_CHANGE, EXPIRATION_RULE_CHANGE, STATUS_CHANGE
description
TEXT
Required
status
ENUM
OPEN, APPROVED, REJECTED, IMPLEMENTED
created_at
TIMESTAMP
Required
updated_at
TIMESTAMP
Required
payment_records
Field
Type
Notes
id
UUID
Primary key
customer_id
UUID
FK customers.id
product_ownership_id
UUID
FK product_ownerships.id
due_date
DATE
Required
paid_at
TIMESTAMP
Optional
amount_due
DECIMAL(12,2)
Required
amount_paid
DECIMAL(12,2)
Optional
status
ENUM
DUE, PAID, OVERDUE, DEFAULT_RISK
reminder_count
INTEGER
Default 0
segments
Field
Type
Notes
id
UUID
Primary key
name
VARCHAR(255)
Required
description
TEXT
Optional
owner_user_id
UUID
FK users.id
visibility
ENUM
PRIVATE, TEAM, GLOBAL
created_at
TIMESTAMP
Required
updated_at
TIMESTAMP
Required
segment_criteria
Field
Type
Notes
id
UUID
Primary key
segment_id
UUID
FK segments.id
field_name
VARCHAR(100)
Example: city, age_group, product_type
operator
ENUM
EQUALS, NOT_EQUALS, CONTAINS, IN, BETWEEN, BEFORE, AFTER
value
TEXT
Required
logical_group
VARCHAR(50)
Optional
join_operator
ENUM
AND, OR
campaigns
Field
Type
Notes
id
UUID
Primary key
name
VARCHAR(255)
Required
objective
TEXT
Required
status
ENUM
DRAFT, SUBMITTED, APPROVED, REJECTED, ACTIVE, PAUSED, COMPLETED, ARCHIVED
owner_user_id
UUID
FK users.id
segment_id
UUID
FK segments.id
channel
ENUM
EMAIL, PHONE, SMS, MIXED
message_subject
VARCHAR(255)
Optional
message_body
TEXT
Required for message channels
start_date
DATE
Optional
end_date
DATE
Optional
approved_by
UUID
FK users.id
approved_at
TIMESTAMP
Optional
rejection_reason
TEXT
Optional
created_at
TIMESTAMP
Required
updated_at
TIMESTAMP
Required
campaign_recipients
Field
Type
Notes
id
UUID
Primary key
campaign_id
UUID
FK campaigns.id
customer_id
UUID
FK customers.id
eligibility_status
ENUM
ELIGIBLE, EXCLUDED, SENT, OPENED, CLICKED, REPLIED, CONVERTED, FAILED
exclusion_reason
TEXT
Optional
eligibility_explanation
TEXT
Optional
sent_at
TIMESTAMP
Optional
opened_at
TIMESTAMP
Optional
clicked_at
TIMESTAMP
Optional
converted_at
TIMESTAMP
Optional
created_at
TIMESTAMP
Required
Unique constraint: campaign_id + customer_id.
contact_events
Field
Type
Notes
id
UUID
Primary key
customer_id
UUID
FK customers.id
campaign_id
UUID
FK campaigns.id, optional
channel
ENUM
EMAIL, SMS, PHONE, IN_APP
event_type
ENUM
SENT, OPENED, CLICKED, REPLIED, FAILED, UNSUBSCRIBED, CALLED, NOTE
outcome
ENUM
INTERESTED, NOT_INTERESTED, CONVERTED, NO_RESPONSE, FAILED
notes
TEXT
Optional
occurred_at
TIMESTAMP
Required
created_by
UUID
FK users.id
follow_up_tasks
Field
Type
Notes
id
UUID
Primary key
customer_id
UUID
FK customers.id
campaign_id
UUID
FK campaigns.id, optional
assigned_to
UUID
FK users.id
title
VARCHAR(255)
Required
description
TEXT
Optional
due_date
DATE
Optional
status
ENUM
OPEN, IN_PROGRESS, COMPLETED, CANCELLED
priority
ENUM
LOW, MEDIUM, HIGH
created_at
TIMESTAMP
Required
completed_at
TIMESTAMP
Optional
reminder_schedules
Field
Type
Notes
id
UUID
Primary key
customer_id
UUID
FK customers.id
product_id
UUID
FK products.id
reminder_type
ENUM
PAYMENT_DUE, PRODUCT_EXPIRATION
reminder_level
ENUM
GREEN, YELLOW, RED
scheduled_date
DATE
Required
status
ENUM
PENDING, SENT, FAILED, CANCELLED
created_at
TIMESTAMP
Required
sent_at
TIMESTAMP
Optional
campaign_metrics
Field
Type
Notes
id
UUID
Primary key
campaign_id
UUID
FK campaigns.id
audience_size
INTEGER
Required
eligible_count
INTEGER
Required
excluded_count
INTEGER
Required
sent_count
INTEGER
Required
opened_count
INTEGER
Required
clicked_count
INTEGER
Required
replied_count
INTEGER
Required
converted_count
INTEGER
Required
estimated_cost
DECIMAL(12,2)
Optional
estimated_revenue
DECIMAL(12,2)
Optional
estimated_roi
DECIMAL(12,2)
Optional
updated_at
TIMESTAMP
Required
audit_logs
Field
Type
Notes
id
UUID
Primary key
actor_user_id
UUID
FK users.id
action
VARCHAR(255)
Required
entity_type
VARCHAR(100)
Required
entity_id
UUID
Optional
old_value
JSONB
Optional
new_value
JSONB
Optional
ip_address
VARCHAR(100)
Optional
created_at
TIMESTAMP
Required
ai_recommendations
Field
Type
Notes
id
UUID
Primary key
recommendation_type
ENUM
PRODUCT, SEGMENT, COPY, RISK, DUPLICATE_WARNING
target_entity_type
VARCHAR(100)
Customer, Campaign, Segment
target_entity_id
UUID
Optional
input_summary
TEXT
Required
recommendation
TEXT
Required
explanation
TEXT
Required
confidence_score
DECIMAL(5,2)
Optional
approved_by_user_id
UUID
Optional
created_at
TIMESTAMP
Required
17. Entity Permission Matrix
Entity
Admin
Campaign Manager
BI Analyst
Product Manager
Compliance Officer
Customer Service
Sales
Executive
Auditor
users
ADMIN
NONE
NONE
NONE
NONE
NONE
NONE
NONE
AUDIT
roles
ADMIN
NONE
NONE
NONE
NONE
NONE
NONE
NONE
AUDIT
customers
CRUD
READ
READ_AGGREGATED
NONE
READ/UPDATE_COMPLIANCE
CRUD_LIMITED
READ_ASSIGNED
READ_AGGREGATED
AUDIT
beneficiaries
CRUD
READ
READ_AGGREGATED
NONE
READ/UPDATE_COMPLIANCE
CRUD_LIMITED
NONE
NONE
AUDIT
consent_records
READ
READ
READ_AGGREGATED
NONE
CRUD/APPROVE
CREATE/UPDATE_LIMITED
READ_LIMITED
NONE
AUDIT
products
CRUD
READ
READ
CRUD
READ
READ
READ
READ_AGGREGATED
AUDIT
product_ownerships
CRUD
READ
READ_AGGREGATED
READ/UPDATE
READ
READ/UPDATE_LIMITED
READ_ASSIGNED
READ_AGGREGATED
AUDIT
payment_records
READ
READ
READ_AGGREGATED
NONE
READ
READ/UPDATE_LIMITED
READ_ASSIGNED
READ_AGGREGATED
AUDIT
segments
CRUD
CRUD
READ/DRAFT
NONE
READ
NONE
NONE
READ_AGGREGATED
AUDIT
campaigns
CRUD
CRUD
READ
READ_PRODUCT_LINKED
APPROVE/READ
READ_LIMITED
READ_ASSIGNED
READ_AGGREGATED
AUDIT
campaign_recipients
CRUD
CRUD
READ_AGGREGATED
NONE
READ/APPROVE
READ_LIMITED
READ_ASSIGNED
READ_AGGREGATED
AUDIT
contact_events
CRUD
CRUD
READ_AGGREGATED
NONE
READ
CREATE/READ
CREATE/READ_ASSIGNED
READ_AGGREGATED
AUDIT
follow_up_tasks
CRUD
CRUD
NONE
NONE
READ
CRUD
CRUD_ASSIGNED
NONE
AUDIT
reminder_schedules
CRUD
CRUD
READ_AGGREGATED
NONE
READ
READ/UPDATE_LIMITED
READ_ASSIGNED
READ_AGGREGATED
AUDIT
campaign_metrics
READ
READ
READ/EXPORT
READ_PRODUCT_LINKED
READ
NONE
READ_ASSIGNED
READ/EXPORT
AUDIT
audit_logs
READ
NONE
NONE
NONE
READ
NONE
NONE
NONE
READ/EXPORT
ai_recommendations
READ
CREATE/READ
CREATE/READ
READ_PRODUCT_LINKED
READ/APPROVE_IF_COPY
NONE
NONE
READ_AGGREGATED
AUDIT
18. API Design
Module
Endpoint
Access
Auth
POST /api/auth/login
Public internal login
Auth
POST /api/auth/refresh
Authenticated
Auth
POST /api/auth/logout
Authenticated
Auth
GET /api/auth/me
Authenticated
Users
GET /api/users
Admin
Users
POST /api/users
Admin
Users
PUT /api/users/{id}
Admin
Users
PATCH /api/users/{id}/disable
Admin
Customers
GET /api/customers
Admin, Campaign Manager, BI Analyst, Compliance, Agent
Customers
POST /api/customers
Admin, Agent
Customers
GET /api/customers/{id}
Authorized roles
Customers
PUT /api/customers/{id}
Admin, Agent, limited Compliance
Customers
DELETE /api/customers/{id}
Admin
Consent
GET /api/customers/{id}/consents
Authorized roles
Consent
POST /api/customers/{id}/consents
Compliance, Agent
Products
GET /api/products
Authorized roles
Products
POST /api/products
Admin, Product Manager
Products
PUT /api/products/{id}
Admin, Product Manager
Products
DELETE /api/products/{id}
Admin, Product Manager
Segments
GET /api/segments
Campaign Manager, BI Analyst
Segments
POST /api/segments
Campaign Manager
Segments
PUT /api/segments/{id}
Campaign Manager
Segments
POST /api/segments/preview
Campaign Manager, BI Analyst
Campaigns
GET /api/campaigns
Authorized roles
Campaigns
POST /api/campaigns
Campaign Manager
Campaigns
PUT /api/campaigns/{id}
Campaign Manager for draft
Campaigns
POST /api/campaigns/{id}/submit
Campaign Manager
Campaigns
POST /api/campaigns/{id}/approve
Compliance Officer
Campaigns
POST /api/campaigns/{id}/reject
Compliance Officer
Campaigns
POST /api/campaigns/{id}/launch
Campaign Manager
Campaigns
POST /api/campaigns/{id}/pause
Campaign Manager
Campaigns
GET /api/campaigns/{id}/recipients
Campaign Manager, Compliance
Reminders
GET /api/reminders
Authorized roles
Reminders
POST /api/reminders/payment-due
Campaign Manager
Reminders
POST /api/reminders/product-expiration
Campaign Manager
Analytics
GET /api/analytics/dashboard
BI, Campaign Manager, Executive
Reports
GET /api/reports/campaigns/{id}/csv
BI, Executive, Campaign Manager
Reports
GET /api/reports/campaigns/{id}/pdf
BI, Executive, Campaign Manager
AI
GET /api/ai/customer-search?q=
Authorized roles
AI
POST /api/ai/segment-suggestions
BI, Campaign Manager
AI
POST /api/ai/product-recommendations
BI, Campaign Manager
AI
POST /api/ai/campaign-copy
Campaign Manager
Audit
GET /api/audit-logs
Admin, Compliance, Auditor
19. Backend Class System
19.1 Common Base Classes
Class
Constructor
Variables
Methods
BaseEntity
BaseEntity()
id, createdAt, updatedAt
markCreated(), markUpdated()
SoftDeletableEntity
SoftDeletableEntity()
deletedAt
softDelete(), restore(), isDeleted()
ApiResponse
ApiResponse(success, message, data)
success, message, data, timestamp
success(), error()
PageResponse
PageResponse(items, page, size, total)
items, page, size, totalElements, totalPages
fromPage()
ErrorResponse
ErrorResponse(code, message)
code, message, details, timestamp
of()
BusinessException
BusinessException(message, code)
code, message
getCode()
AccessDeniedException
AccessDeniedException(message)
message
none
NotFoundException
NotFoundException(entity, id)
entity, id
none
19.2 Entity Classes
Class
Constructor concept
Variables
Methods
User
User(email, passwordHash, fullName, roles)
id, email, passwordHash, fullName, status, roles, lastLoginAt
enable(), disable(), lock(), changePassword(), assignRole(), 
removeRole(), hasRole()
Role
Role(name, description)
id, name, description
isAdminRole(), isComplianceRole()
Customer
Customer(type, firstName, lastName, email, phone)
id, customerType, firstName, lastName, email, phone, addressLine, city, country, dateOfBirth, ageGroup, status, doNotContact, source, deletedAt
updateContactInfo(), markInterested(), markUninterested(), markConverted(), markDoNotContact(), clearDoNotContact(), softDelete(), isContactable()
Beneficiary
Beneficiary(policyholder, beneficiaryCustomer, relationship)
id, policyholderCustomerId, beneficiaryCustomerId, relationship, guardianName, guardianEmail, guardianConsentRequired
requireGuardianConsent(), updateGuardian(), hasGuardianRequirement()
ConsentRecord
ConsentRecord(customer, type, status, purpose, source)
id, customerId, consentType, status, purpose, source, grantedAt, withdrawnAt, expiresAt, evidenceFileUrl, createdBy
grant(), withdraw(), expire(), isValid(), requiresAction()
Product
Product(name, type, price, durationMonths)
id, name, productType, description, price, durationMonths, expirationPolicy, active, deletedAt
updateDetails(), updatePricing(), deactivate(), activate(), softDelete(), isActive()
ProductOwnership
ProductOwnership(customer, product, startDate, expirationDate)
id, customerId, productId, policyNumber, startDate, 
expirationDate, status
expire(), cancel(), isExpiringWithinMonths(), isActive()
ProductChangeRequest
ProductChangeRequest(product, requestedBy, type, description)
id, productId, requestedBy, requestType, description, status
approve(), reject(), markImplemented(), updateDescription()
PaymentRecord
PaymentRecord(customer, ownership, dueDate, amountDue)
id, customerId, productOwnershipId, dueDate, paidAt, amountDue, amountPaid, status, reminderCount
markPaid(), markOverdue(), incrementReminder(), calculateDaysOverdue(), isDefaultRisk()
Segment
Segment(name, description, owner, visibility)
id, name, description, ownerUserId, visibility, criteria
addCriteria(), removeCriteria(), updateName(), isGlobal(), isOwnedBy()
SegmentCriteria
SegmentCriteria(segment, field, operator, value)
id, segmentId, fieldName, operator, value, logicalGroup, joinOperator
matches(), updateValue(), updateOperator()
Campaign
Campaign(name, objective, owner, segment, channel)
id, name, objective, status, ownerUserId, segmentId, channel, subject, body, dates, approvedBy, approvedAt, rejectionReason
submit(), approve(), reject(), launch(), pause(), complete(), archive(), canEdit(), canLaunch()
CampaignProduct
CampaignProduct(campaign, product)
id, campaignId, productId
none
CampaignRecipient
CampaignRecipient(campaign, customer)
id, campaignId, customerId, eligibilityStatus, exclusionReason, eligibilityExplanation, sentAt, openedAt, 
clickedAt, convertedAt
markEligible(), exclude(), markSent(), markOpened(), markClicked(), markConverted(), markFailed()
ContactEvent
ContactEvent(customer, campaign, channel, eventType)
id, customerId, campaignId, channel, eventType, outcome, notes, occurredAt, createdBy
updateOutcome(), addNotes(), isMarketingOptOut()
FollowUpTask
FollowUpTask(customer, assignedTo, title, dueDate)
id, customerId, campaignId, assignedTo, title, description, dueDate, status, priority
assignTo(), start(), complete(), cancel(), updatePriority()
ReminderSchedule
ReminderSchedule(customer, product, type, level, date)
id, customerId, productId, reminderType, reminderLevel, scheduledDate, status, sentAt
markSent(), markFailed(), cancel(), isDue()
CampaignMetric
CampaignMetric(campaign)
id, campaignId, audienceSize, eligibleCount, excludedCount, sentCount, openedCount, clickedCount, repliedCount, convertedCount, estimatedCost, estimatedRevenue, estimatedRoi
recalculate(), calculateOpenRate(), calculateClickRate(), calculateConversionRate(), calculateRoi()
AuditLog
AuditLog(actor, action, entityType, entityId)
id, actorUserId, action, entityType, entityId, oldValue, newValue, ipAddress, createdAt
recordChange()
AiRecommendation
AiRecommendation(type, targetType, targetId, 
input, recommendation, explanation)
id, recommendationT
ype, targetEntityType, targetEntityId, inputSummary, recommendation, explanation, confidenceScore, approvedByUserId
approve(), reject(), updateConfidence()
19.3 Service Classes
Class
Constructor dependencies
Methods
AuthService
UserRepository, PasswordEncoder, JwtService, AuditService
login(), refreshToken(), logout(), getCurrentUser(), validateCredentials()
JwtService
security properties
generateAccessToken(), generateRefreshToken(), validateToken(), extractUserId(), extractRoles()
UserService
UserRepository, RoleRepository, PasswordEncoder, AuditService
createUser(), updateUser(), disableUser(), assignRole(), resetPassword(), findById(), listUsers()
AuthorizationService
current user context
hasRole(), canAccessCustomer(), canEditCampaign(), canApproveCampaign(), requirePermission()
CustomerService
CustomerRepository, ConsentService, AuditService
createCustomer(), updateCustomer(), softDeleteCustomer(), searchCustomers(), getProfile(), markStatus(), importCsv()
BeneficiaryService
BeneficiaryRepository, CustomerRepository, ConsentService
addBeneficiary(), updateGuardian(), listBeneficiaries(), requiresGuardianConsent()
ConsentService
ConsentRepository, AuditService
recordConsent(), withdrawConsent(), getConsentStatus(), hasValidMarketingConsent(), hasValidGuardianConsent(), isCommunicationEligible()
ProductService
ProductRepository, AuditService
createProduct(), updateProduct(), deactivateProduct(), searchProducts(), findActiveProducts()
ProductOwnershipService
OwnershipRepository, ProductRepository
assignProduct(), updateOwnership(), findExpiringWithinMonths(), listCustomerProducts()
ProductChangeRequestService
ChangeRequestRepository, AuditService
createRequest(), approveRequest(), rejectRequest(), markImplemented(), listRequests()
SegmentService
SegmentRepository, CriteriaRepository, CustomerRepository, ConsentService
createSegment(), updateSegment(), deleteSegment(), previewSegment(), saveCriteria(), findMatchingCustomers()
CampaignService
CampaignRepository, SegmentService, EligibilityService, CommunicationService, AuditService, MetricsService
createCampaign(), updateDraft(), submit(), approve(), reject(), generateRecipients(), launch(), pause(), complete(), archive()
EligibilityService
ConsentService, ContactEventRepository, CampaignRecipientRepository
evaluateCustomer(), excludeInvalidConsent(), excludeOptOuts(), excludeDuplicateContacts(), checkMonthlyLimit()
CommunicationService
EmailProvider, SmsProvider, ContactEventRepository
sendCampaignMessage(), mockSend(), recordEvent(), retryFailedSend(), unsubscribe()
ReminderService
ReminderRepository, PaymentRepository, OwnershipRepository, ConsentService
createPaymentReminders(), createExpirationReminders(), sendDueReminders(), markSent(), cancelReminder()
FollowUpService
FollowUpRepository, CustomerRepository, UserRepository
createTask(), assignTask(), updateTask(), completeTask(), listAssignedTasks()
AnalyticsService
CampaignRepository, MetricsRepository, ContactEventRepository
getDashboardMetrics(), getCampaignAnalytics(), calculateConversionRate(), calculateRoi(), compareCampaigns()
ReportService
AnalyticsService, CampaignRepository
exportCampaignCsv(), generateCampaignPdf(), exportAuditReport()
AuditService
AuditLogRepository
logCreate(), logUpdate(), logDelete(), logApproval(), logConsentChange(), logRoleChange()
AiSearchService
CustomerRepository
fuzzyCustomerSearch(), weightedSearch(), explainScore()
AiRecommendationService
CustomerRepository, ProductRepository, OwnershipRepository
recommendProducts(), suggestSegments(), calculateDefaultRisk(), detectDuplicateRisk()
CampaignCopyService
ProductRepository, SegmentRepository
generateCopySuggestion(), requireHumanApproval(), saveSuggestion()
19.4 Controller Classes
Controller
Constructor dependencies
Endpoints/methods
AuthController
AuthService
login(), refresh(), logout(), me()
UserController
UserService
listUsers(), createUser(), updateUser(), disableUser(), assignRole()
CustomerController
CustomerService
listCustomers(), createCustomer(), getCustomer(), updateCustomer(), deleteCustomer(), importCsv()
BeneficiaryController
BeneficiaryService
addBeneficiary(), updateBeneficiary(), listBeneficiaries()
ConsentController
ConsentService
listConsents(), recordConsent(), withdrawConsent(), checkEligibility()
ProductController
ProductService
listProducts(), createProduct(), getProduct(), updateProduct(), deleteProduct()
ProductChangeRequestController
ProductChangeRequestService
createRequest(), listRequests(), approveRequest(), rejectRequest()
SegmentController
SegmentService
listSegments(), createSegment(), updateSegment(), previewSegment(), deleteSegment()
CampaignController
CampaignService
listCampaigns(), createCampaign(), updateCampaign(), submit(), approve(), reject(), launch(), pause(), complete(), archive()
RecipientController
CampaignService
generateRecipients(), listRecipients(), previewRecipients()
ReminderController
ReminderService
listReminders(), createPaymentReminders(), createExpirationReminders(), sendReminder(), cancelReminder()
ContactEventController
CommunicationService
listCustomerHistory(), addContactEvent(), updateOutcome()
FollowUpController
FollowUpService
listTasks(), createTask(), assignTask(), completeTask()
AnalyticsController
AnalyticsService
dashboard(), campaignAnalytics(), conversions(), productPerformance()
ReportController
ReportService
campaignCsv(), campaignPdf(), auditReport()
AuditController
AuditService
listAuditLogs(), getEntityHistory()
AiController
AiSearchService, AiRecommendationService, CampaignCopyService
customerSearch(), segmentSuggestions(), productRecommendations(), defaultRiskScore(), campaignCopy()
19.5 Repository Interfaces
Repository
Main methods
UserRepository
findByEmail(), findById(), findByStatus()
RoleRepository
findByName()
CustomerRepository
search(), findByStatus(), findByCity(), findActive(), findByDoNotContactFalse()
BeneficiaryRepository
findByPolicyholderCustomerId(), findByBeneficiaryCustomerId()
ConsentRepository
findByCustomerId(), findValidConsent(), findLatestByType(), findOptOuts()
ProductRepository
findActive(), searchByNameOrType(), findByType()
ProductOwnershipRepository
findByCustomerId(), findExpiringBetween(), findActiveByProduct()
ProductChangeRequestRepository
findByStatus(), findByProductId()
SegmentRepository
findByOwner(), findGlobal(), findByVisibility()
SegmentCriteriaRepository
findBySegmentId()
CampaignRepository
findByStatus(), findByOwnerUserId(), findActiveCampaigns()
CampaignRecipientRepository
findByCampaignId(), existsByCampaignAndCustomer(), findExcludedByCampaign()
ContactEventRepository
findByCustomerId(), findByCampaignId(), countRecentMarketingContacts()
PaymentRecordRepository
findDuePayments(), findOverduePayments(), findByCustomerId()
ReminderRepository
findDueReminders(), findByStatus(), findByCustomerId()
FollowUpRepository
findByAssignedTo(), findOpenTasks(), findByCustomerId()
CampaignMetricRepository
findByCampaignId()
AuditLogRepository
findByEntityTypeAndEntityId(), findByActorUserId(), findRecent()
AiRecommendationRepository
findByTargetEntity(), findByRecommendationType()
20. Frontend Screens
Screen
Purpose
Login
Employee login
Dashboard
KPIs, campaign overview, consent alerts, reminders
Customers
Customer/prospect list, search, filters, CRUD
Customer Details
Profile, products, beneficiaries, consent, history, tasks, notes
Products
Product list and CRUD
Product Change Requests
Product modification workflow
Campaigns
Campaign list, status filters, actions
Campaign Builder
Multi-step campaign creation
Segmentation
Criteria builder and segment preview
Recipient Preview
Eligible/excluded contacts before launch
Compliance Review
Approve/reject submitted campaigns
Reminders
Payment and product-expiration reminder schedules
Contact History
Communication timeline
Follow-Up Tasks
Assigned follow-up work
Analytics
Engagement, conversion, ROI, comparisons
Reports
CSV/PDF generation
Audit Log
Sensitive action history
User Management
Employee accounts and roles
Settings
System configuration
21. Core Workflows
21.1 Campaign Creation and Launch
Step
Action
1
Campaign Manager creates draft campaign
2
Select product
3
Select or create segment
4
Compose message
5
System previews recipients
6
EligibilityService excludes invalid contacts
7
Campaign Manager submits for review
8
Compliance Officer approves or rejects
9
Campaign Manager launches approved campaign
10
CommunicationService sends or queues messages
11
ContactEvent records communication
12
CampaignMetric updates KPIs
13
AuditLog records sensitive actions
21.2 Consent Eligibility
Step
Rule
1
Check do_not_contact
2
Check opt-out status
3
Check valid marketing consent
4
Check guardian consent if required
5
Check duplicate campaign contact
6
Check monthly contact limit
7
Mark customer eligible or excluded
8
Store exclusion reason
21.3 Product Expiration Campaign
Step
Action
1
System finds product ownerships expiring in 3/6/12 months
2
System identifies related customers
3
Consent and eligibility are checked
4
Reminder or campaign is created
5
Campaign Manager reviews
6
Compliance Officer approves if marketing communication is involved
7
Campaign is launched or reminder is sent
22. Database Indexes
Index
Purpose
idx_customers_email
Fast customer lookup by email
idx_customers_status
Filter by customer status
idx_customers_city
Location segmentation
idx_customers_do_not_contact
Fast opt-out exclusion
idx_products_type
Product filtering
idx_product_ownership_expiration
Expiration campaigns
idx_campaigns_status
Campaign workflow filtering
idx_campaign_recipients_campaign
Campaign recipient lookup
idx_campaign_recipients_customer
Customer campaign history
idx_contact_events_customer
Customer timeline
idx_contact_events_campaign
Campaign analytics
idx_reminder_schedules_date
Due reminder selection
idx_audit_logs_entity
Entity audit history
23. Security Rules
Rule
Description
SEC-001
Passwords must be hashed with BCrypt
SEC-002
APIs must enforce role authorization server-side
SEC-003
Frontend access checks are not sufficient alone
SEC-004
JWT/session secrets must be stored in environment variables
SEC-005
CORS must allow only approved frontend domains
SEC-006
All inputs must be validated
SEC-007
JPA/parameterized queries must be used to prevent SQL injection
SEC-008
Login attempts should be rate-limited
SEC-009
Sensitive actions must be audited
SEC-010
Stack traces must not be exposed in production
SEC-011
Production must use HTTPS
SEC-012
Role changes, consent updates, campaign approvals, and deletions must be logged
24. Compliance Rules
Rule
Description
COMP-001
Consent records must prove permission
COMP-002
Opt-outs must be respected immediately
COMP-003
Do-not-contact overrides all marketing logic
COMP-004
Guardian consent must be required for minors where applicable
COMP-005
AI suggestions require human review
COMP-006
Campaigns require compliance approval
COMP-007
Exclusion reasons must be recorded
COMP-008
Audit logs must be immutable at application level
COMP-009
Personal data access must follow least privilege
COMP-010
Reports for executives should use aggregated data where possible
25. Testing Plan
Test type
Scope
Backend unit tests
Services, eligibility, consent, campaign workflow
Repository tests
Filters, search, segmentation queries
Integration tests
Full API + PostgreSQL Testcontainers
Security tests
Role access, unauthorized requests, forbidden actions
Scheduler tests
Payment reminders, product expiration reminders
Validation tests
Invalid campaigns, missing consent, bad customer data
Frontend component tests
Forms, tables, charts, status badges
Frontend integration tests
Campaign builder, customer workflow
E2E tests
Login 
→
 create customer 
→
 consent 
→
 campaign 
→
 approval 
→
 launch
Accessibility tests
Labels, keyboard navigation, contrast
Report tests
CSV/PDF generation
Audit tests
Sensitive actions logged correctly
Critical Test Cases
ID
Test
TC-001
Campaign cannot launch without approval
TC-002
Customer with do_not_contact is excluded
TC-003
Customer without valid consent is excluded
TC-004
Minor beneficiary without guardian consent is excluded
TC-005
Same customer cannot be duplicated in same campaign
TC-006
Customer cannot exceed monthly contact limit
TC-007
Red reminder appears after previous reminders
TC-008
Product-expiration reminder works for 3/6/12 months
TC-009
BI Analyst cannot edit customers
TC-010
Product Manager cannot launch campaigns
TC-011
Compliance Officer can approve/reject campaigns
TC-012
Contact events update campaign analytics
TC-013
Soft-deleted customers do not appear in active lists
TC-014
Audit log is created after consent change
TC-015
Admin can disable user and disabled user cannot log in
26. Sprint Plan
Sprint
Goal
Output
Sprint 1
Setup and architecture
React, Spring Boot, PostgreSQL, Docker running
Sprint 2
Auth and users
Login, roles, user management
Sprint 3
Customers and consent
Customer CRUD, beneficiaries, consent, opt-out
Sprint 4
Products
Product CRUD, ownership, product-change requests
Sprint 5
Segmentation
Segment builder, preview, eligibility checks
Sprint 6
Campaign lifecycle
Campaign builder, submit, approve, reject
Sprint 7
Launch and tracking
Recipients, sending adapter, contact events
Sprint 8
Reminders
Payment reminders and product-expiration reminders
Sprint 9
Analytics and reports
Dashboard, campaign metrics, exports
Sprint 10
AI, audit, testing, deployment
AI features, audit logs, full test suite, deployment
27. Product Backlog
Epic
User story
Authentication
As an internal user, I want to log in securely so that I can access the platform
Authorization
As an Admin, I want to assign roles so users only access allowed features
Customer management
As a Customer Service Agent, I want to update customer records so that data remains accurate
Customer search
As a Campaign Manager, I want to search customers so I can find campaign targets
Consent
As a Compliance Officer, I want to review consent so marketing respects legal rules
Eligibility
As a Campaign Manager, I want ineligible contacts excluded automatically
Products
As a Product Manager, I want to manage products so campaigns promote correct offers
Segmentation
As a Campaign Manager, I want to create audience segments so targeting is precise
Campaigns
As a Campaign Manager, I want to create and submit campaigns
Compliance review
As a Compliance Officer, I want to approve campaigns before launch
Communication
As a Customer Service Agent, I want to record contact outcomes
Follow-up
As a Sales Agent, I want to follow up with interested prospects
Reminders
As a Campaign Manager, I want to schedule payment and expiration reminders
Analytics
As a BI Analyst, I want to evaluate campaign performance
Reports
As an Executive, I want to view ROI and campaign summaries
Audit
As an Auditor, I want to review sensitive system actions
AI
As a Campaign Manager, I want AI suggestions to improve targeting and messaging
28. Definition of Done
A feature is complete only when:
Requirement
Done condition
Backend
API implemented
Database
Migration exists
Frontend
UI implemented
Validation
Frontend and backend validation exist
Security
Role authorization enforced
Error handling
Errors are handled consistently
Audit
Sensitive actions logged
Tests
Unit/integration tests exist
Documentation
Feature documented
Demo
Feature can be demonstrated
29. Deployment Requirements
Area
Requirement
Environment
dev, test, production
Configuration
environment variables
Database
PostgreSQL with migrations
Containers
Docker and Docker Compose
Reverse proxy
Nginx or Caddy
HTTPS
Required in production
Logs
Backend logs and error tracking
Backups
Scheduled PostgreSQL backups
CI/CD
GitHub Actions build/test/package
Health checks
Spring Actuator health endpoint
Secrets
Never committed to Git
30. Implementation Order
Order
Task
1
Database schema
2
Backend foundation
3
Auth and roles
4
User management
5
Customer CRUD
6
Beneficiary and consent logic
7
Product CRUD
8
Product ownership and payment records
9
Segmentation engine
10
Campaign builder
11
Recipient preview and eligibility
12
Compliance approval workflow
13
Campaign launch
14
Contact tracking
15
Follow-up tasks
16
Reminder scheduling
17
Analytics
18
Reports
19
AI-assisted features
20
Audit logs
21
Testing
22
Deployment
23
Final documentation and report
31. Minimum Operation-Ready MVP
Feature
Required
Login and roles
Yes
User management
Yes
Customer/prospect CRUD
Yes
Beneficiary management
Yes
Consent and opt-out
Yes
Product CRUD
Yes
Product ownership
Yes
Campaign builder
Yes
Segmentation preview
Yes
Eligibility checks
Yes
Compliance approval
Yes
Campaign launch
Yes
Contact history
Yes
Follow-up tasks
Yes
Analytics dashboard
Yes
Reports
Yes
Audit logs
Yes
At least one AI feature
Yes
Tests
Yes
Deployment documentation
Yes
32. Final System Definition
The final product is a full-stack enterprise campaign management platform for insurance marketing automation. It is built with React, Spring Boot, and PostgreSQL. It supports CRM, product management, consent-aware segmentation, campaign scheduling, communication tracking, follow-up management, campaign analytics, reporting, auditability, role-based access control, and AI-assisted recommendations.
The system must be designed as an operation-ready internal business platform. Core compliance, permissions, consent, eligibility, campaign approval, audit, and customer data logic must be fully implemented. Mocking is allowed only for development, testing, demonstration data, or external provider adapters before real business integration.