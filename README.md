# Multilingvistický slovník

Autor: Viktor Modroczký\
Predmet: Vyhľadávanie informácií

Téma môjho projektu je **Vytvorenie multilingvistického slovníka z wikipédie slovenčina vs. iné jazyky (aspoň 2), vytvoriť spoločný slovník spájajúci jazyky s možnosťou vyhľadávania**.

Projekt bude riešený v jazyku **Java 17** s build nástrojom **Maven**.

## Rámce

**Apache Commons** na spracúvanie CSV súborov a reťazcov.\
**Gson** na spracúvanie JSON súborov pre vlastný index.\
**Apache Lucene** bude využitý na indexovanie v druhej fáze projektu.
**Apache Spark** bude využitý na distribuované spracovanie v druhej fáze projektu.

## Dáta

[Slovenská Wikipedia](https://dumps.wikimedia.org/skwiki/latest/)\
[Česká Wikipedia](https://dumps.wikimedia.org/cswiki/latest/)\
[Maďarská Wikipedia](https://dumps.wikimedia.org/huwiki/latest/)

## Zámer

Zámerom projektu je vytvorenie viacjazyčného slovníka z dát z Wikipedie, v ktorom bude používateľovi umožnené vyhľadávať.

Na každej Wikipedia stránke je v ľavom paneli sekcia *V iných jazykoch* (obrázok nižšie), ktorá obsahuje prepojenia na články v iných jazykoch. Na tieto prepojenia sa využivajú tzv. Interwiki Links, presnejšie Interlanguage Links (medzijazyčné odkazy).

![Preklady](wiki-preklady.png)

Medzijazyčné odkazy sú uchovávané v relačnej databáze v tabuľke *langlinks* (bude sa používať tabuľka pre slovenskú Wikipediu). Tabuľka obsahuje 3 stĺpce, a to `ll_from`, `ll_lang` a `ll_title`. Stĺpec `ll_from` je `page_id` odkazujúceho slovenského článku, `ll_lang` je kód jazyka cieľového článku a `ll_title` je názov cieľového článku v jazyku `ll_lang`.

## Verzia 2

**TODO** použitím Apache Spark a Lucene.

Použité verzie:

[Apache Spark 3.3.1 pre-built for Hadoop](https://www.apache.org/dyn/closer.lua/spark/spark-3.3.1/spark-3.3.1-bin-hadoop3.tgz)

[Hadoop 3.3.1 Windows Bin](https://github.com/kontext-tech/winutils/tree/master/hadoop-3.3.1/bin)

```env
SPARK_HOME=absolute\path\to\spark-3.3.1-bin-hadoop3
HADOOP_HOME=absolute\path\to\hadoop-3.3.1
```

K premennej `PATH` treba potom pridať:

```env
%SPARK_HOME%\bin
%HADOOP_HOME%\bin
```

Dependencies pre Spark **musia** byť nastavené nasledovne, keďže použitá verzia Spark používa Scala 2.12

```xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_2.12</artifactId>
    <version>3.3.1</version>
</dependency>
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.12</artifactId>
    <version>3.3.1</version>
    <scope>provided</scope>
</dependency>
```

K Java argumentom treba pridať `--add-exports java.base/sun.nio.ch=ALL-UNNAMED`.

Master node sa spúšťa pomocou `spark-class org.apache.spark.deploy.master.Master --host localhost`.

Worker node sa spúšťa pomocou `spark-class org.apache.spark.deploy.worker.Worker spark://localhost:7077`.

Nasledovne je možné spustiť WikiTranslator.

## Verzia 1

V tabuľke *langlinks* budeme vyhľadávať preklady článkov podľa kódov jazykov pomocou stĺpca `ll_lang`. Názvy článkov v slovenčine nájdeme pomocou hodnoty zo stĺpca `ll_from`, a to tak, že v tabuľke *page* pre slovenské články nájdeme `page_title` slovenských názvov podľa hodnôt `page_id`, ktoré zodpovedajú hodnotám `ll_from` v tabuľke *langlinks*. Takto sa budú vyhľadávať názvy rovnakých článkov v 3 rôznych jazykoch - zoberú sa ich názvy a prvé odseky. Prekladač bude vytvorený vo forme vyhľadávača, ktorý bude vracať výsledky na základe zadanej požiadavky. Napr. sa zadá slovo a jazyk, v ktorom sa má dané slovo vyhľadať, a potom sa vrátia všetky dokumenty (názvy článkov) aj s prekladmi do ostatných 2 jazykov, ktoré obsahujú dané slovo.

## Formát dokumentov

Každý dokument má *id* a verziu v slovenčine (*sk*), češtine (*cs*) a maďarčine (*hu*).

```csv
id,sk,cs,hu
1,Fridrich I. Habsburský,Fridrich I. Habsburský,(III.) Frigyes német király
2,Nulové zariadenie,/dev/null,/dev/null
3,0 (číslo),Nula,0 (szám)
4,1 (číslo),1 (číslo),1 (szám)
5,1 euro,1 euro,1 eurós érme
6,"1,2 - dichlóretán","1,2-dichlorethan","1,2-Diklóretán"
7,"1,4-dioxán","1,4-dioxan","1,4-Dioxán"
8,Linka 1 (parížske metro),1 (linka metra v Paříži),1-es metró (Párizs)
9,Prvý česko-slovenský armádny zbor v ZSSR,1. československý armádní sbor,1. Csehszlovák Hadtest
10,1. FC Lokomotive Leipzig (1966),1. FC Lokomotive Leipzig (1966),1. FC Lokomotive Leipzig
```

## Ako funguje vyhľadávanie?

Dokumenty sú indexované pomocou vlastného inverzného indexu pre každý jazyk, ktorý má formát:

`token: ID[]` - každý unikátny token má zoznam článkov, v ktorom sa nachádza.

```json
"stred": [
    "57622",
    "7259",
    "10883"
],
"poseidon": [
    "47671",
    "46947"
],
"palec": [
    "15616"
],
"hebrejcina": [
    "28625",
    "15510"
]
```

Indexy sú pri vytvorení inštancie triedy SearchEngine načítané z JSON súborov do HashMapy, aby bola efektivita vyhľadávania článkov so zhodou O(1).
Pri vyhľadávaní sa vráti zoznam dokumentov, ktoré zodpovedajú dopytu.

### Syntax vyhľadávača

`lang:qtext`, kde:

- `lang` može byť `sk`, `cs` alebo `hu`,
- `qtext` je text, ktorý sa má vyhľadávať v danom jazyku.

Program sa ukončuje príkazom `exit`.

**Príklad:**

`sk: covid test` vráti nasledovný výsledok:

```txt
1. Rýchly antigénový test COVID-19 (sk) = Rychlý antigenní test na covid-19 (cs) = Covid19-antigén gyorsteszt (hu)
2. Rýchly antigénový test (sk) = Rychlý antigenní test (cs) = Antigén gyorsteszt (hu)
3. Turingov test (sk) = Turingův test (cs) = Turing-teszt (hu)
4. Úmrtia na COVID-19 (sk) = Zemřelí na covid-19 (cs) = Covid19-ben elhunyt személyek (hu)
5. Kolmogorovov-Smirnovov test (sk) = Kolmogorovův-Smirnovův test (cs) = Kolmogorov-Szmirnov-próba (hu)
6. Vakcíny proti chorobe COVID-19 (sk) = Vakcíny proti covidu-19 (cs) = Covid19-védőoltások (hu)
7. Pandémia ochorenia COVID-19 (sk) = Pandemie covidu-19 (cs) = Covid19-koronavírus-járvány (hu)
8. Test (sk) = Testování (cs) = Test (egyértelműsítő lap) (hu)
9. Dáta pandémie ochorenia COVID-19 (sk) = Data pandemie covidu-19 (cs) = Covid19-pandémia adatai (hu)
10. COVID-19 (sk) = Covid-19 (cs) = Covid19 (hu)
11. COVID-19 (sk) = Covid-19 (cs) = Covid19 (hu)
12. Exploration Flight Test-1 (sk) = Exploration Flight Test 1 (cs) = Exploration Flight Test 1 (hu)
13. Vakcína proti chorobe COVID-19 (sk) = Vakcína proti covidu-19 (cs) = Covid19-vakcina (hu)
14. Pandémia ochorenia COVID-19 (sk) = Pandemie covidu-19 (cs) = Covid19-pandémia (hu)
```

Vidíme, že na prvom mieste je výsledok, ktorý v slovenčine obsahuje slová *covid* aj *test*, keďže je najrelevantnejší.
Ďalšie výsledky obsahujú iba jedno zo slov.
