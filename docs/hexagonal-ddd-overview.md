## Hexagonal + DDD 패턴에 따른 프로젝트 구성

### # 배경

- 이 프로젝트는 알림 서비스를 제공하는 Spring Boot 기반의 멀티모듈 프로젝트로, 헥사고날 아키텍처(Ports and Adapters)와 도메인 주도 설계(DDD) 패턴을 적용하여 구성되었습니다.

- 외부 모듈(DB, MQ 등)과의 의존성을 최소화하고 단위 테스트 용이성을 위하여 멀티모듈 구조를 채택하여 각 모듈의 역할을 명확히 분리하였습니다.

- 가능한 한 도메인 로직을 순수하게 유지하고, 기술스택 또는 외부 의존성을 최소화하여 테스트 용이성과 유지보수성을 높이는 것을 목표로 합니다.

<br/>

### # 멀티 모듈 구조
- **Core Modules**
  - **`definition`**: 공통 정의 및 상수, VO
  - **`domain`**: 비즈니스 도메인 모델 및 로직
  - **`application`**: 비즈니스 로직 서비스 및 유스케이스, 포트 설계
  - **`infrastructure`**: 공통 인프라스트럭처 설정 (Jackson, AOP, Scheduler 설정 등)

- **Adapter Modules**
  - **`adapter:web`**: REST API 컨트롤러 (Spring WebFlux, Validation)
  - **`adapter:db`**: 데이터베이스 어댑터 (R2DBC, MariaDB, Flyway)
  - **`adapter:mq`**: 메시지 큐 어댑터 (Kafka, Reactor-Kafka)
  - **`adapter:client`**: 외부 클라이언트 통신 (WebFlux)

- **Bootstrap Module**
  - **`bootstrap`**: 메인 애플리케이션 실행 모듈    

- **Build Configuration**
  - **`build-logic`**: Gradle 공통 빌드 로직 및 플러그인

<br/>

### # 개발시 고려 사항

- Core 모듈은 Adapter 모듈에 의존하지 않도록 설계하여, Core 모듈의 테스트가 Adapter 모듈의 구현에 영향을 받지 않도록 구성 했습니다.

- Adapter 모듈은 특정 기술 스택에 의존하도록 하여, Core 모듈의 변경이 Adapter 모듈에 영향을 주지 않도록 구성 했으며, Adapter 부분의 기술 스택을 변경해도 Core 모듈의 변경 없이 Adapter 모듈만 수정하면 되도록 구성 했습니다.

- Core모듈에서 Port, UseCase를 정의하고, Adapter 모듈에서 해당 Port를 구현하는 방식으로, Core 모듈은 Adapter 모듈의 구현에 의존하지 않도록 구성 했습니다.

<br/>

### # 도메인 설계에서의 순수성 저해 요인과 대응 전략

1. **Domain Layer에서 DB와의 의존성 문제**

    - 고민 
      - 데이터 영속성을 위해 Adapter-DB 모듈에서 Domain <-> Entity 변환하는 과정에서 ID가 필요한 Update, Delete 등의 작업이 발생할 수 있습니다.

      - 데이터 변경을 감지하기 위해 Domain Layer에서 DB의 ID필드를 사용하게 되면, Domain Layer가 DB에 의존하게 되어 순수성을 해칠 수 있습니다.

    - 대응 전략

      - Domain Layer에서 ID를 직접 사용하지 않고 데이터 변경을 반영하려면 기존 데이터를 삭제하여 새로 생성하는 방식으로 처리해야 하는데,
        이러한 방식은 성능 저하를 초래할 수 있습니다. 때문에 Domain 객체에 ID필드를 포함하고 Core 모듈에서는 ID를 사용하지 않도록 설계했습니다.
        대신, ID는 Adapter 모듈에서만 사용하도록 하며 ID 생성의 책임은 Adapter 모듈에 두었습니다. 

      - 이 방식으로 Domain은 ID 필드를 갖고 있지만, ID를 직접적으로 사용하지 않도록 하여 순수성을 유지했습니다. 
        Adapter 모듈에서만 ID를 사용하여 데이터베이스와의 상호작용을 처리하도록 했습니다.

<br/>

2. **Core Layer에서 Infrastructure 모듈 분리**

    - 고민

        - Spring Boot의 기본 설정이나 Jackson, AOP, EventListener 등의 인프라스트럭처 관련 설정을 Domain, Application 모듈에서 직접 사용하게 되면, 
        비즈니스 로직에 프레임워크의 강한 의존성이 생겨 순수성을 해칠 수 있습니다. 또한 이러한 설정이 Core 모듈에 포함되면, Core 모듈의 테스트가 어려워질 수 있습니다.
          
    - 대응 전략

        - Core 모듈에서 인프라스트럭처 관련 설정을 분리하여 Infrastructure 모듈로 이동시켰습니다. 

        - 다른 Core 모듈은 인프라스트럭처 모듈에 의존하지 않도록 설계하여, Core 모듈의 테스트가 인프라스트럭처 설정에 영향을 받지 않도록 했습니다. 

        - 또한 @UnitOfWork 같은 트랜잭션 관리 기능은 Infrastructure 모듈에서 제공하여, Core 모듈이 직접적으로 트랜잭션 관리에 의존하지 않도록 했습니다.
