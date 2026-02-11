---
title: "sMockin Documentation"
date: 2026-02-10
draft: false
description: "Comprehensive guide to sMockin: Architecture, Features, and Usage"
categories: ["Documentation"]
tags: ["Mock Server", "API", "S3", "Email", "Testing"]
---

# sMockin Documentation

sMockin is an open-source development tool designed to dynamically mock API endpoints, S3 buckets, and Email servers. It provides a rich UI dashboard and built-in mock servers to facilitate application development and QA testing.

## Table of Contents
1. [Architecture](#architecture)
2. [Key Features](#key-features)
3. [Installation](#installation)
4. [Usage Guide](#usage-guide)
    - [API Mocking](#api-mocking)
    - [S3 Mocking](#s3-mocking)
    - [Email Mocking](#email-mocking)
    - [Proxy & Interception](#proxy--interception)
5. [Examples](#examples)
6. [Configuration](#configuration)

---

## Architecture

sMockin is built with a modern tech stack, ensuring scalability and ease of use.

- **Backend:** Spring Boot 3.5.x (Java 21).
- **Frontend:** Angular 18.
- **Embedded Servers:** 
    - **Jetty:** Powers the RESTful mock engine.
    - **S3Proxy:** Provides S3 bucket simulation.
    - **GreenMail:** Handles virtual SMTP and POP3/IMAP services.
- **JS Engine:** GraalJS for executing dynamic JavaScript-based mock logic.
- **Database:** H2 (default in-memory/file) or PostgreSQL, with Flyway for migrations.
- **Tunneling:** Integrated Ngrok support for exposing local mocks to the internet.

### High-Level Component Diagram
The `MockedServerEngineService` acts as the central coordinator, managing the lifecycle of the various mock engines:
- `MockedRestServerEngine`: Handles HTTP/REST requests.
- `MockedS3ServerEngine`: Simulates AWS S3 API.
- `MockedMailServerEngine`: Manages virtual email accounts and messages.

---

## Key Features

- **Dynamic API Mocking:** Create RESTful endpoints with custom status codes, headers, and bodies.
- **JavaScript Handlers:** Use JS to programmatically define response logic based on request parameters, headers, or body.
- **S3 Bucket Mocking:** Test S3 integrations without an AWS account.
- **Email Mock Server:** Virtual SMTP server to test email sending/receiving.
- **Live Feed & Interception:** Monitor real-time traffic and intercept/modify requests (Block, Mock, and Swap).
- **Stateful Mocking:** Maintain state between API calls (e.g., CRUD operations on a virtual resource).
- **Proxy Server:** Forward traffic to real backends with path-based mapping.
- **Multi-user Support:** Centralized hosting with user account management.
- **Import/Export:** Share mock definitions via JSON files.

---

## Installation

### Prerequisites
- **Java 21** (Required for version 2.21.0+)
- **Docker** (Optional, for containerized execution)
- Maven 3.x (Optional, if using the provided `./mvnw` wrapper)

### Method 1: Running Locally (Native)
1. Clone the repository:
   ```bash
   git clone https://github.com/mgtechsoftware/smockin.git
   cd smockin
   ```
2. Build the project using the Maven wrapper:
   ```bash
   ./mvnw clean package
   ```
3. Run the application using Java:
   ```bash
   java -jar target/smockin.jar
   ```

### Method 2: Running with Docker
sMockin includes a `Dockerfile` and `docker-compose.yml` for easy deployment.

1. Build and start the containers:
   ```bash
   docker-compose up -d
   ```
2. The admin dashboard will be available at `http://localhost:8000`.

---

## Usage Guide

### API Mocking
You can create mocks via the **Dashboard → HTTP Mocks** section.
- **Static Response:** Define a fixed status code and body.
- **Sequential:** Return a different response for each later call.
- **Rule-based:** Match requests based on headers, parameters, or body content.
- **JavaScript:** Write a JS function to handle the request.
- **Stateful:** Define a virtual resource that maintains state across calls. For example, you can POST an item and then GET it back from the same endpoint.

#### JavaScript Handler Example
```javascript
var status = 200;
var contentType = "application/json";
var body = JSON.stringify({
    message: "Hello " + request.parameters['name'],
    timestamp: new Date()
});

var response = {
    status: status,
    contentType: contentType,
    body: body
};
```

### S3 Mocking
Navigate to **S3 Mocks** to create virtual buckets.
- Use any S3 client (like AWS CLI) pointing to `http://localhost:8002`.
- Supports standard operations: PutObject, GetObject, ListBuckets, etc.

### Email Mocking
The Email Mock Server runs on port **8003**.
- Configure your application to use sMockin as the SMTP server.
- Create virtual inboxes in the dashboard to view incoming emails and attachments.

### Proxy & Interception
The **Live Feed** tool allows you to see all traffic hitting the mock server.
- **Interception:** Enable "Blocking Mode" to pause a request and manually provide a mock response or modify the real one.
- **Proxy Mappings:** Map specific paths (e.g., `/api/v1/*`) to external URLs.

---

## Examples

### Creating a Mock via Admin API
You can also manage mocks programmatically using sMockin's own REST API.

**POST** `/restmock`
```json
{
  "path": "/hello",
  "method": "GET",
  "status": 200,
  "responseBody": "{"message": "Hello World"}",
  "contentType": "application/json"
}
```

---

## Configuration

System-level settings are located in `src/main/resources/application.yaml`.

| Property                | Default         | Description                            |
|-------------------------|-----------------|----------------------------------------|
| `server.port`           | 8000            | Port for the Admin Dashboard and API   |
| `multi.user.mode`       | false           | Enable/Disable user account management |
| `spring.datasource.url` | jdbc:h2:mem:... | Database connection string             |

Mock server ports (default):
- **REST Mock Server:** 8001 (configurable)
- **S3 Mock Server:** 8002
- **Email Mock Server:** 8003

---

*For more details, visit the official [User Guide](https://www.smockin.com/help).*
