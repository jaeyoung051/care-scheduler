# 🎯 즉시 개발 시작 체크리스트

## 📋 현재 완료된 것

✅ 시스템 설계 (01_SYSTEM_DESIGN.md)
- 전체 아키텍처
- Entity 설계 (9개)
- Repository 인터페이스 명세
- Service 메서드 명세
- Controller API 명세
- 검증 로직 상세 설명

✅ 개발 가이드 (02_DEVELOPMENT_GUIDE.md)
- 프로젝트 초기화 방법
- IDE 설정
- 개발 우선순위
- 디렉토리별 파일 작성 순서
- ElderService 예시 코드

✅ 핵심 Service 구현 (03_CORE_SERVICE_IMPLEMENTATION.md)
- ScheduleValidationService 완전한 구현 코드
- ScheduleService 완전한 구현 코드
- 예외 처리 클래스
- 유틸리티 클래스
- 테스트 케이스 8개

✅ Entity 클래스들 (스켈레톤)
- User.java
- Elder.java
- Employee.java
- Vehicle.java
- Team.java
- TeamMember.java
- BathSchedule.java
- CareVisitTime.java
- ScheduleRule.java
- Enum (Gender, ScheduleStatus, EmployeeRole, UserRole)

✅ 설정 파일
- pom.xml (모든 의존성)
- application.yml (DB, JWT, 로깅 설정)

---

## 🚀 즉시 시작하기 (다음 20분)

### Step 1: 데이터베이스 생성 (2분)

```bash
mysql -u root -p
CREATE DATABASE home_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### Step 2: IDE 열기 (2분)

- **IntelliJ IDEA:**
  - File → Open → `/Users/user/Desktop/home_care/pom.xml`
  - Trust Project
  - Maven 자동 동기화 대기 (2~3분)

- **VSCode:**
  - Extension Pack for Java 설치
  - Folder 열기: `/Users/user/Desktop/home_care`
  - 필요 시 Java Version 선택

### Step 3: HomeCareApplication.java 생성 (3분)

`src/main/java/com/homecare/HomeCareApplication.java` 생성:

```java
package com.homecare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.modelmapper.ModelMapper;

@SpringBootApplication
public class HomeCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeCareApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
```

### Step 4: 첫 실행 (3분)

```bash
mvn clean install
mvn spring-boot:run
```

**성공 메시지:**
```
Tomcat started on port(s): 8080 (http)
Started HomeCareApplication in X.XXX seconds
```

### Step 5: 상태 확인 (2분)

```bash
# 헬스 체크
curl http://localhost:8080/api/health

# 응답 예시
{"status":"UP"}
```

---

## 📝 다음 우선순위별 개발 순서

### 우선순위 1️⃣: 예외 처리 & 공통 응답 (15분)

**생성할 파일:**
1. `src/main/java/com/homecare/exception/GlobalExceptionHandler.java`
2. `src/main/java/com/homecare/common/ApiResponse.java`
3. `src/main/java/com/homecare/common/ApiError.java`

**코드 (GlobalExceptionHandler):**
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.error("잘못된 입력: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleScheduleConflictException(
            ScheduleConflictException ex) {
        log.error("일정 충돌: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("서버 오류", ex);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("서버 오류가 발생했습니다."));
    }
}
```

**코드 (ApiResponse):**
```java
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
```

### 우선순위 2️⃣: Repository 인터페이스 (20분)

**생성할 파일:** `src/main/java/com/homecare/repository/`

```java
// ElderRepository.java
public interface ElderRepository extends JpaRepository<Elder, Long> {
    List<Elder> findByIsActiveTrue();
    List<Elder> findByGender(Gender gender);
    Optional<Elder> findByPhoneNumber(String phoneNumber);
}

// EmployeeRepository.java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByIsActiveTrue();
    List<Employee> findByRole(EmployeeRole role);
}

// VehicleRepository.java
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByIsActiveTrue();
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
}

// TeamRepository.java
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByIsActiveTrue();
    List<Team> findByVehicleId(Long vehicleId);
    Optional<Team> findByName(String name);
}

// TeamMemberRepository.java
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamIdAndEndDateIsNull(Long teamId);
    List<TeamMember> findByEmployeeId(Long employeeId);
}

// BathScheduleRepository.java
public interface BathScheduleRepository extends JpaRepository<BathSchedule, Long> {
    List<BathSchedule> findByServiceDateOrderByStartTime(LocalDate serviceDate);
    List<BathSchedule> findByElderId(Long elderId);
    List<BathSchedule> findByTeamIdAndServiceDate(Long teamId, LocalDate serviceDate);
    
    @Query("SELECT b FROM BathSchedule b WHERE b.elder.id = :elderId AND b.serviceDate = :date " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<BathSchedule> findConflictingSchedules(@Param("elderId") Long elderId, 
                                                @Param("date") LocalDate date,
                                                @Param("startTime") LocalTime startTime,
                                                @Param("endTime") LocalTime endTime);
}

// CareVisitTimeRepository.java
public interface CareVisitTimeRepository extends JpaRepository<CareVisitTime, Long> {
    List<CareVisitTime> findByElderIdAndServiceDate(Long elderId, LocalDate serviceDate);
    
    @Query("SELECT c FROM CareVisitTime c WHERE c.elder.id = :elderId AND c.serviceDate = :date " +
           "AND ((c.startTime < :endTime AND c.endTime > :startTime))")
    List<CareVisitTime> findConflictingTimes(@Param("elderId") Long elderId,
                                             @Param("date") LocalDate date,
                                             @Param("startTime") LocalTime startTime,
                                             @Param("endTime") LocalTime endTime);
}

// ScheduleRuleRepository.java
public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, Long> {
    Optional<ScheduleRule> findByRuleCode(String ruleCode);
}

// UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

### 우선순위 3️⃣: DTO 클래스 (30분)

**생성할 위치:** `src/main/java/com/homecare/dto/request/` 및 `response/`

```java
// CreateElderRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateElderRequest {
    @NotBlank private String name;
    @NotNull private Gender gender;
    @NotBlank private String phoneNumber;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private String memo;
}

// ElderResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElderResponse {
    private Long id;
    private String name;
    private Gender gender;
    private String phoneNumber;
    private String address;
    private String memo;
    private Boolean isActive;
}

// CreateBathScheduleRequest.java (중요!)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBathScheduleRequest {
    @NotNull private Long elderId;
    @NotNull private Long teamId;
    @NotNull private LocalDate serviceDate;
    @NotNull private LocalTime startTime;
    @NotNull private LocalTime endTime;
    @NotNull private Integer bathDuration;
    private Integer travelTimeAfter;
    private String notes;
}

// BathScheduleResponse.java (중요!)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BathScheduleResponse {
    private Long id;
    private Long elderId;
    private String elderName;
    private Long teamId;
    private String teamName;
    private LocalDate serviceDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer bathDuration;
    private ScheduleStatus status;
    private LocalDateTime createdAt;
}

// ScheduleValidationResponse.java (중요!)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleValidationResponse {
    private Boolean isValid;
    private List<String> warnings;
    private List<String> errors;
}
```

### 우선순위 4️⃣: Service 클래스 (1시간) ⭐

**생성할 파일:** `src/main/java/com/homecare/service/`

1. **ScheduleValidationService.java** (03_CORE_SERVICE_IMPLEMENTATION.md 참고)
2. **ScheduleService.java** (03_CORE_SERVICE_IMPLEMENTATION.md 참고)
3. **ElderService.java** (02_DEVELOPMENT_GUIDE.md의 예시 참고)
4. **EmployeeService.java** (유사하게 작성)
5. **VehicleService.java** (유사하게 작성)
6. **TeamService.java** (유사하게 작성)
7. **CareVisitService.java** (유사하게 작성)

### 우선순위 5️⃣: Controller 클래스 (45분)

**생성할 파일:** `src/main/java/com/homecare/controller/`

```java
// ScheduleController.java (핵심!)
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleValidationService validationService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateBathScheduleRequest request) {
        try {
            BathScheduleResponse response = scheduleService.create(request);
            return ResponseEntity.ok(ApiResponse.success(response, "일정이 생성되었습니다."));
        } catch (ScheduleConflictException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@Valid @RequestBody CreateBathScheduleRequest request) {
        ScheduleValidationResponse response = validationService.validateSchedule(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<?> getByDate(@PathVariable LocalDate date) {
        List<BathScheduleResponse> response = scheduleService.getSchedulesByDate(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        BathScheduleResponse response = scheduleService.confirm(id);
        return ResponseEntity.ok(ApiResponse.success(response, "일정이 확정되었습니다."));
    }
}

// ElderController.java
@RestController
@RequestMapping("/elders")
@RequiredArgsConstructor
@Slf4j
public class ElderController {

    private final ElderService elderService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateElderRequest request) {
        ElderResponse response = elderService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response, "어르신 정보가 등록되었습니다."));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<ElderResponse> response = elderService.getAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        ElderResponse response = elderService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

---

## 🔧 개발 시 주의사항

### 1. 엔티티 수정 후
```
application.yml 의 spring.jpa.hibernate.ddl-auto 를 'update'로 변경
→ 서버 재시작
→ 개발 완료 후 'validate'로 변경
```

### 2. 트랜잭션 처리
```java
@Service
@Transactional  // 쓰기 작업이 있을 때 필수!
public class ScheduleService {
    // ...
}

@Transactional(readOnly = true)  // 읽기만 할 때 (선택)
public List<BathScheduleResponse> getSchedulesByDate(LocalDate date) {
    // ...
}
```

### 3. 검증 로직은 Service에서
```java
// ❌ 나쁜 예
@PostMapping
public void create(@RequestBody CreateBathScheduleRequest request) {
    // 검증 로직이 없음
}

// ✅ 좋은 예
@PostMapping
public void create(@Valid @RequestBody CreateBathScheduleRequest request) {
    // 1. Service 검증
    ScheduleValidationResponse validation = validationService.validateSchedule(request);
    if (!validation.getIsValid()) {
        throw new ScheduleConflictException("검증 실패");
    }
    // 2. 저장
}
```

### 4. 로깅 활용
```java
log.info("일정 생성: id={}, 어르신={}, 팀={}", saved.getId(), elder.getName(), team.getName());
log.warn("일정 생성 경고: {}", String.join(", ", warnings));
log.error("일정 생성 실패", exception);
```

---

## 📊 현황 요약

| 항목 | 상태 | 예상 시간 |
|------|------|---------|
| 설계 & 문서 | ✅ 완료 | - |
| Entity & Enum | ✅ 완료 | - |
| pom.xml | ✅ 완료 | - |
| application.yml | ✅ 완료 | - |
| **예외 처리** | ⏳ 다음 | 15분 |
| **Repository** | ⏳ 다음 | 20분 |
| **DTO** | ⏳ 다음 | 30분 |
| **Service** | ⏳ 다음 | 1시간 |
| **Controller** | ⏳ 다음 | 45분 |
| **테스트** | ⏳ 다음 | 1시간 |
| **문서화 (Swagger)** | ⏳ 다음 | 30분 |

**총 예상 개발 시간:** 약 3.5~4시간 (MVP)

---

## 🎓 학습 순서

1. **Entity & Repository** 이해하기
   - 각 Entity 간의 관계 파악
   - @ManyToOne, @OneToMany 관계 학습

2. **DTO 패턴** 학습
   - Request/Response 분리
   - ModelMapper 활용

3. **Service 계층** 작성
   - 비즈니스 로직 구현
   - @Transactional 이해

4. **Controller 계층** 작성
   - REST API 설계
   - 예외 처리

5. **테스트** 작성
   - @SpringBootTest 활용
   - 검증 로직 테스트

---

## ✨ 다음 단계 (개발 완료 후)

1. React 프론트엔드 개발
2. JWT 보안 강화
3. OR-Tools 통합
4. Kakao Map API 연동
5. 성능 최적화 (캐싱, 배치 처리)
6. 배포 (Docker, AWS/GCP)

---

**지금 바로 시작하세요! 🚀**

다음 명령어로 개발을 시작하세요:
```bash
cd /Users/user/Desktop/home_care
mvn clean install
mvn spring-boot:run
```
