# 🚀 Safari Sphere Social Backend Engine

[![Deploy to Koyeb](https://www.koyeb.com/static/images/deploy/button.svg)](https://app.koyeb.com/deploy?type=git&repository=github.com/collinsmaster/safarisphere&branch=main&name=safarisphere&builder=docker&env[NODE_ENV]=production&env[PORT]=8080&env[DATABASE_URL]=postgres://safari_sphere_user:70yhVu2mfzuiAzWBO1DEi5Q8VgxXZ2CI@dpg-d8fh6bl8nd3s73fqioag-a.virginia-postgres.render.com/safari_sphere&env[JWT_SECRET]=supersecretjwtpasswordkey123&env[JWT_REFRESH_SECRET]=supersecretjwtrefreshpasswordkey123&env[GEMINI_API_KEY]=)

Safari Sphere is a fresh, futuristic, community-driven social networking platform designed for real-time engagement, creativity, visual discovery, and interactive gamification. This is the production-ready Node.js + Express + PostgreSQL + Socket.IO backend repository, optimized for high performance, server-side Google Gemini AI integration, and instant deployment on **Koyeb**.

### 🔗 Public Backend Live Link: `https://safarisphere.koyeb.app/`

---

## 🏗️ Architectural Overview
The system utilizes a modern, robust **Controller-Service-Repository** architectural separation:
1. **Express Server Router Layers**: Secure, versioned REST parameters (`/api/v1/`).
2. **PostgreSQL Schema Engine**: 25+ structured transactional tables with optimized indices for feed rankings and direct messages.
3. **Socket.IO Real-time Pipeline**: High-speed WS channels for campfire chat channels, member joins, and live typing triggers.
4. **Google Gemini service**: Direct integrations with the `gemini-3.5-flash` model for auto-moderation, caption generations, and automated profile summaries.

---

## 🚀 Instant Deployment on Koyeb

### ⚠️ IMPORTANT: Why the Buildpack compilation failed and how to prevent it
Because this repository holds both `/app` (Android code written in Kotlin/Gradle) and `/backend` (NodeJS Express code), Koyeb's default **Buildpack builder** auto-detection erroneously selects JVM & Gradle wrapper buildpacks instead of Node!

To deploy successfully, you **MUST configure Koyeb to use the Dockerfile Builder**. This tells Koyeb to immediately construct the Node.js Docker container using the optimized `/Dockerfile` configuration and bypass the Android Gradle building tasks entirely.

### Step 1: Push Your Code to GitHub
Create a fresh, public or private GitHub Repository and push the Safari Sphere code to it:
```bash
git init
git add .
git commit -m "Initialize Safari Sphere stack"
git remote add origin https://github.com/collinsmaster/safarisphere.git
git branch -M main
git push -u origin main
```

### Step 2: Use Your Live Render PostgreSQL Database
Your active PostgreSQL connection string is pre-configured in the deploy link:
`postgresql://safari_sphere_user:70yhVu2mfzuiAzWBO1DEi5Q8VgxXZ2CI@dpg-d8fh6bl8nd3s73fqioag-a.virginia-postgres.render.com/safari_sphere`

We also added a database bootstrapper utility. To initialize tables on your remote PostgreSQL, run local configuration or let the service run.

### Step 3: Deployment Procedure
1. Click the **Deploy to Koyeb** button at the top of this file.
2. Sign in or register your Koyeb account.
3. You will be redirected to the service configuration form where all empty environment variables are pre-loaded!
4. **Configure Builder Settings**:
   - 🌟 **Change Builder from Buildpack to Dockerfile** (Crucial! Do not use Buildpacks).
   - Set the **Root Directory** field to: `/` (or leave it empty/default so Koyeb uses the top-level Dockerfile, or set it to `backend` since we also added an optimized `/backend/Dockerfile` as fallback).
5. **Fill in the variables**:
   - `DATABASE_URL`: Preloaded with your active Render Postgres URL (`postgres://safari_sphere_user:70yhVu2mfzuiAzWBO1DEi5Q8VgxXZ2CI@dpg-d8fh6bl8nd3s73fqioag-a.virginia-postgres.render.com/safari_sphere`).
   - `JWT_SECRET` & `JWT_REFRESH_SECRET`: Secure session hash salts.
   - `GEMINI_API_KEY`: Input your custom Google AI Studio Key.
   - `PORT`: Set to `8080`.
6. Set the Exposed Port to **`8080`**.
7. Click **Deploy**. Koyeb will compile the project and assign your custom subdomain `safarisphere.koyeb.app`!

---

## 📡 Dynamic API Endpoint Catalogue

All endpoints are prefix versioned at `/api/v1`.

### 🔓 Authentication & Profiles
#### 1. Pioneer Sign Up (`POST /api/v1/auth/signup`)
Registers a new explorer profile with initial XP and standard options.
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"email":"explorer@domain.com","username":"neo_leo","password":"secret_password_1","displayName":"Neo Leo 🦁"}' \
  https://safari-sphere-xxx.koyeb.app/api/v1/auth/signup
```

#### 2. Clear Entrance Log In (`POST /api/v1/auth/login`)
Signs in and produces active Bearer Access Tokens + Rotate Refresh keys.
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"neo_leo","password":"secret_password_1"}' \
  https://safari-sphere-xxx.koyeb.app/api/v1/auth/login
```

#### 3. Retrieve Identity (`GET /api/v1/auth/profile`)
*Requires `Authorization: Bearer <Token>` Header.*

---

### 📝 Social Feed & Interactions
#### 1. Fetch Feed Updates (`GET /api/v1/posts`)
```bash
curl https://safari-sphere-xxx.koyeb.app/api/v1/posts?category=Adventurer
```

#### 2. Propagate Post with AI Moderation (`POST /api/v1/posts`)
*Protected by JWT bearer.* Passes content through Gemini Content Guard to detect threat layers prior to database entry.
```bash
curl -X POST -H "Authorization: Bearer <Token>" -H "Content-Type: application/json" \
  -d '{"content":"Beautiful morning watching cheetahs run at 100km/h! #WildLife","vibeCategory":"Adventurer"}' \
  https://safari-sphere-xxx.koyeb.app/api/v1/posts
```

#### 3. Toggle Like Stream (`POST /api/v1/posts/:id/like`)

---

### 🏕️ Live Vybe Rooms
Live group channels incorporating presence lists, live sound parameters, and Socket broadcasts.
- `GET /api/v1/rooms` : Lists active rooms on the globe.
- `POST /api/v1/rooms` : Creates a custom camp/thematic room channel.

---

### 📱 Moments (24-Hour Stories)
- `GET /api/v1/moments` : Lists live stories yet to expire.
- `POST /api/v1/moments` : Posts short caption visuals which self-destruct after 24 hours.

---

### 🤖 SphereMate AI Companion
#### 1. Companion Conversation (`POST /api/v1/ai/chat`)
Engage in rich futuristic discourse with your AI social buddy SphereMate, powered by real-time Gemini processing.
```bash
curl -X POST -H "Authorization: Bearer <Token>" -H "Content-Type: application/json" \
  -d '{"message":"Recommend me some community topics about chill electronic synthesizers"}' \
  https://safari-sphere-xxx.koyeb.app/api/v1/ai/chat
```

#### 2. Smart Post Suggest Caption (`POST /api/v1/ai/suggest-caption`)
```bash
curl -X POST -H "Authorization: Bearer <Token>" -H "Content-Type: application/json" \
  -d '{"mood":"Chilled","topic":"Sipping campfire tea"}' \
  https://safari-sphere-xxx.koyeb.app/api/v1/ai/suggest-caption
```

#### 3. Compile Vibe Summary (`GET /api/v1/ai/vibe-summary`)

---

## 🛠️ Local Development Quickstart

1. Clone or download your repository:
   ```bash
   cd baceknd
   npm install
   ```
2. Configure your local `.env` variables from `.env.example`.
3. To bootstrap tables on your local PostgreSQL instace:
   ```bash
   # Make sure DATABASE_URL is set in .env
   npm run db:init
   ```
4. Fire up the development server with Hot-reloads:
   ```bash
   npm run dev
   ```
