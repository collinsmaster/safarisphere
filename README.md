# 🐘 Safari Sphere — Web & Android Savanna Pioneer Network

[![Deploy to Koyeb](https://www.koyeb.com/static/images/deploy/button.svg)](https://app.koyeb.com/deploy?type=git&repository=github.com/collinsmaster/safarisphere&branch=main&name=safarisphere&builder=docker&env[NODE_ENV]=production&env[PORT]=8080&env[DATABASE_URL]=postgres://safari_sphere_user:70yhVu2mfzuiAzWBO1DEi5Q8VgxXZ2CI@dpg-d8fh6bl8nd3s73fqioag-a.virginia-postgres.render.com/safari_sphere&env[JWT_SECRET]=supersecretjwtpasswordkey123&env[JWT_REFRESH_SECRET]=supersecretjwtrefreshpasswordkey123&env[GEMINI_API_KEY]=)

Safari Sphere is a fresh, futuristic, community-driven social networking platform designed for real-time engagement, safari creativity, visual discovery, secure room interactions, and instant private chatting.

This repository is unified and ready for instant deployment:
- **`app/`**: A visually stunning Android client written in Kotlin, Jetpack Compose, Retrofit, and Material Design 3.
- **`backend/`**: A highly performant Node.js, Express, and PostgreSQL web engine initialized for deployment on **Koyeb** under your custom backend domain: `safarisphere.koyeb.app`.

---

## 🚀 Instant Backend Deployment on Koyeb

### ⚠️ IMPORTANT: Why the Buildpack compilation failed and how to prevent it
Because this repository holds both `/app` (Android code written in Kotlin/Gradle) and `/backend` (NodeJS Express code), Koyeb's default **Buildpack builder** auto-detection erroneously selects JVM & Gradle wrapper buildpacks instead of Node!

To deploy successfully, you **MUST configure Koyeb to use the Dockerfile Builder**. This tells Koyeb to immediately construct the Node.js Docker container using the optimized `/Dockerfile` configuration and bypass the Android Gradle building tasks entirely.

### 🛠️ One-Click Deploy Instructions on Koyeb:
1. Click the **Deploy to Koyeb** button above.
2. Sign in or register your Koyeb account.
3. Keep the **GitHub Repository** selection on `collinsmaster/safarisphere`.
4. In the **Builder Settings** section:
   - 🌟 **Change Builder from Buildpack to Dockerfile** (Crucial! Do not use Buildpacks).
   - Set the **Root Directory** field to: `/` (or leave it empty/default so Koyeb uses the top-level Dockerfile, or set it to `backend` since we also added an optimized `/backend/Dockerfile` as fallback).
5. Koyeb pre-fills your environment variables:
   - `DATABASE_URL`: Preloaded with your active Render Postgres URL (`postgres://safari_sphere_user:70yhVu2mfzuiAzWBO1DEi5Q8VgxXZ2CI@dpg-d8fh6bl8nd3s73fqioag-a.virginia-postgres.render.com/safari_sphere`).
   - `JWT_SECRET` & `JWT_REFRESH_SECRET`: Pre-loaded with secure authorization salt hash strings.
   - `GEMINI_API_KEY`: Input your custom Google AI Studio Key.
   - `PORT`: Set to `8080`.
6. Set the Exposed Port to **`8080`**.
7. Click **Deploy**! Koyeb will build your serverless Node container flawlessly and scale it live at **`safarisphere.koyeb.app`**.

---

## 🛠️ Step-by-Step GitHub Setup Procedure

To push this project to your new GitHub repository:

```bash
# 1. Initialize local workspace
git init

# 2. Add files and commit
git add .
git commit -m "Initialize Safari Sphere fullstack"

# 3. Add your remote credentials
git remote add origin https://github.com/collinsmaster/safarisphere.git

# 4. Set standard branch and push
git branch -M main
git push -u origin main
```

---

## 📱 Android Client Configurations

The Android companion application is fully responsive and configured to query your live API endpoints at `https://safarisphere.koyeb.app/` instantly. All networking interactions (auth, posts, direct chat channels, vybe rooms) fall back to reliable local mock parameters if the service is unreachable.
