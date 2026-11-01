# خطة التنفيذ النهائية: إكمال مهارات العلوم الحيوية، لعبة الدفاع الخلوي التفاعلية، ومشاهد Three.js ثلاثية الأبعاد

## 1. نظرة عامة على المنتج والهدف العام (Overview & Creative Direction)
تهدف هذه المرحلة إلى استكمال منظومة **NovaForge AI Studio** عبر دمج ثلاث قدرات تفاعلية متقدمة تعمل بتناغم كامل مع قاعدة بيانات **Cloud Firestore**، محرك **Gemini AI**، وذاكرة المهارات البرمجية:
1. **مختبر ومهارات العلوم الحيوية المتكاملة (Bioscience & Genomic Skills Suite)**: توسيع قسم المختبر الحيوي ليشمل أربعة مسارات عمل تفاعلية فورية وحسابات حيوية حقيقية:
   - **محاكاة طي البروتينات والـ DNA ثلاثية الأبعاد (3D Protein & DNA Folding)**: حساب الكتلة الجزيئية (kDa)، مؤشر الكراهية للماء (GRAVY)، التنبؤ ببنية ألفا الحلزونية وصفائح بيتا، وعرض المجسم الجزيئي ثلاثي الأبعاد.
   - **محاكي التعديل الجيني CRISPR-Cas9**: فحص مواقع PAM (`NGG`) تلقائياً داخل تسلسل الحمض النووي، حساب كفاءة الاستهداف (On-Target Score) ومخاطر القطع غير المستهدف (Off-Target Risk)، وتوليد دليل sgRNA المثالي.
   - **تحليل الجينوم واكتشاف المتغيرات (Genomic Alignment & Variant Calling)**: حساب نسبة GC، التوزيع النيوكليوتيدي، كشف الطفرات النقطية (SNPs/Indels)، وترجمة أطر القراءة المفتوحة (ORFs).
   - **لوحة التجارب السريرية والمؤشرات الحيوية (Clinical Trials & Biomarker Analytics)**: محاكاة منحنيات الاستجابة الدوائية (IC50 / Dose-Response) وتقييم مراحل التجارب السريرية (Phase I–III) مع تصدير خط أنابيب الكود (Biopython / R / Nextflow) مباشرة إلى محرر الكود.
2. **لعبة الدفاع الخلوي والجزيئي التفاعلية (3D Cellular & Molecular Defense Game)**:
   - بناء لعبة كاملة المراحل تعمل بسرعة **60 إطاراً في الثانية (60fps)** مع دورة حالات متكاملة (`Title Screen` ➔ `Playing` ➔ `Pause` ➔ `Victory / Game Over Modal` مع إعادة اللعب الفورية دون إعادة تحميل الصفحة).
   - تصميم واجهة **HUD منخفضة التغطية (Low-Occlusion UI)** تترك أكثر من **85%** من مساحة اللعب مرئية بوضوح، مع عصا تحكم لمسية افتراضية (Virtual Touch Joystick) وأزرار مهارات دفاعية تفاعلية (إطلاق الأجسام المضادة، نبضة CRISPR، والدرع المناعي).
   - دعم كامل لمؤثرات الشعور باللعب (**Game Feel**): اهتزاز الكاميرا الدقيق (Camera Shake)، التوقف اللحظي عند الإصابة الحرجة (Hitstop)، انفجارات الجسيمات المجمعة (Pooled Particle Bursts)، ومؤشرات التحذير المسبقة قبل هجوم الفيروسات (Enemy Telegraphs)، بالإضافة إلى مُركّب صوتي إجرائي عبر **Web Audio API** مع زر كتم/تفعيل الصوت (`Mute Toggle`).
3. **مستعرض ومختبر مشاهد Three.js ثلاثية الأبعاد (Three.js 3D Procedural Scenes & IDE Template)**:
   - توفير محرك عرض ثلاثي الأبعاد هجين (مدمج في قسم المختبر الحيوي وكقالب جاهز **`🎮 3D Three.js & BioGame`** داخل محرر الكود `IDE & Live Preview`).
   - بناء نماذج إجرائية مركبة (Composite Procedural 3D Models من 3 إلى 12 جزءاً هيكلياً: حلزون DNA مزدوج، مركب بروتيني مطوي، كبسولة فيروسية متعددة الأوجه، وخلية مناعية بنواة مضيئة) مع إضاءة قياسية ثلاثية النقاط (Key, Fill, Rim PBR Lighting)، ضباب عمق جوي، وتحكم كاميرا انسيابي (Orbit / Damped Camera) مع شريط أدوات عائم.

---

## 2. الميزات التفصيلية وهيكلة المكونات (Key Features & Component Architecture)

### أ. محرك العلوم الحيوية والمهارات التفاعلية (`BioscienceSimulationEngine.kt`)
- إنشاء وحدة حسابية ومولّد مشاهد متكامل (`com.example.data.skills.BioscienceSimulationEngine`) يقدم:
  - **حسابات بيولوجية فورية محلية (Real-Time Bioinformatics Metrics)**: حساب دقيق لطول التسلسل، نسبة `GC%`، درجة حرارة الانصهار التقريبية (`Tm`)، البحث عن مواقع `NGG` الخاصة بـ CRISPR-Cas9، ترجمة التسلسل إلى أحماض أمينية، وحساب مؤشرات التجارب السريرية (`IC50`, `p-value`, `Survival Rate`).
  - **كتالوج مهارات العلوم الحيوية المدمجة (Bioscience Skills Presets)**: بطاقات مهارات تفاعلية جاهزة للحقن في محرر الكود أو المساعد الذكي (`3D Protein Folding AlphaFold Pipeline`, `CRISPR-Cas9 sgRNA Designer`, `Genomic Variant Caller VCF`, `Clinical Trial Survival & Dose Analytics`).
  - **مولد HTML5 / Three.js WebGL & Canvas 3D التفاعلي**: يولّد كود المشهد ثلاثي الأبعاد واللعبة التفاعلية المتكاملة مع Web Audio API، عصا التحكم اللمسية، وأزرار المهارات ليعمل بسلاسة تامة داخل كل من المختبر الحيوي ونافذة المعاينة الحية في `IdePreviewTabScreen`.

### ب. تطوير واجهة المختبر الحيوي واللعبة (`BioscienceLabTabScreen.kt`)
- إضافة شريط أوضاع تفاعلي في أعلى قسم المختبر الحيوي (`BioscienceLabTabScreen`) للتبديل السلس بين:
  1. **🎮 لعبة الدفاع الخلوي ومستعرض 3D (3D Cellular Defense & Three.js Viewer)**:
     - عارض تفاعلي مباشر (Hardware-Accelerated WebView + Native Compose Telemetry Bar) يعرض:
       - **وضع اللعبة (Cellular Defense Game Mode)**: شاشة عنوان تفاعلية، عداد نقاط وموجات (Waves 1–3)، شريط صحة الخلية المضيفة، عصا تحكم لمسية على اليسار، وأزرار إطلاق الأجسام المضادة / نبضة CRISPR على اليمين، مع زر إيقاف مؤقت (`Pause`) وزر كتم الصوت (`Mute`) وشاشة فوز/انتهاء اللعبة (`Victory / Game Over`).
       - **وضع مستعرض المشاهد 3D (Three.js Procedural 3D Inspector)**: التبديل بين 4 مجسمات ثلاثية الأبعاد مركبة (`DNA Double Helix`, `Folded Protein Complex`, `CRISPR-Cas9 Ribonucleoprotein`, `Viral Capsid & Nanobot`) مع التحكم في الدوران التلقائي، تفكيك الأجزاء (`Explode View`)، وإعادة ضبط الكاميرا (`Reset Camera`).
  2. **🧬 أدوات التحليل الحيوي الأربعة (Protein Folding, CRISPR, Genomics, Clinical Trials)**:
     - شرائح اختيار سريعة للمسارات الأربعة مع عرض بطاقات نتائج فورية (مواقع PAM المكتشفة، نسبة GC، البنية الثانوية المتوقعة، ومؤشرات التجارب السريرية) بجانب التقرير المعمق المدعوم بنموذج `gemini-4-argon` وزر تصدير المشهد أو الكود العلمي مباشرة إلى محرر `IDE`.

### ج. دمج قالب Three.js واللعبة في بيئة التطوير (`IdePreviewTabScreen.kt` & `StudioViewModel.kt`)
- إضافة زر قالب جاهز جديد في شريط قوالب محرر الكود (`IdePreviewTabScreen`):
  - **`🎮 3D Three.js & BioGame`** (`testTag = "template_threejs_game"`): يقوم بتحميل كود HTML/CSS/JS/Backend الكامل لمحرك المشاهد ثلاثية الأبعاد ولعبة الدفاع الخلوي داخل تبويبات المحرر (`HTML`, `CSS`, `JS`, `Backend`) وتشغيله فوراً في نافذة المعاينة الحية (`Live Interactive Preview`) مع إمكانية تعديل سرعة اللعبة، الإضاءة، أو الجسيمات مباشرة من الكود.
- حفظ أعلى نتيجة في لعبة الدفاع الخلوي (`highScore` و `maxWaveReached`) ومزامنتها في ذاكرة المشروع وسجل البناء في **Cloud Firestore**.

---

## 3. معايير الجودة والتحقق (Verification & Quality Criteria)
- **سلاسة الأداء (60fps & Clamped Delta Time)**: استخدام حلقة `requestAnimationFrame` واحدة مع تقييد الزمن التفاضلي `Math.min(dt, 0.05)` وتجميع الجسيمات (Object Pooling) لمنع أي تقطيع أثناء الانفجارات الجزيئية.
- **حماية الواجهة (ErrorBoundary & Safe Insets)**: الحفاظ على عمل جميع المكونات داخل `ErrorBoundary` مع احترام الهوامش الآمنة (`WindowInsets`) وتوفير `Modifier.testTag(...)` ومساحة لمس لا تقل عن `48.dp` لكافة الأزرار التفاعلية.
- **التحقق البرمجي والاختبارات**: تشغيل `compile_applet` وتنفيذ اختبارات الوحدة و Robolectric (`ExampleRobolectricTest`) للتأكد من سلامة التنقل وعرض كافة التبويبات والمكونات الجديدة بنجاح.
