# 방문목욕센터 운영 시스템 - Spring Boot 백엔드 설계

## 📋 목차
1. [전체 패키지 구조](#전체-패키지-구조)
2. [도메인 엔티티 설계](#도메인-엔티티-설계)
3. [ERD 관계도](#erd-관계도)
4. [DTO 구조](#dto-구조)
5. [Repository 구조](#repository-구조)
6. [Service 구조](#service-구조)
7. [Controller API 명세](#controller-api-명세)
8. [일정 배정 검증 로직](#일정-배정-검증-로직)
9. [추후 확장 구조](#추후-확장-구조)
10. [개발 순서 및 우선순위](#개발-순서-및-우선순위)

---

## 전체 패키지 구조

```
home-care-backend
├── src/main/java/com/homecare
│   ├── config/                      # Spring 설정
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   └── WebConfig.java
│   │
│   ├── domain/                      # 도메인 엔티티 & 값 객체
│   │   ├── entity/
│   │   │   ├── Elder.java                    # 어르신
│   │   │   ├── Employee.java                 # 직원
│   │   │   ├── Vehicle.java                  # 차량
│   │   │   ├── Team.java                     # 팀
│   │   │   ├── TeamMember.java               # 팀 구성원
│   │   │   ├── BathSchedule.java             # 목욕 일정
│   │   │   ├── CareVisitTime.java            # 방문요양 시간 (어르신이 이미 예약한 방문요양 시간)
│   │   │   ├── ScheduleRule.java             # 스케줄 규칙 (추후 OR-Tools용 제약조건)
│   │   │   └── User.java                     # 로그인 사용자 (관리자)
│   │   │
│   │   ├── enums/
│   │   │   ├── Gender.java                   # MALE, FEMALE
│   │   │   ├── ScheduleStatus.java           # PENDING, CONFIRMED, COMPLETED, CANCELLED
│   │   │   ├── EmployeeRole.java             # CAREGIVER, DRIVER, ADMIN
│   │   │   └── UserRole.java                 # ADMIN, MANAGER
│   │   │
│   │   └── vo/                      # 값 객체 (추후 확장)
│   │       ├── Location.java                 # 위도/경도 (Kakao Map 연동용)
│   │       └── TimeSlot.java                 # 시간 구간 (HH:mm ~ HH:mm)
│   │
│   ├── dto/                         # DTO (요청/응답)
│   │   ├── request/
│   │   ├── response/
│   │   └── mapper/                  # ModelMapper 또는 MapStruct 설정
│   │
│   ├── repository/                  # JPA Repository
│   │   ├── ElderRepository.java
│   │   ├── EmployeeRepository.java
│   │   ├── VehicleRepository.java
│   │   ├── TeamRepository.java
│   │   ├── TeamMemberRepository.java
│   │   ├── BathScheduleRepository.java
│   │   ├── CareVisitTimeRepository.java
│   │   └── UserRepository.java
│   │
│   ├── service/                     # 비즈니스 로직
│   │   ├── ElderService.java
│   │   ├── EmployeeService.java
│   │   ├── VehicleService.java
│   │   ├── TeamService.java
│   │   ├── ScheduleService.java            # 목욕 일정 관리
│   │   ├── ScheduleValidationService.java  # 일정 검증 로직
│   │   ├── CareVisitService.java           # 방문요양 시간 관리
│   │   └── AuthService.java
│   │
│   ├── controller/                  # REST API
│   │   ├── ElderController.java
│   │   ├── EmployeeController.java
│   │   ├── VehicleController.java
│   │   ├── TeamController.java
│   │   ├── ScheduleController.java
│   │   ├── AuthController.java
│   │   └── DashboardController.java        # 대시보드용 집계 API
│   │
│   ├── exception/                   # 예외 처리
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ScheduleConflictException.java
│   │   ├── GenderMismatchException.java
│   │   └── ValidationException.java
│   │
│   ├── security/                    # JWT, 권한
│   │   ├── JwtProvider.java
│   │   ├── CustomUserDetailsService.java
│   │   └── JwtAuthenticationFilter.java
│   │
│   ├── util/                        # 유틸리티
│   │   ├── TimeCalculator.java              # 이동시간, 목욕시간 계산
│   │   ├── ScheduleValidator.java           # 검증 로직 모음
│   │   └── DateTimeUtil.java
│   │
│   └── HomeCareApplication.java     # 메인 클래스
│
├── src/main/resources/
│   ├── application.yml              # 기본 설정
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/
│       └── migration/               # Flyway DB 마이그레이션 (선택)
│
├── pom.xml                          # Maven 설정
└── README.md
```

---

## 도메인 엔티티 설계

### 1. User (로그인 사용자 - 관리자)
```java
@Entity
@Table(name = "users")
@Getter @Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;      // 아이디
    
    @Column(nullable = false)
    private String password;      // 암호화된 비밀번호
    
    @Column(nullable = false)
    private String name;          // 이름 (센터장, 관리자 이름)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;        // ADMIN, MANAGER
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime lastLoginAt;
    
    @Column(nullable = false)
    private Boolean isActive;
}
```

### 2. Elder (어르신)
```java
@Entity
@Table(name = "elders")
@Getter @Setter
public class Elder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;         // 어르신 이름
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;        // MALE, FEMALE
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column
    private String address;       // 주소
    
    @Column
    private String addressDetail; // 상세주소
    
    @Column
    private Double latitude;      // 위도 (Kakao Map 연동용)
    
    @Column
    private Double longitude;     // 경도
    
    @Column
    private String caregiver;     // 담당 요양보호사 (1차)
    
    @Column(columnDefinition = "TEXT")
    private String memo;          // 특수 요청사항 (예: 남성 종사자 거부 등)
    
    @Column
    private Boolean preferredCaregiverRequired; // 특정 요양보호사만 가능한지 여부
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    // 한 어르신은 여러 목욕 일정을 가짐
    @OneToMany(mappedBy = "elder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BathSchedule> bathSchedules = new ArrayList<>();
    
    // 한 어르신은 여러 방문요양 시간을 가짐
    @OneToMany(mappedBy = "elder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CareVisitTime> careVisitTimes = new ArrayList<>();
}
```

### 3. Employee (직원)
```java
@Entity
@Table(name = "employees")
@Getter @Setter
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;         // 직원 이름
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;        // MALE, FEMALE
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeRole role;    // CAREGIVER(요양보호사), DRIVER(기사)
    
    @Column
    private String caregiverLicense; // 요양보호사 자격증 번호 (CAREGIVER인 경우)
    
    @Column
    private Boolean driverLicense;    // 운전면허 보유여부 (DRIVER인 경우)
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    // 한 직원은 여러 팀에 속할 수 있음 (팀 순환 배정)
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> teamMembers = new ArrayList<>();
}
```

### 4. Vehicle (차량)
```java
@Entity
@Table(name = "vehicles")
@Getter @Setter
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String vehicleNumber;     // 차량번호 (예: 12가1234)
    
    @Column(nullable = false)
    private String model;             // 차종 (예: 그랜저, 아반떼)
    
    @Column
    private String color;             // 색상
    
    @Column
    private LocalDate registrationDate; // 등록일
    
    @Column
    private LocalDate inspectionDate;   // 검사만료일
    
    @Column
    private String driver;            // 배정 기사 이름 (고정 배정인 경우)
    
    @Column
    private Double latitude;          // 현재 위도
    
    @Column
    private Double longitude;         // 현재 경도
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    // 한 차량은 여러 팀에 속할 수 있음
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams = new ArrayList<>();
}
```

### 5. Team (팀)
```java
@Entity
@Table(name = "teams")
@Getter @Setter
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;         // 팀 이름 (예: Team A, 오전팀)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;      // 배정 차량
    
    @Column
    private String description;   // 팀 설명
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    // 한 팀은 여러 팀 구성원을 가짐
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();
    
    // 한 팀은 여러 목욕 일정을 처리함
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BathSchedule> bathSchedules = new ArrayList<>();
}
```

### 6. TeamMember (팀 구성원)
```java
@Entity
@Table(name = "team_members")
@Getter @Setter
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(nullable = false)
    private LocalDate startDate;  // 팀 배정 시작일
    
    @Column
    private LocalDate endDate;    // 팀 배정 종료일 (null이면 현재)
    
    @Column(nullable = false)
    private Integer assignmentOrder; // 같은 팀 내 순서 (1, 2, ...)
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Unique(columnNames = {"team_id", "employee_id"})
    private Boolean isActive;
}
```

### 7. BathSchedule (목욕 일정)
```java
@Entity
@Table(name = "bath_schedules")
@Getter @Setter
public class BathSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elder_id", nullable = false)
    private Elder elder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;           // 배정 팀 (팀에 포함된 요양보호사 + 기사)
    
    @Column(nullable = false)
    private LocalDate serviceDate; // 방문 날짜
    
    @Column(nullable = false)
    private LocalTime startTime;  // 방문 시작 시간
    
    @Column(nullable = false)
    private LocalTime endTime;    // 방문 종료 시간 (목욕 + 이동시간 포함)
    
    @Column(nullable = false)
    private Integer bathDuration; // 실제 목욕 시간 (분, 기본값: 60~65분)
    
    @Column
    private Integer travelTimeAfter; // 방문 후 이동시간 (분)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status; // PENDING, CONFIRMED, COMPLETED, CANCELLED
    
    @Column(columnDefinition = "TEXT")
    private String notes;          // 특수 요청사항
    
    @Column
    private LocalDateTime confirmedAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
}
```

### 8. CareVisitTime (방문요양 시간)
```java
@Entity
@Table(name = "care_visit_times")
@Getter @Setter
public class CareVisitTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elder_id", nullable = false)
    private Elder elder;
    
    @Column(nullable = false)
    private LocalDate serviceDate;  // 방문요양 날짜
    
    @Column(nullable = false)
    private LocalTime startTime;    // 방문요양 시작 시간
    
    @Column(nullable = false)
    private LocalTime endTime;      // 방문요양 종료 시간
    
    @Column
    private String caregiver;       // 담당 요양보호사
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
}
```

### 9. ScheduleRule (스케줄 규칙 - 추후 OR-Tools용)
```java
@Entity
@Table(name = "schedule_rules")
@Getter @Setter
public class ScheduleRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String ruleCode;      // 규칙 코드 (예: MIN_BATH_DURATION, MAX_TRAVEL_TIME)
    
    @Column(nullable = false)
    private String ruleValue;     // 규칙 값 (예: "60", "30")
    
    @Column(columnDefinition = "TEXT")
    private String description;   // 설명
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isActive;
}
```

---

## ERD 관계도

```
[User]
  └── (관리자가 시스템 관리)

[Elder] 1 ------ * [BathSchedule]
  └── (목욕 일정)

[Elder] 1 ------ * [CareVisitTime]
  └── (방문요양 시간 - 기존 예약된 시간)

[Vehicle] 1 ------ * [Team]
  └── (차량에 배정된 팀)

[Team] 1 ------ * [TeamMember]
  └── (팀 구성원)

[Employee] 1 ------ * [TeamMember]
  └── (직원이 여러 팀에 속할 수 있음)

[Team] 1 ------ * [BathSchedule]
  └── (팀이 목욕 일정을 담당)

### 핵심 관계
- 1개 어르신 → 여러 목욕 일정 (주 1회 또는 복수 일정)
- 1개 어르신 → 여러 방문요양 시간 (기존 예약)
- 1개 팀 → 1개 차량 (고정 매칭)
- 1개 팀 → 여러 직원 (요양보호사, 기사)
- 1개 직원 → 여러 팀 (순환 배정 가능)
- 1개 팀 → 여러 목욕 일정 (하루에 여러 건)
```

---

## DTO 구조

### Request DTOs

#### Elder 관련
```java
@Data
public class CreateElderRequest {
    @NotBlank private String name;
    @NotNull private Gender gender;
    @NotBlank private String phoneNumber;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private String memo;
    private Boolean preferredCaregiverRequired;
}

@Data
public class UpdateElderRequest {
    private String name;
    private String phoneNumber;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private String memo;
    private Boolean preferredCaregiverRequired;
}
```

#### Employee 관련
```java
@Data
public class CreateEmployeeRequest {
    @NotBlank private String name;
    @NotNull private Gender gender;
    @NotBlank private String phoneNumber;
    @NotNull private EmployeeRole role;
    private String caregiverLicense;
    private Boolean driverLicense;
}
```

#### Team & TeamMember 관련
```java
@Data
public class CreateTeamRequest {
    @NotBlank private String name;
    @NotNull private Long vehicleId;
    private String description;
}

@Data
public class AddTeamMemberRequest {
    @NotNull private Long teamId;
    @NotNull private Long employeeId;
    @NotNull private LocalDate startDate;
    private LocalDate endDate;
}
```

#### BathSchedule 관련
```java
@Data
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

@Data
public class UpdateBathScheduleRequest {
    private LocalDate serviceDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer bathDuration;
    private Integer travelTimeAfter;
    private String notes;
    private ScheduleStatus status;
}
```

#### CareVisitTime 관련
```java
@Data
public class CreateCareVisitTimeRequest {
    @NotNull private Long elderId;
    @NotNull private LocalDate serviceDate;
    @NotNull private LocalTime startTime;
    @NotNull private LocalTime endTime;
    private String caregiver;
    private String notes;
}
```

### Response DTOs

```java
@Data
public class ElderResponse {
    private Long id;
    private String name;
    private Gender gender;
    private String phoneNumber;
    private String address;
    private String memo;
    private Boolean isActive;
}

@Data
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

@Data
public class ScheduleValidationResponse {
    private Boolean isValid;
    private List<String> warnings;  // 경고 메시지
    private List<String> errors;    // 오류 메시지
}
```

---

## Repository 구조

### 기본 Repository 인터페이스

```java
public interface ElderRepository extends JpaRepository<Elder, Long> {
    Optional<Elder> findByPhoneNumber(String phoneNumber);
    List<Elder> findByGender(Gender gender);
    List<Elder> findByIsActiveTrue();
}

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByRole(EmployeeRole role);
    List<Employee> findByIsActiveTrue();
}

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
    List<Vehicle> findByIsActiveTrue();
}

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    List<Team> findByVehicleId(Long vehicleId);
    List<Team> findByIsActiveTrue();
}

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);
    List<TeamMember> findByEmployeeId(Long employeeId);
    List<TeamMember> findByTeamIdAndEndDateIsNull(Long teamId); // 현재 활성 팀원
}

public interface BathScheduleRepository extends JpaRepository<BathSchedule, Long> {
    // 날짜별 일정 조회
    List<BathSchedule> findByServiceDateOrderByStartTime(LocalDate serviceDate);
    
    // 어르신별 일정 조회
    List<BathSchedule> findByElderId(Long elderId);
    
    // 팀별 일정 조회
    List<BathSchedule> findByTeamIdAndServiceDate(Long teamId, LocalDate serviceDate);
    
    // 날짜 범위 조회
    @Query("SELECT b FROM BathSchedule b WHERE b.serviceDate BETWEEN :startDate AND :endDate")
    List<BathSchedule> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // 시간 중복 확인
    @Query("SELECT b FROM BathSchedule b WHERE b.elder.id = :elderId AND b.serviceDate = :serviceDate " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<BathSchedule> findConflictingSchedules(@Param("elderId") Long elderId, 
                                                @Param("serviceDate") LocalDate serviceDate,
                                                @Param("startTime") LocalTime startTime,
                                                @Param("endTime") LocalTime endTime);
}

public interface CareVisitTimeRepository extends JpaRepository<CareVisitTime, Long> {
    List<CareVisitTime> findByElderIdAndServiceDate(Long elderId, LocalDate serviceDate);
    
    @Query("SELECT c FROM CareVisitTime c WHERE c.elder.id = :elderId AND c.serviceDate = :serviceDate " +
           "AND ((c.startTime < :endTime AND c.endTime > :startTime))")
    List<CareVisitTime> findConflictingTimes(@Param("elderId") Long elderId,
                                             @Param("serviceDate") LocalDate serviceDate,
                                             @Param("startTime") LocalTime startTime,
                                             @Param("endTime") LocalTime endTime);
}

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, Long> {
    Optional<ScheduleRule> findByRuleCode(String ruleCode);
}
```

---

## Service 구조

### ElderService
```java
@Service
@Transactional
public class ElderService {
    @Autowired private ElderRepository elderRepository;
    
    // CRUD
    public ElderResponse create(CreateElderRequest request);
    public ElderResponse getById(Long id);
    public List<ElderResponse> getAll();
    public ElderResponse update(Long id, UpdateElderRequest request);
    public void delete(Long id);
    
    // 비즈니스 로직
    public List<ElderResponse> getByGender(Gender gender);
    public long getTotalElderCount();
}
```

### EmployeeService
```java
@Service
@Transactional
public class EmployeeService {
    @Autowired private EmployeeRepository employeeRepository;
    
    public EmployeeResponse create(CreateEmployeeRequest request);
    public EmployeeResponse getById(Long id);
    public List<EmployeeResponse> getAll();
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request);
    public void delete(Long id);
    
    public List<EmployeeResponse> getByCaregiverRole();
    public List<EmployeeResponse> getByDriverRole();
}
```

### TeamService
```java
@Service
@Transactional
public class TeamService {
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private VehicleRepository vehicleRepository;
    
    public TeamResponse create(CreateTeamRequest request);
    public TeamResponse getById(Long id);
    public List<TeamResponse> getAll();
    public TeamResponse update(Long id, UpdateTeamRequest request);
    
    // 팀원 관리
    public TeamMemberResponse addMember(AddTeamMemberRequest request);
    public void removeMember(Long teamMemberId);
    public List<TeamMemberResponse> getActiveMembers(Long teamId);
    
    // 팀의 현재 상태 조회
    public TeamDetailResponse getTeamDetail(Long teamId);
}
```

### ScheduleValidationService (핵심 검증 로직)
```java
@Service
public class ScheduleValidationService {
    @Autowired private BathScheduleRepository bathScheduleRepository;
    @Autowired private CareVisitTimeRepository careVisitTimeRepository;
    @Autowired private ElderRepository elderRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    
    /**
     * 목욕 일정 등록 시 종합 검증
     */
    public ScheduleValidationResponse validateSchedule(CreateBathScheduleRequest request) {
        ScheduleValidationResponse response = new ScheduleValidationResponse();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // 1. 기본 데이터 존재 확인
            validateBasicData(request, errors);
            if (!errors.isEmpty()) {
                response.setIsValid(false);
                response.setErrors(errors);
                return response;
            }
            
            Elder elder = elderRepository.findById(request.getElderId()).orElse(null);
            Team team = teamRepository.findById(request.getTeamId()).orElse(null);
            
            // 2. 성별 제약 검증 (여성 어르신 + 남성 팀)
            validateGenderMatch(elder, team, warnings);
            
            // 3. 방문요양 시간 충돌 검증
            validateCareVisitConflict(request, elder, errors);
            
            // 4. 같은 차량/팀 시간 중복 검증
            validateTeamTimeConflict(request, team, errors);
            
            // 5. 목욕시간 유효성 검증 (60~65분)
            validateBathDuration(request, errors);
            
            response.setIsValid(errors.isEmpty());
            response.setWarnings(warnings);
            response.setErrors(errors);
            
        } catch (Exception e) {
            response.setIsValid(false);
            response.setErrors(List.of("검증 중 오류 발생: " + e.getMessage()));
        }
        
        return response;
    }
    
    /**
     * 1. 기본 데이터 존재 여부
     */
    private void validateBasicData(CreateBathScheduleRequest request, List<String> errors) {
        if (!elderRepository.existsById(request.getElderId())) {
            errors.add("어르신 정보가 존재하지 않습니다.");
        }
        if (!teamRepository.existsById(request.getTeamId())) {
            errors.add("팀 정보가 존재하지 않습니다.");
        }
    }
    
    /**
     * 2. 성별 제약 검증: 여성 어르신에게 남성 팀원만 배치되는 경우 경고
     */
    private void validateGenderMatch(Elder elder, Team team, List<String> warnings) {
        if (elder.getGender() == Gender.FEMALE) {
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndEndDateIsNull(team.getId());
            boolean allMale = members.stream()
                .allMatch(m -> m.getEmployee().getGender() == Gender.MALE);
            
            if (allMale) {
                warnings.add("여성 어르신(" + elder.getName() + ")에 남성 팀원만 배치됩니다. 확인하세요.");
            }
        }
    }
    
    /**
     * 3. 방문요양 시간 충돌 검증
     */
    private void validateCareVisitConflict(CreateBathScheduleRequest request, Elder elder, List<String> errors) {
        List<CareVisitTime> conflictingTimes = careVisitTimeRepository.findConflictingTimes(
            elder.getId(),
            request.getServiceDate(),
            request.getStartTime(),
            request.getEndTime()
        );
        
        if (!conflictingTimes.isEmpty()) {
            errors.add("방문요양 시간과 중복됩니다. 방문요양 시간: " + conflictingTimes.get(0).getStartTime() 
                      + " ~ " + conflictingTimes.get(0).getEndTime());
        }
    }
    
    /**
     * 4. 팀 시간 중복 검증: 같은 팀이 같은 시간에 다른 어르신 방문할 수 없음
     */
    private void validateTeamTimeConflict(CreateBathScheduleRequest request, Team team, List<String> errors) {
        List<BathSchedule> conflicts = bathScheduleRepository.findByTeamIdAndServiceDate(
            team.getId(), 
            request.getServiceDate()
        );
        
        for (BathSchedule schedule : conflicts) {
            if (isTimeConflict(request.getStartTime(), request.getEndTime(), 
                              schedule.getStartTime(), schedule.getEndTime())) {
                errors.add("같은 팀의 다른 방문(" + schedule.getElder().getName() 
                          + " " + schedule.getStartTime() + " ~ " + schedule.getEndTime() + ")과 중복됩니다.");
            }
        }
    }
    
    /**
     * 5. 목욕 시간 유효성 검증 (60~65분 기준)
     */
    private void validateBathDuration(CreateBathScheduleRequest request, List<String> errors) {
        if (request.getBathDuration() < 60 || request.getBathDuration() > 65) {
            errors.add("목욕 시간은 60~65분이어야 합니다. (입력값: " + request.getBathDuration() + "분)");
        }
    }
    
    /**
     * 시간 충돌 판정 유틸
     */
    private boolean isTimeConflict(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
```

### ScheduleService (일정 관리)
```java
@Service
@Transactional
public class ScheduleService {
    @Autowired private BathScheduleRepository bathScheduleRepository;
    @Autowired private ScheduleValidationService validationService;
    @Autowired private CareVisitTimeRepository careVisitTimeRepository;
    
    /**
     * 목욕 일정 생성
     */
    public BathScheduleResponse create(CreateBathScheduleRequest request) {
        // 1. 검증
        ScheduleValidationResponse validation = validationService.validateSchedule(request);
        if (!validation.getIsValid()) {
            throw new ScheduleConflictException("일정 검증 실패: " + String.join(", ", validation.getErrors()));
        }
        
        // 2. 경고 로깅
        if (!validation.getWarnings().isEmpty()) {
            log.warn("일정 생성 경고: " + String.join(", ", validation.getWarnings()));
        }
        
        // 3. 저장
        BathSchedule schedule = new BathSchedule();
        schedule.setElder(elderRepository.findById(request.getElderId()).orElse(null));
        schedule.setTeam(teamRepository.findById(request.getTeamId()).orElse(null));
        schedule.setServiceDate(request.getServiceDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setBathDuration(request.getBathDuration());
        schedule.setTravelTimeAfter(request.getTravelTimeAfter());
        schedule.setStatus(ScheduleStatus.PENDING);
        schedule.setCreatedAt(LocalDateTime.now());
        
        BathSchedule saved = bathScheduleRepository.save(schedule);
        return toResponse(saved);
    }
    
    /**
     * 날짜별 일정 조회
     */
    public List<BathScheduleResponse> getSchedulesByDate(LocalDate date) {
        return bathScheduleRepository.findByServiceDateOrderByStartTime(date)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 어르신별 일정 조회
     */
    public List<BathScheduleResponse> getSchedulesByElder(Long elderId) {
        return bathScheduleRepository.findByElderId(elderId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 주간 일정 조회 (대시보드용)
     */
    public List<BathScheduleResponse> getWeeklySchedules(LocalDate weekStartDate) {
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        return bathScheduleRepository.findByDateRange(weekStartDate, weekEndDate)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    private BathScheduleResponse toResponse(BathSchedule schedule) {
        // DTO 변환 로직
        return new BathScheduleResponse(/*...*/);
    }
}
```

---

## Controller API 명세

### 1. 인증 API

```
POST /api/auth/login
- Request: { "username": "admin", "password": "****" }
- Response: { "token": "jwt_token", "username": "admin" }

POST /api/auth/logout
- Response: { "success": true }
```

### 2. 어르신 관리 API

```
GET  /api/elders                    # 전체 어르신 조회
GET  /api/elders/{id}               # 어르신 상세 조회
POST /api/elders                    # 어르신 생성
PUT  /api/elders/{id}               # 어르신 수정
DELETE /api/elders/{id}             # 어르신 삭제

GET /api/elders/gender/{gender}     # 성별별 조회 (MALE, FEMALE)
```

### 3. 직원 관리 API

```
GET  /api/employees                 # 전체 직원 조회
GET  /api/employees/{id}            # 직원 상세 조회
POST /api/employees                 # 직원 생성
PUT  /api/employees/{id}            # 직원 수정
DELETE /api/employees/{id}          # 직원 삭제

GET /api/employees/role/caregiver   # 요양보호사만 조회
GET /api/employees/role/driver      # 기사만 조회
```

### 4. 차량 관리 API

```
GET  /api/vehicles                  # 전체 차량 조회
GET  /api/vehicles/{id}             # 차량 상세 조회
POST /api/vehicles                  # 차량 등록
PUT  /api/vehicles/{id}             # 차량 정보 수정
DELETE /api/vehicles/{id}           # 차량 삭제

PATCH /api/vehicles/{id}/location   # 차량 위치 업데이트 (GPS)
```

### 5. 팀 관리 API

```
GET  /api/teams                     # 전체 팀 조회
GET  /api/teams/{id}                # 팀 상세 조회 (팀원 포함)
POST /api/teams                     # 팀 생성
PUT  /api/teams/{id}                # 팀 정보 수정
DELETE /api/teams/{id}              # 팀 삭제

# 팀원 관리
GET  /api/teams/{id}/members        # 팀원 조회
POST /api/teams/{teamId}/members    # 팀원 추가
DELETE /api/teams/{teamId}/members/{memberId}  # 팀원 제거
```

### 6. 목욕 일정 API (핵심)

```
GET  /api/schedules                 # 전체 일정 조회 (필터링 가능)
GET  /api/schedules/{id}            # 일정 상세 조회
POST /api/schedules                 # 일정 생성 (검증 포함)
PUT  /api/schedules/{id}            # 일정 수정
DELETE /api/schedules/{id}          # 일정 삭제

# 검증 API (미리 확인용)
POST /api/schedules/validate        # 일정 검증 (생성 전 확인)
- Request: CreateBathScheduleRequest
- Response: ScheduleValidationResponse { isValid, warnings, errors }

# 날짜별 조회
GET  /api/schedules/date/{date}     # 특정 날짜 일정
GET  /api/schedules/week/{startDate}  # 주간 일정

# 어르신별 조회
GET  /api/schedules/elder/{elderId} # 어르신의 모든 일정

# 상태 변경
PATCH /api/schedules/{id}/confirm   # 일정 확정
PATCH /api/schedules/{id}/complete  # 일정 완료
PATCH /api/schedules/{id}/cancel    # 일정 취소
```

### 7. 방문요양 시간 API

```
GET  /api/care-visits               # 전체 방문요양 조회
GET  /api/care-visits/elder/{elderId}  # 어르신의 방문요양 시간
POST /api/care-visits               # 방문요양 시간 등록
PUT  /api/care-visits/{id}          # 방문요양 시간 수정
DELETE /api/care-visits/{id}        # 방문요양 시간 삭제
```

### 8. 대시보드 API

```
GET  /api/dashboard/summary         # 요약 정보
- Response: {
    totalElders: 50,
    totalEmployees: 15,
    totalVehicles: 2,
    todaySchedules: 8,
    pendingSchedules: 2
  }

GET  /api/dashboard/today           # 오늘 일정
GET  /api/dashboard/week            # 이번 주 일정
GET  /api/dashboard/stats           # 통계 (월별, 직원별 등)
```

---

## 일정 배정 검증 로직

### 검증 플로우 다이어그램

```
[목욕 일정 생성 요청]
        ↓
[1. 기본 데이터 검증] (Elder, Team, Vehicle 존재?)
        ↓
        NO → ERROR: "데이터 없음"
        YES ↓
[2. 성별 제약 검증] (여성 어르신 + 남성 팀?)
        ↓
        YES → WARNING: "여성 어르신에 남성팀만 배치됨"
        ↓
[3. 방문요양 시간 충돌] (목욕 시간과 겹침?)
        ↓
        YES → ERROR: "방문요양과 중복"
        NO ↓
[4. 팀 시간 중복 검증] (같은 팀이 다른 어르신을 이미 배정?)
        ↓
        YES → ERROR: "팀 시간 중복"
        NO ↓
[5. 목욕 시간 유효성] (60~65분?)
        ↓
        NO → ERROR: "목욕 시간 부적절"
        YES ↓
[✓ 검증 통과]
        ↓
[일정 저장 & 상태 = PENDING]
```

### 검증 규칙 상세

#### Rule 1: 성별 제약
```
IF 어르신.gender == FEMALE
AND ALL 팀원.gender == MALE
THEN WARNING("여성 어르신에 남성팀원만 배치됨. 확인하세요.")
```

#### Rule 2: 방문요양 시간 충돌
```
IF CareVisitTime 존재
   WHERE elder_id == 요청 elder_id
   AND service_date == 요청 date
   AND startTime < 요청 endTime
   AND endTime > 요청 startTime
THEN ERROR("방문요양 시간과 중복: [existingStart~existingEnd]")
```

#### Rule 3: 팀 시간 중복
```
FOR EACH BathSchedule 존재
    WHERE team_id == 요청 team_id
    AND service_date == 요청 date
    
IF startTime < 요청 endTime
   AND endTime > 요청 startTime
THEN ERROR("팀 시간 중복: [elderName existingStart~existingEnd]")
```

#### Rule 4: 목욕 시간 검증
```
IF bathDuration < 60 OR bathDuration > 65
THEN ERROR("목욕 시간은 60~65분이어야 함 (입력: Xmin)")
```

#### Rule 5: 최소 이동시간
```
IF travelTimeAfter < 10
THEN ERROR("이동시간은 최소 10분 필요")
```

---

## 추후 확장 구조

### 1. Kakao Map API 통합
```java
// 추후 추가 모듈: kakao-map-service
@Service
public class LocationService {
    // 주소 → 좌표 변환 (지오코딩)
    public Location geocode(String address);
    
    // 좌표 → 주소 변환 (역지오코딩)
    public String reverseGeocode(Double lat, Double lng);
    
    // 2개 지점 간 거리 계산
    public Integer calculateDistance(Location from, Location to);
    
    // 2개 지점 간 이동시간 계산
    public Integer calculateTravelTime(Location from, Location to);
}
```

### 2. OR-Tools (Vehicle Routing Problem with Time Windows) 통합
```java
// 추후 추가 모듈: or-tools-optimizer
@Service
public class RouteOptimizationService {
    
    /**
     * 일일 일정 최적화
     * Input: 일자, 팀 목록, 미배정 어르신 목록
     * Output: 최적화된 배정 + 경로 + 예상 시간
     */
    public OptimizationResult optimizeDaily(LocalDate date, List<Team> teams, List<Elder> elders);
    
    /**
     * 주간 일정 최적화
     */
    public OptimizationResult optimizeWeekly(LocalDate weekStart, List<Team> teams, List<Elder> elders);
}

@Data
public class OptimizationResult {
    private Boolean isFeasible;           // 모든 어르신 배정 가능한지
    private List<RouteAssignment> routes; // 팀별 배정 경로
    private Integer totalDistance;        // 총 이동거리
    private Integer totalTime;            // 총 소요시간
    private List<String> unassignedElders;  // 배정 불가능한 어르신
    private List<String> notes;           // 특수 사항
}

@Data
public class RouteAssignment {
    private Long teamId;
    private List<Long> eldersInOrder;     // 방문 순서
    private List<String> routes;          // 경로
    private List<LocalTime> arrivalTimes; // 도착 시간
}
```

### 3. 확장을 위한 Architecture 설계

```
기존 모놀리식 구조 (MVP)
    ↓ (확장 시)
마이크로서비스 아키텍처

┌─────────────────────────────────────────┐
│  API Gateway                             │
├─────────────────────────────────────────┤
│  Schedule Service (현재)    │  Location Service │
│  - 일정 관리               │  - 주소/좌표     │
│  - 검증                    │  - 거리/시간 계산│
├──────────────────────────────────────────┤
│  Optimization Service (추후)             │
│  - OR-Tools 연동                          │
│  - VRPTW 최적화                           │
└──────────────────────────────────────────┘
```

### 4. ScheduleRule 테이블 활용

```sql
-- MVP 기본 규칙 (수정 가능하도록 테이블화)
INSERT INTO schedule_rules VALUES
(1, 'MIN_BATH_DURATION', '60', '최소 목욕 시간 (분)', NOW(), NOW(), true),
(2, 'MAX_BATH_DURATION', '65', '최대 목욕 시간 (분)', NOW(), NOW(), true),
(3, 'MIN_TRAVEL_TIME', '10', '최소 이동시간 (분)', NOW(), NOW(), true),
(4, 'MAX_TRAVEL_TIME', '30', '최대 이동시간 (분)', NOW(), NOW(), true),
(5, 'START_TIME', '08:00', '업무 시작 시간', NOW(), NOW(), true),
(6, 'END_TIME', '18:00', '업무 종료 시간', NOW(), NOW(), true);
```

---

## 개발 순서 및 우선순위

### Phase 1: 기초 인프라 (1주)
1. **프로젝트 초기화**
   - Spring Boot 프로젝트 생성
   - pom.xml 의존성 설정 (JPA, MySQL, JWT, Lombok 등)
   - application.yml 설정 (DB, 로깅)
   - Spring Security 기본 설정

2. **데이터베이스 설계**
   - Entity 클래스 작성 (10개)
   - JPA Mapping 설정
   - DB 마이그레이션 스크립트 생성
   - 인덱스 추가 (serviceDate, elderId, teamId 등)

### Phase 2: 기본 CRUD & API (1.5주)
3. **User & Auth**
   - User Entity & Repository
   - JWT Provider 구현
   - AuthController (login, logout)
   - SecurityConfig

4. **Elder 관리**
   - ElderRepository, ElderService
   - ElderController (GET/POST/PUT/DELETE)
   - 단위 테스트

5. **Employee & Vehicle**
   - EmployeeRepository, EmployeeService
   - VehicleRepository, VehicleService
   - 각 Controller
   - 단위 테스트

### Phase 3: 팀 & 일정 기초 (1.5주)
6. **Team & TeamMember**
   - TeamRepository, TeamService
   - TeamMemberRepository 및 팀원 추가/제거 로직
   - TeamController
   - 팀 상세 조회 API (팀원 포함)

7. **CareVisitTime (기존 방문요양 시간)**
   - CareVisitTimeRepository, CareVisitService
   - CareVisitTimeController
   - 어르신별 방문요양 시간 조회

### Phase 4: 일정 검증 & 배정 (2주) ⭐ 핵심
8. **ScheduleValidationService**
   - 성별 제약 검증
   - 방문요양 시간 충돌 검증
   - 팀 시간 중복 검증
   - 목욕시간 유효성 검증
   - 통합 검증 로직
   - 단위 테스트 (각 검증별 10+ 케이스)

9. **ScheduleService & BathSchedule CRUD**
   - BathScheduleRepository (복잡한 쿼리 포함)
   - ScheduleService (검증 로직 호출)
   - BathScheduleController
   - 날짜별, 어르신별, 팀별 조회 API
   - 통합 테스트 (실제 시나리오)

### Phase 5: 고급 기능 (1주)
10. **상태 관리**
    - PENDING → CONFIRMED → COMPLETED / CANCELLED
    - 상태 변경 API (confirm, complete, cancel)
    - 상태별 권한 체크

11. **대시보드 API**
    - 요약 정보 (총 어르신, 직원, 일정 수)
    - 오늘 일정
    - 주간 일정
    - 통계 (월별 방문 횟수, 직원별 방문 건수 등)

### Phase 6: 문서 & 배포 (0.5주)
12. **API 문서화**
    - Swagger/Springdoc 설정
    - API 명세서 자동 생성

13. **Docker & 배포**
    - Dockerfile 작성
    - docker-compose 설정 (MySQL + Spring Boot)
    - 배포 스크립트

### Phase 7: 추후 확장 (이후)
14. **Kakao Map API 통합**
15. **OR-Tools 통합**
16. **성능 최적화** (캐싱, 배치 처리 등)
17. **React 프론트엔드 개발**

---

## 개발 중 주의사항

### 1. 트랜잭션 관리
```java
// ScheduleService에서는 반드시 @Transactional 사용
@Service
@Transactional
public class ScheduleService {
    // 검증 → 저장이 하나의 트랜잭션으로 처리되어야 함
}
```

### 2. 성능 고려
```java
// N+1 문제 방지: eager loading 사용
@Query("SELECT s FROM BathSchedule s " +
       "JOIN FETCH s.elder " +
       "JOIN FETCH s.team " +
       "WHERE s.serviceDate = :date")
List<BathSchedule> findByServiceDateWithDetails(@Param("date") LocalDate date);
```

### 3. 시간대 관리
```java
// MySQL은 타임존 이슈가 있으므로 UTC 기준으로 저장
// application.yml에 설정
spring:
  jpa:
    hibernate:
      jdbc:
        time_zone: UTC
```

### 4. 입력 검증
```java
// DTO에서 @Valid, @NotNull 등으로 입력 검증
public BathScheduleResponse create(@Valid @RequestBody CreateBathScheduleRequest request) {
    // ...
}
```

---

## 다음 단계

1. **프로젝트 초기화 스크립트** 생성 (Spring Boot 프로젝트 + 의존성)
2. **Entity 클래스** 코드 생성
3. **Repository & Service** 구현 시작
4. **Controller & API** 구현
5. **통합 테스트** 작성
6. **React 프론트엔드** 설계 및 개발

이 설계는 MVP 기준으로 **확장 가능하고, 명확하며, 구현 가능한 구조**를 제시합니다.
혹시 구체적인 코드 생성이 필요하면 알려주세요!
