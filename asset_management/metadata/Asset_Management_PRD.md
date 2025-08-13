# Product Requirements Document (PRD)

## Project Overview

### Project Title
Asset Management System

### Project Description
This project involves developing a comprehensive system for asset management using RFID/NFC technology. The system includes:
- Mobile applications for Android and iOS to handle NFC tag reading and writing.
- A web application for asset management with hierarchical access controls.
- Backend APIs to support data storage, retrieval, and processing.

### Objectives
- Enable efficient tracking and management of assets using UUIDs stored on NFC-enabled RFID tags.
- Provide secure, role-based access to asset data via a web interface.
- Ensure cross-platform compatibility for mobile apps (Android and iOS).

### Stakeholders
- Clients: End-users managing assets.
- Administrators: Manage user access and system configurations.
- Developers: Build and maintain the system.

## Scope

### In Scope
- Development of mobile apps (Android, iOS) for NFC tag operations.
- Web application for asset details management and UUID mapping.
- Backend API for data handling, authentication, and integration.
- Database storage for assets.
- Hierarchical access controls (e.g., admin, manager, user roles).

### Out of Scope
- Hardware procurement for RFID/NFC tags.
- Advanced analytics or reporting beyond basic CRUD operations.
- Integration with third-party software systems.

## Features

### Feature 1: Asset Management

#### Description
System to tag assets with UUIDs via NFC-enabled RFID tags and manage asset details through a web interface.

#### Sub-Features
1. **Mobile App NFC Operations**
   - Read UUID from NFC tags.
   - Write UUID to NFC tags.
   - User authentication to ensure authorized access.

2. **Web Application**
   - Maintain asset details (e.g., name, description, location, status).
   - Map UUIDs to asset records.
   - Hierarchy-based access (e.g., organizational hierarchy: company > department > user).

3. **Backend API**
   - Endpoints for creating, reading, updating, deleting (CRUD) asset records.
   - UUID generation and validation.
   - Integration with mobile app for syncing UUIDs.

#### Notes
- RFID tags store only the UUID; all other data is in the database.



## Functional Requirements

### User Roles and Permissions
- **Admin**: Full access, manage users and hierarchies.
- **Manager**: Access to department-level assets.
- **User**: Read/write access to assigned assets.

### Mobile App Requirements
- Platforms: Android (min SDK 21) and iOS (min version 12).
- NFC capabilities: Read/Write NDEF messages containing UUID.
- Offline support: Queue operations when offline.
- UI: Simple interface for scanning tags, entering asset details.

### Web App Requirements
- Responsive design for desktop and mobile browsers.
- Frameworks: React.js or similar for frontend.
- Features: Dashboards for assets, search/filter, forms for data entry.

### Backend Requirements
- API: RESTful or GraphQL.
- Database: Relational (e.g., PostgreSQL) for structured data.
- Authentication: JWT or OAuth for secure access.
- Storage: Cloud storage for application data.

### Integration Points
- NFC library integration in mobile apps (e.g., Android NFC API, Core NFC for iOS).

## Non-Functional Requirements

### Performance
- API response time < 2 seconds.
- Support up to 1000 concurrent users.

### Security
- Data encryption in transit (HTTPS) and at rest.
- Role-Based Access Control (RBAC).
- Compliance: GDPR for data privacy.

### Scalability
- Cloud-based deployment (e.g., AWS, Azure).
- Horizontal scaling for backend services.

### Reliability
- 99.9% uptime.
- Automated backups and error logging.

### Usability
- Intuitive UI/UX.
- Accessibility compliance (WCAG 2.1).

## Technical Stack (Suggested)
- **Mobile**: Flutter or React Native for cross-platform development.
- **Web Frontend**: React.js with Redux.
- **Backend**: Java with Spring Boot (alternatively, Node.js/Express or Python/Django).
- **Database**: PostgreSQL.
- **Cloud Services**: AWS (S3 for storage, Lambda for processing).
- **Other**: Docker for containerization, CI/CD with GitHub Actions.

## Assumptions and Dependencies
- Access to NFC-capable devices for testing.
- Client provides sample asset data for testing.

## Risks and Mitigations
- **Risk**: NFC compatibility issues. **Mitigation**: Test on multiple devices.
- **Risk**: Data security. **Mitigation**: Regular audits and encryption.

## Timeline and Milestones (High-Level)
- Phase 1: Requirements and Design (2 weeks).
- Phase 2: Backend Development (4 weeks).
- Phase 3: Mobile App Development (6 weeks).
- Phase 4: Web App Development (4 weeks).
- Phase 5: Integration and Testing (4 weeks).
- Phase 6: Deployment and Launch (2 weeks).

## Appendix
- Glossary: UUID - Universally Unique Identifier; NFC - Near Field Communication; RFID - Radio-Frequency Identification.
- References: [NFC Developer Guide](https://developer.android.com/guide/topics/connectivity/nfc).

This PRD serves as a living document and may be updated based on feedback and discoveries during development.
