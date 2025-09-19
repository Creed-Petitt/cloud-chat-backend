# Cloud-Native LLM Gateway (Backend)

This repository contains the cloud-native Spring Boot backend for AetheriusAI, a multimodal chat application. The service functions as a secure and scalable gateway, providing a unified RESTful API to orchestrate interactions between clients and multiple LLM providers (OpenAI, Anthropic Claude, Google Gemini). It is configured for serverless deployment on Google Cloud Run and features a complete CI/CD pipeline for automated, zero-downtime deployments.

## Key Features

- **LLM Provider Abstraction**: Dynamically switch between OpenAI, Claude, and Gemini models through a single, unified API endpoint.
- **Stateful Conversation Persistence**: Saves user conversations and messages in a PostgreSQL database, allowing for stateful interactions.
- **JWT-Based Security with Firebase**: Integrates with Firebase for robust, token-based user authentication and authorization using Spring Security.
- **Rate Limiting**: Protects the API from abuse with configurable rate limits:
  - **Authenticated users**: 20 messages and 3 images per account
  - **Anonymous users**: 10 messages per IP address (no image generation)
- **Serverless Cloud-Native Architecture**: Designed for serverless deployment on Google Cloud Run, using Cloud SQL for the database and Artifact Registry for container storage.
- **Automated CI/CD**: A complete GitHub Actions workflow automates testing, building, and deploying the application on every push to the `master` branch.

## Architecture Overview

The application follows a decoupled, layered architecture to ensure separation of concerns and maintainability.

1.  **Controllers**: The `ChatController` and `ImageController` expose the public REST API endpoints. They handle incoming HTTP requests, validate them, and pass them to the service layer.
2.  **Security**: A `FirebaseFilter` intercepts incoming requests to validate the Firebase JWT token present in the `Authorization` header. `SecurityConfig` defines the security rules for the application.
3.  **Services**: The core business logic resides here.
    - `ChatServiceFactory`: A factory class that, based on user input, provides the correct implementation of the `ChatService` interface (`OpenAIService`, `ClaudeService`, or `GeminiService`).
    - `ConversationService` & `MessageService`: Manage the logic for conversations and messages, including database interactions.
    - `UserService`: Handles user-related operations.
    - `RateLimitingService`: Manages API request limits.
4.  **Repositories**: Spring Data JPA repositories (`ConversationRepository`, `MessageRepository`, `UserRepository`) define the database operations for the corresponding entities.
5.  **Models**: JPA entities (`AppUser`, `Conversation`, `Message`) that map to the database tables.
6.  **Database**: A PostgreSQL database (hosted on Google Cloud SQL in production) persists all application data.

## API Endpoints

All endpoints are prefixed with `/api`. Authentication requirements vary by endpoint (see individual endpoint documentation).

### Rate Limiting

The API implements rate limiting to prevent abuse:

- **Authenticated Users**:
  - Chat messages: 20 per account
  - Image generation: 3 per account
- **Anonymous Users**:
  - Chat messages: 10 per IP address
  - Image generation: Not allowed

Rate limit information is included in response headers and response bodies.

### Error Responses

All endpoints may return the following error responses:

- **401 Unauthorized** (when authentication is required but missing/invalid):
  ```json
  {
    "error": "Unauthorized access"
  }
  ```

- **429 Too Many Requests** (when rate limit is exceeded):
  ```json
  {
    "error": "Rate limit exceeded",
    "message": "You have reached the maximum of 20 messages. Please upgrade your account to continue.",
    "remainingRequests": 0
  }
  ```

- **400 Bad Request** (for invalid input):
  ```json
  {
    "error": "Bad request",
    "message": "Content cannot be empty"
  }
  ```

- **500 Internal Server Error** (for server-side errors):
  ```json
  {
    "error": "Internal server error"
  }
  ```

### Chat

- **`POST /conversations/{id}/messages`**: Send a message to a conversation.
  - **Authentication**: Optional (Firebase JWT in `Authorization: Bearer <token>` header)
  - If `{id}` is `0`, a new conversation is created.
  - **Request Body**:
    ```json
    {
      "content": "Your message to the AI",
      "aiModel": "openai" // "openai", "claude", or "gemini"
    }
    ```
  - **Response**:
    ```json
    {
      "conversationId": 1,
      "userMessage": { ... },
      "aiMessage": { ... },
      "remainingRequests": 9
    }
    ```

- **`POST /conversations/{id}/messages/stream`**: Send a message to a conversation with real-time streaming response.
  - **Authentication**: Optional (Firebase JWT in `Authorization: Bearer <token>` header)
  - If `{id}` is `0`, a new conversation is created.
  - **Content-Type**: `text/event-stream`
  - **Request Body**:
    ```json
    {
      "content": "Your message to the AI",
      "aiModel": "openai" // "openai", "claude", or "gemini"
    }
    ```
  - **Response**: Server-sent events stream with AI response chunks

- **`GET /conversations`**: Get a list of all conversations for the authenticated user.
  - **Authentication**: Required (Firebase JWT in `Authorization: Bearer <token>` header)

- **`GET /conversations/{id}`**: Get a specific conversation and all its messages.
  - **Authentication**: Required (Firebase JWT in `Authorization: Bearer <token>` header)

- **`DELETE /conversations/{id}`**: Delete a conversation and its messages.
  - **Authentication**: Required (Firebase JWT in `Authorization: Bearer <token>` header)

### Image Generation

- **`POST /images/generate`**: Generate an image based on a prompt.
  - **Authentication**: Required (Firebase JWT in `Authorization: Bearer <token>` header)
  - **Request Body**:
    ```json
    {
      "prompt": "A futuristic city skyline at sunset"
    }
    ```
  - **Response**:
    ```json
    {
      "imageUrl": "URL_to_the_generated_image",
      "prompt": "A futuristic city skyline at sunset",
      "remainingImages": 2,
      "generatedAt": "2024-01-15T10:30:00"
    }
    ```

## Deployment & CI/CD

This project is configured for automated deployment to Google Cloud Run.

### Manual Deployment Steps (for first-time setup)

The initial deployment involves setting up the Google Cloud environment and deploying the application manually once to create the Cloud Run service.

1.  **Build the Docker Image**: The `Dockerfile` uses a multi-stage build with Maven 3.9 on Eclipse Temurin 21 to compile the application, then packages it into a lightweight JRE 21 runtime image.
2.  **Push to Artifact Registry**: The built Docker image is pushed to Google Artifact Registry (`us-central1-docker.pkg.dev/spring-ai-auth/spring-ai-repo/aiservices-backend`).
3.  **Deploy to Cloud Run**: The container from Artifact Registry is deployed to Google Cloud Run service `aiservices-backend` in the `us-central1` region. This command also:
    - Connects the service to the Cloud SQL database instance.
    - Injects secrets (API keys, database credentials) from Google Secret Manager.
    - Sets environment variables (e.g., `SPRING_PROFILES_ACTIVE=production`).

### Zero-Downtime CI/CD with GitHub Actions

The `.github/workflows/deploy.yml` file defines the CI/CD pipeline, which automates the manual steps above. On every push to the `main` branch, the workflow does the following:

1.  **Checks out the code.**
2.  **Authenticates with Google Cloud** using a service account key stored in GitHub Secrets.
3.  **Configures Docker** to push to Artifact Registry.
4.  **Builds and pushes the Docker image** to Artifact Registry.
5.  **Deploys the new image to Cloud Run**, updating the existing service.

This setup ensures that the latest version of the application is always deployed without manual intervention.

## Project Structure

```
.
├── .github/workflows/deploy.yml  # GitHub Actions CI/CD pipeline
├── Dockerfile                    # Defines the container image
├── pom.xml                       # Maven dependencies and project info
└── src
    └── main
        ├── java/com/creedpetitt/aiservicesbackend
        │   ├── aiservices/         # Implementations for each AI provider
        │   ├── config/             # Spring Security and Firebase config
        │   ├── controllers/        # REST API controllers
        │   ├── models/             # JPA data models
        │   ├── repositories/       # Spring Data JPA repositories
        │   ├── security/           # Firebase authentication filter
        │   └── services/           # Core business logic
        └── resources
            ├── application.properties          # Default application config
            └── application-production.properties # Production-specific config
```

## Quick Start

### Prerequisites
- Java 21+
- PostgreSQL database
- Firebase project with Admin SDK
- API keys for OpenAI, Anthropic
- Google Cloud/VertexAI Service Account

### Local Development
```bash
# Clone and build
git clone https://github.com/Creed-Petitt/spring-ai-backend
cd AiServicesBackend
mvn clean install

# Set environment variables
export OPENAI_API_KEY=your_openai_key
export ANTHROPIC_API_KEY=your_anthropic_key
export DATABASE_USERNAME=your_db_username
export DATABASE_PASSWORD=your_db_password
# Add firebase-service-account.json to src/main/resources/

# Run locally
mvn spring-boot:run
```

## API Consumer: AetheriusAI Frontend

This backend API was designed to be consumed by any client. A first-party reference implementation has been developed using React, which serves as the public-facing web application.

This client application demonstrates the backend's full capabilities, including:
- Secure consumption of JWT-protected endpoints.
- Real-time, streaming responses from the `/messages/stream` endpoint.
- Limited image generation with OpenAI's flagship DALL-E-3 model.
- Stateful management of conversation history.
- Graceful handling of API errors and rate-limiting responses.

The frontend repository can be found [here](https://github.com/Creed-Petitt/spring-ai-frontend).

## Technology Stack

- **Backend**: Spring Boot 3
- **Language**: Java 21
- **AI**: Spring AI (OpenAI, Anthropic, Vertex AI)
- **Database**: PostgreSQL with Spring Data JPA
- **Authentication/Persistence**: Spring Security & Firebase Admin SDK for a multi-client provider
- **Cloud**: Google Cloud Run, Google Cloud SQL, Google Artifact Registry
- **CI/CD**: GitHub Actions
- **Build Tool**: Maven
