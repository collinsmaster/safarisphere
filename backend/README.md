# 🚀 Safari Sphere Social Backend Engine

[![Deploy to Koyeb](https://www.koyeb.com/static/images/deploy/button.svg)](https://app.koyeb.com/deploy?type=git&repository=github.com/your-github-username/safari-sphere&branch=main&name=safari-sphere&env[NODE_ENV]=production&env[PORT]=8080&env[DATABASE_URL]=&env[JWT_SECRET]=&env[JWT_REFRESH_SECRET]=&env[GEMINI_API_KEY]=)

Safari Sphere is a fresh, futuristic, community-driven social networking platform designed for real-time engagement, creativity, visual discovery, and interactive gamification. This is the production-ready Node.js + Express + PostgreSQL + Socket.IO backend repository, optimized for high performance, server-side Google Gemini AI integration, and instant deployment on **Koyeb**.

---

## 🏗️ Architectural Overview
The system utilizes a modern, robust **Controller-Service-Repository** architectural separation:
1. **Express Server Router Layers**: Secure, versioned REST parameters (`/api/v1/`).
2. **PostgreSQL Schema Engine**: 25+ structured transactional tables with optimized indices for feed rankings and direct messages.
3. **Socket.IO Real-time Pipeline**: High-speed WS channels for campfire chat channels, member joins, and live typing triggers.
4. **Google Gemini service**: Direct integrations with the `gemini-3.5-flash` model for auto-moderation, caption generations, and automated profile summaries.

---

## 🚀 Instant Deployment on Koyeb

Koyeb is a developer-friendly serverless app platform. You can deploy Safari Sphere in minutes from your GitHub fork.

### Step 1: Push Your Code to GitHub
Create a fresh, private or public GitHub Repository and push the Safari Sphere code to it:
```bash
git init
git add .
git commit -m "Initialize Safari Sphere stack"
git remote add origin https://github.com/your-github-username/safari-sphere.git
git branch -M main
git push -u origin main
```

### Step 2: Provision a PostgreSQL Database on Koyeb
1. Sign in to your **Koyeb Console** (https://app.koyeb.com/).
2. Click **Create** -> **Database**.
3. Choose **PostgreSQL**, pick your nearest region, configure the **Free Tier**, and click **Create**.
4. Koyeb will generate a secret `DATABASE_URL` for you. **Copy this string immediately!** (It looks like `postgres://user:password@hostname:5432/dbname?sslmode=require`).

### Step 3: Deploy the Backend Service
1. In your Koyeb console, click **Create** -> **Service**.
2. Select **GitHub** as the deployment provider.
3. Choose your `safari-sphere` repository.
4. In the **Builder and Run Settings**:
   - Set the **Root Directory** to `backend` (or leave as `/` if your repository only contains the backend code).
   - The platform will automatically identify the `Dockerfile` and build the container!
5. In **Environment Variables**, add the following required production keys:

| Key | Value Purpose / Example |
| :--- | :--- |
| `NODE_ENV` | Set to `production` |
| `PORT` | Set to `8080` (Koyeb automatically routes incoming traffic to this port) |
| `DATABASE_URL` | Enter the connection string copied from Step 2 |
| `JWT_SECRET` | A secure long cryptographic key for signups (e.g., `s9f8gjh4762jhdwq9031`) |
| `JWT_REFRESH_SECRET` | A distinct secure cryptographic key for token rotation |
| `GEMINI_API_KEY` | Your Google Gemini API Key from **AI Studio Secrets** (For SphereMate companion) |

6. Click **Deploy**! Koyeb will provision, compile, build your Docker environment, and provide you with a public endpoint link (e.g., `https://safari-sphere-xxx.koyeb.app`).

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
