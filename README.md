# 🎮 Music Rhythm Match

> **Find your music soulmates. Unmask your nemeses.**

A full-stack web application that analyzes your Spotify listening habits and mathematically compares your musical DNA against other users to find your top 5 soulmates (most similar listeners) and top 5 nemeses (most opposite listeners) — all wrapped in a retro 8-bit arcade aesthetic.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue?style=flat-square&logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)

---

## 📸 Preview

```
╔══════════════════════════════════════════╗
║          MUSIC RHYTHM MATCH              ║
║                                          ║
║  ⚡ ENERGY        ████████░░  82%        ║
║  💃 DANCEABILITY  ███████░░░  74%        ║
║  😊 VIBE          ██████░░░░  65%        ║
║  🎸 ACOUSTIC      ██░░░░░░░░  12%        ║
║                                          ║
║  💚 SOULMATE #1 — john_doe  94% MATCH    ║
║  💀 NEMESIS  #1 — jane_x    91% DIFF     ║
╚══════════════════════════════════════════╝
```

---

## ✨ Features

- **Spotify OAuth2 Login** — no passwords, no sign-up forms, just Spotify
- **Audio Feature Analysis** — energy, danceability, valence, and acousticness averaged across your top 50 tracks
- **Genre DNA Fingerprinting** — Jaccard similarity on your top genres extracted from your top artists
- **DSA Matching Engine** — in-memory Cosine Similarity + Jaccard Similarity algorithm running entirely in Java
- **Top 5 Soulmates** — users whose taste vector is closest to yours
- **Top 5 Nemeses** — users whose taste vector is furthest from yours
- **Delete My Data** — one-click permanent deletion from the database (Spotify ToS compliant)
- **Retro 8-bit UI** — NES.css + Press Start 2P font, CRT scanline overlay, pixel avatars

---

## 🧠 How the Matching Algorithm Works

Each user is reduced to a **4-dimensional taste vector**:

```
User Vector = [avgEnergy, avgDanceability, avgValence, avgAcousticness]
Example:      [0.82,      0.74,           0.65,        0.12]
```

All values are floats between `0.0` and `1.0` sourced from Spotify's audio-features API.

### Step 1 — Cosine Similarity (audio features, 60% weight)

Measures the angle between two vectors in 4D feature space:

```
cos(A, B) = (A · B) / (|A| × |B|)
```

Normalized from `[-1, 1]` → `[0, 1]`:  `normalizedCosine = (cosine + 1.0) / 2.0`

### Step 2 — Jaccard Similarity (genres, 40% weight)

Measures genre set overlap between two users:

```
J(A, B) = |A ∩ B| / |A ∪ B|
```

### Step 3 — Combined Score

```
finalScore = (0.6 × normalizedCosine) + (0.4 × jaccard)
```

### Step 4 — Sort & Select

- **Soulmates** → sort by `finalScore` DESCENDING → top 5
- **Nemeses** → sort by `finalScore` ASCENDING → bottom 5

**Time complexity: O(n)** per query, running entirely in-memory in Java — no database extensions needed.

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.0.6 |
| Frontend | Thymeleaf, NES.css, Press Start 2P |
| Auth | Spotify OAuth2 (Spring Security) |
| HTTP Client | Spring WebFlux (WebClient) |
| Database | PostgreSQL via Supabase |
| ORM | Spring Data JPA / Hibernate |
| Deployment | Render / Koyeb (Docker) |

---

## 📁 Project Structure

```
src/main/
├── java/com/musicrhythmatch/
│   ├── MusicRhythmMatchApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java               # OAuth2 security rules
│   │   ├── OAuth2LoginSuccessHandler.java     # Fires on login, fetches Spotify data
│   │   └── WebClientConfig.java              # Configured WebClient bean
│   ├── controller/
│   │   ├── HomeController.java               # Landing page
│   │   ├── DashboardController.java          # Main dashboard + match results
│   │   └── ProfileController.java            # Profile view + data deletion
│   ├── dto/
│   │   └── MatchResult.java                  # Wraps User + similarity score
│   ├── entity/
│   │   └── User.java                         # JPA entity (taste vector)
│   ├── repository/
│   │   └── UserRepository.java               # Spring Data JPA repository
│   └── service/
│       ├── SpotifyApiService.java            # All Spotify API calls
│       ├── UserService.java                  # DB upsert logic
│       └── MatchingEngineService.java        # Cosine + Jaccard DSA engine
└── resources/
    ├── application.properties                # All config via env variables
    ├── static/css/
    │   └── custom.css                        # Retro 8-bit styles
    └── templates/
        ├── index.html                        # Landing page
        ├── dashboard.html                    # Soulmates + Nemeses display
        └── profile.html                      # User profile + delete account
```

---

## 🚀 Local Setup

### Prerequisites

- Java 21
- Maven 3.9+
- A [Spotify Developer App](https://developer.spotify.com/dashboard)
- A [Supabase](https://supabase.com) project (free tier works)

### Step 1 — Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/music-rhythm-match.git
cd music-rhythm-match
```

### Step 2 — Create Your Spotify App

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Click **Create App**
3. Set **Redirect URI** to: `http://localhost:8080/login/oauth2/code/spotify`
4. Copy your **Client ID** and **Client Secret**

### Step 3 — Set Up Supabase

1. Create a new project at [supabase.com](https://supabase.com)
2. Go to **Settings → Database → URI** and copy the connection string
3. Change `postgresql://` → `jdbc:postgresql://` and append `?sslmode=require`
4. Run this SQL in the Supabase **SQL Editor**:

```sql
CREATE TABLE IF NOT EXISTS users (
    spotify_id        VARCHAR(255)     PRIMARY KEY,
    display_name      VARCHAR(255),
    profile_image_url TEXT,
    top_genres        TEXT,
    avg_energy        DOUBLE PRECISION DEFAULT 0.5,
    avg_danceability  DOUBLE PRECISION DEFAULT 0.5,
    avg_valence       DOUBLE PRECISION DEFAULT 0.5,
    avg_acousticness  DOUBLE PRECISION DEFAULT 0.5,
    last_login        TIMESTAMPTZ      DEFAULT NOW()
);
```

### Step 4 — Configure Environment Variables

Create a `.env` file in the project root (same level as `pom.xml`):

```env
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
SUPABASE_DB_URL=jdbc:postgresql://db.yourref.supabase.co:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=your_supabase_password
```

> The `spring-dotenv` dependency automatically loads this file on startup — no extra config needed.

### Step 5 — Run

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080** in your browser.

---

## 🌐 Deployment

### Deploy on Render

1. Push your code to GitHub
2. Go to [render.com](https://render.com) → **New → Web Service**
3. Connect your GitHub repo
4. Set **Runtime** to **Docker** (auto-detects your `Dockerfile`)
5. Add all 5 environment variables under the **Environment** tab
6. Add your Render URL as a Spotify redirect URI:
   ```
   https://your-app.onrender.com/login/oauth2/code/spotify
   ```
7. Click **Deploy**

### Deploy on Koyeb (faster cold starts)

1. Go to [koyeb.com](https://koyeb.com) → **New App**
2. Select **GitHub** → choose your repo
3. Set **Builder** to **Dockerfile**
4. Add all 5 environment variables
5. Add your Koyeb URL as a Spotify redirect URI
6. Deploy

> **Note:** Render's free tier spins down after 15 minutes of inactivity (~30s cold start). Koyeb's free tier has faster cold starts and is recommended for a smoother experience.

---

## 🔑 Environment Variables Reference

| Variable | Description |
|---|---|
| `SPOTIFY_CLIENT_ID` | From your Spotify Developer App settings |
| `SPOTIFY_CLIENT_SECRET` | From your Spotify Developer App settings |
| `SUPABASE_DB_URL` | `jdbc:postgresql://db.ref.supabase.co:5432/postgres?sslmode=require` |
| `SUPABASE_DB_USER` | Usually `postgres` |
| `SUPABASE_DB_PASSWORD` | Your Supabase database password |

---

## ⚖️ Legal & Spotify ToS Compliance

- **Non-commercial** — no ads, no paywalls, no monetization of any kind
- **Brand separation** — UI is entirely distinct from Spotify's design language
- **Data transparency** — users can permanently delete all their stored data at any time via the Profile page
- **Minimal scopes** — only `user-top-read` and `user-read-private` are requested

---

## 🤝 Contributing

Pull requests are welcome. For major changes, open an issue first to discuss what you'd like to change.

---

## 📄 License

[MIT](LICENSE)

---

<p align="center">Built with ☕ Java, 🎵 Spotify API, and way too much pixel energy</p>
