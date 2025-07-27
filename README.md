# Notification Server

- 알림 서비스를 제공하는 Spring Boot 기반의 멀티모듈 프로젝트입니다. 
    Hexagonal 아키텍처와 DDD를 적용하여 유지보수성과 확장성을 고려하여 설계되었습니다.

- 프로젝트는 알림 요청을 처리하고, Template, Language, Channel을 통해 사용자에게 알림을 전송하는 기능을 제공합니다.


<br/>

## 🛠️ 기술 스택

- **Java 17+**
- **Spring Boot 3.5.0**
- **Spring WebFlux** (Reactive)
- **Spring Data R2DBC** (Reactive Database)
- **Apache Kafka** (Message Queue)
- **MariaDB** (Database)
- **Gradle** (Build Tool)
- **Docker Compose** (Container Orchestration)
- **TestContainers** (Integration Testing)

<br/>

## 🏗️ 아키텍처

이 프로젝트는 **Hexagonal(Ports and Adapters)** + **DDD(Domain-Driven Design)** 패턴을 따릅니다:

- **Domain Layer**: 핵심 비즈니스 로직
- **Application Layer**: 유스케이스 구현
- **Infrastructure Layer**: 기술적 구현체
- **Adapter Layer**: 외부 시스템과의 인터페이스

<br/>

## 📁 모듈 구조

### Core Modules
- **`definition`** - 공통 정의 및 상수
- **`domain`** - 비즈니스 도메인 모델 및 로직
- **`application`** - 애플리케이션 서비스 및 유스케이스
- **`infrastructure`** - 공통 인프라스트럭처 설정 (Jackson, 설정 등)

### Adapter Modules
- **`adapter:web`** - REST API 컨트롤러 (Spring WebFlux, Validation)
- **`adapter:db`** - 데이터베이스 어댑터 (R2DBC, MariaDB, Flyway)
- **`adapter:mq`** - 메시지 큐 어댑터 (Kafka, Reactor-Kafka)
- **`adapter:client`** - 외부 클라이언트 통신 (WebFlux)

### Bootstrap Module
- **`bootstrap`** - 메인 애플리케이션 실행 모듈

### Build Configuration
- **`build-logic`** - Gradle 공통 빌드 로직 및 플러그인

<br/>

## 🚀 실행 방법

### Docker Compose로 실행
```bash
docker-compose up -d
```

<br/>

## 📖 사용자 알림요청 처리 프로세스

- > [사용자 알림요청 처리 프로세스](./docs/notification-request-process.md)

<br/>

## 📚 기술적 이슈와 해결방안

- > [Hexagonal + DDD 패턴에 따른 프로젝트 구성](./docs/hexagonal-ddd-overview.md)

- > [멱등성 키를 활용한 중복 방지 전략](./docs/idempotency-key-handling.md)

- > [Outbox Transaction Pattern을 사용한 메시지 수신](./docs/outbox-transaction-pattern.md)

- > [다중 인스턴스 환경에서 스케줄링 시 데이터 중복 조회 방지 전략](./docs/select-locking-strategy.md)

<br/>