# Notification Server

알림 서비스를 제공하는 Spring Boot 기반의 멀티모듈 프로젝트입니다.

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

## 🚀 실행 방법

### Docker Compose로 실행
```bash
# Docker Compose를 사용하여 MariaDB와 Kafka를 실행합니다.
docker-compose up -d

# docker-compose가 실행된 후, 애플리케이션을 빌드하고 실행합니다.
cd notification-request
./gradlew clean build :bootstrap:bootRun
```

## 🏗️ 아키텍처

이 프로젝트는 **Hexagonal(Ports and Adapters)** + **DDD(Domain-Driven Design)** 패턴을 따릅니다:

- **Domain Layer**: 핵심 비즈니스 로직
- **Application Layer**: 유스케이스 구현
- **Infrastructure Layer**: 기술적 구현체
- **Adapter Layer**: 외부 시스템과의 인터페이스

## 📚 기술적 이슈와 해결방안

- [다중 인스턴스 환경에서 스케줄링 시 Outbox 중복 조회 방지 전략](docs/outbox-locking-strategy.md)


