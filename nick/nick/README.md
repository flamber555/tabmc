# Ranks (LuckPerms nametag)

Prosty poradnik jak pobrać i uruchomić wtyczkę odpowiedzialną za rangi przy głowie na bazie LuckPerms.

## Wymagania
- Java 17
- Paper/Spigot 1.20.4+
- LuckPerms na serwerze (biblioteka jest oznaczona jako `provided`)
- Maven do zbudowania pliku JAR

## Jak zbudować i pobrać JAR
1. Sklonuj repozytorium i przejdź do katalogu projektu:
   ```bash
   git clone <adres-repo>
   cd tabmc/nick/nick
   ```
2. Zbuduj projekt Mavenem:
   ```bash
   mvn clean package
   ```
   Po udanym buildzie gotowy plik znajdziesz w `target/nick-1.0.0.jar`.
3. Skopiuj `target/nick-1.0.0.jar` do folderu `plugins/` swojego serwera Paper/Spigot.
4. Uruchom serwer — wtyczka automatycznie podłączy się do LuckPerms i ustawi prefixy/sufixy w tagach graczy.

## Aktualizacja/ponowne pobranie
Po każdej zmianie w kodzie powtórz komendę `mvn clean package` i podmień plik w `plugins/` najnowszą wersją z katalogu `target/`.
