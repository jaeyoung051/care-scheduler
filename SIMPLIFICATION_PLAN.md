# MVP 아키텍처 단순화 계획

**목표**: 엑셀 기반 스케줄링 시스템을 웹 기반으로 전환 (내부용 최소 기능)

---

## 1️⃣ 삭제/보류 파일 목록

### 🗑️ 즉시 삭제 대상
- `User.java` - 로그인/권한 불필요
- `Employee.java` - 직원 일지는 별도 관리
- `Team.java` - 차량 직관리로 충분
- `TeamMember.java` - 팀 구성원 추적 불필요
- `ScheduleRule.java` - 초기 MVP에서 하드코딩
- 관련 Repository (UserRepository, EmployeeRepository, TeamRepository, etc.)
- 관련 Service 클래스
- 관련 Controller
- JWT/Security 설정
- Application.yml의 JWT, Security 관련 설정

### ⚠️ 보류 (향후 확장 시)
- 실제 지도 API 연동
- OR-Tools 최적화 엔진
- 실시간 GPS 추적
- 복잡한 검증 규칙 확장

---

## 2️⃣ 남길 4개 핵심 엔티티

### Elder (어르신)
```
- id (PK)
- name (이름)
- gender (MALE/FEMALE)
- address (기본 주소)
- region (지역 코드: 강남, 서초, 송파 등)
- allowMaleStaff (남성 종사자 허용 여부)
- memo (특수 요청사항)
- createdAt
- updatedAt
```

### Vehicle (차량)
```
- id (PK)
- name (차량명: "1호차", "2호차")
- hasMaleStaff (남성 종사자 포함 여부)
- memo (특이사항)
- createdAt
- updatedAt
```

### BathSchedule (목욕 일정)
```
- id (PK)
- elder (FK → Elder)
- vehicle (FK → Vehicle)  // team 대신 vehicle로 변경
- serviceDate (일정 날짜)
- startTime (시작 시간)
- endTime (종료 시간)      // 새로 추가 (기존: bathDuration만 있음)
- memo (특이사항)
- createdAt
- updatedAt
```

### CareVisitTime (방문요양 시간)
```
- id (PK)
- elder (FK → Elder)
- dayOfWeek (요일: MONDAY ~ SUNDAY)  // 변경: 고정 요일 관리
- startTime (시작 시간)
- endTime (종료 시간)
- memo (메모)
- createdAt
- updatedAt
```

---

## 3️⃣ 검증 규칙 (ScheduleValidationService)

### Rule 1: 동일 차량 시간 겹침
```
SELECT * FROM bath_schedules 
WHERE vehicle_id = ? 
  AND service_date = ? 
  AND NOT (end_time <= ? OR start_time >= ?)
  
→ 결과 있으면 CONFLICT
```

### Rule 2: 목욕 시간 검사
```
duration = endTime - startTime
IF duration < 60분 → ERROR/WARNING
IF duration > 90분 → WARNING (비정상)
```

### Rule 3: 방문요양 시간 겹침
```
SELECT * FROM care_visit_times 
WHERE elder_id = ? 
  AND day_of_week = DAYOFWEEK(serviceDate) 
  AND NOT (endTime <= ? OR startTime >= ?)
  
→ 결과 있으면 CONFLICT
```

### Rule 4: 성별/차량 제약
```
IF elder.gender == FEMALE 
   AND elder.allowMaleStaff == false
   AND vehicle.hasMaleStaff == true
→ REJECT or STRONG WARNING
```

---

## 4️⃣ Repository 구조

```
repository/
├── ElderRepository
│   ├── findAll()
│   ├── findById(Long)
│   ├── findByGender(Gender)
│   ├── findByRegion(String)
│   └── findByNameContaining(String)
│
├── VehicleRepository
│   ├── findAll()
│   ├── findById(Long)
│   ├── findByHasMaleStaff(Boolean)
│   └── findByName(String)
│
├── BathScheduleRepository
│   ├── findByServiceDate(LocalDate)
│   ├── findByElderId(Long)
│   ├── findByVehicleId(Long)
│   ├── findByServiceDateAndVehicleId(LocalDate, Long)
│   └── findByServiceDateAndElderIdAndVehicleId(...)
│
└── CareVisitTimeRepository
    ├── findByElderId(Long)
    ├── findByElderIdAndDayOfWeek(Long, DayOfWeek)
    └── deleteByElderIdAndDayOfWeek(Long, DayOfWeek)
```

---

## 5️⃣ DTO 구조

```
dto/
├── request/
│   ├── CreateElderRequest
│   ├── UpdateElderRequest
│   ├── CreateBathScheduleRequest
│   ├── UpdateBathScheduleRequest
│   ├── CreateCareVisitTimeRequest
│   └── CreateVehicleRequest
│
└── response/
    ├── ElderResponse (id, name, gender, region, allowMaleStaff, ...)
    ├── VehicleResponse (id, name, hasMaleStaff, ...)
    ├── BathScheduleResponse (id, elder, vehicle, serviceDate, startTime, endTime, ...)
    ├── CareVisitTimeResponse (id, elder, dayOfWeek, startTime, endTime, ...)
    └── ScheduleSlotResponse (vehicle, startTime, endTime, score, reason)
```

---

## 6️⃣ Service 구조

### BathScheduleService
- createSchedule(CreateBathScheduleRequest)
- updateSchedule(Long, UpdateBathScheduleRequest)
- deleteSchedule(Long)
- getSchedulesByDate(LocalDate)
- getSchedulesByElder(Long)
- getSchedulesByVehicle(Long)
- getDailySchedule(LocalDate) → List<BathScheduleResponse> (정렬됨)

### ScheduleValidationService (새로 작성)
- validateVehicleTimeConflict(...)
- validateBathDuration(...)
- validateCareVisitTimeConflict(...)
- validateGenderConstraint(...)
- validateSchedule(...) → ValidationResult

### ScheduleRecommendationService (새로 작성)
- recommendSlots(RecommendSlotRequest) → List<ScheduleSlotResponse>
  - Rule 1: 성별/차량 제약으로 가능한 차량 필터
  - Rule 2: 방문요양 시간 제외
  - Rule 3: 기존 일정 조회
  - Rule 4: 이동 비용 계산 및 정렬
  - 상위 2-3개 반환

### ElderService, VehicleService, CareVisitTimeService
- 표준 CRUD

---

## 7️⃣ Controller API 명세

### Elder API
```
GET    /api/elders                    → List<ElderResponse>
GET    /api/elders/{id}               → ElderResponse
GET    /api/elders?gender=FEMALE      → List<ElderResponse>
GET    /api/elders?region=강남         → List<ElderResponse>
POST   /api/elders                    → ElderResponse (body: CreateElderRequest)
PUT    /api/elders/{id}               → ElderResponse (body: UpdateElderRequest)
DELETE /api/elders/{id}               → 204 No Content
```

### Vehicle API
```
GET    /api/vehicles                  → List<VehicleResponse>
GET    /api/vehicles/{id}             → VehicleResponse
POST   /api/vehicles                  → VehicleResponse (body: CreateVehicleRequest)
PUT    /api/vehicles/{id}             → VehicleResponse
DELETE /api/vehicles/{id}             → 204 No Content
```

### BathSchedule API
```
GET    /api/schedules                                  → List<BathScheduleResponse>
GET    /api/schedules?date=2024-01-15                 → List<BathScheduleResponse> (일일 일정)
GET    /api/schedules?elder={elderId}                 → List<BathScheduleResponse>
GET    /api/schedules?vehicle={vehicleId}             → List<BathScheduleResponse>
GET    /api/schedules/{id}                            → BathScheduleResponse
POST   /api/schedules                                 → BathScheduleResponse (body: CreateBathScheduleRequest)
PUT    /api/schedules/{id}                            → BathScheduleResponse
DELETE /api/schedules/{id}                            → 204 No Content
```

### CareVisitTime API
```
GET    /api/care-visits/{elderId}                     → List<CareVisitTimeResponse>
GET    /api/care-visits/{elderId}/{dayOfWeek}         → CareVisitTimeResponse
POST   /api/care-visits                               → CareVisitTimeResponse
PUT    /api/care-visits/{elderId}/{dayOfWeek}         → CareVisitTimeResponse
DELETE /api/care-visits/{elderId}/{dayOfWeek}         → 204 No Content
```

### Validation & Recommendation API
```
POST   /api/schedules/validate                        → ValidationResult
POST   /api/schedules/recommend-slots                 → List<ScheduleSlotResponse>
```

---

## 8️⃣ 개발 순서

### Phase 1: 엔티티 & 기본 구조 (1-2일)
1. ✅ Elder, Vehicle, BathSchedule, CareVisitTime 엔티티 정리
2. ✅ Enum 정의 (Gender, DayOfWeek)
3. Repository 생성
4. DTO 생성

### Phase 2: Service 계층 (1-2일)
5. CRUD Service 작성
6. ScheduleValidationService 작성
7. ScheduleRecommendationService 작성

### Phase 3: Controller & API (1일)
8. REST Controller 작성
9. Exception Handling (GlobalExceptionHandler)
10. 기본 테스트

### Phase 4: 프론트엔드 연동 (2-3일)
11. UI 개발 (React 또는 Vue)
12. 통합 테스트
13. 배포

---

## 9️⃣ 오버엔지니어링 피하기 위한 주의점

### ❌ 하지 말 것
1. 복잡한 권한 체계 (로그인/JWT/RBAC)
2. 이벤트 기반 아키텍처
3. 마이크로서비스 분할
4. 복잡한 캐싱 전략
5. 메시지 큐 (RabbitMQ, Kafka)
6. 복잡한 비즈니스 규칙 엔진
7. 무거운 로깅 프레임워크 (ELK)
8. 동시성 제어 (비관적/낙관적 잠금)

### ✅ 할 것
1. 단순한 CRUD + 검증
2. 메모리 기반 추천 알고리즘
3. 지역 코드 기반 이동 비용 추정
4. SQL 쿼리 직접 작성 (최소한의 JPA)
5. LocalDate/LocalTime 활용
6. 정렬 기반 결과 반환

### 🔧 Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/home_care
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
logging:
  level:
    root: WARN
    com.homecare: INFO
```

---

## 🔟 Database 초기화

### 생성 스크립트
```sql
-- 어르신 테이블
CREATE TABLE elders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    address VARCHAR(200),
    region VARCHAR(50),
    allow_male_staff BOOLEAN DEFAULT true,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_gender (gender),
    INDEX idx_region (region)
);

-- 차량 테이블
CREATE TABLE vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    has_male_staff BOOLEAN DEFAULT false,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 목욕 일정 테이블
CREATE TABLE bath_schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    elder_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    service_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (elder_id) REFERENCES elders(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    INDEX idx_service_date (service_date),
    INDEX idx_vehicle_date (vehicle_id, service_date)
);

-- 방문요양 시간 테이블
CREATE TABLE care_visit_times (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    elder_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (elder_id) REFERENCES elders(id),
    UNIQUE KEY uk_elder_day (elder_id, day_of_week)
);
```

---

## 다음 단계

1. 이 계획 검토 및 피드백
2. 4개 엔티티 코드 작성
3. Repository/DTO 작성
4. Service 계층 구현
5. Controller 작성
6. 테스트

**예상 소요 시간**: 3-5일 (풀타임 1명 기준)
