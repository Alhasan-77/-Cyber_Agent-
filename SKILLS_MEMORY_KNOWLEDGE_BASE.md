# سجل الذاكرة الشامل للمهارات والأدوات والمكتبات المستخرجة (Skills-Main Knowledge Base)

- **إجمالي الملفات المفهرسة**: 184 ملفاً
- **إجمالي المهارات المستخرجة**: 25 مهارة
- **خادم MCP المدمج**: `https://mcp.expo.dev/mcp`

## expo-skill-eval — تقييم المهارات والتدقيق البصري الآلي (Evaluation, Testing & MCP)
- **المسار**: `skills-main/.claude/skills/expo-skill-eval/SKILL.md`
- **الوصف**: Evaluate Expo skills in this repo end-to-end - trigger accuracy, generated code quality, and runtime screenshots on iOS simulator and Android emulator via Expo Go (web optional). Use when the user wants to eval an Expo skill, test that a skill produces working code, benchmark a skill with device screenshots, or verify a skill's output renders correctly.
- **الحزم والمكتبات**: `@expo/ui, expo-app, expo-app-clip, expo-brownfield, expo-data-fetching, expo-dev-client, expo-dom, expo-go, expo-module, expo-modules, expo-native-ui, expo-observe, expo-router, expo-skill-eval, expo-skill-eval-, expo-skill-eval-expo-ui, expo-skill-eval-x, expo-tailwind-setup, expo-ui, expo-upgrade, react-native-css`
- **المراجع الفرعية (3)**: `agents/visual-grader.md, references/design-rubric.md, references/runtime-matrix.md`
- **السكريبتات (9)**: `scripts/check-static.sh, scripts/clean-fixture.sh, scripts/generate_viewer.py, scripts/latest-sdk.sh, scripts/make-fixture.sh, scripts/make-workspace.sh, scripts/snapshot-android.sh, scripts/snapshot-ios.sh, scripts/snapshot-web.sh`

## expo-migrate-module — ترقية الوحدات الأصلية القديمة إلى Expo Modules API (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo-experiments/skills/expo-migrate-module/SKILL.md`
- **الوصف**: Migrate an existing Apple/Swift Expo native module from the Expo Modules API 1.0 definition DSL to the 2.0 macro API (sometimes called v2) while preserving its JavaScript and TypeScript contract. Use when converting or incrementally adopting @ExpoModule, @JS, @Event, @SharedObject, or @Record in an existing module. Do not use for creating a new module, general Expo SDK upgrades, or Android/Kotlin migrations.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-migrate-module, expo-module, expo-modules-autolinking, expo-modules-core`
- **المراجع الفرعية (4)**: `agents/openai.yaml, references/compatibility.md, references/example.md, references/migration-map.md`
- **السكريبتات (0)**: ``

## eas-app-stores — النشر على متجري Google Play و App Store و TestFlight (EAS Cloud & DevOps)
- **المسار**: `skills-main/plugins/expo/skills/eas-app-stores/SKILL.md`
- **الوصف**: Build and submit iOS and Android apps with EAS to TestFlight, the App Store, or Google Play. Supports Expo and other React Native projects, plus existing native apps. Use for eas.json setup, release pipelines, signing, app versions and build numbers, store submissions, and listing metadata. For Expo websites and API routes, use eas-hosting; for adding React Native screens to a native app, use expo-brownfield.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-brownfield, expo-dev-client`
- **المراجع الفرعية (7)**: `agents/openai.yaml, references/app-store-metadata.md, references/ios-app-store.md, references/native-ios.md, references/play-store.md, references/testflight.md, references/workflows.md`
- **السكريبتات (0)**: ``

## eas-hosting — استضافة تطبيقات الويب وواجهات API السحابية (EAS Cloud & DevOps)
- **المسار**: `skills-main/plugins/expo/skills/eas-hosting/SKILL.md`
- **الوصف**: Deploy Expo websites and Expo Router API routes to EAS Hosting - export the web bundle, run eas deploy for production and PR preview URLs, manage environment secrets and custom domains, and work within the Cloudflare Workers runtime. Also covers authoring API routes (+api.ts handlers, HTTP methods, request handling, CORS). Use when deploying an Expo web app or API routes, setting up EAS Hosting, or configuring hosting environments and domains. Not for native builds or store releases - use the eas-app-stores skill for those.
- **الحزم والمكتبات**: `@expo/agent-cli`
- **المراجع الفرعية (1)**: `agents/openai.yaml`
- **السكريبتات (0)**: ``

## eas-observe — مراقبة الأداء والمقاييس الحية واكتشاف الأعطال (EAS Cloud & DevOps)
- **المسار**: `skills-main/plugins/expo/skills/eas-observe/SKILL.md`
- **الوصف**: Use for anything related to EAS Observe - adding `expo-observe` to an Expo project (AppMetricsRoot/ObserveRoot HOC, markInteractive and ObserveInteractiveMarker, the useObserve hook, the Expo Router / React Navigation integrations for per-route metrics, user-defined events via `Observe.logEvent`, error reporting via ObserveErrorBoundary and `Observe.reportError`, native crash reporting, source map upload for symbolicated stack traces, and runtime config such as sampleRate and dispatchInDebug), querying via the EAS CLI (`eas observe:metrics-summary`, `observe:metrics`, `observe:routes`, `observe:events`, `observe:session`, `observe:versions`, `observe:errors`, `observe:event`), interpreting the resulting metrics (cold/warm launch, TTR, TTI, navigation cold/warm TTR, update download, and the TTI frameRate/device/network params for triaging slow startups), or shipping an Observe integration inside a third-party package.
- **الحزم والمكتبات**: `@expo/agent-cli, @react-navigation/native, expo-doctor, expo-observe, expo-router, expo-splash-screen`
- **المراجع الفرعية (5)**: `agents/openai.yaml, references/metrics.md, references/queries.md, references/setup.md, references/third-party.md`
- **السكريبتات (0)**: ``

## eas-simulator — إدارة المحاكيات وفحص السجلات والانهيارات (Evaluation, Testing & MCP)
- **المسار**: `skills-main/plugins/expo/skills/eas-simulator/SKILL.md`
- **الوصف**: Run and control a user's app on a remote iOS/Android simulator hosted on EAS cloud. Read before running any `eas simulator:*` commands - it has the current syntax for this experimental API. Use whenever the user needs a simulator they can't run locally - 'run my app on a cloud simulator', 'use eas simulator to run/install/screenshot my app', 'I'm on Linux/Cursor and need an iOS device', 'no sim on this box / headless CI', 'let an agent click through my app and screenshot it', 'test my dev build on a remote sim with live reload', 'stream a sim to my browser' - even when they don't say 'EAS Simulator' or 'cloud'. On a host WITHOUT a local simulator (Linux, CI, cloud sandbox) it's the default; on macOS, do NOT auto-trigger for a plain 'run on the simulator' - use it only for a cloud/remote/shareable sim, an iOS version they lack, or an agent-driven session. NOT for local sims (expo run:ios, Xcode, Android Studio), EAS Build/Update, web preview, or physical devices.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-app, expo-dev-client, expo-development-client, expo-go, expo-platform, expo-router`
- **المراجع الفرعية (5)**: `agents/openai.yaml, references/controllers.md, references/logs-and-crashes.md, references/run-your-app.md, references/troubleshooting.md`
- **السكريبتات (0)**: ``

## eas-update-insights — تحليلات وإحصائيات قنوات التحديث (EAS Cloud & DevOps)
- **المسار**: `skills-main/plugins/expo/skills/eas-update-insights/SKILL.md`
- **الوصف**: Check the health of published EAS Update: crash rates, install/launch counts, unique users, payload size, and the split between embedded and OTA users per channel. Use when the user asks how an update is performing, whether a rollout is healthy, how many users are on the embedded build vs OTA, or wants to gate CI on update health.
- **الحزم والمكتبات**: `@expo/agent-cli`
- **المراجع الفرعية (3)**: `agents/openai.yaml, references/channel-insights-schema.md, references/update-insights-schema.md`
- **السكريبتات (0)**: ``

## eas-update — التحديثات الهوائية الفورية (OTA Updates) (EAS Cloud & DevOps)
- **المسار**: `skills-main/plugins/expo/skills/eas-update/SKILL.md`
- **الوصف**: Configure and use EAS Update for over-the-air JavaScript and asset updates with expo-updates and EAS CLI. Use when setting up OTA updates, running eas update:configure or eas update, publishing to preview/staging/production channels, explaining branches/channels/runtime versions, testing updates, or debugging why an installed build still shows old code. Load for TestFlight, preview, or production updates that do not appear, including questions about cold launches or reopening the app. Not for update health metrics; use eas-update-insights for adoption, crashes, and rollout monitoring.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-channel-name, expo-dev-client, expo-updates`
- **المراجع الفرعية (1)**: `agents/openai.yaml`
- **السكريبتات (0)**: ``

## eas-workflows — أتمتة خطوط أنابيب البناء والـ CI/CD السحابية (EAS Cloud & DevOps)
- **المسار**: `skills-main/plugins/expo/skills/eas-workflows/SKILL.md`
- **الوصف**: Helps understand and write EAS workflow YAML files for Expo projects. Use this skill when the user asks about CI/CD or workflows in an Expo or EAS context, mentions .eas/workflows/, or wants help with EAS build pipelines or deployment automation.
- **الحزم والمكتبات**: `@expo/agent-cli`
- **المراجع الفرعية (1)**: `agents/openai.yaml`
- **السكريبتات (2)**: `scripts/fetch.js, scripts/package.json`

## expo-animation — الرسوم المتحركة الفيزيائية والإيماءات (Reanimated v4) (UI, Compose & Animations)
- **المسار**: `skills-main/plugins/expo/skills/expo-animation/SKILL.md`
- **الوصف**: Build animations in React Native and Expo, making the decisions in the order that determines whether they feel right — should it animate, which thread it runs on, which properties, spring or timing, how the gesture hands off, how it degrades. Writes the implementation with Reanimated, Gesture Handler, Expo Router and expo-haptics. Use when animating anything in an Expo app, adding gestures, sheets, screen transitions, press feedback or haptics, or fixing motion that stutters on device. For web animation use `animate`.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-animation, expo-haptics, expo-router, react-native-gesture-handler, react-native-keyboard-controller, react-native-reanimated, react-native-skia, react-native-worklets`
- **المراجع الفرعية (2)**: `RECIPES.md, agents/openai.yaml`
- **السكريبتات (0)**: ``

## expo-app-clip — بناء App Clips لتجارب التشغيل الفوري (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo/skills/expo-app-clip/SKILL.md`
- **الوصف**: Add an iOS App Clip target to an Expo app. Use when the user mentions App Clip, AASA, apple-app-site-association, appclips, smart app banner, or wants to ship a lightweight iOS Clip invoked from a URL alongside their parent app.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-app-clip, expo-apple-targets, expo-module, expo-router, expo-target`
- **المراجع الفرعية (2)**: `agents/openai.yaml, references/native-module.md`
- **السكريبتات (0)**: ``

## expo-brownfield — دمج Expo داخل تطبيقات Android و iOS الحالية (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo/skills/expo-brownfield/SKILL.md`
- **الوصف**: Integrate Expo and React Native into an existing native iOS or Android app. Use for brownfield, embedding a React Native screen in SwiftUI/UIKit or Kotlin, or AAR/XCFramework packaging. Covers isolated and integrated approaches. For building or distributing a purely native app with EAS, use eas-app-stores.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/cli, @expo/ui, expo-app, expo-autolinking-settings, expo-brownfield, expo-build-properties, expo-gradle-plugin, expo-modules-autolinking, expo-root-project, expo-router, expo-sdk-version-depends-on-a-react-native-version, expo-template-bare-minimum, expo-upgrade, react-native-gradle-plugin, react-native-screen`
- **المراجع الفرعية (7)**: `agents/openai.yaml, references/brownfield-integrated.md, references/brownfield-isolated.md, references/comparison.md, references/feature-integration.md, references/troubleshooting.md, references/version-compatibility.md`
- **السكريبتات (0)**: ``

## expo-data-fetching — جلب البيانات والعمل دون اتصال (Offline & Loaders) (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo/skills/expo-data-fetching/SKILL.md`
- **الوصف**: Use when implementing or debugging ANY network request, API call, or data fetching. Covers fetch API, React Query, SWR, error handling, caching, offline support, loading/empty/error screen states, and Expo Router data loaders (`useLoaderData`).
- **الحزم والمكتبات**: `@expo/agent-cli, @tanstack/react-query, expo-data-fetching, expo-router, expo-router-loaders, expo-secure-store, expo-server, react-native-community`
- **المراجع الفرعية (3)**: `agents/openai.yaml, references/expo-router-loaders.md, references/offline-and-cancellation.md`
- **السكريبتات (0)**: ``

## expo-design-system — نظام التصميم القياسي والتدقيق البصري (UI, Compose & Animations)
- **المسار**: `skills-main/plugins/expo/skills/expo-design-system/SKILL.md`
- **الوصف**: Build and maintain a design system inside an Expo app - a reusable theme of design tokens (color, spacing, typography, radius, shadow, motion), reusable component structure with variant/size/state prop conventions, and rules for when to extract a repeated view into a shared component. Use when creating or organizing theme files and design tokens (theme.ts / theme/), extending an existing theme or styling library (NativeWind, Tamagui, Restyle, Unistyles) in its own idiom, standardizing styles so screens (including AI-generated ones) look consistent and polished, fixing an app that looks AI-generated or generic instead of native (the named native-slop tells), building an in-app component library, or auditing an app for design-system drift (hardcoded colors, spacing, fonts). For platform styling specifics (semantic colors, HIG rules, native controls) use expo-native-ui; for folder layout of a new app use expo-project-structure.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/ui, expo-animation, expo-app, expo-data-fetching, expo-design-system, expo-font, expo-native-ui, expo-project-structure, expo-router, react-native-keyboard-controller`
- **المراجع الفرعية (3)**: `agents/openai.yaml, references/audit.md, references/native-slop.md`
- **السكريبتات (0)**: ``

## expo-dev-client — بناء عميل التطوير المخصص (Development Builds) (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo/skills/expo-dev-client/SKILL.md`
- **الوصف**: Build and distribute Expo development clients locally or via TestFlight for internal testing. For production TestFlight releases and store submission, use the eas-app-stores skill.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-dev-client, expo-development-client, expo-overview`
- **المراجع الفرعية (1)**: `agents/openai.yaml`
- **السكريبتات (0)**: ``

## expo-dom — دمج مكونات الويب داخل البيئة الأصلية (use dom) (UI, Compose & Animations)
- **المسار**: `skills-main/plugins/expo/skills/expo-dom/SKILL.md`
- **الوصف**: Use Expo DOM components to run web code in a webview on native and as-is on web. Migrate web code to native incrementally. For the end-to-end migration of a whole web app, use the expo-web-to-native skill.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-dom, expo-router, expo-web-to-native`
- **المراجع الفرعية (1)**: `agents/openai.yaml`
- **السكريبتات (0)**: ``

## expo-examples — كتالوج الأمثلة والقوالب الرسمية (Architecture & Routing)
- **المسار**: `skills-main/plugins/expo/skills/expo-examples/SKILL.md`
- **الوصف**: Expo's official example projects - the expo/examples repo of ~70 `with-*` integrations (Stripe, Clerk, Supabase, OpenAI, maps, Reanimated, SQLite, Skia, NativeWind, and more). Use when integrating a third-party library or service into an existing Expo app and you want the canonical, version-matched pattern to adapt, or when scaffolding a new project from one with `npx create-expo --example`.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/ui, expo-camera, expo-examples, expo-module, expo-native-ui, expo-ref, expo-router, expo-sqlite, expo-ui, expo-upgrade, expo-web-browser, react-native-maps, react-native-svg`
- **المراجع الفرعية (2)**: `agents/openai.yaml, references/catalog.md`
- **السكريبتات (0)**: ``

## expo-module — بناء الوحدات والمناظر الأصلية بـ Kotlin و Swift (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo/skills/expo-module/SKILL.md`
- **الوصف**: Guide for creating and writing Expo native modules and views using the Expo Modules API (Swift, Kotlin, TypeScript). Covers module definition DSL, native views, shared objects, config plugins, lifecycle hooks, autolinking, and type system. Use when building or modifying native modules for Expo. Not for migrating an existing Swift module from the definition DSL to the Expo Modules API 2.0 macros; use expo-migrate-module (from the expo-experiments plugin) for that.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-experiments, expo-key-value-store, expo-migrate-module, expo-module, expo-module-template`
- **المراجع الفرعية (7)**: `agents/openai.yaml, references/config-plugin.md, references/create-expo-module.md, references/lifecycle.md, references/module-config.md, references/native-module.md, references/native-view.md`
- **السكريبتات (0)**: ``

## expo-native-ui — عناصر التحكم والأيقونات والمؤثرات و WebGPU (UI, Compose & Animations)
- **المسار**: `skills-main/plugins/expo/skills/expo-native-ui/SKILL.md`
- **الوصف**: Build beautiful, native-feeling Expo screens. Covers Apple HIG styling, semantic colors, native controls, SF Symbols, media, visual effects, gradients, storage, and responsive layout. For routing and navigation, use the expo-router skill; for motion and animation, use the expo-animation skill.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/ui, @expo/vector-icons, @react-navigation/native, expo-animation, expo-audio, expo-av, expo-blur, expo-camera, expo-data-fetching, expo-file-system, expo-glass-effect, expo-haptics, expo-image, expo-image-picker, expo-linear-gradient, expo-media-library, expo-native-ui, expo-permissions, expo-router, expo-secure-store, expo-sqlite, expo-symbols, expo-ui, expo-video`
- **المراجع الفرعية (8)**: `agents/openai.yaml, references/controls.md, references/gradients.md, references/icons.md, references/media.md, references/storage.md, references/visual-effects.md, references/webgpu-three.md`
- **السكريبتات (0)**: ``

## expo-overview — التوجيه المركزي لمهارات Expo (Architecture & Routing)
- **المسار**: `skills-main/plugins/expo/skills/expo-overview/SKILL.md`
- **الوصف**: Entry point and router for every Expo or EAS task. Load this skill first — before writing code and before choosing another expo-* / eas-* skill — when the request, PRD, or spec mentions Expo, EAS, Expo Go, or an expo-* package, or the project has an `expo` dependency in `package.json`. Within that gate it also covers app specs and designs to implement (tabs, stacks, maps, lists, navigation, building from a screenshot), and phrasings like 'implement a mobile app', 'make my app look native', 'add navigation', 'fetch some data', 'upgrade my SDK', 'add Expo to my existing native app', 'ship to the App Store', or 'I'm new to Expo, where do I start'. A fully specified request (SDK pinned, libraries named, layout given) still routes through here — the shared setup rules still apply. Do NOT load it when neither signal is present: a bare React Native project with no `expo` dependency is not Expo work. Detects the real goal, routes to the right expo-* / eas-* skill, and owns the shared setup rules.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/ui, expo-animation, expo-app, expo-app-clip, expo-brownfield, expo-data-fetching, expo-design-system, expo-dev-client, expo-dev-launcher, expo-development-client, expo-dom, expo-examples, expo-module, expo-native-ui, expo-overview, expo-project-structure, expo-router, expo-tailwind-setup, expo-ui, expo-upgrade, expo-web-to-native`
- **المراجع الفرعية (2)**: `agents/openai.yaml, references/launch-without-dev-menu.md`
- **السكريبتات (0)**: ``

## expo-project-structure — هيكلة المشاريع ومسارات الملفات (Architecture & Routing)
- **المسار**: `skills-main/plugins/expo/skills/expo-project-structure/SKILL.md`
- **الوصف**: Folder structure for a new Expo app. Use when scaffolding or laying out a new Expo project with Expo Router, or deciding where a file should live in one. For new projects only — never restructure an existing app to match.
- **الحزم والمكتبات**: `@expo/agent-cli, expo-app, expo-app-folder-structure-best-practices, expo-project-structure`
- **المراجع الفرعية (1)**: `agents/openai.yaml`
- **السكريبتات (0)**: ``

## expo-router — التنقل القائم على الملفات والتبويبات (Architecture & Routing)
- **المسار**: `skills-main/plugins/expo/skills/expo-router/SKILL.md`
- **الوصف**: Navigation and routing for Expo Router. Covers file-based routes, groups and dynamic routes, folder organization, Link with previews and context menus, native Stack, page titles, modals and form sheets, NativeTabs, headers and toolbars, and header search bars.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/vector-icons, @react-navigation/native, expo-animation, expo-native-ui, expo-router, react-native-screens`
- **المراجع الفرعية (7)**: `agents/openai.yaml, references/form-sheet.md, references/route-structure.md, references/search.md, references/tabs.md, references/toolbar-and-headers.md, references/zoom-transitions.md`
- **السكريبتات (0)**: ``

## expo-ui — واجهات Jetpack Compose و SwiftUI الأصلية (@expo/ui) (UI, Compose & Animations)
- **المسار**: `skills-main/plugins/expo/skills/expo-ui/SKILL.md`
- **الوصف**: Build native UI with the @expo/ui package: real SwiftUI on iOS and Jetpack Compose on Android. Default to @expo/ui for sheets (BottomSheet), pickers, sliders, toggles, menus, and grouped-form sections — do NOT reach for Reanimated, @gorhom/bottom-sheet, or RN built-in Picker/Switch; use @expo/ui instead. Fall back to RN built-ins only when @expo/ui is missing the component. NOTE: @expo/ui List renders native grouped rows like an iOS Settings screen — it is NOT a virtualized list; use FlatList/FlashList for large datasets. Covers universal components (Host, Column, Row, Button, Text, List, BottomSheet, FieldGroup, Switch, Slider, Picker, Menu), drop-in replacements for RN community libraries, and platform-specific SwiftUI/Jetpack Compose trees. Not for Expo Router navigation, Reanimated, or data fetching.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/ui, expo-ui, expo-ui-jetpack-compose, expo-ui-swift-ui, react-native-community, react-native-masked-view, react-native-menu, react-native-pager-view, react-native-picker, react-native-segmented-control, react-native-web, react-native-worklets`
- **المراجع الفرعية (5)**: `agents/openai.yaml, references/drop-in-replacements.md, references/jetpack-compose.md, references/swift-ui.md, references/universal.md`
- **السكريبتات (1)**: `scripts/list-components.js`

## expo-upgrade — ترقية الحزم إلى React 19 و React Compiler والمعمارية الجديدة (Data, Native & Modules)
- **المسار**: `skills-main/plugins/expo/skills/expo-upgrade/SKILL.md`
- **الوصف**: Guidelines for upgrading Expo SDK versions and fixing dependency issues
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/vector-icons, @react-navigation/bottom-tabs, @react-navigation/core, @react-navigation/elements, @react-navigation/material-top-tabs, @react-navigation/native, @react-navigation/native-stack, @react-navigation/routers, @react-navigation/stack, expo-app-loading, expo-audio, expo-av, expo-av-to-audio, expo-av-to-video, expo-codemod, expo-constants, expo-doctor, expo-linear-gradient, expo-permissions, expo-router, expo-router-react-navigation-replace, expo-splash-screen, expo-sqlite, expo-symbols`
- **المراجع الفرعية (8)**: `agents/openai.yaml, references/expo-av-to-audio.md, references/expo-av-to-video.md, references/native-tabs.md, references/new-architecture.md, references/react-19.md, references/react-compiler.md, references/react-navigation-to-expo-router.md`
- **السكريبتات (0)**: ``

## expo-web-to-native — تحويل تطبيقات React Web إلى تطبيقات أصلية (Architecture & Routing)
- **المسار**: `skills-main/plugins/expo/skills/expo-web-to-native/SKILL.md`
- **الوصف**: Migrate an existing web React app to a native iOS/Android app with Expo. Use when the user wants to turn a website into a mobile app, port a Next.js/Vite/CRA React codebase to React Native, reuse web code on native incrementally, or asks how web idioms (the DOM, CSS, React Router, localStorage, window) map to native. This is the end-to-end migration guide; use the `expo-dom` skill for the DOM-component mechanism itself.
- **الحزم والمكتبات**: `@expo/agent-cli, @expo/ui, expo-animation, expo-app, expo-app-clip, expo-auth-session, expo-camera, expo-clipboard, expo-data-fetching, expo-dev-client, expo-document-picker, expo-dom, expo-examples, expo-haptics, expo-image, expo-image-picker, expo-location, expo-native-ui, expo-notifications, expo-overview, expo-router, expo-secure-store, expo-sqlite, expo-ui, expo-web-to-native`
- **المراجع الفرعية (5)**: `agents/openai.yaml, references/false-friends.md, references/native-patterns.md, references/run-as-goal.md, references/verify-on-device.md`
- **السكريبتات (0)**: ``

