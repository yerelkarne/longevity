# Longevity Coach (Offline-first Android MVP)

## Paket/Klasör Yapısı
- `com.leosoft.longevity.data`
  - `local`: Room database, entities, dao, converters, datastore preferences
  - `repository`: repository implementasyonu
- `com.leosoft.longevity.domain`
  - `model`: MacroTotals, ScoreBreakdown, DayData, DashboardSummary
  - `repository`: repository sözleşmesi
  - `usecase`: makro ve skor hesaplama
- `com.leosoft.longevity.ui`
  - `onboarding`: ilk kurulum akışı
  - `main`: MainViewModel
  - `navigation`: bottom nav modeli
  - `screens.tabs`: bottom module + top tabs/pager ekranları
  - `components`: ortak kart/progress bileşenleri
  - `theme`: soft wellness tema
- `workers`, `reminders`, `steps`: hatırlatıcı ve Health Connect altyapısı

## MVP Kapsamı
- Clean Architecture + MVVM (tek activity).
- Bottom navigation (Günüm, Beslenme, Aktivite, Yaşam, Analiz).
- Her modülde sticky top tabs + swipe pager.
- Tam MVP ekranlar:
  - Günüm > Özet
  - Beslenme > Kayıt
- Room tabanlı offline-first veri saklama.
- Günlük Longevity Score hesaplama (0-100), veri değişince yeniden hesaplama.
- Onboarding ile hedef kaydı.
- WorkManager tabanlı hatırlatıcı altyapısı.
- Health Connect steps manager + manuel fallback için temel yapı.

## Nasıl Çalıştırırım
1. Android Studio (Ladybug+), SDK 35 kur.
2. Projeyi aç, Gradle sync yap.
3. Emülatör veya fiziksel cihazda çalıştır.
4. İlk açılışta onboarding tamamla.
5. `Günüm > Özet` ve `Beslenme > Kayıt` sekmelerinde canlı veri/score güncellemesini test et.

## Ekranlar Nerede
- Entry: `MainActivity`
- Onboarding: `ui/onboarding/OnboardingScreen.kt`
- Main tabs: `ui/screens/tabs/ModuleScreens.kt`
- Score logic: `domain/usecase/CalculateDailyScoreUseCase.kt`
