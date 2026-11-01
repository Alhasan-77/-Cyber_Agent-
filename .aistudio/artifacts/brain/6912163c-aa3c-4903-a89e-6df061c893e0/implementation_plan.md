# خطة تنفيذ نظام المصادقة السحابية (Google Sign-In) وقاعدة بيانات Firestore المتكاملة

## 1. نظرة عامة والهدف
تهدف هذه الخطة إلى استكمال وتوسيع البنية السحابية في تطبيق **NovaForge Studio** بحيث يتم ربط جلسة المستخدم الموثقة عبر **Google Sign-In (`CredentialManager`)** بمزامنة سحابية شاملة وفورية في **Cloud Firestore** تشمل:
1. **ملف تعريف المطور السحابي (`UserProfile`)**: حفظ بيانات حساب Google، إحصائيات النشاط، وتوقيت آخر مزامنة سحابية.
2. **تجارب المختبر الحيوي وطي البروتين (`BioExperimentRecord`)**: حفظ تسلسلات الأحماض الأمينية، نتائج طي البروتين ثلاثي الأبعاد، طاقة الاستقرار (`freeEnergyKcal`)، ومؤشر الثقة (`pLddtScore`) في Firestore لاستعادتها عبر الأجهزة.
3. **المشاريع البرمجية ولقطات الإصدارات ومخططات البيانات وسجلات البناء (`StudioProject`, `VersionSnapshot`, `DbCollectionSchema`, `BuildExecutionLog`)**: دعم المزامنة الفورية التلقائية مع إمكانية الحفظ اليدوي واستعادة النسخ بنقرة واحدة.

---

## 2. تجربة المستخدم وتدفق الواجهات (UX & UI Flow)

### أ. بطاقة الملف الشخصي التفاعلية وإدارة الحسابات
- إضافة **بطاقة ملف شخصي تفاعلية (Interactive Cloud Profile Card & Sheet)** في الشريط العلوي للاستوديو وداخل لوحة التحكم:
  - تعرض اسم المستخدم، البريد الإلكتروني، الصورة الرمزية أو شارة الهوية السحابية، وحالة الاتصال والمزامنة الفورية (`Live Sync Active` / `Syncing...` / `Synced`) مع زمن الاستجابة (`Latency ms`).
  - تتيح **تبديل حساب Google** مباشرة (عبر مسح جلسة `CredentialManager` وفتح منتقي حسابات Google) أو **تسجيل الخروج الآمن**، بالإضافة إلى زر **مزامنة سحابية فورية يدوية (Force Cloud Sync)**.

### ب. حفظ واستعادة تجارب المختبر الحيوي (Bioscience Lab Cloud Sync)
- داخل تبويب **المختبر الحيوي (`BioscienceLabTabScreen`)**:
  - حفظ تلقائي وفوري أو يدوي لأي تجربة طي بروتين أو تحليل جيني في مجموعة `users/{userId}/bio_experiments`.
  - قائمة سحابية تفاعلية للتجارب المحفوظة تتيح **استعادة التجربة (Restore Experiment)** إلى العارض ثلاثي الأبعاد أو حذفها من السحابة.

### ج. المزامنة التلقائية واليدوية للمشاريع وإدارة قاعدة البيانات
- تحديث تلقائي لحظي (`Real-Time Snapshot Listeners`) عند تعديل المشاريع أو إضافة لقطات إصدار (`Snapshots`) مع إشعار حالة المزامنة.
- توفير لوحة تحكم شاملة في تبويب **قاعدة البيانات والإصدارات (`DatabaseAndVcsTabScreen`)** ومكون **`FirestoreDataManagerComponent`** لاستعراض وإدارة جميع المجموعات السحابية الخمس (`Profile`, `Projects`, `Snapshots`, `Collections`, `BioExperiments`, `Logs`).

---

## 3. هيكلة قاعدة البيانات وقواعد الأمان (Firestore Schema & Security Rules)

سيتم تحديث كل من `firebase-blueprint.json` و `firestore.rules` وفق معمارية **Zero-Trust** بحيث يقتصر الوصول على مالك الحساب الموثق (`request.auth.uid == userId`):

1. **وثيقة الملف الشخصي (`/users/{userId}`)**:
   - الحقول: `userId`, `email`, `displayName`, `photoUrl`, `role`, `projectsCount`, `bioExperimentsCount`, `createdAt`, `updatedAt`.
2. **مجموعة تجارب المختبر الحيوي (`/users/{userId}/bio_experiments/{experimentId}`)**:
   - الحقول: `userId`, `title`, `sequence`, `foldingMode`, `pLddtScore`, `freeEnergyKcal`, `helixPercent`, `sheetPercent`, `aiSummary`, `createdAt`, `updatedAt`.
3. **المجموعات الحالية المحمية (`/users/{userId}/projects`, `/snapshots`, `/collections`, `/logs`)**:
   - الحفاظ على التحقق الصارم من الأنواع والأحجام وملكية المستخدم (`existing().userId == request.auth.uid`).

---

## 4. المكونات البرمجية المقرر إضافتها أو تحديثها

- **`data/model/StudioModels.kt`**:
  - إضافة نموذجي `UserProfile` و `BioExperimentRecord` المتوافقين مع Firestore (`@DocumentId`, `@ServerTimestamp`, قيم افتراضية آمنة).
- **`data/repository/StudioRepository.kt`**:
  - إضافة دوال المزامنة اللحظية والعمليات (`observeUserProfile`, `upsertUserProfile`, `observeBioExperiments`, `saveBioExperiment`, `deleteBioExperiment`).
- **`ui/viewmodel/StudioViewModel.kt` و `ui/viewmodel/DataViewModel.kt`**:
  - ربط حالة المزامنة التلقائية للملف الشخصي وتجارب المختبر الحيوي والمشاريع عند تسجيل الدخول، وتوفير دوال الحفظ اليدوي واستعادة النسخ.
- **`ui/components/MainScreenLayout.kt` و `ui/screens/AuthScreen.kt`**:
  - إضافة بطاقة الملف الشخصي التفاعلية مع مؤشر المزامنة السحابية وخيار تبديل حساب Google.
- **`ui/screens/BioscienceLabTabScreen.kt` و `ui/components/FirestoreDataManagerComponent.kt`**:
  - دمج سجل التجارب الحيوية السحابية مع إمكانية الحفظ اليدوي والتلقائي والاستعادة الفورية.
- **`firestore.rules` و `FirestoreSecurityRulesTest.kt`**:
  - تحديث قواعد الأمان واختباراتها ثم نشرها عبر `deploy_firebase` والتحقق من البناء عبر `compile_applet`.
