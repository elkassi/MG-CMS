# MG-CMS Project Structure Guide

**MG-CMS (Cutting Management System)** - A comprehensive manufacturing management system developed for LEAR Corporation to manage cutting operations, quality control, maintenance, and production workflows.

> **Version:** 3.59  
> **Author:** Mouad El Ghazi  
> **Last Updated:** December 2025

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Application Features](#application-features)
4. [File Structure](#file-structure)
5. [Authentication (JWT)](#authentication-jwt)
6. [Routing Structure](#routing-structure)
7. [Generic Components](#generic-components)
8. [Backend API Patterns](#backend-api-patterns)
9. [Role Management](#role-management)
10. [Multi-Database Configuration](#multi-database-configuration)

---

## Project Overview

MG-CMS is a full-stack web application designed to manage cutting operations in a manufacturing environment. It integrates multiple data sources and provides real-time tracking, quality management, scheduling optimization, and comprehensive reporting.

### Key Modules
- **Production Management** - Cutting requests, series management, box tracking
- **CAD Department** - Cutting plans, placements, pattern management
- **Quality Control** - Quality notices, audits, validation workflows
- **Maintenance** - Preventive maintenance, 1st level checks, interventions
- **Scheduling/Ordonnancement** - Machine scheduling, optimization algorithms
- **Logistics** - Stock reports, roll consumption tracking, shortage reports
- **KPI & Reports** - Machine performance, IPPM, consumable statistics

---

## Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 16 | Core programming language |
| Spring Boot | 2.5.3 | Application framework |
| Spring Security | - | JWT-based authentication |
| Spring Data JPA | - | Database access layer |
| Hibernate | - | ORM with SQL Server dialect |
| WebSocket/STOMP | - | Real-time communication |
| Apache POI | - | Excel file processing |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 17.0.1 | UI framework |
| Redux | 4.0.5 | State management |
| React Router | 5.2.0 | Client-side routing |
| Material UI | 5.14.14 | UI components |
| Bootstrap | 4.6.0 | CSS framework |
| Axios | 0.21.1 | HTTP client |
| Recharts/Chart.js | - | Data visualization |
| DevExpress Scheduler | 4.0.5 | Scheduling components |

### Database
- **Primary Database:** Microsoft SQL Server (MG_CMS)
- **Additional Data Sources:**
  - `plt_viewer` - PLT file viewer data
  - `qualite` - Quality management data
  - `MG_PLS_NEW` - PLS system data
  - `splice` - Splice management data
  - `LearPokaYoke` - Poka-yoke system data
  - `IMS_NEWAPP` - IMS application data

---

## Application Features

### 1. Production Module
| Feature | Description | Route |
|---------|-------------|-------|
| Préparation | Production preparation | `/preparation` |
| Gamme CMS | CMS recipe management | `/gammeCMS` |
| Statut Process Coupe | Cutting process status | `/demande-de-coupe` |
| Matelassage/Coupe | Spreading and cutting | `/matelassage` |
| Fiche de matelassage | Spreading sheet | `/form` |
| Consommation Rouleaux | Roll consumption | `/cuttingRequestSerieRouleauData` |
| Scan Rouleau | Roll scanning | `/scanRouleau` |

### 2. CAD Module
| Feature | Description | Route |
|---------|-------------|-------|
| Plan de coupe | Cutting plan management | `/cuttingPlan` |
| Combinaison plans | Plan combination | `/cuttingPlanCombination` |
| Pattern Search | Pattern search | `/patternSearch` |
| Placement | Placement management | `/placement` |
| Part Number Material | Material configuration | `/partNumberMaterialConfig` |
| Part Number Weight | Part number weight management | `/partNumberWeight` |
| Box Type Config | Empty box weight configuration | `/boxTypeConfig` |
| Drill Emp | Drill patterns | `/drillEmp` |
| Vitesse de coupe | Cutting speed config | `/cuttingSpeed` |

### 3. Quality Module
| Feature | Description | Route |
|---------|-------------|-------|
| Flash qualité | Quality flash reports | `/qn` |
| Vérification Qualité | Quality verification | `/verificationQualite` |
| Audit Qualité | Quality audits | `/auditQualite` |
| Quality Notice | Quality notices | `/qualityNotice` |
| Validation défaut rouleau | Roll defect validation | `/validationDefautRouleau` |
| Suivi Qualité | Quality tracking | `/suiviQulite` |

### 4. Maintenance Module
| Feature | Description | Route |
|---------|-------------|-------|
| Maintenance Machine | Machine maintenance | `/maintenanceIntervention` |
| Maintenance 1er niveau | First level maintenance | `/firstCheck` |
| Machine de Coupe | Cutting machines config | `/productionTable` |

### 5. KPI & Reporting
| Feature | Description | Route |
|---------|-------------|-------|
| KPI machines | Machine KPIs | `/rapportLectra` |
| KPI Charge Machines | Stacked bar chart of machine load per shift | `/kpiChargeMachine` |
| KPI Réactivité Maintenance | Maintenance reaction time KPIs | `/kpiMaintenance` |
| Status Maintenance | Maintenance status | `/statusMaintenance` |
| Consumable Stats | Consumable statistics | `/consumableStat` |
| IPPM Report | IPPM reporting | `/ippmReport` |
| Rapport Usage/BOM | Usage/BOM report | `/rapportUsage` |
| Rapport Shortage | Shortage report | `/rapportShortage` |

### 6. Scheduling (Ordonnancement)
| Feature | Description | Route |
|---------|-------------|-------|
| Scheduling Dashboard | Main scheduling view | `/schedulingDashboard` |
| Ordonnancement | Scheduling management | `/ordonnancement` |

---

## File Structure

```
MG-CMS/
├── pom.xml                      # Maven configuration
├── package.json                 # NPM dependencies
├── webpack.config.js            # Webpack bundling config
├── src/
│   ├── main/
│   │   ├── java/com/lear/
│   │   │   ├── MGCMS/           # Main application module
│   │   │   │   ├── MgcmsApplication.java  # Spring Boot entry
│   │   │   │   ├── ScheduledTask.java     # Scheduled jobs
│   │   │   │   ├── domain/               # JPA entities (90+ entities)
│   │   │   │   │   ├── CuttingPlan/      # Cutting plan entities
│   │   │   │   │   ├── CuttingRequest/   # Cutting request entities
│   │   │   │   │   ├── scheduling/       # Scheduling entities
│   │   │   │   │   └── scanCoupe/        # Scan cutting entities
│   │   │   │   ├── repositories/         # JPA repositories
│   │   │   │   ├── services/             # Business logic services
│   │   │   │   ├── controller/           # REST API controllers
│   │   │   │   ├── security/             # Security configuration
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── PersistenceConfiguration.java  # Multi-DB config
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── payload/              # Request/Response DTOs
│   │   │   │   ├── utils/                # Utility classes
│   │   │   │   └── validator/            # Custom validators
│   │   │   ├── cms/                 # CMS integration module
│   │   │   ├── ctc/                 # CTC integration module
│   │   │   ├── pls/                 # PLS integration module
│   │   │   ├── splice/              # Splice integration module
│   │   │   └── learpokayoke/        # Poka-yoke integration
│   │   ├── js/
│   │   │   ├── App.js               # Main React component & routes
│   │   │   ├── index.js             # React entry point
│   │   │   ├── store.js             # Redux store configuration
│   │   │   ├── metadata.js          # Entity metadata definitions
│   │   │   ├── history.js           # Browser history
│   │   │   ├── actions/             # Redux actions
│   │   │   │   ├── securityAction.js
│   │   │   │   ├── userAction.js
│   │   │   │   └── types.js
│   │   │   ├── reducers/            # Redux reducers
│   │   │   ├── securityUtils/       # JWT utilities
│   │   │   │   ├── SecuredRoute.js
│   │   │   │   └── setJWTToken.js
│   │   │   ├── components/
│   │   │   │   ├── EntityList.js    # Generic CRUD table
│   │   │   │   ├── EntityForm.js    # Generic form component
│   │   │   │   ├── Dashboard.js     # Navigation sidebar
│   │   │   │   ├── Dashboard-Tunisie.js
│   │   │   │   ├── Layout/          # Page components (60+ pages)
│   │   │   │   │   ├── Home.js
│   │   │   │   │   ├── CuttingPlan.js
│   │   │   │   │   ├── DemandeDeCoupe.js
│   │   │   │   │   ├── SchedulingDashboard.js
│   │   │   │   │   ├── ordonnancement/  # Scheduling sub-components
│   │   │   │   │   └── ...
│   │   │   │   ├── styles/          # SCSS stylesheets
│   │   │   │   └── utils/           # Utility components
│   │   │   └── styles/              # Global styles
│   │   └── resources/
│   │       ├── application.properties       # Main config
│   │       ├── application-local.properties
│   │       ├── application-tanger.properties
│   │       ├── application-tunisie.properties
│   │       └── templates/
│   │           └── index.html
│   └── test/                        # Test files
├── scripts/
│   └── optimizer.py                 # Python optimization script
└── target/                          # Build output
```

---

## Authentication (JWT)

### Overview
The application uses JWT (JSON Web Token) for authentication. The token is stored in `localStorage` and automatically attached to all API requests.

### Frontend Setup

#### 1. Token Storage and Validation (`App.js`)
```javascript
import jwt_decode from 'jwt-decode';
import setJWTToken from './securityUtils/setJWTToken';
import { SET_CURRENT_USER } from './actions/types';
import { logout } from './actions/securityAction';
import store from './store';

const jwtToken = localStorage.jwtToken;

if(jwtToken){ 
  setJWTToken(jwtToken);
  const decode_jwtToken = jwt_decode(jwtToken);
  store.dispatch({
      type: SET_CURRENT_USER,
      payload: decode_jwtToken
  });  
  const currentTime = Date.now() / 1000;
  if(decode_jwtToken.exp < currentTime){
    store.dispatch(logout())
    window.location.pathname = "/login";
  }
} else {
  if(window.location.pathname !== "/login") {
    window.location.pathname = "/login";
  }
}
```

#### 2. Setting JWT Token in Axios Headers (`securityUtils/setJWTToken.js`)
```javascript
import axios from 'axios';

const setJWTToken = token => {
    if(token) {
        axios.defaults.headers.common['Authorization'] = token;
    } else {
        delete axios.defaults.headers.common['Authorization'];
    }
}

export default setJWTToken;
```

#### 3. Secured Route Component (`securityUtils/SecuredRoute.js`)
```javascript
import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

const SecuredRoute = ({ component: Component, security, ...otherProps }) => (
    <Route {...otherProps} render={props =>
        security.validToken === true ? (
            <Component {...props} />
        ) : (
            <Redirect to="/login" />
        )
    }/>
);

SecuredRoute.propTypes = {
    security: PropTypes.object.isRequired
};

const mapStateToProps = state => ({
    security: state.security
});

export default connect(mapStateToProps)(SecuredRoute);
```

### Backend Setup (Spring Boot)

#### 1. JWT Token Provider
```java
@Component
public class JWTTokenProvider {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    public String generateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setSubject(Long.toString(user.getId()))
            .claim("username", user.getUsername())
            .claim("roles", user.getRoles())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 2. JWT Authentication Filter
```java
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String jwt = getJWTFromRequest(request);
        if(StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Long userId = tokenProvider.getUserIdFromJWT(jwt);
            User user = userService.loadUserById(userId);
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
```

---

## Routing Structure

### App.js - Route Configuration
```javascript
import { Route, BrowserRouter, Switch } from 'react-router-dom';
import SecuredRoute from './securityUtils/SecuredRoute';

class App extends Component {
  render() {
    return (
      <Provider store={store}>  
        <BrowserRouter history={history}>
          <div className="App">
            {/* Public Route */}
            <Route exact path="/login" component={Landing} />     
            
            <Switch>
              {/* Protected Routes */}
              <SecuredRoute exact path="/" component={Home} />
              <SecuredRoute exact path="/profile" component={Profile} />
              <SecuredRoute exact path="/user" component={Users} />
              
              {/* Custom Components */}
              <SecuredRoute exact path="/customPage" component={CustomPage} />
              
              {/* Entity Routes with ID parameter */}
              <SecuredRoute exact path="/cuttingPlan/new" component={CuttingPlan} />
              <SecuredRoute exact path="/cuttingPlan/:entityId?" component={CuttingPlan} />
              
              {/* Generic Entity Route - MUST BE LAST */}
              <SecuredRoute exact path="/:entity/:entityId?" component={EntityList} />
              
              {/* 404 Route */}
              <SecuredRoute path="*" component={NoMatchRoute} />
            </Switch>
          </div> 
        </BrowserRouter>
      </Provider>
    )
  }
}
```

### Dashboard.js - Menu Structure
```javascript
class Dashboard extends Component {
  renderMenuSection = (condition, menuKey, icon, title, children, itemCount) => {
    if (!condition) return null;
    const isExpanded = this.state.menuElem === menuKey;
    
    return (
      <div className='dashboard-section'>
        <div className='dashboard-elem' 
             onClick={() => this.setMenuElem(menuKey)}
             style={isExpanded ? { color: "#ee3124" } : {}}>
          <FontAwesomeIcon icon={icon} />
          <span>{title}</span>
          <FontAwesomeIcon icon={isExpanded ? faAngleUp : faAngleDown} />
        </div>
        <ul className='dashboard-subelem-list' 
            style={isExpanded ? { maxHeight: 39 * itemCount } : { maxHeight: "0" }}>
          {children}
        </ul>
      </div>
    );
  }

  render() {
    const { user } = this.props.security;
    return (
      <div className='dashboard-container'>
        {/* Role-based menu sections */}
        {user.roles?.some(role => ["ROLE_ADMIN"].includes(role.authority)) && (
          <div className='dashboard-section'>
            <Link to="/entityName" className='dashboard-subelem-listelem'>
              <span>Entity Name</span>
            </Link>
          </div>
        )}
      </div>
    );
  }
}
```

---

## Generic Components

### EntityList - Generic Table Component

The `EntityList` component provides a reusable table with CRUD operations based on metadata configuration.

#### Usage
Navigate to `/entityName` to use EntityList with metadata definition.

#### Metadata Configuration (`metadata.js`)
```javascript
export const metadata = {
  entityName: {
    displayName: "Entity Display Name",
    operation: ["add", "edit", "delete", "search"], // Available operations
    firstOrderProperty: "name", // Default sort property
    fields: [
      { 
        name: "id", 
        displayName: "ID", 
        type: "hidden" 
      },
      { 
        name: "name", 
        displayName: "Name", 
        type: "text", 
        required: true 
      },
      { 
        name: "description", 
        displayName: "Description", 
        type: "text" 
      },
      { 
        name: "active", 
        displayName: "Active", 
        type: "boolean",
        defaultValue: true 
      },
      { 
        name: "createdAt", 
        displayName: "Created At", 
        type: "datetime",
        hideForm: true  // Hide in form, show in table
      },
      { 
        name: "relatedEntity", 
        displayName: "Related Entity", 
        type: "object",
        formDisplayProperty: "name"  // Property to display for objects
      },
    ],
    fieldsFilter: [/* Optional: different fields for filtering */],
  }
}
```

#### Field Types
| Type | Description |
|------|-------------|
| `text` | Standard text input |
| `number` | Numeric input |
| `boolean` | Switch/toggle |
| `date` | Date picker |
| `datetime` | Date and time picker |
| `select` | Dropdown with options |
| `object` | Related entity (dropdown) |
| `hidden` | Hidden field |
| `ROLE` | Special role selector |
| `password` | Password input |
| `textarea` | Multi-line text |

#### Field Options
| Option | Type | Description |
|--------|------|-------------|
| `required` | boolean | Field is required |
| `hideForm` | boolean | Hide in form (show in table only) |
| `notShowTable` | boolean | Hide in table (show in form only) |
| `defaultValue` | any | Default value for new entities |
| `formDisplayProperty` | string | Property name for object display |
| `options` | array | Options for select fields |

### EntityForm - Generic Form Component

Automatically generates forms based on metadata field definitions. Used internally by EntityList.

---

## Backend API Patterns

### Required Endpoints for EntityList

For each entity managed by `EntityList`, the following REST endpoints must be created:

#### 1. Repository (`repositories/EntityRepository.java`)
```java
@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {
    // Custom queries if needed
    List<Entity> findByNameContaining(String name);
}
```

#### 2. Service (`services/EntityService.java`)
```java
@Service
public class EntityService {
    
    @Autowired
    private EntityRepository entityRepository;
    
    public List<Entity> findAll() {
        return entityRepository.findAll();
    }
    
    public Page<Entity> findAll(Pageable pageable) {
        return entityRepository.findAll(pageable);
    }
    
    public Entity findById(Long id) {
        return entityRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Entity not found"));
    }
    
    public Entity save(Entity entity) {
        return entityRepository.save(entity);
    }
    
    public void deleteById(Long id) {
        entityRepository.deleteById(id);
    }
}
```

#### 3. Controller (`controller/EntityController.java`)
```java
@RestController
@RequestMapping("/api/entity")
public class EntityController {
    
    @Autowired
    private EntityService entityService;
    
    // GET /api/entity/list - Get all entities
    @GetMapping("/list")
    public List<Entity> list() {
        return entityService.findAll();
    }
    
    // GET /api/entity/all - Paginated list with filtering and sorting
    @GetMapping("/all")
    public Page<Entity> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam Map<String, String> filters) {
        
        Pageable pageable = PageRequest.of(page, size, 
            dir.equals("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());
        
        // Apply filters from Map (equal.fieldName, like.fieldName, etc.)
        return entityService.findAll(pageable, filters);
    }
    
    // GET /api/entity/{id} - Get entity by ID
    @GetMapping("/{id}")
    public Entity findById(@PathVariable Long id) {
        return entityService.findById(id);
    }
    
    // POST /api/entity - Create new entity
    @PostMapping
    public Entity create(@RequestBody Entity entity) {
        return entityService.save(entity);
    }
    
    // PUT /api/entity/{id} - Update entity
    @PutMapping("/{id}")
    public Entity update(@PathVariable Long id, @RequestBody Entity entity) {
        entity.setId(id);
        return entityService.save(entity);
    }
    
    // DELETE /api/entity/{id} - Delete entity
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        entityService.deleteById(id);
    }
}
```

### Filter Parameters Convention
The `/all` endpoint supports these filter prefixes:
- `equal.fieldName=value` - Exact match
- `like.fieldName=value` - Contains match
- `gt.fieldName=value` - Greater than
- `lt.fieldName=value` - Less than
- `gte.fieldName=value` - Greater than or equal
- `lte.fieldName=value` - Less than or equal

---

## Role Management

### User Entity with Roles
```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String password;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
```

### Role Entity
```java
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String authority; // e.g., "ROLE_ADMIN", "ROLE_USER"
    private String description;
}
```

### Common Roles
| Role | Description |
|------|-------------|
| `ROLE_ADMIN` | Full system access, manages users and roles |
| `ROLE_USER` | Standard user access |
| `ROLE_IMPORTER` | Can import cutting plans |
| `ROLE_CAD` | CAD department access |
| `ROLE_QUALITE` | Quality department access |
| `ROLE_PROCESS` | Process department access |
| `ROLE_MAINTENANCE` | Maintenance department access |
| `ROLE_INDICATEUR` | KPI/Reports access |

### Frontend Role Check
```javascript
const { user } = this.props.security;

// Single role check
if (user.roles.map(e => e.authority).includes("ROLE_ADMIN")) {
  // Admin-only content
}

// Multiple roles check
if (user.roles.some(role => ["ROLE_ADMIN", "ROLE_CAD"].includes(role.authority))) {
  // Admin or CAD content
}
```

### Backend Role Check
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin-only")
public ResponseEntity<?> adminEndpoint() {
    // Only accessible by ROLE_ADMIN
}

@PreAuthorize("hasAnyRole('ADMIN', 'CAD')")
@GetMapping("/admin-or-cad")
public ResponseEntity<?> adminOrCadEndpoint() {
    // Accessible by ROLE_ADMIN or ROLE_CAD
}
```

---

## Multi-Database Configuration

The application connects to multiple SQL Server databases using Spring's multiple datasource configuration.

### Datasource Configuration

```java
// Primary datasource (MG_CMS)
@Configuration
public class PersistenceConfiguration {
    @Primary
    @Bean(name = "mainDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }
}

// CTC datasource
@Configuration
public class PersistenceCTCConfiguration {
    @Bean(name = "ctcDataSource")
    @ConfigurationProperties(prefix = "ctc.datasource")
    public DataSource ctcDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

### Database Connections
| Datasource | Database | Purpose |
|------------|----------|---------|
| spring.datasource | MG_CMS | Main application data |
| ctc.datasource | plt_viewer | PLT viewer integration |
| cms.datasource | qualite | Quality management |
| pls.datasource | MG_PLS_NEW | PLS system |
| splice.datasource | splice | Splice management |
| learpokayoke.datasource | LearPokaYoke | Poka-yoke system |
| ims.datasource | IMS_NEWAPP | IMS integration |

---

## Quick Start: Adding a New Entity

1. **Create Domain Class** (`domain/NewEntity.java`)
2. **Create Repository** (`repositories/NewEntityRepository.java`)
3. **Create Service** (`services/NewEntityService.java`)
4. **Create Controller** (`controller/NewEntityController.java`)
5. **Add Metadata** (`metadata.js`)
6. **Add to Dashboard Menu** (`Dashboard.js`)
7. (Optional) **Add Custom Route** (`App.js`)

---

## New Entities & Features (Recent Updates)

### PartNumberWeight Entity
**Purpose**: Store unit weight for each part number to enable accurate box weight estimation.

#### Entity Structure
```java
@Entity
@Table(name = "PartNumberWeight")
public class PartNumberWeight {
    private Long id;
    private String partnumber;      // Part number identifier
    private Double weightUnit;      // Unit weight in kg/grams
}
```

#### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/partNumberWeight/list` | Get all part number weights |
| GET | `/api/partNumberWeight/all` | Paginated list with sorting |
| GET | `/api/partNumberWeight/{id}` | Get by ID |
| GET | `/api/partNumberWeight/byPartnumber/{partnumber}` | Get by part number |
| POST | `/api/partNumberWeight` | Create new entry |
| PUT | `/api/partNumberWeight/{id}` | Update entry |
| DELETE | `/api/partNumberWeight/{id}` | Delete entry |
| POST | `/api/partNumberWeight/import` | Import from Excel file |

#### Excel Import
The import endpoint accepts Excel files with:
- Column 0: Part Number (string)
- Column 1: Weight Unit (numeric)

Returns import summary with success/error counts.

---

### BoxTypeConfig Entity
**Purpose**: Configure empty box weights for different box types used in weight verification.

#### Entity Structure
```java
@Entity
@Table(name = "BoxTypeConfig")
public class BoxTypeConfig {
    private Long id;
    private String boxType;           // Box type identifier (e.g., "gray", "black")
    private Double emptyBoxWeight;    // Weight of empty box in kg
}
```

#### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/boxTypeConfig/list` | Get all box type configurations |
| GET | `/api/boxTypeConfig/{id}` | Get by ID |
| GET | `/api/boxTypeConfig/byBoxType/{boxType}` | Get by box type |
| POST | `/api/boxTypeConfig` | Create new configuration |
| PUT | `/api/boxTypeConfig/{id}` | Update configuration |
| DELETE | `/api/boxTypeConfig/{id}` | Delete configuration |

---

### BoxWeight Entity Updates
**New Fields**: Added support for weight estimation and quantity tracking.

#### Updated Fields
```java
@Entity
@Table(name = "BoxWeight")
public class BoxWeight {
    // ... existing fields ...
    private Integer quantity;          // Number of pieces in box
    private Double estimatedWeight;    // Calculated estimated weight
}
```

#### Weight Estimation Logic
```
estimatedWeight = (quantity × partNumberUnitWeight) + emptyBoxWeight
```

Where:
- `quantity`: Number of pieces in the box
- `partNumberUnitWeight`: From PartNumberWeight table
- `emptyBoxWeight`: From BoxTypeConfig table

#### Updated Workflow
1. User enters box details (type, ID, quantity)
2. System calculates estimated weight using PartNumberWeight and BoxTypeConfig
3. System displays estimated vs. actual weight for comparison
4. Optional validation: Compare estimated with actual sent weight

---

### MachineLog Security Enhancement
**Path**: `/api/machineLog/folder/{folderName}`

#### Security Improvements
Added path traversal protection:

```java
private boolean isPathSafe(String folderName, String fileName) {
    if (folderName != null && (folderName.contains("..") || 
        folderName.contains("/") || folderName.contains("\\"))) {
        return false;
    }
    return true;
}

private boolean isPathWithinBase(File file, File baseDir) {
    Path filePath = file.getCanonicalFile().toPath();
    Path basePath = baseDir.getCanonicalFile().toPath();
    return filePath.startsWith(basePath);
}
```

**Security Features**:
- Validates folder and file names for path traversal attempts
- Checks canonical paths to prevent directory traversal
- Returns 403 Forbidden for suspicious paths
- Restricted to authorized roles (ADMIN, PROCESS, MAINTENANCE, INDICATEUR)

---

### Quality Notice Validation Enhancement
**Updated**: Placement name validation now supports both `-0BF` and `-MBF` suffixes.

#### Validation Rule (Verification 4)
For materials with `validated0BF = true`:
- Placement name must contain either `-0BF` OR `-MBF` suffix
- Previously only checked for `-0BF`
- Applies to Lectra machine type only

#### Implementation (CuttingPlanForm.js)
```javascript
if (configObj.validated0BF === true && cpmp.machine === "Lectra"
    && !cpmp.placement.includes("-0BF") && !cpmp.placement.includes("-MBF")) {
    error.push(cpmp.placement + ": validated 0BF: placement name must contain -0BF or -MBF")
}
```

---

### CuttingPlanForm getMarge Enhancement
**Updated**: Now uses `ReftissuMargin` configuration for dynamic margin calculation.

#### Margin Calculation Logic
The `getMarge` function determines the appropriate margin based on:
- **longueur**: Length of the placement
- **nbrCouche**: Number of layers
- **partNumberMaterial**: Material reference

#### How It Works
```javascript
getMarge(longueur, nbrCouche, partNumberMaterial) {
    const config = this.state.partNumberMaterialConfigs[partNumberMaterial];
    if (!config || !config.reftissuMargins) return 0;
    
    // Find margin interval matching the length
    const marginConfig = config.reftissuMargins.find(rm => 
        longueur >= rm.minLength && longueur <= rm.maxLength
    );
    
    if (!marginConfig) return 0;
    
    // Parse pliesConfig: "10;margin1|20;margin2|50;margin3"
    // Find appropriate margin based on nbrCouche
    const pliesConfigs = marginConfig.pliesConfig.split('|');
    for (let pc of pliesConfigs) {
        const [maxLayers, margin] = pc.split(';');
        if (nbrCouche <= parseInt(maxLayers)) {
            return parseFloat(margin);
        }
    }
    
    return 0;
}
```

#### Example Configuration
```
reftissuMargins = [
    {
        minLength: 0,
        maxLength: 1000,
        pliesConfig: "10;50|20;75|50;100"
    },
    {
        minLength: 1001,
        maxLength: 2000,
        pliesConfig: "10;75|20;100|50;150"
    }
]
```

For length=800, layers=15:
- Matches first interval (0-1000)
- pliesConfig: "10;50|20;75|50;100"
- 15 layers > 10, ≤ 20 → margin = 75

---

## Updates (February 2026)

### New Routes Added
| Route | Component | Description |
|-------|-----------|-------------|
| `/kpiChargeMachine` | KpiChargeMachine | Stacked bar chart KPI for machine load |
| `/projetPLS` | EntityList | PLS project management via metadata |

### New Metadata Entities
| Entity | Fields | Operations |
|--------|--------|------------|
| `projetPLS` | id, nom | Add, Edit, Delete |

### Metadata Updates
- `plsDemande.projet` changed from `type: "text"` to `type: "object", formObject: "projetPLS", formDisplayProperty: "nom"` for relational dropdown

### New Java Entities
| Entity | Table | Purpose |
|--------|-------|---------|
| `ReftissuMachineData` | ReftissuMachine | Read-only data entity |
| `ReftissuCategoryData` | ReftissuCategory | Read-only data entity |
| `ReftissuMarginData` | ReftissuMargin | Read-only data entity |
| `PartNumberValidatedWeight` | PartNumberValidatedWeight | Process-validated weight per PN |

### New Controllers
| Controller | Path | Access |
|------------|------|--------|
| `PartNumberValidatedWeightController` | `/api/partNumberValidatedWeight` | ROLE_PROCESS |

### New Endpoints on Existing Controllers
| Controller | Endpoint | Description |
|------------|----------|-------------|
| `GammeTechniqueImprimerController` | `GET /serie/{serie}` | Lookup gamme by série number |

### Modified Query Logic
- `CuttingRequestSerieInfoRepository`: Aggregated cutting time queries now multiply `tempsDeCoupe × COALESCE(nbrCouche, 1)` for accurate LASER-DXF time estimation
- `PlanDeChargeService`: nbrCouche extracted and applied in detailed series calculations

---

## File Structure
```
src/
├── main/
│   ├── java/com/lear/MGCMS/
│   │   ├── domain/           # Entity classes
│   │   ├── repositories/     # JPA repositories
│   │   ├── services/         # Business logic
│   │   ├── controller/       # REST controllers
│   │   └── security/         # JWT & security config
│   ├── js/
│   │   ├── App.js            # Routes configuration
│   │   ├── metadata.js       # Entity metadata
│   │   ├── store.js          # Redux store
│   │   ├── actions/          # Redux actions
│   │   ├── reducers/         # Redux reducers
│   │   ├── securityUtils/    # JWT utilities
│   │   └── components/
│   │       ├── EntityList.js # Generic table
│   │       ├── EntityForm.js # Generic form
│   │       ├── Dashboard.js  # Navigation menu
│   │       └── Layout/       # Page components
│   └── resources/
│       └── application.properties
```
