# 방문목욕센터 운영 시스템 - 개발 가이드

## 📌 빠른 시작 (Quick Start)

### 1단계: 프로젝트 초기화

```bash
# 1. Git 저장소 초기화
cd /Users/user/Desktop/home_care
git init

# 2. IDE에서 pom.xml 열기 (Maven 동기화)
# IntelliJ: File → Open → pom.xml
# 또는 VSCode에서 Java Pack 설치 후 자동 동기화

# 3. MySQL 데이터베이스 생성
mysql -u root -p
CREATE DATABASE home_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE home_care;
```

### 2단계: IDE 설정

**IntelliJ IDEA 기준:**
1. File → Project Structure → Project
   - SDK: Java 17 이상
   - Language level: 17

2. File → Settings → Build, Execution, Deployment → Build Tools → Maven
   - Maven home path 설정

3. Plugins 설치:
   - Lombok
   - Spring Boot
   - Database Navigator

### 3단계: 메인 Application 클래스 생성

`src/main/java/com/homecare/HomeCareApplication.java`를 생성하고 다음 코드를 입력:

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
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
            .setSkipNullEnabled(true)
            .setAmbiguityIgnored(true);
        return modelMapper;
    }
}
```

### 4단계: 첫 실행

```bash
mvn clean install
mvn spring-boot:run
```

성공 메시지: `Tomcat started on port(s): 8080`

---

## 개발 우선순위 & 로드맵

### Phase 1: 기초 설정 (✅ 완료)
- ✅ pom.xml 의존성 정의
- ✅ application.yml 설정
- ✅ Entity 클래스 작성
- ✅ Enum 정의

### Phase 2: 인프라 (1-2시간)
```bash
# 해야 할 작업:
1. GlobalExceptionHandler 작성
   → src/main/java/com/homecare/exception/

2. Security & JWT 설정
   → src/main/java/com/homecare/config/SecurityConfig.java
   → src/main/java/com/homecare/security/JwtProvider.java

3. 기본 Response DTO
   → src/main/java/com/homecare/common/ApiResponse.java
```

### Phase 3: Repository & Service (2시간)
```bash
# 우선순위:
1. ElderRepository → ElderService → ElderController
2. EmployeeRepository → EmployeeService → EmployeeController
3. VehicleRepository → VehicleService → VehicleController
4. TeamRepository → TeamService → TeamController
```

### Phase 4: 핵심 로직 (3시간) ⭐
```bash
# 가장 중요한 부분:
1. ScheduleValidationService (검증 로직)
2. ScheduleService (일정 관리)
3. ScheduleController (API)
4. 통합 테스트
```

### Phase 5: 대시보드 & 마무리 (1시간)
```bash
1. DashboardController
2. Swagger 설정
3. 로그 설정
```

---

## 📁 디렉토리별 파일 작성 순서

### 1. Exception & Common 패키지
```
src/main/java/com/homecare/
├── exception/
│   ├── GlobalExceptionHandler.java      (1순위)
│   ├── ScheduleConflictException.java   (검증 예외)
│   ├── GenderMismatchException.java
│   └── ValidationException.java
│
└── common/
    ├── ApiResponse.java                (1순위)
    ├── ApiError.java
    └── PagedResponse.java
```

### 2. Config & Security 패키지
```
├── config/
│   ├── SecurityConfig.java             (2순위)
│   ├── WebConfig.java
│   └── SwaggerConfig.java              (선택)
│
└── security/
    ├── JwtProvider.java                (2순위)
    ├── CustomUserDetailsService.java
    └── JwtAuthenticationFilter.java
```

### 3. Repository 패키지
```
└── repository/
    ├── ElderRepository.java            (3순위)
    ├── EmployeeRepository.java
    ├── VehicleRepository.java
    ├── TeamRepository.java
    ├── TeamMemberRepository.java
    ├── BathScheduleRepository.java     (중요)
    ├── CareVisitTimeRepository.java    (중요)
    ├── ScheduleRuleRepository.java
    └── UserRepository.java
```

### 4. DTO 패키지
```
└── dto/
    ├── request/
    │   ├── CreateElderRequest.java
    │   ├── CreateEmployeeRequest.java
    │   ├── CreateTeamRequest.java
    │   ├── AddTeamMemberRequest.java
    │   ├── CreateBathScheduleRequest.java  (중요)
    │   └── CreateCareVisitTimeRequest.java
    │
    └── response/
        ├── ElderResponse.java
        ├── EmployeeResponse.java
        ├── TeamResponse.java
        ├── BathScheduleResponse.java       (중요)
        ├── ScheduleValidationResponse.java (중요)
        └── DashboardResponse.java
```

### 5. Service 패키지
```
└── service/
    ├── ElderService.java               (3순위)
    ├── EmployeeService.java
    ├── VehicleService.java
    ├── TeamService.java
    ├── CareVisitService.java
    ├── ScheduleValidationService.java  (4순위 - 핵심)
    ├── ScheduleService.java            (4순위 - 핵심)
    └── AuthService.java
```

### 6. Controller 패키지
```
└── controller/
    ├── AuthController.java             (3순위)
    ├── ElderController.java
    ├── EmployeeController.java
    ├── VehicleController.java
    ├── TeamController.java
    ├── ScheduleController.java         (4순위 - 핵심)
    ├── CareVisitController.java
    └── DashboardController.java
```

### 7. Utility 패키지
```
└── util/
    ├── TimeCalculator.java             (4순위)
    ├── ScheduleValidator.java
    └── DateTimeUtil.java
```

---

## 🔑 핵심 개발 체크리스트

### MVP 필수 기능
- [ ] 로그인/로그아웃 (JWT)
- [ ] 어르신 CRUD
- [ ] 직원 CRUD
- [ ] 차량 CRUD
- [ ] 팀 CRUD
- [ ] 팀원 추가/제거
- [ ] 목욕 일정 CRUD
- [ ] **일정 검증 (성별, 시간 충돌, 방문요양 충돌)**
- [ ] 일정 상태 관리 (PENDING → CONFIRMED → COMPLETED)
- [ ] 방문요양 시간 관리
- [ ] 대시보드

### 선택 기능 (추후)
- [ ] Kakao Map API 통합
- [ ] OR-Tools 최적화
- [ ] 엑셀 다운로드
- [ ] 문자 알림
- [ ] 모바일 앱

---

## 📝 작성 예시: ElderService 구현

```java
package com.homecare.service;

import com.homecare.domain.entity.Elder;
import com.homecare.domain.enums.Gender;
import com.homecare.dto.request.CreateElderRequest;
import com.homecare.dto.request.UpdateElderRequest;
import com.homecare.dto.response.ElderResponse;
import com.homecare.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ElderService {
    private final ElderRepository elderRepository;
    private final ModelMapper modelMapper;

    /**
     * 어르신 생성
     */
    public ElderResponse create(CreateElderRequest request) {
        Elder elder = Elder.builder()
            .name(request.getName())
            .gender(request.getGender())
            .phoneNumber(request.getPhoneNumber())
            .address(request.getAddress())
            .addressDetail(request.getAddressDetail())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .memo(request.getMemo())
            .preferredCaregiverRequired(request.getPreferredCaregiverRequired())
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build();

        Elder saved = elderRepository.save(elder);
        log.info("어르신 생성: {}", saved.getId());
        return modelMapper.map(saved, ElderResponse.class);
    }

    /**
     * 어르신 조회
     */
    @Transactional(readOnly = true)
    public ElderResponse getById(Long id) {
        Elder elder = elderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("어르신 정보가 없습니다."));
        return modelMapper.map(elder, ElderResponse.class);
    }

    /**
     * 전체 어르신 조회
     */
    @Transactional(readOnly = true)
    public List<ElderResponse> getAll() {
        return elderRepository.findByIsActiveTrue()
            .stream()
            .map(elder -> modelMapper.map(elder, ElderResponse.class))
            .collect(Collectors.toList());
    }

    /**
     * 어르신 정보 수정
     */
    public ElderResponse update(Long id, UpdateElderRequest request) {
        Elder elder = elderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("어르신 정보가 없습니다."));

        if (request.getName() != null) elder.setName(request.getName());
        if (request.getPhoneNumber() != null) elder.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) elder.setAddress(request.getAddress());
        if (request.getMemo() != null) elder.setMemo(request.getMemo());

        Elder updated = elderRepository.save(elder);
        log.info("어르신 수정: {}", updated.getId());
        return modelMapper.map(updated, ElderResponse.class);
    }

    /**
     * 어르신 삭제 (soft delete)
     */
    public void delete(Long id) {
        Elder elder = elderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("어르신 정보가 없습니다."));
        elder.setIsActive(false);
        elderRepository.save(elder);
        log.info("어르신 삭제: {}", id);
    }

    /**
     * 성별별 어르신 조회
     */
    @Transactional(readOnly = true)
    public List<ElderResponse> getByGender(Gender gender) {
        return elderRepository.findByGender(gender)
            .stream()
            .filter(Elder::getIsActive)
            .map(elder -> modelMapper.map(elder, ElderResponse.class))
            .collect(Collectors.toList());
    }

    /**
     * 전체 어르신 수
     */
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return elderRepository.findByIsActiveTrue().size();
    }
}
```

---

## 테스트 데이터 삽입 (SQL)

```sql
-- 사용자 (관리자)
INSERT INTO users (username, password, name, role, created_at, is_active) VALUES
('admin', '$2a$10$...', '센터장', 'ADMIN', NOW(), true);

-- 어르신
INSERT INTO elders (name, gender, phone_number, address, created_at, is_active) VALUES
('김영희', 'FEMALE', '010-1111-1111', '서울시 강남구', NOW(), true),
('박동수', 'MALE', '010-2222-2222', '서울시 강남구', NOW(), true),
('이순신', 'MALE', '010-3333-3333', '서울시 강남구', NOW(), true);

-- 직원
INSERT INTO employees (name, gender, phone_number, role, created_at, is_active) VALUES
('이소영', 'FEMALE', '010-4444-4444', 'CAREGIVER', NOW(), true),
('박준호', 'MALE', '010-5555-5555', 'CAREGIVER', NOW(), true),
('최기사', 'MALE', '010-6666-6666', 'DRIVER', NOW(), true);

-- 차량
INSERT INTO vehicles (vehicle_number, model, created_at, is_active) VALUES
('12가1234', '그랜저', NOW(), true),
('34나5678', '아반떼', NOW(), true);

-- 팀
INSERT INTO teams (name, vehicle_id, created_at, is_active) VALUES
('오전팀', 1, NOW(), true),
('오후팀', 2, NOW(), true);

-- 팀원
INSERT INTO team_members (team_id, employee_id, start_date, assignment_order, created_at, is_active) VALUES
(1, 1, '2024-01-01', 1, NOW(), true),
(1, 3, '2024-01-01', 2, NOW(), true),
(2, 2, '2024-01-01', 1, NOW(), true);

-- 스케줄 규칙
INSERT INTO schedule_rules (rule_code, rule_value, description, created_at, is_active) VALUES
('MIN_BATH_DURATION', '60', '최소 목욕 시간 (분)', NOW(), true),
('MAX_BATH_DURATION', '65', '최대 목욕 시간 (분)', NOW(), true),
('MIN_TRAVEL_TIME', '10', '최소 이동시간 (분)', NOW(), true),
('MAX_TRAVEL_TIME', '30', '최대 이동시간 (분)', NOW(), true);
```

---

## 🚀 배포 & 실행

### 개발 환경
```bash
# 터미널에서
mvn spring-boot:run

# Swagger UI 접속
http://localhost:8080/api/swagger-ui.html
```

### 프로덕션 환경 (Docker)
```bash
# Dockerfile 생성
# 이후 내용 제공 예정

docker build -t home-care:1.0 .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod home-care:1.0
```

---

## 다음 진행 사항

1. **GlobalExceptionHandler** 작성
2. **SecurityConfig & JWT** 구현
3. **ElderRepository & Service** 완성
4. **ScheduleValidationService** (핵심) 구현
5. 통합 테스트 & 데이터 검증

---

## 참고 자료

- [Spring Boot 3.2 공식 문서](https://spring.io/projects/spring-boot)
- [JPA/Hibernate 가이드](https://spring.io/guides/gs/accessing-data-jpa/)
- [JWT 인증 예제](https://jjwt.io)
- [ModelMapper 사용법](http://modelmapper.org/)
