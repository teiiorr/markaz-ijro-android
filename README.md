# Markaz Ijro — Android

Нативное Android-приложение для **Ichki Ijro** (BKRM).

## Архитектура

Это **Capacitor WebView-обёртка** над живым сайтом `https://markaz-ijro.uz`:

* **Тот же дизайн** — внутри приложения работает та же Next.js версия, что и в браузере. UI обновляется автоматически при каждом деплое веба — APK переустанавливать не нужно.
* **Тот же бекенд** — все запросы идут на тот же сервер, та же база, та же авторизация.
* **Респонсивно везде** — веб уже адаптивен (см. `ichki-ijro/src/app/[locale]/(dashboard)/layout.tsx`), WebView просто отдаёт mobile breakpoint.
* **Нативные возможности** через Capacitor:
  * выбор и скачивание файлов (deliverables, attachments)
  * статус сети (offline-уведомление)
  * splash-экран + статус-бар в брендовых цветах
  * системный браузер для внешних ссылок (`mailto:`, посторонние сайты)

## Что внутри

| Файл | Что |
|---|---|
| `capacitor.config.ts` | Указывает на `https://markaz-ijro.uz`, splash + status-bar настройки |
| `android/` | Сгенерированный Android Studio проект |
| `android/app/src/main/AndroidManifest.xml` | Разрешения: INTERNET, NETWORK_STATE, READ/WRITE_EXTERNAL_STORAGE |
| `android/app/src/main/res/values/colors.xml` | Brand-палитра |
| `android/app/src/main/res/values/styles.xml` | Темы: status-bar / splash тёмные |
| `android/app/build.gradle` | Build + signing-config через external properties |
| `www/index.html` | Bootstrap-страница (мгновенный redirect на прод-URL) |

## 🚀 Самый простой способ — GitHub Actions собирает APK за вас

**Не нужно ставить ни Android Studio, ни SDK на Mac.** GitHub собирает APK
в своём облаке бесплатно.

1. Залить этот репо на GitHub (приватный или публичный — без разницы)
2. Запушить любой коммит в `main`
3. Открыть на GitHub: **Actions** → последний run → **Artifacts** →
   скачать **markaz-ijro-debug-apk** (zip с APK внутри)

Workflow в `.github/workflows/build-android.yml` запускается на каждый
push в `main` и собирает APK за ~3-5 минут на linux-runner с предустановленным
Android SDK.

### Релизный подписанный APK (для раздачи сотрудникам)

Тегните коммит как `v1.0.0` — GitHub Actions автоматически:
1. соберёт `app-release.apk`
2. подпишет (если настроен keystore в repo secrets — см. ниже)
3. создаст **GitHub Release** с APK файлом, который любой может скачать без
   аккаунта GitHub:

```bash
git tag v1.0.0
git push origin v1.0.0
# через 3-5 минут — Releases → v1.0.0 → markaz-ijro-v1.0.0.apk
```

### Настройка подписания в GitHub Actions (один раз)

1. Создать keystore локально (только для генерации ключа):

```bash
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 -alias markaz-ijro
```

2. Закодировать keystore в base64:

```bash
base64 -i release.keystore | pbcopy
# скопирует в буфер обмена
```

3. На GitHub: **Settings → Secrets and variables → Actions → New repository secret**, создать 4 секрета:

| Имя секрета | Значение |
|---|---|
| `KEYSTORE_BASE64` | вставить из буфера обмена |
| `KEYSTORE_PASSWORD` | пароль который указали в keytool |
| `KEY_ALIAS` | `markaz-ijro` |
| `KEY_PASSWORD` | пароль ключа (обычно тот же что keystore) |

4. Сохранить `release.keystore` в защищённое место (Bitwarden / 1Password / печатное хранилище). **Если потеряете — не сможете больше выпускать апдейты к уже установленным APK на телефонах.**

5. Удалить локальный файл:

```bash
rm release.keystore
```

С этого момента каждый тег `v*` → подписанный APK в Releases автоматически.

---

## 🛠 Альтернатива — собрать на Mac локально

Если хотите собирать APK с ноутбука без GitHub:



```bash
# Java 17 — уже есть (openjdk@17)
brew install --cask android-studio
# открыть Android Studio → SDK Manager:
#   Android SDK Platform 34 + Build-Tools 34
#   Android SDK Command-line Tools (latest)
#   Android SDK Platform-Tools
```

После установки добавить в `~/.zshrc`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

## Debug APK (для тестирования)

```bash
npm install
npm run build:debug
# готовый APK: ./markaz-ijro-debug.apk
```

Установить на телефон:

```bash
# подключить телефон по USB, включить "USB debugging"
adb install markaz-ijro-debug.apk
```

Или просто перетащить `.apk` файл на телефон через файловый менеджер.

## Release APK (для публикации сотрудникам)

### 1. Создать keystore (один раз)

```bash
keytool -genkey -v \
  -keystore android/app/release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias markaz-ijro
```

Запомните или сохраните **пароли** в защищённом месте (например в Bitwarden).

### 2. Создать `android/app/keystore.properties`

```properties
storeFile=release.keystore
storePassword=ВАШ_ПАРОЛЬ_KEYSTORE
keyAlias=markaz-ijro
keyPassword=ВАШ_ПАРОЛЬ_KEY
```

Этот файл `.gitignored` — он **не должен** попадать в репозиторий.

### 3. Собрать подписанный APK

```bash
npm run build:release
# готовый APK: android/app/build/outputs/apk/release/app-release.apk
```

Или AAB-bundle для Google Play:

```bash
npm run build:bundle
# готовый bundle: android/app/build/outputs/bundle/release/app-release.aab
```

### 4. Раздать сотрудникам

Внутренний инструмент — Google Play не нужен. Варианты:

* **Прямая ссылка** на `.apk` файл с самого сайта (например `markaz-ijro.uz/app.apk`)
* **MDM/EMM** систему (для гос-орг типа БКРМ — например через Yo'ldosh или Beeline Mobile)
* **Telegram-канал** для сотрудников с приложенным `.apk`

При установке сотрудники должны разрешить "Установка из неизвестных источников" один раз.

## Обновление приложения

**Хорошая новость**: для UI-изменений переустанавливать APK **не нужно**. Каждый раз когда деплоится новая версия веба (`https://markaz-ijro.uz`), приложение автоматически её подхватывает при следующем открытии.

**Когда нужна переустановка**:
* Изменения в native-коде Capacitor (новые плагины, новые разрешения)
* Обновление splash-экрана / иконки / brand-цветов
* Изменение `applicationId` или подписи

В таком случае:

```bash
# Поднять versionCode и versionName в android/app/build.gradle
npm run build:release
# раздать новый APK как раньше
```

## Что НЕ работает offline

WebView-приложение требует интернет. Без сети показывается стандартная ошибка Chrome. Если нужен offline-режим — отдельная задача (придётся переписывать backend под локальную БД + sync).

## Структура коммитов

Этот репозиторий **отдельный** от веба (`ichki-ijro`). Ничего из веба сюда копировать не надо — приложение **тянет UI с продакшен URL** в рантайме.

Если меняется веб — коммитьте в `ichki-ijro`, деплоите через `deploy/deploy.sh`. Android-приложение само подхватит изменения.

Если меняется нативная обёртка (splash, иконка, разрешения) — коммитьте сюда, пересоберите APK, раздайте.

## Лицензия

Приватная — внутренний инструмент BKRM.
