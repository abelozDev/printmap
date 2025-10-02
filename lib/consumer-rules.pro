# Proj4j
-keep class org.osgeo.proj4j.** { *; }

# Ваши модели
-keep class ru.maplyb.printmap.api.model.** { *; }
-keep class ru.maplyb.printmap.impl.domain.model.** { *; }

# Apache POI - основные правила
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class org.apache.commons.compress.** { *; }

# Log4j и SLF4J
-keep class org.apache.logging.log4j.** { *; }
-keepclassmembers class org.apache.logging.log4j.** { *; }
-keep class org.slf4j.** { *; }

# Предупреждения для классов, которых нет на Android
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.collections4.**
-dontwarn org.apache.commons.math3.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.logging.log4j.core.**
-dontwarn org.apache.logging.log4j.util.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.slf4j.**