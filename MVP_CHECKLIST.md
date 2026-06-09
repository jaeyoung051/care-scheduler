# MVP 아키텍처 단순화 - 최종 체크리스트

---

## ✅ 완성된 항목 (Phase 1-3)

### 📦 엔티티 단순화 (4개만 유지)
- ✅ Elder.java - 단순화 완료 (7개 필드)
- ✅ Vehicle.java - 단순화 완료 (3개 필드)
- ✅ BathSchedule.java - 개편 완료 (team → vehicle, 6개 필드)
- ✅ CareVisitTime.java - 개편 완료 (dayOfWeek 추가, 5개 필드)
- ✅ Gender.java enum
- ✅ DayOfWeek enum (Java 기본)

### 🗂️ Repository (4개)
- ✅ ElderRepository
- ✅ VehicleRepository
- ✅ BathScheduleRepository (검증용 쿼리 포함)
- ✅ CareVisitTimeRepository (충돌 검사 쿼리 포함)

### 📋 DTO (Request/Response)
- ✅ CreateElderRequest / ElderResponse
- ✅ CreateVehicleRequest / VehicleResponse
- ✅ CreateBathScheduleRequest / BathScheduleResponse
- ✅ CreateCareVisitTimeRequest / CareVisitTimeResponse
- ✅ ScheduleSlotResponse (추천 슬롯)

### 🔧 Service 계층
- ✅ ElderService (CRUD)
- ✅ VehicleService (CRUD)
- ✅ BathScheduleService (CRUD + 검증 통합)
- ✅ CareVisitTimeService (CRUD)
- ✅ **ScheduleValidationService** (4가지 검증 규칙)
- ✅ **ScheduleRecommendationService** (추천 슬롯 생성)

### 🌐 Controller (REST API)
- ✅ ElderController
- ✅ VehicleController
- ✅ BathScheduleController (+ validate, recommend-slots 엔드포인트)
- ✅ CareVisitTimeController

---

## 🚀 다음 단계 (구현 순서)

### Phase 4: 통합 설정
1. **ModelMapper 설정**
   ```java
   @Configuration
   public class MapperConfig {
       @Bean
       public ModelMapper modelMapper() {
           return new ModelMapper();
       }
   }
   ```

2. **application.yml 정리**
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate  # 기존 스키마 사용
     datasource:
       url: jdbc:mysql://localhost:3306/home_care
       username: root
       password: root
   ```

3. **GlobalExceptionHandler** (선택사항)
   ```java
   @RestControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(RuntimeException.class)
       public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
           return ResponseEntity.badRequest()
               .body(new ErrorResponse(e.getMessage()));
       }
   }
   ```

### Phase 5: 테스트
1. Controller 통합 테스트
   ```
   POST /api/elders                     // 어르신 등록
   GET  /api/elders/1                   // 어르신 조회
   POST /api/vehicles                   // 차량 등록
   POST /api/schedules                  // 일정 등록 (검증 포함)
   POST /api/schedules/validate         // 검증만 실행
   POST /api/schedules/recommend-slots  // 추천 슬롯 조회
   ```

2. 데이터베이스 스크립트
   ```sql
   INSERT INTO vehicles (name, has_male_staff) VALUES ('1호차', false), ('2호차', true);
   INSERT INTO elders (name, gender, region, allow_male_staff) 
   VALUES ('김○○', 'FEMALE', '강남', false);
   ```

### Phase 6: 배포 준비
1. Dockerfile 작성
2. docker-compose.yml (MySQL + Spring Boot)
3. CI/CD 파이프라인 (GitHub Actions)

---

## ⚠️ 오버엔지니어링 피하기 - 주의점

### ❌ 절대 하지 말 것

| 항목 | 이유 | 대안 |
|------|------|------|
| JWT 토큰 인증 | 내부용, 필요 없음 | 없음 (나중 추가 가능) |
| Role 기반 권한 | 내부용, 모두 같은 권한 | 없음 |
| 복잡한 검증 규칙 | 엔진 불필요 | 4가지 단순 규칙만 |
| 메시지 큐 (RabbitMQ) | MVP에 과도함 | 단순 DB 트랜잭션 |
| 캐싱 (Redis) | 데이터양 적음 | DB 쿼리로 충분 |
| 비동기 처리 | 응답 시간이 빠름 | 동기식 CRUD |
| 실시간 GPS 추적 | 초기 MVP에 불필요 | 나중에 추가 가능 |
| Kakao Map API | 지역 코드로 충분 | 단순 점수 계산 |
| OR-Tools 최적화 | 복잡함, 시간 낭비 | 규칙 기반 추천 |
| 마이크로서비스 | 팀 1명, 과도함 | 모놀리식 |

### ✅ 할 것

| 항목 | 설명 |
|------|------|
| 단순한 CRUD | 표준 REST API |
| SQL 쿼리 직접 작성 | @Query로 최적화 |
| LocalDate/LocalTime | Java 8+ 활용 |
| 트랜잭션 관리 | @Transactional만 사용 |
| 단순 정렬/필터링 | JPA 메서드명 활용 |
| 지역 기반 점수 | "강남" == "강남" ? 낮음 : 높음 |

---

## 📊 파일 구조 최종 정리

```
src/main/java/com/homecare/
├── domain/
│   ├── entity/
│   │   ├── Elder.java ✅
│   │   ├── Vehicle.java ✅
│   │   ├── BathSchedule.java ✅
│   │   └── CareVisitTime.java ✅
│   ├── enums/
│   │   └── Gender.java ✅
│   └── repository/
│       ├── ElderRepository.java ✅
│       ├── VehicleRepository.java ✅
│       ├── BathScheduleRepository.java ✅
│       └── CareVisitTimeRepository.java ✅
├── application/
│   └── service/
│       ├── ElderService.java ✅
│       ├── VehicleService.java ✅
│       ├── BathScheduleService.java ✅
│       ├── CareVisitTimeService.java ✅
│       ├── ScheduleValidationService.java ✅ (핵심)
│       └── ScheduleRecommendationService.java ✅ (핵심)
├── api/
│   ├── dto/
│   │   ├── request/ (4개) ✅
│   │   └── response/ (5개) ✅
│   └── controller/
│       ├── ElderController.java ✅
│       ├── VehicleController.java ✅
│       ├── BathScheduleController.java ✅
│       └── CareVisitTimeController.java ✅

🗑️ 삭제된 파일
├── User.java (로그인 불필요)
├── Employee.java (직원 관리 불필요)
├── Team.java (team 제거, vehicle 직접 사용)
├── TeamMember.java (팀 구성원 추적 불필요)
├── ScheduleRule.java (하드코딩 가능)
├── 관련 Repository/Service/Controller
└── JWT/Security 설정
```

---

## 🔍 검증 규칙 최종 정리

### ScheduleValidationService의 4가지 규칙

**Rule 1: 동일 차량 시간 겹침**
```
SELECT * FROM bath_schedules 
WHERE vehicle_id = ? AND service_date = ? 
  AND NOT (end_time <= ? OR start_time >= ?)
```
- 예: 1호차 2024-01-15 09:00-10:00 등록 시도
- 같은 시간에 1호차의 일정이 있으면 REJECT

**Rule 2: 목욕 시간 검사**
```
duration = endTime - startTime
IF duration < 60분 → ERROR (목욕 불가)
IF duration > 90분 → WARNING (비정상)
```
- 예: 09:00-10:30 (90분) → WARNING
- 예: 09:00-09:45 (45분) → ERROR

**Rule 3: 방문요양 시간 겹침**
```
SELECT * FROM care_visit_times 
WHERE elder_id = ? AND day_of_week = ? 
  AND NOT (endTime <= ? OR startTime >= ?)
```
- 예: 월요일 방문요양 09:00-10:00이 있는데
- 월요일 목욕 09:30-10:30 등록 시도 → REJECT

**Rule 4: 성별/차량 제약**
```
IF elder.gender == FEMALE 
   AND elder.allowMaleStaff == false
   AND vehicle.hasMaleStaff == true
→ WARNING (확인 필요)
```
- 예: 여성 어르신(남성 거부) + 2호차(남성 기사) → WARNING

---

## 📈 성능 최적화 (선택사항, 나중에)

| 항목 | 현재 | 개선 |
|------|------|------|
| 데이터베이스 | MySQL 단일 | 읽기 복제본 (나중) |
| 캐싱 | 없음 | Redis (나중) |
| 검색 | 단순 쿼리 | Elasticsearch (나중) |
| 실시간 | 없음 | WebSocket (나중) |
| 배포 | 단일 인스턴스 | Kubernetes (나중) |

---

## 🎯 MVP 완성 기준

### 필수 (반드시 구현)
- ✅ 4개 엔티티 CRUD
- ✅ 4가지 검증 규칙
- ✅ REST API 엔드포인트
- ✅ 데이터베이스 연동

### 선택 (있으면 좋음)
- ⭐ 신규 추천 슬롯 기능
- ⭐ ValidationResult 반환
- ⭐ 예외 처리

### 나중에 (확장 사항)
- 🚀 Kakao Map API 연동
- 🚀 OR-Tools 최적화
- 🚀 실시간 GPS 추적
- 🚀 로그인/권한 추가
- 🚀 프론트엔드 UI

---

## 💡 빠른 시작 (5분)

```bash
# 1. 프로젝트 실행
mvn spring-boot:run

# 2. 초기 데이터 입력
curl -X POST http://localhost:8080/api/vehicles \
  -H "Content-Type: application/json" \
  -d '{"name":"1호차","hasMaleStaff":false}'

curl -X POST http://localhost:8080/api/vehicles \
  -H "Content-Type: application/json" \
  -d '{"name":"2호차","hasMaleStaff":true}'

# 3. 어르신 등록
curl -X POST http://localhost:8080/api/elders \
  -H "Content-Type: application/json" \
  -d '{"name":"김○○","gender":"FEMALE","region":"강남","allowMaleStaff":false}'

# 4. 일정 등록
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "elderId":1,
    "vehicleId":1,
    "serviceDate":"2024-01-15",
    "startTime":"09:00",
    "endTime":"10:00"
  }'

# 5. 추천 슬롯 조회
curl "http://localhost:8080/api/schedules/recommend-slots?elderId=1&serviceDate=2024-01-16"
```

---

## 🎓 주요 학습 포인트

이 MVP를 통해 얻을 것:
1. **DDD 원칙** - 도메인 모델 설계
2. **JPA/Hibernate** - 엔티티 매핑
3. **검증 로직** - 비즈니스 규칙 구현
4. **REST API** - 엔드포인트 설계
5. **트랜잭션 관리** - @Transactional 활용

다음 버전에서 추가할 것:
1. 인증/인가 (JWT)
2. 캐싱 (Redis)
3. 검색 (Elasticsearch)
4. 최적화 (OR-Tools)
5. 프론트엔드 (React/Vue)

---

**현재 상황**: MVP 구조 100% 완성
**다음 할 일**: 컴파일 테스트 → 데이터베이스 스크립트 → API 테스트
