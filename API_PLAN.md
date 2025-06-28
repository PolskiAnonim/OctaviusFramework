# API Integration Plan

## Overview
Adding embedded REST API to the Kotlin Compose Multiplatform desktop application for external integrations and browser extension support.

## Phase 1: Basic API Infrastructure

### Settings Screen
- New "Settings" tab in main navigation (alongside Novel/Game tabs)
- API Configuration section:
  - Enable API Server [toggle]
  - Port configuration [default: 8080]
  - Server status indicator (Running/Stopped)

### Embedded Ktor Server
- **Technology**: Ktor (Kotlin-native, lightweight)
- **Architecture**: Embedded server within main application
- **Lifecycle**: Starts/stops with application, controlled by settings toggle
- **Binding**: localhost-only (127.0.0.1) for security
- **CORS**: Allow `chrome-extension://*` origins for browser extension

### Basic Endpoints (Phase 1)
```
GET /api/status - Server health check
GET /api/games - List games with basic filtering
GET /api/asian-media - List publications with basic filtering
GET /api/check?title=... - Check if item exists in database
```

## Phase 2: Browser Extension Integration

### Extension Capabilities
- Content scripts on game/manga websites
- Quick "Add to collection" functionality
- Check if item already exists before adding
- Navigate to main application with pre-filled forms

### API Extensions
```
POST /api/quick-add - Simple item addition
POST /api/navigate - Open main app with specific form/data
GET /api/search?query=... - Search suggestions for extension
```

### Authentication (Future)
- Generated API keys in settings
- Extension configuration for API key
- Header-based authentication (`X-API-Key`)

## Phase 3: External API Integrations (Future)

### IGDB Integration
- Game metadata fetching
- New form control: `SearchApiControl` for IGDB lookup
- Automatic cover art and description population

### Steam API Integration
- Library import functionality
- Ignored games management
- Play time synchronization

## Technical Integration Points

### Reuse Existing Architecture
- **FormDataManager**: Reuse validation and database logic
- **DatabaseManager**: Direct database access for API endpoints
- **Domain Models**: Same data structures for API responses
- **Validators**: Reuse form validation in API endpoints

### Database Integration
- Same PostgreSQL database and schemas
- Reuse existing `DatabaseFetcher` for complex queries
- Utilize `TypeRegistry` for proper type handling
- Maintain transaction consistency with desktop app

### Configuration
- API settings stored in same `.env` file
- New environment variables:
  - `API_ENABLED=false`
  - `API_PORT=8080`
  - `API_KEY=<generated>`

## Implementation Priority
1. **Settings screen with API toggle**
2. **Basic Ktor server with /api/status endpoint**
3. **CORS configuration for browser extensions**
4. **Basic CRUD endpoints for games and asian media**
5. **Browser extension development**
6. **API key authentication**
7. **External API integrations**

## Browser Extension Flow
1. User visits game/manga website
2. Extension detects page content
3. Extension calls `/api/check` to see if item exists
4. If not exists: show "Add to collection" button
5. On click: either quick-add or open main app with form
6. Extension provides feedback on success/failure

## Security Considerations
- Localhost-only binding (127.0.0.1)
- CORS restricted to browser extension origins
- Future: API key authentication
- Rate limiting for API endpoints
- Input validation using existing form validators

## File Structure (Proposed)
```
composeApp/src/desktopMain/kotlin/org/octavius/
├── api/
│   ├── ApiServer.kt
│   ├── routes/
│   │   ├── GameRoutes.kt
│   │   ├── AsianMediaRoutes.kt
│   │   └── StatusRoutes.kt
│   └── models/
│       └── ApiModels.kt
├── settings/
│   ├── SettingsScreen.kt
│   ├── SettingsHandler.kt
│   └── ApiConfigurationSection.kt
└── config/
    └── ApiConfig.kt (extend EnvConfig)
```

This plan allows for incremental development while maintaining integration with existing application architecture and preparing for future browser extension development.