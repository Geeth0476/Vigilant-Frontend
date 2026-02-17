## Vigilant – Risk Analysis Algorithm (On‑Device + Backend)

This document explains:
- How the **current app** calculates risk locally (`RiskScoreManager`, `ScanDataManager`, `ScanLocalDataSource`).
- How a **backend component** should extend this to real‑time, multi‑source risk.
- Where backend updates live (tables, PHP modules) and how the Android app should integrate.

---

## 1. Current On‑Device Risk Model (As Implemented)

### 1.1 Per‑App Static Risk – `RiskScoreManager`

File: `RiskScoreManager.kt`

For each installed app (`PackageInfo`), the manager:

1. **Initialises scores**
   - `permScore = 0`
   - `behaviorScore = 0`
   - `modifierScore = 0`

2. **Permission‑based score (`PERMISSION` factors)**
   - Looks at `requestedPermissions` and applies weights from `PERM_WEIGHTS` (e.g.):
     - `BIND_ACCESSIBILITY_SERVICE` → +25
     - `RECORD_AUDIO` → +15
     - `ACCESS_FINE_LOCATION` → +12
     - `ACCESS_BACKGROUND_LOCATION` → +15
     - `READ_SMS`, `SEND_SMS`, `READ_CALL_LOG`, `USAGE_STATS`, etc.
   - For each matched perm it adds a `RiskFactor(description, score, FactorType.PERMISSION)`.
   - Caps `permScore` at **65**.

3. **Inferred behaviour score (`BEHAVIOR` factors)**
   - If `RECEIVE_BOOT_COMPLETED` → +5 (`"Runs automatically at startup"`).
   - If `RECORD_AUDIO` + `BIND_ACCESSIBILITY_SERVICE` → +20 (`"High Risk: Can record audio and control screen"`).
   - If `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` → +10 (`"Tracks location in background"`).
   - If `READ_SMS` + `READ_CONTACTS` → +10 (`"Accesses both SMS and Contacts"`).
   - Caps `behaviorScore` at **40**.

4. **Modifiers (`MODIFIER` factors)**
   - Install time:
     - `< 3 days` → +5 (`"Recently installed (< 3 days)"`).
     - `> 180 days` → −5 (`"Trusted (Installed > 6 months)"`).
   - Suspicious keywords in package name:
     - If package contains words like `"tracker"`, `"spy"`, `"monitoring"`, `"stealth"`, etc. → +20 (`"Suspicious package name: 'spy'"`).

5. **Total score & level**
   - `finalScore = permScore + behaviorScore + modifierScore`.
   - Clamped to `[0, 100]`.
   - Risk level:

     ```text
     0–20   → SAFE
     21–40  → LOW
     41–70  → MEDIUM
     71–90  → HIGH
     91–100 → CRITICAL
     ```

6. **Return**
   - `RiskResult(totalScore, riskLevel, riskFactors)`.
   - Used to fill `InstalledAppRepository.InstalledApp(riskScore, riskFactors, riskDescription)`.

### 1.2 Device‑Level Risk – `ScanDataManager` / `ScanLocalDataSource` / `ScanRepository`

- `ScanDataManager` (object) and `ScanLocalDataSource` (class) **mirror** each other:
  - Use `SharedPreferences` `"vigilant_scan_data"` with keys:
    - `last_scan_time` – `Long` (millis).
    - `risk_score` – `Int` (0..100).
    - `risk_level` – `SAFE`, `MEDIUM`, `HIGH`.
    - `top_risk_app` – package string.
  - Provide helpers:
    - `saveScanResult(score, level)`.
    - `getRiskScore()`, `getRiskLevel()`, `getLastScanTime()`.
    - `getRiskLevelFromScore(score)` (simple thresholds: `<30 SAFE`, `<60 MEDIUM`, else HIGH).

- `ScanRepository` wraps `ScanLocalDataSource` and:
  - Proxies save/get functions to the ViewModels.
  - Generates `SecurityAlert` instances from high‑risk apps if needed.

### 1.3 Alerts – `SecurityAlert`

- `SecurityAlert.createAlertFromApp(appPackage, appName, riskScore, riskFactors)`:
  - Maps `riskScore`:
    - `>= 80` → `Severity.HIGH`.
    - `>= 50` → `Severity.MEDIUM`.
    - Else `Severity.LOW`.
  - Builds `title`, `description` (first factor), and a multi‑line `detailedInfo`.
  - Returns a one‑off alert object stored in memory / prefs via `ScanRepository` or `ScanDataManager`.
- `generateRandomAlerts()` is used as a fallback demo when no risky apps are found.

### 1.4 Where Risk Is Used in UI

- **Dashboard**
  - `DashboardViewModel` reads risk score/level and last scan time from `ScanRepository` / `ScanDataManager` and animates the hero ring + score.
  - Uses `ScanLocalDataSource.RiskLevel` or `ScanDataManager.RiskLevel` to choose Safe/Medium/High UI.

- **Scan screens**
  - `ScanProgressActivity` computes `RiskResult` using `RiskScoreManager` and sends it through to:
    - `ScanResultsActivity`, `ScanResultActivity`, and `InstalledAppsActivity`.

- **Assistant & Detail screens**
  - `VigilantAssistantActivity` uses per‑app `riskScore` to generate explanation copy.
  - `AppAnalysisActivity` maps `riskScore` again to `RiskLevel` for the app detail breakdown.

> **Important**: All of this is **local** – no backend, no cross‑user or cross‑device intelligence yet.

---

## 2. Backend Risk Model – Extension Design

The backend should **not replace** the on‑device calculation, but **augment & correct it** based on:
- Long‑term scan history.
- Permission usage timelines.
- Community threat data.
- Backend‑curated risk feeds (optional).

### 2.1 Data Sources (from MySQL)

Using `MYSQL_SCHEMA.md`:

- `app_scans` / `app_scan_results` / `risk_factors`
  - Historical per‑scan, per‑app risk and explanation factors.

- `device_risk_scores`
  - Last known device risk, for fast dashboard queries.

- `permission_events`
  - Timeline of real permission use (`USED`, `GRANTED`, `REVOKED`) and context (foreground, background, screen off).

- `security_alerts`
  - Previous high‑severity alerts and whether the user acted on them.

- `community_threats` / `community_reports`
  - Aggregated category and risk for packages across the community.

---

## 3. Proposed Hybrid Risk Algorithm

### 3.1 Per‑App Backend Score

Let:
- \( S_\text{local} \in [0, 100] \) be the on‑device `RiskScoreManager` score.
- \( S_\text{perm} \) be permission pattern risk (already encoded locally).
- \( S_\text{beh} \) be behavioral risk from `permission_events`.
- \( S_\text{comm} \) be community risk from `community_threats` & `community_reports`.
- \( S_\text{hist} \) be historical risk from prior scans.

We define a backend **composite score**:

\[
S_\text{backend} =
  w_\text{local} S_\text{local} +
  w_\text{beh} S_\text{beh} +
  w_\text{comm} S_\text{comm} +
  w_\text{hist} S_\text{hist}
\]

with weights (example):
- \( w_\text{local} = 0.4 \)
- \( w_\text{beh} = 0.25 \)
- \( w_\text{comm} = 0.25 \)
- \( w_\text{hist} = 0.10 \)

Then clamp to `[0,100]` and map to risk levels exactly like `RiskScoreManager` (SAFE/LOW/MEDIUM/HIGH/CRITICAL).

#### 3.1.1 Behavioural score \( S_\text{beh} \)

From `permission_events` over a recent window (e.g. last 7 days):

- Count **background** usage of sensitive permissions:
  - `RECORD_AUDIO` with `context = 'SCREEN_OFF'` or `'BACKGROUND'`.
  - `ACCESS_FINE_LOCATION` with `context = 'BACKGROUND'`.
  - `READ_SMS` / `READ_CONTACTS` at odd times.

Example:

```sql
SELECT
  SUM(CASE
        WHEN permission = 'android.permission.RECORD_AUDIO'
         AND context IN ('SCREEN_OFF','BACKGROUND')
        THEN 15 ELSE 0 END) +
  SUM(CASE
        WHEN permission = 'android.permission.ACCESS_FINE_LOCATION'
         AND context = 'BACKGROUND'
        THEN 10 ELSE 0 END) +
  SUM(CASE
        WHEN permission IN ('android.permission.READ_SMS','android.permission.READ_CONTACTS')
        THEN 8 ELSE 0 END) AS beh_score
FROM permission_events
WHERE installed_app_id = :app_id
  AND created_at >= NOW() - INTERVAL 7 DAY;
```

Then clamp `beh_score` to e.g. `[0, 40]` as in the on‑device logic.

#### 3.1.2 Community score \( S_\text{comm} \)

From `community_threats`:

- If `community_threats.risk_level = CRITICAL` → base +40.
- `HIGH` → +30.
- `MEDIUM` → +20.
- `LOW` → +10.
- Extra +5..10 if `report_count` is very high (e.g. > 100).

Example pseudo‑SQL:

```sql
SELECT
  CASE risk_level
    WHEN 'CRITICAL' THEN 40
    WHEN 'HIGH' THEN 30
    WHEN 'MEDIUM' THEN 20
    WHEN 'LOW' THEN 10
    ELSE 0
  END +
  CASE
    WHEN report_count > 100 THEN 10
    WHEN report_count > 50  THEN 5
    ELSE 0
  END AS comm_score
FROM community_threats
WHERE package_name = :package_name
LIMIT 1;
```

If no row exists, `S_comm = 0`.

#### 3.1.3 Historical score \( S_\text{hist} \)

From previous `app_scan_results`:

- Increase score if the app’s risk has been **consistently high**.
  - E.g. last 3 scans all `>= 70` → +10.
  - Last 3 average **rising** → +5.

Example:

```sql
SELECT AVG(risk_score) AS avg_score
FROM app_scan_results asr
JOIN app_scans s ON s.id = asr.scan_id
JOIN installed_apps ia ON ia.id = asr.installed_app_id
WHERE ia.package_name = :package_name
  AND s.device_id = :device_id
ORDER BY s.completed_at DESC
LIMIT 3;
```

Map average to a small modifier `[0, 15]`.

---

## 4. Real‑Time Device Risk Updates

### 4.1 Backend Flow on Scan Completion

When the app finishes a scan, it should:

1. **POST `/v1/scan/complete`** with:
   - Device + user id.
   - Local `risk_score` and `risk_level`.
   - List of apps + local per‑app `riskScoreManager` results (base score + factors).

2. Backend:
   - Inserts into `app_scans`, `installed_apps` and `app_scan_results` + `risk_factors`.
   - For each app, calculates backend composite \( S_\text{backend} \) using:
     - Local score from payload (`S_local`).
     - Behavioural data from `permission_events` (`S_beh`).
     - Community data from `community_threats` (`S_comm`).
     - History from `app_scan_results` (`S_hist`).
   - Identifies top high‑risk apps and creates `security_alerts` as needed.
   - Updates `device_risk_scores.last_score`, `last_level`, and `last_scan_id`.

3. **Response**:
   - Option A (simple): return **one** corrected device risk score/level.
   - Option B (richer): also return **per‑app corrected scores** so the app can update UI labels (`High Risk Apps`, `Medium`, `Safe` counts).

### 4.2 How Android Uses Backend Risk

#### 4.2.1 Dashboard & Scan Results

- After finishing a scan and receiving backend response:
  - `ScanResultsActivity` uses backend score/level **if present**, else fallbacks to local:

    ```kotlin
    val backendScore = response.backendRiskScore ?: localRiskScore
    val backendLevel = response.backendRiskLevel ?: localRiskLevel
    ```

- `DashboardViewModel` periodically (e.g. on resume) calls:
  - `GET /v1/scan/latest?device_id=...`
  - Uses `device_risk_scores` to refresh hero ring and subtitle.

#### 4.2.2 Alerts

- `DashboardActivity` and `AlertDetailActivity` fetch from `/v1/alerts/recent`.
- Android still locally constructs `SecurityAlert` style objects but now from backend JSON:
  - id, title, description, detailedInfo, severity, timestamp, recommendations, packageName.

---

## 5. Where Updates Take Place (Backend Code)

### 5.1 PHP Risk Engine (`core/risk_engine.php`)

Responsibilities:
- Given:
  - `device_id`, `user_id`, `package_name`, localScore, factors list.
- It:
  - Reads `permission_events`, `community_threats`, `app_scan_results`.
  - Computes \( S_\text{beh} \), \( S_\text{comm} \), \( S_\text{hist} \).
  - Combines them into `backend_score` + `backend_level`.
  - Returns:

    ```php
    [
      'backend_score' => 78,
      'backend_level' => 'HIGH',
      'explanations'  => [
        'Mic + Accessibility used in background 3 times this week',
        'App flagged as High Risk by community (127 reports)'
      ]
    ]
    ```

- `ScanController` uses this to:
  - Update `app_scan_results` with backend‑adjusted score if desired.
  - Populate `security_alerts` and `device_risk_scores`.

### 5.2 Controllers

- `ScanController::completeScan`
  - Calls `RiskEngine::computeDeviceRisk(...)`.

- `AlertsController`
  - Simple queries on `security_alerts` with alert rules / quiet hours filters.

- `CommunityController`
  - Writes to `community_reports` and updates `community_threats`.

---

## 6. Future Enhancements

1. **Server‑Side Model Updates without App Changes**
   - By keeping the on‑device model simple and the backend composite score in one place (`risk_engine.php`), you can:
     - Adjust weights.
     - Add new behavioural rules.
     - Integrate new feeds (e.g. threat intelligence lists).
   - Without shipping a new app; the client just consumes the updated backend score.

2. **Real‑Time Push**
   - Optional: use WebSockets or FCM data messages to push:
     - New alerts.
     - Significant risk changes.
   - Android would refresh the dashboard and alerts list when receiving such events.

3. **Per‑User / Per‑Region Tunings**
   - Different default alert rules or weights for regions with specific regulations.

---

## 7. Summary

- **Today**: your app has a clear, deterministic local risk model (`RiskScoreManager`) based on permissions + static behaviour + install age + package name.
- **Backend**: should ingest local scores and enrich them using:
  - Behavioural telemetry (`permission_events`),
  - Community data (`community_threats`, `community_reports`),
  - Historical trends (`app_scan_results`),
  - Then maintain aggregated device risk (`device_risk_scores`) and alerts (`security_alerts`).
- **Android integration**: mostly consists of:
  - Posting scan results to `/v1/scan/complete`,
  - Using `/v1/scan/latest`, `/v1/apps`, `/v1/alerts/recent`, `/v1/community/*` for richer UI,
  - Gradually preferring backend risk levels when present, while keeping the local risk logic as a reliable fallback.

