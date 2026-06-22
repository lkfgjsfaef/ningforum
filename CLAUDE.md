# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=ClassName

# Package as JAR
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080` with context path `/` (no prefix).

## Project Overview

NingForum is a campus social/forum platform built with Spring Boot 3.2.5 + Java 17. It serves two frontends from a single backend:

| Frontend | Package | Auth | Rendering |
|----------|---------|------|-----------|
| **PC Admin Panel** | `com.pc.*` | Session-based (HttpSession) | Thymeleaf server-rendered HTML |
| **Mobile/App API** | `com.app.*` | Token-based (Sa-Token) | REST JSON (`@RestController`) |

## Architecture

```
com.pc.controller/          # PC @Controller — Thymeleaf views under /admin/*
com.app.controller/         # App @RestController — JSON APIs under /api/*
com.pc.service/             # PC service interfaces
com.app.service/            # App service interfaces
com.pc.service.impl/        # PC service implementations
com.app.service.impl/       # App service implementations
com.pc.dao/                 # MyBatis mapper interfaces (PC)
com.app.dao/                # MyBatis mapper interfaces (App)
com.pc.pojo/                # Entity/VO classes (Post, User, Tag, Category, Report, Message, etc.)
com.app.pojo/               # App-specific entity/VO classes
com.pc.config/              # RedisConfig, WebMvcConfig (static resource paths)
com.app.config/             # CorsConfig, JacksonConfig, SmsConfig, RabbitMQConfig
com.app.common/             # Result<T>, CResult<T>, ApiResponse, GlobalExceptionHandler, VerificationCodeManager
com.app.mq/                 # RabbitMQ: messages + consumer/producer (chat, notification, cache invalidation)
com.pc.utils/               # RedisCacheUtil, OssUtil
```

**Mapping XML files:**
- PC mappers: `src/main/resources/mapper/*.xml`
- App mappers: `src/main/resources/appmapper/*.xml`
- Thymeleaf templates: `src/main/resources/templates/html/*.html`
- Static assets: `src/main/resources/static/` (H+ admin theme)

## Key Conventions

### Response Patterns

- **PC controllers**: Return `Map<String, Object>` with `success`/`message`/`data` keys, or Thymeleaf view names with `Model` attributes
- **App controllers**: Return `Result<T>` (from `com.app.common`) with `code`/`message`/`data`, or `CResult<T>` for client-facing APIs, or raw `Map<String, Object>` with `success`/`message`
- **Global exception handler**: `com.app.common.GlobalExceptionHandler` catches exceptions only for `com.app.controller` package

### Auth

- PC admin: Login via `LoginController`, user stored in `HttpSession` as `"loginUser"`, session timeout 30 minutes
- App API: Sa-Token (`cn.dev33:sa-token-spring-boot3-starter`) with Redis-backed token storage, token name `Authorization`, timeout 7200s, auto-renew active

### Service Naming

- PC-side service interfaces use standard names: `PostService`, `UserService`, `TagService`, etc.
- Some service implementations have "Chang" suffix (e.g., `UserServiceChang`, `UserServiceChangImpl`) — this is the active implementation that overrides the base interface
- App-side services prefix with module letter: `CUserService` (Client User), `SPostService` (Social Post), `WCircleService`, `WErrandService`, etc.

### Post Categories & Statuses

- **Categories**: 1=Circle (校园圈), 2=Errand (跑腿), 3=SecondHand (二手交易), 4=LostFound (失物招领)
- **Statuses**: 0=pending, 1=approved, 2=deleted, 3=ended, 4=rejected
- Status transitions allowed in `PostsController.updatePostStatus`: only 0→1 or 0→4

### Caching

- `RedisCacheUtil` wraps `RedisTemplate` with key prefix `forum:`, supports cache-through with distributed locks, random TTL jitter, and null-value caching
- Cache invalidation propagates via RabbitMQ (`CacheInvalidationMessage`) for distributed consistency

### RabbitMQ

Three exchange/routing-key pairs (defined in `RabbitMQConfig`):
- Notification exchange — push notifications
- Message exchange — private chat messages
- Cache exchange — cache invalidation broadcasts

### Scheduled Tasks

`AutoUnbanTask` runs every hour (`@Scheduled cron = "0 0 */1 * * ?"`) to auto-unban users whose ban time has expired.

## External Services

- **AliCloud OSS**: File/avatar upload via `OssUtil` — bucket `inforum`, endpoint `oss-cn-wulanchabu`
- **AliCloud SMS**: Verification codes via `dysmsapi20170525` SDK — sign name configured in application-dev.yml

## Configuration Setup

Copy the example config and fill in your own credentials:

```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
# Edit application-dev.yml with your real DB/Redis/RabbitMQ/AliCloud credentials
```

The real `application-dev.yml` is gitignored. The `.example` file serves as the template.

## Important Notes

- The project uses **field injection** (`@Autowired` on fields) rather than constructor injection throughout
- `OssUtil` reads credentials from `application.yml` via `@Value` (aliyun.oss.*) — set these before running
- `mapper-locations` scans both `classpath:mapper/*.xml` and `classpath:appmapper/*.xml`
- `@ComponentScan({"com.pc", "com.app"})` and `@MapperScan({"com.pc.dao", "com.app.dao"})` are explicitly configured on the main class
- MyBatis SQL logging is enabled (`StdOutImpl`) — remove for production
