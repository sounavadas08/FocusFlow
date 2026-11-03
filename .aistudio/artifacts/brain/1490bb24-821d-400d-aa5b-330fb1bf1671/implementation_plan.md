# FocusFlow — Digital Detox & Anti-Addiction Engine

A battery-efficient, minimal OLED-themed Android application engineered to curb smartphone addiction through app-specific usage limits, sub-section blocking (Instagram Reels, YouTube Shorts) with a 60-second countdown lock, strict AI-driven distraction pings that challenge excessive scrolling, cross-device cloud synchronization, and distraction-free Pomodoro focus sessions.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following parameters were confirmed during the initial interview phase and have been locked into the system architecture:

- **Sub-Section Blocking Strategy**: When short-form algorithmic loops (Instagram Reels or YouTube Shorts) are detected, FocusFlow grants a strict **60-second cooldown timer** with real-time on-screen countdown and haptic pulsing before displaying a full-screen hard lock overlay.
- **Coaching Tone & Distraction Pings**: Strict and challenging tone with tangible **streak stakes**. AI distraction pings proactively confront scrolling habits, warning users of streak forfeiture and quantified hours lost to algorithms.
- **Visual Aesthetic & Efficiency**: True **Deep OLED Dark Mode** (`#000000` pitch black background) accented with vivid **Calm Teal** (`#00E5BC` / `#00BFA5`) and cyber-indigo highlights, maximizing battery conservation on OLED/AMOLED panels while maintaining high contrast.
- **Screen & URL/Section Detection Architecture**: Combines Android `AccessibilityService` (for real-time view-tree inspection of specific UI nodes such as Reels tabs and Shorts players) and `UsageStatsManager` (for aggregate app time limits and dashboard analytics).

---

## 1. Overview & Core Concept

### What It Does
FocusFlow actively monitors and intervenes in habitual phone usage. It allows users to set daily budgets for addicting apps (e.g. TikTok, Instagram, YouTube, X, Reddit) and specifically isolates toxic micro-surfaces (e.g., blocking only Reels while allowing direct messaging; blocking Shorts while allowing educational long-form tutorials). When limits or micro-surfaces are triggered, FocusFlow enforces a 60-second grace cooldown followed by a hard blocking shield. It integrates an AI Distraction Ping engine powered by Gemini, a Pomodoro focus timer with auto-DND scheduling, productivity scoring, milestone badges, and an Emergency Bypass with strict cooldowns.

### Target Audience & Persona
Students, professionals, creators, and mindful smartphone users who want the utility of communication and video platforms without falling into endless doomscrolling traps.

### Key Value
- **Surgical Blocking**: Users don't need to delete Instagram or YouTube entirely; they eliminate only the algorithmic rabbit holes (Reels/Shorts).
- **Hard Accountability**: Strict streak loss mechanisms prevent casual dismissing of timer alerts.
- **OLED Energy Conservation**: Pure black theme ensures background monitoring and timer displays consume minimal battery.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & Productivity Scoring (Home)**:
   - Live daily screen time dial, productivity score (0–100 calculated from focus vs. distraction ratio), and active app time budgets.
   - Quick toggle for "Focus Mode" / "Pomodoro Session" and active streak counter (e.g. "🔥 6-Day Clean Streak").
   - Quick status cards for Section Blockers (Reels Guard & Shorts Guard active indicators).

2. **App & Section Limit Manager**:
   - List of installed and target apps with daily minute quotas and progress rings.
   - Granular toggles: **"Block Instagram Reels"**, **"Block YouTube Shorts"**, **"Block Twitter/X For You"**.
   - 60-second cooldown alert preview and customize intervention sensitivity.

3. **In-App Intervention & 60-Second Cooldown Shield**:
   - When a user opens Instagram Reels or YouTube Shorts, a floating non-intrusive HUD displays: `"Reels Intercepted: 60s cooldown remaining..."` with a ticking countdown ring.
   - At `00:00`, a full-screen blocker appears: `"Session Terminated. Protect your 6-Day Streak. Return to your goals."`
   - Includes **"Emergency Bypass"** button (requires typing a mindful confirmation phrase and costs a streak deduction if abused).

4. **Focus Mode & Pomodoro Hub**:
   - Configurable work/break intervals (25/5 min or custom).
   - Auto-DND triggers to mute incoming notifications during active intervals.
   - Notification filtering log: holds low-priority social pings in a batch tray until the focus session concludes.

5. **AI Distraction Ping & Milestone Badges**:
   - Generates contextual, strict AI intervention pings based on current time of day and app usage patterns (e.g. *"You've opened Instagram 14 times today. At this rate, your 6-day streak dies before midnight. Put the phone down."*).
   - Badges screen showcasing unlocks: *"Zen Novice (3-day streak)"*, *"Shorts Slayer (Saved 5h from YouTube Shorts)"*, *"Deep Work Master"*.

6. **Cloud Sync & Cross-Device Profile**:
   - Google Sign-In with Firebase Firestore sync to preserve streaks, daily logs, and app rules across phones and tablets.

### Visual Identity & Theme
- **Background**: Absolute OLED Black (`#000000`), Dark Surface (`#0D1117`), Elevated Card Surface (`#161B22`).
- **Primary / Accent**: Electric Mint Teal (`#00E5BC`), Deep Teal (`#00BFA5`), Crisp Cyan (`#38E1FF`).
- **Stakes / Warning**: Alert Crimson (`#FF3B30`) for lockouts, streak warning amber (`#FF9500`).
- **Typography**: Clean sans-serif hierarchy with bold tabular figures for timers and clean monospaced analytics accents.
- **Motion & Micro-interactions**: Smooth radial progress arcs, countdown pulsing, spring-damped bottom sheets, and haptic feedback on timer triggers.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Android Accessibility Service for Section-Level Blocking
- **Chosen Approach**: Implement an `AccessibilityService` that analyzes window content and UI hierarchy node text / view IDs (e.g. detecting `reels_tray_container`, `shorts_container`, or specific reel player view groups).
- **Why**: Standard `UsageStatsManager` only reports app package names, not internal sections. An Accessibility Service enables sub-app filtering without requiring root.
- **Fallback**: Provide clear in-app permission onboarding and simulation mode for environments without active accessibility toggles.

### Decision 2: 60-Second Cooldown with Haptic Cues
- **Chosen Approach**: Immediately upon entering Reels/Shorts, launch a floating countdown overlay giving 60 seconds of grace before enforcing the full block overlay.
- **Why**: Direct user choice from interview. Avoids jarring sudden crashes while ensuring the user remains acutely conscious of every second slipping away.

### Decision 3: Cloud Sync Architecture (Firebase Firestore + Google Sign-In)
- **Chosen Approach**: Sync user settings, limits, daily screen time history, milestone badges, and streak status to Firebase Firestore under `/users/{userId}/...` authenticated via Google Sign-In.
- **Why**: Satisfies cross-platform sync requirement; local Room cache ensures instant offline readiness and zero latency when tracking locally.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              FocusFlow App                             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼─────────────────────────────┐
       ▼                            ▼                             ▼
┌───────────────┐           ┌───────────────┐             ┌───────────────┐
│ Accessibility │           │  Usage Stats  │             │ Focus / DND   │
│    Service    │           │    Manager    │             │   Scheduler   │
└──────┬────────┘           └───────┬───────┘             └───────┬───────┘
       │                            │                             │
       │ (Detects Reels/Shorts)     │ (App usage minutes)         │ (Pomodoro &
       │                            │                             │  Silent Mode)
       ▼                            ▼                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Core Domain & Engine                           │
│  • AntiAddictionEngine (60s Cooldown State Machine & Intercept Shield)  │
│  • ProductivityScorer (Focus vs Distraction Scoring Model)              │
│  • StreakStakesManager (Streak accountability & penalty rules)          │
│  • NotificationFilterRepository (Batch notification tray)               │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼─────────────────────────────┐
       ▼                            ▼                             ▼
┌───────────────┐           ┌───────────────┐             ┌───────────────┐
│  Firebase AI  │           │   Firestore   │             │   Room / Local│
│     Logic     │           │   Database    │             │   Preferences │
│ (Gemini 3.8)  │           │ (Cross-Device)│             │ (Offline-1st) │
└───────────────┘           └───────────────┘             └───────────────┘
```

### Component & Screen Hierarchy
1. `MainActivity.kt`: App lifecycle, App Check initialization, Credential Manager auth gate.
2. `FocusFlowNavHost.kt`:
   - `DashboardScreen`: Live screen time gauge, productivity score, Reels/Shorts shield toggles, active streaks.
   - `AppLimitsScreen`: App list with minute limits, custom social media rules, and section blocking switches.
   - `PomodoroScreen`: Circular timer, interval settings, auto-DND toggle, filtered notifications tray.
   - `StatisticsScreen`: Weekly/monthly trends, time saved by section blocker, hourly usage heatmaps.
   - `MilestonesScreen`: Streak shields, achievement badges, penalty history.
3. `CooldownOverlayService.kt`: Float-over-app 60s countdown timer and full-screen hard block shield.
4. `FocusAccessibilityService.kt`: Detects Instagram Reels and YouTube Shorts view-tree components.

---

## 5. Verification & Testing Plan
- **Build Verification**: Run `compile_applet` to confirm clean compilation with Jetpack Compose M3 and Firebase dependencies.
- **State & Timer Testing**: Verify 60-second countdown lifecycle, pause/resume, and hard lock trigger transitions.
- **Database & Sync Rules**: Local testing of Firestore sync schemas and Credential Manager flows.
- **Edge-to-Edge & Accessibility**: Ensure touch targets are >= 48dp, deep OLED dark mode contrast ratios meet WCAG AA, and navigation bars adapt smoothly across device sizes.
