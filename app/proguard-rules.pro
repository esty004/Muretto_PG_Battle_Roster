# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 1. Regole per Kotlinx Serialization e Supabase
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# 2. Manteniamo intatti i nomi dei modelli dati della nostra app
-keep class com.example.muretto_pg_app.Freestyler { *; }
-keep class com.example.muretto_pg_app.ProfiloUtente { *; }
-keep class com.example.muretto_pg_app.RichiestaAccount { *; }
-keep class com.example.muretto_pg_app.Evento { *; }
-keep class com.example.muretto_pg_app.EventoPreferito { *; }