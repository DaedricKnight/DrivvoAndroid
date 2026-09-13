# Правила R8 для релизной сборки. Room, Navigation и kotlinx.serialization приносят свои consumer-правила.

# Перечисления в аргументах маршрутов навигация восстанавливает по имени класса.
-keepnames class com.artemkhateev.carlog.data.model.EntryType
-keepnames class com.artemkhateev.carlog.data.model.CatalogKind
