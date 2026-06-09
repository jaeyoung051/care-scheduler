# 🏥 방문목욕센터 운영 관리 시스템

Spring Boot + React 기반의 방문목욕센터 내부 운영 관리 시스템입니다. 어르신 관리, 직원 관리, 차량 관리, 목욕 일정 배정 및 최적화 등의 기능을 제공합니다.

## 📋 프로젝트 개요

### 주요 기능
- ✅ **관리자 로그인/권한 관리** (JWT)
- ✅ **어르신 관리** CRUD
- ✅ **직원 관리** CRUD (요양보호사, 기사)
- ✅ **차량 관리** CRUD
- ✅ **팀 관리** (차량 + 직원 그룹)
- ✅ **목욕 일정 관리** CRUD
- ✅ **일정 충돌 검사** (성별, 시간, 방문요양 겹침)
- ✅ **방문요양 시간 관리** (기존 예약된 시간)
- ⏳ **OR-Tools 기반 경로 최적화** (추후)
- ⏳ **Kakao Map API** 연동 (추후)

### 기술 스택

**백엔드:**
- Java 17+
- Spring Boot 3.2
- JPA/Hibernate
- MySQL 8.0 / MariaDB
- JWT (인증)
- Lombok, ModelMapper

**프론트엔드 (별도 프로젝트):**
- React 18+
- TypeScript
- Tailwind CSS

## 📂 프로젝트 구조

```
home-care-backend/
├── src/main/java/com/homecare/
│   ├── config/                       # Spring 설정
│   ├── domain/
│   │   ├── entity/                   # JPA Entity (9개)
│   │   ├── enums/                    # Enum (4개)
│   │   └── vo/                       # 값 객체
│   ├── repository/                   # JPA Repository (8개)
│   ├── service/                      # 비즈니스 로직 (7개)
│   ├── controller/                   # REST API (7개)
│   ├── dto/                          # Request/Response DTO
│   ├── exception/                    # 예외 처리
│   ├── security/                     # JWT, 보안
│   ├── util/                         # 유틸리티
│   └── HomeCareApplication.java
│
├── src/main/resources/
│   ├── application.yml               # 메인 설정
│   ├── application-dev.yml           # 개발 환경
│   └── application-prod.yml          # 운영 환경
│
├── src/test/java/com/homecare/      # 단위 테스트
├── pom.xml                           # Maven 설정
├── Dockerfile                        # Docker 이미지
├── docker-compose.yml                # Docker Compose
│
├── 01_SYSTEM_DESIGN.md               # 전체 설계 문서
├── 02_DEVELOPMENT_GUIDE.md           # 개발 가이드
├── 03_CORE_SERVICE_IMPLEMENTATION.md # 핵심 구현 가이드
└── README.md                         # 이 파일
```

## 🚀 빠른 시작

### 필수 사항
- Java 17+
- Maven 3.8+
- MySQL 8.0+ 또는 MariaDB 10.5+
- IntelliJ IDEA 또는 VSCode

### 1단계: 데이터베이스 생성

```bash
mysql -u root -p
CREATE DATABASE home_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2단계: 프로젝트 설정

```bash
cd /Users/user/Desktop/home_care

# Maven 의존성 설치
mvn clean install

# application.yml에서 DB 설정 확인
# spring.datasource.username, password 설정
```

### 3단계: 애플리케이션 실행

```bash
mvn spring-boot:run
```

**접근 가능 URL:**
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- Health Check: http://localhost:8080/api/health

## 📊 데이터 모델

### Entity 관계도

```
User (로그인 계정)
  └─ (시스템 관리)

Elder (어르신) 1 ──┬── * BathSchedule (목욕 일정)
                   └── * CareVisitTime (방문요양 시간)

Employee (직원) 1 ─── * TeamMember (팀 구성원)

Vehicle (차량) 1 ─── * Team (팀)

Team 1 ──┬── * TeamMember
         └── * BathSchedule

ScheduleRule (스케줄 규칙)
```

### Entity 상세

| Entity | 용도 | 주요 필드 |
|--------|------|---------|
| **Elder** | 어르신 정보 | name, gender, address, memo |
| **Employee** | 직원 정보 | name, gender, role (CAREGIVER/DRIVER) |
| **Vehicle** | 차량 정보 | vehicleNumber, model, latitude, longitude |
| **Team** | 팀 정보 | name, vehicle_id |
| **TeamMember** | 팀 구성원 | team_id, employee_id, startDate, endDate |
| **BathSchedule** | 목욕 일정 | elder_id, team_id, serviceDate, startTime, endTime |
| **CareVisitTime** | 방문요양 시간 | elder_id, serviceDate, startTime, endTime |
| **ScheduleRule** | 스케줄 규칙 | ruleCode, ruleValue (60, 65, 10, 30 등) |
| **User** | 로그인 계정 | username, password, role |

## 🔐 API 명세

### 인증
```
POST   /api/auth/login
POST   /api/auth/logout
```

### 어르신 관리
```
GET    /api/elders
GET    /api/elders/{id}
POST   /api/elders
PUT    /api/elders/{id}
DELETE /api/elders/{id}
GET    /api/elders/gender/{gender}
```

### 직원 관리
```
GET    /api/employees
GET    /api/employees/{id}
POST   /api/employees
PUT    /api/employees/{id}
DELETE /api/employees/{id}
```

### 차량 관리
```
GET    /api/vehicles
GET    /api/vehicles/{id}
POST   /api/vehicles
PUT    /api/vehicles/{id}
DELETE /api/vehicles/{id}
PATCH  /api/vehicles/{id}/location
```

### 팀 관리
```
GET    /api/teams
GET    /api/teams/{id}
POST   /api/teams
PUT    /api/teams/{id}
DELETE /api/teams/{id}
GET    /api/teams/{id}/members
POST   /api/teams/{teamId}/members
DELETE /api/teams/{teamId}/members/{memberId}
```

### **목욕 일정 관리** (핵심)
```
GET    /api/schedules                  # 전체 일정
GET    /api/schedules/{id}             # 상세 조회
POST   /api/schedules                  # 일정 생성 (검증 포함)
PUT    /api/schedules/{id}             # 수정
DELETE /api/schedules/{id}             # 삭제

POST   /api/schedules/validate         # 검증만 (미리 확인용)
GET    /api/schedules/date/{date}      # 날짜별
GET    /api/schedules/week/{startDate} # 주간
GET    /api/schedules/elder/{elderId}  # 어르신별

PATCH  /api/schedules/{id}/confirm     # 확정
PATCH  /api/schedules/{id}/complete    # 완료
PATCH  /api/schedules/{id}/cancel      # 취소
```

### 방문요양 시간 관리
```
GET    /api/care-visits
GET    /api/care-visits/elder/{elderId}
POST   /api/care-visits
PUT    /api/care-visits/{id}
DELETE /api/care-visits/{id}
```

### 대시보드
```
GET    /api/dashboard/summary
GET    /api/dashboard/today
GET    /api/dashboard/week
GET    /api/dashboard/stats
```

## ✅ 검증 로직

### 목욕 일정 생성 시 자동 검증

#### 1. 성별 제약 (경고 ⚠️)
```
IF 어르신.gender == "여성" AND 모든 팀원.gender == "남성"
  → WARNING: "여성 어르신에 남성 팀원만 배치됩니다."
```

#### 2. 방문요양 시간 충돌 (에러 ❌)
```
IF 신청 목욕 시간과 기존 방문요양 시간이 겹침
  → ERROR: "방문요양 시간과 중복됩니다. (09:00 ~ 10:00)"
```

#### 3. 팀 시간 중복 (에러 ❌)
```
IF 같은 팀이 같은 날짜에 겹치는 두 일정을 가짐
  → ERROR: "같은 팀의 다른 방문과 시간이 중복됩니다."
```

#### 4. 목욕 시간 유효성 (에러 ❌)
```
IF 목욕시간 < 60분 OR > 65분
  → ERROR: "목욕 시간은 60~65분이어야 합니다."
```

#### 5. 최소 이동시간 (에러 ❌)
```
IF 이동시간 < 10분
  → ERROR: "이동시간은 최소 10분이어야 합니다."
```

## 📈 개발 로드맵

### Phase 1: 기초 인프라 ✅
- [x] pom.xml 의존성
- [x] application.yml 설정
- [x] Entity 설계
- [x] Enum 정의

### Phase 2: 보안 & 공통 (1-2시간)
- [ ] GlobalExceptionHandler
- [ ] SecurityConfig & JWT
- [ ] ApiResponse 구조

### Phase 3: CRUD (2시간)
- [ ] Elder CRUD
- [ ] Employee CRUD
- [ ] Vehicle CRUD
- [ ] Team CRUD

### Phase 4: 핵심 로직 (3시간) ⭐
- [ ] ScheduleValidationService
- [ ] ScheduleService
- [ ] ScheduleController
- [ ] 통합 테스트

### Phase 5: 고급 기능 (1시간)
- [ ] DashboardController
- [ ] Swagger 설정
- [ ] 로깅 최적화

### Phase 6: 추후 확장
- [ ] OR-Tools 통합
- [ ] Kakao Map API
- [ ] React 프론트엔드
- [ ] 성능 최적화 (캐싱, 배치)

## 🧪 테스트

```bash
# 모든 테스트 실행
mvn test

# 특정 테스트만 실행
mvn test -Dtest=ScheduleValidationServiceTest

# 커버리지 리포트
mvn test jacoco:report
```

## 🐳 Docker 배포

```bash
# 이미지 빌드
docker build -t home-care:1.0 .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/home_care \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  home-care:1.0

# Docker Compose로 (MySQL + Spring Boot)
docker-compose up
```

## 📖 설정 파일 설명

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/home_care
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate  # 운영: validate, 개발: update
    show-sql: false
```

## 🔍 디버깅 팁

### 로그 레벨 변경
```yaml
logging:
  level:
    com.homecare: DEBUG
    org.hibernate.SQL: DEBUG
```

### 데이터베이스 확인
```sql
-- 모든 테이블 확인
SHOW TABLES;

-- 어르신 정보
SELECT * FROM elders;

-- 목욕 일정
SELECT * FROM bath_schedules 
WHERE service_date = CURDATE();

-- 방문요양 시간
SELECT * FROM care_visit_times
WHERE service_date = CURDATE();
```

## 📝 코딩 컨벤션

### 네이밍
- Entity: `Elder`, `Employee` (단수형)
- Repository: `ElderRepository`
- Service: `ElderService`
- Controller: `ElderController`
- DTO: `CreateElderRequest`, `ElderResponse`

### 파일 구조
- `src/main/java/com/homecare/[domain]/[Component].java`
- 패키지 명은 기능 기반 (layer 기반 아님)

### 코드 스타일
- Lombok `@Data`, `@Builder` 사용
- `LocalDateTime`, `LocalDate`, `LocalTime` 사용
- Try-catch는 Service 레벨에서만
- Repository는 조회만 (비즈니스 로직 없음)

## 🚨 트러블슈팅

### MySQL 연결 오류
```
"Could not create connection to database server"
→ MySQL 실행 확인: mysql -u root -p
→ DB 존재 확인: CREATE DATABASE home_care;
→ application.yml의 URL/username/password 확인
```

### Hibernate 오류
```
"Unknown column 'xxx' in 'where clause'"
→ Entity 수정 후 application.yml의 ddl-auto를 update로 변경
→ 서버 재시작
```

### 포트 충돌
```
"Address already in use: 8080"
→ 기존 프로세스 종료: lsof -i :8080 | kill -9 <PID>
→ 또는 application.yml에서 server.port 변경
```

## 📞 문의 사항

더 자세한 내용은 다음 문서를 참고하세요:
- [시스템 설계](./01_SYSTEM_DESIGN.md)
- [개발 가이드](./02_DEVELOPMENT_GUIDE.md)
- [핵심 Service 구현](./03_CORE_SERVICE_IMPLEMENTATION.md)

## 📄 라이선스

Private Project (내부용)

---

**Last Updated:** 2024-06-09
**Version:** 1.0.0-MVP
**Status:** 개발 중
