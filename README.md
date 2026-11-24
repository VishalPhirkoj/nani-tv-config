# Nani TV

Haier Android TV (912MB RAM, Android 11) ke liye ek halka launcher —
full-screen serial carousel, OK dabate hi serial khul jaye. TV on hote hi
aur Home dabate hi yahi app saamne aata hai.

## TV pe kaise dikhta hai

- Ek waqt pe ek serial — full-screen landscape poster, neeche bada Telugu
  naam (Zee5 me naam poster me hi hota hai, wahan chhupa rehta hai), uske
  neeche "[JioHotstar logo] · [Star Maa logo] · Brahmamudi · New Episode
  Today" ("New Episode Today" sirf jab `schedule` ke hisaab se aaj episode
  hai), sabse neeche dots (kaunsa serial chal raha hai)
- Har `slide_seconds` (default 3) me agla serial, crossfade ke saath
- Remote: ◀ ▶ se aage/peeche. Remote dabate hi auto-slide 15 sec ruk jaata hai
- OK = screen pe jo serial hai wahi khulega. Slide badalte hi (0.8 sec ke
  andar) OK dabaya toh pichla wala khulta hai — jo Nani dekh rahi thi
- Back = kuch nahi. **Back 3 second pakdo** = Android TV ka asli home
  khulta hai (settings / doosre apps ke liye). Screen pe upar-daayein
  likha rehta hai: "Hold BACK for 3 seconds to exit". Android home pe 2 minute koi button na dabe
  → Nani TV khud wapas (button dabte raho toh nahi kudta; Settings jaise
  doosre app me ho toh beech me nahi aata)

## Architecture

```
config.json  GitHub repo (static)  →  serials, wallpaper, theme, schedule
app/         Kotlin                →  JSON fetch karo, carousel dikhao, Intent fire karo, TV on/Home pe saamne aao
```

Koi server nahi hai. App ek static JSON padhta hai:

```
https://raw.githubusercontent.com/VishalPhirkoj/nani-tv-config/refs/heads/main/config.json
```

App me koi serial hardcoded nahi hai. Naya serial, wallpaper ya theme
badalna = `nani-tv-config` repo me `config.json` edit karke commit karo.
APK dobara build karne ki zarurat nahi.

Network na mile toh app last good config (cache) dikhata hai.

## Config update karna

1. GitHub pe `nani-tv-config` repo me `config.json` edit karo
   (browser me hi pencil icon se ho jata hai)
2. Commit karo
3. TV pe launcher pe wapas aao — list refresh ho jayegi

GitHub raw URL ~5 minute tak cache rehta hai (`Cache-Control: max-age=300`),
isliye change turant na dikhe toh thodi der ruko.

Commit se pehle JSON valid hai ye check kar lena — toota JSON aaya toh app
purani cached list dikhata rahega (crash nahi hoga, par update bhi nahi).

### Format

Poora example is repo ke [config.json](config.json) me hai.

```json
{
  "updated": "2026-11-24",
  "wallpaper": null,
  "accent": "#E6C068",
  "slide_seconds": 3,
  "shows": [
    {
      "id": "brahmamudi",
      "enabled": true,
      "name": "బ్రహ్మముడి",
      "name_en": "Brahmamudi",
      "channel": "Star Maa",
      "ott": "hotstar",
      "image_landscape": "https://.../posters/brahmamudi-landscape.jpg",
      "image_portrait": "https://.../posters/brahmamudi-portrait.jpg",
      "url": "https://www.hotstar.com/in/shows/brahma-mudi/1260129377",
      "package": "in.startv.hotstar",
      "schedule": "mon-sat"
    }
  ]
}
```

**Top level**

| Field | Zaroori? | Kaam |
| --- | --- | --- |
| `updated` | nahi | Config kab badla. Logcat me dikhta hai — TV ne naya config uthaya ya nahi, yahi se pata chalta hai. Har commit pe badal dena. |
| `wallpaper` | nahi | Background image URL, jab poster na ho tab dikhta hai |
| `accent` | nahi | Active dot ka rang (default `#E6C068`) |
| `slide_seconds` | nahi | Har serial kitne second dikhe, 2–30 (default 3) |
| `logos` | nahi | `{ "Star Maa": url, "hotstar": url, ... }` — key channel ya `ott` ka naam (capital/space se fark nahi). Subtitle me naam ki jagah logo dikhta hai; load na ho toh naam. Transparent PNG, andhere background pe dikhne wala. Files `logos/` me ([credits](logos/CREDITS.md)) |

**Har show**

| Field | Zaroori? | Kaam |
| --- | --- | --- |
| `url` | **haan** | Deep link. Khaali ho toh show skip |
| `ott` | `package` na ho toh **haan** | OTT ka naam (`hotstar`, `zee5`, `aha`, `sunnxt`, `etvwin`, `sonyliv`, `netflix`, `prime`, `youtube`, `smarttube`). Isi se package khud mil jaata hai, aur TV pe subtitle me dikhta hai. Capital/space se fark nahi padta |
| `package` | `ott` na ho toh **haan** | OTT app ka package. Diya ho toh `ott` wale package se zyada maana jaata hai. Dono na ho toh show skip |
| `id` | nahi | Chhota pakka naam (`brahmamudi`). Refresh pe carousel isi se same serial pe rukta hai |
| `enabled` | nahi | `false` = chhupa do, delete mat karo (default `true`) |
| `name` | nahi | Bada naam (Telugu) |
| `name_en` | nahi | Neeche chhota English naam |
| `channel` | nahi | Subtitle me OTT ke baad: "JioHotstar · Star Maa · Brahmamudi" (logo ho toh logo) |
| `image_landscape` | nahi | Carousel poster, 1280x720 (16:9), poori screen bharta hai. Purana `image` field bhi chalta hai |
| `image_portrait` | nahi | Backup — landscape na ho ya load fail ho toh ye poora (bina kaate) dikhta hai |
| `title_in_image` | nahi | `true` = poster me naam pehle se likha hai, bada title mat dikhao. Default: `zee5` pe `true`, baaki pe `false`. Poster load na ho toh title wapas aa jaata hai |
| `schedule` | nahi | "New Episode Today" kab dikhe (neeche dekho). Default `daily` |

- Shows usi order me dikhte hain jis order me array me hain
- Koi image na ho toh wallpaper/kaala background + bada naam dikhta hai

### Schedule values

`daily`, `mon-fri`, `mon-sat`, `weekly:sat` (koi bhi din: `mon` … `sun`), `none`

`schedule` na diya ho toh `daily` maana jata hai. Badge app khud calculate
karta hai (`Badge.kt`) — TV ke local time se, jo IST pe set hai.

## App build karo

1. Android Studio me open karo, ya:

```bash
./gradlew assembleRelease
```

2. Install:

```bash
adb connect 192.168.1.3:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

Release build hi test karna — debug build TV pe hamesha slow lagta hai.

## Naya OTT app add karna ho

Do jagah change karni hai:

1. `AndroidManifest.xml` ke `<queries>` block me uska package add karo.
   Android 11+ pe bina iske `setPackage()` chup-chaap fail hota hai.
   (Ye APK rebuild maangta hai.)
2. `config.json` me show entry daal do. Agar `"ott": "<naam>"` se kaam
   chalana hai (package likhe bina), toh `Ott.kt` me bhi us OTT ki entry
   chahiye — warna `"package"` seedha likh do.

Deep link nikalne ka tareeka: laptop browser me show page kholo, URL
copy karo, phir test:

```bash
adb shell am start -a android.intent.action.VIEW -d "<URL>" -p <package>
```

Serial page pe land kare tabhi `config.json` me daalna.

## Known packages

```
in.startv.hotstar                   JioHotstar
com.graymatrix.did                  Zee5
ahaflix.tv                          aha
com.suntv.sunnxt                    Sun NXT
com.etvwin.androidtv                ETV Win
com.sonyliv                         SonyLIV
com.netflix.ninja                   Netflix
com.amazon.amazonvideo.livingroom   Prime Video
com.google.android.youtube.tv       YouTube
org.smarttube.stable                SmartTube
```

## Posters

**1280x720 landscape (16:9)**, JPG, ~300KB se kam. Poster poori screen
bharta hai (centerCrop), isliye 16:9 na ho toh kinare kat jayenge. Neeche
ka ~1/3 hissa andhera gradient se dhaka rehta hai (naam ke liye) — poster
ka zaroori hissa upar/beech me rakhna.

912MB RAM wale TV pe full-HD posters memory kha jate hain. Coil already
1280x720 pe downscale karta hai, par bandwidth bachane ke liye source bhi
isi size ka ho toh behtar.

Posters `nani-tv-config` repo ke `posters/` folder me rakho aur unka raw
`https://` URL `image_landscape` me daalo — ya OTT (Hotstar/Zee5) ka
1280x720 image URL seedha. OTT URL kabhi badal gaye toh wo poster gayab
hoga (naam phir bhi dikhega) — naya URL daal dena.

## App icon / banner

Source images `icons/` me hain (`banner-source.png` 16:9,
`icon-source.png` square). Badalna ho toh yahi files replace karo aur
Pillow script se `drawable-*/app_banner.png` (160x90 … 640x360) aur
`mipmap-*/ic_launcher.png` (48 … 192) dobara banao.

## TV setup (ek baar, adb se)

TV reset ho ya app uninstall ho, toh ye dobara chalana:

```bash
adb connect 192.168.1.3:5555
adb install -r app/build/outputs/apk/release/app-release.apk

# 1. Background se app kholne ki permission (auto-start + Home ke liye)
adb shell appops set tv.nani.launcher SYSTEM_ALERT_WINDOW allow

# 2. Home button watcher. PEHLE purani list dekho — AnyDesk ki service
#    already hai, use mat hatao, ':' se jodo:
adb shell settings get secure enabled_accessibility_services
adb shell settings put secure enabled_accessibility_services \
  "com.anydesk.adcontrol.ad1/com.anydesk.adcontrol.AccService:tv.nani.launcher/tv.nani.launcher.HomeWatcher"
adb shell settings put secure accessibility_enabled 1
```

Kya kya hota hai:

- **TV on** (is TV pe remote se band = poora shutdown): boot ke ~10 sec
  baad app khud khulta hai (`AutoStart.kt`). Standby se jaagne pe bhi
  (screen on) 3 sec me.
- **Home button**: Haier firmware Home pe hamesha Android TV launcher
  kholta hai — default-home setting ignore karta hai. `HomeWatcher.kt`
  (accessibility service — kaunsa app saamne hai, sirf package naam; screen
  content nahi padhta) Android TV launcher dikhte hi Nani TV wapas le aata
  hai. ~0.5 sec purana home dikhta hai.
- Background service ~45MB RAM leti hai (screen-on sunne ke liye zaroori).

Check: `adb logcat -s NaniTV:*` me `boot broadcast`, `screen on`,
`auto-start: home button` jaisi lines.

**Sab band karna ho** (wapas normal TV):

```bash
adb shell settings put secure enabled_accessibility_services \
  "com.anydesk.adcontrol.ad1/com.anydesk.adcontrol.AccService"
adb shell appops set tv.nani.launcher SYSTEM_ALERT_WINDOW default
```

ADB + Tailscale hamesha alive rakhna. Wahi rescue line hai — app crash
loop me gaya toh Home bhi wapas app pe hi aayega, tab sirf adb se nikal
paoge.
