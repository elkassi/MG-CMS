# MG-CMS Performance & Code Quality Recommendations

This document provides comprehensive recommendations to improve performance, maintainability, security, and user experience for the MG-CMS project.

> **Last Updated:** December 2025  
> **Review Scope:** Full code review of backend, frontend, database, and architecture

---

## Executive Summary

### Critical Issues Found
1. ⚠️ **Security**: Credentials exposed in `application.properties` - use environment variables
2. ⚠️ **Spring Boot Version**: 2.5.3 is outdated (EOL) - upgrade to 2.7.x or 3.x
3. ⚠️ **React Version**: 17.0.1 should be upgraded to React 18
4. ⚠️ **Class Components**: Most components use class-based patterns - consider migration to hooks

### Project Strengths
- ✅ Well-organized multi-module architecture
- ✅ Comprehensive role-based access control
- ✅ Generic EntityList/EntityForm components reduce code duplication
- ✅ Multi-database configuration properly implemented
- ✅ WebSocket integration for real-time features
- ✅ Existing scheduling optimization integration

---

## Table of Contents
1. [Backend Recommendations](#backend-recommendations)
2. [Frontend Recommendations](#frontend-recommendations)
3. [Database Recommendations](#database-recommendations)
4. [Security Recommendations](#security-recommendations)
5. [UI/UX Improvements](#uiux-improvements)
6. [Missing Functionality](#missing-functionality)
7. [Architecture Improvements](#architecture-improvements)
8. [Priority Action Items](#priority-action-items)

---

## Backend Recommendations

### 1. Upgrade Spring Boot Version

**Current:** 2.5.3 (EOL)  
**Recommended:** 2.7.18 (LTS) or 3.2.x (latest)

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>
```

**Benefits:**
- Security patches and bug fixes
- Better performance
- Updated dependencies
- Java 17+ support

---

### 2. Upgrade Java Version

**Current:** Java 16  
**Recommended:** Java 17 (LTS) or Java 21 (LTS)

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

---

### 3. Fix Deprecated Security Configuration

The current `WebSecurityConfigurerAdapter` is deprecated in Spring Security 5.7+.

**Current Code:**
```java
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // ...
    }
}
```

**Recommended:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors().and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

---

### 4. Improve Service Layer Architecture

**Issue:** Large service classes with multiple responsibilities.

**Recommendation:** Extract common patterns and use composition.

```java
// Abstract base service for common CRUD operations
public abstract class BaseService<T, ID> {
    
    protected abstract JpaRepository<T, ID> getRepository();
    
    public Page<T> findAll(Pageable pageable) {
        return getRepository().findAll(pageable);
    }
    
    public Optional<T> findById(ID id) {
        return getRepository().findById(id);
    }
    
    @Transactional
    public T save(T entity) {
        return getRepository().save(entity);
    }
    
    @Transactional
    public void deleteById(ID id) {
        getRepository().deleteById(id);
    }
}

// Entity-specific service
@Service
public class CuttingPlanService extends BaseService<CuttingPlan, Long> {
    
    @Autowired
    private CuttingPlanRepository repository;
    
    @Override
    protected JpaRepository<CuttingPlan, Long> getRepository() {
        return repository;
    }
    
    // Add specific business logic here
}
```

---

### 5. Add Global Exception Handling

**Missing:** Centralized exception handling.

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", "Access denied"));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", errors));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

---

### 6. Optimize ScheduledTask.java

**Issue:** The `ScheduledTask.java` file is 1029+ lines with too many responsibilities.

**Recommendation:** Split into multiple focused task classes.

```java
// Split into:
// - PlacementScheduledTask.java - Placement-related tasks
// - ReportScheduledTask.java - Report generation tasks
// - DataSyncScheduledTask.java - Data synchronization tasks
// - CleanupScheduledTask.java - Cleanup and archiving tasks

@Component
@Slf4j
public class PlacementScheduledTask {
    
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncPlacements() {
        log.info("Starting placement sync...");
        // Focused logic
    }
}
```

---

### 7. Implement DTO Pattern

**Issue:** Entity objects exposed directly in API responses.

**Recommendation:** Use DTOs for API layer.

```java
// DTO class
public record CuttingPlanDTO(
    Long id,
    String description,
    String projet,
    String version,
    LocalDateTime createdAt,
    String createdByName,
    Boolean enabled
) {}

// Mapper
@Mapper(componentModel = "spring")
public interface CuttingPlanMapper {
    CuttingPlanDTO toDTO(CuttingPlan entity);
    List<CuttingPlanDTO> toDTOList(List<CuttingPlan> entities);
}
```

---

### 8. Add Caching for Static Data

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("machines", "projects", "zones", "roles");
    }
}

@Service
public class MachineService {
    
    @Cacheable("machines")
    public List<Machine> findAll() {
        return machineRepository.findAll();
    }
    
    @CacheEvict(value = "machines", allEntries = true)
    public Machine save(Machine machine) {
        return machineRepository.save(machine);
    }
}
```

---

## Frontend Recommendations

### 1. Component Optimization

#### Use React.memo and PureComponent
```javascript
// Before
class MyComponent extends Component { ... }

// After - for class components
class MyComponent extends PureComponent { ... }

// Or for functional components
const MyComponent = React.memo(({ prop1, prop2 }) => {
  return <div>{prop1} {prop2}</div>;
});
```

#### Implement useMemo and useCallback (for functional components)
```javascript
// Memoize expensive calculations
const expensiveResult = useMemo(() => {
  return computeExpensiveValue(data);
}, [data]);

// Memoize callback functions
const handleClick = useCallback(() => {
  doSomething(id);
}, [id]);
```

### 2. Lazy Loading Components
```javascript
// Before - all components loaded at once
import HeavyComponent from './HeavyComponent';

// After - lazy load components
const HeavyComponent = React.lazy(() => import('./HeavyComponent'));

// Usage with Suspense
<Suspense fallback={<div>Loading...</div>}>
  <HeavyComponent />
</Suspense>
```

### 3. Virtualize Large Lists
For tables with many rows (EntityList), implement virtualization:
```javascript
import { FixedSizeList } from 'react-window';

// Virtualized list only renders visible items
<FixedSizeList
  height={500}
  itemCount={items.length}
  itemSize={35}
  width="100%"
>
  {Row}
</FixedSizeList>
```

### 4. Debounce Search Inputs
```javascript
import { debounce } from 'lodash';

// Debounce search to avoid excessive API calls
const debouncedSearch = debounce((searchTerm) => {
  this.searchData(searchTerm);
}, 300);

<input onChange={(e) => debouncedSearch(e.target.value)} />
```

### 5. Optimize Re-renders

#### Avoid Creating Objects in Render
```javascript
// Bad - creates new object every render
<Component style={{ color: 'red' }} />

// Good - define outside or memoize
const style = { color: 'red' };
<Component style={style} />
```

#### Use Keys Properly
```javascript
// Bad - using index as key
{items.map((item, index) => <Item key={index} {...item} />)}

// Good - use unique identifier
{items.map((item) => <Item key={item.id} {...item} />)}
```

### 6. Bundle Size Optimization

#### Import Only What You Need
```javascript
// Bad - imports entire library
import _ from 'lodash';

// Good - import specific function
import debounce from 'lodash/debounce';
```

#### Use Dynamic Imports for Heavy Libraries
```javascript
// Load chart library only when needed
const loadCharts = async () => {
  const { BarChart } = await import('recharts');
  return BarChart;
};
```

### 7. Axios Request Optimization

#### Cancel Pending Requests
```javascript
componentWillUnmount() {
  this.cancelTokenSource.cancel('Component unmounted');
}

// Use with axios
axios.get('/api/data', {
  cancelToken: this.cancelTokenSource.token
});
```

#### Implement Request Caching
```javascript
const cache = new Map();
const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

async function fetchWithCache(url) {
  const cached = cache.get(url);
  if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
    return cached.data;
  }
  
  const response = await axios.get(url);
  cache.set(url, { data: response.data, timestamp: Date.now() });
  return response.data;
}
```

---

## Backend Optimizations

### 1. Database Query Optimization

#### Use Projections for Large Entities
```java
// Before - fetches all columns
@Query("SELECT e FROM Entity e")
List<Entity> findAll();

// After - fetch only needed columns
@Query("SELECT new com.lear.dto.EntityDTO(e.id, e.name) FROM Entity e")
List<EntityDTO> findAllLight();
```

#### Use Pagination Everywhere
```java
// Always paginate large datasets
@GetMapping("/all")
public Page<Entity> findAll(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    Pageable pageable) {
    return repository.findAll(pageable);
}
```

### 2. N+1 Query Problem

#### Use JOIN FETCH
```java
// Bad - causes N+1 queries
@Query("SELECT e FROM Entity e")
List<Entity> findAll();

// Good - eager load relationships
@Query("SELECT e FROM Entity e LEFT JOIN FETCH e.relatedEntities")
List<Entity> findAllWithRelations();
```

#### Use EntityGraph
```java
@EntityGraph(attributePaths = {"relatedEntities", "otherRelation"})
List<Entity> findAll();
```

### 3. Caching

#### Enable Second-Level Cache
```properties
# application.properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Entity { ... }
```

#### Use Spring Cache for Services
```java
@Cacheable(value = "entities", key = "#id")
public Entity findById(Long id) {
    return repository.findById(id).orElse(null);
}

@CacheEvict(value = "entities", key = "#entity.id")
public Entity save(Entity entity) {
    return repository.save(entity);
}
```

### 4. Async Processing

#### Use @Async for Non-Blocking Operations
```java
@Async
public CompletableFuture<Void> sendNotificationAsync(String message) {
    // Long-running operation
    return CompletableFuture.completedFuture(null);
}
```

### 5. Connection Pool Optimization
```properties
# HikariCP settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

### 6. Batch Operations
```java
// For bulk inserts
@Modifying
@Query("UPDATE Entity e SET e.status = :status WHERE e.id IN :ids")
void updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") String status);

// Enable batch inserts
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

---

## Database Recommendations

### 1. Index Optimization

Based on the entity structure, add these critical indexes:

```sql
-- CuttingPlan table
CREATE INDEX idx_cutting_plan_projet ON CuttingPlan(projet);
CREATE INDEX idx_cutting_plan_enabled ON CuttingPlan(enabled);
CREATE INDEX idx_cutting_plan_created ON CuttingPlan(createdAt DESC);
CREATE NONCLUSTERED INDEX idx_cutting_plan_search 
    ON CuttingPlan(projet, version, enabled) INCLUDE (description, createdAt);

-- CuttingRequestSerie table (heavily queried)
CREATE INDEX idx_serie_due_date ON CuttingRequestSerie(dueDate, dueShift);
CREATE INDEX idx_serie_machine ON CuttingRequestSerie(machine);
CREATE INDEX idx_serie_status ON CuttingRequestSerie(status);
CREATE NONCLUSTERED INDEX idx_serie_composite 
    ON CuttingRequestSerie(machine, dueDate, status) INCLUDE (serie, quantity);

-- User table
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_active ON users(active);
CREATE INDEX idx_user_site ON users(site_id);

-- QualityNotice table
CREATE INDEX idx_qn_date ON QualityNotice(createdAt DESC);
CREATE INDEX idx_qn_status ON QualityNotice(status);

-- Machine tables
CREATE INDEX idx_machine_type ON Machine(machineType_id);
CREATE INDEX idx_machine_dxf_date ON MachineDxfRapport(date DESC);
CREATE INDEX idx_machine_lsr_date ON MachineLsrRapport(date DESC);

-- Placement table
CREATE INDEX idx_placement_name ON Placement(name);
CREATE INDEX idx_placement_folder ON Placement(folder_id);
```

---

### 2. Query Optimization

**Issue:** N+1 queries in repository methods.

**Solution:** Use JOIN FETCH for related entities.

```java
// CuttingPlanRepository.java
@Query("SELECT cp FROM CuttingPlan cp " +
       "LEFT JOIN FETCH cp.createdBy " +
       "LEFT JOIN FETCH cp.cuttingPlanPartNumbers " +
       "WHERE cp.id = :id")
Optional<CuttingPlan> findByIdWithDetails(@Param("id") Long id);

// Use EntityGraph for complex relationships
@EntityGraph(attributePaths = {
    "cuttingPlanPartNumbers", 
    "cuttingPlanMaterials", 
    "createdBy"
})
List<CuttingPlan> findByProjet(String projet);
```

---

### 3. Connection Pool Tuning

```properties
# application.properties - HikariCP optimization
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.leak-detection-threshold=60000

# For secondary datasources
ctc.datasource.hikari.maximum-pool-size=10
pls.datasource.hikari.maximum-pool-size=10
```

---

### 4. Enable Batch Operations

```properties
# Hibernate batch configuration
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.batch_versioned_data=true
```

---

### 5. Data Archiving Strategy

```sql
-- Archive tables for old data (recommended for data > 1 year)
CREATE TABLE CuttingRequestSerie_Archive AS 
SELECT * FROM CuttingRequestSerie WHERE 1=0;

-- Archiving procedure
CREATE PROCEDURE sp_ArchiveOldData
AS
BEGIN
    INSERT INTO CuttingRequestSerie_Archive
    SELECT * FROM CuttingRequestSerie 
    WHERE createdAt < DATEADD(YEAR, -1, GETDATE())
    AND status = 'COMPLETED';
    
    DELETE FROM CuttingRequestSerie 
    WHERE createdAt < DATEADD(YEAR, -1, GETDATE())
    AND status = 'COMPLETED';
END;
```

---

### 6. Database Monitoring

```sql
-- Enable query store for SQL Server
ALTER DATABASE MG_CMS SET QUERY_STORE = ON;
ALTER DATABASE MG_CMS SET QUERY_STORE (
    OPERATION_MODE = READ_WRITE,
    DATA_FLUSH_INTERVAL_SECONDS = 900,
    INTERVAL_LENGTH_MINUTES = 60
);

-- Identify slow queries
SELECT TOP 20 
    qs.total_elapsed_time / qs.execution_count AS avg_elapsed_time,
    qs.execution_count,
    SUBSTRING(qt.text, 1, 200) AS query_text
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) qt
ORDER BY avg_elapsed_time DESC;
```

---

## Security Recommendations

### 1. CRITICAL: Remove Hardcoded Credentials

**Current (DANGEROUS):**
```properties
spring.datasource.password=Maroc2023*
ctc.datasource.password=tangier11
spring.mail.password=Tanger10
```

**Recommended:** Use environment variables or secrets management.

```properties
# application.properties
spring.datasource.password=${DB_PASSWORD}
spring.mail.password=${MAIL_PASSWORD}

# Or use Spring Cloud Config / Vault
```

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DB_PASSWORD=${DB_PASSWORD}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
```

---

### 2. JWT Security Improvements

```java
@Component
public class JwtTokenProvider {
    
    // Use asymmetric keys for production
    @Value("${jwt.private-key}")
    private String privateKeyPem;
    
    @Value("${jwt.public-key}")
    private String publicKeyPem;
    
    // Token expiration should be configurable
    @Value("${jwt.expiration:3600000}") // 1 hour default
    private long expiration;
    
    // Add refresh token support
    public String generateRefreshToken(Authentication auth) {
        // Longer expiration for refresh tokens
    }
}
```

---

### 3. Rate Limiting

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RateLimitFilter());
        bean.addUrlPatterns("/api/*");
        return bean;
    }
}

// Add to pom.xml
// <dependency>
//     <groupId>io.github.resilience4j</groupId>
//     <artifactId>resilience4j-ratelimiter</artifactId>
// </dependency>
```

---

### 4. Input Validation Enhancement

```java
// Add validation to all request DTOs
public class CuttingPlanRequest {
    
    @NotBlank(message = "Project is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9-]+$")
    private String projet;
    
    @NotNull
    @Min(1)
    @Max(10000)
    private Integer quantity;
    
    @NotEmpty
    @Valid
    private List<CuttingPlanPartNumberRequest> partNumbers;
}
```

---

### 5. CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Restrict to specific origins in production
        config.setAllowedOrigins(Arrays.asList(
            "http://matnr-app16:8085",
            "https://cms.lear.com"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### 1. Index Recommendations
```sql
-- Add indexes for frequently filtered columns
CREATE INDEX idx_entity_name ON Entity(name);
CREATE INDEX idx_entity_created_at ON Entity(createdAt);
CREATE INDEX idx_entity_status ON Entity(status);

-- Composite indexes for common filter combinations
CREATE INDEX idx_entity_date_shift ON CuttingRequestSerie(dueDate, dueShift);
CREATE INDEX idx_serie_machine ON CuttingRequestSerie(machine, serie);
```

### 2. Query Analysis
- Enable SQL logging in development to identify slow queries
- Use EXPLAIN ANALYZE on slow queries
- Add appropriate indexes based on query patterns

### 3. Archive Old Data
```sql
-- Move old data to archive tables
INSERT INTO CuttingRequestSerie_Archive
SELECT * FROM CuttingRequestSerie 
WHERE createdAt < DATEADD(year, -1, GETDATE());

DELETE FROM CuttingRequestSerie 
WHERE createdAt < DATEADD(year, -1, GETDATE());
```

---

## UI/UX Improvements

### 1. Dashboard Navigation Redesign

**Current Issue:** Long navigation menu with many nested items (18+ sections).

**Recommendation:**

```javascript
// Implement collapsible mega-menu with search
const Dashboard = () => {
  const [searchTerm, setSearchTerm] = useState('');
  
  // Add quick search in menu
  return (
    <div className="dashboard-container">
      <input 
        type="text"
        placeholder="Rechercher une fonction..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        className="menu-search"
      />
      
      {/* Filtered menu items */}
      {filteredMenuItems.map(section => (
        <MenuSection key={section.key} {...section} />
      ))}
      
      {/* Quick access bar for favorites */}
      <QuickAccessBar favorites={userFavorites} />
    </div>
  );
};
```

---

### 2. Loading States & Skeleton Screens

**Current:** No consistent loading indicators.

```javascript
// Add loading skeleton component
const TableSkeleton = ({ rows = 10 }) => (
  <div className="skeleton-table">
    {Array(rows).fill().map((_, i) => (
      <div key={i} className="skeleton-row">
        <div className="skeleton-cell" style={{width: '10%'}} />
        <div className="skeleton-cell" style={{width: '30%'}} />
        <div className="skeleton-cell" style={{width: '40%'}} />
        <div className="skeleton-cell" style={{width: '20%'}} />
      </div>
    ))}
  </div>
);

// Usage in EntityList
{loading ? <TableSkeleton /> : <DataTable data={data} />}
```

---

### 3. Toast Notifications

**Missing:** User feedback for actions.

```javascript
// Add react-toastify for notifications
import { toast, ToastContainer } from 'react-toastify';

// Success notification
toast.success('Plan de coupe enregistré avec succès');

// Error notification
toast.error('Erreur lors de la sauvegarde');

// Add to App.js
<ToastContainer 
  position="top-right"
  autoClose={3000}
  hideProgressBar={false}
/>
```

---

### 4. Mobile Responsiveness

**Current:** Limited mobile support.

```scss
// Add responsive breakpoints
$breakpoints: (
  mobile: 576px,
  tablet: 768px,
  desktop: 992px,
  large: 1200px
);

// Responsive table
.entity-table {
  @media (max-width: 768px) {
    display: block;
    
    thead {
      display: none;
    }
    
    tr {
      display: block;
      margin-bottom: 1rem;
      border: 1px solid #ddd;
    }
    
    td {
      display: flex;
      justify-content: space-between;
      padding: 0.5rem;
      
      &::before {
        content: attr(data-label);
        font-weight: bold;
      }
    }
  }
}
```

---

### 5. Dark Mode Support

```javascript
// Add theme context
const ThemeContext = createContext();

const ThemeProvider = ({ children }) => {
  const [isDark, setIsDark] = useState(
    localStorage.getItem('theme') === 'dark'
  );
  
  useEffect(() => {
    document.body.classList.toggle('dark-theme', isDark);
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
  }, [isDark]);
  
  return (
    <ThemeContext.Provider value={{ isDark, toggleTheme: () => setIsDark(!isDark) }}>
      {children}
    </ThemeContext.Provider>
  );
};
```

---

### 6. Form UX Improvements

```javascript
// Auto-save drafts
const useAutoSave = (formData, key) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      localStorage.setItem(`draft_${key}`, JSON.stringify(formData));
    }, 1000);
    return () => clearTimeout(timer);
  }, [formData, key]);
};

// Confirmation before leaving unsaved forms
const useUnsavedChangesWarning = (hasChanges) => {
  useEffect(() => {
    const handler = (e) => {
      if (hasChanges) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [hasChanges]);
};
```

---

### 7. Accessibility (a11y) Improvements

```javascript
// Add ARIA labels
<button 
  aria-label="Supprimer le plan de coupe"
  onClick={handleDelete}
>
  <FontAwesomeIcon icon={faTrash} />
</button>

// Keyboard navigation
<div 
  role="menu" 
  onKeyDown={(e) => {
    if (e.key === 'Escape') closeMenu();
    if (e.key === 'ArrowDown') focusNextItem();
    if (e.key === 'ArrowUp') focusPrevItem();
  }}
>
  {menuItems.map(item => (
    <div role="menuitem" tabIndex={0} key={item.id}>
      {item.label}
    </div>
  ))}
</div>
```

---

## Missing Functionality

### 1. Audit Trail / Activity Logging

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String entityType;
    private String entityId;
    private String action; // CREATE, UPDATE, DELETE
    private String previousValue;
    private String newValue;
    
    @ManyToOne
    private User performedBy;
    
    @CreatedDate
    private LocalDateTime performedAt;
    
    private String ipAddress;
}

// AOP aspect for automatic auditing
@Aspect
@Component
public class AuditAspect {
    
    @AfterReturning(pointcut = "@annotation(Auditable)", returning = "result")
    public void logAudit(JoinPoint jp, Object result) {
        // Log the action
    }
}
```

---

### 2. Export Functionality Enhancement

```javascript
// Add Excel export with formatting
const exportToExcel = async (data, filename) => {
  const workbook = new ExcelJS.Workbook();
  const worksheet = workbook.addWorksheet('Data');
  
  // Add headers with styling
  worksheet.columns = columns.map(col => ({
    header: col.displayName,
    key: col.name,
    width: 20
  }));
  
  // Style header row
  worksheet.getRow(1).font = { bold: true };
  worksheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FF366092' }
  };
  
  // Add data
  data.forEach(row => worksheet.addRow(row));
  
  // Auto-filter
  worksheet.autoFilter = {
    from: 'A1',
    to: `${String.fromCharCode(64 + columns.length)}1`
  };
  
  // Save
  const buffer = await workbook.xlsx.writeBuffer();
  saveAs(new Blob([buffer]), `${filename}.xlsx`);
};
```

---

### 3. Real-time Notifications

```javascript
// WebSocket notification service
class NotificationService {
  constructor() {
    this.stompClient = null;
    this.subscribers = new Map();
  }
  
  connect(userId) {
    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    
    this.stompClient.connect({}, () => {
      this.stompClient.subscribe(`/user/${userId}/notifications`, (message) => {
        const notification = JSON.parse(message.body);
        this.notifySubscribers(notification);
      });
    });
  }
  
  subscribe(callback) {
    const id = Date.now();
    this.subscribers.set(id, callback);
    return () => this.subscribers.delete(id);
  }
  
  notifySubscribers(notification) {
    this.subscribers.forEach(callback => callback(notification));
  }
}

// Usage in component
useEffect(() => {
  const unsubscribe = notificationService.subscribe((notification) => {
    toast.info(notification.message);
  });
  return unsubscribe;
}, []);
```

---

### 4. Bulk Operations

```javascript
// Add bulk selection and actions
const EntityList = () => {
  const [selectedIds, setSelectedIds] = useState(new Set());
  
  const handleBulkDelete = async () => {
    if (confirm(`Supprimer ${selectedIds.size} éléments?`)) {
      await axios.delete('/api/entity/bulk', {
        data: { ids: Array.from(selectedIds) }
      });
      setSelectedIds(new Set());
      refreshData();
    }
  };
  
  const handleBulkExport = () => {
    const selectedData = data.filter(item => selectedIds.has(item.id));
    exportToExcel(selectedData, 'export');
  };
  
  return (
    <>
      {selectedIds.size > 0 && (
        <div className="bulk-actions">
          <span>{selectedIds.size} sélectionné(s)</span>
          <button onClick={handleBulkDelete}>Supprimer</button>
          <button onClick={handleBulkExport}>Exporter</button>
        </div>
      )}
      {/* Table with checkboxes */}
    </>
  );
};
```

---

### 5. Advanced Search & Filters

```javascript
// Saved filters feature
const SavedFilters = () => {
  const [savedFilters, setSavedFilters] = useState([]);
  
  const saveCurrentFilter = () => {
    const filterName = prompt('Nom du filtre:');
    if (filterName) {
      const newFilter = {
        id: Date.now(),
        name: filterName,
        filters: currentFilters
      };
      setSavedFilters([...savedFilters, newFilter]);
      localStorage.setItem('savedFilters', JSON.stringify([...savedFilters, newFilter]));
    }
  };
  
  return (
    <div className="saved-filters">
      <button onClick={saveCurrentFilter}>Sauvegarder le filtre</button>
      <select onChange={(e) => applyFilter(e.target.value)}>
        <option value="">Filtres sauvegardés</option>
        {savedFilters.map(f => (
          <option key={f.id} value={f.id}>{f.name}</option>
        ))}
      </select>
    </div>
  );
};
```

---

### 6. Dashboard Analytics

```javascript
// Add real-time dashboard widgets
const AnalyticsDashboard = () => {
  const [stats, setStats] = useState({});
  
  useEffect(() => {
    // Fetch real-time stats
    const fetchStats = async () => {
      const [production, quality, maintenance] = await Promise.all([
        axios.get('/api/stats/production'),
        axios.get('/api/stats/quality'),
        axios.get('/api/stats/maintenance')
      ]);
      setStats({ production, quality, maintenance });
    };
    
    fetchStats();
    const interval = setInterval(fetchStats, 60000); // Refresh every minute
    return () => clearInterval(interval);
  }, []);
  
  return (
    <div className="analytics-grid">
      <StatCard title="Productions du jour" value={stats.production?.todayCount} />
      <StatCard title="Taux de qualité" value={`${stats.quality?.rate}%`} />
      <StatCard title="Machines actives" value={stats.maintenance?.activeMachines} />
      <ChartWidget data={stats.production?.hourlyData} />
    </div>
  );
};
```

### 1. Extract Common Logic

#### Create Utility Classes
```javascript
// utils/dateUtils.js
export const formatDate = (date) => moment(date).format('YYYY-MM-DD');
export const getShift = (date) => {
  const hour = moment(date).hour();
  if (hour >= 0 && hour < 8) return 1;
  if (hour >= 8 && hour < 16) return 2;
  return 3;
};
```

### 2. Error Handling

#### Global Error Boundary
```javascript
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return <h1>Something went wrong.</h1>;
    }
    return this.props.children;
  }
}
```

#### Backend Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(500).body("Internal server error");
    }
}
```

### 3. Type Safety

#### Use PropTypes Consistently
```javascript
import PropTypes from 'prop-types';

MyComponent.propTypes = {
  data: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
  })).isRequired,
  onSelect: PropTypes.func.isRequired,
};
```

### 4. Constants and Enums

#### Define Constants
```javascript
// constants.js
export const SHIFT_TIMES = {
  1: { start: '22:00', end: '06:00' },
  2: { start: '06:00', end: '14:00' },
  3: { start: '14:00', end: '22:00' },
};

export const STATUS = {
  PENDING: 'pending',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled',
};
```

### 5. Reduce Code Duplication

#### Extract Common Patterns
```javascript
// Custom hook for data fetching
function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get(url)
      .then(res => setData(res.data))
      .catch(err => setError(err))
      .finally(() => setLoading(false));
  }, [url]);

  return { data, loading, error };
}
```

---

## Security Enhancements

### 1. Input Validation
```java
// Backend validation
@PostMapping
public Entity create(@Valid @RequestBody EntityDTO dto) {
    // Validated automatically
}

// DTO with validation
public class EntityDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;
    
    @Email
    private String email;
}
```

### 2. SQL Injection Prevention
```java
// Always use parameterized queries
@Query("SELECT e FROM Entity e WHERE e.name = :name")
Entity findByName(@Param("name") String name);

// Never concatenate user input into queries
```

### 3. Rate Limiting
```java
@RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
@GetMapping("/api/resource")
public ResponseEntity<?> getResource() { ... }
```

### 4. CORS Configuration
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        // ...
    }
}
```

---

## Architecture Improvements

### 1. Migrate to Functional Components with Hooks

**Current:** Class-based components throughout.

```javascript
// Before (Current pattern)
class CuttingPlan extends Component {
  constructor() {
    super();
    this.state = { data: [], loading: true };
  }
  
  componentDidMount() {
    this.fetchData();
  }
  
  fetchData = async () => { ... }
  
  render() { ... }
}

// After (Recommended pattern)
const CuttingPlan = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useSelector(state => state.security);
  
  useEffect(() => {
    fetchData();
  }, []);
  
  const fetchData = async () => { ... };
  
  return ( ... );
};

// Custom hooks for reusable logic
const useFetch = (url) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    const controller = new AbortController();
    
    axios.get(url, { signal: controller.signal })
      .then(res => setData(res.data))
      .catch(err => {
        if (!axios.isCancel(err)) setError(err);
      })
      .finally(() => setLoading(false));
    
    return () => controller.abort();
  }, [url]);
  
  return { data, loading, error };
};
```

---

### 2. Implement Redux Toolkit

**Current:** Legacy Redux with manual action creators.

```javascript
// Current pattern
export const SET_CURRENT_USER = 'SET_CURRENT_USER';

export const setCurrentUser = (user) => ({
  type: SET_CURRENT_USER,
  payload: user
});

// Recommended: Redux Toolkit
import { createSlice, createAsyncThunk, configureStore } from '@reduxjs/toolkit';

export const loginUser = createAsyncThunk(
  'security/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await axios.post('/api/users/login', credentials);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

const securitySlice = createSlice({
  name: 'security',
  initialState: {
    user: {},
    validToken: false,
    loading: false,
    error: null
  },
  reducers: {
    logout: (state) => {
      state.user = {};
      state.validToken = false;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.loading = true;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.validToken = true;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  }
});

export const store = configureStore({
  reducer: {
    security: securitySlice.reducer,
    // Add more slices
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false
    })
});
```

---

### 3. API Layer Abstraction

```javascript
// services/api.js
import axios from 'axios';

class ApiClient {
  constructor(baseURL = '/api') {
    this.client = axios.create({
      baseURL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    this.setupInterceptors();
  }
  
  setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('jwtToken');
        if (token) {
          config.headers.Authorization = token;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );
    
    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('jwtToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }
  
  // Generic CRUD methods
  async getAll(entity, params = {}) {
    const response = await this.client.get(`/${entity}/all`, { params });
    return response.data;
  }
  
  async getById(entity, id) {
    const response = await this.client.get(`/${entity}/${id}`);
    return response.data;
  }
  
  async create(entity, data) {
    const response = await this.client.post(`/${entity}`, data);
    return response.data;
  }
  
  async update(entity, id, data) {
    const response = await this.client.put(`/${entity}/${id}`, data);
    return response.data;
  }
  
  async delete(entity, id) {
    await this.client.delete(`/${entity}/${id}`);
  }
}

export const api = new ApiClient();

// Entity-specific services
export const cuttingPlanService = {
  getAll: (params) => api.getAll('cuttingPlan', params),
  getById: (id) => api.getById('cuttingPlan', id),
  create: (data) => api.create('cuttingPlan', data),
  update: (id, data) => api.update('cuttingPlan', id, data),
  delete: (id) => api.delete('cuttingPlan', id),
  enable: (id) => api.client.post(`/cuttingPlan/${id}/enable`),
  disable: (id) => api.client.post(`/cuttingPlan/${id}/disable`)
};
```

---

### 4. Testing Infrastructure

```javascript
// Add Jest + React Testing Library
// package.json
{
  "scripts": {
    "test": "react-scripts test",
    "test:coverage": "react-scripts test --coverage"
  },
  "jest": {
    "collectCoverageFrom": [
      "src/**/*.{js,jsx}",
      "!src/index.js"
    ]
  }
}

// Example test
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { store } from './store';
import CuttingPlan from './components/Layout/CuttingPlan';

describe('CuttingPlan', () => {
  test('renders cutting plan form', async () => {
    render(
      <Provider store={store}>
        <CuttingPlan />
      </Provider>
    );
    
    expect(screen.getByText('Plan de coupe')).toBeInTheDocument();
  });
  
  test('saves cutting plan on submit', async () => {
    render(
      <Provider store={store}>
        <CuttingPlan />
      </Provider>
    );
    
    fireEvent.change(screen.getByLabelText('Projet'), {
      target: { value: 'TEST-PROJECT' }
    });
    
    fireEvent.click(screen.getByText('Enregistrer'));
    
    await waitFor(() => {
      expect(screen.getByText('Sauvegardé avec succès')).toBeInTheDocument();
    });
  });
});
```

---

### 5. TypeScript Migration (Long-term)

```typescript
// Gradual migration strategy
// 1. Add TypeScript dependencies
// npm install --save-dev typescript @types/react @types/react-dom @types/node

// 2. Create tsconfig.json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "noFallthroughCasesInSwitch": true,
    "module": "esnext",
    "moduleResolution": "node",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx"
  },
  "include": ["src"]
}

// 3. Create types
// types/entities.ts
interface CuttingPlan {
  id: number;
  description: string;
  projet: string;
  version: string;
  quantity: number;
  enabled: boolean;
  createdAt: string;
  createdBy: User;
  cuttingPlanPartNumbers: CuttingPlanPartNumber[];
  cuttingPlanMaterials: CuttingPlanMaterial[];
}

interface User {
  matricule: string;
  username: string;
  firstName: string;
  lastName: string;
  roles: Role[];
}

// 4. Convert components gradually
// CuttingPlan.tsx
interface Props {
  match: {
    params: {
      entityId?: string;
    };
  };
}

const CuttingPlan: React.FC<Props> = ({ match }) => {
  const [plan, setPlan] = useState<CuttingPlan | null>(null);
  // ...
};
```

---

## Priority Action Items

### 🔴 Critical (Immediate)
| # | Item | Impact | Effort |
|---|------|--------|--------|
| 1 | Remove hardcoded credentials from properties | Security | Low |
| 2 | Upgrade Spring Boot to 2.7.18 | Security, Stability | Medium |
| 3 | Add database indexes for slow queries | Performance | Low |
| 4 | Fix N+1 query issues in repositories | Performance | Medium |

### 🟠 High Priority (Next Sprint)
| # | Item | Impact | Effort |
|---|------|--------|--------|
| 5 | Upgrade to Java 17 LTS | Compatibility | Medium |
| 6 | Upgrade React to 18.x | Performance | Medium |
| 7 | Add global exception handler | Reliability | Low |
| 8 | Implement debouncing on search inputs | UX, Performance | Low |
| 9 | Add loading skeletons | UX | Low |

### 🟡 Medium Priority (Next Quarter)
| # | Item | Impact | Effort |
|---|------|--------|--------|
| 10 | Implement Redis/Caffeine caching | Performance | Medium |
| 11 | Add audit logging | Compliance | Medium |
| 12 | Split ScheduledTask.java into smaller classes | Maintainability | Medium |
| 13 | Add toast notifications | UX | Low |
| 14 | Implement bulk operations | UX | Medium |

### 🟢 Low Priority (Long-term)
| # | Item | Impact | Effort |
|---|------|--------|--------|
| 15 | Migrate to functional components | Maintainability | High |
| 16 | Implement Redux Toolkit | Maintainability | High |
| 17 | Add TypeScript | Type Safety | High |
| 18 | Add comprehensive testing | Quality | High |
| 19 | Implement dark mode | UX | Medium |
| 20 | Add mobile responsiveness | Accessibility | Medium |

---

## Monitoring Recommendations

### 1. Application Monitoring
```yaml
# Add Spring Actuator
# pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

# application.properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when_authorized
```

### 2. Frontend Performance
```javascript
// Add React Profiler
import { Profiler } from 'react';

const onRenderCallback = (id, phase, actualDuration) => {
  if (actualDuration > 16) { // Longer than 1 frame
    console.warn(`Slow render: ${id} took ${actualDuration}ms`);
  }
};

<Profiler id="App" onRender={onRenderCallback}>
  <App />
</Profiler>
```

### 3. Error Tracking
```javascript
// Add Sentry for error tracking
import * as Sentry from '@sentry/react';

Sentry.init({
  dsn: 'YOUR_SENTRY_DSN',
  environment: process.env.NODE_ENV,
  tracesSampleRate: 0.1
});

// Wrap App with error boundary
const App = Sentry.withErrorBoundary(AppComponent, {
  fallback: <ErrorFallback />
});
```

---

## Recent Updates & Recommendations

### 1. PartNumberWeight Management

**Implementation Status**: ✅ Completed

**Description**: New entity to store unit weights for part numbers, enabling accurate box weight estimation.

**Recommendations**:
1. **Data Population**:
   - Use the Excel import feature to bulk-load part number weights
   - Establish a process to update weights when new part numbers are added
   - Consider integrating with ERP/PLM system for automatic weight updates

2. **Data Validation**:
   - Add weight range validation (e.g., 0.001 kg to 100 kg)
   - Flag missing weights in production workflow
   - Create reports for part numbers without configured weights

3. **Integration**:
   ```java
   // Recommended: Add validation in BoxWeightService
   public void validatePartNumberWeight(String partNumber) {
       Optional<PartNumberWeight> weight = partNumberWeightRepository.findByPartnumber(partNumber);
       if (weight.isEmpty()) {
           throw new ValidationException("Weight not configured for part number: " + partNumber);
       }
   }
   ```

4. **Frontend Enhancement**:
   - Add autocomplete for part number search
   - Show weight history/audit trail
   - Display visual indicator when weight is missing

---

### 2. BoxWeight Estimation Improvements

**Implementation Status**: ✅ Completed (Entity fields added)

**Current State**:
- New fields: `quantity`, `estimatedWeight`
- Estimation formula: `(quantity × unitWeight) + emptyBoxWeight`

**Recommendations**:
1. **Automatic Calculation**:
   ```java
   // BoxWeightService.java
   public BoxWeight createWithEstimation(BoxWeight boxWeight, String partNumber) {
       // Get part number weight
       PartNumberWeight pnWeight = partNumberWeightService.findByPartnumber(partNumber)
           .orElse(null);
       
       // Get empty box weight
       BoxTypeConfig boxConfig = boxTypeConfigService.findByBoxType(boxWeight.getBoxType())
           .orElse(null);
       
       if (pnWeight != null && boxConfig != null && boxWeight.getQuantity() != null) {
           double estimated = (boxWeight.getQuantity() * pnWeight.getWeightUnit()) 
                            + boxConfig.getEmptyBoxWeight();
           boxWeight.setEstimatedWeight(estimated);
       }
       
       return save(boxWeight);
   }
   ```

2. **Variance Threshold Validation**:
   ```java
   // Add configurable threshold
   private static final double WEIGHT_VARIANCE_THRESHOLD = 0.05; // 5%
   
   public boolean isWeightWithinExpectedRange(BoxWeight boxWeight) {
       if (boxWeight.getEstimatedWeight() == null || boxWeight.getSentWeight() == null) {
           return true; // Skip validation if no estimate
       }
       
       double variance = Math.abs(boxWeight.getSentWeight() - boxWeight.getEstimatedWeight()) 
                       / boxWeight.getEstimatedWeight();
       
       return variance <= WEIGHT_VARIANCE_THRESHOLD;
   }
   ```

3. **Enhanced Validation Workflow**:
   - **Green**: Weight within 5% of estimate
   - **Yellow**: Weight 5-10% variance (warning)
   - **Red**: Weight >10% variance (requires review)

4. **Frontend Improvements**:
   ```javascript
   // BoxWeightFilling.js - Add estimation display
   renderEstimation() {
       const { quantity, partNumber, boxType } = this.state;
       
       if (!quantity || !partNumber) return null;
       
       // Call API to get estimation
       axios.get(`/api/boxWeight/estimate`, { 
           params: { partNumber, quantity, boxType }
       })
       .then(response => {
           this.setState({ estimatedWeight: response.data.estimatedWeight });
       });
       
       return (
           <div className="estimation-display">
               <strong>Estimated Weight:</strong> {this.state.estimatedWeight} kg
           </div>
       );
   }
   ```

5. **Reporting**:
   - Add dashboard showing boxes with high weight variance
   - Track estimation accuracy over time
   - Alert quality team for consistent variances by part number

---

### 3. MachineLog Security Enhancement

**Implementation Status**: ✅ Completed

**Description**: Added path traversal protection to prevent unauthorized file access.

**Security Measures Implemented**:
```java
// Path validation
private boolean isPathSafe(String input) {
    return input != null && !input.contains("..") 
        && !input.contains("/") && !input.contains("\\");
}

// Canonical path verification
private boolean isPathWithinBase(File file, File baseDir) {
    Path filePath = file.getCanonicalFile().toPath();
    Path basePath = baseDir.getCanonicalFile().toPath();
    return filePath.startsWith(basePath);
}
```

**Additional Recommendations**:
1. **Comprehensive Audit Logging**:
   ```java
   @Aspect
   @Component
   public class FileAccessAuditAspect {
       
       @Autowired
       private AuditLogRepository auditLogRepository;
       
       @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) && " +
               "execution(* com.lear.MGCMS.controller.MachineLogController.*(..))")
       public Object auditFileAccess(ProceedingJoinPoint joinPoint) throws Throwable {
           String username = SecurityContextHolder.getContext()
               .getAuthentication().getName();
           String method = joinPoint.getSignature().getName();
           Object[] args = joinPoint.getArgs();
           
           // Log access attempt
           AuditLog log = new AuditLog(username, method, Arrays.toString(args));
           
           try {
               Object result = joinPoint.proceed();
               log.setStatus("SUCCESS");
               return result;
           } catch (Exception e) {
               log.setStatus("FAILED: " + e.getMessage());
               throw e;
           } finally {
               auditLogRepository.save(log);
           }
       }
   }
   ```

2. **Rate Limiting**:
   - Implement request rate limiting per user
   - Prevent automated file scanning attempts

3. **File Access Whitelist**:
   ```properties
   # application.properties
   lear.lectraHistory.allowedExtensions=.txt,.log
   lear.lectraHistory.maxFileSize=10485760  # 10MB
   ```

4. **Additional Validations**:
   ```java
   private void validateFileAccess(File file) {
       // Check file extension
       String extension = getFileExtension(file.getName());
       if (!ALLOWED_EXTENSIONS.contains(extension)) {
           throw new SecurityException("File type not allowed: " + extension);
       }
       
       // Check file size
       if (file.length() > MAX_FILE_SIZE) {
           throw new SecurityException("File too large: " + file.length());
       }
       
       // Check file age (e.g., only files modified in last 90 days)
       long daysSinceModified = (System.currentTimeMillis() - file.lastModified()) 
                               / (1000 * 60 * 60 * 24);
       if (daysSinceModified > 90) {
           throw new SecurityException("File too old: " + daysSinceModified + " days");
       }
   }
   ```

---

### 4. Quality Notice Validation Improvements

**Implementation Status**: ✅ Completed

**Update**: Enhanced 0BF validation to accept both `-0BF` and `-MBF` suffixes.

**Current Implementation**:
```javascript
// CuttingPlanForm.js - Verification 4
if (configObj.validated0BF === true && cpmp.machine === "Lectra"
    && !cpmp.placement.includes("-0BF") && !cpmp.placement.includes("-MBF")) {
    error.push(cpmp.placement + ": validated 0BF: placement name must contain -0BF or -MBF")
}
```

**Recommendations**:
1. **Centralized Validation Rules**:
   ```javascript
   // validationRules.js
   export const PLACEMENT_NAMING_RULES = {
       VALIDATED_0BF: {
           suffixes: ['-0BF', '-MBF'],
           machines: ['Lectra'],
           errorMessage: 'Placement name must contain -0BF or -MBF for validated 0BF materials'
       },
       ESPACE_RELARGE: {
           required: 'ESP00',
           condition: 'validated0BF',
           errorMessage: 'Buffer space must be ESP00 for validated 0BF'
       }
   };
   
   export function validatePlacementNaming(placement, config) {
       const errors = [];
       
       if (config.validated0BF === true) {
           // Check suffix
           const hasSuffix = PLACEMENT_NAMING_RULES.VALIDATED_0BF.suffixes
               .some(suffix => placement.name.includes(suffix));
           
           if (!hasSuffix && PLACEMENT_NAMING_RULES.VALIDATED_0BF.machines.includes(placement.machine)) {
               errors.push(PLACEMENT_NAMING_RULES.VALIDATED_0BF.errorMessage);
           }
           
           // Check espace relarge
           if (placement.espaceRelarge !== PLACEMENT_NAMING_RULES.ESPACE_RELARGE.required) {
               errors.push(PLACEMENT_NAMING_RULES.ESPACE_RELARGE.errorMessage);
           }
       }
       
       return errors;
   }
   ```

2. **Backend Validation**:
   ```java
   // Add server-side validation to complement frontend checks
   @Service
   public class CuttingPlanValidationService {
       
       public List<String> validatePlacement(CuttingPlanMaterialPlacement placement, 
                                            PartNumberMaterialConfig config) {
           List<String> errors = new ArrayList<>();
           
           if (Boolean.TRUE.equals(config.getValidated0BF())) {
               String placementName = placement.getPlacement();
               boolean hasValidSuffix = placementName.contains("-0BF") 
                                     || placementName.contains("-MBF");
               
               if (!hasValidSuffix && "Lectra".equals(placement.getMachine())) {
                   errors.add("Placement " + placementName + 
                            " must contain -0BF or -MBF for validated 0BF materials");
               }
               
               if (!"ESP00".equals(placement.getEspaceRelarge())) {
                   errors.add("Placement " + placementName + 
                            " must have ESP00 buffer space for validated 0BF");
               }
           }
           
           return errors;
       }
   }
   ```

3. **Configuration Management**:
   ```java
   // Make validation rules configurable
   @Entity
   public class PlacementValidationRule {
       private Long id;
       private String ruleName;
       private String machineType;
       private Boolean requiresSuffix;
       private String allowedSuffixes; // Comma-separated: "-0BF,-MBF"
       private String requiredEspaceRelarge;
       private Boolean active;
   }
   ```

4. **Documentation & User Guidance**:
   - Add tooltip in UI explaining suffix requirements
   - Show examples of valid placement names
   - Provide auto-suggestion when creating placement names

5. **Historical Data Migration**:
   ```sql
   -- Find non-compliant placements
   SELECT cp.id, cp.definition, cpmp.placement
   FROM cutting_plan cp
   JOIN cutting_plan_material cpm ON cp.id = cpm.cutting_plan_id
   JOIN cutting_plan_material_placement cpmp ON cpm.id = cpmp.cutting_plan_material_id
   JOIN part_number_material_config pnmc ON cpm.part_number_material = pnmc.part_number_material
   WHERE pnmc.validated_0BF = 1
     AND cpmp.machine = 'Lectra'
     AND cpmp.placement NOT LIKE '%-0BF%'
     AND cpmp.placement NOT LIKE '%-MBF%';
   ```

---

### 5. ReftissuMargin Integration in getMarge

**Implementation Status**: ✅ Completed

**Description**: Dynamic margin calculation based on length intervals and layer configuration.

**Recommendations**:
1. **Configuration UI**:
   - Create admin interface for managing ReftissuMargin rules
   - Support bulk import/export of margin configurations
   - Version control for configuration changes

2. **Validation & Testing**:
   ```javascript
   // Add unit tests for margin calculation
   describe('getMarge', () => {
       it('should return correct margin for length 800 and 15 layers', () => {
           const margin = getMarge(800, 15, 'MATERIAL-001');
           expect(margin).toBe(75);
       });
       
       it('should return 0 when no config exists', () => {
           const margin = getMarge(1000, 10, 'UNKNOWN-MATERIAL');
           expect(margin).toBe(0);
       });
       
       it('should handle edge cases at interval boundaries', () => {
           const margin1 = getMarge(1000, 10, 'MATERIAL-001');
           const margin2 = getMarge(1001, 10, 'MATERIAL-001');
           // Should use different intervals
           expect(margin1).not.toBe(margin2);
       });
   });
   ```

3. **Performance Optimization**:
   ```javascript
   // Cache margin lookups
   class MarginCalculator {
       constructor() {
           this.cache = new Map();
       }
       
       getMarge(longueur, nbrCouche, partNumberMaterial) {
           const cacheKey = `${partNumberMaterial}_${longueur}_${nbrCouche}`;
           
           if (this.cache.has(cacheKey)) {
               return this.cache.get(cacheKey);
           }
           
           const margin = this._calculateMarge(longueur, nbrCouche, partNumberMaterial);
           this.cache.set(cacheKey, margin);
           
           return margin;
       }
       
       _calculateMarge(longueur, nbrCouche, partNumberMaterial) {
           // Existing calculation logic
       }
   }
   ```

4. **Error Handling**:
   ```javascript
   getMarge(longueur, nbrCouche, partNumberMaterial) {
       try {
           const config = this.state.partNumberMaterialConfigs[partNumberMaterial];
           
           if (!config) {
               console.warn(`No config found for material: ${partNumberMaterial}`);
               return 0;
           }
           
           if (!config.reftissuMargins || config.reftissuMargins.length === 0) {
               console.warn(`No margin rules for material: ${partNumberMaterial}`);
               return 0;
           }
           
           const marginConfig = config.reftissuMargins.find(rm => 
               longueur >= rm.minLength && longueur <= rm.maxLength
           );
           
           if (!marginConfig) {
               console.warn(`No margin rule matches length ${longueur} for ${partNumberMaterial}`);
               return 0;
           }
           
           if (!marginConfig.pliesConfig) {
               console.error(`Invalid pliesConfig for ${partNumberMaterial}`);
               return 0;
           }
           
           // Parse and find margin
           const pliesConfigs = marginConfig.pliesConfig.split('|');
           for (let pc of pliesConfigs) {
               const parts = pc.split(';');
               if (parts.length !== 2) {
                   console.error(`Invalid pliesConfig format: ${pc}`);
                   continue;
               }
               
               const [maxLayers, margin] = parts;
               if (nbrCouche <= parseInt(maxLayers)) {
                   return parseFloat(margin);
               }
           }
           
           console.warn(`No margin found for ${nbrCouche} layers in ${partNumberMaterial}`);
           return 0;
           
       } catch (error) {
           console.error('Error calculating margin:', error);
           return 0;
       }
   }
   ```

5. **Visualization**:
   ```javascript
   // Add visual helper showing margin rules
   renderMarginRulesHelper(partNumberMaterial) {
       const config = this.state.partNumberMaterialConfigs[partNumberMaterial];
       if (!config?.reftissuMargins) return null;
       
       return (
           <div className="margin-rules-helper">
               <h4>Margin Rules for {partNumberMaterial}</h4>
               {config.reftissuMargins.map((rule, idx) => (
                   <div key={idx} className="margin-rule">
                       <strong>Length: {rule.minLength} - {rule.maxLength} mm</strong>
                       <table>
                           <thead>
                               <tr><th>Layers</th><th>Margin</th></tr>
                           </thead>
                           <tbody>
                               {rule.pliesConfig.split('|').map(pc => {
                                   const [layers, margin] = pc.split(';');
                                   return (
                                       <tr key={pc}>
                                           <td>≤ {layers}</td>
                                           <td>{margin} mm</td>
                                       </tr>
                                   );
                               })}
                           </tbody>
                       </table>
                   </div>
               ))}
           </div>
       );
   }
   ```

6. **Backend API**:
   ```java
   // Add endpoint to calculate margin (useful for validation)
   @GetMapping("/api/cuttingPlan/calculateMargin")
   public ResponseEntity<Double> calculateMargin(
           @RequestParam Double longueur,
           @RequestParam Integer nbrCouche,
           @RequestParam String partNumberMaterial) {
       
       Double margin = cuttingPlanService.calculateMargin(
           longueur, nbrCouche, partNumberMaterial);
       
       return ResponseEntity.ok(margin);
   }
   ```

---

*This document should be reviewed and updated quarterly to reflect project evolution and new best practices.*
