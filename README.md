# Wykrywacz Upadków dla Seniorów

**Wykrywacz Upadków dla Seniorów** to mobilna aplikacja na system Android, której celem jest zwiększenie bezpieczeństwa osób starszych poprzez automatyczne wykrywanie upadków oraz szybkie powiadamianie opiekunów. Wykorzystuje dane z wbudowanych czujników ruchu — akcelerometru i żyroskopu — aby analizować nagłe zmiany ruchu oraz stabilność ciała użytkownika.

## Kluczowe funkcje
- **Automatyczne wykrywanie upadków:** Algorytm analizuje dane sensorów, rozróżniając upadki od zwykłych ruchów lub upuszczenia telefonu.
- **Powiadomienia PUSH:** W przypadku wykrycia upadku aplikacja natychmiast wysyła powiadomienie do wyznaczonych opiekunów.
- **Role użytkowników:** System rejestracji z podziałem na seniorów oraz opiekunów, umożliwiający łatwe parowanie przez unikalny token.
- **Intuicyjny interfejs:** Przyjazny dla seniorów design, z dużymi czcionkami i czytelnymi kolorami.
- **Działanie w tle:** Aplikacja działa bez konieczności ciągłego otwierania, nie wymaga stałej interakcji użytkownika.

## Technologie użyte w projekcie
- **Android Kotlin:** Aplikacja mobilna napisana w Kotlinie.
- **Firebase:** Backend zapewniający autoryzację, bazę danych i obsługę powiadomień.
- **Random Forest:** Model klasyfikacji upadków oparty na uczeniu maszynowym.

## Instalacja i konfiguracja
1. Pobierz aplikację na telefon z systemem Android.
2. Zarejestruj konto wybierając rolę seniora lub opiekuna.
3. Senior udostępnia opiekunowi unikalny token do parowania.
4. Opiekun dodaje seniora za pomocą tokenu, aby otrzymywać powiadomienia o upadkach.
5. Aplikacja działa w tle, monitorując ruch i wykrywając potencjalne upadki.